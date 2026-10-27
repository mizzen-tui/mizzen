package mizzen.events.keyboard;

import java.util.Arrays;

/**
 * Encapsulates a key that has been pressed.
 * 
 * <p/>
 * 
 * Usually used in conjunction with a {@link KeyEvent}
 * 
 * @author david
 *
 */
public class KeyStroke {
    boolean altPressed = false;
    private boolean ctrl = false;
    protected KeySequence keys[];
    private int funcKey = 0;
    String name;

    public static KeyStroke fromFuncKey(int funcKey) {
        KeyStroke ks = new KeyStroke();
        ks.funcKey = funcKey;
        ks.name = "F" + funcKey;
        return ks;
    }

    private KeyStroke() {
    }

    public KeyStroke(int[][] keys, String name) {
        this.keys = new KeySequence[keys.length];
        for (int i = 0; i < keys.length; i++) {
            this.keys[i] = new KeySequence(keys[i]);
        }
        this.name = name;
    }

    public KeyStroke(int[] keys, String name) {
        this(new int[][] { keys }, name);
    }

    protected KeyStroke(int[] keys) {
        this(keys, "???");
    }

    public KeyStroke(int key, String name) {
        this(new int[] { key }, name);
    }

    public KeyStroke(char ch) {
        this(new int[] { ch }, ch + "");
    }

    public static KeyStroke caseInsensitive(char ch) {
        Character carUpper = Character.toUpperCase(ch);
        Character carLower = Character.toLowerCase(ch);

        return new KeyStroke(new int[][] {{ carUpper.charValue() },{ carLower.charValue() }}, carUpper.toString() ) {
            protected boolean keySequencesEqual(KeyStroke other) {
                if (other.keys==null) return false;
                if (other.keys == this.keys) return true;
                for (int i=0; i<this.keys.length; i++) {
                    for (int j=0; j<other.keys.length; j++) {
                        if (other.keys[j].equals(this.keys[i])) {
                            return true;
                        }
                    }
                }
                return false;
            }

            protected boolean namesEqual(KeyStroke other) {
                if (name == null) {
                    if (other.name != null)
                        return false;
                } 
                if (carUpper.toString().equals(other.name)) return true;
                if (carLower.toString().equals(other.name)) return true;
                return false;
            }
        };

    }

    public KeyStroke(int key) {
        this(key, ((char) key) + "");
    }

    public static KeyStroke ALTKey(char ch) {
        return new KeyStroke(ch, "ALT-" + ch, true);
    }

    public KeyStroke(int key, String name, boolean altPressed) {
        this(key, name);
        this.altPressed = altPressed;
    }

    public boolean isCtrlKey() {
        return this.ctrl;
    }

    public boolean isAltKey() {
        return this.altPressed;
    }

    public static KeyStroke CTRL_keyStroke(int i) {
        if (i > 0x1F) {
            throw new IllegalArgumentException("CTRL_keyStroke(i) - i should have value <= 0x1F, i = " + i);
        }
        boolean alt = false;
        switch (i) {
        case 0x0D:
            // Carriage return --> ENTER
            return new KeyStroke(13, "ENTER", alt);
        case 0x0A:
            // Linefeed --> ENTER
            return new KeyStroke(13, "ENTER", alt);
        case 0x1B:
            // ESC
            return new KeyStroke(27, "ESC", alt);
        case '\t':
            // TAB
            return new KeyStroke('\t', "TAB", alt);
        default:
            // Make all other control characters come back as the alphabetic
            // character with the ctrl field set. So SOH would be 'A' +
            // ctrl.
            KeyStroke keyStroke = new KeyStroke(i, "CTRL-" + ((char) ('A' + i - 1)), alt);
            keyStroke.ctrl = true;
            return keyStroke;
        }
    }

    public int getKeySequenceLength() {
        return this.getKeys(0).getLength();
    }

    public char getChar() {
        return (char) this.keys[0].getKeys()[0];
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public KeySequence getKeys(int index) {
        return keys[index];
    }

    public KeySequence[] getKeys() {
        return keys;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (altPressed ? 1231 : 1237);
        result = prime * result + (ctrl ? 1231 : 1237);
        result = prime * result + funcKey;
        result = prime * result + Arrays.hashCode(keys);
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    protected boolean keySequencesEqual(KeyStroke other) {
        if (!Arrays.equals(keys, other.keys))
            return false; 
        return true;
    }

    protected boolean namesEqual(KeyStroke other) {
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        KeyStroke other = (KeyStroke) obj;
        if (altPressed != other.altPressed)
            return false;
        if (ctrl != other.ctrl)
            return false;
        if (funcKey != other.funcKey)
            return false;
        
        if (!this.keySequencesEqual(other)) {
            if (!other.keySequencesEqual(this)) {
                return false;
            }
        }
        return this.namesEqual(other) || other.namesEqual(this);
    }

    @Override
    public String toString() {
        return "KeyStroke [name =" + name + ", altPressed=" + altPressed + ", ctrl=" + ctrl + ", keys="
                + Arrays.toString(keys) + ", funcKey=" + funcKey + "]";
    }

}