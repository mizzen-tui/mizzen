package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ColorTransform;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.ColorProvider;

/**
 * A hilite action that takes a color and transforms it to
 * its hilited equivalent which usually means making it bold
 */
public class ColorHiliter implements Hiliter, ColorProvider {
    
    private boolean hilited = false;
    
    private ControlCode.ColorProvider colorToHilite;
    private ColorTransform colorHilitedTransform = (color)->color.bold();

    public ColorHiliter(ControlCode.ColorProvider colorProvider) {
        this.colorToHilite = colorProvider;
    }
    
    protected Color getColorToHilite() {
        return colorToHilite.getColor();
    }
    
    public Color getColor() {
        if (hilited) {
           return colorHilitedTransform.transform(getColorToHilite().getColor());
        }
        return getColorToHilite().getColor();
    }
    
    @Override
    public void hilite() {
        this.hilited = true;
    }

    @Override
    public void unHilite() {
        if (getColorToHilite() instanceof Hiliter) {
            ((Hiliter) getColorToHilite()).unHilite();;
       }
        this.hilited = false;
    }

    public Color asColor() {
        return getColor();
    }

}
