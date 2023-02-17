public class Instruction {
    private int instructionNum;
    private String opCode; // will be either 'c', '+', '-', '*', '/' ----- c stands for constant
    private int leftInstruction;
    private int rightInstruction;
    private int val; // a constants value
    private boolean isConstant;

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
    public int getLeftInstruction(){
        return leftInstruction;
    }
    public int getRightInstruction(){
        return rightInstruction;
    }
    public int getInstructionNum(){
        return instructionNum;
    }
    public boolean equals(Instruction instruction2){
        return this.getLeftInstruction() == instruction2.getLeftInstruction() && this.getRightInstruction() == instruction2.getRightInstruction();
    }
    public String toString(){
        return "InstructionNum: " + instructionNum + " opcode: " + opCode + " leftInstruction: " + leftInstruction + " rightInstruction: " + rightInstruction + " val: " + val;
    }
}

/*

    for constants store the constant value as the key and the instruction num as the val (put this in its own symbol table)
    for identifiers store the identifier name as the key and the instruction num as the val (put this in its own symbol table)
 */
