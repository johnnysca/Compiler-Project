import java.io.*;

public class Tokenizer {
    private fileReader myFileReader;
    private char inputSym;
    private int number;
    private int id;
    private StringBuilder sb;
    SymbolTable symbolTable;

    public Tokenizer(String filename) {
        myFileReader = new fileReader(filename);
        sb = new StringBuilder();
        symbolTable = new SymbolTable();
        next();
    }

    private void next() {
        inputSym = myFileReader.getNext();
    }

    public int getNext() {
        while (inputSym == ' ' || inputSym == '\n') next();
        if (inputSym == Tokens.eofToken) return Tokens.eofToken;
        else {
            switch (inputSym) {
                case '+':
                    next();
                    //id = Tokens.plusToken;
                    return Tokens.plusToken;
                case '-':
                    next();
                    //id = Tokens.minusToken;
                    return Tokens.minusToken;
                case '*':
                    next();
                    //id = Tokens.timesToken;
                    return Tokens.timesToken;
                case '/':
                    next();
                    //id = Tokens.divToken;
                    return Tokens.divToken;
                case '(':
                    next();
                    //id = Tokens.openParenToken;
                    return Tokens.openParenToken;
                case ')':
                    next();
                    //id = Tokens.closeParenToken;
                    return Tokens.closeParenToken;
                case '<':
                    next();
                    if (inputSym == '-') {
                        next();
                        //id = Tokens.becomesToken;
                        return Tokens.becomesToken;
                    }
                    else if(inputSym == '='){
                        next();
                        //id = Tokens.leqToken;
                        return Tokens.leqToken;
                    }
                    else{
                        //id = Tokens.lssToken;
                        return Tokens.lssToken;
                    }
                case '>':
                    next();
                    if(inputSym == '='){
                        next();
                        //id = Tokens.geqToken;
                        return Tokens.geqToken;
                    }
                    else{
                        //id = Tokens.gtrToken;
                        return Tokens.gtrToken;
                    }
                case '=':
                    next();
                    if(inputSym == '='){
                        next();
                        //id = Tokens.eqlToken;
                        return Tokens.eqlToken;
                    }
                case '!':
                    next();
                    if(inputSym == '='){
                        next();
                        //id = Tokens.neqToken;
                        return Tokens.neqToken;
                    }
                case ';':
                    next();
                    //id = Tokens.semiToken;
                    return Tokens.semiToken;
                case '.':
                    next();
                    //id = Tokens.periodToken;
                    return Tokens.periodToken;
                case ',':
                    next();
                    //id = Tokens.commaToken;
                    return Tokens.commaToken;
                case '[':
                    next();
                    //id = Tokens.openbracketToken;
                    return Tokens.openbracketToken;
                case ']':
                    next();
                    //id = Tokens.closebracketToken;
                    return Tokens.closebracketToken;
                case '}':
                    next();
                    //id = Tokens.endToken;
                    return Tokens.endToken;
                case '{':
                    next();
                    //id = Tokens.beginToken;
                    return Tokens.beginToken;
                default:
                    if (isDigit()) {
                        number();
                        //id = Tokens.number;
                        return Tokens.number;
                    }
                    else if (isAlpha()) {
                        alpha();
                        if (symbolTable.isKeyword(sb.toString())) { // if its a keyword return the Token matched with the keyword
                            //id = symbolTable.getKeywordId(sb.toString());
                            return symbolTable.getKeywordId(sb.toString());
                        }
                        id = Tokens.ident;
                        return Tokens.ident;
                    }
                    else {
                        //myFileReader.Error("Unknown character");
                        //id = Tokens.eofToken;
                        return Tokens.eofToken;
                    }
            }
        }
    }
    void number(){
        number = inputSym - '0';
        next();
        while(isDigit()){
            number = number * 10 + (inputSym - '0');
            next();
        }
    }
    void alpha(){
        sb.setLength(0);
        sb.append(inputSym);
        next();
        while(isAlpha() || isDigit()){
            sb.append(inputSym);
            next();
        }
    }
    boolean isDigit(){
        return inputSym >= '0' && inputSym <= '9';
    }
    boolean isAlpha(){
        return inputSym >= 'a' && inputSym <= 'z';
    }
    public String getIdentifier(){
        return sb.toString();
    }
    public void Error(String errorMsg){
        System.out.println(errorMsg);
    }
    public char getInputSym(){
        return inputSym;
    }
    public int getNumber(){
        return number;
    }
    public int getId(){
        return id;
    }
    public int lookupSymbolTable(String key){
        return symbolTable.lookup(key);
    }
}