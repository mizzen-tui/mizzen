package mizzen.components;

import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.Keys;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Focus when the screen changes AFTER it was built - fields added, removed, hidden or shown later,
 * nested inside plain panels, the way apps grow. Layout: root[A, panel[B, C], D], root as TUIApp
 * builds it. No rebuildFocusManager() calls anywhere: a developer should not need to know about it.
 *
 * Since focus redesign step 7c each focus manager's children are worked out from the components
 * whenever needed, so none of this needs registering. (Before, fields added / removed / hidden
 * later were missed: TAB skipped new ones, clicks on them did nothing, TAB landed on removed ones.)
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

    /** a field added later inside the nested panel: TAB reaches it */
    public void testFieldAddedLaterInsidePanelIsReachedByTab() {
        TextInput e = field("E");
        panel.add(e);

        assertEquals("B C E D A", tabs(5));
    }

    /** a field added later inside the nested panel: clicking it focuses it */
    public void testFieldAddedLaterInsidePanelCanBeClicked() {
        TextInput e = field("E");
        panel.add(e);

        click(e);

        assertEquals("E", focused());
    }

    /** a field removed later from the nested panel: TAB no longer lands on it */
    public void testFieldRemovedLaterFromPanelIsSkipped() {
        panel.remove(c);

        assertEquals("B D A", tabs(3));
    }

    /** a field hidden later: TAB skips it */
    public void testFieldHiddenLaterIsSkipped() {
        panel.hide(c);

        assertEquals("B D A", tabs(3));
    }

    /** a whole panel added later at the root: works today, because the root registers it on add */
    public void testPanelAddedLaterAtRoot_tabReachesItsFields() {
        VerticalPanel later = new VerticalPanel();
        TextInput e = field("E");
        later.add(e);
        root.add(later);

        assertEquals("B C D E A", tabs(5));
    }

    /** a field that counts the custom (non-key) events it gets */
    static class CountingField extends TextInput {
        int got;
        CountingField(String name) {
            super(Colors.IYel, 10);
            setName(name);
        }
        @Override
        public mizzen.events.EventHandler canHandle(mizzen.events.TerminalEvent<?> te) {
            return te.getType() == mizzen.events.TerminalEvent.TYPE.CUSTOM_APPLICATION_EVENT ? this : super.canHandle(te);
        }
        @Override
        public void handle(mizzen.events.TerminalEvent<?> te) {
            if (te.getType() == mizzen.events.TerminalEvent.TYPE.CUSTOM_APPLICATION_EVENT) {
                got++;
                return;
            }
            super.handle(te);
        }
    }

    /**
     * A removed component is not offered events any more, even if it had focus when it was removed
     * (it was still the "current child"). E.g. a dropdown's old scroll pane got scroll-to-view
     * requests for the list it no longer held: "Bad state: offset null".
     */
    public void testRemovedFocusedComponentGetsNoMoreEvents() {
        CountingField gone = new CountingField("gone");
        panel.add(gone);
        click(gone);
        panel.remove(gone);

        bus.deliver(new mizzen.events.TerminalEvent<Object>("test", mizzen.events.TerminalEvent.TYPE.CUSTOM_APPLICATION_EVENT, null));

        assertEquals(0, gone.got);
    }
}
