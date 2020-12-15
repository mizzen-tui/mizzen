package dtb.terminalIO.examples;

import dtb.terminalIO.components.Composite;
import dtb.terminalIO.core.Canvas;

public class CompositeScrollTest {
    
    public static void main(String[] args) {

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));
        master_canvas.setEmptyChar(' ');


        CompositeTest.build_bounded_pre(master_canvas);

        CompositeTest.build_bounded_post(master_canvas);


        Composite text2 = CompositeTest.build_text2(master_canvas);

        
        Composite text3 = CompositeTest.build_text3(master_canvas);

        master_canvas.t(0, 3, String.valueOf(text2));
        master_canvas.t(1, 3, String.valueOf(text3));
        
                        
        master_canvas.refresh();
        

    }
    
    

}
