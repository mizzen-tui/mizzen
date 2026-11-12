package mizzen.components.input;

import mizzen.controlcodes.color.Color;

public class FloatInputWithIncrDecrButtons extends TextInputWithIncrDecrButtons<Float> {

    /** as the constructor with a colour first, in its theme's colours */
    public FloatInputWithIncrDecrButtons(String labelText, String downButtonText, String upButtonText) {
        this((Color) null, labelText, downButtonText, upButtonText);
    }


    /** the input 5 columns wide */
    public FloatInputWithIncrDecrButtons(Color color, String labelText, String downButtonText, String upButtonText) {
        this(color, labelText, downButtonText, upButtonText, 5);
    }

    /** width: the input's columns */
    public FloatInputWithIncrDecrButtons(Color color, String labelText, String downButtonText, String upButtonText, int width) {
        super(color, labelText, downButtonText, upButtonText, width);
    }

    @Override
    protected TextInputForType<Float> buildTextInputForType(int width) {
        return new TextInputFloat(this.getColor(), width);
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
