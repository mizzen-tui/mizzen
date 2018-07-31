package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode.FGBGColors;

public class Box extends RectangleComponent {

    public Box(FGBGColors color, int length) {
        super(color,length, length);
    }
}