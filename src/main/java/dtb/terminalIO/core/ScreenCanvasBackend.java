package dtb.terminalIO.core;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.util.Logging;

/**
 * Canvas contents written to a TerminalScreen
 * @author david
 */
public class ScreenCanvasBackend implements CanvasBackend {
	
	TerminalScreen screen;

	public ScreenCanvasBackend(TerminalScreen screen) {
		super();
		this.screen = screen;
	}
	
	@Override
	public void writeToBackend(ScreenLineSegment sc) {
		screen.add(sc.getColors());
		screen.t(sc.getText());
	}
	@Override
	public void signal_eol() {
		screen.add(ControlCode.RCol);
	}
	
	@Override
	public void refresh() {
		screen.refresh();
	}

	@Override
	public void cr() {
		screen.cr();
	}

    @Override
    public void showCursorAt(Position showCursorPosition) {
        //screen.add(ControlCode.SHOW_CURSOR);
        screen.showCursorAt(showCursorPosition);
        // screen.add(ControlCode.SHOW_CURSOR);
        Logging.info("showCursorAt " + showCursorPosition);
    }
    
    @Override
    public void showCursor() {
        screen.showCursor();
        Logging.info("showCursor " );
    }

    @Override
    public void hideCursor() {
        screen.hideCursor();
        Logging.info("hideCursor " );
    }
}