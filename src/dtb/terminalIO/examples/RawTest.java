package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.util.Stty;

public class RawTest {
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub

        Stty.setRaw();
        
        
        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));
        
        master_canvas.t(10,10,"Hello there!");
        
        master_canvas.setSize(20, 40);
        
        master_canvas.refresh();
        
        
        Stty.setCooked();

        master_canvas.refresh();
    
    }

}
