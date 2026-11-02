package mizzen.components.input;

import mizzen.controlcodes.color.Color;

public class IntegerInputWithIncrDecrButtons extends TextInputWithIncrDecrButtons<Integer> {
    
    public IntegerInputWithIncrDecrButtons(Color color, String labelText, String downButtonText, String upButtonText) {
        super(color, labelText, downButtonText, upButtonText);
    }

    /** in its theme's colours */
    public IntegerInputWithIncrDecrButtons(String labelText, String downButtonText, String upButtonText) {
        this(null, labelText, downButtonText, upButtonText);
    }

    @Override
    protected TextInputForType<Integer> buildTextInputForType() {
        return new TextInputInteger(this.getOwnColor(), 3);       // none given: it stays themed too
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
