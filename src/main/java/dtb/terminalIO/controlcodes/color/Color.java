package dtb.terminalIO.controlcodes.color;

import java.util.Comparator;
import java.util.StringJoiner;

import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.HasBackgroundColor;
import dtb.terminalIO.controlcodes.ControlCodeContext;
import dtb.terminalIO.controlcodes.color.Colors.BgColor;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.controlcodes.color.Colors.FgColor;
import dtb.terminalIO.util.IntArrayComparator;

/**
 * A {@link ControlCode} that encapsulates a Color
 */
public abstract class Color extends ControlCode implements ColorProvider, HasBackgroundColor {

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

    protected int[] codes;

    abstract protected int[] buildControlCodes();

    final protected int[] getControlCodes() {
        if (codes == null) {
            codes = buildControlCodes();
        }
        return codes;
    }

    protected ColorTransform boldTransform = ColorTransform.NULL_colorTransform;
    ColorTransform invertTransform = ColorTransform.NULL_colorTransform;
    public static final ColorComparator COLOR_COMPARE = new ColorComparator();

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
        return mergeColors(this, other);
    }

    public Color getBackGroundColor() {
        return this;
    }

    public Color getForeGroundColor() {
        return this;
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

    public Colors.FGBGColors newFrom(Colors.FgColor fgcNew) {
        throw new IllegalStateException("Not implemented! Only works for FGBGColors object");
    }

    public Colors.FGBGColors newFrom(Colors.BgColor bgcNew) {
        throw new IllegalStateException("Not implemented! Only works for FGBGColors object");
    }

    public Color bold() {
        return getBoldTransform().transform(this);
    }

    public Color invert() {
        return getInvertTransform().transform(this);
    }

    public static Color fromStr(String colorStr) {
        return new Colors.ColorFromString(colorStr);
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
        return Colors.ColorFromCodes.instance(newCodes);
    }

    public Color appendCodes(Color color) {
        return appendCodes(color.getControlCodes());
    }
    
    
    Color mergeColors(Color c1, Color c2) {
        if (c1 == null && c2 == null) {
            throw new NullPointerException("Cannot merge two null colors!");
        }

        if (c1 == c2 || c2 == Colors.RCol) {
            return c1;
        }

        if (c1 == Colors.RCol) {
            return c2;
        }

        if (c1 instanceof FGBGColors && c2 instanceof FGBGColors) {
            Color fg = c1.getForeGroundColor() instanceof FgColor ?  c1.getForeGroundColor() : null;
            Color bg = c2.getBackGroundColor() instanceof BgColor ?  c2.getBackGroundColor() : null;
            return new FGBGColors((FgColor)fg, (BgColor)bg);
        }

        if (c1 instanceof FGBGColors && c2 != null) {
            Color fg = c1.getForeGroundColor() instanceof FgColor ?  c1.getForeGroundColor() : null;
            Color bg = c2 instanceof BgColor ? c2 : null;
            if (bg == null && c2 instanceof FgColor) {
                bg = c2.invert();
                if (!(bg instanceof BgColor)) {
                    bg = null;
                }
            }

            if (!(fg instanceof FgColor)) {
                throw new IllegalStateException("GOT HERE! fg is a " + fg.getClass().getCanonicalName());
            }

            return new FGBGColors((FgColor)fg, (BgColor)bg);
        }

        if (c2 instanceof FGBGColors && c1 != null) {
            Color bg = c2.getBackGroundColor() instanceof BgColor ?  c2.getBackGroundColor() : null;
            Color fg = c1 instanceof FgColor ? c2 : null;
            if (fg == null && c1 instanceof BgColor) {
                fg = c1.invert();
                if (!(fg instanceof FgColor)) {
                    bg = null;
                }
            }

            if (fg instanceof FGBGColors) {
                if (bg != null && bg instanceof BgColor) {
                    ((FGBGColors)fg).bgc = (BgColor)bg;
                }
                return fg;
            }
            
            return new FGBGColors((FgColor)fg, (BgColor)bg);
        }

        if ((c1 == null || c1 instanceof FgColor) && (c2 == null || c2 instanceof BgColor)) {
            return new FGBGColors((FgColor)c1, (BgColor)c2);
        }
        
        if ((c1 == null || c1 instanceof BgColor) && (c2 == null || c2 instanceof FgColor)) {
            return new FGBGColors((FgColor)c2, (BgColor)c1);
        }

        if ((c1 == null || c1 instanceof FgColor) && (c2 == null || c2 instanceof FgColor)) {
            Color cb2 = c2.invert();
            return new FGBGColors((FgColor)c1, cb2 instanceof BgColor ? (BgColor)cb2 : null);
        }
        
        if ((c1 == null || c1 instanceof BgColor) && (c2 == null || c2 instanceof BgColor)) {
            Color cb2 = c2.invert();
            return new FGBGColors(cb2 instanceof FgColor ? (FgColor)cb2 : null, (BgColor)c1);
        }
        
        // TODO:  handle RGB colors
        
        return c1;
        
    }
    
    public static BgColor backgroundColor(Color color) {
        if (color == null) {
            return null;
        }
        if (color instanceof FGBGColors ) {
            Color bg = color.getBackGroundColor() == null ? null : (color.getBackGroundColor() instanceof BgColor ? color.getBackGroundColor() : null);
            return (BgColor)bg;
        }

        if (color instanceof BgColor ) {
            return (BgColor) color;
        }
        
        return null;
    }
    
    public static boolean hasBackgroundColor(Color color) {
        return backgroundColor(color) != null; 
    }
    
 // In Color.java
    public Color asDisabled() {
        return Colors.DEFAULT_DISABLED_COLOR;
    }
    
    

}