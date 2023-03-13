import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.*;

public class Main {
    static HashSet<Integer> seen = new HashSet<>();
    static Parser parser;
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter filename: ");
        String filename = sc.nextLine();
        parser = new Parser(filename);

        for(int i = 0; i<parser.BBS.size(); i++){
            System.out.println(i + " bb: left: right");
            System.out.println(parser.BBS.get(i).getBBNum());
            if(parser.BBS.get(i).getLeftBasicBlock() == null) System.out.println("left null");
            else System.out.println(parser.BBS.get(i).getLeftBasicBlock().getBBNum());
            if(parser.BBS.get(i).getRightBasicBlock() == null) System.out.println("right null");
            else System.out.println(parser.BBS.get(i).getRightBasicBlock().getBBNum());
            System.out.println("--- Instructions for BB" + i);
            for(Instruction instruction : parser.BBS.get(i).getStatements()){
                System.out.println(instruction);
            }
            System.out.println();
        }
        DFS(parser.BBS.get(0));
        seen = new HashSet<>();
        writeDot("IR.dot", parser.BBS.get(0));
    }
    public static void DFS(BasicBlock basicBlock){
        if(basicBlock == null || seen.contains(basicBlock.getBBNum())) return;
        System.out.println(basicBlock.getBBNum());
        seen.add(basicBlock.getBBNum());

        DFS(basicBlock.getLeftBasicBlock());

        DFS(basicBlock.getRightBasicBlock());
    }
    public static void writeDot(String filename, BasicBlock curr){
        try{
            PrintWriter output = new PrintWriter(new FileWriter(filename));

            output.println("digraph G {");
            if(curr != null){
                writeDotRecursive(curr, output);
            }
            output.println("}");
            output.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    public static void writeDotRecursive(BasicBlock curr, PrintWriter output) throws Exception {
        if(curr == null || seen.contains(curr.getBBNum())) return;
        seen.add(curr.getBBNum());
        StringBuilder sb = new StringBuilder();
        sb.append("BB").append(curr.getBBNum());
        sb.append(" [shape=record, label=");
        sb.append("\"<b>BB").append(curr.getBBNum()).append("| { ");

        int start = 0, end = curr.getStatements().size();
        for(Instruction instruction : curr.getStatements()){
            if(curr.getBBNum() == 0) {
                int instructionNum = instruction.getInstructionNum();
                String opCode = instruction.getOpCode();
                String pound = " #";
                int val = instruction.getVal();
                sb.append(instructionNum).append(": ").append(opCode).append(pound).append(val);
                start++;
                if(start != end) sb.append(" | ");

            }
            else{
                int instructionNum = instruction.getInstructionNum();
                String opCode = instruction.getOpCode();
                int leftInstruction = instruction.getLeftInstruction();
                int rightInstruction = instruction.getRightInstruction();
                sb.append(instructionNum).append(": ").append(opCode);
                if(leftInstruction != -1){
                    sb.append(" (").append(leftInstruction).append(")");
                }
                if(rightInstruction != -1){
                    sb.append(" (").append(rightInstruction).append(")");
                }
                start++;
                if(start != end) sb.append(" | ");
            }
        }
        sb.append(" }}\"];");
        output.println(sb.toString());

        if(curr.getLeftBasicBlock() != null){
            sb = new StringBuilder("BB" + curr.getBBNum() + ":s -> BB" + curr.getLeftBasicBlock().getBBNum() + ":n");
            if(curr.isIf){
                sb.append(" [label=\"fall-through\"]");
                output.println("BB" + curr.getBBNum() + ":s -> BB" + curr.getLeftBasicBlock().getBBNum() + ":n [label=\"dom\", style=\"dotted\", color=\"blue\"];");
            }
            else if(curr.isThen){
                if(curr.getLeftBasicBlock().isWhile) {
                    sb.append(" [label=\"fall-through\"]");
                    output.println("BB" + curr.getBBNum() + ":s -> BB" + curr.getLeftBasicBlock().getBBNum() + ":n [label=\"dom\", style=\"dotted\", color=\"blue\"];");
                }
                else sb.append(" [label=\"branch\"]");
            }
            else if(curr.isElse){
                if(curr.getStatements().get(curr.getStatements().size() - 1).getOpCode().equals("bra")){
                    sb.append(" [label=\"branch\"]");
                }
                else
                    sb.append(" [label=\"fall-through\"]");
            }
            else if(curr.isJoin){
                if(curr.getStatements().get(curr.getStatements().size() - 1).getOpCode().equals("bra"))
                    sb.append(" [label=\"branch\"]");
                else sb.append(" [label=\"fall-through\"]");
            }
            else if(curr.isWhile){
                sb.append(" [label=\"fall-through\"]");
                output.println("BB" + curr.getBBNum() + ":s -> BB" + curr.getLeftBasicBlock().getBBNum() + ":n [label=\"dom\", style=\"dotted\", color=\"blue\"];");
            }
            output.println(sb.toString() + ";");
            writeDotRecursive(curr.getLeftBasicBlock(), output);
        }
        if(curr.getRightBasicBlock() != null){
            if(curr.getBBNum() != curr.getRightBasicBlock().getBBNum()) {
                sb = new StringBuilder("BB" + curr.getBBNum() + ":s -> BB" + curr.getRightBasicBlock().getBBNum() + ":n");
                if(curr.isIf) {
                    sb.append(" [label=\"branch\"]");
                    output.println("BB" + curr.getBBNum() + ":s -> BB" + curr.getRightBasicBlock().getBBNum() + ":n [label=\"dom\", style=\"dotted\", color=\"blue\"];");
                }
                else if(curr.isWhile) {
                    sb.append(" [label=\"follow\"]");
                    output.println("BB" + curr.getBBNum() + ":s -> BB" + curr.getRightBasicBlock().getBBNum() + ":n [label=\"dom\", style=\"dotted\", color=\"blue\"];");
                }
                else sb.append(" [label=\"branch\"]");
                output.println(sb.toString() + ";");
            }
            writeDotRecursive(curr.getRightBasicBlock(), output);
        }
        if(curr.getReturnToBasicBlock() != null && curr.getStatements().get(curr.getStatements().size() - 1).getOpCode().equals("bra")){
            output.println("BB" + curr.getBBNum() + ":s -> BB" + curr.getReturnToBasicBlock().getBBNum() + ":n [label=\"branch\", color=\"red\"];");
        }
    }
}