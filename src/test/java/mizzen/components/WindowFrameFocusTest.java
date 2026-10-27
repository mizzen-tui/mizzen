package mizzen.components;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.composite.Composite;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * A window whose content is wrapped (ClickableFocusableWrapper, as WindowFrameTest does) and holds a
 * button bar with hot keys: a hot key pressed with focus elsewhere moves focus into the window to its
 * button. It threw "Cannot set as focusable (Buttonbar) that has not been first added" when the
 * wrapper hid the bar behind a focus manager of its own.
 * Layout: root[outside, window[wrapper[panel[bar[increase(UP), decrease(DOWN)]]]]].
 */
public class WindowFrameFocusTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();
    private final TextInput outside = new TextInput(Colors.IYel, 10);
    private final List<String> pressed = new ArrayList<String>();
    private WindowFrame window;

    @Override
    protected void setUp() {
        outside.setName("outside");
        ButtonBar bar = new ButtonBar(Colors.IGre);
        bar.addButton("increase", Keys.UP_KEYSTROKE, (te) -> pressed.add("increase")).setName("increase");
        bar.addButton("decrease", Keys.DOWN_KEYSTROKE, (te) -> pressed.add("decrease")).setName("decrease");
        Composite panel = new Composite().setBorder(false);
        panel.add(new TextComponent("Sparkline demo"));
        panel.add(1, 0, bar);
        window = new WindowFrame(Colors.Blu, "SparkLine Demo", ClickableFocusableWrapper.wrappedIfNeeded(panel));
        root.add(outside);
        root.add(3, 0, window);
        bus.setRoot(root);
        bus.deliver(RequestFocusEvent.fromSource(outside));
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    private String focusedName() {
        Focusable focused = FocusManager.UTIL.findCurrentFocus(root);
        return focused == null ? "-" : HasName.UTIL.getName(focused.getFocusComponent());
    }

    public void testHotKeyFromOutsideMovesFocusIntoTheWindow() {
        press(Keys.DOWN_KEYSTROKE);

        assertEquals("decrease", focusedName());
        assertEquals("[decrease]", pressed.toString());
        assertTrue("the window shows it has focus", window.asFocusable().hasFocus());
    }

    public void testTabGoesIntoTheWindowContentAndOut() {
        press(Keys.TAB_KEYSTROKE);
        assertEquals("increase", focusedName());
        press(Keys.TAB_KEYSTROKE);
        assertEquals("decrease", focusedName());
        press(Keys.TAB_KEYSTROKE);
        assertEquals("outside", focusedName());
    }
}
