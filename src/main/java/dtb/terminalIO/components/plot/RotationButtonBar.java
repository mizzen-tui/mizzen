package dtb.terminalIO.components.plot;

import dtb.terminalIO.components.input.IntegerInputWithIncrDecrButtons;
import dtb.terminalIO.controlcodes.color.Color;

public class RotationButtonBar extends IntegerInputWithIncrDecrButtons {

    public RotationButtonBar(Color color) {
        super(color, "Rotatation (degrees)", "<-", "->");
        this.setValue(0);
    }
    
}
