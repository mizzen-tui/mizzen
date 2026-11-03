package mizzen.events.keyboard;

import mizzen.events.TerminalEvent;

/**
 * Encapsulates an event where a key has been pressed ( A {@link KeyStroke} ).
 */
public class KeyEvent extends TerminalEvent<KeyStroke> {

    private static final long serialVersionUID = -6657532777213840024L;

    public KeyEvent(Object source, KeyStroke eventData) {
        super(source, TYPE.KEYPRESS, eventData);
    }

    public boolean isKeyStroke(KeyStroke keyStroke) {
        // either side's equals: a case insensitive key (KeyStroke.caseInsensitive) knows N and n, a typed key doesn't
        return keyStroke != null && (keyStroke.equals(getKeyStroke()) || getKeyStroke().equals(keyStroke));
    }

    public String getKeyName() {
        return getEventData().name;
    }
    
    public KeyStroke getKeyStroke() {
        return this.getEventData();
    }
    
    public char getChar() {
        return getKeyStroke().getChar();
    }

}