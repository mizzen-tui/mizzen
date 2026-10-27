package mizzen.components;

import mizzen.components.input.RadioGroup;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Hot keys reach their component from anywhere on screen - as long as the focused component (or
 * a focus manager around it) does not take the key first. Layout as in LineChartTest:
 * root[bar[toggle 1, toggle 2, toggle 3]], each an on/off RadioGroup with hot key '1' / '2' / '3'.
 * Keys are typed as NEW KeyStroke objects, the way the keyboard input produces them.
 */
public class HotKeyCharacterisationTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    @SuppressWarnings("unchecked")
    private final RadioGroup<Boolean>[] toggles = new RadioGroup[3];

    @Override
    protected void setUp() {
        ButtonBar bar = new ButtonBar(Colors.IYel);
        for (int i = 0; i < toggles.length; i++) {
            toggles[i] = RadioGroup.onOff("series" + (i + 1), true).setColor(Colors.IYel);
            toggles[i].setHotKey(new KeyStroke((char) ('1' + i)));
            bar.addComponent(toggles[i]);
        }
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(bar);
        bus.setRoot(root);
        // focus on toggle 1
        bus.deliver(new RequestFocusEvent(toggles[0].asFocusable()));
    }

    private void type(char c) {
        bus.deliver(new KeyEvent("test", new KeyStroke(c)));
    }

    private String values() {
        StringBuilder s = new StringBuilder();
        for (RadioGroup<Boolean> r : toggles) {
            s.append(r.getValue() ? "ON " : "OFF ");
        }
        return s.toString().trim();
    }

    /** the focused toggle's own hot key toggles it */
    public void testOwnHotKey() {
        type('1');

        assertEquals("OFF ON ON", values());
    }

    /** another toggle's hot key, typed while toggle 1 has focus, toggles that other one */
    public void testOtherRockersHotKey() {
        type('3');

        assertEquals("ON ON OFF", values());
    }
}
