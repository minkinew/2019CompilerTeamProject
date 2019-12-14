package listener.main;

import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import generated.MiniCBaseListener;
import generated.MiniCParser;
import generated.MiniCParser.ParamsContext;

import static listener.main.BytecodeGenListenerHelper.getCurrentClassName;
import static listener.main.BytecodeGenListenerHelper.getFunName;
import static listener.main.BytecodeGenListenerHelper.getFunProlog;
import static listener.main.BytecodeGenListenerHelper.getLocalVarName;
import static listener.main.BytecodeGenListenerHelper.getLocalVarSize;
import static listener.main.BytecodeGenListenerHelper.getStackSize;
import static listener.main.BytecodeGenListenerHelper.initVal;
import static listener.main.BytecodeGenListenerHelper.isArrayDecl;
import static listener.main.BytecodeGenListenerHelper.isDeclWithInit;
import static listener.main.BytecodeGenListenerHelper.isFunDecl;
import static listener.main.BytecodeGenListenerHelper.isIntReturn;
import static listener.main.BytecodeGenListenerHelper.noElse;
import static listener.main.BytecodeGenListenerHelper.isVoidF;
import static listener.main.SymbolTable.*;

public class BytecodeGenListener extends MiniCBaseListener implements ParseTreeListener {
    ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
    SymbolTable symbolTable = new SymbolTable();

    int tab = 0;
    int label = 0;
    int count = 0;
    // program	: decl+

    @Override
    public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
        symbolTable.initFunDecl();

        String fname = getFunName(ctx);
        ParamsContext params;

        if (fname.equals("main")) {
            symbolTable.putLocalVar("args", Type.INTARRAY);
        } else {
            symbolTable.putFunSpecStr(ctx);
            params = (MiniCParser.ParamsContext) ctx.getChild(3);
            symbolTable.putParams(params);
        }
    }


    // var_decl	: type_spec IDENT ';' | type_spec IDENT '=' LITERAL ';'|type_spec IDENT '[' LITERAL ']' ';'
    @Override
    public void enterVar_decl(MiniCParser.Var_declContext ctx) {
        String varName = ctx.IDENT().getText();

        if (isArrayDecl(ctx)) {
            symbolTable.putGlobalVar(varName, Type.INTARRAY);
        } else if (isDeclWithInit(ctx)) {
            symbolTable.putGlobalVarWithInitVal(varName, Type.INT, initVal(ctx));
        } else { // simple decl
            symbolTable.putGlobalVar(varName, Type.INT);
        }
    }


    @Override
    public void enterLocal_decl(MiniCParser.Local_declContext ctx) {
        if (isArrayDecl(ctx)) {
            symbolTable.putLocalVar(getLocalVarName(ctx), Type.INTARRAY);
        } else if (isDeclWithInit(ctx)) {
            symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), Type.INT, initVal(ctx));
        } else { // simple decl
            symbolTable.putLocalVar(getLocalVarName(ctx), Type.INT);
        }
    }


    @Override
    public void exitProgram(MiniCParser.ProgramContext ctx) {
        String classProlog = getFunProlog();

        String fun_decl = "", var_decl = "";

        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (isFunDecl(ctx, i))
                fun_decl += newTexts.get(ctx.decl(i));
            else
                var_decl += newTexts.get(ctx.decl(i));
        }

        newTexts.put(ctx, classProlog + var_decl + fun_decl);

        System.out.println(newTexts.get(ctx));
    }


    // decl	: var_decl | fun_decl
    @Override
    public void exitDecl(MiniCParser.DeclContext ctx) {
        String decl = "";
        if (ctx.getChildCount() == 1) {
            if (ctx.var_decl() != null)                //var_decl
                decl += newTexts.get(ctx.var_decl());
            else                            //fun_decl
                decl += newTexts.get(ctx.fun_decl());
        }
        newTexts.put(ctx, decl);
    }

    // stmt	: expr_stmt | compound_stmt | if_stmt | while_stmt | return_stmt
    @Override
    public void exitStmt(MiniCParser.StmtContext ctx) {
        String stmt = "";
        if (ctx.getChildCount() > 0) {
            if (ctx.expr_stmt() != null)                // expr_stmt
                stmt += newTexts.get(ctx.expr_stmt());
            else if (ctx.compound_stmt() != null)    // compound_stmt
                stmt += newTexts.get(ctx.compound_stmt());
                // <(0) Fill here>
            else if (ctx.if_stmt() != null)            // if_stmt
                stmt += newTexts.get(ctx.if_stmt());
            else if (ctx.while_stmt() != null)        // while_stmt
                stmt += newTexts.get(ctx.while_stmt());
            else if (ctx.return_stmt() != null)        // return_stmt
                stmt += newTexts.get(ctx.return_stmt());
        }
        newTexts.put(ctx, stmt);
    }

    // expr_stmt	: expr ';'
    @Override
    public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
        String stmt = "";
        if (ctx.getChildCount() == 2) {
            stmt += newTexts.get(ctx.expr());    // expr
        }
        newTexts.put(ctx, stmt);
    }


    // while_stmt	: WHILE '(' expr ')' stmt
    @Override
    public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) {
        // <(1) Fill here!>
        String loopStartLabel = symbolTable.newLabel(); // 루프 시작 심볼
        String loopEndLabel = symbolTable.newLabel(); // 루프 종료 심볼
        String whileStmt = "";
        whileStmt += loopStartLabel + ":\n"; // 루프의 시작
        whileStmt += newTexts.get(ctx.expr()); // 조건식
        whileStmt += "ifeq " + loopEndLabel + "\n"; // expr에 대한 계산 값이 0이면 루프를 종료한다.
        whileStmt += newTexts.get(ctx.stmt()); // 루프 실행 내용
        whileStmt += "goto " + loopStartLabel + "\n";
        whileStmt += loopEndLabel + ":\n"; // 종료 문구
        newTexts.put(ctx, whileStmt);
    }

    @Override
    public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
        // <(2) Fill here!>
        String funDecl = "";
        funDecl += funcHeader(ctx, getFunName(ctx));
        funDecl += newTexts.get(ctx.compound_stmt());

        // return_stmt가 fun_decl의 compound_stmt에 속하는 stmt가 return_stmt인지 확인.
        // 즉, 결합 상태가 아닌 채로 밖에 나와있는 return이 있는지 확인한다. (jmp 오류 방지)
        Boolean noReturnStmt = true; // returnStmt가 있는지 확인하는 변수
        MiniCParser.Compound_stmtContext compound_stmtContext = ctx.compound_stmt();
        for (int index = 0; index < compound_stmtContext.getChildCount(); ++index) {
            if (compound_stmtContext.getChild(index) instanceof MiniCParser.Return_stmtContext) {
                noReturnStmt = false;
                break;
            }
        }
        if (noReturnStmt)
            funDecl += "";

        funDecl += "";
        newTexts.put(ctx, funDecl);
    }


    private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
        return "0000000000000000" + symbolTable.getFunSpecStr(fname) + "\n"
                + "push %rbp"+ "\n"
                + "mov %rsp,%rbp" + "\n"
                + "mov %0x0,%eax \n"; // 탭 기능 지움.

    }


    @Override
    public void exitVar_decl(MiniCParser.Var_declContext ctx) {
        String varName = ctx.IDENT().getText();
        String varDecl = "";

        if (isDeclWithInit(ctx)) {
            varDecl += "putfield " + varName + "\n";
            // v. initialization => Later! skip now..:
        }
        newTexts.put(ctx, varDecl);
    }


    @Override
    public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
        String varDecl = "";
        String temp = "";
        if (isDeclWithInit(ctx)) {
            String vId = symbolTable.getVarId(ctx);
            temp = ctx.LITERAL().getText();
            int to = Integer.parseInt(temp);
            String s = String.format("%02X%n", to);
            s = "$0x" + s;
            varDecl += "movl " + s + ""
                    + "istore_" + vId + "\n";
        }
        newTexts.put(ctx, varDecl);
    }


    // compound_stmt	: '{' local_decl* stmt* '}'
    @Override
    public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) {
        // <(3) Fill here>
        String compoundStmt = "";
        int index = 0;
        int localDeclIndex;
        int stmtIndex;

        for (index = 1, localDeclIndex = 0; ctx.getChild(index) instanceof MiniCParser.Local_declContext; ++index, ++localDeclIndex)
            compoundStmt += newTexts.get(ctx.local_decl(localDeclIndex));

        for (stmtIndex = 0; ctx.getChild(index) instanceof MiniCParser.StmtContext; ++index, ++stmtIndex)
            compoundStmt += newTexts.get(ctx.stmt(stmtIndex));

        newTexts.put(ctx, compoundStmt);
    }

    // if_stmt	: IF '(' expr ')' stmt | IF '(' expr ')' stmt ELSE stmt;
    @Override
    public void exitIf_stmt(MiniCParser.If_stmtContext ctx) {
        String stmt = "";
        String condExpr = newTexts.get(ctx.expr());
        String thenStmt = newTexts.get(ctx.stmt(0));

        String lend = symbolTable.newLabel();
        String lelse = symbolTable.newLabel();


        if (noElse(ctx)) {
            stmt += condExpr
                    + "ifeq " + lend + "\n"
                    + thenStmt
                    + lend + ":" + "\n";
        } else {
            String elseStmt = newTexts.get(ctx.stmt(1));
            stmt += condExpr
                    + "ifeq " + lelse + "\n"
                    + thenStmt
                    + "goto " + lend + "\n"
                    + lelse + ":\n" + elseStmt
                    + lend + ":" + "\n";
        }

        newTexts.put(ctx, stmt);
    }


    // return_stmt	: RETURN ';' | RETURN expr ';'
    @Override
    public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
        // <(4) Fill here>
        String returnStmt = "";
//        if (isIntReturn(ctx)) {
//            returnStmt += newTexts.get(ctx.expr());
//            returnStmt += "i";
//        }
        returnStmt += "ret\n";
        newTexts.put(ctx, returnStmt);
    }


    @Override
    public void exitExpr(MiniCParser.ExprContext ctx) {
        String expr = "";

        if (ctx.getChildCount() <= 0) {
            newTexts.put(ctx, "");
            return;
        }

        if (ctx.getChildCount() == 1) { // IDENT | LITERAL
            if (ctx.IDENT() != null) {
                String idName = ctx.IDENT().getText();
                if (symbolTable.getVarType(idName) == Type.INT) {
                    expr += "mov" + symbolTable.getVarId(idName) + " \n";
                }
                //else	// Type int array => Later! skip now..
                //	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
            } else if (ctx.LITERAL() != null) {
                String literalStr = ctx.LITERAL().getText();
                expr += "ldc " + literalStr + " \n";
            }
        } else if (ctx.getChildCount() == 2) { // UnaryOperation
            expr = handleUnaryExpr(ctx, newTexts.get(ctx) + expr);
        } else if (ctx.getChildCount() == 3) {
            if (ctx.getChild(0).getText().equals("(")) {        // '(' expr ')'
                expr = newTexts.get(ctx.expr(0));

            } else if (ctx.getChild(1).getText().equals("=")) {    // IDENT '=' expr
                expr = newTexts.get(ctx.expr(0))
                        + "istore_" + symbolTable.getVarId(ctx.IDENT().getText()) + " \n";

            } else {                                            // binary operation
                expr = handleBinExpr(ctx, expr);

            }
        }
        // IDENT '(' args ')' |  IDENT '[' expr ']'
        else if (ctx.getChildCount() == 4) {
            if (ctx.args() != null) {        // function calls
                expr = handleFunCall(ctx, expr);
            } else { // expr
                // Arrays: TODO
            }
        }
        // IDENT '[' expr ']' '=' expr
        else { // Arrays: TODO			*/
        }
        newTexts.put(ctx, expr);
    }


    private String handleUnaryExpr(MiniCParser.ExprContext ctx, String expr) {
        String l1 = symbolTable.newLabel();
        String l2 = symbolTable.newLabel();
        String lend = symbolTable.newLabel();
        expr += newTexts.get(ctx.expr(0));
        switch (ctx.getChild(0).getText()) {
            case "-":
                expr += "           ineg \n";
                break;
            case "--":
                expr += "ldc 1" + "\n"
                        + "isub" + "\n"
                        + "istore_" + symbolTable.getVarId(ctx.getChild(1).getChild(0).getText()) + "\n";
                break;
            case "++":
                expr += "ldc 1" + "\n"
                        + "iadd" + "\n"
                        + "istore_" + symbolTable.getVarId(ctx.getChild(1).getChild(0).getText()) + "\n";
                break;
            case "!":
                expr += "ifeq " + l2 + "\n"
                        + l1 + ":\n" + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ":\n" + "ldc 1" + "\n"
                        + lend + ":\n";
                break;
        }
        return expr;
    }


    private String handleBinExpr(MiniCParser.ExprContext ctx, String expr) {
        String l2 = symbolTable.newLabel();
        String lend = symbolTable.newLabel();

        expr += newTexts.get(ctx.expr(0));
        expr += newTexts.get(ctx.expr(1));

        switch (ctx.getChild(1).getText()) {
            case "*":
                expr += "imul \n";
                break;
            case "/":
                expr += "idiv \n";
                break;
            case "%":
                expr += "irem \n";
                break;
            case "+":        // expr(0) expr(1) iadd
                expr += "iadd \n";
                break;
            case "-":
                expr += "isub \n";
                break;

            case "==":
                expr += "isub " + "\n"
                        + "ifeq " + l2 + "\n"
                        + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ":\n" + "ldc 1" + "\n"
                        + lend + ":\n";
                break;
            case "!=":
                expr += "isub " + "\n"
                        + "ifne l2" + "\n"
                        + "ldc 0" + "\n"
                        + "goto " + lend + "\n"
                        + l2 + ":\n" + "ldc 1" + "\n"
                        + lend + ":\n";
                break;
            case "<=":
                // <(5) Fill here>
                expr += "isub \n"
                        + "ifle " + l2 + "\n"
                        + "ldc 0\n"
                        + "goto " + lend + "\n"
                        + l2 + ":\n" + "ldc 1" + "\n"
                        + lend + ":\n";
                break;
            case "<":
                // <(6) Fill here>
                expr += "isub \n"
                        + "iflt " + l2 + "\n"
                        + "ldc 0\n"
                        + "goto " + lend + "\n"
                        + l2 + ":\n" + "ldc 1" + "\n"
                        + lend + ":\n";
                break;

            case ">=":
                // <(7) Fill here>
                expr += "isub \n"
                        + "ifge " + l2 + "\n"
                        + "ldc 0\n"
                        + "goto " + lend + "\n"
                        + l2 + ":\n" + "ldc 1" + "\n"
                        + lend + ":\n";
                break;

            case ">":
                // <(8) Fill here>
                expr += "isub \n"
                        + "ifgt " + l2 + "\n"
                        + "ldc 0\n"
                        + "goto " + lend + "\n"
                        + l2 + ":\n" + "ldc 1" + "\n"
                        + lend + ":\n";
                break;

            case "and":
                expr += "ifne " + lend + "\n"
                        + "pop" + "\n" + "ldc 0" + "\n"
                        + lend + ":";
                break;
            case "or":
                // <(9) Fill here>
                expr += "ifeq " + lend + "\n"
                        + "pop" + "\n" + "ldc 1" + "\n"
                        + lend + ":";
                break;

        }
        return expr;
    }

    private String handleFunCall(MiniCParser.ExprContext ctx, String expr) {
        String fname = getFunName(ctx);

        if (fname.equals("_print")) {        // System.out.println
            expr = "getstatic java/lang/System/out Ljava/io/PrintStream;\n"
                    + newTexts.get(ctx.args())
                    + "invokevirtual " + symbolTable.getFunSpecStr("_print") + "\n";
        } else {
            expr = newTexts.get(ctx.args())
                    + "invokestatic " + getCurrentClassName() + "/" + symbolTable.getFunSpecStr(fname) + "\n";
        }

        return expr;

    }

    // args	: expr (',' expr)* | ;
    @Override
    public void exitArgs(MiniCParser.ArgsContext ctx) {

        String argsStr = "";

        for (int i = 0; i < ctx.expr().size(); i++) {
            argsStr += newTexts.get(ctx.expr(i));
        }
        newTexts.put(ctx, argsStr);
    }

}
