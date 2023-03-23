import java.sql.SQLOutput;
import java.util.*;
public class Parser {
    private Tokenizer myTokenizer;
    private int inputSym;
    private String seenIdent; // store identifier just seen
    private int val; // store value just seen
    private int instructionNum; // to track what instruction number this new instruction will be
    private BasicBlock bb; // current Basic Block obj
    private int basicBlockNum; // current Basic Block number
    public HashMap<Integer, List<Integer>> BBMapping; // key: Basic Block Num, val: list of children Basic Blocks
    public HashMap<Integer, BasicBlock> BBS; // key: Basic Block Num, val: actual Basic Block
    private Scanner scanner;
    private Stack<BasicBlock> ifBBS; // tracks if blocks to handle nesting
    private Stack<BasicBlock> joinBBS; // track join blocks to handle nesting
    private BasicBlock lastSeenJoinBB; // tracks last join block for merging with other joins if needed
    private int joinBlocksNeeded; // track how many join blocks we need, 1 per if
    private List<BasicBlock> whileBBS; // track while blocks to handle nesting
    private HashMap<Integer, Integer> updateMap; // keys will be old instruction num which will be replaced with value instruction nums
    private BasicBlock lastSeenFollowBB; // track last seen follow to make branching to while header
    private boolean done; // for Replacement Algorithm to stop
    private BasicBlock lastSeenBB;
    // MAKE GLOBAL RIGHT INSTRUCTION FOR ARRAYS JUST INCASE THERES CSE FOR STORES
    private int RIGHT;
    private List<Integer> dimension; // dimensions of array
    private boolean isArray; // track if ident seen is an array
    private List<Integer> indices; // tracks indices array is referencing to make muls and adds needed
    private boolean LHS; // flag for if we are on the LHS of becomes token
    private String arrName;
    private boolean LHSIsArray;
    public Parser(String filename) {
        myTokenizer = new Tokenizer(filename);
        BBMapping = new HashMap<>();
        BBS = new HashMap<>();
        BBS.put(0, new BasicBlock(0));
        List<Integer> list = new ArrayList<>();
        list.add(1);
        BBMapping.put(0, list);
        instructionNum = 0;
        basicBlockNum = 1; // constants will automatically be added to BB0 guaranteed so start from BB1
        bb = new BasicBlock(basicBlockNum);
        BBS.get(0).setLeftBasicBlock(bb);
        scanner = new Scanner(System.in);
        ifBBS = new Stack<>();
        joinBBS = new Stack<>();
        whileBBS = new Stack<>();
        updateMap = new HashMap<>();
        dimension = new ArrayList<>();
        indices = new ArrayList<>();
        next();
        computation();
    }

    private void next() {
        inputSym = myTokenizer.getNext();
    }

    public void checkFor(int token) {
        if (inputSym == token) next();
        else myTokenizer.Error("SyntaxErr, Expected '" + token + "' but missing");
    }
    public void computation(){
        BBS.put(basicBlockNum, bb);
        checkFor(Tokens.mainToken);
        while(inputSym == Tokens.varToken || inputSym == Tokens.arrToken){
            varDecl();
        }
        checkFor(Tokens.beginToken);
        statSequence();
        checkFor(Tokens.endToken);
        checkFor(Tokens.periodToken);
        bb.addStatement(new Instruction(instructionNum, "end", -1, -1));
        if(lastSeenBB != null){
            lastSeenBB.getStatements().get(lastSeenBB.getStatements().size() - 1).setRightInstruction(bb.getStatements().get(0).getInstructionNum());
        }
        instructionNum++;
    }
    public void varDecl(){
        typeDecl();
        if(inputSym == Tokens.ident){
            seenIdent = myTokenizer.getIdentifier();
            // IF SEENIDENT IS ARRAY BASED ON FLAG CREATE CREATE VAR_ADDR IN BB0 IF ARRAY INSERT TO ARRAY SYM TABLE
            if(isArray){
                BBS.get(0).addStatement(new Instruction(instructionNum, "const", seenIdent + "_addr"));
                bb.getSymbolTable().arrayToDimensions.put(seenIdent, dimension);
                bb.getSymbolTable().arrayToAddr.put(seenIdent, instructionNum);
                instructionNum++;
            }
            else
                myTokenizer.symbolTable.insertToSymbolTable(seenIdent, 0);
            next();
        }
        while(inputSym == Tokens.commaToken){
            next();
            seenIdent = myTokenizer.getIdentifier();
            if(isArray){
                BBS.get(0).addStatement(new Instruction(instructionNum, "const", seenIdent + "_addr"));
                bb.getSymbolTable().arrayToDimensions.put(seenIdent, dimension);
                bb.getSymbolTable().arrayToAddr.put(seenIdent, instructionNum);
                instructionNum++;
            }
            else
                myTokenizer.symbolTable.insertToSymbolTable(seenIdent, 0);
            next();
        }
        checkFor(Tokens.semiToken);
    }
    public void typeDecl(){
        if(inputSym == Tokens.varToken){
            isArray = false;
            next();
        }
        else if(inputSym == Tokens.arrToken){
            isArray = true;
            dimension = new ArrayList<>();
            next();
            // create const 4 and assign to SIZE var
            if(!BBS.get(0).constantinstructionExists(4)){
                BBS.get(0).addConstantToSymbolTable(4, instructionNum);
                BBS.get(0).addStatement(new Instruction(instructionNum, "const", 4));
                instructionNum++;
                BBS.get(0).addStatement(new Instruction(instructionNum, "const", "BASE"));
                instructionNum++;
            }
            checkFor(Tokens.openbracketToken);
            if(inputSym == Tokens.number){ // MAYBE INSTEAD CALL FACTOR
                val = myTokenizer.getNumber();
                if(!BBS.get(0).constantinstructionExists(val)){
                    BBS.get(0).addConstantToSymbolTable(val, instructionNum);
                    BBS.get(0).addStatement(new Instruction(instructionNum, "const", val));
                    instructionNum++;
                }
                dimension.add(BBS.get(0).getConstantInstructionNum(val));
                next();
            }
            checkFor(Tokens.closebracketToken);
            while(inputSym == Tokens.openbracketToken){
                next();
                val = myTokenizer.getNumber();
                if(!BBS.get(0).constantinstructionExists(val)){
                    BBS.get(0).addConstantToSymbolTable(val, instructionNum);
                    BBS.get(0).addStatement(new Instruction(instructionNum, "const", val));
                    instructionNum++;
                }
                dimension.add(BBS.get(0).getConstantInstructionNum(val));
                next();
                checkFor(Tokens.closebracketToken);
            }
            // SET FLAG FOR ARRAY TO TRUE
            // CREATE BASE FOR THE ARRAY
        }
        else{
            myTokenizer.Error("Expected var or array declaration");
        }
    }
    public void statSequence(){
        statement();
        while(inputSym == Tokens.semiToken){
            next(); // consume semi
            statement();
        }
        if(inputSym == Tokens.semiToken) next(); // optional ;
    }
    public void statement(){
        if(inputSym == Tokens.letToken){
            assignment();
        }
        else if(inputSym == Tokens.callToken){
            funcCall();
        }
        else if(inputSym == Tokens.ifToken){
            ifStatement();
            bb = lastSeenJoinBB;
        }
        else if(inputSym == Tokens.whileToken){
            whileStatement();
        }
        else if(inputSym == Tokens.returnToken){
            returnStatement();
        }
    }
    public void assignment(){
        next(); // eat let
        int leftInstruction = -1, rightInstruction = -1, rootInstruction = -1;
        if(inputSym == Tokens.ident) {
            System.out.println("going to designator");
            LHS = true;
            Node desNode = designator(); // designator node will contain LHS of <-
            if(desNode.adda == -1) LHSIsArray = false;
            else LHSIsArray = true;
            System.out.println("back from designator");
            System.out.println(desNode.data + " " + desNode.adda);
            checkFor(Tokens.becomesToken);

            System.out.println("going to expression");
            LHS = false;
            Node expr = expression(); // will contain a tree of the expression we saw
            System.out.println("back from expression");
            if(desNode.adda != -1 && expr == null){
                //Instruction store = new Instruction(instructionNum, "store", desNode.adda, instructionNum - 1);
                Instruction store = new Instruction(instructionNum, "store", instructionNum - 1, desNode.adda);
                bb.getSymbolTable().arrayKills.put((String) desNode.data, -1); // need kill if in an if or while
                bb.addStatement(store);
                bb.getSymbolTable().hasStores.put((String) desNode.data, instructionNum);
                instructionNum++;
                return;
            }
            if(desNode.adda == -1 && expr == null && whileBBS.size() != 0){
                BasicBlock whileBB = whileBBS.get(whileBBS.size() - 1);
                if(!whileBB.containsPhi((String) desNode.data)){
                    if(!whileBB.identifierInstructionExists((String) desNode.data)){
                        if(!BBS.get(0).constantinstructionExists(0)){
                            BBS.get(0).addStatement(new Instruction(instructionNum, "const", 0));
                            BBS.get(0).addConstantToSymbolTable(0, instructionNum);
                            instructionNum++;
                        }
                        leftInstruction = BBS.get(0).getConstantInstructionNum(0);
                    }
                    else{
                        leftInstruction = whileBB.getIdentifierInstructionNum((String) expr.data);
                        for (int i = whileBBS.size() - 1; i >= 0; i--) {
                            boolean found = false;
                            if (whileBBS.get(i).containsPhi((String) expr.data)) {
                                int instr = whileBBS.get(i).getIdentifierInstructionNum((String) expr.data);
                                for (int j = 0; j < whileBBS.get(i).numPhis; j++) {
                                    if (whileBBS.get(i).getStatements().get(j).getInstructionNum() == instr) {
                                        if (whileBBS.get(i).getStatements().get(j).getRightInstruction() == -1) {
                                            leftInstruction = whileBBS.get(i).getStatements().get(j).getInstructionNum();
                                        } else leftInstruction = whileBBS.get(i).getStatements().get(j).getRightInstruction();
                                        found = true;
                                    }
                                    if(found) break;
                                }
                            }
                            if(found) break;
                        }
                    }
                    System.out.println("for phiiiiii " + leftInstruction);
                    whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", leftInstruction, instructionNum - 1));
                    whileBB.addToPhiTable((String) desNode.data, instructionNum);
                    whileBB.addIdentifierToSymbolTable((String) desNode.data, instructionNum);
                    instructionNum++;
                    whileBB.numPhis++;

                }
                else leftInstruction = whileBB.getIdentifierInstructionNum((String) desNode.data);
            }
            if(expr == null) return;
            System.out.println(expr.data);
            if (expr.left != null)
                System.out.println(expr.left.data);
            else System.out.println(expr.left);
            if (expr.right != null)
                System.out.println(expr.right.data);
            else System.out.println(expr.right);

            // CHECK IF LHS WAS AN ARRAY, IF YES GEN STORE INSTRUCTION WITH LEFT AS ADDA WHICH IS IN DESNODE AND RIGHT AS INSTRUCTION NUM-1
            if (!expr.constant) { // root not a constant meaning its a +, -, *, / or identifier. constants would have already generated their instruction in factor
                System.out.println("root not a constant " + expr.data);
                if (expr.left == null) { // if the root is an identifier
                    System.out.println("left null");
                    System.out.println(expr.data + " " + expr.adda);
                    //if (bb.isWhile) { // if came from while block structure create a phi in while header
                    if(whileBBS.size() != 0 && expr.adda == -1){
                        BasicBlock whileBB = whileBBS.get(whileBBS.size() - 1);
                        if(!whileBB.containsPhi((String) expr.data)) {
                            // have to search all previous whileBBSS to see if theres a phi and use the phis right instruction if not == -1 else use the instructionNum of that phi
                            leftInstruction = whileBBS.get(whileBBS.size() - 1).getIdentifierInstructionNum((String) expr.data);
                            for (int i = whileBBS.size() - 1; i >= 0; i--) {
                                boolean found = false;
                                if (whileBBS.get(i).containsPhi((String) expr.data)) {
                                    int instr = whileBBS.get(i).getIdentifierInstructionNum((String) expr.data);
                                    for (int j = 0; j < whileBBS.get(i).numPhis; j++) {
                                        if (whileBBS.get(i).getStatements().get(j).getInstructionNum() == instr) {
                                            if (whileBBS.get(i).getStatements().get(j).getRightInstruction() == -1) {
                                                leftInstruction = whileBBS.get(i).getStatements().get(j).getInstructionNum();
                                            } else leftInstruction = whileBBS.get(i).getStatements().get(j).getRightInstruction();
                                            found = true;
                                        }
                                        if(found) break;
                                    }
                                }
                                if(found) break;
                            }
                            System.out.println("left in while right " + leftInstruction);
                            //whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", whileBB.getIdentifierInstructionNum((String) rel.right.data), -1));
                            whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", leftInstruction, -1));
                            whileBB.addToPhiTable((String) expr.data, instructionNum);
                            whileBB.addIdentifierToSymbolTable((String) expr.data, instructionNum);
                            instructionNum++;
                            whileBB.numPhis++;
                        }
                        else leftInstruction = whileBB.getIdentifierInstructionNum((String) expr.data);
                    }
                    else if(whileBBS.size() != 0 && expr.adda != -1){
                        if(desNode.adda == -1){ // desNode is an ident
                            BasicBlock whileBB = whileBBS.get(whileBBS.size() - 1);
                            if(!whileBB.containsPhi((String) desNode.data)) {
                                // have to search all previous whileBBSS to see if theres a phi and use the phis right instruction if not == -1 else use the instructionNum of that phi
                                leftInstruction = whileBBS.get(whileBBS.size() - 1).getIdentifierInstructionNum((String) desNode.data);
                                for (int i = whileBBS.size() - 1; i >= 0; i--) {
                                    boolean found = false;
                                    if (whileBBS.get(i).containsPhi((String) expr.data)) {
                                        int instr = whileBBS.get(i).getIdentifierInstructionNum((String) desNode.data);
                                        for (int j = 0; j < whileBBS.get(i).numPhis; j++) {
                                            if (whileBBS.get(i).getStatements().get(j).getInstructionNum() == instr) {
                                                if (whileBBS.get(i).getStatements().get(j).getRightInstruction() == -1) {
                                                    leftInstruction = whileBBS.get(i).getStatements().get(j).getInstructionNum();
                                                } else leftInstruction = whileBBS.get(i).getStatements().get(j).getRightInstruction();
                                                found = true;
                                            }
                                            if(found) break;
                                        }
                                    }
                                    if(found) break;
                                }
                                whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", leftInstruction, expr.adda));
                                whileBB.addToPhiTable((String) desNode.data, instructionNum);
                                whileBB.addIdentifierToSymbolTable((String) desNode.data, instructionNum);
                                instructionNum++;
                                whileBB.numPhis++;
                            }
                            else leftInstruction = whileBB.getIdentifierInstructionNum((String) desNode.data);
                        }
                        else{ // its array so create store
                            //Instruction store = new Instruction(instructionNum, "store", desNode.adda, expr.adda);
                            Instruction store = new Instruction(instructionNum, "store", expr.adda, desNode.adda);
                            bb.getSymbolTable().arrayKills.put((String) desNode.data, -1);
                            bb.getSymbolTable().hasStores.put((String) desNode.data, instructionNum);
                            bb.addStatement(store);
                            bb.addOpInstruction("load", store);
                            instructionNum++;
                        }
                        return;
                    }
                    else { // didnt come from a while header so create as normal in current bb
                        if (bb.containsPhi((String) desNode.data)) return;
                        System.out.println("no left expr " + expr.left);
                        if (!bb.identifierInstructionExists((String) expr.data)) {
                            if (!BBS.get(0).constantinstructionExists(0)) {
                                System.out.println("No value was assigned to variable. Will be defaulted to 0");
                                BBS.get(0).addConstantToSymbolTable(0, instructionNum);
                                BBS.get(0).addStatement(new Instruction(instructionNum, bb.getOpCode('c'), 0)); // c just means constant
                                instructionNum++;
                            }
                            bb.addIdentifierToSymbolTable((String) desNode.data, BBS.get(0).getConstantInstructionNum(0));
                        }
                        else {
                            rootInstruction = bb.getIdentifierInstructionNum((String) expr.data);
                            System.out.println("root instr " + rootInstruction);
                            if(desNode.adda == -1)
                                bb.addIdentifierToSymbolTable((String) desNode.data, rootInstruction);
                        }
                        RIGHT = expr.constant ? BBS.get(0).getConstantInstructionNum((int) expr.data) : bb.getIdentifierInstructionNum((String) expr.data);
                        System.out.println("RIGHT " + desNode.adda + " " + RIGHT);
                        if(desNode.adda != -1){
                            System.out.println("here in not -1");
                            //Instruction store = new Instruction(instructionNum, "store", desNode.adda, RIGHT);
                            Instruction store = new Instruction(instructionNum, "store", RIGHT, desNode.adda);
                            // mark that desNode.data has a store instruction for CSE later of recreating a new load
                            bb.getSymbolTable().arrayKills.put((String) desNode.data, -1);
                            bb.getSymbolTable().hasStores.put((String) desNode.data, instructionNum);
                            bb.addStatement(store);
                            bb.addOpInstruction("load", store);
                            instructionNum++;
                        }
                        return;
                    }
                    RIGHT = expr.constant ? BBS.get(0).getConstantInstructionNum((int) expr.data) : bb.getIdentifierInstructionNum((String) expr.data);
                    if(desNode.adda != -1){
                        //Instruction store = new Instruction(instructionNum, "store", desNode.adda, RIGHT);
                        Instruction store = new Instruction(instructionNum, "store", RIGHT, desNode.adda);
                        bb.getSymbolTable().arrayKills.put((String) desNode.data, -1);
                        bb.getSymbolTable().hasStores.put((String) desNode.data, instructionNum);
                        bb.addStatement(store);
                        bb.addOpInstruction("load", store);
                        instructionNum++;
                    }
                    return;
                }
                if (!expr.left.constant) { // if its an identifier check to see if an instruction was previously generated
                    System.out.println("left not a constant " + expr.left.data);
                    //if (bb.isWhile) { // came from a while header
                    if(whileBBS.size() != 0){
                        if(expr.left.adda != -1){
                            leftInstruction = expr.adda;
                        }
                        else {
                            BasicBlock whileBB = whileBBS.get(whileBBS.size() - 1);
                            if (!whileBB.containsPhi((String) expr.left.data)) {
                                // have to search all previous whileBBSS to see if theres a phi and use the phis right instruction if not == -1 else use the instructionNum of that phi
                                if(bb.identifierInstructionExists((String) expr.left.data)){
                                    leftInstruction = bb.getIdentifierInstructionNum((String) expr.left.data);
                                }
                                //leftInstruction = whileBBS.get(whileBBS.size() - 1).getIdentifierInstructionNum((String) expr.left.data);
                                for (int i = whileBBS.size() - 1; i >= 0; i--) {
                                    boolean found = false;
                                    if (whileBBS.get(i).containsPhi((String) expr.left.data)) {
                                        int instr = whileBBS.get(i).getIdentifierInstructionNum((String) expr.left.data);
                                        for (int j = 0; j < whileBBS.get(i).numPhis; j++) {
                                            if (whileBBS.get(i).getStatements().get(j).getInstructionNum() == instr) {
                                                if (whileBBS.get(i).getStatements().get(j).getRightInstruction() == -1) {
                                                    leftInstruction = whileBBS.get(i).getStatements().get(j).getInstructionNum();
                                                } else
                                                    leftInstruction = whileBBS.get(i).getStatements().get(j).getRightInstruction();
                                                found = true;
                                            }
                                            if (found) break;
                                        }
                                    }
                                    if (found) break;
                                }
                                System.out.println("left in while right " + leftInstruction);
                                //whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", whileBB.getIdentifierInstructionNum((String) rel.right.data), -1));
                                whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", leftInstruction, -1));
                                whileBB.addToPhiTable((String) expr.left.data, instructionNum);
                                whileBB.addIdentifierToSymbolTable((String) expr.left.data, instructionNum);
                                instructionNum++;
                                whileBB.numPhis++;
                            }
                            leftInstruction = whileBB.getIdentifierInstructionNum((String) expr.left.data);
                        }
                    } else { // didnt come from while header
                        // IF EXPR.LEFT IS AN ARRAY SET LEFT INSTRUCTION AS WHATEVER IS IN THE ADDA FIELD AND BREAK
                        if(expr.left.adda != -1){
                            leftInstruction = expr.left.adda;
                        }
                        else if (!bb.identifierInstructionExists((String) expr.left.data)) { // identifier used before it was assigned a value default to 0
                            System.out.println("left has no identifier instruction");
                            if (!BBS.get(0).constantinstructionExists(0)) { // default val to 0
                                System.out.println("No value was assigned to variable. Will be defaulted to 0");
                                BBS.get(0).addConstantToSymbolTable(0, instructionNum);
                                BBS.get(0).addStatement(new Instruction(instructionNum, bb.getOpCode('c'), 0)); // c just means constant
                                instructionNum++;
                            }
                            bb.addIdentifierToSymbolTable((String) expr.left.data, BBS.get(0).getConstantInstructionNum(0));
                            leftInstruction = bb.getIdentifierInstructionNum((String) expr.left.data);
                        }
                        else {
                            System.out.println("left has instruction for ident");
                            leftInstruction = bb.getIdentifierInstructionNum((String) expr.left.data);
                            System.out.println("done getting left instruction");
                        }
                    }
                }
                else {
                    System.out.println("left is a constant");
                    leftInstruction = BBS.get(0).getConstantInstructionNum((int) expr.left.data); // left side is a constant
                    System.out.println("done with left constant");
                }
                if (!expr.right.constant) { // right is an identifier
                    System.out.println("right not a constant " + expr.right.data);
                    //if (bb.isWhile) {
                    if(whileBBS.size() != 0){
                        if(expr.right.adda != -1){
                            leftInstruction = expr.right.adda;
                        }
                        else {
                            BasicBlock whileBB = whileBBS.get(whileBBS.size() - 1);
                            if (!whileBB.containsPhi((String) expr.right.data)) {
                                // have to search all previous whileBBSS to see if theres a phi and use the phis right instruction if not == -1 else use the instructionNum of that phi
                                leftInstruction = whileBBS.get(whileBBS.size() - 1).getIdentifierInstructionNum((String) expr.right.data);
                                for (int i = whileBBS.size() - 1; i >= 0; i--) {
                                    boolean found = false;
                                    if (whileBBS.get(i).containsPhi((String) expr.right.data)) {
                                        int instr = whileBBS.get(i).getIdentifierInstructionNum((String) expr.right.data);
                                        for (int j = 0; j < whileBBS.get(i).numPhis; j++) {
                                            if (whileBBS.get(i).getStatements().get(j).getInstructionNum() == instr) {
                                                if (whileBBS.get(i).getStatements().get(j).getRightInstruction() == -1) {
                                                    leftInstruction = whileBBS.get(i).getStatements().get(j).getInstructionNum();
                                                } else
                                                    leftInstruction = whileBBS.get(i).getStatements().get(j).getRightInstruction();
                                                found = true;
                                            }
                                            if (found) break;
                                        }
                                    }
                                    if (found) break;
                                }
                                System.out.println("left in while right " + leftInstruction);
                                //whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", whileBB.getIdentifierInstructionNum((String) rel.right.data), -1));
                                whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", leftInstruction, -1));
                                whileBB.addToPhiTable((String) expr.right.data, instructionNum);
                                whileBB.addIdentifierToSymbolTable((String) expr.right.data, instructionNum);
                                instructionNum++;
                                whileBB.numPhis++;
                            }
                            rightInstruction = whileBB.getIdentifierInstructionNum((String) expr.right.data);
                        }
                    } else {
                        // IF EXPR.RIGHT IS AN ARRAY SET RIGHT INSTRUCTION TO WHATEVER IS IN ADDA FIELD AND BREAK
                        if(expr.right.adda != -1){
                            rightInstruction = expr.right.adda;
                        }
                        else if (!bb.identifierInstructionExists((String) expr.right.data)) { // unassigned identifier on right side
                            System.out.println("right has no identifier instruction");
                            if (!BBS.get(0).constantinstructionExists(0)) { // default identifier value 0
                                System.out.println("No value was assigned to variable. Will be defaulted to 0");
                                BBS.get(0).addConstantToSymbolTable(0, instructionNum);
                                BBS.get(0).addStatement(new Instruction(instructionNum, bb.getOpCode('c'), 0)); // c just means constant
                                instructionNum++;
                            }
                            bb.addIdentifierToSymbolTable((String) expr.right.data, BBS.get(0).getConstantInstructionNum(0));
                            rightInstruction = bb.getIdentifierInstructionNum((String) expr.right.data);
                        } else {
                            System.out.println("right has instruction for ident");
                            rightInstruction = bb.getIdentifierInstructionNum((String) expr.right.data);
                            System.out.println("done getting right instruction");
                        }
                    }
                }
                else {
                    System.out.println("right is a constant");
                    rightInstruction = BBS.get(0).getConstantInstructionNum((int) expr.right.data);
                    System.out.println("done with right constant");
                }

                System.out.println("adding to statement with: " + instructionNum + " " + bb.getOpCode((Character) expr.data) + " " + leftInstruction + " " + rightInstruction);
                System.out.println("op, left, right " + (Character) expr.data + " " + expr.left.data + " " + expr.right.data);
                if(expr.left.adda != -1){
                    leftInstruction = expr.left.adda;
                }
                if(expr.right.adda != -1){
                    rightInstruction = expr.right.adda;
                }
                System.out.println("left right is adda " + expr.left.adda + " " + expr.right.adda);
                Instruction instruction = new Instruction(instructionNum, bb.getOpCode((Character) expr.data), leftInstruction, rightInstruction);
                // TWEAK CSE FOR ARRAYS, IF ARRAY DONT ASSIGN TO SYMBOL TABLE. IF DESNODE NOT AN ARRAY (DOESNT HAVE ADDA CONTINUE ELSE SKIP)
                if (!bb.opInstructionExists(bb.getOpCode((Character) expr.data), instruction)) { // instruction doesnt already exists in opcode LinkedList, then add it
                    bb.addOpInstruction(bb.getOpCode((Character) expr.data), instruction);
                    bb.addStatement(new Instruction(instructionNum, bb.getOpCode((Character) expr.data), leftInstruction, rightInstruction));
                    // SKIP ADDING TO SYM TABLE IF ITS AN ARRAY EX: HAS AN ADDA INSTRUCTION NOT == -1
                    if(desNode.adda == -1)
                        bb.addIdentifierToSymbolTable((String) desNode.data, instructionNum);
                    //System.out.println(desNode.data + " " + bb.getIdentifierInstructionNum((String) desNode.data));
                    instructionNum++;
                    // IF ADDA != -1 FOR DESNODE
                    // RIGHT = instructionNum - 1
                    if(desNode.adda != -1)
                        RIGHT = instructionNum - 1;
                }
                else {
                    // IF AN ARRAY SKIP THIS HAS ADDA INSTRUCTION NOT == -1 AND STORE RIGHT = bb.getOPInstructionNum()
                    if(desNode.adda == -1)
                        bb.addIdentifierToSymbolTable((String) desNode.data, bb.getOpInstructionNum());
                    else{
                        RIGHT = bb.getOpInstructionNum();
                    }
                }
                //if(bb.isWhile){
                if(whileBBS.size() != 0){
                    BasicBlock whileBB = whileBBS.get(whileBBS.size() - 1);
                    if(desNode.adda == -1 && !whileBB.containsPhi((String) desNode.data)){ // not a phi in while header block
                        whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", whileBB.getIdentifierInstructionNum((String) desNode.data), bb.getIdentifierInstructionNum((String) desNode.data)));
                        whileBB.addToPhiTable((String) desNode.data, instructionNum);
                        whileBB.addIdentifierToSymbolTable((String) desNode.data, instructionNum);
                        instructionNum++;
                        whileBB.numPhis++;
                    }
                    else{ // phi already exists in while header so update the right instruction to be the value in the bb symbol table for desNode
                        if(desNode.adda == -1) {
                            int phi = whileBB.getIdentifierInstructionNum((String) desNode.data); // phi we are looking for to update the right instruction
                            for (int i = 0; i < whileBB.numPhis; i++) {
                                if (whileBB.getStatements().get(i).getInstructionNum() == phi) {
                                    whileBB.getStatements().get(i).setRightInstruction(bb.getIdentifierInstructionNum((String) desNode.data));
                                }
                            }
                        }
                    }
                }
            } else {
                System.out.println(desNode.data);
                System.out.println(expr.data);
                System.out.println(BBS.get(0).constantinstructionExists((int) expr.data));
                if(desNode.adda == -1)
                    bb.addIdentifierToSymbolTable((String) desNode.data, BBS.get(0).getConstantInstructionNum((int) expr.data));
                else RIGHT = BBS.get(0).getConstantInstructionNum((int) expr.data);
            }
            // generate the instruction based on the expr we got and add it to the basic block
            // then add key: desNode.data, value: instruction number for the instruction we generated from expr and add that to the BasicBlock symbol table

            // IF DESNODE IS ARRAY: ADDA != -1
            // GEN STORE WITH LEFT IS DESNODE.ADDA AND RIGHT IS THE GLOBAL RIGHT THAT I CREATED
            System.out.println("AT DESNODE.ADDA");
            if(desNode.adda != -1){
                System.out.println("here in not -1");
                //Instruction store = new Instruction(instructionNum, "store", desNode.adda, RIGHT);
                Instruction store = new Instruction(instructionNum, "store", RIGHT, desNode.adda);
                // mark that desNode.data has a store instruction for CSE later of recreating a new load
                bb.getSymbolTable().arrayKills.put((String) desNode.data, -1);
                bb.getSymbolTable().hasStores.put((String) desNode.data, instructionNum);
                bb.addStatement(store);
                bb.addOpInstruction("load", store);
                instructionNum++;
            }
        }
        else{
            myTokenizer.Error("Expected identifier");
        }
        System.out.println("leaving assignment");
    }
    public Node designator() {
        seenIdent = myTokenizer.getIdentifier();
        if (bb.getSymbolTable().arrayToAddr.containsKey(seenIdent)) {
            arrName = seenIdent;
            indices = new ArrayList<>();
        }
        next();
        if (inputSym != Tokens.openbracketToken){
            if(!bb.identifierInstructionExists(seenIdent) && !LHS){
                if(!BBS.get(0).constantinstructionExists(0)){
                    BBS.get(0).addConstantToSymbolTable(0, instructionNum);
                    BBS.get(0).addStatement(new Instruction(instructionNum, "const", 0));
                    instructionNum++;
                }
                bb.addIdentifierToSymbolTable(seenIdent, BBS.get(0).getConstantInstructionNum(0));
            }
            return new Node(seenIdent, false);
        }
        while (inputSym == Tokens.openbracketToken) {
            next();
            Node expr = expression();
            // IF EXPR IS CONSTANT GET VAL FROM BB0 ELSE GET VAL FROM CURR BB IDENT SYM TABLE AND ADD TO LIST
            int instr;
            if(expr.left == null)
                instr = expr.constant ? BBS.get(0).getConstantInstructionNum((int) expr.data) : bb.getIdentifierInstructionNum((String) expr.data);
            else{
                int left = expr.left.constant ? BBS.get(0).getConstantInstructionNum((int) expr.left.data) : bb.getIdentifierInstructionNum((String) expr.left.data);
                int right = expr.right.constant ? BBS.get(0).getConstantInstructionNum((int) expr.right.data) : bb.getIdentifierInstructionNum((String) expr.right.data);
                Instruction instruction = new Instruction(instructionNum, bb.getOpCode((char) expr.data), left, right);
                if(!bb.opInstructionExists(bb.getOpCode((Character) expr.data), instruction)){
                    bb.addStatement(instruction);
                    bb.addOpInstruction(bb.getOpCode((char) expr.data), instruction);
                    instr = instructionNum;
                    instructionNum++;
                }
                else{
                    instr = bb.getOpInstructionNum();
                }
            }
            System.out.println(arrName + " " + expr.data + " " + instr);
            indices.add(instr);
            checkFor(Tokens.closebracketToken);
            // FLAG FOR IF WE SAW AN ARRAY = TRUE
            // MAKE FLAG FOR IF IM ON LHS OR RHS
            // GEN MUL WITH LEFT AS WHATEVER INSIDE [] AND RIGHT AS SIZE OF ARRAY CHECK IF MUL AND ADD ARE CSE
            // GEN ADD WITH LEFT AS BASE AND RIGHT AS VAR_ADDR
            // STORE ADD INSTRUCTION TO ARRAY SYMBOL TABLE THAT MAPS ARRAY NAME(STRING) TO LIST OF INSTRUCTION NUMS (INTEGER) GROUPS OF 2 WILL BE A DIMENSION
        }
        System.out.println("is array " + arrName + " " + isArray + " " + indices);
        // IF FLAG
        // IF RHS CREATE LOAD AND CHECK IF THERES A PREV STORE IF YES CREATE NEW LOAD ELSE CSE IT
        // GEN ADDA WITH LEFT AS INDEX 0 AND RIGHT AS INDEX 1, REMOVE INDEX 0 TWICE WHEN DONE USING
        // RETURN new Node(seenIdent, false, adda: instructionNum - 1)
        //if(isArray){
        if (indices.size() > 0) {
            System.out.println("is array " + isArray);
            System.out.println(indices.size());
            int leftInstruction, rightInstruction;
            if (indices.size() > 1) {
                int prevInstr = indices.get(0);
                for (int i = 1; i < indices.size(); i++) {
                    Instruction mulInstr = new Instruction(instructionNum, "mul", prevInstr, bb.getSymbolTable().arrayToDimensions.get(arrName).get(i));
                    if (!bb.opInstructionExists("mul", mulInstr)) {
                        bb.addOpInstruction("mul", mulInstr);
                        bb.addStatement(mulInstr);
                        prevInstr = instructionNum;
                        instructionNum++;
                    } else {
                        prevInstr = bb.getOpInstructionNum();
                    }
                    Instruction addInstr = new Instruction(instructionNum, "add", prevInstr, indices.get(i));
                    if (!bb.opInstructionExists("add", addInstr)) {
                        bb.addOpInstruction("add", addInstr);
                        bb.addStatement(addInstr);
                        prevInstr = instructionNum;
                        instructionNum++;
                    } else {
                        prevInstr = bb.getOpInstructionNum();
                    }
                }
                Instruction mulInstr = new Instruction(instructionNum, "mul", prevInstr, 0);
                if (!bb.opInstructionExists("mul", mulInstr)) {
                    bb.addOpInstruction("mul", mulInstr);
                    bb.addStatement(mulInstr);
                    leftInstruction = instructionNum;
                    instructionNum++;
                } else {
                    leftInstruction = bb.getOpInstructionNum();
                }
                Instruction addInstr = new Instruction(instructionNum, "add", 1, bb.getSymbolTable().arrayToAddr.get(arrName));
                if (!bb.opInstructionExists("add", addInstr)) {
                    bb.addOpInstruction("add", addInstr);
                    bb.addStatement(addInstr);
                    rightInstruction = instructionNum;
                    instructionNum++;
                } else {
                    rightInstruction = bb.getOpInstructionNum();
                }
                Instruction adda = new Instruction(instructionNum, "adda", leftInstruction, rightInstruction);
                if (!bb.opInstructionExists("load", adda, arrName)) {
                    bb.addOpInstruction("load", adda);
                    bb.addStatement(adda);
                    leftInstruction = instructionNum;
                    instructionNum++;
                } else {
                    leftInstruction = bb.getOpInstructionNum();
                }
                System.out.println("array LHS " + LHS);
                if (!LHS) {
                    System.out.println("NOT FROM LHS OF ARRAY");
                    Instruction load = new Instruction(instructionNum, "load", leftInstruction, -1);
                    System.out.println("LOAD FOR MULTI " + instructionNum + " " + leftInstruction);
                    if (!bb.opInstructionExists("load", load, arrName)) {
                        System.out.println("no load");
                        bb.addOpInstruction("load", load);
                        bb.addStatement(load); // CHECK IF CSE
                        leftInstruction = instructionNum;
                        instructionNum++;
                    } else {
                        leftInstruction = bb.getOpInstructionNum();
                    }
                    System.out.println("lefffff " + leftInstruction);
                }
                //return new Node(arrName, false, leftInstruction);
            } else {
                System.out.println("in arrays else ");
                Instruction mulInstr = new Instruction(instructionNum, "mul", indices.get(0), 0);
                if (!bb.opInstructionExists("mul", mulInstr)) {
                    bb.addOpInstruction("mul", mulInstr);
                    bb.addStatement(mulInstr);
                    leftInstruction = instructionNum;
                    instructionNum++;
                } else {
                    leftInstruction = bb.getOpInstructionNum();
                }
                Instruction addInstr = new Instruction(instructionNum, "add", 1, bb.getSymbolTable().arrayToAddr.get(arrName));
                if (!bb.opInstructionExists("add", addInstr)) {
                    bb.addOpInstruction("add", addInstr);
                    bb.addStatement(addInstr);
                    rightInstruction = instructionNum;
                    instructionNum++;
                } else {
                    rightInstruction = bb.getOpInstructionNum();
                }
                Instruction adda = new Instruction(instructionNum, "adda", leftInstruction, rightInstruction);
                if (!bb.opInstructionExists("load", adda)) {
                    bb.addOpInstruction("load", adda);
                    bb.addStatement(adda);
                    leftInstruction = instructionNum;
                    instructionNum++;
                } else {
                    leftInstruction = bb.getOpInstructionNum();
                }
                System.out.println("array LHS " + LHS + " " + leftInstruction);
                if (!LHS) {
                    System.out.println("NOT FROM LHS OF ARRAY " + leftInstruction);
                    Instruction load = new Instruction(instructionNum, "load", leftInstruction, -1);
                    System.out.println("LOAD ISNTR " + instructionNum + " " + leftInstruction);
                    if (!bb.opInstructionExists("load", load, arrName)) {
                        bb.addOpInstruction("load", load);
                        bb.addStatement(load); // CHECK IF CSE
                        leftInstruction = instructionNum;
                        instructionNum++;
                    } else {
                        leftInstruction = bb.getOpInstructionNum();
                    }
                }
            }
            System.out.println("arrName, indices, left instr " + arrName + " " + indices + " " + leftInstruction);
            return new Node(arrName, false, leftInstruction);
        }
        return new Node(seenIdent, false); // ADDA: WILL BE DEFAULT -1
    }
    public Node expression(){
        System.out.println("going to term");
        Node term1 = term();
        System.out.println("back from term");

        while(inputSym == Tokens.plusToken || inputSym == Tokens.minusToken){
            Node op = null;
            if(inputSym == Tokens.plusToken){
                op = new Node('+', false);
            }
            else{
                op = new Node('-', false);
            }
            next(); // eat the op
            Node term2 = term();
            op.left = term1;
            op.right = term2;
            return op;
            // if constant add to BB0 else add to current BB
            // generate and return instruction for either constants or term1, term2 if theyre identifiers
        }
        return term1; // return the instruction for term1
    }
    public Node term(){
        System.out.println("going to factor");
        Node factor1 = factor();
        System.out.println("back from factor");

        while(inputSym == Tokens.timesToken || inputSym == Tokens.divToken){
            Node op = null;
            if(inputSym == Tokens.timesToken){
                op = new Node('*', false);
            }
            else{
                op = new Node('/', false);
            }
            next(); // eat the op
            Node factor2 = factor();
            op.left = factor1;
            op.right = factor2;
            return op;
        }
        System.out.println("leaving term");
        return factor1;
    }
    public Node factor(){ // returns either node or instruction
        System.out.println("in factor");
        Node ret = null;
        if(inputSym == Tokens.ident){
            System.out.println("going to designator");
            ret = designator();
            System.out.println("back from designator");
        }
        else if(inputSym == Tokens.number){ // generate instruction for constants here if it is not already in BB0
            BasicBlock basicBlock0 = BBS.get(0);
            val = myTokenizer.getNumber();
            if(!basicBlock0.constantinstructionExists(val)){ // instruction not created for constant before so create it
                basicBlock0.addConstantToSymbolTable(val, instructionNum);
                basicBlock0.addStatement(new Instruction(instructionNum, basicBlock0.getOpCode('c'), val)); // c just means constant
                instructionNum++;
            }
            ret = new Node(val, true);
            next(); // eat number
        }
        else if(inputSym == Tokens.openParenToken){
            next();
            ret = expression();
            checkFor(Tokens.closeParenToken);
        }
        else if(inputSym == Tokens.callToken){
            System.out.println("going to funcCall");
            funcCall();
            System.out.println("back from funcCall");
        }
        else{
            myTokenizer.Error("Expected identifier, number, ( for expression, or funcCall");
        }
        System.out.println("leaving factor");
        return ret;
    }
    public void funcCall(){
        System.out.println("bbNum " + basicBlockNum);
        System.out.println(bb.getBBNum());
        System.out.println("in funcCall");
        System.out.println(inputSym);
        next(); // eat call
        if(inputSym == Tokens.inputNum){
            next(); // eat InputNum

            // TO READ FROM USER
//            val = scanner.nextInt();
//            BasicBlock basicBlock0 = BBS.get(0);
//            if(!basicBlock0.constantinstructionExists(val)){ // instruction not created for constant before so create it
//                basicBlock0.addConstantToSymbolTable(val, instructionNum);
//                basicBlock0.addStatement(new Instruction(instructionNum, basicBlock0.getOpCode('c'), val)); // c just means constant
//                instructionNum++;
//            }
            bb.addStatement(new Instruction(instructionNum, "read", -1, -1));
            if(!LHSIsArray) {
                bb.addIdentifierToSymbolTable(seenIdent, instructionNum);
            }
            instructionNum++;
            checkFor(Tokens.openParenToken);
            checkFor(Tokens.closeParenToken);
        }
        else if(inputSym == Tokens.outputNum){
            System.out.println(inputSym);
            next(); // eat outputNum
            seenIdent = myTokenizer.getIdentifier();
            System.out.println("seee " + seenIdent);
            System.out.println(bb.getBBNum());

            System.out.println("before");
            checkFor(Tokens.openParenToken);
            System.out.println("after");
            seenIdent = myTokenizer.getIdentifier();
            // IF SEEN IDENT IS ARRAY CALL DESIGNATOR TO PARSE THE [] AND WHATEVER IS INSIDE THE BRACKETS
            if(bb.getSymbolTable().arrayToAddr.containsKey(seenIdent)){
                Node desNode = designator();
                bb.addStatement(new Instruction(instructionNum, "write", desNode.adda, -1));
                instructionNum++;
                checkFor(Tokens.closeParenToken);
            }
            else {
                Node ret = expression();
                int left = -1, right = -1;
                if(ret.left == null){
                    if(ret.constant) left = BBS.get(0).getConstantInstructionNum((int) ret.data);
                    else if(ret.adda == -1) left = bb.getIdentifierInstructionNum((String) ret.data);
                    else left = ret.adda;
                }
                else{
                    if(ret.left.adda == -1)
                        left = ret.left.constant ? BBS.get(0).getConstantInstructionNum((int)ret.left.data) : bb.getIdentifierInstructionNum((String) ret.left.data);
                    else left = ret.left.adda;
                    if(ret.right.adda == -1)
                        right = ret.right.constant ? BBS.get(0).getConstantInstructionNum((int)ret.right.data) : bb.getIdentifierInstructionNum((String) ret.right.data);
                    else right = ret.right.adda;
                    Instruction instruction = new Instruction(instructionNum, bb.getOpCode((Character) ret.data), left, right);
                    if(!bb.opInstructionExists(bb.getOpCode((Character) ret.data), instruction)){
                        bb.addStatement(instruction);
                        left = instructionNum;
                        instructionNum++;
                    }
                    else left = bb.getOpInstructionNum();
                }
                bb.addStatement(new Instruction(instructionNum, "write", left, -1));
                //bb.addIdentifierToSymbolTable(seenIdent, instructionNum);
                instructionNum++;
                //next(); // eat identifier
                checkFor(Tokens.closeParenToken);
            }
            System.out.println(seenIdent);
//            next(); // eat identifier
//            checkFor(Tokens.closeParenToken);
        }
        else if(inputSym == Tokens.outputNewLine){
            next();
            bb.addStatement(new Instruction(instructionNum, "writeNewLine", -1, -1));
            instructionNum++;
            checkFor(Tokens.openParenToken);
            checkFor(Tokens.closeParenToken);
            System.out.println(); // print the new line to console
        }
        System.out.println("leaving funcCall");
    }
    public void ifStatement(){
        System.out.println("in ifStatement");
        BasicBlock ifBB = null; // for when we see an if
        BasicBlock elseBB = null; // for when we see an else
        BasicBlock joinBB = null; // for when we can join blocks

        bb.isIf = true;

        next(); // eat if
        joinBlocksNeeded++; // added
        System.out.println("going to relation");
        Node rel = relation();
        System.out.println("back from relation");
//        System.out.println((String) rel.data);
//        System.out.println((String) rel.left.data);
//        System.out.println((int) rel.right.data);

//        System.out.println(rel.left.constant);
//        System.out.println(BBS.get(0).getConstantInstructionNum((int) rel.right.data));
        // get left and right instructions and generate cmp instruction
        int leftInstruction = rel.left.constant ? BBS.get(0).getConstantInstructionNum((int)rel.left.data) : bb.getIdentifierInstructionNum((String) rel.left.data);
        int rightInstruction = rel.right.constant ? BBS.get(0).getConstantInstructionNum((int) rel.right.data) : bb.getIdentifierInstructionNum((String) rel.right.data);
       // System.out.println("here");
        bb.addStatement(new Instruction(instructionNum, "cmp", leftInstruction, rightInstruction));
        instructionNum++;

        // generate negation of relOp
        String relOp = (String) rel.data;
        if(relOp.equals("==")){
            relOp = "bne";
        }
        else if(relOp.equals("!=")){
            relOp = "beq";
        }
        else if(relOp.equals("<")){
            relOp = "bge";
        }
        else if(relOp.equals("<=")){
            relOp = "bgt";
        }
        else if(relOp.equals(">")){
            relOp = "ble";
        }
        else{
            relOp = "blt";
        }
        // generate branching instruction for if condition not met
        bb.addStatement(new Instruction(instructionNum, relOp, instructionNum-1, -1)); // left = prev Instruction, right = -1 temporarily
        instructionNum++;

        // process else block if any
        if(joinBlocksNeeded <= 0) return;
        basicBlockNum++;
        ifBB = new BasicBlock(basicBlockNum);
        ifBB.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
        ifBB.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
        ifBB.deepCopyArrayAddrTable(bb.getSymbolTable().arrayToAddr);
        ifBB.deepCopyArrayDimensionTable(bb.getSymbolTable().arrayToDimensions);
        ifBB.isThen = true;

        BBS.put(basicBlockNum, ifBB);

        if(bb.getLeftBasicBlock() == null){
            bb.setLeftBasicBlock(ifBB);
        }
        //System.out.println("beforeeeee the stat in if " + bb.getBBNum());
        ifBBS.push(bb); // store current basic block, will be used to set any elses
        bb = ifBB;
        //System.out.println("afterrrrr the stat in if " + bb.getBBNum());

        checkFor(Tokens.thenToken);

        statSequence();
        //System.out.println("after coming back from stat " + bb.getBBNum());

        // generate branch for current block
        bb.addStatement(new Instruction(instructionNum, "bra", -1, -1));
        instructionNum++;

        if(lastSeenBB != null){
            lastSeenBB.getStatements().get(lastSeenBB.getStatements().size() - 1).setRightInstruction(bb.getStatements().get(0).getInstructionNum());
            lastSeenBB = null;
        }
        ifBB = bb;
        bb = ifBBS.pop();
//        System.out.println("bb if" + bb.getBBNum() + " " + ifBB.getBBNum());
//        System.out.println(bb.getOpInstructionsHM());
//        System.out.println(ifBB.getOpInstructionsHM());
        if(joinBlocksNeeded <= 0) return;

        if(inputSym == Tokens.elseToken){
            next(); // eat else
            if(joinBlocksNeeded <= 0) return;
            basicBlockNum++;
            elseBB = new BasicBlock(basicBlockNum);
            elseBB.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
            //System.out.println("elsebb op inst " + elseBB.getOpInstructionsHM());
            elseBB.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
            elseBB.deepCopyArrayAddrTable(bb.getSymbolTable().arrayToAddr);
            elseBB.deepCopyArrayDimensionTable(bb.getSymbolTable().arrayToDimensions);
           // System.out.println("ifbb op inst " + bb.getOpInstructionsHM());
            elseBB.isElse = true;
            BBS.put(basicBlockNum, elseBB);
            if(bb.getRightBasicBlock() == null){
                bb.setRightBasicBlock(elseBB);
            }
            BasicBlock parent = bb;
            bb = elseBB;
            //System.out.println("going to stat seq from else in if " + elseBB.getBBNum() + " " + parent.getBBNum() + " " + parent.getStatements().size() + " " + parent.getOpInstructionsHM());
            statSequence();
            if(elseBB.getStatements().size() == 0){ // generate empty instruction
                elseBB.addStatement(new Instruction(instructionNum, "empty", -1, -1));
                instructionNum++;
            }

            Instruction instruction = parent.getStatements().get(parent.getStatements().size()-1);
            instruction.setRightInstruction(elseBB.getStatements().get(0).getInstructionNum());
            parent.getStatements().set(parent.getStatements().size()-1, instruction);
            bb = parent;
        }

        if(inputSym == Tokens.fiToken){
            next(); // eat fi
            // generate join Block
            if(joinBlocksNeeded <= 0) return;
            basicBlockNum++;
            joinBB = new BasicBlock(basicBlockNum);
            joinBB.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
            joinBB.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
            joinBB.deepCopyArrayAddrTable(bb.getSymbolTable().arrayToAddr);
            joinBB.deepCopyArrayDimensionTable(bb.getSymbolTable().arrayToDimensions);
//            if(bb.isElse) joinBB.isElse = true;
//            else if(bb.isThen) joinBB.isJoin = true;
            joinBB.isJoin = true;
            BBS.put(basicBlockNum, joinBB);
            joinBlocksNeeded--;

            if(elseBB != null){ // theres an else block so compare if to else symbol table
                if(bb.getRightBasicBlock() == null){
                    bb.setRightBasicBlock(elseBB);
                }
                if(ifBB.getLeftBasicBlock() == null){
                    ifBB.setLeftBasicBlock(joinBB);
                }
                if(elseBB.getLeftBasicBlock() == null){
                    elseBB.setLeftBasicBlock(joinBB);
                }
                // if elseBB.getBBNum() - lastSeenJoin.getBBNum() == 1 compare elseBB to lastSeenjoin to make curr JoinBB and set lastSeenJoin to joinBB
                // else do whatever is here now below
                if(lastSeenJoinBB != null && elseBB.getBBNum() - lastSeenJoinBB.getBBNum() == 1){
                    lastSeenJoinBB.setLeftBasicBlock(joinBB);
                    elseBB.setLeftBasicBlock(joinBB);
                    HashMap<String, Integer> lastSeenJoinHM = lastSeenJoinBB.getSymbolTable().getIdentifierToInstructionNumHM();
                    HashMap<String, Integer> elseHM = elseBB.getSymbolTable().getIdentifierToInstructionNumHM();

                    for(Map.Entry<String, Integer> entry : lastSeenJoinHM.entrySet()){
                        if(entry.getValue() != elseHM.get(entry.getKey())){
                            joinBB.addStatement(new Instruction(instructionNum, "phi", entry.getValue(), elseHM.get(entry.getKey())));
                            joinBB.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                            joinBB.addToPhiTable(entry.getKey(), instructionNum);
                            instructionNum++;
                        }
                    }
                    for(Map.Entry<String, Integer> entry : lastSeenJoinBB.getSymbolTable().arrayKills.entrySet()){
                        if(!joinBB.getSymbolTable().arrayKills.containsKey(entry.getKey())){
                            Instruction killInstr = new Instruction(instructionNum, "kill " + entry.getKey());
                            joinBB.addKill(joinBB.numKills, killInstr);
                            joinBB.numKills++;
                            joinBB.getSymbolTable().arrayKills.put(entry.getKey(), instructionNum);
                            instructionNum++;
                        }
                    }
                    for(Map.Entry<String, Integer> entry : elseBB.getSymbolTable().arrayKills.entrySet()){
                        if(!joinBB.getSymbolTable().arrayKills.containsKey(entry.getKey())){
                            Instruction killInstr = new Instruction(instructionNum, "kill " + entry.getKey());
                            joinBB.addKill(joinBB.numKills, killInstr);
                            joinBB.numKills++;
                            joinBB.getSymbolTable().arrayKills.put(entry.getKey(), instructionNum);
                            instructionNum++;
                        }
                    }

                    Instruction instruction =  lastSeenJoinBB.getStatements().get(lastSeenJoinBB.getStatements().size()-1);
                    instruction.setLeftInstruction(joinBB.getStatements().get(0).getInstructionNum());
                    lastSeenJoinBB.getStatements().set(lastSeenJoinBB.getStatements().size()-1, instruction);
                    lastSeenJoinBB = joinBB;
                    return;
                }
                HashMap<String, Integer> ifHM = ifBB.getSymbolTable().getIdentifierToInstructionNumHM();
                HashMap<String, Integer> elseHM = elseBB.getSymbolTable().getIdentifierToInstructionNumHM();

                for(Map.Entry<String, Integer> entry : ifHM.entrySet()){
                    if(entry.getValue() != elseHM.get(entry.getKey())){
                        joinBB.addStatement(new Instruction(instructionNum, "phi", entry.getValue(), elseHM.get(entry.getKey())));
                        joinBB.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                        joinBB.addToPhiTable(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                }
                for(Map.Entry<String, Integer> entry : ifBB.getSymbolTable().arrayKills.entrySet()){
                    if(!joinBB.getSymbolTable().arrayKills.containsKey(entry.getKey())){
                        Instruction killInstr = new Instruction(instructionNum, "kill " + entry.getKey());
                        joinBB.addKill(joinBB.numKills, killInstr);
                        joinBB.numKills++;
                        joinBB.getSymbolTable().arrayKills.put(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                }
                for(Map.Entry<String, Integer> entry : elseBB.getSymbolTable().arrayKills.entrySet()){
                    if(!joinBB.getSymbolTable().arrayKills.containsKey(entry.getKey())){
                        Instruction killInstr = new Instruction(instructionNum, "kill " + entry.getKey());
                        joinBB.addKill(joinBB.numKills, killInstr);
                        joinBB.numKills++;
                        joinBB.getSymbolTable().arrayKills.put(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                }
                // setting bra instructions
                Instruction instruction = ifBB.getStatements().get(ifBB.getStatements().size()-1);
                instruction.setLeftInstruction(joinBB.getStatements().get(0).getInstructionNum());
                ifBB.getStatements().set(ifBB.getStatements().size()-1, instruction);
            }
            else{ // theres no else so compare if to parent symbol table
                if(ifBB.getLeftBasicBlock() == null){
                    ifBB.setLeftBasicBlock(joinBB);
                }
                if(bb.getRightBasicBlock() == null){
                    bb.setRightBasicBlock(joinBB);
                }
                HashMap<String, Integer> ifHM = ifBB.getSymbolTable().getIdentifierToInstructionNumHM();
                HashMap<String, Integer> parentHM = bb.getSymbolTable().getIdentifierToInstructionNumHM();

//                System.out.println(ifHM);
//                System.out.println(parentHM);

                for(Map.Entry<String, Integer> entry : ifHM.entrySet()){
                    if(!parentHM.containsKey(entry.getKey())){
                        if(!BBS.get(0).constantinstructionExists(0)){
                            BBS.get(0).addStatement(new Instruction(instructionNum, "const", 0));
                            BBS.get(0).addConstantToSymbolTable(0, instructionNum);
                            instructionNum++;
                        }
                        parentHM.put(entry.getKey(), BBS.get(0).getConstantInstructionNum(0));
                    }
//                    System.out.println("key + val " + entry.getKey());
//                    System.out.println(entry.getValue() + " " + parentHM.get(entry.getKey()));
                    if(entry.getValue() != parentHM.get(entry.getKey())){
                        joinBB.addStatement(new Instruction(instructionNum, "phi", entry.getValue(), parentHM.get(entry.getKey())));
                        joinBB.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                        joinBB.addToPhiTable(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                }
                for(Map.Entry<String, Integer> entry : ifBB.getSymbolTable().arrayKills.entrySet()){
                    if(!joinBB.getSymbolTable().arrayKills.containsKey(entry.getKey())){
                        Instruction killInstr = new Instruction(instructionNum, "kill " + entry.getKey());
                        joinBB.addKill(joinBB.numKills, killInstr);
                        joinBB.numKills++;
                        joinBB.getSymbolTable().arrayKills.put(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                }
                Instruction instruction = ifBB.getStatements().get(ifBB.getStatements().size()-1);
                instruction.setLeftInstruction(joinBB.getStatements().get(0).getInstructionNum());
                ifBB.getStatements().set(ifBB.getStatements().size()-1, instruction);

                instruction = bb.getStatements().get(bb.getStatements().size()-1);
                instruction.setRightInstruction(joinBB.getStatements().get(0).getInstructionNum());
                bb.getStatements().set(bb.getStatements().size()-1, instruction);
            }
            if(lastSeenJoinBB != null){ // merge two joins to one
                if(joinBlocksNeeded < 0) return;
                basicBlockNum++;
                BasicBlock mergedJoin = new BasicBlock(basicBlockNum);
                mergedJoin.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
                mergedJoin.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
                mergedJoin.deepCopyArrayAddrTable(bb.getSymbolTable().arrayToAddr);
                mergedJoin.deepCopyArrayDimensionTable(bb.getSymbolTable().arrayToDimensions);
                if(bb.isThen) mergedJoin.isJoin = true;
                else if(bb.isElse) mergedJoin.isElse = true;
                BBS.put(basicBlockNum, mergedJoin);
                joinBlocksNeeded--;

                if(lastSeenJoinBB.getLeftBasicBlock() == null){
                    lastSeenJoinBB.setLeftBasicBlock(mergedJoin);
                }
                if(joinBB.getLeftBasicBlock() == null){
                    joinBB.setLeftBasicBlock(mergedJoin);
                }
                HashMap<String, Integer> lastSeenJoinHM = lastSeenJoinBB.getSymbolTable().getIdentifierToInstructionNumHM();
                HashMap<String, Integer> joinBBHM = joinBB.getSymbolTable().getIdentifierToInstructionNumHM();

                for(Map.Entry<String, Integer> entry : lastSeenJoinHM.entrySet()){
                    if(entry.getValue() != joinBBHM.get(entry.getKey())){
                        mergedJoin.addStatement(new Instruction(instructionNum, "phi", entry.getValue(), joinBBHM.get(entry.getKey())));
                        mergedJoin.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                        mergedJoin.addToPhiTable(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                }
                for(Map.Entry<String, Integer> entry : lastSeenJoinBB.getSymbolTable().arrayKills.entrySet()){
                    if(!mergedJoin.getSymbolTable().arrayKills.containsKey(entry.getKey())){
                        Instruction killInstr = new Instruction(instructionNum, "kill " + entry.getKey());
                        mergedJoin.addKill(mergedJoin.numKills, killInstr);
                        mergedJoin.numKills++;
                        mergedJoin.getSymbolTable().arrayKills.put(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                }
                for(Map.Entry<String, Integer> entry : joinBB.getSymbolTable().arrayKills.entrySet()){
                    if(!mergedJoin.getSymbolTable().arrayKills.containsKey(entry.getKey())){
                        Instruction killInstr = new Instruction(instructionNum, "kill " + entry.getKey());
                        mergedJoin.addKill(mergedJoin.numKills, killInstr);
                        mergedJoin.numKills++;
                        mergedJoin.getSymbolTable().arrayKills.put(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                }
                Instruction instruction = lastSeenJoinBB.getStatements().get(lastSeenJoinBB.getStatements().size()-1);
                instruction.setLeftInstruction(mergedJoin.getStatements().get(0).getInstructionNum());
                lastSeenJoinBB.getStatements().set(lastSeenJoinBB.getStatements().size()-1, instruction);
                lastSeenJoinBB = mergedJoin;
            }
            else{
                lastSeenJoinBB = joinBB;
            }
            if(whileBBS.size() != 0){
                lastSeenJoinBB.setReturnToBasicBlock(whileBBS.get(whileBBS.size() - 1));
            }
        }
        // join if and else blocks or join if and parent if theres no else does Dead Code Elimination in the process
    }
    public void whileStatement(){
        next(); // eat while

        BasicBlock whileBB = null;
        BasicBlock doBB = null;
        BasicBlock followBB = null;

        basicBlockNum++;
        whileBB = new BasicBlock(basicBlockNum);

        BBS.put(basicBlockNum, whileBB);

        whileBB.isWhile = true;
        whileBB.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
        whileBB.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
        whileBB.deepCopyArrayAddrTable(bb.getSymbolTable().arrayToAddr);
        whileBB.deepCopyArrayDimensionTable(bb.getSymbolTable().arrayToDimensions);
        whileBB.deepCopyStores(bb.getSymbolTable().hasStores);

        if(bb.getLeftBasicBlock() == null){
            bb.setLeftBasicBlock(whileBB);
        }
        BasicBlock parent = bb;
        bb = whileBB;
        Node rel = relation();
        System.out.println("rel while " + rel.data + " " + rel.left.data + " " + rel.right.data + " " + rel.left.adda + " " + rel.right.adda);
        bb = parent;

        int leftInstruction, rightInstruction = 0;
        // generate phis in while header if used
        if(!rel.left.constant){
            if(rel.left.adda != -1){
                leftInstruction = rel.left.adda;
            }
            //else if(whileBBS.size() != 0 && !whileBB.containsPhi((String) rel.left.data)){
            else if(!whileBB.containsPhi((String) rel.left.data)){
                // have to search all previous whileBBSS to see if theres a phi and use the phis right instruction if not == -1 else use the instructionNum of that phi
                //leftInstruction = whileBBS.get(whileBBS.size() - 1).getIdentifierInstructionNum((String) rel.left.data);
                if(!whileBB.identifierInstructionExists((String) rel.left.data)){
                    if(!BBS.get(0).constantinstructionExists(0)){
                        BBS.get(0).addStatement(new Instruction(instructionNum, "const", 0));
                        BBS.get(0).addConstantToSymbolTable(0, instructionNum);
                        instructionNum++;
                    }
                    whileBB.addIdentifierToSymbolTable((String) rel.left.data, instructionNum - 1);
                }
                leftInstruction = whileBB.getIdentifierInstructionNum((String) rel.left.data);
                for (int i = whileBBS.size() - 1; i >= 0; i--) {
                    boolean found = false;
                    if (whileBBS.get(i).containsPhi((String) rel.left.data)) {
                        int instr = whileBBS.get(i).getIdentifierInstructionNum((String) rel.left.data);
                        for (int j = 0; j < whileBBS.get(i).numPhis; j++) {
                            if (whileBBS.get(i).getStatements().get(j).getInstructionNum() == instr) {
                                if (whileBBS.get(i).getStatements().get(j).getRightInstruction() == -1) {
                                    leftInstruction = whileBBS.get(i).getStatements().get(j).getInstructionNum();
                                } else leftInstruction = whileBBS.get(i).getStatements().get(j).getRightInstruction();
                                found = true;
                            }
                            if(found) break;
                        }
                    }
                    if(found) break;
                }
            }
            else leftInstruction = whileBB.getIdentifierInstructionNum((String) rel.left.data);
            System.out.println("left in while " + leftInstruction);
            //whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", whileBB.getIdentifierInstructionNum((String) rel.left.data), -1));
            if(rel.left.adda == -1) {
                whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", leftInstruction, -1));
                whileBB.addToPhiTable((String) rel.left.data, instructionNum);
                whileBB.addIdentifierToSymbolTable((String) rel.left.data, instructionNum);
                instructionNum++;
                whileBB.numPhis++;
                leftInstruction = whileBB.getIdentifierInstructionNum((String) rel.left.data);
            }
        }
        else{
            leftInstruction = BBS.get(0).getConstantInstructionNum((int) rel.left.data);
        }
        if(!rel.right.constant){
            if(rel.right.adda != -1){
                rightInstruction = rel.right.adda;
            }
            else if(!whileBB.containsPhi((String) rel.right.data)) {
                // have to search all previous whileBBSS to see if theres a phi and use the phis right instruction if not == -1 else use the instructionNum of that phi
                leftInstruction = whileBBS.get(whileBBS.size() - 1).getIdentifierInstructionNum((String) rel.right.data);
                for (int i = whileBBS.size() - 1; i >= 0; i--) {
                    boolean found = false;
                    if (whileBBS.get(i).containsPhi((String) rel.right.data)) {
                        int instr = whileBBS.get(i).getIdentifierInstructionNum((String) rel.right.data);
                        for (int j = 0; j < whileBBS.get(i).numPhis; j++) {
                            if (whileBBS.get(i).getStatements().get(j).getInstructionNum() == instr) {
                                if (whileBBS.get(i).getStatements().get(j).getRightInstruction() == -1) {
                                    leftInstruction = whileBBS.get(i).getStatements().get(j).getInstructionNum();
                                } else leftInstruction = whileBBS.get(i).getStatements().get(j).getRightInstruction();
                                found = true;
                            }
                            if(found) break;
                        }
                    }
                    if(found) break;
                }
            }
            else leftInstruction = whileBB.getIdentifierInstructionNum((String) rel.right.data);
            System.out.println("left in while right " + leftInstruction);
            //whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", whileBB.getIdentifierInstructionNum((String) rel.right.data), -1));
            if(rel.right.adda == -1) {
                whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", leftInstruction, -1));
                whileBB.addToPhiTable((String) rel.right.data, instructionNum);
                whileBB.addIdentifierToSymbolTable((String) rel.right.data, instructionNum);
                instructionNum++;
                whileBB.numPhis++;
                rightInstruction = whileBB.getIdentifierInstructionNum((String) rel.right.data);
            }
        }
        else{
            rightInstruction = BBS.get(0).getConstantInstructionNum((int) rel.right.data);
        }

        // add cmp to while header
        whileBB.addStatement(new Instruction(instructionNum, "cmp", leftInstruction, rightInstruction));
        instructionNum++;

        // generate negation of relOp
        String relOp = (String) rel.data;
        if(relOp.equals("==")){
            relOp = "bne";
        }
        else if(relOp.equals("!=")){
            relOp = "beq";
        }
        else if(relOp.equals("<")){
            relOp = "bge";
        }
        else if(relOp.equals("<=")){
            relOp = "bgt";
        }
        else if(relOp.equals(">")){
            relOp = "ble";
        }
        else{
            relOp = "blt";
        }

        // generate branching instruction for if condition not met
        whileBB.addStatement(new Instruction(instructionNum, relOp, instructionNum-1, -1));
        instructionNum++;

        checkFor(Tokens.doToken);

        basicBlockNum++;
        doBB = new BasicBlock(basicBlockNum);

        BBS.put(basicBlockNum, doBB);

        doBB.isWhile = true;
        doBB.isDo = true;

        doBB.deepCopyOfOPInstructions(whileBB.getOpInstructionsHM());
        doBB.deepCopyOfSymbolTable(whileBB.getSymbolTable().getIdentifierToInstructionNumHM());
        doBB.deepCopyArrayAddrTable(whileBB.getSymbolTable().arrayToAddr);
        doBB.deepCopyArrayDimensionTable(whileBB.getSymbolTable().arrayToDimensions);
        doBB.deepCopyStores(whileBB.getSymbolTable().hasStores);

        if(whileBB.getLeftBasicBlock() == null){
            whileBB.setLeftBasicBlock(doBB);
        }
        if(doBB.getReturnToBasicBlock() == null){
            doBB.setReturnToBasicBlock(whileBB);
        }

        whileBBS.add(whileBB);
        bb = doBB;

        statSequence();

        System.out.println("while bb has store " + bb.getSymbolTable().hasStores);

        for(Map.Entry<String, Integer> entry : bb.getSymbolTable().arrayKills.entrySet()){ // doBB
            if(!whileBB.getSymbolTable().arrayKills.containsKey(entry.getKey())){
                Instruction killInstr = new Instruction(instructionNum, "kill " + entry.getKey());
                whileBB.addKill(whileBB.numKills, killInstr);
                whileBB.numKills++;
                whileBB.getSymbolTable().arrayKills.put(entry.getKey(), instructionNum);
                instructionNum++;
                System.out.println("REL.LEFT.ADDA " + rel.left.adda);
                if(rel.left.adda != -1 && ((String) rel.left.data).equals(entry.getKey())){
                    Instruction load = new Instruction(instructionNum, "load", rel.left.adda, -1);
                    whileBB.addOpInstruction("load", load);
                    whileBB.addLoad(whileBB.numKills + whileBB.numPhis, load);
                    updateMap.put(rel.left.adda, instructionNum);
                    instructionNum++;
                    whileBB.getStatements().get(whileBB.getStatements().size() - 2).setLeftInstruction(instructionNum-1);
                }
                if(rel.right.adda != -1 && rel.right.data.equals(entry.getKey())){
                    Instruction load = new Instruction(instructionNum, "load", rel.right.adda, -1);
                    whileBB.addOpInstruction("load", load);
                    whileBB.addLoad(whileBB.numKills + whileBB.numPhis, load);
                    updateMap.put(rel.right.adda, instructionNum);
                    instructionNum++;
                    whileBB.getStatements().get(whileBB.getStatements().size() - 2).setRightInstruction(instructionNum - 1);
                }
            }
        }

        int instr = -1;
        for(Instruction instruction : whileBBS.get(whileBBS.size() - 1).getStatements()){
            if(instruction.getOpCode().contains("kill")){
                instr = instruction.getInstructionNum();
                break;
            }
            if(instruction.getRightInstruction() != -1){
                instr = instruction.getInstructionNum();
                break;
            }
        }
        // generate branch for the current bb which is the doBB
        bb.addStatement(new Instruction(instructionNum, "bra", instr, -1));
        instructionNum++;

        if(lastSeenBB != null){
            lastSeenBB.getStatements().get(lastSeenBB.getStatements().size() - 1).setRightInstruction(bb.getStatements().get(0).getInstructionNum());
        }
        System.out.println("before replc " + bb.getBBNum() + " " + bb.getOpInstructionsHM());
        // run replace Algorithm to replace non-used instructions to original or for CSE
        replacementAlgorithm();

        checkFor(Tokens.odToken);

        basicBlockNum++;
        followBB = new BasicBlock(basicBlockNum);

        BBS.put(basicBlockNum, followBB);

        followBB.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
        followBB.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
        followBB.deepCopyArrayAddrTable(bb.getSymbolTable().arrayToAddr);
        followBB.deepCopyArrayDimensionTable(bb.getSymbolTable().arrayToDimensions);
        followBB.deepCopyStores(bb.getSymbolTable().hasStores);
        followBB.isJoin = true;
        //System.out.println("follow bb in while " + bb.getBBNum() + " " + bb.getOpInstructionsHM());

        if(bb.getRightBasicBlock() == null){
            bb.setRightBasicBlock(followBB);
        }

        if(whileBBS.size() != 0){
            if(followBB.getReturnToBasicBlock() == null){
                followBB.setReturnToBasicBlock(whileBBS.get(whileBBS.size() - 1));
                followBB.isWhile = true;
                lastSeenFollowBB = followBB;
            }
        }
        lastSeenBB = bb;
        bb = followBB;
    }
    public void returnStatement(){
        checkFor(Tokens.returnToken);
    }
    public Node relation(){
        System.out.println("in relation");
        Node expr1 = expression();
        if(inputSym == Tokens.eqlToken || inputSym == Tokens.neqToken ||
                inputSym == Tokens.lssToken || inputSym == Tokens.leqToken||
                inputSym == Tokens.gtrToken || inputSym == Tokens.geqToken){
            Node relOp = new Node(myTokenizer.symbolTable.getRelOp(inputSym), false);
            next(); // eat relOp
            Node expr2 = expression();
            System.out.println(inputSym);
            relOp.left = expr1;
            relOp.right = expr2;
            System.out.println("leaving relation");
            return relOp;
        }
        else{
            myTokenizer.Error("Expected relOp but missing");
        }
        return null; // no relOp
    }
    public void replacementAlgorithm(){
        bb = whileBBS.remove(whileBBS.size() - 1); // start replacement algorithm from the most recent while header
        System.out.println("after replc " + bb.getBBNum() + " " + bb.getOpInstructionsHM());
        if(lastSeenFollowBB != null){
            for(Map.Entry<String, Integer> entry : lastSeenFollowBB.getSymbolTable().getIdentifierToInstructionNumHM().entrySet()){
                if(bb.containsPhi(entry.getKey())){
                    int instr = bb.getIdentifierInstructionNum(entry.getKey());
                    for(int i = bb.numKills; i<bb.numKills + bb.numPhis; i++){
                        if(bb.getStatements().get(i).getInstructionNum() == instr){
                            bb.getStatements().get(i).setRightInstruction(entry.getValue());
                            break;
                        }
                    }
                }
                else{ // create new phi
                    bb.addPhi(bb.numPhis ,new Instruction(instructionNum, "phi", bb.getIdentifierInstructionNum(entry.getKey()), entry.getValue()));
                    bb.addToPhiTable(entry.getKey(), instructionNum);
                    bb.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                    instructionNum++;
                    bb.numPhis++;
                }
            }
            bb.getStatements().get(bb.getStatements().size() - 1).setRightInstruction(lastSeenFollowBB.getStatements().get(0).getInstructionNum());
        }
        if(lastSeenJoinBB != null){
            for(Map.Entry<String, Integer> entry : lastSeenJoinBB.getSymbolTable().getIdentifierToInstructionNumHM().entrySet()){
                if(bb.containsPhi(entry.getKey())){
                    int instr = bb.getIdentifierInstructionNum(entry.getKey());
                    for(int i = bb.numKills; i<bb.numKills + bb.numPhis; i++){
                        if(bb.getStatements().get(i).getInstructionNum() == instr){
                            if(bb.getStatements().get(i).getLeftInstruction() != entry.getValue()) {
                                bb.getStatements().get(i).setRightInstruction(entry.getValue());
                                break;
                            }
                        }
                    }
                }
                else{
                    bb.addPhi(bb.numPhis, new Instruction(instructionNum, "phi", bb.getIdentifierInstructionNum(entry.getKey()), entry.getValue()));
                    bb.addToPhiTable(entry.getKey(), instructionNum);
                    bb.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                    instructionNum++;
                    bb.numPhis++;
                }
            }
        }
        for(int i = bb.numKills; i<bb.numKills + bb.numPhis; i++){
            if(bb.getStatements().get(i).getRightInstruction() == -1 || bb.getStatements().get(i).getLeftInstruction() == bb.getStatements().get(i).getRightInstruction()){ // add to update map since this phi wasnt used so we can replace with the original value in whileBBS.peek() bb
                updateMap.put(bb.getStatements().get(i).getInstructionNum(), bb.getStatements().get(i).getLeftInstruction()); // left instruction will be the original instruction or the last used version
                bb.getStatements().remove(i);
                i--;
                bb.numPhis--;
            }
        }
        System.out.println("update map " + updateMap);

        while(!updateMap.isEmpty()) {
            while(!done) {
                done = true;
                System.out.println(updateMap);
                DFS(bb, new HashSet<>());
                System.out.println("done " + done);
            }
            updateMap = new HashMap<>();
        }
    }
    public void DFS(BasicBlock curr, HashSet<Integer> visited){
        if(curr == null || visited.contains(curr.getBBNum())) return;
        visited.add(curr.getBBNum());

        List<Instruction> instructions = curr.getStatements();
        for(Instruction instruction : instructions){
            if(instruction.getOpCode().equals("load")) continue;
            if(updateMap.containsKey(instruction.getLeftInstruction())){
                instruction.setLeftInstruction(updateMap.get(instruction.getLeftInstruction()));

                //perform CSE if needed and add change to updateMap
                if(curr.opInstructionExists(instruction.getOpCode(), instruction)){
                    updateMap.put(instruction.getInstructionNum(), curr.getOpInstructionNum());
                    instructions.remove(instruction);
                    //curr.removeOpInstruction(instruction.getOpCode(), instruction);
                    done = false;
                }
            }
            if(updateMap.containsKey(instruction.getRightInstruction())){
                instruction.setRightInstruction(updateMap.get(instruction.getRightInstruction()));

                // perform CSE if needed and add change to updateMap
                if(curr.opInstructionExists(instruction.getOpCode(), instruction)){
                    updateMap.put(instruction.getInstructionNum(), curr.getOpInstructionNum());
                    instructions.remove(instruction);
                    //curr.removeOpInstruction(instruction.getOpCode(), instruction);
                    done = false;
                }
            }
        }
        // update any use in the current blocks symbol table as well
        for(Map.Entry<String, Integer> entry : curr.getSymbolTable().getIdentifierToInstructionNumHM().entrySet()){
            if(updateMap.containsKey(entry.getValue())){
                entry.setValue(updateMap.get(entry.getValue()));
            }
        }
        DFS(curr.getLeftBasicBlock(), visited);
        DFS(curr.getRightBasicBlock(), visited);
    }
}