import java.util.HashMap;
public class SymbolTable {
    private HashMap<String, Integer> keywords;
    private HashMap<Integer, String> TokenToString;
    private HashMap<Integer, String> relOps;
    private HashMap<String, Integer> vars;

    private HashMap<String, Integer> strings;

    private HashMap<Integer, String> ids;
    private HashMap<Character, String> opcodeToString;
    private HashMap<Integer, Integer> constantToInstructionNum;
    private HashMap<String, Integer> identifierToInstructionNum;
    private HashMap<String, Integer> predefinedFunctions;

    SymbolTable(){
        keywords = new HashMap<>();
        TokenToString = new HashMap<>();
        relOps = new HashMap<>();
        vars = new HashMap<>();
        strings = new HashMap<>();
        ids = new HashMap<>();
        opcodeToString = new HashMap<>();
        constantToInstructionNum = new HashMap<>();
        identifierToInstructionNum = new HashMap<>();
        predefinedFunctions = new HashMap<>();

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

        opcodeToString.put('c', "const");
        opcodeToString.put('+', "add");
        opcodeToString.put('-', "sub");
        opcodeToString.put('*', "mul");
        opcodeToString.put('/', "div");

        predefinedFunctions.put("InputNum", Tokens.inputNum);
        predefinedFunctions.put("OutputNum", Tokens.outputNum);
        predefinedFunctions.put("OutputNewLine", Tokens.outputNewLine);
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
        vars.put(varName, val);
    }
    public String IdToString(int id){
        return ids.get(id);
    }
    public int StringToId(String s){
        return strings.get(s);
    }
    public int lookup(String key){
        return vars.get(key);
    }
    public String getOpcodeAsString(char key){
        return opcodeToString.get(key);
    }
    public boolean checkIfInstructionExists(int key){ // check if the constant is already defined in the symbol table. e.x. 0 : const #2
        return constantToInstructionNum.containsKey(key);
    }
    public int getInstructionForConstant(int key){ // gets the instructionNum mapped to the constant
        return constantToInstructionNum.get(key);
    }
    public void addConstantToInstructionNum(int key, int value){ // adds the instruction generate to the constant symbol table
        constantToInstructionNum.put(key, value);
    }
    public boolean checkIfIdentifierInstructionExists(String key){
        return identifierToInstructionNum.containsKey(key);
    }
    public int getInstructionForIdentifier(String key){
        return identifierToInstructionNum.get(key);
    }
    public void addIdentifierToInstructionNum(String key, int val){
        identifierToInstructionNum.put(key, val);
    }
    public boolean isPredefinedFunction(String key){
        return predefinedFunctions.containsKey(key);
    }
    public int getPredefinedToken(String key){
        return predefinedFunctions.get(key);
    }
    public HashMap<String, Integer> getIdentifierToInstructionNumHM(){
        return identifierToInstructionNum;
    }
    public void createDeepCopySymbolTable(HashMap<String, Integer> toCopy){
        this.identifierToInstructionNum = new HashMap<>(toCopy);
    }
}