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
    private Stack<BasicBlock> ifBBS;
    private Stack<BasicBlock> joinBBS;
    private BasicBlock lastSeenJoinBB;
    private int joinBlocksNeeded;
    //private Stack<BasicBlock> whileBBS;
    private List<BasicBlock> whileBBS;
    private HashMap<Integer, Integer> updateMap;
    private BasicBlock lastSeenFollowBB;
    private boolean done; // for Replacement Algorithm to stop
    private BasicBlock lastSeenBB;
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
        System.out.println(inputSym);
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
            myTokenizer.symbolTable.insertToSymbolTable(seenIdent, 0);
            next();
        }
        while(inputSym == Tokens.commaToken){
            next();
            seenIdent = myTokenizer.getIdentifier();
            myTokenizer.symbolTable.insertToSymbolTable(seenIdent, 0);
            next();
        }
        checkFor(Tokens.semiToken);
    }
    public void typeDecl(){
        if(inputSym == Tokens.varToken){
            next();
        }
        else if(inputSym == Tokens.arrToken){
            next();
            checkFor(Tokens.openbracketToken);
            if(inputSym == Tokens.number){
                val = myTokenizer.getNumber();
                next();
            }
            checkFor(Tokens.closebracketToken);
            while(inputSym == Tokens.openbracketToken){
                next();
                val = myTokenizer.getNumber();
                next();
                checkFor(Tokens.closebracketToken);
            }
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
        //System.out.println(inputSym);
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
//        else{
//            myTokenizer.Error("Expected let, if, while, or return statement");
//        }
    }
    public void assignment(){
        next(); // eat let
        int leftInstruction, rightInstruction, rootInstruction;
        if(inputSym == Tokens.ident) {
            System.out.println("going to designator");
            Node desNode = designator(); // designator node will contain LHS of <-
            System.out.println("back from designator");
            System.out.println(desNode.data);
            checkFor(Tokens.becomesToken);

            System.out.println("going to expression");
            Node expr = expression(); // will contain a tree of the expression we saw
            System.out.println("back from expression");
            if(expr == null) return;
            System.out.println(expr.data);
            if (expr.left != null)
                System.out.println(expr.left.data);
            else System.out.println(expr.left);
            if (expr.right != null)
                System.out.println(expr.right.data);
            else System.out.println(expr.right);

            if (!expr.constant) { // root not a constant meaning its a +, -, *, / or identifier. constants would have already generated their instruction in factor
                System.out.println("root not a constant " + expr.data);
                if (expr.left == null) { // if the root is an identifier
                    //if (bb.isWhile) { // if came from while block structure create a phi in while header
                    if(whileBBS.size() != 0){
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
                            bb.addIdentifierToSymbolTable((String) desNode.data, rootInstruction);
                        }
                        return;
                    }
                }
                if (!expr.left.constant) { // if its an identifier check to see if an instruction was previously generated
                    System.out.println("left not a constant " + expr.left.data);
                    //if (bb.isWhile) { // came from a while header
                    if(whileBBS.size() != 0){
                        BasicBlock whileBB = whileBBS.get(whileBBS.size() - 1);
                        if(!whileBB.containsPhi((String) expr.left.data)) {
                            // have to search all previous whileBBSS to see if theres a phi and use the phis right instruction if not == -1 else use the instructionNum of that phi
                            leftInstruction = whileBBS.get(whileBBS.size() - 1).getIdentifierInstructionNum((String) expr.left.data);
                            for (int i = whileBBS.size() - 1; i >= 0; i--) {
                                boolean found = false;
                                if (whileBBS.get(i).containsPhi((String) expr.left.data)) {
                                    int instr = whileBBS.get(i).getIdentifierInstructionNum((String) expr.left.data);
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
                            whileBB.addToPhiTable((String) expr.left.data, instructionNum);
                            whileBB.addIdentifierToSymbolTable((String) expr.left.data, instructionNum);
                            instructionNum++;
                            whileBB.numPhis++;
                        }
                        leftInstruction = whileBB.getIdentifierInstructionNum((String) expr.left.data);
                    } else { // didnt come from while header
                        if (!bb.identifierInstructionExists((String) expr.left.data)) { // identifier used before it was assigned a value default to 0
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
                        BasicBlock whileBB = whileBBS.get(whileBBS.size() - 1);
                        if(!whileBB.containsPhi((String) expr.right.data)) {
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
                            whileBB.addToPhiTable((String) expr.right.data, instructionNum);
                            whileBB.addIdentifierToSymbolTable((String) expr.right.data, instructionNum);
                            instructionNum++;
                            whileBB.numPhis++;
                        }
                        rightInstruction = whileBB.getIdentifierInstructionNum((String) expr.right.data);
                    } else {
                        if (!bb.identifierInstructionExists((String) expr.right.data)) { // unassigned identifier on right side
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
                Instruction instruction = new Instruction(instructionNum, bb.getOpCode((Character) expr.data), leftInstruction, rightInstruction);
                if (!bb.opInstructionExists(bb.getOpCode((Character) expr.data), instruction)) { // instruction doesnt already exists in opcode LinkedList, then add it
                    bb.addOpInstruction(bb.getOpCode((Character) expr.data), instruction);
                    bb.addStatement(new Instruction(instructionNum, bb.getOpCode((Character) expr.data), leftInstruction, rightInstruction));
                    bb.addIdentifierToSymbolTable((String) desNode.data, instructionNum);
                    System.out.println(desNode.data + " " + bb.getIdentifierInstructionNum((String) desNode.data));
                    instructionNum++;
                }
                else {
                    bb.addIdentifierToSymbolTable((String) desNode.data, bb.getOpInstructionNum());
                }
                //if(bb.isWhile){
                if(whileBBS.size() != 0){
                    BasicBlock whileBB = whileBBS.get(whileBBS.size() - 1);
                    if(!whileBB.containsPhi((String) desNode.data)){ // not a phi in while header block
                        whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", whileBB.getIdentifierInstructionNum((String) desNode.data), bb.getIdentifierInstructionNum((String) desNode.data)));
                        whileBB.addToPhiTable((String) desNode.data, instructionNum);
                        whileBB.addIdentifierToSymbolTable((String) desNode.data, instructionNum);
                        instructionNum++;
                        whileBB.numPhis++;
                    }
                    else{ // phi already exists in while header so update the right instruction to be the value in the bb symbol table for desNode
                        int phi = whileBB.getIdentifierInstructionNum((String) desNode.data); // phi we are looking for to update the right instruction
                        for(int i = 0; i<whileBB.numPhis; i++){
                            if(whileBB.getStatements().get(i).getInstructionNum() == phi){
                                whileBB.getStatements().get(i).setRightInstruction(bb.getIdentifierInstructionNum((String) desNode.data));
                            }
                        }
                    }
                }
            } else {
                System.out.println(desNode.data);
                System.out.println(expr.data);
                System.out.println(BBS.get(0).constantinstructionExists((int) expr.data));
                bb.addIdentifierToSymbolTable((String) desNode.data, BBS.get(0).getConstantInstructionNum((int) expr.data));
            }
            // generate the instruction based on the expr we got and add it to the basic block
            // then add key: desNode.data, value: instruction number for the instruction we generated from expr and add that to the BasicBlock symbol table
        }
        else{
            myTokenizer.Error("Expected identifier");
        }
        System.out.println("leaving assignment");
    }
    public Node designator(){
        seenIdent = myTokenizer.getIdentifier();
        next();
        while(inputSym == Tokens.openbracketToken){
            next();
            expression();
            checkFor(Tokens.closebracketToken);
        }
        return new Node(seenIdent, false);
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
            val = scanner.nextInt();
            BasicBlock basicBlock0 = BBS.get(0);
            if(!basicBlock0.constantinstructionExists(val)){ // instruction not created for constant before so create it
                basicBlock0.addConstantToSymbolTable(val, instructionNum);
                basicBlock0.addStatement(new Instruction(instructionNum, basicBlock0.getOpCode('c'), val)); // c just means constant
                instructionNum++;
            }
            bb.addStatement(new Instruction(instructionNum, "read", -1, -1));
            bb.addIdentifierToSymbolTable(seenIdent, instructionNum);
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
            bb.addStatement(new Instruction(instructionNum, "write", bb.getIdentifierInstructionNum(seenIdent), -1));
            bb.addIdentifierToSymbolTable(seenIdent, instructionNum);
            instructionNum++;
            System.out.println(seenIdent);
            next(); // eat identifier
            checkFor(Tokens.closeParenToken);
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
        System.out.println((String) rel.data);
        System.out.println((String) rel.left.data);
        System.out.println((int) rel.right.data);

        System.out.println(rel.left.constant);
        System.out.println(BBS.get(0).getConstantInstructionNum((int) rel.right.data));
        // get left and right instructions and generate cmp instruction
        int leftInstruction = rel.left.constant ? BBS.get(0).getConstantInstructionNum((int)rel.left.data) : bb.getIdentifierInstructionNum((String) rel.left.data);
        int rightInstruction = rel.right.constant ? BBS.get(0).getConstantInstructionNum((int) rel.right.data) : bb.getIdentifierInstructionNum((String) rel.right.data);
        System.out.println("here");
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
        ifBB.isThen = true;

        BBS.put(basicBlockNum, ifBB);

        if(bb.getLeftBasicBlock() == null){
            bb.setLeftBasicBlock(ifBB);
        }
        ifBBS.push(bb); // store current basic block, will be used to set any elses
        bb = ifBB;

        checkFor(Tokens.thenToken);

        statSequence();

        // generate branch for current block
        bb.addStatement(new Instruction(instructionNum, "bra", -1, -1));
        instructionNum++;

        if(lastSeenBB != null){
            lastSeenBB.getStatements().get(lastSeenBB.getStatements().size() - 1).setRightInstruction(bb.getStatements().get(0).getInstructionNum());
            lastSeenBB = null;
        }
        ifBB = bb;
        bb = ifBBS.pop();
        System.out.println("bb if" + bb.getBBNum() + " " + ifBB.getBBNum());
        System.out.println(bb.getOpInstructionsHM());
        System.out.println(ifBB.getOpInstructionsHM());
        if(joinBlocksNeeded <= 0) return;

        if(inputSym == Tokens.elseToken){
            next(); // eat else
            if(joinBlocksNeeded <= 0) return;
            basicBlockNum++;
            elseBB = new BasicBlock(basicBlockNum);
            elseBB.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
            System.out.println("elsebb op inst " + elseBB.getOpInstructionsHM());
            elseBB.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
            System.out.println("ifbb op inst " + bb.getOpInstructionsHM());
            elseBB.isElse = true;
            BBS.put(basicBlockNum, elseBB);
            if(bb.getRightBasicBlock() == null){
                bb.setRightBasicBlock(elseBB);
            }
            BasicBlock parent = bb;
            bb = elseBB;
            System.out.println("going to stat seq from else in if " + elseBB.getBBNum() + " " + parent.getBBNum() + " " + parent.getStatements().size() + " " + parent.getOpInstructionsHM());
            statSequence();
            if(elseBB.getStatements().size() == 0){ // generate empty instruction
                elseBB.addStatement(new Instruction(instructionNum, "empty", -1, -1));
                instructionNum++;
            }

            Instruction instruction = parent.getStatements().get(parent.getStatements().size()-1);
            instruction.setRightInstruction(elseBB.getStatements().get(0).getInstructionNum());
            parent.getStatements().set(parent.getStatements().size()-1, instruction);
        }

        if(inputSym == Tokens.fiToken){
            next(); // eat fi
            // generate join Block
            if(joinBlocksNeeded <= 0) return;
            basicBlockNum++;
            joinBB = new BasicBlock(basicBlockNum);
            joinBB.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
            joinBB.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
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

                for(Map.Entry<String, Integer> entry : ifHM.entrySet()){
                    if(entry.getValue() != parentHM.get(entry.getKey())){
                        joinBB.addStatement(new Instruction(instructionNum, "phi", entry.getValue(), parentHM.get(entry.getKey())));
                        joinBB.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                        joinBB.addToPhiTable(entry.getKey(), instructionNum);
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

        if(bb.getLeftBasicBlock() == null){
            bb.setLeftBasicBlock(whileBB);
        }
        Node rel = relation();

        int leftInstruction, rightInstruction;
        // generate phis in while header if used
        if(!rel.left.constant){
            if(whileBBS.size() != 0 && !whileBB.containsPhi((String) rel.left.data)){
                // have to search all previous whileBBSS to see if theres a phi and use the phis right instruction if not == -1 else use the instructionNum of that phi
                leftInstruction = whileBBS.get(whileBBS.size() - 1).getIdentifierInstructionNum((String) rel.left.data);
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
            whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", leftInstruction, -1));
            whileBB.addToPhiTable((String) rel.left.data, instructionNum);
            whileBB.addIdentifierToSymbolTable((String) rel.left.data, instructionNum);
            instructionNum++;
            whileBB.numPhis++;
            leftInstruction = whileBB.getIdentifierInstructionNum((String) rel.left.data);
        }
        else{
            leftInstruction = BBS.get(0).getConstantInstructionNum((int) rel.left.data);
        }
        if(!rel.right.constant){
            if(!whileBB.containsPhi((String) rel.right.data)) {
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
            whileBB.addPhi(whileBB.numPhis, new Instruction(instructionNum, "phi", leftInstruction, -1));
            whileBB.addToPhiTable((String) rel.left.data, instructionNum);
            whileBB.addIdentifierToSymbolTable((String) rel.left.data, instructionNum);
            instructionNum++;
            whileBB.numPhis++;
            rightInstruction = whileBB.getIdentifierInstructionNum((String) rel.right.data);
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

        if(whileBB.getLeftBasicBlock() == null){
            whileBB.setLeftBasicBlock(doBB);
        }
        if(doBB.getReturnToBasicBlock() == null){
            doBB.setReturnToBasicBlock(whileBB);
        }

        whileBBS.add(whileBB);
        bb = doBB;

        statSequence();

        int instr = -1;
        for(Instruction instruction : whileBBS.get(whileBBS.size() - 1).getStatements()){
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
        followBB.isJoin = true;
        System.out.println("follow bb in while " + bb.getBBNum() + " " + bb.getOpInstructionsHM());

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
                    for(int i = 0; i<bb.numPhis; i++){
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
                    for(int i = 0; i<bb.numPhis; i++){
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
        for(int i = 0; i<bb.numPhis; i++){
            if(bb.getStatements().get(i).getRightInstruction() == -1){ // add to update map since this phi wasnt used so we can replace with the original value in whileBBS.peek() bb
                updateMap.put(bb.getStatements().get(i).getInstructionNum(), bb.getStatements().get(i).getLeftInstruction()); // left instruction will be the original instruction or the last used version
                bb.getStatements().remove(i);
                i--;
                bb.numPhis--;
            }
        }

        System.out.println("update map " + updateMap);

        int iters = 0;
        while(!updateMap.isEmpty()) {
            while(!done) {
                done = true;
                System.out.println("iter " + iters++);
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