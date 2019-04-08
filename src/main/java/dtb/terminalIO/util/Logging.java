package dtb.terminalIO.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class Logging {

    
    public static void info(String msg) {
        appendTextGOOD(msg);
    }
    
    private static void appendTextGOOD(String msg) {
        // do nothing
    }
    private static void appendText(String msg) {
        
        try {
            // todo:  FIX THIS!!
            Thread.currentThread().getName();
            msg = Thread.currentThread().getName() + ":  " + msg;
            Files.write(Paths.get("/home/david/workspace/dtbTerminalIO/dtbTTerminal.log"), (msg + "\n").getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e) {
            //exception handling left as an exercise for the reader
            System.out.println(e.toString());
        }        
        
    }
    
    
    public static void main(String[] args) {
        

        String s = "\na\n";
        
        String[] s_a  = s.split("\n");
        String[] s_a1 = s.split("\n",-1);
        
        System.out.println( String.format("%s  :  %d   %d  %s   %s", s, s_a.length, s_a1.length, Arrays.toString(s_a),Arrays.toString(s_a1)));
        
    }
    
    
}
