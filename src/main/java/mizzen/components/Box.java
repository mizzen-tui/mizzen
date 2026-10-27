package mizzen.components;

import mizzen.controlcodes.color.Colors;

/**
 * A RectangleComponent that renders a Box ( equal length sides)
 *
 */
public class Box extends RectangleComponent {

    public Box(Colors.FGBGColors color, int length) {
        super(color,length, length);
    }
}