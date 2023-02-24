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
        System.out.println(inputSym);
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

            //bb = BBS.get(basicBlockNum);

            if (!expr.constant) { // root not a constant meaning its a +, -, *, / or identifier. constants would have already generated their instruction in factor
                System.out.println("root not a constant " + expr.data);
                if (expr.left == null) { // if the root is an identifier
                    System.out.println("no left expr " + expr.left);
                    if (!bb.identifierInstructionExists((String) expr.data)) {
                        if (!BBS.get(0).constantinstructionExists(0)) {
                            //bb = BBS.get(0);
                            System.out.println("No value was assigned to variable. Will be defaulted to 0");
                            BBS.get(0).addConstantToSymbolTable(0, instructionNum);
                            BBS.get(0).addStatement(new Instruction(instructionNum, bb.getOpCode('c'), 0)); // c just means constant
                            instructionNum++;
                        }
                        //bb = BBS.get(basicBlockNum);
                        bb.addIdentifierToSymbolTable((String) desNode.data, BBS.get(0).getConstantInstructionNum(0));
                    } else {
                        rootInstruction = bb.getIdentifierInstructionNum((String) expr.data);
                        System.out.println("root instr " + rootInstruction);
                        bb.addIdentifierToSymbolTable((String) desNode.data, rootInstruction);
                    }
                    return;
                }
                if (!expr.left.constant) { // if its an identifier check to see if an instruction was previously generated
                    System.out.println("left not a constant " + expr.left.data);
                    if (!bb.identifierInstructionExists((String) expr.left.data)) { // identifier used before it was assigned a value default to 0
                        System.out.println("left has no identifier instruction");
                        if (!BBS.get(0).constantinstructionExists(0)) { // default val to 0
                            //bb = BBS.get(0);
                            System.out.println("No value was assigned to variable. Will be defaulted to 0");
                            BBS.get(0).addConstantToSymbolTable(0, instructionNum);
                            BBS.get(0).addStatement(new Instruction(instructionNum, bb.getOpCode('c'), 0)); // c just means constant
                            instructionNum++;
                        }
                        //bb = BBS.get(basicBlockNum);
                        bb.addIdentifierToSymbolTable((String) expr.left.data, BBS.get(0).getConstantInstructionNum(0));
                        leftInstruction = bb.getIdentifierInstructionNum((String) expr.left.data);
                    } else {
                        System.out.println("left has instruction for ident");
                        leftInstruction = bb.getIdentifierInstructionNum((String) expr.left.data);
                        System.out.println("done getting left instruction");
                    }
                } else {
                    System.out.println("left is a constant");
                    leftInstruction = BBS.get(0).getConstantInstructionNum((int) expr.left.data); // left side is a constant
                    System.out.println("done with left constant");
                }
                if (!expr.right.constant) { // right is an identifier
                    System.out.println("right not a constant " + expr.right.data);
                    if (!bb.identifierInstructionExists((String) expr.right.data)) { // unassigned identifier on right side
                        System.out.println("right has no identifier instruction");
                        if (!BBS.get(0).constantinstructionExists(0)) { // default identifier value 0
                            //bb = BBS.get(0);
                            System.out.println("No value was assigned to variable. Will be defaulted to 0");
                            BBS.get(0).addConstantToSymbolTable(0, instructionNum);
                            BBS.get(0).addStatement(new Instruction(instructionNum, bb.getOpCode('c'), 0)); // c just means constant
                            instructionNum++;
                        }
                        //bb = BBS.get(basicBlockNum);
                        bb.addIdentifierToSymbolTable((String) expr.right.data, BBS.get(0).getConstantInstructionNum(0));
                        rightInstruction = bb.getIdentifierInstructionNum((String) expr.right.data);
                    } else {
                        System.out.println("right has instruction for ident");
                        rightInstruction = bb.getIdentifierInstructionNum((String) expr.right.data);
                        System.out.println("done getting right instruction");
                    }
                } else {
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
                } else {
                    bb.addIdentifierToSymbolTable((String) desNode.data, bb.getOpInstructionNum());
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
            //bb = BBS.get(basicBlockNum);
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
            bb.addStatement(new Instruction(instructionNum, "write", -1, -1));
            bb.addIdentifierToSymbolTable(seenIdent, instructionNum);
            instructionNum++;

            System.out.println("before");
            checkFor(Tokens.openParenToken);
            System.out.println("after");
            seenIdent = myTokenizer.getIdentifier();
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

        bb = ifBBS.pop();
        if(joinBlocksNeeded <= 0) return;

        if(inputSym == Tokens.elseToken){
            next(); // eat else
            if(joinBlocksNeeded <= 0) return;
            basicBlockNum++;
            elseBB = new BasicBlock(basicBlockNum);
            elseBB.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
            elseBB.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
            BBS.put(basicBlockNum, elseBB);
            if(bb.getRightBasicBlock() == null){
                bb.setRightBasicBlock(elseBB);
            }
            ifBBS.push(bb);
            bb = elseBB;
            statSequence();
            if(elseBB.getStatements().size() == 0){ // generate empty instruction
                elseBB.addStatement(new Instruction(instructionNum, "<empty>", -1, -1));
                instructionNum++;
            }
            bb = ifBBS.pop();
        }

        if(inputSym == Tokens.fiToken){
            next(); // eat fi
            // generate join Block
            if(joinBlocksNeeded <= 0) return;
            basicBlockNum++;
            joinBB = new BasicBlock(basicBlockNum);
            joinBB.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
            joinBB.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
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
                            instructionNum++;
                        }
                    }
                    lastSeenJoinBB = joinBB;
                    return;
                }
                HashMap<String, Integer> ifHM = ifBB.getSymbolTable().getIdentifierToInstructionNumHM();
                HashMap<String, Integer> elseHM = elseBB.getSymbolTable().getIdentifierToInstructionNumHM();

                for(Map.Entry<String, Integer> entry : ifHM.entrySet()){
                    if(entry.getValue() != elseHM.get(entry.getKey())){
                        joinBB.addStatement(new Instruction(instructionNum, "phi", entry.getValue(), elseHM.get(entry.getKey())));
                        joinBB.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                }
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
                        instructionNum++;
                    }
                }
            }
            if(lastSeenJoinBB != null){ // merge two joins to one
                if(joinBlocksNeeded < 0) return;
                basicBlockNum++;
                BasicBlock mergedJoin = new BasicBlock(basicBlockNum);
                mergedJoin.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
                mergedJoin.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
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
                        instructionNum++;
                    }
                }
                lastSeenJoinBB = mergedJoin;
            }
            else{
                lastSeenJoinBB = joinBB;
            }
        }
        // join if and else blocks or join if and parent if theres no else does Dead Code Elimination in the process
    }
    public void whileStatement(){

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
}