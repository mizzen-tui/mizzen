package mizzen.controlcodes;

import java.util.List;

import mizzen.components.HasName;
import mizzen.controlcodes.color.Colors.FGBGColors.HasRowChange;
import mizzen.controlcodes.color.ColorParsingUtils;
import mizzen.controlcodes.color.Colors;


/**
 * A ControlCode is a {@link Renderer} that renders some 
 * kind of linux terminal control code (or linux escape sequence) to
 * manipulate either cursor position,  color of subsequent
 * text output. etc ..  It is the fundamental building block
 * used in this library to manipulate the the Linux Terminal
 * 
 * <p/>
 * 
 * Think VT100 Terminal Escape Codes. 
 * 
 * @author david
 *
 */
public abstract class ControlCode implements Renderer, HasName<ControlCode> {


    private String name;
    
    public ControlCode() {
    }

    @Override
    public void render(ControlCodeContext ctx) {
        if (ctx.isControlCodesEnabled()) {
            doRender(ctx);
        }
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public ControlCode setName(String name) {
         this.name = name;
         return this;
    }
    
    public String toString() {
        return HasName.UTIL.getName(this);
    }

    abstract protected void doRender(ControlCodeContext ctx);

    public static class BaseTextControlCode extends ControlCode {

        private String controlCode;

        public BaseTextControlCode(String controlCode) {
            this.controlCode = controlCode;
        }

        public String getControlCode() {
            return controlCode;
        }

        public void setControlCode(String controlCode) {
            this.controlCode = controlCode;
        }

        protected void doRender(ControlCodeContext ctx) {
            //ctx.write(this.getControlCode());
            ctx.write(this);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((controlCode == null) ? 0 : controlCode.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            BaseTextControlCode other = (BaseTextControlCode) obj;
            if (controlCode == null) {
                if (other.controlCode != null)
                    return false;
            } else if (!controlCode.equals(other.controlCode))
                return false;
            return true;
        }
        
        
    }

//    static int maxColorCodeLen = -1;
//    static int colorCount = 0;

    public static final String      escape_CHR          = "\u001B";
    public static final String      escape_PREFIX       = escape_CHR + "[";

    

                                                        
    // erasing ..

    public static final ControlCode ERASE_END_OF_LINE   = new BaseTextControlCode(escape_CHR + "[K").setName("ERASE_END_OF_LINE");
    public static final ControlCode ERASE_START_OF_LINE = new BaseTextControlCode(escape_CHR + "[1K").setName("ERASE_START_OF_LINE");
    public static final ControlCode ERASE_LINE          = new BaseTextControlCode(escape_CHR + "[2K").setName("ERASE_LINE");
    public static final ControlCode ERASE_DOWN          = new BaseTextControlCode(escape_CHR + "[J").setName("ERASE_DOWN");
    public static final ControlCode ERASE_UP            = new BaseTextControlCode(escape_CHR + "[1J").setName("ERASE_UP");
    public static final ControlCode ERASE_SCREEN        = new BaseTextControlCode(escape_CHR + "[2J").setName("ERASE_SCREEN");

    // \033[?25l")
    public static final ControlCode HIDE_CURSOR         = new BaseTextControlCode(escape_CHR + "[?25l").setName("HIDE_CURSOR");
    public static final ControlCode SHOW_CURSOR         = new BaseTextControlCode(escape_CHR + "[?25h").setName("SHOW_CURSOR");

    
    /* MOUSE */
    
    
    public static final ControlCode SET_X10_MOUSE               = new BaseTextControlCode(escape_CHR + "[?9h").setName("SET_X10_MOUSE");
    public static final ControlCode SET_VT200_MOUSE             = new BaseTextControlCode(escape_CHR + "[?1000h").setName("SET_VT200_MOUSE");
    public static final ControlCode SET_VT200_HIGHLIGHT_MOUSE   = new BaseTextControlCode(escape_CHR + "[?1001h").setName("SET_VT200_HIGHLIGHT_MOUSE");
    public static final ControlCode SET_BTN_MOTION_TRACKING     = new BaseTextControlCode(escape_CHR + "[?1002h").setName("SET_BTN_MOTION_TRACKING");
    public static final ControlCode SET_BTN_MOTION_TRACKING_DISABLE     = new BaseTextControlCode(escape_CHR + "[?1002l").setName("SET_BTN_MOTION_TRACKING_DISABLE");
    public static final ControlCode SET_BTN_EVENT_MOUSE         = new BaseTextControlCode(escape_CHR + "[?1002h").setName("SET_BTN_EVENT_MOUSE");
    public static final ControlCode SET_ANY_EVENT_MOUSE         = new BaseTextControlCode(escape_CHR + "[?1003h").setName("SET_ANY_EVENT_MOUSE");

    public static final ControlCode SET_FOCUS_EVENT_MOUSE       = new BaseTextControlCode(escape_CHR + "[?1004h").setName("SET_FOCUS_EVENT_MOUSE");

    public static final ControlCode SET_ALTERNATE_SCROLL        = new BaseTextControlCode(escape_CHR + "[?1007h").setName("SET_ALTERNATE_SCROLL");

    public static final ControlCode SET_EXT_MODE_MOUSE          = new BaseTextControlCode(escape_CHR + "[?1005h").setName("SET_EXT_MODE_MOUSE");
    public static final ControlCode SET_SGR_EXT_MODE_MOUSE      = new BaseTextControlCode(escape_CHR + "[?1006h").setName("SET_SGR_EXT_MODE_MOUSE");
    public static final ControlCode SET_URXVT_EXT_MODE_MOUSE    = new BaseTextControlCode(escape_CHR + "[?1015h").setName("SET_URXVT_EXT_MODE_MOUSE");
    public static final ControlCode SET_PIXEL_POSITION_MOUSE    = new BaseTextControlCode(escape_CHR + "[?1016h").setName("SET_PIXEL_POSITION_MOUSE");

    public static final ControlCode ENABLE_MOUSE_TRACKING  = SET_BTN_MOTION_TRACKING; //SET_VT200_HIGHLIGHT_MOUSE; //SET_VT200_MOUSE;
    public static final ControlCode DISABLE_MOUSE_TRACKING = new BaseTextControlCode(escape_CHR + "[?1000l").setName("DISABLE_MOUSE_TRACKING");

    public static final ControlCode DISABLE_SGR_EXT_MODE_MOUSE = new BaseTextControlCode(escape_CHR + "[?1006l").setName("DISABLE_SGR_EXT_MODE_MOUSE");

    public static final ControlCode ENABLE_MOUSE_DRAG_TRACKING =  new BaseTextControlCode("\033[?1003h").setName("ENABLE_MOUSE_DRAG_TRACKING");

    public static final ControlCode DISABLE_MOUSE_DRAG_TRACKING = new BaseTextControlCode("\033[?1003l").setName("DISABLE_MOUSE_DRAG_TRACKING");

    public static final ControlCode DISABLE_X10_MOUSE             = new BaseTextControlCode(escape_CHR + "[?9l").setName("DISABLE_X10_MOUSE");
    public static final ControlCode DISABLE_VT200_MOUSE           = new BaseTextControlCode(escape_CHR + "[?1000l").setName("DISABLE_VT200_MOUSE");
    public static final ControlCode DISABLE_VT200_HIGHLIGHT_MOUSE= new BaseTextControlCode(escape_CHR + "[?1001l").setName("DISABLE_VT200_HIGHLIGHT_MOUSE");
    public static final ControlCode DISABLE_BTN_MOTION_TRACKING   = new BaseTextControlCode(escape_CHR + "[?1002l").setName("DISABLE_BTN_MOTION_TRACKING");
    public static final ControlCode DISABLE_BTN_EVENT_MOUSE       = new BaseTextControlCode(escape_CHR + "[?1002l").setName("DISABLE_BTN_EVENT_MOUSE");
    public static final ControlCode DISABLE_ANY_EVENT_MOUSE       = new BaseTextControlCode(escape_CHR + "[?1003l").setName("DISABLE_ANY_EVENT_MOUSE");
    public static final ControlCode DISABLE_FOCUS_EVENT_MOUSE     = new BaseTextControlCode(escape_CHR + "[?1004l").setName("DISABLE_FOCUS_EVENT_MOUSE");
    public static final ControlCode DISABLE_ALTERNATE_SCROLL      = new BaseTextControlCode(escape_CHR + "[?1007l").setName("DISABLE_ALTERNATE_SCROLL");
    public static final ControlCode DISABLE_EXT_MODE_MOUSE        = new BaseTextControlCode(escape_CHR + "[?1005l").setName("DISABLE_EXT_MODE_MOUSE");
    public static final ControlCode DISABLE_URXVT_EXT_MODE_MOUSE  = new BaseTextControlCode(escape_CHR + "[?1015l").setName("DISABLE_URXVT_EXT_MODE_MOUSE");
    public static final ControlCode DISABLE_PIXEL_POSITION_MOUSE  = new BaseTextControlCode(escape_CHR + "[?1016l").setName("DISABLE_PIXEL_POSITION_MOUSE");
    
    
    
    public static final String      CURSOR_FAR_LEFT_STR = escape_CHR + "[G";
    public static final ControlCode CURSOR_LEFT         = new BaseTextControlCode(CURSOR_FAR_LEFT_STR).setName("CURSOR_LEFT");

    // Device status report (DSR): Answer is ESC [ 0 n (Terminal OK).
    public static final ControlCode DSR = new BaseTextControlCode(escape_CHR + "[5n").setName("DSR");

    // Cursor position report (CPR): Answer is ESC [ y ; x R, where
    // x,y is the cursor location.
    public static final ControlCode CPR = new BaseTextControlCode(escape_CHR + "[6n").setName("CPR");
    
    
    static Renderer make_CR() {
        
        final TextRenderer cooked_CR = new TextRenderer("\n");
        final Renderer raw_CR = new RendererBuffer().append(ControlCode.CURSOR_LEFT ).append( new CursorDown(1));
        
        Renderer the_CR = new Renderer() {

            @Override
            public void render(ControlCodeContext ctx) {
                if (ctx.isRaw()) {
                    raw_CR.render(ctx);
                } else {
                    cooked_CR.render(ctx);
                }
            }
        };
        
        return the_CR;
     }
    
    static class CarriageReturn implements Renderer , HasRowChange, HasName<CarriageReturn> {


        String name;
        final TextRenderer cooked_CR = new TextRenderer("\n");
        final Renderer raw_CR = new RendererBuffer().append(ControlCode.CURSOR_LEFT ).append( new CursorDown(1));
        
        @Override
        public void render(ControlCodeContext ctx) {
            if (ctx.isRaw()) {
                raw_CR.render(ctx);
            } else {
                cooked_CR.render(ctx);
            }
        }
        
        @Override
        public int deltaRow() {
            return 1;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public CarriageReturn setName(String name) {
            this.name = name;
            return this;
        }
        
        public String toString() {
            return HasName.UTIL.getName(this);
        }
         
    }
    
    
    // TODO - should this be moved to ControlCode class?
    public static Renderer CR = new CarriageReturn().setName("CR");
    public static TextRenderer LF = new TextRenderer("\r");
    
    protected static class CursorMovementWithCount extends BaseTextControlCode {

        protected int count;

        public CursorMovementWithCount(int count, String suffixChar) {
            super(escape_CHR + "[" + count + suffixChar);
            this.count = count;
        }

        protected int getCount() {
            return count;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + count;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            CursorMovementWithCount other = (CursorMovementWithCount) obj;
            if (count != other.count)
                return false;
            return true;
        }

    }

    public static class CursorUp extends CursorMovementWithCount implements HasRowChange {
        public CursorUp(int nLines) {
            super(nLines, "A");
        }

        @Override
        public int deltaRow() {
            return getCount()*-1;
        }
        
        public String getName() {
            return "CursorUp("+count+")";
        }
        
    }

    public static class CursorDown extends CursorMovementWithCount implements HasRowChange {
        public CursorDown(int nLines) {
            super(nLines, "B");
        }

        @Override
        public int deltaRow() {
            return getCount();
        }
    }

    public static CursorMovementWithCount moveToDeltaRow(int nLines) {
        if (nLines < 0) {
            return new CursorUp(nLines*-1);
        }
        return new CursorDown(nLines);
    }
    
    public static class CursorForward extends CursorMovementWithCount {
        public CursorForward(int nLines) {
            super(nLines, "C");
        }

        public int getCursorDelta() {
            return getCount();
        }
    }

    public static class CursorBackward extends CursorMovementWithCount {
        public CursorBackward(int positionsBackCount) {
            super(positionsBackCount, "D");
        }

        public int getCursorDelta() {
            return getCount()*-1;
        }
    }

    public static class CursorUpAndClearLines implements Renderer, HasRowChange {
        int nLines;

        public CursorUpAndClearLines(int nLines) {
            this.nLines = nLines;
        }

        @Override
        public void render(ControlCodeContext ctx) {
            if (!ctx.isControlCodesEnabled()) {
                return;
            }
            RendererBuffer rb = new RendererBuffer();
            rb.append(new CursorUp(nLines));
            for (int i = 0; i < nLines; i++) {
                // rb.append("|");
                rb.append(ERASE_END_OF_LINE);
                rb.append(ControlCode.CR); // todo - should I create and use a
                                           // movedown renderer? .. might be
                                           // smarter
            }
            rb.append(new CursorUp(nLines));
            rb.render(ctx);
        }

        @Override
        public int deltaRow() {
            return -nLines;
        }

    }
    
    public static interface HasBackgroundColor {
        boolean hasBackGroundColor();

        public static class UTIL {
            public static boolean hasBackGroundColor(Renderer r) {
                if (!(r instanceof HasBackgroundColor)) {
                    return false;
                }
                return ((HasBackgroundColor)r).hasBackGroundColor();
            }
        }
        
    }

    public static void main(String[] args) {
	
	List<ColorParsingUtils.ColorParseSegment> result = ColorParsingUtils.parseColors("Hello there "+escape_PREFIX+"1;2;3;5m What is going on?"+escape_PREFIX+"45;12;3;1;232mGeorge of the Jungle");
	
	System.out.println("Hello world!  " + result.size()+"\n\n");
	
	
	
	
        System.out.println(Colors.Gre.asString() +" Colors.Gre "+ Colors.Gre  + "\n\n");
	
        System.out.println(Colors.Gre.asBold().asString() +" Colors.Gre.asBold() "+ Colors.Gre.asBold()  + "\n\n");

        System.out.println(Colors.RCol.asString());
	
    }

}
