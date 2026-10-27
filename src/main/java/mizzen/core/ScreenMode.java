package mizzen.core;

import mizzen.geom.position.Position;

/**
 * How the app's screen sits in the terminal - the part of TerminalScreen that differs between:
 * <pre>
 *   InlineScreenMode  below the shell prompt, positioned by counting lines (the canvas can be smaller
 *                     than the terminal: a pop-up in a command line session)
 *   FullScreenMode    the whole terminal: its alternate screen, positioned absolutely, and the shell's
 *                     screen back as it was on exit (as less, nano, vim do)
 * </pre>
 * Everything that draws goes to "home" (the canvas's top-left) and moves relative to it, so only
 * home, clear and cursor placement differ - plus taking over the terminal at start and handing it
 * back at exit.
 * <p/>
 * TerminalScreen holds one (inline unless told otherwise) and delegates to it; nothing else needs to know.
 */
public interface ScreenMode {

    /** at start, before the app's first paint: inline makes room below the prompt, full screen switches screens */
    void takeOverTerminal(Canvas canvas);

    /** cursor to the canvas's top-left */
    void home();

    /** blank the canvas; the cursor ends at home */
    void clear();

    void showCursorAt(Position position, boolean showCursor);

    /** one screen update: render writes it */
    void render(Runnable render);

    /** at exit (raw mode already off): inline leaves the cursor below the canvas, full screen switches back */
    void handBackTerminal(Canvas canvas);
}
