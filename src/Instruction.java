public class Instruction {
    private int instructionNum;
    private String opCode; // will be either 'c', '+', '-', '*', '/' ----- c stands for constant
    private int leftInstruction;
    private int rightInstruction;
    private int val; // a constants value
    private boolean isConstant;
    private BasicBlock basicBlock;

    Instruction(int instructionNum, String opCode, int leftInstruction, int rightInstruction){ // add the current
        this.instructionNum = instructionNum;
        //this.opCode = basicBlock.getOpCode(opCode);
        this.opCode = opCode;
        this.leftInstruction = leftInstruction;
        this.rightInstruction = rightInstruction;
        this.val = 0;
        this.isConstant = false;
    }
    Instruction(int instructionNum, String c, int val){ // for constants
        System.out.println("in constant constructor");
        this.instructionNum = instructionNum;
        //this.opCode = basicBlock.getOpCode(c);
        this.opCode = c;
        this.val = val;
        this.isConstant = true;
        System.out.println("leaving constant constructor");
    }

//    public Instruction(int instructionNum, String opCode, int leftInstruction, int rightInstruction) {
//        this.instructionNum = instructionNum;
//        this.opCode = opCode;
//        this.leftInstruction = leftInstruction;
//        this.rightInstruction = rightInstruction;
//        this.val = 0;
//        this.isConstant = false;
//    }

    public String toString(){
        return "InstructionNum: " + instructionNum + " opcode: " + opCode + " leftInstruction: " + leftInstruction + " rightInstruction: " + rightInstruction + " val: " + val;
    }
}

/*

    for constants store the constant value as the key and the instruction num as the val (put this in its own symbol table)
    for identifiers store the identifier name as the key and the instruction num as the val (put this in its own symbol table)
 */
