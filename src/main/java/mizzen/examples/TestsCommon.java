package mizzen.examples;

import java.util.function.Consumer;

import mizzen.components.BlockChars;
import mizzen.components.plot.braille.BrailleUtils;
import mizzen.core.Canvas;
import mizzen.core.ScreenCanvasBackend;
import mizzen.core.ScreenCanvasBackend.UpdateStrategy;
import mizzen.core.TerminalScreen;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.util.MapChars;
import mizzen.util.OSType;
import mizzen.util.SparkLine;
import mizzen.util.TerminalDimensions;

public class TestsCommon {

    private static boolean isNOGRAPHICS(String[] args) {
        if (args == null || args.length == 0) {
            return false;
        }
        for (int i = 0; i < args.length; i++) {
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
        screen.setPrintStream(System.out);
        return screen;
    }

    public static MapChars buildNOGraphicsMapChar() {
        MapChars mapChars = new MapChars();
        mapChars.mapAll(BlockChars.UTIL.getNICE_TO_BASIC());
        mapChars.mapAll(BrailleUtils.getBrailleToAsciiMapChars());

        mapChars.mapAll(SparkLine.UTIL.getSparklineToAsciiMapChars());

        mapChars.map('┈', '~');
        mapChars.map('┊', '!');
        mapChars.map('░', '#');
        mapChars.map('▒', '=');
        mapChars.map('▫', 'o');
        mapChars.map('■', '*');
        mapChars.map('◍', '*');
        mapChars.map('□', 'o');
        mapChars.map('◌', 'o');

        return mapChars;
    }

    public static TerminalScreen getNoGraphicsTerminalScreenForTesting(String[] args) {
        TerminalScreen screen = __getTerminalScreenForTesting(args);
        setNoGraphicsOnTerminal(screen);
        return screen;
    }

    static boolean mappedChars_toggle = false;

    public static TerminalScreen setNoGraphicsOnTerminal(TerminalScreen screen) {
        screen.setMappedCharacters(buildNOGraphicsMapChar());
        mappedChars_toggle = true;
        return screen;
    }

    public static void addNoGraphicsToggleEvent(EventBus eventBus, TerminalScreen screen, Runnable updateScreen) {
        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_X_KEYSTROKE, new EventHandler() {
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
        }));
    }

    public static void addNoGraphicsToggleEvent(EventBus eventBus, TerminalScreen screen) {
        addNoGraphicsToggleEvent(eventBus, screen, null);
    }

    public static Dimension terminalSize() {
        Dimension size = null;
        try {
            size = TerminalDimensions.create();
        } catch (Exception e) {
            size = new Dimension.Simple(35, 150);
        }
        return size;
    }

    public static Canvas getMasterCanvasForTesting(TerminalScreen screen) {
        Dimension size = terminalSize();
        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(size.getHeight(), size.getWidth());

        if (OSType.isCYGWIN()) {
            ScreenCanvasBackend backEnd = (ScreenCanvasBackend) master_canvas.getCanvasBackend();
            backEnd.setUpdateStrategy(ScreenCanvasBackend.UpdateStrategy.UPDATE_FULL); // <-- TODO: hopefully this is temporary
        }

        return master_canvas;
    }

    
    public static KeyInputEventHandler toggleScreenupStrategyKeyboardHandling(Canvas canvas, 
                                                              EventBus eventBus,
                                                              Consumer<ScreenCanvasBackend.UpdateStrategy> updateNotifier) {
        
        return toggleScreenupStrategyKeyboardHandling(Keys.CTRL_U_KEYSTROKE, 
                                                      canvas, 
                                                      eventBus, 
                                                      updateNotifier);
    }
    
    public static KeyInputEventHandler toggleScreenupStrategyKeyboardHandling(KeyStroke onKeyStroke,
                                                              Canvas canvas, 
                                                              EventBus eventBus,
                                                              Consumer<ScreenCanvasBackend.UpdateStrategy> updateNotifier) {

        ScreenCanvasBackend canvasBackend = ((ScreenCanvasBackend)canvas.getCanvasBackend());

        //UpdateStrategy lastRememberedUpdateStrategy[] = {canvasBackend.getScreenUpdateStrategy()};

         Runnable toggleScreenUpdateStrategy = ()-> {
             if (canvasBackend.getScreenUpdateStrategy() == UpdateStrategy.UPDATE_FULL) {
                 canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_CHANGED);
                 updateNotifier.accept(canvasBackend.getScreenUpdateStrategy());
             } else {
                 canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_FULL);
                 updateNotifier.accept(canvasBackend.getScreenUpdateStrategy());
             }
         };

         KeyInputEventHandler toggleUpdateEventHandler = new KeyInputEventHandler(onKeyStroke, (te) -> {
             toggleScreenUpdateStrategy.run();
         });
         
        eventBus.appendEventHandler(toggleUpdateEventHandler);

         canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_CHANGED);
         updateNotifier.accept(canvasBackend.getScreenUpdateStrategy());
         
         return toggleUpdateEventHandler;
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
            showScreen_DEBUGINFO = new boolean[] { false };
            eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_N_KEYSTROKE, (te) -> {

                showScreen_DEBUGINFO[0] = !showScreen_DEBUGINFO[0];
            }

            ));
        }

        if (showScreen_DEBUGINFO != null && showScreen_DEBUGINFO[0]) {
            int master_canvas_height = master_canvas.getHeight();

            for (int i = 0; i < master_canvas_height; i++) {
                master_canvas.t(i, 0, String.format("|  %03d  |", i));
            }

            master_canvas.t(0, 10, "linecounter =    " + screen.getEmitter().DEBUGCTR());
        }

    }

}
