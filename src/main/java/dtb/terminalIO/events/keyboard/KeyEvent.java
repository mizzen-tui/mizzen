package dtb.terminalIO.events.keyboard;

import dtb.terminalIO.events.TerminalEvent;

/**
 * Encapsulates an event where a key has been pressed ( A {@link KeyStroke} ).
 */
public class KeyEvent extends TerminalEvent<KeyStroke> {

    private static final long serialVersionUID = -6657532777213840024L;

    public KeyEvent(Object source, KeyStroke eventData) {
        super(source, TYPE.KEYPRESS, eventData);
    }

    public boolean isKeyStroke(KeyStroke keyStroke) {
	if (getKeyStroke().equals(keyStroke)) {
	    return true;
	}
        return false;
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