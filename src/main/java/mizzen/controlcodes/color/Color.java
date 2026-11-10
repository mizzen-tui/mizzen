package mizzen.controlcodes.color;

import java.util.Comparator;
import java.util.StringJoiner;

import mizzen.components.input.DisplayableValue;
import mizzen.controlcodes.ControlCode;
import mizzen.controlcodes.ControlCode.HasBackgroundColor;
import mizzen.controlcodes.ControlCodeContext;
import mizzen.controlcodes.color.Colors.BgColor;
import mizzen.controlcodes.color.Colors.FGBGColors;
import mizzen.controlcodes.color.Colors.FgColor;
import mizzen.util.IntArrayComparator;

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

        if (!isNamed(c1) || !isNamed(c2)) {             // RGB, or built from codes: merged by reading their codes
            return mergeByParts(c1, c2);
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
            Color fg = c1 instanceof FgColor ? c1 : null;     // c1's foreground (was c2: it lost c1, and changed c2 - a shared colour)
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
        
        return c1;
        
    }

    /**
     * A colour that sets no background takes the background of what it is drawn on, as in a terminal: only a
     * foreground given means "the background stays". For example red text on a card - Colors.IRed is a
     * foreground only - shows on the card's background, not the screen's:
     * <pre>
     *     onBackgroundOf(Colors.IRed, cardColor)       red on the card's background
     *     onBackgroundOf(rgb(red, white), cardColor)   as it is: it sets its own background
     *     onBackgroundOf(Colors.IRed, legacyText)      as it is: there is no background to take (Legacy has none)
     * </pre>
     * It is foreground over background, part by part - not mergeWith, whose older rules for the named colours
     * do other things (a foreground-only second colour is inverted into a background).
     */
    public static Color onBackgroundOf(Color color, Color under) {
        if (color == null || under == null || setsBackground(color) || !setsBackground(under)) {
            return color;
        }
        return new OnBackground(color, under);
    }

    /**
     * A foreground-only colour shown on another's background (onBackgroundOf). It still behaves as the colour it
     * was given: bold() and invert() are the given colour's - bold green, its inverse - shown on that background
     * again. Without that, a focused frame drawn in Colors.Gre would lose its bold, and a selected item its inverse.
     */
    private static final class OnBackground extends Color {
        private final Color color;
        private final Color under;

        OnBackground(Color color, Color under) {
            this.color = color;
            this.under = under;
            setBoldTransform((c) -> onBackgroundOf(color.bold(), under));
            setInvertTransform((c) -> onBackgroundOf(color.invert(), under));
        }

        @Override
        protected int[] buildControlCodes() {
            return mergeByParts(color, under).getControlCodes();
        }

        @Override
        public Color getForeGroundColor() {
            return color.getForeGroundColor();
        }

        @Override
        public Color getBackGroundColor() {
            return under.getBackGroundColor();
        }

        @Override
        public boolean hasBackGroundColor() {
            return true;                                  // that is what it is for
        }

        @Override
        public boolean equals(Object other) {             // the same colour on the same background: the same, e.g. for
            return other instanceof OnBackground          // a plotter joining neighbouring cells into one segment
                    && ((OnBackground) other).color.equals(color) && ((OnBackground) other).under.equals(under);
        }

        @Override
        public int hashCode() {
            return 31 * color.hashCode() + under.hashCode();
        }

        @Override
        public String toString() {
            return color + " on " + under;
        }
    }

    // the named colours: merged as they always were
    private static boolean isNamed(Color c) {
        return c == null || c instanceof FgColor || c instanceof BgColor || c instanceof FGBGColors;
    }

    // c1's foreground over c2's background, whatever kind each is (named or RGB); either missing: the other alone
    private static Color mergeByParts(Color c1, Color c2) {
        Color fg = foregroundOf(c1);
        Color bg = backgroundOf(c2);
        if (fg == null || bg == null) {
            return fg != null ? fg : bg != null ? bg : c1;
        }
        if (fg instanceof Colors.RGBColorForeground && bg instanceof Colors.RGBColorBackground) {
            return Colors.RGBColorFGBG.instance((Colors.RGBColorForeground) fg, (Colors.RGBColorBackground) bg);
        }
        return Colors.ColorFromCodes.instance(fg, bg);
    }

    private static Color foregroundOf(Color c) {
        if (c instanceof FgColor || c instanceof Colors.RGBColorForeground) {
            return c;
        }
        if (c instanceof BgColor || c instanceof Colors.RGBColorBackground) {
            return null;
        }
        if (c instanceof FGBGColors) {
            return ((FGBGColors) c).getFgc();
        }
        return part(c.getControlCodes(), false);
    }

    private static Color backgroundOf(Color c) {
        if (c instanceof BgColor || c instanceof Colors.RGBColorBackground) {
            return c;
        }
        if (c instanceof FgColor || c instanceof Colors.RGBColorForeground) {
            return null;
        }
        if (c instanceof FGBGColors) {
            return ((FGBGColors) c).getBgc();
        }
        return part(c.getControlCodes(), true);
    }

    // a colour's codes split in two: its background (40-47, 100-107, 48;2;r;g;b, 48;5;n) or the rest - reset, bold,
    // the foreground. null: none of that part
    private static Color part(int[] codes, boolean background) {
        int[] kept = new int[codes.length];
        int n = 0;
        for (int i = 0; i < codes.length; i++) {
            int code = codes[i];
            int length = 1;
            if (code == 38 || code == 48) {
                length = (i + 1 < codes.length && codes[i + 1] == 5) ? 3 : 5;
            }
            boolean isBackground = code == 48 || (code >= 40 && code <= 47) || (code >= 100 && code <= 107);
            for (int k = 0; k < length && i + k < codes.length; k++) {
                if (isBackground == background) {
                    kept[n++] = codes[i + k];
                }
            }
            i += length - 1;
        }
        return n == 0 ? null : Colors.ColorFromCodes.instance(java.util.Arrays.copyOf(kept, n));
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

    /**
     * Whether sending color sets a background - read from its codes, so any kind of colour answers (named, RGB, parsed
     * from text). false: what is drawn in it shows whatever background is behind it (the canvas fills that in).
     * <pre>
     *     Colors.BGre                        false   (a foreground: it resets, then sets the foreground only)
     *     new FGBGColors(Colors.Whi, On_Blu) true
     *     RGBColorFGBG.instance(..)          true    (48;2;r;g;b)
     * </pre>
     */
    public static boolean setsBackground(Color color) {
        if (color == null) {
            return false;
        }
        int[] codes = color.getControlCodes();
        for (int i = 0; i < codes.length; i++) {
            int code = codes[i];
            if (code == 38 || code == 48) {                  // 38;2;r;g;b / 38;5;n - foreground; 48: background
                if (code == 48) {
                    return true;
                }
                i += (i + 1 < codes.length && codes[i + 1] == 5) ? 2 : 4;
            } else if ((code >= 40 && code <= 47) || (code >= 100 && code <= 107)) {
                return true;
            }
        }
        return false;
    }
    
 // In Color.java
    public Color asDisabled() {
        return Colors.DEFAULT_DISABLED_COLOR;
    }
    
    

}