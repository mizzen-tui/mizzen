package dtb.terminalIO.components.input;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;

public class TextInputFloat extends TextInputForType<Float> {

    public TextInputFloat(Color color, int length) {
        super(color, length, 
             (t)-> validateText(t),
             (t)-> toDesiredType(t));
    }

    public TextInputFloat(int length) {
          this(Colors.DEFAULT_COLOR, length);
    }

    static Float toDesiredType(String text) {
        return Float.parseFloat(text);
    }

    static boolean validateText(String newText) {
        try {
            if (newText == null || newText.trim().length()==0) {
                return true;
            }
            toDesiredType(newText);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
