import java.util.*;

public class Parser {
    private Tokenizer myTokenizer;
    private int inputSym;
    private String seenIdent; // store identifier just seen
    private int val; // store value just seen
    private int instructionNum; // to track what instruction number this new instruction will be
    private BasicBlock bb; // current Basic Block obj
    private int basicBlockNum; // current Basic Block number
    private HashMap<Integer, List<Integer>> BBMapping; // key: Basic Block Num, val: list of children Basic Blocks
    public HashMap<Integer, BasicBlock> BBS; // key: Basic Block Num, val: actual Basic Block
    private Scanner scanner;
    private Stack<BasicBlock> ifBBS;
    private Stack<BasicBlock> joinBBS;
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
        else myTokenizer.Error("SyntaxErr, Expected '" + myTokenizer.symbolTable.getStringFromToken(token) + "' but missing");
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
            bb = joinBBS.pop();
        }
        else if(inputSym == Tokens.whileToken){
            whileStatement();
        }
        else if(inputSym == Tokens.returnToken){
            returnStatement();
        }
        else{
            myTokenizer.Error("Expected let, if, while, or return statement");
        }
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

            bb = BBS.get(basicBlockNum);

            if (!expr.constant) { // root not a constant meaning its a +, -, *, / or identifier. constants would have already generated their instruction in factor
                System.out.println("root not a constant " + expr.data);
                if (expr.left == null) { // if the root is an identifier
                    System.out.println("no left expr " + expr.left);
                    if (!bb.identifierInstructionExists((String) expr.data)) {
                        if (!bb.constantinstructionExists(0)) {
                            bb = BBS.get(0);
                            System.out.println("No value was assigned to variable. Will be defaulted to 0");
                            bb.addConstantToSymbolTable(0, instructionNum);
                            bb.addStatement(new Instruction(instructionNum, bb.getOpCode('c'), 0)); // c just means constant
                            instructionNum++;
                        }
                        bb = BBS.get(basicBlockNum);
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
                        if (!bb.constantinstructionExists(0)) { // default val to 0
                            bb = BBS.get(0);
                            System.out.println("No value was assigned to variable. Will be defaulted to 0");
                            bb.addConstantToSymbolTable(0, instructionNum);
                            bb.addStatement(new Instruction(instructionNum, bb.getOpCode('c'), 0)); // c just means constant
                            instructionNum++;
                        }
                        bb = BBS.get(basicBlockNum);
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
                        if (!bb.constantinstructionExists(0)) { // default identifier value 0
                            bb = BBS.get(0);
                            System.out.println("No value was assigned to variable. Will be defaulted to 0");
                            bb.addConstantToSymbolTable(0, instructionNum);
                            bb.addStatement(new Instruction(instructionNum, bb.getOpCode('c'), 0)); // c just means constant
                            instructionNum++;
                        }
                        bb = BBS.get(basicBlockNum);
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
            bb.addStatement(new Instruction(instructionNum, "InputNum", -1, -1));
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
            bb.addStatement(new Instruction(instructionNum, "OutputNum", -1, -1));
            bb.addIdentifierToSymbolTable(seenIdent, instructionNum);
            instructionNum++;

            System.out.println("before");
            checkFor(Tokens.openParenToken);
            System.out.println("after");
            seenIdent = myTokenizer.getIdentifier();
            System.out.println(seenIdent);
            System.out.println(bb.identifierInstructionExists(seenIdent));
            System.out.println(bb.getIdentifierInstructionNum(seenIdent));
            System.out.println("affffff");
            next(); // eat identifier
            checkFor(Tokens.closeParenToken);
        }
        else if(inputSym == Tokens.outputNewLine){
            next();
            bb.addStatement(new Instruction(instructionNum, "OutputNewLine", -1, -1));
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

        ifBBS.push(bb); // store current bb on stack. Wil be used to link ifs and elses later
        // process if block
        basicBlockNum++; // new BB for then
        ifBB = new BasicBlock(basicBlockNum);
        BBS.put(basicBlockNum, ifBB);
        bb.setLeftBasicBlock(ifBB); // left child of parent will be if block

        // create deep copy of CSE table
        ifBB.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
        ifBB.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());

        checkFor(Tokens.thenToken);

        System.out.println("going back to statSequence from ifStatement");
        statSequence(); // will return once if is done
        System.out.println("back from statSequence in ifStatement");
        // add branching instruction to skip else
        bb.addStatement(new Instruction(instructionNum, "bra", -1, -1)); // will branch to the first phi in join. will reassign later

        bb = ifBBS.pop(); // get the parent block of if/else blocks

        if(inputSym == Tokens.elseToken){
            next(); // eat else
            basicBlockNum++;
            elseBB = new BasicBlock(basicBlockNum);
            BBS.put(basicBlockNum, elseBB);
            bb.setRightBasicBlock(elseBB); // right child of parent will be else block
            // create deep copy of CSE table
            elseBB.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
            elseBB.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
            statSequence();
        }

        if(inputSym == Tokens.fiToken){ // will need to generate join block and fill in
            System.out.println("here");
            next(); // eat fi
            basicBlockNum++;
            joinBB = new BasicBlock(basicBlockNum);
            BBS.put(basicBlockNum, joinBB);
            ifBB.setLeftBasicBlock(joinBB); // if's left child will be join block
            joinBB.deepCopyOfOPInstructions(bb.getOpInstructionsHM());
            joinBB.deepCopyOfSymbolTable(bb.getSymbolTable().getIdentifierToInstructionNumHM());
            System.out.println("here2");
            if(elseBB != null){ // there was an else block generated
                System.out.println("in fi elseBB != null");
                elseBB.setLeftBasicBlock(joinBB); // else's left child will be join block, compare if and else symbol tables to generate phis
                // now compare symbol tables to generate phis
                HashMap<String, Integer> ifHM = ifBB.getSymbolTable().getIdentifierToInstructionNumHM();
                HashMap<String, Integer> elseHM = elseBB.getSymbolTable().getIdentifierToInstructionNumHM();
                HashMap<String, Integer> parentHM = bb.getSymbolTable().getIdentifierToInstructionNumHM();

                // compare if to else or parent
                for(Map.Entry<String, Integer> entry : ifHM.entrySet()){
                    if(elseHM.containsKey(entry.getKey()) && elseHM.get(entry.getKey()) != entry.getValue()){
                        joinBB.addStatement(new Instruction(instructionNum, "phi", entry.getValue(), elseHM.get(entry.getKey())));
                        joinBB.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                    else if(!elseHM.containsKey(entry.getKey())){ // compare with parent
                        if(parentHM.containsKey(entry.getKey()) && parentHM.get(entry.getKey()) != entry.getValue()){
                            joinBB.addStatement(new Instruction(instructionNum, "phi", entry.getValue(), parentHM.get(entry.getKey())));
                            joinBB.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                            instructionNum++;
                        }
                    }
                }

                // compare else to if or parent
                for(Map.Entry<String, Integer> entry : elseHM.entrySet()){
                    if(ifHM.containsKey(entry.getKey()) && ifHM.get(entry.getKey()) != entry.getValue()){
                        joinBB.addStatement(new Instruction(instructionNum, "phi", ifHM.get(entry.getKey()), entry.getValue()));
                        joinBB.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                    else if(!ifHM.containsKey(entry.getKey())){
                        if(parentHM.containsKey(entry.getKey()) && parentHM.get(entry.getKey()) != entry.getValue()){
                            joinBB.addStatement(new Instruction(instructionNum, "phi", parentHM.get(entry.getKey()), entry.getValue()));
                            joinBB.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                            instructionNum++;
                        }
                    }
                }
            }
            else{ // there was no else, so parent symbol table will be compared to left child to generate phis
                System.out.println("here3 else");
                bb.setRightBasicBlock(joinBB);
                // now compare symbol tables to generate phis
                HashMap<String, Integer> parentHM = bb.getSymbolTable().getIdentifierToInstructionNumHM();
                HashMap<String, Integer> ifHM = ifBB.getSymbolTable().getIdentifierToInstructionNumHM();
                System.out.println("t");

                System.out.println("everything in ifHM");
                for(Map.Entry<String, Integer> entry : ifHM.entrySet()){
                    System.out.println(entry.getKey() + " " + entry.getValue());
                }

                System.out.println("\neverything in parentHM");
                for(Map.Entry<String, Integer> entry : parentHM.entrySet()){
                    System.out.println(entry.getKey() + " " + entry.getValue());
                }


                for(Map.Entry<String, Integer> entry : ifHM.entrySet()){
                    if(entry.getValue() != parentHM.get(entry.getKey())){
                        joinBB.addStatement(new Instruction(instructionNum, "phi", entry.getValue(), parentHM.get(entry.getKey())));
                        joinBB.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                }
            }
            System.out.println("r");
            if(!joinBBS.isEmpty()){
                // join both basic blocks and compare symbol tables
                System.out.println("ex");
                HashMap<String, Integer> prevJoinHM = joinBBS.pop().getSymbolTable().getIdentifierToInstructionNumHM();
                HashMap<String, Integer> currJoinHM = joinBB.getSymbolTable().getIdentifierToInstructionNumHM();
                HashMap<String, Integer> parentHM = bb.getSymbolTable().getIdentifierToInstructionNumHM();

                basicBlockNum++;
                BasicBlock mergedJoin = new BasicBlock(basicBlockNum); // join containing left and right joins

                for(Map.Entry<String, Integer> entry : prevJoinHM.entrySet()){ // generating phis by comparing preJoin to currJoin or parent
                    if(!currJoinHM.containsKey(entry.getKey())){
                        if(parentHM.containsKey(entry.getKey()) && parentHM.get(entry.getKey()) != entry.getValue()) {
                            mergedJoin.addStatement(new Instruction(instructionNum, "phi", entry.getValue(), parentHM.get(entry.getKey())));
                            mergedJoin.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                            instructionNum++;
                        }
                    }
                    else if(currJoinHM.containsKey(entry.getKey()) && currJoinHM.get(entry.getKey()) != entry.getValue()){
                        mergedJoin.addStatement(new Instruction(instructionNum, "phi", entry.getValue(), currJoinHM.get(entry.getKey())));
                        mergedJoin.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                }

                for(Map.Entry<String, Integer> entry : currJoinHM.entrySet()){ // generating phis by comparing currJoin to prevJoin or parent
                    if(!prevJoinHM.containsKey(entry.getKey())){
                        if(parentHM.containsKey(entry.getKey()) && parentHM.get(entry.getKey()) != entry.getValue()){
                            mergedJoin.addStatement(new Instruction(instructionNum, "phi", parentHM.get(entry.getKey()), entry.getValue()));
                            mergedJoin.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                            instructionNum++;
                        }
                    }
                    else if(prevJoinHM.containsKey(entry.getKey()) && prevJoinHM.get(entry.getKey()) != entry.getValue()){
                        mergedJoin.addStatement(new Instruction(instructionNum, "phi", prevJoinHM.get(entry.getKey()), entry.getValue()));
                        mergedJoin.addIdentifierToSymbolTable(entry.getKey(), instructionNum);
                        instructionNum++;
                    }
                }
                joinBBS.push(mergedJoin);
                System.out.println("expec");
            }
            else joinBBS.push(joinBB);
        }
        else{
            myTokenizer.Error("SyntaxErr missing fi");
        }
        System.out.println("out");


        // process else block if any

        // join if and else blocks or join if and parent if theres no else
    }
    public void whileStatement(){

    }
    public void returnStatement(){

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