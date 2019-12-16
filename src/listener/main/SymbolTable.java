package listener.main;

import java.util.HashMap;
import java.util.Map;

import generated.MiniCParser;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.Type_specContext;
import generated.MiniCParser.Var_declContext;

import static listener.main.BytecodeGenListenerHelper.getFunName;
import static listener.main.BytecodeGenListenerHelper.getParamTypesText;
import static listener.main.BytecodeGenListenerHelper.getTypeText;
import static listener.main.BytecodeGenListenerHelper.isVoidF;


public class SymbolTable {
    enum Type {
        INT, INTARRAY, VOID, ERROR
    }

    static public class VarInfo {
        Type type;
        int id;
        int initVal;

        public VarInfo(Type type, int id, int initVal) {
            this.type = type;
            this.id = id;
            this.initVal = initVal;
        }

        public VarInfo(Type type, int id) {
            this.type = type;
            this.id = id;
            this.initVal = 0;
        }
    }

    static public class FInfo {
        public String sigStr;
    }

    private Map<String, VarInfo> _lsymtable = new HashMap<>();    // local v.
    private Map<String, VarInfo> _gsymtable = new HashMap<>();    // global v.
    private Map<String, FInfo> _fsymtable = new HashMap<>();    // function

    private int _localVarOffset = 0;
    private int _globalVarID = 0;
    private int _localVarID = 0;
    private int _labelID = 0;
    private int _tempVarID = 0;

    public int get_localVarOffset() {
        return _localVarOffset;
    }

    public void set_localVarOffset(int _localVarOffset) {
        this._localVarOffset = _localVarOffset;
    }
    SymbolTable() {
        initFunDecl();
        initFunTable();
    }

    void initFunDecl() {        // at each func decl
        _lsymtable = new HashMap<>(); // 버그 수정
        _localVarID = 0;
        _labelID = 0;
        _tempVarID = 32;
    }

    void putLocalVar(String varname, Type type) {
        //<Fill here>
        VarInfo localVarInfo = new VarInfo(type,_localVarOffset );
        _localVarOffset -= 4;
        if(varname == "args")
            _localVarOffset += 4;
        _lsymtable.put(varname, localVarInfo);
    }

    void putGlobalVar(String varname, Type type) {
        //<Fill here>
        VarInfo glovalVarInfo = new VarInfo(type, _globalVarID++);
        _gsymtable.put(varname, glovalVarInfo);
    }

    void putLocalVarWithInitVal(String varname, Type type, int initVar) {
        //<Fill here>
//        System.out.println(_localVarID+" putLocalVarWithInitVal "+varname +"_localVarOffset " +_localVarOffset);
        VarInfo initLocalVarInfo = new VarInfo(type, _localVarOffset, initVar);
//        System.out.println(_localVarOffset);
        _localVarOffset -= 4;
        _lsymtable.put(varname, initLocalVarInfo);
    }

    void putGlobalVarWithInitVal(String varname, Type type, int initVar) {
        //<Fill here>
        VarInfo initGloablVarInfo = new VarInfo(type, _globalVarID++, initVar);
        _gsymtable.put(varname, initGloablVarInfo);
    }

    void putParams(MiniCParser.ParamsContext params) {
        for (int i = 0; i < params.param().size(); i++) {
            //<Fill here>
            MiniCParser.ParamContext paramCtx = params.param(i);
            Type_specContext paramType = paramCtx.type_spec();
            String paramVarType = getTypeText(paramType);
            String paramName = paramCtx.IDENT().getText();

            if (paramVarType.equals("I"))
                putLocalVar(paramName, Type.INT);
            else if(paramVarType.equals("V"))
                putLocalVar(paramName, Type.VOID);
        }
    }

    private void initFunTable() {
        FInfo printlninfo = new FInfo();
        printlninfo.sigStr = "printf";

        FInfo maininfo = new FInfo();
        maininfo.sigStr = "main :";
        _fsymtable.put("printf", printlninfo);
        _fsymtable.put("main", maininfo);
    }

    public String getFunSpecStr(String fname) {
        // <Fill here>
        return _fsymtable.get(fname).sigStr;
    }

    public String getFunSpecStr(Fun_declContext ctx) {
        // <Fill here>
        return getFunSpecStr(getFunName(ctx));
    }

    public String putFunSpecStr(Fun_declContext ctx) {
        String fname = getFunName(ctx);
        String argtype = "";
        String rtype = "";
        String res = "";

        // <Fill here>
        argtype += getParamTypesText(ctx.params());
        if (!isVoidF(ctx))
            rtype += "I";
        res += fname +":" ;

        FInfo finfo = new FInfo();
        finfo.sigStr = res;
        _fsymtable.put(fname, finfo);

        return res;
    }

    String getVarId(String name) {
        // <Fill here>
        if (!(_gsymtable.get(name) == null)) {
            return Integer.toString(_gsymtable.get(name).id);

        }

        return Integer.toString(_lsymtable.get(name).id);
    }

    Type getVarType(String name) {
        VarInfo lvar = (VarInfo) _lsymtable.get(name);
        if (lvar != null) {
            return lvar.type;
        }

        VarInfo gvar = (VarInfo) _gsymtable.get(name);
        if (gvar != null) {
            return gvar.type;
        }

        return Type.ERROR;
    }

    String newLabel() {
        return "label" + _labelID++;
    }

    String newTempVar() {
        String id = "";
        return id + _tempVarID--; // ??
    }

    // global
    public String getVarId(Var_declContext ctx) {
        // <Fill here>
        String sname = "";
        sname += getVarId(ctx.IDENT().getText());
        return sname;
    }

    // local
    public String getVarId(Local_declContext ctx) {
        String sname = "";
        sname += getVarId(ctx.IDENT().getText());
        return sname;
    }

}