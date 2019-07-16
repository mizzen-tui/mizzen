package dtb.terminalIO.controlcodes;

import dtb.terminalIO.controlcodes.ControlCode.Color;

/**
 * Transform a Color from one color to another color
 */
@FunctionalInterface
public interface ColorTransform extends RendererTransform<Color, Color> {
    
    public static ColorTransform NULL_colorTransform = (color) -> color;   
}
