package mizzen.core;

import mizzen.controlcodes.ControlCode;
import mizzen.controlcodes.ControlCode.BaseTextControlCode;
import mizzen.events.keyboard.InputParserManager;
import mizzen.geom.position.Position;
import mizzen.util.Stty;

/**
 * The canvas is the whole terminal, as with less, nano or vim: the app runs on the terminal's
 * alternate screen (no scrollback, nothing of the shell's), positions the cursor absolutely, and on
 * exit switches back - the shell's screen and cursor reappear exactly as they were.
 * <p/>
 * Each screen update is sent as "synchronized output" (terminals that support it show it all at
 * once; others ignore the markers), and a clear goes out with the repaint that follows it - so a
 * resize's clear-and-repaint does not flicker.
 * <p/>
 * A JVM shutdown hook hands the terminal back if the app ends without handing the terminal back (System.exit, a
 * crash): mouse off, cursor shown, the shell's screen back, cooked mode.
 */
public class FullScreenMode implements ScreenMode {

    static final ControlCode ALTERNATE_SCREEN_ON  = new BaseTextControlCode(ControlCode.escape_CHR + "[?1049h").setName("ALTERNATE_SCREEN_ON");
    static final ControlCode ALTERNATE_SCREEN_OFF = new BaseTextControlCode(ControlCode.escape_CHR + "[?1049l").setName("ALTERNATE_SCREEN_OFF");
    static final ControlCode HOME                 = new BaseTextControlCode(ControlCode.escape_CHR + "[H").setName("HOME");
    static final ControlCode ERASE_SCREEN         = new BaseTextControlCode(ControlCode.escape_CHR + "[2J").setName("ERASE_SCREEN");
    static final ControlCode SYNC_START           = new BaseTextControlCode(ControlCode.escape_CHR + "[?2026h").setName("SYNC_START");
    static final ControlCode SYNC_END             = new BaseTextControlCode(ControlCode.escape_CHR + "[?2026l").setName("SYNC_END");

    private final TerminalScreen screen;
    private final Thread onShutdown = new Thread(this::handBackAtShutdown, "FullScreenMode-shutdown");
    private boolean tookOver;
    private boolean handedBack;

    public FullScreenMode(TerminalScreen screen) {
        this.screen = screen;
    }

    /** the alternate screen (saves the shell's screen and cursor), blank, cursor home */
    @Override
    public synchronized void takeOverTerminal(Canvas canvas) {
        if (tookOver) {
            return;
        }
        tookOver = true;
        write(ALTERNATE_SCREEN_ON);
        write(HOME);
        write(ERASE_SCREEN);
        Runtime.getRuntime().addShutdownHook(onShutdown);
    }

    @Override
    public void home() {
        write(HOME);
        screen.getEmitter().resetLineCounter();      // lines are counted for inline mode's home; not needed here
    }

    /** queued, not written: it goes out with the repaint that follows, in the same update */
    @Override
    public void clear() {
        screen.add(HOME);
        screen.add(ERASE_SCREEN);
    }

    @Override
    public void showCursorAt(Position position, boolean showCursor) {
        write(new BaseTextControlCode(ControlCode.escape_CHR + "[" + (position.getRow() + 1) + ";" + (position.getCol() + 1) + "H"));
        if (showCursor) {
            write(ControlCode.SHOW_CURSOR);
        }
    }

    @Override
    public void render(Runnable render) {
        write(SYNC_START);
        try {
            render.run();
        } finally {
            write(SYNC_END);
        }
    }

    /** back to the shell's screen and cursor - once, whichever way the app ends */
    @Override
    public synchronized void handBackTerminal(Canvas canvas) {
        if (!tookOver || handedBack) {
            return;
        }
        handedBack = true;
        write(ALTERNATE_SCREEN_OFF);
        try {
            Runtime.getRuntime().removeShutdownHook(onShutdown);
        } catch (IllegalStateException alreadyShuttingDown) {
            // called by the hook itself
        }
    }

    // the app ended without handing the terminal back: undo what it did to the terminal
    private void handBackAtShutdown() {
        synchronized (this) {
            if (handedBack) {
                return;
            }
        }
        InputParserManager.disableMouseTracking(screen);
        screen.add(ControlCode.SHOW_CURSOR);
        screen.refresh();
        handBackTerminal(null);
        try {
            Stty.setCooked();
        } catch (Exception e) {
            // nothing more we can do at shutdown
        }
    }

    // straight to the terminal, in order with everything else written
    private void write(ControlCode code) {
        screen.getEmitter().outputRenderer(code);
    }
}
