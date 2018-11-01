package dtb.terminalIO.examples;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.TerminalScreen;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.TerminalDimensions;

public class TestsCommon {

    public static TerminalScreen getTerminalScreenForTesting(String[] args) {
        TerminalScreen screen = new TerminalScreen();
        screen.setPrintStream(getOutputStream(args));
        return screen;
    }

    public static Canvas getMasterCanvasForTesting(TerminalScreen screen) {
        Dimension size = null;
        try {
            size = TerminalDimensions.create();
        } catch (Exception e) {
            size = new Dimension.Simple(35, 150);
        }
        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(size.getHeight() - 1, size.getWidth() - 1); // todo: detect term size
        return master_canvas;
    }

    static class MyPrintStream extends PrintStream {

        Socket socket;

        public MyPrintStream(OutputStream out, Socket socket) {
            super(out);
            this.socket = socket;
        }
    }

    // static Socket socket;

    public static PrintStream getNetCatStream() {
        // String hostname = "localhost";
        String hostname = "192.168.2.16";
        int port = 9878;

        try {

            Socket socket = new Socket(hostname, port);

            OutputStream out = socket.getOutputStream();

            MyPrintStream p = new MyPrintStream(out, socket);

            p.println("Initiating contact!");

            return p;

        } catch (UnknownHostException ex) {

            System.out.println("Server not found: " + ex.getMessage());
            return null;

        } catch (IOException ex) {

            System.out.println("I/O error: " + ex.getMessage());
            return null;
        }
    }

    public static PrintStream getOutputStream(String[] args) {
        if (args == null || args.length < 1 || !args[0].equalsIgnoreCase("netcat")) {
            return System.out;
        }

        return getNetCatStream();
    }

    public static void sleep(int m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
        }
    }

}
