package mizzen.components.input;

import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;

public class TextInputInteger  extends TextInputForType<Integer> {

    
    public TextInputInteger(Color color, int length) {
        super(color, length, 
              (s)-> validateText(s),
              (s)-> toDesiredType(s),
              (t)-> typeToString(t));
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

    static String typeToString(Integer number) {
        return String.valueOf(number);
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
