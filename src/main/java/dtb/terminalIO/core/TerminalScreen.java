package dtb.terminalIO.core;

import java.io.IOException;
import java.io.PrintStream;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.BaseTextControlCode;
import dtb.terminalIO.controlcodes.ControlCodeContext;
import dtb.terminalIO.controlcodes.Renderer;
import dtb.terminalIO.controlcodes.Renderer.RendererBuffer;
import dtb.terminalIO.controlcodes.Renderer.TextRenderer;
import dtb.terminalIO.core.CLIEmitter.DelegateEmitter;
import dtb.terminalIO.core.CLIEmitter.MapCharsEmitter;
import dtb.terminalIO.core.CLIEmitter.PrintStreamEmitter;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.MapChars;
import dtb.terminalIO.util.Stty;

public class TerminalScreen implements ControlCodeContext {

    boolean                 refreshed   = false;

    // this ctx is used simply to hold the iscolor and is controlcode enables flags
    ControlCodeContext      ctx        = ControlCodeContext.DEFAULT;

    // Typically points to System.out
    CLIEmitter              baseEmitter = CLIEmitter.DEFAULT;

    // An emitter that tracks # lines output to baseEmitter (System.out)
    LineTrackingCLIEmmitter out         = new LineTrackingCLIEmmitter(new TerminalContextDelegate(this) {
                                            @Override
                                            public void write(String s) {
                                                TerminalScreen.this.getBaseEmitter().print(s);
                                            }
                                        });

    RendererBuffer          rb          = new RendererBuffer();

    public void setPrintStream(PrintStream out) {
        if (out == null) {
            out = System.out;
        }

        this.baseEmitter = new PrintStreamEmitter(out);
    }
    
    CLIEmitter getBaseEmitter() {
        return this.baseEmitter;
    }

    public void setMappedCharacters(MapChars mapChars) {
        // this.baseEmitter = null;
        this.wrapEmitter( new MapCharsEmitter(null, mapChars));
    }

    // returns true if mapped chars removed
    public boolean removeMappedChars() {
        if (this.baseEmitter instanceof MapCharsEmitter) {
            return unwrapEmitter();
        }
        return false;
    }

    public void wrapEmitter(DelegateEmitter wrappingEmitter) {
        wrappingEmitter.setDelegate(baseEmitter);
        this.baseEmitter = wrappingEmitter;
    }
    
    // returns true if emitter is unwrapped
    public boolean unwrapEmitter() {
        if (this.baseEmitter instanceof DelegateEmitter) {
            CLIEmitter baseE = ((DelegateEmitter)this.baseEmitter).getDelegate();
            this.baseEmitter = baseE;
            return true;
        }
        return false;
    }
    
    public void setRaw()  {
        try {
            Stty.setRaw();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setCooked() throws IOException {
        try {
            Stty.setCooked();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isRaw() {
        return Stty.isRaw();
    }

    public boolean isCooked() {
        return Stty.isCooked();
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

    @Override
    public void write(BaseTextControlCode cc) {
        out.outputRenderer(cc);
    }
    
    public TerminalScreen add(Renderer r) {
        if (refreshed) {
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
        return add(ControlCode.CR);
    }

    public TerminalScreen clear() {
        rb.clear();
        out.clearLastTrackedLines(true);
        refreshed = true;
        return this;
    }
    
    /**
     * Move cursor to screen's to its top-left (0,0) position
     */
    public TerminalScreen home() {
        boolean erasePreviousContent = false;
        this.getEmitter().clearLastTrackedLines(erasePreviousContent);
        return this;
    }


    public LineTrackingCLIEmmitter getEmitter() {
	return this.out;
    }

    public void showCursorAt(Position showCursorPosition) {
        if (showCursorPosition == null ||
            showCursorPosition.getRow()<0 || 
            showCursorPosition.getCol()<0) {
            throw new IllegalStateException("showCursor request illegal " + String.valueOf(showCursorPosition));
        }
        out.showCursorAt(showCursorPosition);
    }

    public void showCursor() {
        add(ControlCode.SHOW_CURSOR);
    }

    public void hideCursor() {
        add(ControlCode.HIDE_CURSOR);
    }

}
