package dtb.terminalIO.events.focus;

import java.util.StringJoiner;

import dtb.terminalIO.components.input.ArrowKeys;
import dtb.terminalIO.supportfortesting.FocusGroup;
import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Tests for the NEW navigation protocol (enter / advance / FocusPolicy, docs/ARCHITECTURE.md §4.3)
 * on real components. The protocol is not yet used by the focus routing: here a small helper plays
 * the future router - it asks the focus managers where focus should go (navDirectionFor / advance) and
 * moves focus with today's mechanism (a REQUEST_FOCUS event), so remembered children stay as today.
 *
 * Expected sequences are the CORRECT ones (compare FocusCharacterisationTest, which records
 * today's buggy order for the same layouts).
 */
public class FocusNavigationProtocolTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();

    private static TextInput field(String name) {
        TextInput field = new TextInput(Colors.IYel, 10);
        field.setName(name);
        return field;
    }

    private static Focusable focusable(Object component) {
        return ToFocusable.UTIL.toFocusable(component);
    }

    private static FocusGroup group(TextInput... fields) {
        VerticalPanel panel = new VerticalPanel();
        for (TextInput f : fields) panel.add(f);
        return new FocusGroup(panel);
    }

    /** a ONE_STOP manager: one stop, UP / DOWN move inside (like DropdownListBox) */
    private static FocusGroup oneStop(TextInput... fields) {
        FocusGroup w = group(fields);
        w.focus().singleTabStop()
                 .arrowsMoveInside(ArrowKeys.UP_DOWN);
        return w;
    }

    private CompositeWithFocusManager root(Object... children) {
        CompositeWithFocusManager root = new CompositeWithFocusManager();   // as TUIApp builds it
        for (Object c : children) root.add((dtb.terminalIO.components.Component<?>) c);
        bus.setRoot(root);
        return root;
    }

    private void click(Object component) {
        bus.deliver(new RequestFocusEvent(focusable(component)));
    }

    /** the future router: offer the nav key to each focus manager from the focused leaf upwards */
    private void navigate(CompositeWithFocusManager root, KeyStroke key) {
        Focusable leaf = FocusManager.UTIL.findCurrentFocus(root);
        for (FocusManager fm = FocusManager.UTIL.parentOf(leaf); fm != null; fm = FocusManager.UTIL.parentOf(fm)) {
            FocusDirection dir = fm.navDirectionFor(key);
            if (dir == null) {
                continue;
            }
            Navigation nav = fm.advance(leaf, dir);
            if (nav.getKind() == Navigation.Kind.MOVED) {
                bus.deliver(new RequestFocusEvent(nav.getTarget()));
                return;
            }
            if (nav.getKind() == Navigation.Kind.STAY) {
                return;
            }
        }
    }

    private static String focused(TextInput... fields) {
        StringJoiner names = new StringJoiner("+");
        for (TextInput f : fields) if (focusable(f).hasFocus()) names.add(f.getName());
        return names.length() == 0 ? "-" : names.toString();
    }

    private String keys(CompositeWithFocusManager root, TextInput[] fields, KeyStroke... keys) {
        StringJoiner seq = new StringJoiner(" ");
        for (KeyStroke k : keys) {
            navigate(root, k);
            seq.add(focused(fields));
        }
        return seq.toString();
    }

    private static KeyStroke[] times(KeyStroke key, int n) {
        KeyStroke[] keys = new KeyStroke[n];
        java.util.Arrays.fill(keys, key);
        return keys;
    }

    // ---------------------------------------------------------------- GROUP

    public void testGroup_tabVisitsEveryFieldEveryLap() {
        TextInput a = field("A"), b = field("B"), c = field("C"), d = field("D");
        CompositeWithFocusManager root = root(a, group(b, c), d);
        click(a);

        assertEquals("B C D A B C D A B", keys(root, new TextInput[] { a, b, c, d }, times(Keys.TAB_KEYSTROKE, 9)));
    }

    public void testGroup_shiftTabEntersGroupAtItsLastField() {
        TextInput a = field("A"), b = field("B"), c = field("C"), d = field("D");
        CompositeWithFocusManager root = root(a, group(b, c), d);
        click(a);

        assertEquals("D C B A D C B A", keys(root, new TextInput[] { a, b, c, d }, times(Keys.SHIFT_TAB_KEYSTROKE, 8)));
    }

    /** a group as the root's only child must not dead-end (today it gets stuck) */
    public void testSingleGroupUnderRoot_noDeadEnd() {
        TextInput a = field("A"), b = field("B"), c = field("C"), d = field("D");
        CompositeWithFocusManager root = root(group(a, b, c, d));
        click(a);

        assertEquals("B C D A B", keys(root, new TextInput[] { a, b, c, d }, times(Keys.TAB_KEYSTROKE, 5)));
    }

    public void testDisabledFieldIsSkipped() {
        TextInput a = field("A"), b = field("B"), c = field("C"), d = field("D");
        c.setEnabled(false, bus);
        CompositeWithFocusManager root = root(a, group(b, c), d);
        click(a);

        assertEquals("B D A B", keys(root, new TextInput[] { a, b, c, d }, times(Keys.TAB_KEYSTROKE, 4)));
    }

    // ---------------------------------------------------------------- ONE_STOP

    /** a one-stop component is one stop; its own keys move inside; TAB re-enters at the remembered child */
    public void testOneStop_remembersChild() {
        TextInput a = field("A"), b = field("B"), c = field("C"), d = field("D");
        CompositeWithFocusManager root = root(a, oneStop(b, c), d);
        TextInput[] all = { a, b, c, d };
        click(a);

        assertEquals("B D A B", keys(root, all, times(Keys.TAB_KEYSTROKE, 4)));
        assertEquals("C", keys(root, all, Keys.DOWN_KEYSTROKE));          // move inside the one-stop component
        assertEquals("C", keys(root, all, Keys.DOWN_KEYSTROKE));          // at the end: stays
        assertEquals("D A C", keys(root, all, times(Keys.TAB_KEYSTROKE, 3)));   // re-enters at C
        assertEquals("A C", keys(root, all, Keys.SHIFT_TAB_KEYSTROKE, Keys.TAB_KEYSTROKE));
    }

    // ---------------------------------------------------------------- TRAP

    public void testTrap_focusNeverLeaves() {
        TextInput a = field("A"), ok = field("OK"), cancel = field("CANCEL"), d = field("D");
        FocusGroup dialog = group(ok, cancel);
        dialog.focus().keepsFocusInside();
        CompositeWithFocusManager root = root(a, dialog, d);
        click(ok);

        assertEquals("CANCEL OK CANCEL OK", keys(root, new TextInput[] { a, ok, cancel, d }, times(Keys.TAB_KEYSTROKE, 4)));
    }

    // ---------------------------------------------------------------- initial focus

    private DefaultFocusManager rootManager(CompositeWithFocusManager root) {
        return root.getFocusManager();
    }

    public void testInitialFocus_defaultsToFirstField() {
        TextInput a = field("A"), b = field("B"), c = field("C");
        CompositeWithFocusManager root = root(a, group(b, c));

        rootManager(root).giveInitialFocus(bus);
        bus.drain();

        assertEquals("A", focused(a, b, c));
    }

    public void testInitialFocus_chosenComponent() {
        TextInput a = field("A"), b = field("B"), c = field("C");
        CompositeWithFocusManager root = root(a, group(b, c));

        rootManager(root).setInitialFocus(c).giveInitialFocus(bus);
        bus.drain();

        assertEquals("C", focused(a, b, c));
        assertEquals("then TAB continues from there", "A", keys(root, new TextInput[] { a, b, c }, Keys.TAB_KEYSTROKE));
    }

    public void testInitialFocus_offOrAlreadyFocused_changesNothing() {
        TextInput a = field("A"), b = field("B");
        CompositeWithFocusManager root = root(a, b);

        rootManager(root).setAutoFocus(false).giveInitialFocus(bus);
        String whenOff = focused(a, b);
        rootManager(root).setAutoFocus(true);
        click(b);
        rootManager(root).giveInitialFocus(bus);

        assertEquals("-", whenOff);
        assertEquals("B", focused(a, b));
    }

    // ---------------------------------------------------------------- answers only

    /** enter / advance only answer: asking changes no focus state */
    public void testEnterAndAdvanceDoNotChangeFocus() {
        TextInput a = field("A"), b = field("B"), c = field("C");
        FocusGroup g = group(b, c);
        CompositeWithFocusManager root = root(a, g);
        click(a);

        Focusable entered = ((FocusManager) g.asFocusable()).enter(FocusDirection.BACKWARD);
        Navigation next = root.advance(focusable(a), FocusDirection.FORWARD);

        assertSame(focusable(c), entered);
        assertSame(focusable(b), next.getTarget());
        assertEquals("A", focused(a, b, c));
    }
}
