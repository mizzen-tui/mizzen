package dtb.terminalIO.controlcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dtb.terminalIO.controlcodes.ControlCode.FGBGColors.HasRowChange;
import dtb.terminalIO.textsegments.CleanString;


/**
 * A ControlCode is a {@link Renderer} that renders some 
 * kind of linux terminal control code (or linux escape sequence) to
 * manipulate either cursor position,  color of subsequent
 * text output. etc ..  It is the fundamental building block
 * used in this library to manipulate the the Linux Terminal
 * 
 * @author david
 *
 */
public abstract class ControlCode implements Renderer {

    public ControlCode() {
    }

    @Override
    public void render(ControlCodeContext ctx) {
        if (ctx.isControlCodesEnabled()) {
            doRender(ctx);
        }
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
            // ctx.write(this.getControlCode());
            ctx.write(this);
        }
    }

    

    /**
     * A {@link ControlCode} that encapsulates a Color
     */
    public static abstract class Color extends ControlCode implements ColorProvider {

        abstract protected int[] buildControlCodes();
        
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

        protected String buildColorControlCode() {
            int codes[] = buildControlCodes();
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
            throw new IllegalStateException("not implemented! only works for FGBGColors object");
        }

        public FGBGColors newFrom(BgColor bgcNew) {
            throw new IllegalStateException("not implemented!only works for FGBGColors object");
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
        
    }
    
    public static class ColorFromCodes extends Color {

        int[] codes;
        
        public ColorFromCodes(int[] codes) {
            this.codes = codes;
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

        public FgColor(int colorCode) {
            super(colorCode);
            this.setBoldTransform((color)->asBold());
        }

        public FgColor(int colorCode, boolean bold, boolean italic, boolean underline) {
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
            return new FgColor(colorCode, true, italic, underline);
        }
        
        public FgColor removeBold() {
            return new FgColor(colorCode, false, italic, underline);
        }

        public FgColor asItalic() {
            return new FgColor(colorCode, bold, true, underline);
        }

        public FgColor asUnderline() {
            return new FgColor(colorCode, bold, italic, true);
        }

        @Override
        protected int[] buildControlCodes() {
            int codes[] = new int[(bold ? 1 : 0) + (italic ? 1 : 0) + (underline ? 1 : 0) + 2];
            int idx = 0;
            codes[idx++] = 0; // reset previous attributes
            if (bold)
                codes[idx++] = 1; // conditionally set bold
            if (italic)
                codes[idx++] = 3; // conditionally set italic
            if (underline)
                codes[idx++] = 4; // conditionally set underline
            codes[idx] = this.getColorCode(); // now finally set the foreground
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

        public BgColor(int colorCode) {
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

        // public static final FGBGColors DEFAULT = new
        // FGBGColors(Colors.On_Bla,Colors.Whi);
        public static final FGBGColors DEFAULT            = new FGBGColors(Colors.Whi, null);

        public static final FGBGColors NO_COLOR_SPECIFIED = new FGBGColors(null, null);

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
        
        
        public Color setInvertTransform(ColorTransform invertTransform) {
            this.invertTransform = invertTransform;
            return this;
        }
        
        
//        public FGBGColors bold() {
//            return new FGBGColors((fgc==null?(new FgColor(0)):fgc).asBold(), bgc);
//        }

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
            int [] fgcControlCodes = fgc != null? fgc.buildControlCodes() : new int[] {};
            int [] bgcControlCodes = bgc != null? bgc.buildControlCodes() : new int[] {};
            
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

    public static final ControlCode RCol                = new Color() {
                                                            @Override
                                                            protected int[] buildControlCodes() {
                                                                return new int[] { 0 };
                                                            }
                                                        };

    // erasing ..

    public static final ControlCode ERASE_END_OF_LINE   = new BaseTextControlCode(escape_CHR + "[K");
    public static final ControlCode ERASE_START_OF_LINE = new BaseTextControlCode(escape_CHR + "[1K");
    public static final ControlCode ERASE_LINE          = new BaseTextControlCode(escape_CHR + "[2K");
    public static final ControlCode ERASE_DOWN          = new BaseTextControlCode(escape_CHR + "[J");
    public static final ControlCode ERASE_UP            = new BaseTextControlCode(escape_CHR + "[1J");
    public static final ControlCode ERASE_SCREEN        = new BaseTextControlCode(escape_CHR + "[2J");

    // \033[?25l")
    public static final ControlCode HIDE_CURSOR         = new BaseTextControlCode(escape_CHR + "[?25l");
    public static final ControlCode SHOW_CURSOR         = new BaseTextControlCode(escape_CHR + "[?25h");

    public static final String      CURSOR_FAR_LEFT_STR = escape_CHR + "[G";
    public static final ControlCode CURSOR_LEFT         = new BaseTextControlCode(CURSOR_FAR_LEFT_STR);

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
            return -getCount();
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
                rb.append(Renderer.CR); // todo - should I create and use a
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

    public static void parseColorsAndBuildResult(String colorCodedStr, ColorColorSegmentBuilder colorColorSegmentBuilder) {
        
        int PARSE_STATE=PARSE_TEXT;
        int codesBuffer[] = new int[50];
        int codesBufferLen=0;
        
        StringBuffer sb = new StringBuffer(50);
        sb.setLength(0);
        CleanString simpleText = null;
        Color color = null;
        int colorCodedStrLength = colorCodedStr.length();
        for (int i=0; i<colorCodedStrLength; i++) {
            
            char ch = colorCodedStr.charAt(i);
            
            switch (PARSE_STATE) {
	    case PARSE_TEXT:
                if (ch == '\u001B') {
                    simpleText = new CleanString.Simple(sb.toString());
                    boolean continueParsing = colorColorSegmentBuilder.newColorSegment(color, simpleText);
                    if (!continueParsing) return;
                    //result.add( new  ColorParseSegment(color, simpleText) ); 
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
                    break;  // ignore
                } else {
                    if (Character.isDigit(ch)) {
                	PARSE_STATE = PARSE_COLOR_CODE;
                	codesBufferLen=0;
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
                    if (num.length()==0) {
                	PARSE_STATE = PARSE_TEXT;
                	break;
                    }
                    int code=Integer.parseInt(num);
                    codesBuffer[codesBufferLen] = code;
                    codesBufferLen++;
                    sb = new StringBuffer();
                    if (ch == 'm' || codesBufferLen >= codesBuffer.length) {
                	PARSE_STATE = PARSE_COLOR_END;
                    }
                } else {
                    sb.append(ch);
                    if (! Character.isDigit(ch)) {
                	PARSE_STATE = PARSE_TEXT;
                    }
                }
		break;

	    case PARSE_COLOR_END:
		int[] codes = new int[codesBufferLen];
		System.arraycopy(codesBuffer, 0, codes, 0, codesBufferLen);
		color = new ColorFromCodes(codes);
		PARSE_STATE = PARSE_TEXT;
                
		if (ch == '\u001B') { // this can happen if we have color codes back to back.
                    simpleText = new CleanString.Simple(sb.toString());
                    boolean continueParsing = colorColorSegmentBuilder.newColorSegment(color, CleanString.UTIL.EMPTY_CLEAN_STRING);
                    if (!continueParsing) return;
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
        
      if (sb.length()>0) {
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
