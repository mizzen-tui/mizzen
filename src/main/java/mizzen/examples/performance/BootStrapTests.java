package mizzen.examples.performance;

import mizzen.core.CLIEmitter.CountOutputEmitter;
import mizzen.core.Canvas;
import mizzen.core.ScreenCanvasBackend;
import mizzen.core.TerminalScreen;
import mizzen.examples.TestsCommon;

/**
 * A simple test to compare Screen update strategies..
 * 
 * ScreenCanvasBackend.UpdateStrategy.UPDATE_CHANGED   versus  ScreenCanvasBackend.UpdateStrategy.UPDATE_FULL
 *   
 * @author dbalme
 *
 */
public class BootStrapTests {

    public static void main(String[] args) {
        int countUpdateOnChangedStrategy = testUsingUpdateStrategy(args, ScreenCanvasBackend.UpdateStrategy.UPDATE_CHANGED);
        int countFullUpdateStrategy = testUsingUpdateStrategy(args, ScreenCanvasBackend.UpdateStrategy.UPDATE_FULL);
        System.out.println( String.format("update onchanged strategy COUNT: %10s", countUpdateOnChangedStrategy));
        System.out.println( String.format("update full      strategy COUNT: %10d", countFullUpdateStrategy));
    }
    
    public static int testUsingUpdateStrategy(String[] args, ScreenCanvasBackend.UpdateStrategy updateStrategy) {

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        CountOutputEmitter countOutputEmitter = new CountOutputEmitter(screen.getEmitter());
        
        screen.wrapEmitter( countOutputEmitter );
        
        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
        
        ScreenCanvasBackend backEnd = (ScreenCanvasBackend) master_canvas.getCanvasBackend();
        backEnd.setUpdateStrategy(updateStrategy);
        
        master_canvas.setSize(master_canvas.getHeight()-2, master_canvas.getWidth()-4);
        
        master_canvas.setEmptyChar(' ');
        
        for (int h = 0 ; h < master_canvas.getHeight()-5; h++) {
            master_canvas.t(h,1,""+h);
        }
        
        master_canvas.clearCanvas();
        
        for (int h = 0 ; h < master_canvas.getHeight()-5; h++) {
            master_canvas.t(h,1,""+h);
        }
        
        
        master_canvas.t(10,10,"Hello World!");

        master_canvas.refresh();
        
        sleep(500);
        
        master_canvas.t(10,10,"Good by World!");

        master_canvas.refresh();
        
        screen.home();

        sleep(500);
        
        master_canvas.t(20,10,"By David Balme");

        master_canvas.refresh();

        sleep(500);
        master_canvas.t(22,10,"And Vanessa Lloyd");
        master_canvas.t(23,10,"At 23");

        master_canvas.refresh();
        sleep(500);

        master_canvas.t(25,10,"At 25");

        master_canvas.refresh();
        
        
        for (int i = 1 ; i < 250; i++) {
            
            master_canvas.t(25,10,"count: " + i +" ");
            master_canvas.refresh();
            
            //sleep(200);
            
        }
      

//        sleep(500);
        
        return countOutputEmitter.getCharacterCount();

    }

    public static void sleep(int m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block

        }
    }
    
    
}
