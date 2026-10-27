package mizzen.components.plot;

import mizzen.components.input.IntegerInputWithIncrDecrButtons;
import mizzen.controlcodes.color.Color;

public class RotationButtonBar extends IntegerInputWithIncrDecrButtons {

    public RotationButtonBar(Color color) {
        super(color, "Rotatation (degrees)", "<-", "->");
        this.setValue(0);
    }
    
}
