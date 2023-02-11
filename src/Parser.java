public class Parser {
    private Tokenizer myTokenizer;
    private int inputSym;
    private String seenIdent;
    public Parser(String filename) {
        myTokenizer = new Tokenizer(filename);
        next();
        computation();
    }

    private void next() {
        inputSym = myTokenizer.getNext();
    }

    public void checkFor(int token) {
        if (inputSym == token) next();
        else myTokenizer.Error("SyntaxErr, Expected '" + myTokenizer.symbolTable.getStringFromToken(token) + "' but missing");
    }
    public void computation(){
        checkFor(Tokens.mainToken);
        int val;
        while(inputSym == Tokens.varToken){
            next();
            checkFor(Tokens.ident);
            seenIdent = myTokenizer.getIdentifier();
            checkFor(Tokens.becomesToken);
            val = expression();
            myTokenizer.symbolTable.insertToSymbolTable(seenIdent, val);
            checkFor(Tokens.semiToken);
        }
        val = expression();
        System.out.println(val);
        while(inputSym == Tokens.semiToken){
            next();
            val = expression();
            System.out.println(val);
        }
        checkFor(Tokens.periodToken);
    }
    public int expression(){
        int val = term();
        while(inputSym == Tokens.plusToken || inputSym == Tokens.minusToken){
            if(inputSym == Tokens.plusToken){
                next();
                val += term();
            }
            else{
                next();
                val -= term();
            }
        }
        return val;
    }
    public int term(){
        int val = factor();
        while(inputSym == Tokens.timesToken || inputSym == Tokens.divToken){
            if(inputSym == Tokens.timesToken){
                next();
                val *= factor();
            }
            else{
                next();
                val /= factor();
            }
        }
        return val;
    }
    public int factor(){
        int val = 0;
        if(inputSym == Tokens.ident){
//            seenIdent = myTokenizer.getIdentifier();
//            val = myTokenizer.lookupSymbolTable(seenIdent);
//            next();
            designator();
        }
        else if(inputSym == Tokens.number){
            val = myTokenizer.getNumber();
            next();
        }
        else if(inputSym == Tokens.openParenToken){
            next();
            val = expression();
            checkFor(Tokens.closeParenToken);
        }
        // else if funcCall --- TO BE ADDED
        else{
            myTokenizer.Error("SyntaxErr");
        }
        return val;
    }
    public void designator(){
        //checkFor(Tokens.ident);
        seenIdent = myTokenizer.getIdentifier();
        while(inputSym == Tokens.openbracketToken){ // '['
            next(); // consume bracket
            int val = expression(); // get expression
            checkFor(Tokens.closebracketToken); // expected ']'
        }
    }
    public void relation(){
        int val1 = expression();
        if(inputSym == Tokens.eqlToken || inputSym == Tokens.neqToken || inputSym == Tokens.lssToken ||
           inputSym == Tokens.leqToken || inputSym == Tokens.gtrToken || inputSym == Tokens.geqToken){
            String relOp = myTokenizer.symbolTable.getRelOp(inputSym);
            next();
        }
        else{
            myTokenizer.Error("SyntaxErr. Missing relOp");
        }
        int val2 = expression();
    }
    public void assignment(){
        checkFor(Tokens.letToken);
        designator();

        checkFor(Tokens.becomesToken);
        int val = expression();
        seenIdent = myTokenizer.getIdentifier();
        myTokenizer.symbolTable.insertToSymbolTable(seenIdent, val);
    }
    public void statement(){

    }
    public void statSequence(){

    }
    public void typeDecl(){

    }
    public void varDecl(){

    }
    public void funcDecl(){

    }
    public void formalParam(){

    }
    public void funcBody(){

    }
}