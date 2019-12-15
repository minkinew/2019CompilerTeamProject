package listener.main;

import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import generated.MiniCBaseListener;
import generated.MiniCParser;
import generated.MiniCParser.ParamsContext;

import javax.xml.bind.SchemaOutputResolver;

import static listener.main.BytecodeGenListenerHelper.getFunName;
import static listener.main.BytecodeGenListenerHelper.getFunProlog;
import static listener.main.BytecodeGenListenerHelper.getLocalVarName;
import static listener.main.BytecodeGenListenerHelper.initVal;
import static listener.main.BytecodeGenListenerHelper.isArrayDecl;
import static listener.main.BytecodeGenListenerHelper.isDeclWithInit;
import static listener.main.BytecodeGenListenerHelper.isFunDecl;
import static listener.main.BytecodeGenListenerHelper.noElse;
import static listener.main.SymbolTable.*;

public class BytecodeGenListener extends MiniCBaseListener implements ParseTreeListener {
    ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
    SymbolTable symbolTable = new SymbolTable();
    int lo_count = 0;
    boolean subcheck = false;
    int tab = 0;
    int label = 0;
    int count = 0;
    boolean maincheck = false;
    String[] normalRegister = {"%edx", "%eax"};
    int registerCount = 0;
    int register_sub = 1;
    boolean check = false;
    String calc_temp_number = "";
    // program   : decl+

    @Override
    public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
        int numberOfLocalDeclCtx = 0; // Local_Decl의 개수
        int allocStackStartPoint; //
        for (int i = 1; ctx.getChild(5).getChild(i) instanceof MiniCParser.Local_declContext; ++i)
            ++numberOfLocalDeclCtx;
        allocStackStartPoint = (numberOfLocalDeclCtx * 4); // 변수를 할당하기 시작하는 지점
        symbolTable.initFunDecl();
        symbolTable.set_localVarOffset(allocStackStartPoint);

        String fname = getFunName(ctx);
        ParamsContext params;
        if (fname.equals("main")) { // 메인 함수인 경우
            symbolTable.putLocalVar("args", Type.INTARRAY);
            maincheck = true;
        } else if(fname.equals("printf")) {

        } else { // 메인 함수가 아닌 경우
            maincheck = false;
            symbolTable.putFunSpecStr(ctx);
            params = (MiniCParser.ParamsContext) ctx.getChild(3);
            symbolTable.putParams(params);
        }
    }

    // var_decl   : type_spec IDENT ';' | type_spec IDENT '=' LITERAL ';'|type_spec IDENT '[' LITERAL ']' ';'
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
        lo_count++; // local_decl 문장의 개수(변수 선언 개수)
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
        String programProlog = ".section .data\n" +
                "printf_format:\n\t.string \"%d\"\n"+
                ".section .text\n" +
                ".global main\n";
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

    // decl   : var_decl | fun_decl
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

    // stmt   : expr_stmt | compound_stmt | if_stmt | while_stmt | return_stmt
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

    // expr_stmt   : expr ';'
    @Override
    public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
        String stmt = "";
        if (ctx.getChildCount() == 2) {
            stmt += newTexts.get(ctx.expr());    // expr
        }
        newTexts.put(ctx, stmt);
    }


    // while_stmt   : WHILE '(' expr ')' stmt
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

        funDecl += "\tleaveq\n\tretq\n";
        newTexts.put(ctx, funDecl);
    }


    private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
        lo_count = lo_count * 4;
        int stackSize = lo_count; // 변수 하나의 stack 크기가 4이므로 4를 곱한다.
        int subSize = lo_count + 4; //함수 <<mov $edi,-0x08(%rsp)
        stackSize = ((stackSize + 15) / 16) * 16;  // 나머지 부분 제거 방식 16의 배수로 맞춤
        String mainStackAlloc = "\tsub \t$0x" + String.format("%02x", stackSize) + ",%rsp";
        String normalFuncStackAlloc = String.format("%02x", subSize);
        lo_count = 0;
        String funcProlog = "";
        if (maincheck == true) { //edi는 함수 부분
            funcProlog = symbolTable.getFunSpecStr(fname) + "\n"
                    + "\tpush \t%rbp" + "\n"
                    + "\tmov \t%rsp,%rbp" + "\n"
                    + mainStackAlloc + "\n";
        } else {
            funcProlog = "%edi";
            funcProlog = symbolTable.getFunSpecStr(fname) + "\n"
                    + "\tpush \t%rbp" + "\n"
                    + "\tmov \t%rsp,%rbp" + "\n"
                    + "\tmov \t$edi,-0x" + normalFuncStackAlloc + "(%rsp)\n";
        }
        return funcProlog;
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

    public String changeNum(String num) { // 숫자값에 해당하는 문자열을 16진수의 숫자값으로 변환 후 문자열로 반환
        int inputNum = Integer.parseInt(num);
        String changedNum = "0x" + String.format("%02x", inputNum);
        return changedNum;
    }

    @Override
    public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
        String varDecl = "";
        String initValue = "";
        String targetVar = "";
        if (isDeclWithInit(ctx)) {
            String vId = symbolTable.getVarId(ctx);
            targetVar = vId;
            String offset = "-" + changeNum(targetVar);
            String varAllocPosition = offset + "(%rbp)";
            initValue = "$" + changeNum(ctx.LITERAL().getText());
            varDecl += "movl \t" + initValue + "," + varAllocPosition + "\n";
        }
        newTexts.put(ctx, "\t"+varDecl);
    }

    // compound_stmt   : '{' local_decl* stmt* '}'
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

    // if_stmt   : IF '(' expr ')' stmt | IF '(' expr ')' stmt ELSE stmt;
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

    // return_stmt   : RETURN ';' | RETURN expr ';'
    @Override
    public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
        // <(4) Fill here>
        String returnStmt = "";

        String returnValue = ctx.expr().getText(); // return 대상의 변수명 또는 상수값
        String returnVarOffset = ""; // return 변수 offset

        if (maincheck == true)
            returnStmt += "\tmov \t$0x0,%eax\n";
        else {
            if (!isNumber(returnValue)) {
                returnVarOffset = "-" + changeNum(symbolTable.getVarId(returnValue));
                String addressOfReturnVar = returnVarOffset;
                returnStmt += "\tmov \t" + returnVarOffset + "(%rbp),%eax\n" +
                        "\tleaveq\n\tretq\n";
            } else {
                returnStmt += "\tmov \t" + returnValue + "\tpop $rbpq \n\tret\n";
            }
        }
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
            if (ctx.IDENT() != null) { // 변수인 경우
                String varName = ctx.IDENT().getText();
                if (symbolTable.getVarType(varName) == Type.INT) {
                    String varOffset = "-" + changeNum(symbolTable.getVarId(varName));

                    String firstAddNum = "", secondAddNum = "";
                    if (ctx.parent.getChild(2).getChild(0) != null) {
                        firstAddNum = ctx.parent.getChild(2).getChild(0).getText();
                    }
                    if (ctx.parent.getChild(0).getChild(0) != null) {
                        secondAddNum = ctx.parent.getChild(0).getChild(0).getText();
                    }
                    if (isNumber(firstAddNum) || isNumber(secondAddNum)) {
                        if(isNumber(secondAddNum) && ctx.parent.getChild(1).getText().equals("-")){ // - 연산자인경우
                            String firstNum = ctx.parent.getChild(0).getText(); // ex ) a = 3 - c 일때 3을 처리
                            firstNum = changeNum(firstNum);
                            expr += "\tmov \t$" + firstNum + "," + normalRegister[1] + " \n";
                            subcheck = true;
                        }
                        else{
                            expr += "\tmov \t" + varOffset + "(%rbp)," + normalRegister[1] + " \n";
                        }
                        registerCount = registerCount - 1;
                    } else {
                        if(ctx.parent.getChild(1).getText().equals("-")){ //parent.getchild의 첫번째 자식이 - 를 가지고있으면 -처리과정
                            System.out.println(ctx.parent.getChild(1).getText()); // add랑 edx와 eax위치를바꿈 그래서 register_sub 변수생성
                            expr += "\tmov \t" + varOffset + "(%rbp), " + normalRegister[register_sub] + " \n";
                            register_sub = register_sub -1;
                            if(register_sub == -1){
                                register_sub = 1;
                            }
                        }
                        else{
                            expr += "\tmov \t" + varOffset + "(%rbp), " + normalRegister[registerCount] + " \n";
                        }

                    }
                    registerCount = registerCount + 1;
                    if (registerCount == 2) {
                        registerCount = 0;
                    }
                }
            } else if (ctx.LITERAL() != null) { // LITERAL
                String literalStr = ctx.LITERAL().getText(); // 숫자 문자열
                String checkReturnStmt = "";
                String varOffset = "";
                Boolean terminalNodeCheck = false;
                checkReturnStmt = String.valueOf(ctx.parent.getChild(0));

                if (!(ctx.parent.getChild(0) instanceof MiniCParser.ExprContext)) {
                    if (!checkReturnStmt.equals("return")) { // ex) a = 4 처럼 하는 경우
                        varOffset = "-" + changeNum(symbolTable.getVarId(checkReturnStmt)) + "(%rbp)";
                    }
                } else { // 앞이 더 이상 전개가 안되는 경우 마킹
                    terminalNodeCheck = true;
                }

                literalStr = "$0x" + String.format("%02x", Integer.parseInt(literalStr));
                int addPairVarOffset = 0;
                expr += "\tmov \t" + literalStr + "," + varOffset + " \n";
                if (ctx.parent.getChild(2) != null) {// 상위 구조가 (상수 + 변수) 또는 (상수 + 상수)인 경우
                    if (ctx.parent.getChild(2).getChild(0) != null) { // (return 상수)의 경우를 걸러주어야함
                        String add_number = ctx.parent.getChild(2).getChild(0).getText();
                        if (terminalNodeCheck) {
                            if (isNumber(add_number)) {
                                addPairVarOffset = Integer.parseInt(add_number);
                            } else {
                                add_number = ctx.parent.getChild(0).getChild(0).getText();
                                addPairVarOffset = Integer.parseInt(add_number);
                            }
                            String addedValue = "$0x" + String.format("%02x", addPairVarOffset);
                            calc_temp_number = addedValue;
                            expr = "";
                        }
                    }
                    varOffset = "";
                }
            }
        } else if (ctx.getChildCount() == 2) { // UnaryOperation
            expr = handleUnaryExpr(ctx, newTexts.get(ctx) + expr);
        } else if (ctx.getChildCount() == 3) {
            if (ctx.getChild(0).getText().equals("(")) {        // '(' expr ')'
                expr = newTexts.get(ctx.expr(0));

            } else if (ctx.getChild(1).getText().equals("=")) {

                String targetVar = "";
                targetVar = symbolTable.getVarId(ctx.IDENT().getText());
                String targetVarOffset = changeNum(targetVar);
                if(ctx.expr().size() == 1 && isNumber(ctx.expr(0).getChild(0).getText())) // ex) (c = 상수)와 같은 단순 대입 연산 처리
                    expr = newTexts.get(ctx.expr(0)) + "\tmov \t%eax," + "-" + targetVarOffset + "(%rbp)\n";
                else
                    expr = newTexts.get(ctx.expr(0)) + "\tmov \t%eax," + "-" + targetVarOffset + "(%rbp)\n";

                String[] codeOfExpr = newTexts.get(ctx.expr(0)).split("\n");
                String[] wordsInCode = codeOfExpr[codeOfExpr.length - 1].split(" ");

                String moveCheck = newTexts.get(ctx.expr(0)).split(" ")[0];
                if (moveCheck.equals("movl \t")) {
                    expr = newTexts.get(ctx.expr(0));
                    if (wordsInCode[0].equals("callq")) {
                        String targetOffset = "-" + changeNum(symbolTable.getVarId(ctx.IDENT().getText()));
                        expr = newTexts.get(ctx.expr(0))
                                + "mov \t%eax,-" + targetOffset + "(%rbp)\n";
                    }
                }

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
        else { // Arrays: TODO         */
        }
        newTexts.put(ctx, expr);
    }

    public static boolean isNumber(String str) {
        boolean result = false;

        try {
            Double.parseDouble(str);
            result = true;
        } catch (Exception e) {
        }


        return result;
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
                if (calc_temp_number != "") {
                    expr += "\timul 1111 \t" + calc_temp_number + ",%eax \n";
                    calc_temp_number = ""; // 초기화
                } else {
                    expr += "\timul \t%edx,%eax \n";
                }
                break;
            case "/":
                if (calc_temp_number != "") {
                    expr += "\tdiv \t" + calc_temp_number + ",%eax \n";
                    calc_temp_number = ""; // 초기화
                } else {
                    expr += "\tdiv \t%edx,%eax \n";
                }
                break;
            case "%":
                expr += "irem \n";
                break;
            case "+":        // expr(0) expr(1) iadd
                if (calc_temp_number != "") {
                    expr += "\tadd \t" + calc_temp_number + ",%eax \n";
                    calc_temp_number = ""; // 초기화
                } else {
                    expr += "\tadd \t%edx,%eax \n";
                }
                break;
            case "-":
                if (subcheck == true){ //ex)   z = 9 - b  이면 b부분처리 ex ) sub -0x04(%rbp),%eax
                    String value = "";
                    value = ctx.parent.getChild(2).getChild(2).getChild(0).getText();
                    value = symbolTable.getVarId(value);
                    value = changeNum(value);
                    expr += "\tsub \t-" +value+"(%rbp)"+ ",%eax \n";
                    subcheck = false;
                    calc_temp_number = "";
                    break;
                }
                if (calc_temp_number != "") {
                    expr += "\tsub \t" + calc_temp_number + ",%eax \n";
                    calc_temp_number = ""; // 초기화
                } else {
                    expr += "\tsub \t %edx,%eax \n";
                }
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

        if (fname.equals("printf")) {        // System.out.println
            String varName = ctx.getChild(2).getChild(2).getText();
            String printVar = "-"+changeNum(symbolTable.getVarId(varName))+"(%rbp)";
            expr = "\tmovq \t$printf_format,%rdi\n" +
                    "\tmovq \t" + printVar + ",%rsi\n" +
                    "\tmovq \t$0x0,%rax\n" +
                    "\tcall \tprintf\n";
        } else {
            expr = newTexts.get(ctx.args())
                    + "\tcallq \t<" + fname + ">\n";
        }

        return expr;

    }

    // args   : expr (',' expr)* | ;
    @Override
    public void exitArgs(MiniCParser.ArgsContext ctx) {

        String argsStr = "";

        for (int i = 0; i < ctx.expr().size(); i++) {
            argsStr += newTexts.get(ctx.expr(i));
        }
        newTexts.put(ctx, argsStr);
    }

}