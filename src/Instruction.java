public class Instruction {
    private int instructionNum;
    private String opCode; // const, add, sub, mul, div, phi, adda, load, store
    private int leftInstruction;
    private int rightInstruction;
    private int val; // a constants value
    private boolean isConstant;
    private String base; // base for arrays

    Instruction(int instructionNum, String opCode, int leftInstruction, int rightInstruction){ // add the current
        this.instructionNum = instructionNum;
        this.opCode = opCode;
        this.leftInstruction = leftInstruction;
        this.rightInstruction = rightInstruction;
        this.val = 0;
        this.isConstant = false;
    }
    Instruction(int instructionNum, String c, int val){ // for constants
        this.instructionNum = instructionNum;
        this.opCode = c;
        this.leftInstruction = -1; // to avoid confusion with actual instruction 0
        this.rightInstruction = -1;
        this.val = val;
        this.isConstant = true;
    }
    Instruction(int instructionNum, String c, String base){ // for base array
        this.instructionNum = instructionNum;
        this.opCode = c;
        this.base = base;
    }
    Instruction(int instructionNum, String kill){ // for kill instructions
        this.instructionNum = instructionNum;
        this.opCode = kill;
        this.leftInstruction = -1;
        this.rightInstruction = -1;
    }
    public String getOpCode(){
        return opCode;
    }
    public int getLeftInstruction(){
        return leftInstruction;
    }
    public int getRightInstruction(){
        return rightInstruction;
    }
    public int getInstructionNum(){
        return instructionNum;
    }
    public void setLeftInstruction(int instruction){
        this.leftInstruction = instruction;
    }
    public void setRightInstruction(int instruction){
        this.rightInstruction = instruction;
    }
    public int getVal(){
        return val;
    }
    public String getBase(){
        return base;
    }
    public boolean equals(Instruction instruction2){
        return this.getLeftInstruction() == instruction2.getLeftInstruction() && this.getRightInstruction() == instruction2.getRightInstruction();
    }
    public String toString(){
        return "InstructionNum: " + instructionNum + " opcode: " + opCode + " leftInstruction: " + leftInstruction + " rightInstruction: " + rightInstruction + " base: " + base + " val: " + val;
    }
}

/*

    for constants store the constant value as the key and the instruction num as the val (put this in its own symbol table)
    for identifiers store the identifier name as the key and the instruction num as the val (put this in its own symbol table)
 */
