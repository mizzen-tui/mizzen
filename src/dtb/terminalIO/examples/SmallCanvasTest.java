package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.TerminalScreen;

public class SmallCanvasTest {
    
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub

        //Stty.setRaw();
        
        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(4, 30);
        
        screen.setRaw();
        
        master_canvas.setEmptyChar('.');
        
        master_canvas.t("Some text");
        
        master_canvas.refresh();
        
        TestsCommon.sleep(1000);

        master_canvas.t("\n and more text!");
        
        master_canvas.refresh();

        screen.setCooked();
        
    }
    

}
