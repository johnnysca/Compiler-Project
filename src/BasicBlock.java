import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class BasicBlock {
    private List<Instruction> statements;
    private int BBNum;
    private SymbolTable symbolTable;
    private BasicBlock leftBasicBlock;
    private BasicBlock rightBasicBlock;
    private HashMap<String, LinkedList<Instruction>>  opInstructions;
    private int opInstructionNum; // store the instruction num for an op that already exists. used for CSE (common subexpression elimination)
    BasicBlock(){
        statements = new ArrayList<>();
        BBNum = 0;
        symbolTable = new SymbolTable();
        leftBasicBlock = null;
        rightBasicBlock = null;
        opInstructions = new HashMap<>();
        opInstructions.put("add", new LinkedList<>());
        opInstructions.put("sub", new LinkedList<>());
        opInstructions.put("mul", new LinkedList<>());
        opInstructions.put("div", new LinkedList<>());
    }
    BasicBlock(int BBNum){
        statements = new ArrayList<>();
        this.BBNum = BBNum;
        symbolTable = new SymbolTable();
        leftBasicBlock = null;
        rightBasicBlock = null;
        opInstructions = new HashMap<>();
        opInstructions.put("add", new LinkedList<>());
        opInstructions.put("sub", new LinkedList<>());
        opInstructions.put("mul", new LinkedList<>());
        opInstructions.put("div", new LinkedList<>());
    }
    public void addStatement(Instruction instruction){
        statements.add(instruction);
    }
    public SymbolTable getSymbolTable(){
        return symbolTable;
    }
    public String getOpCode(char opcode){
        return symbolTable.getOpcodeAsString(opcode);
    }
    public boolean constantinstructionExists(int val){ // for constants
        return symbolTable.checkIfInstructionExists(val);
    }
    public int getConstantInstructionNum(int key){
        return symbolTable.getInstructionForConstant(key);
    }
    public boolean identifierInstructionExists(String key){
        return symbolTable.checkIfIdentifierInstructionExists(key);
    }
    public int getIdentifierInstructionNum(String key){
        return symbolTable.getInstructionForIdentifier(key);
    }
    public void addConstantToSymbolTable(int key, int val){
        symbolTable.addConstantToInstructionNum(key, val);
    }
    public void addIdentifierToSymbolTable(String key, int val){
        symbolTable.addIdentifierToInstructionNum(key, val);
    }
    public List<Instruction> getStatements(){
        return statements;
    }
    public boolean opInstructionExists(String key, Instruction instruction){ // do not add duplicate SSA instructions to the opcode list, return the previous instruction defined before
        LinkedList<Instruction> ll = opInstructions.get(key);
        for(Instruction i : ll){
            if(checkIfSame(i, instruction)){
                System.out.println("true");
                opInstructionNum = i.getInstructionNum();
                return true;
            }
        }
        System.out.println("false");
        return false;
    }
    public void addOpInstruction(String key, Instruction value){ // add the instruction generated to the opcode Linked list
        LinkedList<Instruction> ll = opInstructions.get(key);
        ll.addFirst(value);
        opInstructions.put(key, ll);
    }
    public boolean checkIfSame(Instruction instruction1, Instruction instruction2){
        return instruction1.equals(instruction2);
    }
    public int getOpInstructionNum(){
        return opInstructionNum;
    }
    public void setLeftBasicBlock(BasicBlock basicBlock){
        leftBasicBlock = basicBlock;
    }
    public void setRightBasicBlock(BasicBlock basicBlock){
        rightBasicBlock = basicBlock;
    }
    public BasicBlock getLeftBasicBlock(){
        return leftBasicBlock;
    }
    public BasicBlock getRightBasicBlock(){
        return rightBasicBlock;
    }
    public HashMap<String, LinkedList<Instruction>> deepCopyOfOPInstructions(HashMap<String, LinkedList<Instruction>> toCopy){
        this.opInstructions = new HashMap<>(toCopy);
        return opInstructions;
    }
    public HashMap<String, LinkedList<Instruction>> getOpInstructionsHM(){
        return opInstructions;
    }
    public void deepCopyOfSymbolTable(HashMap<String, Integer> toCopy){
        this.symbolTable.createDeepCopySymbolTable(toCopy);
    }
    public int getBBNum(){
        return BBNum;
    }
}
