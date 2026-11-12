package mizzen.components.input;

import mizzen.controlcodes.color.Color;

/**
 * A whole number with - and + buttons. The input is 3 columns unless a width is given:
 * <pre>
 *     new IntegerInputWithIncrDecrButtons(color, "UID", "-", "+", 6)     // room for 150000
 * </pre>
 */
public class IntegerInputWithIncrDecrButtons extends TextInputWithIncrDecrButtons<Integer> {

    public IntegerInputWithIncrDecrButtons(Color color, String labelText, String downButtonText, String upButtonText) {
        this(color, labelText, downButtonText, upButtonText, 3);
    }

    /** width: the input's columns */
    public IntegerInputWithIncrDecrButtons(Color color, String labelText, String downButtonText, String upButtonText, int width) {
        super(color, labelText, downButtonText, upButtonText, width);
    }

    /** in its theme's colours */
    public IntegerInputWithIncrDecrButtons(String labelText, String downButtonText, String upButtonText) {
        this(null, labelText, downButtonText, upButtonText);
    }

    @Override
    protected TextInputForType<Integer> buildTextInputForType(int width) {
        return new TextInputInteger(this.getOwnColor(), width);       // none given: it stays themed too
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
