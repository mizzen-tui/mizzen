package mizzen.components.input;

import mizzen.controlcodes.color.Color;

public class FloatInputWithIncrDecrButtons extends TextInputWithIncrDecrButtons<Float> {

    public FloatInputWithIncrDecrButtons(Color color, String labelText, String downButtonText, String upButtonText) {
        super(color, labelText, downButtonText, upButtonText);
    }

    @Override
    protected TextInputForType<Float> buildTextInputForType() {
        return new TextInputFloat(this.getColor(),5);
    }

    @Override
    protected Float decr() {
        if (getValue() == null) {
            return -1f;
        }
        return getValue() - 1f;
    }

    @Override
    protected Float incr() {
        if (getValue() == null) {
            return 1f;
        }
        return getValue() + 1f;
    }

}
