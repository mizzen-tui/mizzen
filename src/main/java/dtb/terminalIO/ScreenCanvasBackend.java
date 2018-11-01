package dtb.terminalIO;

import dtb.terminalIO.Canvas.CanvasBackend;

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
}