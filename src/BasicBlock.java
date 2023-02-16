import java.util.ArrayList;
import java.util.List;

public class BasicBlock {
    private List<Instruction> statements;
    private SymbolTable symbolTable;
    private BasicBlock leftBasicBlock;
    private BasicBlock rightBasicBlock;
    BasicBlock(){
        statements = new ArrayList<>();
        symbolTable = new SymbolTable();
        leftBasicBlock = null;
        rightBasicBlock = null;
    }
    public void addStatement(Instruction instruction){
        statements.add(instruction);
    }
    public BasicBlock getLeftBasicBlock(){
        return leftBasicBlock;
    }
    public BasicBlock getRightBasicBlock(){
        return rightBasicBlock;
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
//    public String toString(){
//        return
//    }
}
