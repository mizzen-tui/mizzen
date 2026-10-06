package dtb.terminalIO.core;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.geom.position.Position;

/**
 * The canvas below the shell prompt: TerminalScreen's line tracking (LineTrackingCLIEmmitter) counts
 * the lines written since home, and home is "up that many lines". The canvas may be smaller than the
 * terminal - a pop-up in a command line session - and what it showed stays in the scrollback on exit.
 */
public class InlineScreenMode implements ScreenMode {

    private final TerminalScreen screen;

    public InlineScreenMode(TerminalScreen screen) {
        this.screen = screen;
    }

    /**
     * Paint once before raw mode: rows end with real newlines then, which scroll the terminal to make
     * room for the canvas below the prompt.
     */
    @Override
    public void takeOverTerminal(Canvas canvas) {
        canvas.refresh();
    }

    @Override
    public void home() {
        screen.getEmitter().clearLastTrackedLines(false);
    }

    @Override
    public void clear() {
        screen.getEmitter().clearLastTrackedLines(true);
    }

    @Override
    public void showCursorAt(Position position, boolean showCursor) {
        screen.getEmitter().showCursorAt(position, showCursor);
    }

    @Override
    public void render(Runnable render) {
        render.run();
    }

    /** a full redraw (raw mode is off: real newlines), then one more newline: the prompt goes below the canvas */
    @Override
    public void handBackTerminal(Canvas canvas) {
        if (canvas.getCanvasBackend() instanceof ScreenCanvasBackend) {
            ((ScreenCanvasBackend) canvas.getCanvasBackend()).fullUpdateOnNextRefresh();
        }
        canvas.refresh();
        screen.getEmitter().outputRenderer(ControlCode.CR);     // straight out: screen.cr() would go home first
    }
}
