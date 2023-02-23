import java.sql.SQLOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.*;

public class Main {
    static HashSet<Integer> seen = new HashSet<>();
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        System.out.println("Enter filename: ");
        String filename = sc.nextLine();
        Parser parser = new Parser(filename);
        BasicBlock hm = parser.BBS.get(0);
        BasicBlock hm2 = parser.BBS.get(1);
        System.out.println("size " + parser.BBS.size());

//        System.out.println("---Instructions for BB0---");
//        for(Instruction i : hm.getStatements()){
//            System.out.println(i);
//        }
//        System.out.println();
//        System.out.println("---Instructions for BB1---");
//        for(Instruction i : hm2.getStatements()){
//            System.out.println(i);
//        }
//System.out.println(parser.BBS.get(8).getStatements().size());
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
//        for(Map.Entry<Integer, List<Integer>> entry: parser.BBMapping.entrySet()){
//            System.out.println(entry.getKey() + " " + entry.getValue());
//        }
    }
    public static void DFS(BasicBlock basicBlock){
        if(basicBlock == null || seen.contains(basicBlock.getBBNum())) return;
        System.out.println(basicBlock.getBBNum());
        seen.add(basicBlock.getBBNum());
        DFS(basicBlock.getLeftBasicBlock());

        DFS(basicBlock.getRightBasicBlock());
    }
}