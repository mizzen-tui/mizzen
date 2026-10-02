package dtb.terminalIO.components;

import dtb.terminalIO.components.input.RockerButton;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Hot keys reach their component from anywhere on screen - as long as the focused component (or
 * a focus manager around it) does not take the key first. Layout as in LineChartTest:
 * root[bar[rocker 1, rocker 2, rocker 3]], each rocker an on/off switch with hot key '1' / '2' / '3'.
 * Keys are typed as NEW KeyStroke objects, the way the keyboard input produces them.
 */
public class HotKeyCharacterisationTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final RockerButton[] rockers = new RockerButton[3];

    @Override
    protected void setUp() {
        ButtonBar bar = new ButtonBar(Colors.IYel);
        for (int i = 0; i < rockers.length; i++) {
            rockers[i] = new RockerButton(Colors.IYel, "series" + (i + 1), true);
            rockers[i].setHotKey(new KeyStroke((char) ('1' + i)));
            bar.addComponent(rockers[i]);
        }
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(bar);
        bus.setRoot(root);
        // focus on rocker 1 (its first part)
        bus.deliver(new RequestFocusEvent(FocusManager.UTIL.getFocusablesFromComponent(rockers[0]).get(0)));
    }

    private void type(char c) {
        bus.deliver(new KeyEvent("test", new KeyStroke(c)));
    }

    private String values() {
        StringBuilder s = new StringBuilder();
        for (RockerButton r : rockers) {
            s.append(r.getValue() ? "ON " : "OFF ");
        }
        return s.toString().trim();
    }

    /** the focused rocker's own hot key toggles it */
    public void testOwnHotKey() {
        type('1');

        assertEquals("OFF ON ON", values());
    }

    /** another rocker's hot key, typed while rocker 1 has focus, toggles that other rocker */
    public void testOtherRockersHotKey() {
        type('3');

        assertEquals("ON ON OFF", values());
    }
}
