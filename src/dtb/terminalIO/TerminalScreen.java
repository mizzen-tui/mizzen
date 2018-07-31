package dtb.terminalIO;

import java.io.PrintStream;

import dtb.terminalIO.CLIEmitter.PrintStreamEmitter;
import dtb.terminalIO.Renderer.RendererBuffer;
import dtb.terminalIO.Renderer.TextRenderer;

public class TerminalScreen implements TerminalContext {

	boolean refreshed = false;
	
	TerminalContext ctx = TerminalContext.DEFAULT;
	
	CLIEmitter baseEmitter = CLIEmitter.DEFAULT;
	
	LineTrackingCLIEmmitter out = 
			new LineTrackingCLIEmmitter(new TerminalContextDelegate(this){
				@Override
				public void write(String s) {
					TerminalScreen.this.baseEmitter.print(s);
				}
			});
	
	RendererBuffer rb = new RendererBuffer();
	
	public void setPrintStream(PrintStream out) {
		if (out == null) {
			out = System.out;
		}
		
		baseEmitter = new PrintStreamEmitter(out);
		
	}
	
	
	@Override
	public boolean isControlCodesEnabled() {
		return ctx.isControlCodesEnabled();
	}

	@Override
	public void setControlCodesEnabled(boolean enable) {
		ctx.setControlCodesEnabled(enable);
	}

	@Override
	public boolean isColorEnabled() {
		return ctx.isColorEnabled();
	}

	@Override
	public void write(String s) {
		out.print(s);
	}

	public TerminalScreen add(Renderer r) {
		if (refreshed)
		{
			out.clearLastTrackedLines(false);
			refreshed = false;
		}
		rb.append(r);
		return this;
	}
	
	public TerminalScreen t(String s) {
		return add(new TextRenderer(s));
	}

	public TerminalScreen refresh() {
		rb.render(this);
		rb.clear();
		refreshed = true;
		return this;
	}

	public TerminalScreen cr() {
		return add( Renderer.CR) ;
	}

	public TerminalScreen clear() {
		rb.clear();
		out.clearLastTrackedLines(true);
		refreshed = true;
		return this;
	}

}
