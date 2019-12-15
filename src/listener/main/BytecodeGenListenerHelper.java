package listener.main;

import generated.MiniCParser;
import generated.MiniCParser.ExprContext;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.If_stmtContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.ParamContext;
import generated.MiniCParser.ParamsContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;
import listener.main.SymbolTable;
import listener.main.SymbolTable.VarInfo;

public class BytecodeGenListenerHelper {
	
	// <boolean functions>
	
	static boolean isFunDecl(MiniCParser.ProgramContext ctx, int i) {
		return ctx.getChild(i).getChild(0) instanceof MiniCParser.Fun_declContext;
	}

	// type_spec IDENT '[' ']'
	static boolean isArrayParamDecl(ParamContext param) {
		return param.getChildCount() == 4;
	}

	// global vars
	static int initVal(Var_declContext ctx) {
		return Integer.parseInt(ctx.LITERAL().getText());
	}

	// var_decl	: type_spec IDENT '=' LITERAL ';
	static boolean isDeclWithInit(Var_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}
	// var_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static boolean isArrayDecl(Var_declContext ctx) {
		return ctx.getChildCount() == 6;
	}

	// <local vars>
	// local_decl	: type_spec IDENT '[' LITERAL ']' ';'
	static int initVal(Local_declContext ctx) {
		return Integer.parseInt(ctx.LITERAL().getText());
	}

	static boolean isArrayDecl(Local_declContext ctx) {
		return ctx.getChildCount() == 6;
	}

	static boolean isDeclWithInit(Local_declContext ctx) {
		return ctx.getChildCount() == 5 ;
	}

	static boolean isVoidF(Fun_declContext ctx) {
			// <Fill in>
		return (ctx.type_spec().getChild(0).getText().equals("void")) ? true : false;
	}

	static boolean isIntReturn(MiniCParser.Return_stmtContext ctx) {
		return ctx.getChildCount() ==3;
	}


	static boolean isVoidReturn(MiniCParser.Return_stmtContext ctx) {
		return ctx.getChildCount() == 2;
	}

	// <information extraction>
	static String getStackSize(Fun_declContext ctx) {
		return "32";
	}
	static String getLocalVarSize(Fun_declContext ctx) {
		return "32";
	}
	static String getTypeText(Type_specContext typespec) {
			// <Fill in>
		return (typespec.getChild(0).getText().equals("int")) ? "I":"V";
	}

	// params
	static String getParamName(ParamContext param) {
		// <Fill in>
		return param.IDENT().getChild(0).getText();
	}

	static String getParamTypesText(ParamsContext params) {
		String typeText = "";

		for(int i = 0; i < params.param().size(); i++) {
			MiniCParser.Type_specContext typespec = (MiniCParser.Type_specContext)  params.param(i).getChild(0);
			typeText += getTypeText(typespec); // + ";";
		}
		return typeText;
	}
	
	static String getLocalVarName(Local_declContext local_decl) {
		// <Fill in>
		return local_decl.IDENT().getText();
	}
	
	static String getFunName(Fun_declContext ctx) {
		// <Fill in>
		return ctx.IDENT().getText();
	}
	
	static String getFunName(ExprContext ctx) {
		// <Fill in>
		// expr : IDENT '(' args ')' 인 점을 이용해서 text 추출
		if(ctx.getChildCount() == 4 && ctx.getChild(1).getText().equals("(") && ctx.getChild(3).getText().equals(")"))
			return ctx.getChild(0).getText();

		return null;
	}
	
	static boolean noElse(If_stmtContext ctx) {
		return ctx.getChildCount() <= 5;
	}
	
	static String getFunProlog() {
        String programProlog = ".section .data\n" +
                "printf_format:\n\t.string \"%d\\n\"\n"+
                ".section .text\n" +
                ".global main\n";
		return programProlog;
	}
	
	static String getCurrentClassName() {
		return "";
	}
}
