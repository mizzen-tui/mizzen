package mizzen.components.input;

import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;

/**
 * The two keys that move focus between a component's children: back to the previous one, on to the
 * next. Example: a radio group's options, UP_DOWN; a tab strip, LEFT_RIGHT.
 */
public final class ArrowKeys {

    public static final ArrowKeys UP_DOWN    = new ArrowKeys(Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE);
    public static final ArrowKeys LEFT_RIGHT = new ArrowKeys(Keys.LEFT_KEYSTROKE, Keys.RIGHT_KEYSTROKE);

    private final KeyStroke previous;
    private final KeyStroke next;

    private ArrowKeys(KeyStroke previous, KeyStroke next) {
        this.previous = previous;
        this.next = next;
    }

    /** any other pair of keys */
    public static ArrowKeys of(KeyStroke previous, KeyStroke next) {
        return new ArrowKeys(previous, next);
    }

    public KeyStroke previous() {
        return previous;
    }

    public KeyStroke next() {
        return next;
    }
}
