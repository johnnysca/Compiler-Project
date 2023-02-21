import java.util.HashMap;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        System.out.println("Enter filename: ");
        String filename = sc.nextLine();
        Parser parser = new Parser(filename);
        BasicBlock hm = parser.BBS.get(0);
        BasicBlock hm2 = parser.BBS.get(1);
        System.out.println(parser.BBS.size());

//        System.out.println("---Instructions for BB0---");
//        for(Instruction i : hm.getStatements()){
//            System.out.println(i);
//        }
//        System.out.println();
//        System.out.println("---Instructions for BB1---");
//        for(Instruction i : hm2.getStatements()){
//            System.out.println(i);
//        }
        for(int i = 0; i<parser.BBS.size(); i++){
            System.out.println("--- Instructions for BB" + i);
            for(Instruction instruction : parser.BBS.get(i).getStatements()){
                System.out.println(instruction);
            }
            System.out.println();
        }
        DFS(parser.BBS.get(0));
    }
    public static void DFS(BasicBlock basicBlock){
        if(basicBlock == null) return;
        System.out.println(basicBlock.getBBNum());
        DFS(basicBlock.getLeftBasicBlock());

        DFS(basicBlock.getRightBasicBlock());
    }
}