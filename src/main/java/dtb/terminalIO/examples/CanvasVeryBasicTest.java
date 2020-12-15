package dtb.terminalIO.examples;

import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.util.Stty;

public class CanvasVeryBasicTest {

    public static void  main(String args[]) throws Exception {
        TerminalScreen terminal = TestsCommon.getTerminalScreenForTesting(args);
        
        Canvas canvas = new Canvas(new ScreenCanvasBackend(terminal));

        canvas.setSize(20, 50);
        
        canvas.t(1,1,"Hello there");
        
        canvas.refresh();
        
        
        canvas.t(2,2,"Hello there again");

        canvas.refresh();
        
        
    }
     
    
}
