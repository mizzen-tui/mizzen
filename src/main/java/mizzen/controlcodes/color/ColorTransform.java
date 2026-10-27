package mizzen.controlcodes.color;

import mizzen.controlcodes.RendererTransform;

/**
 * Transform a Color from one color to another color
 */
@FunctionalInterface
public interface ColorTransform extends RendererTransform<Color, Color> {
    
    public static ColorTransform NULL_colorTransform = (color) -> color;   
}
