package dtb.terminalIO.controlcodes.color;

import dtb.terminalIO.controlcodes.RendererTransform;

/**
 * Transform a Color from one color to another color
 */
@FunctionalInterface
public interface ColorTransform extends RendererTransform<Color, Color> {
    
    public static ColorTransform NULL_colorTransform = (color) -> color;   
}
