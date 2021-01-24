package dtb.terminalIO.components.input;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;

public class TextInputInteger  extends TextInputForType<Integer> {

    
    public TextInputInteger(Color color, int length) {
        super(color, length, 
              (t)-> validateText(t),
              (t)-> toDesiredType(t));
        this.setText("0");
    }

    public TextInputInteger(int length) {
          this(Colors.DEFAULT_COLOR, length);
    }
    
    static Integer toDesiredType(String text) {
        if (text.trim().equals("-")) {
            return 0;
        }
        return Integer.parseInt(text);
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
