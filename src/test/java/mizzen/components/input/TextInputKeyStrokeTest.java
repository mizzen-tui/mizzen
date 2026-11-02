package mizzen.components.input;

import java.io.UnsupportedEncodingException;

import mizzen.components.CompositeWithFocusManager;
import mizzen.controlcodes.color.Colors;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventHandler;
import mizzen.events.Registration;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * A listener on a key in a text field: it hears the key once the field has done its own work with it,
 * and a key the field has no use for becomes the field's - it no longer reaches the root.
 */
public class TextInputKeyStrokeTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final TextInput field = new TextInput(Colors.IYel, 10);
    private final StringBuilder heard = new StringBuilder();

    @Override
    protected void setUp() throws UnsupportedEncodingException {
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        root.add(1, 1, field);
        KeyInputEventHandler unused = new KeyInputEventHandler(Keys.F5_KEYSTROKE, (te) -> heard.append("root "));
        bus.setRoot(new ConditionalEventHandler() {                   // the screen first; F5 only if nothing on it took it
            public EventHandler canHandle(TerminalEvent<?> te) {
                return root.canHandle(te) != null ? root : unused.canHandle(te);
            }

            public void handle(TerminalEvent<?> te) {
                canHandle(te).handle(te);
            }
        });
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(field)));
        bus.drain();
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
        bus.drain();
    }

    public void testEnterStillChangesThenTheListenerHears() {
        field.onChange((te) -> heard.append("change "));
        field.onEnter((te) -> heard.append("enter "));
        press(Keys.ENTER_KEYSTROKE);
        assertEquals("change enter ", heard.toString());
    }

    public void testATypedKeyIsStillTyped() {
        field.onKeyStroke('?', (te) -> heard.append("help "));
        press(new KeyStroke('?'));
        assertEquals("?", field.getText());
        assertEquals("help ", heard.toString());
    }

    public void testAKeyTheFieldDoesNotUseBecomesItsOwn() {
        press(Keys.F5_KEYSTROKE);
        assertEquals("not heard: the root has it", "root ", heard.toString());
        heard.setLength(0);
        Registration listening = field.onKeyStroke(Keys.F5_KEYSTROKE, (te) -> heard.append("field "));
        press(Keys.F5_KEYSTROKE);
        assertEquals("field ", heard.toString());
        heard.setLength(0);
        listening.unregister();
        press(Keys.F5_KEYSTROKE);
        assertEquals("root ", heard.toString());
    }
}
