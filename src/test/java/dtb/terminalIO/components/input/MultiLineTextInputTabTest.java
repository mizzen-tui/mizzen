package dtb.terminalIO.components.input;

import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.supportfortesting.CanvasForTesting;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * MultiLineTextInput: TAB inserts spaces up to the next tab stop (every TAB_WIDTH columns), and
 * setText turns tabs into spaces the same way - the text never holds a tab char, which would draw
 * as nothing and leave the line one column short.
 */
public class MultiLineTextInputTabTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();   // as TUIApp builds it
    private final MultiLineTextInput text = new MultiLineTextInput();

    @Override
    protected void setUp() throws Exception {
        root.add(text);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        bus.setRoot(root);
        bus.deliver(new RequestFocusEvent(text));
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    public void testTabInsertsSpacesToTheNextStop() {
        text.setText("ab");                         // caret after "ab"
        press(Keys.TAB_KEYSTROKE);
        assertEquals("ab  ", text.getValue());
        press(new KeyStroke('x'));
        press(Keys.TAB_KEYSTROKE);
        assertEquals("ab  x   ", text.getValue());
    }

    public void testSetTextTurnsTabsIntoSpaces() {
        text.setText("a\tb\n\tc");
        assertEquals("a   b\n    c", text.getValue());
    }
}
