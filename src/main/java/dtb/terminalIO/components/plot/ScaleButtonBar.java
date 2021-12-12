package dtb.terminalIO.components.plot;

import dtb.terminalIO.components.input.IntegerInputWithIncrDecrButtons;
import dtb.terminalIO.controlcodes.color.Color;

public class ScaleButtonBar extends IntegerInputWithIncrDecrButtons {

    public ScaleButtonBar(Color color) {
        super(color, "Scale (%)", "-", "+");
        this.setValue(100);
    }
    
}
