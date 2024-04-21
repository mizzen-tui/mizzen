package dtb.terminalIO.supportfortesting;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;

public class CanvasForTesting {

    public static Canvas buildCanvasForTestingNoControlCodesInOutput() throws UnsupportedEncodingException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, "UTF-8");

        TerminalScreen screen = new TerminalScreen();
        screen.setPrintStream(ps);
        screen.setControlCodesEnabled(false);

        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(25, 80);
        return master_canvas;
    }
    
    
}
