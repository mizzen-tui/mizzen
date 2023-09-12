package dtb.terminalIO.components.input;

import dtb.terminalIO.controlcodes.color.Color;

public class IntegerInputWithIncrDecrButtons extends TextInputWithIncrDecrButtons<Integer> {
    
    public IntegerInputWithIncrDecrButtons(Color color, String labelText, String downButtonText, String upButtonText) {
        super(color, labelText, downButtonText, upButtonText);
    }

    @Override
    protected TextInputForType<Integer> buildTextInputForType() {
        return new TextInputInteger(this.getColor(),3);
    }

    @Override
    protected Integer decr() {
        if (getValue() == null) {
            return -1;
        }
        return getValue() - 1;
    }

    @Override
    protected Integer incr() {
        if (getValue() == null) {
            return 1;
        }
        return getValue() + 1;
    }

}
