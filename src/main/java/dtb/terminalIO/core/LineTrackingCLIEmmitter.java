package dtb.terminalIO.core;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors.HasRowChange;
import dtb.terminalIO.controlcodes.ControlCodeContext;
import dtb.terminalIO.controlcodes.Renderer;
import dtb.terminalIO.geom.position.Position;

/**
 * A CLIEmmitter that tracks the # of lines emitted (output) so far
 * 
 * TODO: There is a fundamental problem with the lineCounter variable.it always
 * seems to be +1 of the actual value. This causes issues when using the
 * showCursorAtFromBottomUP(showCursorPosition) method when canvas is full
 * screen..
 * 
 * Also can have issues when outputting to last line of canvas when canvas is
 * full screen
 * 
 * 
 */
public class LineTrackingCLIEmmitter implements CLIEmitter {

    int lineCounter = 0;

    Position lastShowCursorPosition = null;

    ControlCodeContext ctx;

    public LineTrackingCLIEmmitter(ControlCodeContext ctx) {
        this.ctx = ctx;
    }

    public LineTrackingCLIEmmitter() {
        this(ControlCodeContext.DEFAULT);
    }

    public void outputRenderer(Renderer cc) {
        if (cc instanceof HasRowChange) {
            lineCounter += ((HasRowChange) cc).deltaRow();
            if (lineCounter<0) {
                lineCounter = 0;
                throw new IllegalStateException("LineTrackingCLIEmmitter.lineCounter < 0 : "+lineCounter);
            }
        }
        cc.render(this.ctx);
    }

    protected void outputStr(String line) {
        ctx.write(line);
    }

    public String DEBUGCTR() {
        return String.format("lineCounter: [%4d]: ", lineCounter) + "  cursor: " + Position.UTIL.asString(lastShowCursorPosition);
    }

    public int getLineCounter() {
        return this.lineCounter;
    }

    protected void CR() {
        outputRenderer(ControlCode.ERASE_END_OF_LINE);
        outputRenderer(ControlCode.CR);
    }

    protected void resetLineCounter() {
        lineCounter = 0;
    }

    public void clearLastTrackedLines(boolean erasePreviousContent) {
        if (lineCounter < 1) {
            outputRenderer(ControlCode.LF); // move to start of line - goal is to get cursor
                                            // to the left
            outputRenderer(ControlCode.CURSOR_LEFT); // one of these might be superfluous
            resetLineCounter();
            return;
        }

        outputRenderer(ControlCode.LF); // move to start of line - goal is to get cursor
                                        // to the left
        outputRenderer(ControlCode.CURSOR_LEFT);

        if (erasePreviousContent)
            outputRenderer(ControlCode.ERASE_LINE); // erase current line
        if (lineCounter > 0) {
            if (erasePreviousContent)
                outputRenderer(new ControlCode.CursorUpAndClearLines(lineCounter));
            else
                outputRenderer(new ControlCode.CursorUp(lineCounter));
        }
        resetLineCounter();
    }

    @Override
    public void print(String s) {
        if (s == null) {
            outputStr("NULL");
            return;
        }
        // split is causing me grief - that's why all these checks - I'm
        // probably being stupid
        if (!s.contains("\n")) {
            outputStr(s);
            return;
        }
        if ("\n".equals(s)) {
            CR();
            return;
        }
        String[] lines = s.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            outputStr(lines[i]);
            if (i < lines.length-2) {
                CR();
            }
        }
        if (s.endsWith("\n")) {
            CR();
        }
    }

    @Override
    public void println(String s) {
        print(s + "\n");
    }

    /**
     * moves the cursor to showCursorPosition relative to the start position of this
     * LineTrackingCLIEmitter
     * 
     * @param showCursorPosition
     */
    private void showCursorAtFromTopDown(Position showCursorPosition) {

        clearLastTrackedLines(false);  // false = go to top but don't erase previous content

        int currentLineCounter = lineCounter;
        Position curPos = new Position.Simple(currentLineCounter, 0);
        Position delta = curPos.offsetTo(showCursorPosition);

        outputRenderer(ControlCode.CURSOR_LEFT);

        if (delta.getRow() != 0) {
            outputRenderer(ControlCode.moveToDeltaRow(delta.getRow()));
        }

        if (delta.getCol() > 0) {
            outputRenderer(new ControlCode.CursorForward(delta.getCol()));
        } else if (delta.getCol() < 0) {
            outputRenderer(new ControlCode.CursorBackward(delta.getCol()));
        }

        lastShowCursorPosition = showCursorPosition;
    }

    /**
     * moves the cursor to showCursorPosition relative to the start position of this
     * LineTrackingCLIEmitter
     * 
     * @param showCursorPosition
     */
    public void showCursorAt(Position showCursorPosition) {

        showCursorAtFromTopDown(showCursorPosition);

    }

}