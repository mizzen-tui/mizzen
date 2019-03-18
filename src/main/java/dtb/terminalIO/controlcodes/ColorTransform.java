package dtb.terminalIO.controlcodes;

import dtb.terminalIO.controlcodes.ControlCode.Color;

@FunctionalInterface
public interface ColorTransform extends RendererTransform<Color, Color> {
    
    public static ColorTransform NULL_colorTransform = (color) -> color;   
}
