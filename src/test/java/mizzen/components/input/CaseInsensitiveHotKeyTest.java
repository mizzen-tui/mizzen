package mizzen.components.input;

import java.io.UnsupportedEncodingException;

import mizzen.components.CompositeWithFocusManager;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * A case insensitive hot key (KeyStroke.caseInsensitive) presses its button from anywhere on the screen, in either case -
 * the No in a yes / no question. It used to fire only while its button had focus: the lookup compared the typed key's
 * equals, which knows one case only.
 */
public class CaseInsensitiveHotKeyTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private int pressed;

    private Button[] screen() throws UnsupportedEncodingException {
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        Button yes = new Button("Yes");
        Button no = new Button("No");
        no.setHotKey(KeyStroke.caseInsensitive('n'));
        no.onChange((te) -> pressed++);
        root.add(1, 1, yes);
        root.add(1, 10, no);
        bus.setRoot(root);
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(yes)));
        return new Button[] { yes, no };
    }

    public void testLowerCasePressesItFromAnotherButton() throws UnsupportedEncodingException {
        screen();
        bus.deliver(new KeyEvent("test", new KeyStroke('n')));
        assertEquals(1, pressed);
    }

    public void testUpperCaseToo() throws UnsupportedEncodingException {
        screen();
        bus.deliver(new KeyEvent("test", new KeyStroke('N')));
        assertEquals(1, pressed);
    }
}
