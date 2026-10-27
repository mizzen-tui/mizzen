package mizzen.events.keyboard;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import junit.framework.TestCase;

/** AsKeyboardHandler.UTIL.toKeyboardHandler: any object as a KeyboardHandler, digging inside if needed */
public class AsKeyboardHandlerTest extends TestCase {

    private static KeyEvent key(char c) {
        return new KeyEvent("test", new KeyStroke(c));
    }

    /** an event handler: takes the keys its canHandle accepts, and can tell without acting */
    public void testEventHandler() {
        int[] pressed = { 0 };
        KeyboardHandler handler = AsKeyboardHandler.UTIL.toKeyboardHandler(
                new KeyInputEventHandler((te) -> pressed[0]++, new KeyStroke('a')));

        assertTrue(handler.wantsKey(key('a')));
        assertFalse(handler.wantsKey(key('b')));
        assertEquals("asking doesn't act", 0, pressed[0]);

        assertTrue(handler.onKey(key('a')));
        assertFalse(handler.onKey(key('b')));
        assertEquals(1, pressed[0]);
    }

    /** a focusable component: its Focusable (an event handler) - a text field wants a letter */
    public void testFocusableComponent() {
        KeyboardHandler handler = AsKeyboardHandler.UTIL.toKeyboardHandler(new TextInput(Colors.IYel, 10));

        assertNotNull(handler);
        assertTrue(handler.wantsKey(key('a')));
    }

    /** a focus manager passes events on to its children: refused */
    public void testFocusManagerIsRefused() {
        try {
            AsKeyboardHandler.UTIL.toKeyboardHandler(new CompositeWithFocusManager());
            fail("a focus manager must be refused");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }

    /** anything else: none */
    public void testOtherObject() {
        assertNull(AsKeyboardHandler.UTIL.toKeyboardHandler("not a handler"));
    }
}
