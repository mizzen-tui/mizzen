package dtb.terminalIO.core;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCodeContext;
import dtb.terminalIO.controlcodes.Renderer;
import dtb.terminalIO.controlcodes.ControlCode.BaseTextControlCode;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors.HasRowChange;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.util.Logging;

/**
 * A CLIEmmitter that tracks the # of lines emitted (output) so far
 */
public class LineTrackingCLIEmmitter implements CLIEmitter {

    int lineCounter = 0;
    boolean nextOutputIncsLineCounter = true;

    ControlCodeContext ctx;

    public LineTrackingCLIEmmitter(ControlCodeContext ctx) {
	this.ctx = ctx;
    }

    public LineTrackingCLIEmmitter() {
	this(ControlCodeContext.DEFAULT);
    }

    public void outputRenderer(BaseTextControlCode cc) {
	if (cc instanceof HasRowChange) {
	    lineCounter += ((HasRowChange)cc).deltaRow();
	    outputStr(cc.getControlCode(), false);
	    return;
	}
	outputStr(cc.getControlCode());
    }

    private void output(Renderer r) {
	if (nextOutputIncsLineCounter) {
	    nextOutputIncsLineCounter = false;
	    lineCounter++;
	    // ctx.write(DEBUGCTR());
	}
	r.render(this.ctx);
    }

    protected void outputStr(String line, boolean incLineCounter) {
	if (nextOutputIncsLineCounter) {
	    nextOutputIncsLineCounter = false;
	    if (incLineCounter)
	        lineCounter++;
	}
	ctx.write(line);
    }

    protected void outputStr(String line) {
        outputStr(line, true);
    }

    public String DEBUGCTR() {
	return String.format("[%4d]: ", lineCounter);
    }
    
    public int getLineCounter() {
        return this.lineCounter;
    }
    

    protected void CR() {
	output(ControlCode.ERASE_END_OF_LINE);
	output(Renderer.CR);
	nextOutputIncsLineCounter = true;
    }

    private void resetLineCounter() {
	lineCounter = 0;
	nextOutputIncsLineCounter = true;
    }

    // used?
    public void moveUpToLastKnownTopRow() {
	if (lineCounter < 1) {
	    resetLineCounter();
	    return;
	}
	output(new ControlCode.CursorUp(lineCounter - 1));
	resetLineCounter();
    }

    public void clearLastTrackedLines() {
        boolean erasePreviousContent = true;
	clearLastTrackedLines(erasePreviousContent);
    }
    
    int lastLoggedLineCounter = -1;
    private void logStats() {
        if (lastLoggedLineCounter != lineCounter) {
            Logging.info("lineCounter:" + lineCounter);
            lastLoggedLineCounter = lineCounter;
        }
    }

    public void clearLastTrackedLines(boolean erasePreviousContent) {
	if (lineCounter < 1) {
	    resetLineCounter();
	    return;
	}
	logStats();
	
	output(ControlCode.LF); // move to start of line - goal is to get cursor
				// to the left
        output( ControlCode.CURSOR_LEFT );

	if (erasePreviousContent)
	    output(ControlCode.ERASE_LINE); // erase current line
	if (lineCounter > 1) {
	    if (erasePreviousContent)
		output(new ControlCode.CursorUpAndClearLines(lineCounter - 1));
	    else
		output(new ControlCode.CursorUp(lineCounter - 1));
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
	String[] lines = s.split("\n",-1);
	for (int i = 0; i < lines.length; i++) {
	    outputStr(lines[i]);
	    if (i < lines.length) {
		CR();
	    }
	}
    }

    @Override
    public void println(String s) {
	print(s + "\n");
    }

    /**
     * moves the cursor to showCursorPosition
     * relative to the start position of this LineTrackingCLIEmitter
     * @param showCursorPosition
     */
    public void showCursorAt(Position showCursorPosition) {
        int currentLineCounter = lineCounter;
        Position curPos = new Position.Simple(currentLineCounter,0); 
        Position delta = curPos.offsetTo(showCursorPosition);
        
        output( ControlCode.CURSOR_LEFT );
        
        output(ControlCode.moveToDeltaRow(  delta.getRow() + 1) );
        
        output( new ControlCode.CursorForward(delta.getCol()) );
        
    }
}