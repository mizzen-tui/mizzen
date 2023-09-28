package dtb.terminalIO.controlcodes.color;

import dtb.terminalIO.controlcodes.ControlCodeContext;
import dtb.terminalIO.controlcodes.Renderer;
import dtb.terminalIO.controlcodes.Renderer.TextRenderer;

/**
 * I was having issues with background colors "bleeding" into other components because
 * I was not managing the background color setting.
 * 
 * <p/>
 * 
 * This component keeps track of all renderer getting output to the
 * screen and it will occasionally emit the {@link Colors.BackGroundColorRESET} code
 * when it deems necessary.
 * 
 * @author dbalme
 *
 */
public class ColorBGPipe  {

    public static boolean DISABLE = false;
    
    boolean lastColorHadBackGround = false;
    boolean lastControlCodeWasAColor = false;

    public Renderer wrap(Renderer r) {
        if (DISABLE || r instanceof TextRenderer) {
            return r;
        }
        if (!(r instanceof Color )) {
//            if (lastControlCodeWasAColor && //lastColorHadBackGround &&
//               (r == ControlCode.CR /*|| 
//                
//                r == ControlCode.ERASE_END_OF_LINE ||
//                r == ControlCode.ERASE_START_OF_LINE ||
//                r == ControlCode.ERASE_LINE ||
//                r == ControlCode.ERASE_END_OF_LINE ||
//                r instanceof CursorForward ||
//                r instanceof CursorBackward ||
//                r instanceof CursorUp ||
//                r instanceof CursorDown ||
//                r instanceof CursorUpAndClearLines*/)) {
//                    lastColorHadBackGround = false;
//                    lastControlCodeWasAColor = false;
//                    return new ResetBGColorAndThenRenderer(r);
//                }
            lastControlCodeWasAColor = false;
            return r;
        }
        lastControlCodeWasAColor = true;
        Color currentColor = (Color) r;
        if (! lastColorHadBackGround) {
            this.lastColorHadBackGround = currentColor.hasBackGroundColor();
            return currentColor;
        }
        // last color had background
        if (currentColor.hasBackGroundColor()) {
            this.lastColorHadBackGround = true;
            return currentColor;
        }
        // previous color had background and current color does not so reset background
        lastColorHadBackGround = false;
        return new ResetBGColorAndThenRenderer(currentColor);
    }

    static class ResetBGColorAndThenRenderer implements Renderer {
        Renderer renderer;

        public ResetBGColorAndThenRenderer(Renderer currentColor) {
            super();
            this.renderer = currentColor;
        }

        @Override
        public void render(ControlCodeContext ctx) {
            Colors.BackGroundColorRESET.render(ctx);
            renderer.render(ctx);
        }
        
    } 

}
