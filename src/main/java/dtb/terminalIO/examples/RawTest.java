package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.function.Consumer;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.Renderer.TextRenderer;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Stty;

public class RawTest {
    public static void main(String[] args) throws Exception {

        Stty.setRaw();

        int choice = 2;

        try {
            if (choice == 1)
                test1(args);
            if (choice == 2)
                test2(args);
        } finally {
            Stty.setCooked();
        }

    }

    public static void test1(String[] args) throws IOException {
        // TODO Auto-generated method stub

        
        TerminalScreen scr = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(scr);

        master_canvas.t(10, 10, "Hello there!");

        master_canvas.setSize(20, 40);

        master_canvas.refresh();

        TestsCommon.sleep(500);

//        scr.t("Testing 123\n");
//        scr.t("Testing 456\n");

        master_canvas.t(10, 10, "Hello there! Again!");

        master_canvas.refresh();

//        scr.refresh();
//
//        TestsCommon.sleep(500);	

//        scr.t("XX Testing 123\n");
//        scr.t("XX Testing 456\n");
//        
        scr.refresh();

    }

    public static void test2(String[] args) throws IOException {

        ElapsedTime timer = new ElapsedTime();
        
        TerminalScreen scr = TestsCommon.getTerminalScreenForTesting(args);

        scr.hideCursor();
        
        Consumer<Integer> displayRAW = (i) -> {
        
        scr.add(new TextRenderer("hello!\n\rSup?\n\n"))
           .add(ControlCode.CURSOR_LEFT)
           .add(new TextRenderer("Dude!"))
           .add(new ControlCode.CursorDown(10));

        scr.refresh();

        //TestsCommon.sleep(500);

        scr.t("Again I saw hello!");
        scr.add(new ControlCode.CursorDown(10));
        scr.add(new ControlCode.CursorUp(10));
        scr.t("The rain in spain stays mainly on the plain");
        scr.add(new ControlCode.CursorDown(10));
        scr.add(ControlCode.CURSOR_LEFT);
        scr.t("i = " + i);
        scr.cr();

        scr.refresh();
        
        };
        
        for (int i = 0; i<2000; i++) {
            displayRAW.accept(i);
        }

        scr.add(new ControlCode.CursorDown(10));
        scr.add(new ControlCode.CursorDown(10));
        scr.t("elapsed : " + timer.elapsed());
        
        scr.cr();
        scr.showCursor();
        scr.refresh();
        
        
    }

}
