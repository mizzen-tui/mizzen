package dtb.terminalIO.controlcodes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors.HasRowChange;
import dtb.terminalIO.textsegments.CleanString;
import dtb.terminalIO.util.IntArrayComparator;


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


    String name;
    
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
    }

//    static int maxColorCodeLen = -1;
//    static int colorCount = 0;

    /**
     * A {@link ControlCode} that encapsulates a Color
     */
    public static abstract class Color extends ControlCode implements ColorProvider {

        protected int[] codes;

        abstract protected int[] buildControlCodes();

        final protected int[] getControlCodes() {
            if (codes == null) {
                codes = buildControlCodes();
            }
            return codes;
        }

        ColorTransform boldTransform = ColorTransform.NULL_colorTransform;
        ColorTransform invertTransform = ColorTransform.NULL_colorTransform;

        public Color setBoldTransform(ColorTransform boldTransform ) {
            this.boldTransform = boldTransform;
            return this;
        }

        public ColorTransform getBoldTransform() {
            return this.boldTransform;
        }

        public ColorTransform getInvertTransform() {
            return invertTransform;
        }

        public Color setInvertTransform(ColorTransform invertTransform) {
            this.invertTransform = invertTransform;
            return this;
        }

        public Color getColor() {
            return this;
        }

        public Color mergeWith(Color other) {
            return this; // TODO:  fix!
        }

        public boolean isMergeable() {
            return false;
        }

        final protected String buildColorControlCode() {
            int codes[] = getControlCodes();
            StringJoiner sj = new StringJoiner(";");
            for (int code : codes) {
                sj.add(code + "");
            }
            return escape_PREFIX + sj.toString() + "m";
        }

        protected void doRender(ControlCodeContext ctx) {
            if (ctx.isColorEnabled()) {
                ctx.write(this.buildColorControlCode());
            }
        }
        
        public FGBGColors newFrom(FgColor fgcNew) {
            throw new IllegalStateException("Not implemented! Only works for FGBGColors object");
        }

        public FGBGColors newFrom(BgColor bgcNew) {
            throw new IllegalStateException("Not implemented! Only works for FGBGColors object");
        }

        public Color bold() {
            return getBoldTransform().transform(this);
        }

        public Color invert() {
            return getInvertTransform().transform(this);
        }

        public static Color fromStr(String colorStr) {
            return new ColorFromString(colorStr);
        }
        
        public DisplayableValue<Color> asDisplayableValue() {
            return new DisplayableValue<Color>() {
                @Override
                public Color getValue() {
                    return Color.this;
                }
                @Override
                public String getDisplayValue() {
                    return Color.this.toString();
                }
            };
        }
        
        /**
         * Returns itself with more color codes
         */
        public Color appendCodes(int[] additionalCodes) {
            int[] codes = getControlCodes();
            int codesLength = codes.length;
            int[] newCodes = new int[additionalCodes.length + codesLength];
            
            int i = 0;
            for (int j=0; j<codesLength; j++) {
                newCodes[i] = codes[j];
                i++;
            }
            for (int j=0; j<additionalCodes.length; j++) {
                newCodes[i] = additionalCodes[j];
                i++;
            }
            return ColorFromCodes.instance(newCodes);
        }
        
        public Color appendCodes(Color color) {
            return appendCodes(color.getControlCodes());
        }
        
    }

    public static class ColorComparator implements Comparator<Color> {

        @Override
        public int compare(Color c1, Color c2) {
            if (c1 == c2) {
                return 0;
            }

            int[] c1codes = c1.getControlCodes();
            int[] c2codes = c2.getControlCodes();
            return IntArrayComparator.INTARRAY_COMPARE.compare(c1codes, c2codes);
        }
        
    }
    
    public static final ColorComparator COLOR_COMPARE = new ColorComparator();

    static TreeMap<int[],Color> colorsCache = new TreeMap<int[],Color>(IntArrayComparator.INTARRAY_COMPARE);

    public static Color getColorFromCache(int[] codes) {
        return colorsCache.get(codes);
    }

    public static <T extends Color> T getColorFromCacheOrBuild(int[] codes, 
                                                               Supplier<T> makeColor) {
        T color = (T) colorsCache.get(codes);
        if (color == null) {
            color = makeColor.get();
            colorsCache.put(color.getControlCodes(), color);
        } 
        return color;
    }

    public static class ColorFromCodes extends Color {

        public static Color instance(int[] codes) {
            return getColorFromCacheOrBuild(codes, ()->new ColorFromCodes(codes));
        }
        
        public static Color instance(Color color) {
            return getColorFromCacheOrBuild(color.getControlCodes(), ()->new ColorFromCodes(color));
        }
        
        private ColorFromCodes(int[] codes) {
            this.codes = codes;
        }
        
        private ColorFromCodes(Color color) {
            this(color.getControlCodes());
        }
        

        
        @Override
        protected int[] buildControlCodes() {
            return codes;
        }
    }
    
    
    public static class ColorFromString extends Color {

        int[] codes;
        
        public ColorFromString(String colorStr) {
            colorStr = colorStr.replace(escape_PREFIX,"");
            colorStr = colorStr.replace("m","");
            String[] codesStr = colorStr.split(";");
            codes = new int[codesStr.length];
            for (int i = 0; i< codes.length; i++) {
                codes[i] = Integer.parseInt(codesStr[i]);
            }
        }
        
        @Override
        protected int[] buildControlCodes() {
            return codes;
        }
        
    }


    public static abstract class SimpleColor extends Color {
        int colorCode = 0;

        public SimpleColor(int colorCode) {
            this.colorCode = colorCode;
        }

        public int getColorCode() {
            return colorCode;
        }

        public void setColorCode(int colorCode) {
            this.colorCode = colorCode;
        }

    }

    /**
     * Foreground Color
     */
    public static class FgColor extends SimpleColor {

        boolean bold      = false;
        boolean italic    = false;
        boolean underline = false;

        public static FgColor instance(int colorCode) {
            int[] codes =  __buildControlCodes(false, false, false, colorCode);
            return getColorFromCacheOrBuild(codes, ()->new FgColor(colorCode));
        }

        public static FgColor instance(int colorCode, boolean bold, boolean italic, boolean underline) {
            int[] codes =  __buildControlCodes(bold, italic, underline, colorCode);
            return getColorFromCacheOrBuild(codes, ()->new FgColor(colorCode,bold, italic, underline));
        }
        
        private FgColor(int colorCode) {
            super(colorCode);
            this.setBoldTransform((color)->asBold());
        }

        private FgColor(int colorCode, boolean bold, boolean italic, boolean underline) {
            this(colorCode);
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.setBoldTransform((color)->asBold());
        }
        
        public FgColor setInvertTransform(ColorTransform invertTransform) {
            super.setInvertTransform(invertTransform);
            return this;
        }


        public FgColor asBold() {
            return FgColor.instance(colorCode, true, italic, underline);
        }
        
        public FgColor removeBold() {
            return FgColor.instance(colorCode, false, italic, underline);
        }

        public FgColor asItalic() {
            return FgColor.instance(colorCode, bold, true, underline);
        }

        public FgColor asUnderline() {
            return FgColor.instance(colorCode, bold, italic, true);
        }

        @Override
        protected int[] buildControlCodes() {
            return __buildControlCodes(bold, italic, underline, this.getColorCode());
        }
        
        static int[] __buildControlCodes(boolean bold,
                                         boolean italic,
                                         boolean underline,
                                         int colorCode) {
            int codes[] = new int[(bold ? 1 : 0) + (italic ? 1 : 0) + (underline ? 1 : 0) + 2];
            int idx = 0;
            codes[idx++] = 0; // reset previous attributes
            if (bold)
                codes[idx++] = 1; // conditionally set bold
            if (italic)
                codes[idx++] = 3; // conditionally set italic
            if (underline)
                codes[idx++] = 4; // conditionally set underline
            codes[idx] = colorCode; // now finally set the foreground
                                              // color
            return codes;
        }

        public String toString() {
            String name = Colors.getNameOfFGColor(this);
            if (name == null) {
                name = this.buildColorControlCode();
            }
            return "[FG=" + name + "]";
        }

    }

    /*
     * Background color
     */
    public static class BgColor extends SimpleColor {

        public static BgColor instance(int colorCode) {
            return getColorFromCacheOrBuild(new int[] { colorCode }, ()->new BgColor(colorCode));
        }
        
        private BgColor(int colorCode) {
            super(colorCode);
        }

        public BgColor setInvertTransform(ColorTransform invertTransform) {
            super.setInvertTransform(invertTransform);
            return this;
        }
        
        @Override
        protected int[] buildControlCodes() {
            return new int[] { this.getColorCode() };
        }

        public String toString() {
            String name = Colors.getNameOfBGColor(this);
            if (name == null) {
                name = this.buildColorControlCode();
            }
            return "[BG=" + name + "]";
        }
        
        
    }

    /**
     * encapsulate foreground and background colors together
     */
    public static class FGBGColors extends Color {

        public static final Color NO_COLOR_SPECIFIED = ControlCode.DEFAULT_COLOR ; //new FGBGColors(null, null);

        BgColor                        bgc;
        FgColor                        fgc;

        public FGBGColors(FgColor fgc, BgColor bgc) {
            super();
            this.bgc = bgc;
            this.fgc = fgc;
            this.setBoldTransform((color)->asBold());
        }

        public FGBGColors(FgColor fgc) {
            super();
            this.bgc = null;
            this.fgc = fgc;
            this.setBoldTransform((color)->asBold());
        }

        public FGBGColors(BgColor bgc) {
            super();
            this.bgc = bgc;
            this.fgc = null;
            this.setBoldTransform((color)->asBold());
//            this.setInvertTransform( (color) -> new  FGBGColors((FgColor)bgc==null?null: bgc.invert()) , ((BgColor)fgc==null?null:fgc.invert())) );
        }

        
        public FGBGColors asBold() {
            return new FGBGColors((fgc==null?(new FgColor(0)):fgc).asBold(), bgc);
        }
        
        
        protected ColorTransform buildDefaultColorTransform() {
            if (this.fgc != null && this.fgc.getInvertTransform() != null) {
                
                if (this.bgc == null || this.bgc.getInvertTransform() == null) {
                    return this.fgc.getInvertTransform(); 
                }
                
                int[] fgcInvertedColorCodes = this.fgc.getInvertTransform().transform(this.fgc).getControlCodes();
                int[] bgcInvertedColorCodes = this.bgc.getInvertTransform().transform(this.bgc).getControlCodes();
                
                int[] combinedCodes = new int[fgcInvertedColorCodes.length + bgcInvertedColorCodes.length];
                System.arraycopy(fgcInvertedColorCodes, 0, combinedCodes, 0, fgcInvertedColorCodes.length);
                System.arraycopy(bgcInvertedColorCodes, 0, combinedCodes, fgcInvertedColorCodes.length, bgcInvertedColorCodes.length);
                
                return (color) -> new ColorFromCodes(combinedCodes).setInvertTransform((c)->FGBGColors.this);
                
                
            } else if ((this.bgc != null && this.bgc.getInvertTransform()!=null)) {
                return this.bgc.getInvertTransform();
            } else {
                return ColorTransform.NULL_colorTransform;
            }
        }
        
        public ColorTransform getInvertTransform() {
            return buildDefaultColorTransform();
        }


        public FGBGColors removebold() {
            return new FGBGColors(fgc.removeBold(), bgc);
        }

        public BgColor getBgc() {
            return bgc;
        }

        public void setBgc(BgColor bgc) {
            this.bgc = bgc;
        }

        public FgColor getFgc() {
            return fgc;
        }

        public void setFgc(FgColor fgc) {
            this.fgc = fgc;
        }

        @Override
        public boolean isMergeable() {
            return this.getBgc() == null || this.getFgc() == null;
        }

        @Override
        public Color mergeWith(Color otherColor) {
            if (! (otherColor instanceof FGBGColors)) {
                return super.mergeWith(otherColor);
            }
            FGBGColors other = (FGBGColors) otherColor;
            FGBGColors newColor = new FGBGColors(this.getFgc() != null ? this.getFgc() : other.getFgc(),
                                                 this.getBgc() != null ? this.getBgc() : other.getBgc());
            return newColor;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((bgc == null) ? 0 : bgc.hashCode());
            result = prime * result + ((fgc == null) ? 0 : fgc.hashCode());
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
            FGBGColors other = (FGBGColors) obj;
            if (bgc == null) {
                if (other.bgc != null)
                    return false;
            } else if (!bgc.equals(other.bgc))
                return false;
            if (fgc == null) {
                if (other.fgc != null)
                    return false;
            } else if (!fgc.equals(other.fgc))
                return false;
            return true;
        }

        @Override
        protected int[] buildControlCodes() {
            int [] fgcControlCodes = fgc != null? fgc.getControlCodes() : new int[] {};
            int [] bgcControlCodes = bgc != null? bgc.getControlCodes() : new int[] {};
            
            int[] controlCodes = new int[fgcControlCodes.length + bgcControlCodes.length];
            int i = 0;
            for (int j=0; j<fgcControlCodes.length; j++) {
                controlCodes[i] = fgcControlCodes[j];
                i++;
            }
            for (int j=0; j<bgcControlCodes.length; j++) {
                controlCodes[i] = bgcControlCodes[j];
                i++;
            }
            return controlCodes;
        }

        public FGBGColors newFrom(FgColor fgcNew) {
            return new FGBGColors(fgcNew, this.getBgc());
        }

        public FGBGColors newFrom(BgColor bgcNew) {
            return new FGBGColors(this.getFgc(), bgcNew);
        }

        public String toString() {
            return "(" + String.valueOf(fgc) + "," + String.valueOf(bgc) + ")";
        }

        public interface HasRowChange {
            int deltaRow();
        }

    }

    public static final String      escape_CHR          = "\u001B";
    public static final String      escape_PREFIX       = escape_CHR + "[";

    public static final Color RCol                = new Color() {
                                                            @Override
                                                            protected int[] buildControlCodes() {
                                                                boldTransform = (c)-> new Color() {
                                                                    protected int[] buildControlCodes() {
                                                                        return new int[] {0,1};
                                                                    }
                                                                };
                                                                invertTransform = ColorTransform.NULL_colorTransform;
                                                                return new int[] { 0 };
                                                            }
                                                            public Color setInvertTransform(ColorTransform invertTransform) {
                                                                throw new IllegalStateException("Cannot change DEFAULT color!");
                                                            }
                                                            public Color setBoldTransform(ColorTransform boldTransform ) {
                                                                throw new IllegalStateException("Cannot change DEFAULT color!");
                                                            }
                                                            
                                                            public String toString() {
                                                                return "RCol";
                                                            }
                                                            
                                                        };

     public static final Color DEFAULT_COLOR            = RCol ; // new FGBGColors(Colors.Whi, null);

                                                        
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
    public static final ControlCode SET_BTN_EVENT_MOUSE         = new BaseTextControlCode(escape_CHR + "[?10021h").setName("SET_BTN_EVENT_MOUSE");
    public static final ControlCode SET_ANY_EVENT_MOUSE         = new BaseTextControlCode(escape_CHR + "[?10031h").setName("SET_ANY_EVENT_MOUSE");

    public static final ControlCode SET_FOCUS_EVENT_MOUSE       = new BaseTextControlCode(escape_CHR + "[?10041h").setName("SET_FOCUS_EVENT_MOUSE");

    public static final ControlCode SET_ALTERNATE_SCROLL        = new BaseTextControlCode(escape_CHR + "[?10071h").setName("SET_ALTERNATE_SCROLL");

    public static final ControlCode SET_EXT_MODE_MOUSE          = new BaseTextControlCode(escape_CHR + "[?10051h").setName("SET_EXT_MODE_MOUSE");
    public static final ControlCode SET_SGR_EXT_MODE_MOUSE      = new BaseTextControlCode(escape_CHR + "[?10061h").setName("SET_SGR_EXT_MODE_MOUSE");
    public static final ControlCode SET_URXVT_EXT_MODE_MOUSE    = new BaseTextControlCode(escape_CHR + "[?10151h").setName("SET_URXVT_EXT_MODE_MOUSE");
    public static final ControlCode SET_PIXEL_POSITION_MOUSE    = new BaseTextControlCode(escape_CHR + "[?10161h").setName("SET_PIXEL_POSITION_MOUSE");

    public static final ControlCode ENABLE_MOUSE_TRACKING  = SET_VT200_MOUSE;
    public static final ControlCode DISABLE_MOUSE_TRACKING = new BaseTextControlCode(escape_CHR + "[?1000l").setName("ERASE_END_OF_LINE");

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

    }

    public static class CursorUp extends CursorMovementWithCount implements HasRowChange {
        public CursorUp(int nLines) {
            super(nLines, "A");
        }

        @Override
        public int deltaRow() {
            return getCount()*-1;
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
    }

    public static class CursorBackward extends CursorMovementWithCount {
        public CursorBackward(int nLines) {
            super(nLines, "D");
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

    static String  colorCODE_patternStr = "\\e\\[[0-9,;]*m";
    static Pattern colorCODE_pattern    = Pattern.compile(colorCODE_patternStr);

    public static boolean hasColorCodes(String source) {
        Matcher matcher = colorCODE_pattern.matcher(source);
        boolean matchFound = matcher.find();
        return matchFound;
    }

    public static String removeColorCodes(String source) {

        Matcher matcher = colorCODE_pattern.matcher(source);
        boolean matchFound = matcher.find();
        if (!matchFound) {
            return source;
        }
        StringBuffer sb = new StringBuffer();
        while (matchFound) {
            matcher.appendReplacement(sb, "");
            matchFound = matcher.find();
        }
        matcher.appendTail(sb);

        return sb.toString();
    }
    
    public static class ColorParseSegment {
        Color color;
        CleanString simpleText;
        public ColorParseSegment(Color color, CleanString simpleText) {
            super();
            this.color = color;
            this.simpleText = simpleText;
        }
        
        public int length() {
            return this.simpleText!=null?this.simpleText.length():0;
        }
	@Override
	public String toString() {
	    return "ColorParseSegment {color=" + color + ", simpleText=" + simpleText + "}";
	}
    }

    static final int PARSE_TEXT=1;
    static final int PARSE_COLOR_START=2;
    static final int PARSE_COLOR_CODE=3;
    static final int PARSE_COLOR_END=4;

    
    public static interface ColorColorSegmentBuilder {
        boolean newColorSegment(Color color, CleanString simpleText);
    }

    @FunctionalInterface
    public static interface ColorProvider {
        Color getColor();
    }

    public static void parseColorsAndBuildResult(String colorCodedStr,
                                                 ColorColorSegmentBuilder colorColorSegmentBuilder) {

        int PARSE_STATE = PARSE_TEXT;
        int codesBuffer[] = new int[50];
        int codesBufferLen = 0;

        StringBuffer sb = new StringBuffer(50);
        sb.setLength(0);
        CleanString simpleText = null;
        Color color = null;
        int colorCodedStrLength = colorCodedStr.length();
        for (int i = 0; i < colorCodedStrLength; i++) {

            char ch = colorCodedStr.charAt(i);

            switch (PARSE_STATE) {
            case PARSE_TEXT:
                if (ch == '\u001B') {
                    simpleText = new CleanString.Simple(sb.toString());
                    boolean continueParsing = colorColorSegmentBuilder.newColorSegment(color, simpleText);
                    if (!continueParsing)
                        return;
                    // result.add( new ColorParseSegment(color, simpleText) );
                    PARSE_STATE = PARSE_COLOR_START;
                    simpleText = null;
                    color = null;
                    sb = new StringBuffer();
                    sb.append(ch);
                } else {
                    sb.append(ch);
                }
                break;

            case PARSE_COLOR_START:
                if (ch == '[') {
                    break; // ignore
                } else {
                    if (Character.isDigit(ch)) {
                        PARSE_STATE = PARSE_COLOR_CODE;
                        codesBufferLen = 0;
                        sb = new StringBuffer();
                    } else {
                        PARSE_STATE = PARSE_TEXT;
                    }
                    sb.append(ch);
                }
                break;

            case PARSE_COLOR_CODE:
                if (ch == ';' || ch == 'm') {
                    String num = sb.toString();
                    sb.append(ch);
                    if (num.length() == 0) {
                        PARSE_STATE = PARSE_TEXT;
                        break;
                    }
                    int code = Integer.parseInt(num);
                    codesBuffer[codesBufferLen] = code;
                    codesBufferLen++;
                    sb = new StringBuffer();
                    if (ch == 'm' || codesBufferLen >= codesBuffer.length) {
                        PARSE_STATE = PARSE_COLOR_END;
                    }
                } else {
                    sb.append(ch);
                    if (!Character.isDigit(ch)) {
                        PARSE_STATE = PARSE_TEXT;
                    }
                }
                break;

            case PARSE_COLOR_END:
                int[] codes = new int[codesBufferLen];
                System.arraycopy(codesBuffer, 0, codes, 0, codesBufferLen);
                color = ColorFromCodes.instance(codes);
                PARSE_STATE = PARSE_TEXT;

                if (ch == '\u001B') { // this can happen if we have color codes back to back.
                    simpleText = new CleanString.Simple(sb.toString());
                    boolean continueParsing = colorColorSegmentBuilder.newColorSegment(color,
                            CleanString.UTIL.EMPTY_CLEAN_STRING);
                    if (!continueParsing)
                        return;
                    PARSE_STATE = PARSE_COLOR_START;
                    simpleText = null;
                    color = null;
                    sb = new StringBuffer();
                }

                sb.append(ch);
                break;

            default:
                throw new IllegalStateException("Should not get here!");
            }
        }

        if (sb.length() > 0) {
            colorColorSegmentBuilder.newColorSegment(color, new CleanString.Simple(sb.toString()));
        }
    }

    public static List<ColorParseSegment> parseColors(String colorCodedStr) {
        final List<ColorParseSegment> result = new ArrayList<ColorParseSegment>();

        parseColorsAndBuildResult(colorCodedStr, new ColorColorSegmentBuilder() {
            @Override
            public boolean newColorSegment(Color color, CleanString simpleText) {
                result.add(new ColorParseSegment(color, simpleText));
                return true; // true means keep parsing
            }
        });

        return result;

    }
    
    public static void main(String[] args) {
	
	List<ColorParseSegment> result = parseColors("Hello there "+escape_PREFIX+"1;2;3;5m What is going on?"+escape_PREFIX+"45;12;3;1;232mGeorge of the Jungle");
	
	System.out.println("Hello world!  " + result.size()+"\n\n");
	
	
	
	
        System.out.println(Colors.Gre.asString() +" Colors.Gre "+ Colors.Gre  + "\n\n");
	
        System.out.println(Colors.Gre.asBold().asString() +" Colors.Gre.asBold() "+ Colors.Gre.asBold()  + "\n\n");

        System.out.println(RCol.asString());
	
    }

}
