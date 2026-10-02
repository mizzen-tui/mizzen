package dtb.terminalIO.components;

import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Focus when the screen changes AFTER it was built - fields added, removed, hidden or shown later,
 * nested inside plain panels, the way apps grow. Layout: root[A, panel[B, C], D], root as TUIApp
 * builds it. No rebuildFocusManager() calls anywhere: a developer should not need to know about it.
 *
 * Tests named ..._today_... record what happens TODAY where it is wrong (focus redesign step 7
 * fixes these: each focus manager's children are worked out from the components when needed).
 */
public class DynamicStructureCharacterisationTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private CompositeWithFocusManager root;
    private VerticalPanel panel;
    private TextInput a, b, c, d;

    @Override
    protected void setUp() {
        a = field("A");
        b = field("B");
        c = field("C");
        d = field("D");
        panel = new VerticalPanel();
        panel.add(b);
        panel.add(c);
        root = new CompositeWithFocusManager();
        root.add(a);
        root.add(panel);
        root.add(d);
        bus.setRoot(root);
        click(a);
    }

    private static TextInput field(String name) {
        TextInput field = new TextInput(Colors.IYel, 10);
        field.setName(name);
        return field;
    }

    private void click(Object component) {
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(component)));
    }

    /** the focused field's name, "-" if none */
    private String focused() {
        Object f = FocusManager.UTIL.findCurrentFocus(root);
        String name = f == null ? "-" : String.valueOf(HasName.UTIL.getName(f));
        return name.startsWith("TextInput[") ? name.substring("TextInput[".length(), name.indexOf(']')) : name;
    }

    /** TAB n times from the current focus, e.g. "B C D A" */
    private String tabs(int n) {
        StringBuilder seq = new StringBuilder();
        for (int i = 0; i < n; i++) {
            bus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));
            seq.append(i == 0 ? "" : " ").append(focused());
        }
        return seq.toString();
    }

    public void testBuiltUpFront_tabVisitsEveryField() {
        assertEquals("B C D A", tabs(4));
    }

    /** a field added later inside the nested panel: TAB should reach it (B C E D A) */
    public void testFieldAddedLaterInsidePanel_today_tabSkipsIt() {
        TextInput e = field("E");
        panel.add(e);

        assertEquals("B C D A", tabs(4));
    }

    /** a field added later inside the nested panel: clicking it should focus it */
    public void testFieldAddedLaterInsidePanel_today_clickDoesNothing() {
        TextInput e = field("E");
        panel.add(e);

        click(e);

        assertEquals("A", focused());
    }

    /** a field removed later from the nested panel: TAB should not land on it (B D A) */
    public void testFieldRemovedLaterFromPanel_today_tabStillLandsOnIt() {
        panel.remove(c);

        assertEquals("B C D A", tabs(4));
    }

    /** a field hidden later: TAB should skip it (B D A) */
    public void testFieldHiddenLater_today_tabStillLandsOnIt() {
        panel.hide(c);

        assertEquals("B C D A", tabs(4));
    }

    /** a whole panel added later at the root: works today, because the root registers it on add */
    public void testPanelAddedLaterAtRoot_tabReachesItsFields() {
        VerticalPanel later = new VerticalPanel();
        TextInput e = field("E");
        later.add(e);
        root.add(later);

        assertEquals("B C D E A", tabs(5));
    }
}
