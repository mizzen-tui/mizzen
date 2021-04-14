package dtb.terminalIO.controlcodes.color;

import java.util.Comparator;
import java.util.StringJoiner;

import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCodeContext;
import dtb.terminalIO.util.IntArrayComparator;

/**
 * A {@link ControlCode} that encapsulates a Color
 */
public abstract class Color extends ControlCode implements ColorProvider {

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
        return this; // TODO:  fix!
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

}