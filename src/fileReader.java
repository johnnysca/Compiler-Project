import java.io.*;

public class fileReader {
    private File file;
    private BufferedReader br;

    fileReader(String filename) {
        try {
            file = new File(filename);
            br = new BufferedReader(new FileReader(file));
        }
        catch(FileNotFoundException e){
            Error(e.getMessage());
        }
    }
    public char getNext(){
        char ret = 0xff;
        try {
            int currChar = br.read();
            if (currChar == -1) return Tokens.eofToken;
            ret = (char) currChar;
            //System.out.println(ret);
        }
        catch(IOException e){
            Error(e.getMessage());
        }
        return ret;
    }
    public void Error(String errorMsg){
        System.out.println(errorMsg);
    }
}