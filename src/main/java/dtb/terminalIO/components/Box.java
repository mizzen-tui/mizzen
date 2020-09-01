package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;

/**
 * A RectangleComponent that renders a Box ( equal length sides)
 *
 */
public class Box extends RectangleComponent {

    public Box(FGBGColors color, int length) {
        super(color,length, length);
    }
}