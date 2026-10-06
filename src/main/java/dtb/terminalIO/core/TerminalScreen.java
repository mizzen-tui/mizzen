package dtb.terminalIO.core;

import java.io.IOException;
import java.io.PrintStream;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.BaseTextControlCode;
import dtb.terminalIO.controlcodes.ControlCodeContext;
import dtb.terminalIO.controlcodes.Renderer;
import dtb.terminalIO.controlcodes.Renderer.RendererBuffer;
import dtb.terminalIO.controlcodes.Renderer.TextRenderer;
import dtb.terminalIO.controlcodes.color.ColorBGPipe;
import dtb.terminalIO.core.CLIEmitter.DelegateEmitter;
import dtb.terminalIO.core.CLIEmitter.MapCharsEmitter;
import dtb.terminalIO.core.CLIEmitter.PrintStreamEmitter;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.MapChars;
import dtb.terminalIO.util.Stty;

public class TerminalScreen implements ControlCodeContext {

    boolean                 refreshed   = false;

    // how the screen sits in the terminal: below the prompt (inline) or the whole terminal
    private ScreenMode      screenMode  = new InlineScreenMode(this);

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
    ColorBGPipe             colorPipe   = new ColorBGPipe();

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
            screenMode.home();
            refreshed = false;
        }
        rb.append( colorPipe.wrap(r));
        return this;
    }

    /**
     * Write text to screen
     * @param s the text
     */
    public TerminalScreen t(String s) {
        return add(new TextRenderer(s));
    }

    public TerminalScreen refresh() {
        screenMode.render(() -> rb.render(this));
        rb.clear();
        refreshed = true;
        return this;
    }

    public TerminalScreen cr() {
        return add(ControlCode.CR);
    }

    public TerminalScreen clear() {
        rb.clear();
        screenMode.clear();
        refreshed = true;
        return this;
    }
    
    /**
     * Move cursor to screen's top-left (0,0) position
     */
    public TerminalScreen home() {
        screenMode.home();
        return this;
    }

    public ScreenMode getScreenMode() {
        return screenMode;
    }

    /** inline (the default) or full screen - set before the app starts drawing */
    public TerminalScreen setScreenMode(ScreenMode screenMode) {
        this.screenMode = screenMode;
        return this;
    }


    public LineTrackingCLIEmmitter getEmitter() {
        return this.out;
    }

    public void showCursorAt(Position showCursorPosition, boolean showCursor) {
        if (showCursorPosition == null ||
            showCursorPosition.getRow()<0 || 
            showCursorPosition.getCol()<0) {
            throw new IllegalStateException("showCursor request illegal " + String.valueOf(showCursorPosition));
        }
        screenMode.showCursorAt(showCursorPosition, showCursor);
    }

    public void showCursor() {
        //Logging.info("TerminalScreen.showCursor()");
        add(ControlCode.SHOW_CURSOR);
    }

    public void hideCursor() {
        
      //  Logging.info("TerminalScreen.hideCursor()");
       // add(ControlCode.HIDE_CURSOR);
        out.hideCursor();
        
    }

}
