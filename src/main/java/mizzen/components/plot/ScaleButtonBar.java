package mizzen.components.plot;

import mizzen.components.input.IntegerInputWithIncrDecrButtons;
import mizzen.controlcodes.color.Color;

public class ScaleButtonBar extends IntegerInputWithIncrDecrButtons {

    public ScaleButtonBar(Color color) {
        super(color, "Scale (%)", "-", "+");
        this.setValue(100);
    }
    
}
