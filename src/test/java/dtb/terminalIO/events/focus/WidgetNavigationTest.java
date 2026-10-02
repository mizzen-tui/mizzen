package dtb.terminalIO.events.focus;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusOwnerTest.Leaf;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * A WIDGET's own nav keys (e.g. UP / DOWN in a dropdown) go through the root's keyboard navigation,
 * in this order: the focused component, then the widget's own handlers, then moving focus.
 * Layout: root[A, widget[B, C]] - the widget moves between B and C with UP / DOWN.
 */
public class WidgetNavigationTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final List<String> log = new ArrayList<>();
    private final Leaf a = new Leaf("A"), b = new Leaf("B"), c = new Leaf("C");
    private DefaultFocusManager root, widget;

    @Override
    protected void setUp() {
        widget = new DefaultFocusManager(Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE).setCycling(false).setFocusPolicy(FocusPolicy.WIDGET).setName("widget");
        widget.addFocusable(b);
        widget.addFocusable(c);
        root = new DefaultFocusManager().setName("root");
        root.addFocusable(a);
        root.addFocusable(widget);
        bus.setRoot(root);
    }

    /** a handler that takes key and logs "name" */
    private ConditionalEventHandler logging(String name, KeyStroke key) {
        return new ConditionalEventHandler() {
            public EventHandler canHandle(TerminalEvent<?> te) {
                return te instanceof KeyEvent && ((KeyEvent) te).isKeyStroke(key) ? this : null;
            }
            public void handle(TerminalEvent<?> te) { log.add(name); }
        };
    }

    private void click(Focusable target) { bus.deliver(new RequestFocusEvent(target)); }

    private void press(KeyStroke key) { bus.deliver(new KeyEvent("test", key)); }

    private Focusable focused() { return FocusManager.UTIL.findCurrentFocus(root); }

    public void testNavKeyMovesFocusInsideWidget() {
        click(b);

        press(Keys.DOWN_KEYSTROKE);

        assertSame(c, focused());
    }

    /** the widget's own handler wants the key: it gets it, and focus does not move (e.g. "DOWN opens my list") */
    public void testOwnHandlerComesBeforeMovingFocus() {
        widget.setCustomExtendedEventHandler(logging("own", Keys.DOWN_KEYSTROKE));
        click(b);

        press(Keys.DOWN_KEYSTROKE);

        assertEquals("[own]", log.toString());
        assertSame(b, focused());
    }

    /** at the widget's last child the key is used up: focus stays, and nothing further out gets it */
    public void testAtTheEndFocusStays() {
        root.setCustomExtendedEventHandler(logging("outside", Keys.DOWN_KEYSTROKE));
        click(c);

        press(Keys.DOWN_KEYSTROKE);

        assertSame(c, focused());
        assertEquals("[]", log.toString());
    }

    /** a widget set to cycle (e.g. a tab strip) wraps from its last child to its first */
    public void testWidgetSetToCycleWrapsAround() {
        widget.setCycling(true);
        click(c);

        press(Keys.DOWN_KEYSTROKE);

        assertSame(b, focused());
    }

    /** TAB still leaves the widget as one stop: from B (or C) to the next stop outside, A (root wraps) */
    public void testTabLeavesTheWidget() {
        click(b);

        press(Keys.TAB_KEYSTROKE);

        assertSame(a, focused());
    }
}
