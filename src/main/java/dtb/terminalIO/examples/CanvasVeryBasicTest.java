package dtb.terminalIO.examples;

import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.TerminalScreen;

public class CanvasVeryBasicTest {

    public static void  main(String args[]) throws Exception {
        TerminalScreen terminal = TestsCommon.getTerminalScreenForTesting(args);
        
        Canvas canvas = TestsCommon.getMasterCanvasForTesting(terminal);
        canvas.setEmptyChar('.');

        canvas.setSize(20, 50);
        
        canvas.t(1,1,"Hello there");

        for (int i=0; i<10; i++) {
           canvas.addComponentSegments(2,2,new TextComponent(Colors.BIYel,"sleeping.. "+(10-i)+" "));
           canvas.refresh();
           Thread.sleep(1000);
        }

        
        
        
        canvas.t(2,2,"Hello there again");

        canvas.refresh();
        
    }
     
    
}
