package dtb.terminalIO.events;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import dtb.terminalIO.events.Keys.KeyStroke;

public class Keys {

    public static char ESC = 27;

    public static class KeyStroke {
        boolean altPressed = false;
        int     key;
        int     funcKey = 0;

        
        
        public static KeyStroke fromFuncKey(int funcKey) {
            KeyStroke ks = new KeyStroke();
            ks.funcKey = funcKey;
            return ks;
        }
        
        private KeyStroke() {
            
        }
        
        public KeyStroke(int key) {
            super();
            this.key = key;
        }
        
        public KeyStroke(int key, boolean altPressed) {
            this(key);
            this.altPressed = altPressed;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (altPressed ? 1231 : 1237);
            result = prime * result + funcKey;
            result = prime * result + key;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            KeyStroke other = (KeyStroke) obj;
            if (altPressed != other.altPressed)
                return false;
            if (funcKey != other.funcKey)
                return false;
            if (key != other.key)
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "KeyStroke [altPressed=" + altPressed + ", key=" + key + ", funcKey=" + funcKey + "]";
        }
        
    }
    
    public static final KeyStroke F1_KEYSTROKE = KeyStroke.fromFuncKey(1);
    public static final KeyStroke F2_KEYSTROKE = KeyStroke.fromFuncKey(2);
    public static final KeyStroke F3_KEYSTROKE = KeyStroke.fromFuncKey(3);
    public static final KeyStroke F4_KEYSTROKE = KeyStroke.fromFuncKey(4);

    
    public static KeyStroke ESC_KEYSTROKE   = new KeyStroke(ESC);
    public static KeyStroke ENTER_KEYSTROKE = new KeyStroke(13);
    public static KeyStroke TAB_KEYSTROKE   = new KeyStroke('\t');
    
    public static KeyStroke ALT_ESC_KEYSTROKE   = new KeyStroke(ESC, true);
    public static KeyStroke ALT_ENTER_KEYSTROKE = new KeyStroke(13, true);
    public static KeyStroke ALT_TAB_KEYSTROKE   = new KeyStroke('\t', true);

    public static KeyStroke A_KEYSTROKE     = new KeyStroke('A');
    public static KeyStroke B_KEYSTROKE     = new KeyStroke('B');

    public static class KeyEvent extends TerminalEvent<KeyStroke> {

        private static final long serialVersionUID = -6657532777213840024L;

        public KeyEvent(Object source, KeyStroke eventData) {
            super(source, TYPE.KEYPRESS, eventData);
        }

        public boolean isKeyStroke(KeyStroke keyStroke) {
            return this.getEventData().equals(keyStroke);
        }

    }

    // IS_KeyStroke.test(evt,ESC_KEYSTROKE)
    public static BiPredicate<TerminalEvent<?>, KeyStroke> IS_KeyStroke = new BiPredicate<TerminalEvent<?>, KeyStroke>() {

        @Override
        public boolean test(TerminalEvent<?> te, KeyStroke keyStroke) {
            if (!(te instanceof KeyEvent)) {
                return false;
            }
            KeyEvent ke = (KeyEvent) te;
            boolean result = ke.isKeyStroke(keyStroke);
            return result;
        }
    };

    public static class IsKeyStrokeEventPredicate implements Predicate<TerminalEvent<?>> {

        private KeyStroke keyStroke;

        public IsKeyStrokeEventPredicate(KeyStroke keyStroke) {
            this.keyStroke.equals(keyStroke);
        }

        @Override
        public boolean test(TerminalEvent<?> te) {
            return IS_KeyStroke.test(te, keyStroke);
        }
    }

    public static KeyEvent controlChar(final Object inputSource, final char ch, final boolean alt) {

        switch (ch) {
            case 0x0D:
                // Carriage return --> ENTER
                return new KeyEvent(inputSource, new KeyStroke(13, alt));
            case 0x0A:
                // Linefeed --> ENTER
                return new KeyEvent(inputSource, new KeyStroke(13, alt));
            case 0x1B:
                // ESC
                return new KeyEvent(inputSource, new KeyStroke(ESC, alt));
            case '\t':
                // TAB
                return new KeyEvent(inputSource, new KeyStroke('\t',alt));
            default:
                // Make all other control characters come back as the alphabetic
                // character with the ctrl field set. So SOH would be 'A' +
                // ctrl.
                return new KeyEvent(inputSource, new KeyStroke(ch + 0x40, alt));

        }
    }

    public static KeyEvent normalKeyEvent(final Object inputSource, char ch) {
        return new KeyEvent(inputSource, new KeyStroke(ch));
    }

}
