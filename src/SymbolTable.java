import java.util.HashMap;

public class SymbolTable {
    private HashMap<String, Integer> keywords;
    private HashMap<Integer, String> TokenToString;
    private HashMap<Integer, String> relOps;
    private HashMap<String, Integer> allOtherVars;

    private HashMap<String, Integer> strings;

    private HashMap<Integer, String> ids;
    SymbolTable(){
        keywords = new HashMap<>();
        TokenToString = new HashMap<>();
        relOps = new HashMap<>();
        allOtherVars = new HashMap<>();
        strings = new HashMap<>();
        ids = new HashMap<>();
        keywords.put("main", Tokens.mainToken);
        keywords.put("var", Tokens.varToken);
        keywords.put("then", Tokens.thenToken);
        keywords.put("do", Tokens.doToken);
        keywords.put("od", Tokens.odToken);
        keywords.put("fi", Tokens.fiToken);
        keywords.put("else", Tokens.elseToken);
        keywords.put("let", Tokens.letToken);
        keywords.put("call", Tokens.callToken);
        keywords.put("if", Tokens.ifToken);
        keywords.put("while", Tokens.whileToken);
        keywords.put("return", Tokens.returnToken);
        keywords.put("array", Tokens.arrToken);
        keywords.put("void", Tokens.voidToken);
        keywords.put("function", Tokens.funcToken);
        keywords.put("procedure", Tokens.procToken);

        TokenToString.put(Tokens.mainToken, "main" );
        TokenToString.put(Tokens.varToken, "var");
        TokenToString.put(Tokens.thenToken, "then");
        TokenToString.put(Tokens.doToken, "do");
        TokenToString.put(Tokens.odToken, "od");
        TokenToString.put(Tokens.fiToken, "fi");
        TokenToString.put(Tokens.elseToken, "else");
        TokenToString.put(Tokens.letToken, "let");
        TokenToString.put(Tokens.callToken, "call");
        TokenToString.put(Tokens.ifToken, "if");
        TokenToString.put(Tokens.whileToken, "while");
        TokenToString.put(Tokens.returnToken, "return");
        TokenToString.put(Tokens.arrToken, "array");
        TokenToString.put(Tokens.voidToken, "void");
        TokenToString.put(Tokens.funcToken, "function");
        TokenToString.put(Tokens.procToken, "procedure");

        relOps.put(Tokens.eqlToken, "==");
        relOps.put(Tokens.neqToken, "!=");
        relOps.put(Tokens.lssToken, "<");
        relOps.put(Tokens.leqToken, "<=");
        relOps.put(Tokens.gtrToken, ">");
        relOps.put(Tokens.geqToken, ">=");
    }
    public boolean isKeyword(String s){
        return keywords.containsKey(s);
    }
    public int getKeywordId(String s){
        return keywords.get(s);
    }
    public String getStringFromToken(int token) { return TokenToString.get(token); }
    public String getRelOp(int token) { return relOps.get(token); }
    public void insertToSymbolTable(String varName, Integer val){
        allOtherVars.put(varName, val);
    }
    public String IdToString(int id){
        return ids.get(id);
    }
    public int StringToId(String s){
        return strings.get(s);
    }
    public int lookup(String key){
        return allOtherVars.get(key);
    }
}