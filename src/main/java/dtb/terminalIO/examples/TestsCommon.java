package dtb.terminalIO.examples;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.net.UnknownHostException;

import dtb.terminalIO.components.BlockChars;
import dtb.terminalIO.components.braille.BrailleUtils;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.MapChars;
import dtb.terminalIO.util.TerminalDimensions;

public class TestsCommon {

    
    private static boolean isNOGRAPHICS(String[] args) {
        if (args == null || args.length == 0) {
            return false;
        }
        for (int i=0; i<args.length; i++) {
            if ("NOGRAPHICS".equalsIgnoreCase(args[i])) {
                return true;
            }
        }
        return false;
    }
    
    public static TerminalScreen getTerminalScreenForTesting(String[] args) {
        if (isNOGRAPHICS(args)) {
            return getNoGraphicsTerminalScreenForTesting(args);
        }
        return __getTerminalScreenForTesting(args);
    }

    private static TerminalScreen __getTerminalScreenForTesting(String[] args) {
        TerminalScreen screen = new TerminalScreen();
        screen.setPrintStream(getOutputStream(args));
        return screen;
    }
    
    public static MapChars buildNOGraphicsMapChar() {
        MapChars mapChars = new MapChars();
        mapChars.mapAll(BlockChars.UTIL.getNICE_TO_BASIC()  );
        mapChars.mapAll(BrailleUtils.getBrailleToAsciiMapChars());
        
        
        mapChars.map('┈','~');
        mapChars.map('┊','!');
        mapChars.map('░','#');
        mapChars.map('▫','o');
        mapChars.map('■','*');
        mapChars.map('◍','*');
        mapChars.map('◌','o');

        
        return mapChars;
    }

    public static TerminalScreen getNoGraphicsTerminalScreenForTesting(String[] args) {
        TerminalScreen screen = __getTerminalScreenForTesting(args);
        setNoGraphicsOnTerminal(screen);
        return screen;
    }

    static boolean mappedChars_toggle = false; 
    
    public static TerminalScreen setNoGraphicsOnTerminal(TerminalScreen screen) {
        screen.setMappedCharacters( buildNOGraphicsMapChar() );
        mappedChars_toggle = true;
        return screen;
    }
    
    public static void addNoGraphicsToggleEvent(EventBus eventBus, TerminalScreen screen, Runnable updateScreen) {
        eventBus.appendEventHandler( new KeyInputEventHandler(Keys.CTRL_X_KEYSTROKE,
                                    new EventHandler() {
                                        @Override
                                        public void handle(TerminalEvent<?> te) {
                                            if (!mappedChars_toggle) {
                                                setNoGraphicsOnTerminal(screen);
                                            } else {
                                                screen.removeMappedChars();
                                                mappedChars_toggle = false;
                                            }
                                            if (updateScreen != null) {
                                                updateScreen.run();
                                            }
                                        }
                                    }
                                   ) );
    }
    
    public static void addNoGraphicsToggleEvent(EventBus eventBus, TerminalScreen screen) {
        addNoGraphicsToggleEvent(eventBus, screen, null);
    }
    
    

    public static Canvas getMasterCanvasForTesting(TerminalScreen screen) {
        Dimension size = null;
        try {
            size = TerminalDimensions.create();
        } catch (Exception e) {
            size = new Dimension.Simple(35, 150);
        }
        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(size.getHeight() , size.getWidth() ); // todo: detect term size
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

    static boolean[] showScreen_DEBUGINFO = null;

    public static void renderScreenDEBUG(Canvas master_canvas, TerminalScreen screen, EventBus eventBus) {
	
	
	if (showScreen_DEBUGINFO == null && eventBus != null) {
	    showScreen_DEBUGINFO = new boolean[]{false};
	    eventBus.appendEventHandler(  new KeyInputEventHandler(  Keys.CTRL_N_KEYSTROKE  , 
		    (te)-> { 
			
			showScreen_DEBUGINFO[0] = ! showScreen_DEBUGINFO[0]; 
			     		    
		    }
		    
		    ));
	}
	
        if (showScreen_DEBUGINFO != null && showScreen_DEBUGINFO[0]) {
            int master_canvas_height = master_canvas.getHeight();
            
            for (int i = 0; i< master_canvas_height; i++) {
                master_canvas.t(i,0, String.format("|  %03d  |", i));
            }
            
            master_canvas.t(0,10, "linecounter =    " + screen.getEmitter().DEBUGCTR());
        }
	
    }

}
