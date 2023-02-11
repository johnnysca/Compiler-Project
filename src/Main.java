import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        System.out.println("Enter filename: ");
        String filename = sc.nextLine();
        Parser parser = new Parser(filename);
    }
}