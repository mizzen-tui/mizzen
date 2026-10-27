package mizzen.events.focus;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.HasName;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Characterisation tests for the focus *protocol*: which focus / unFocus / setFocus calls a
 * component receives, in which order and with which argument; plus traversal edge cases.
 * Records TODAY's behaviour (including known bugs, marked _today_) so the focus redesign
 * (docs/ARCHITECTURE.md §4) cannot change it silently. A failure means "behaviour changed":
 * decide whether that is wanted, then update the expectation deliberately.
 *
 * Uses real DefaultFocusManagers: a root (no parent, like TUIApp's) and nested GROUPs.
 */
public class FocusProtocolCharacterisationTest extends TestCase {

    private final List<String> log = new ArrayList<>();
    private final SyncEventLoop bus = new SyncEventLoop();

    /** a leaf focusable that records every focus notification it receives */
    class Leaf implements Focusable, HasName<Leaf> {
        String name;
        boolean focus;
        boolean enabled = true;
        FocusManager parent;
        TerminalEvent.TYPE claims;      // event type this leaf claims (null = none)
        int handled;

        Leaf(String name) { this.name = name; }

        public boolean hasFocus() { return focus; }
        public void setFocus(boolean f, EventBus e) { log.add(name + ".setFocus(" + f + ")"); focus = f; }
        public void focus(Focusable old, EventBus e) { log.add(name + ".focus(old=" + nameOf(old) + ")"); focus = true; }
        public void unFocus(Focusable nu, EventBus e) { log.add(name + ".unFocus(new=" + nameOf(nu) + ")"); focus = false; }
        public boolean isEnabled() { return enabled; }
        public EventHandler canHandle(TerminalEvent<?> te) { return te.getType() == claims ? this : null; }
        public void handle(TerminalEvent<?> te) { handled++; }
        public FocusManager getParentFocusManager() { return parent; }
        public void setParentFocusManager(FocusManager fm) { parent = fm; }
        public String getName() { return name; }
        public Leaf setName(String n) { name = n; return this; }
        public String toString() { return name; }
    }

    private static String nameOf(Object o) {
        return o == null ? "null" : String.valueOf(HasName.UTIL.getName(o));
    }

    private DefaultFocusManager root(Focusable... children) {
        DefaultFocusManager root = new DefaultFocusManager().setName("root");
        for (Focusable c : children) root.addFocusable(c);
        bus.setRoot(root);
        return root;
    }

    private DefaultFocusManager group(String name, Focusable... children) {
        DefaultFocusManager group = new DefaultFocusManager().setName(name);
        for (Focusable c : children) group.addFocusable(c);
        return group;
    }

    private void press(KeyStroke key) { bus.deliver(new KeyEvent("test", key)); }

    /** what a mouse click does: REQUEST_FOCUS for the clicked focusable */
    private void click(Focusable target) { bus.deliver(new RequestFocusEvent(target)); }

    private String focused(Leaf... leaves) {
        StringBuilder sb = new StringBuilder();
        for (Leaf l : leaves) if (l.hasFocus()) sb.append(sb.length() == 0 ? "" : "+").append(l.name);
        return sb.length() == 0 ? "-" : sb.toString();
    }

    // ------------------------------------------------------------------ notifications

    /** within one manager: old gets unFocus(new) first, then new gets focus(old) */
    public void testNotificationsWithinOneManager() {
        Leaf a = new Leaf("A"), b = new Leaf("B");
        root(a, b);
        click(a);
        log.clear();

        press(Keys.TAB_KEYSTROKE);

        assertEquals("[A.unFocus(new=B), B.focus(old=A)]", log.toString());
    }

    /**
     * Leaving a nested group: the leaf inside the group is told the new focusable is the
     * *sibling at the level where focus moved* (D), not something inside the group.
     * DropdownListBox / Menu rely on unFocus(newFocusable) for their hand-off logic.
     */
    public void testUnFocusArgumentIsSiblingAtSplitLevel_leavingGroup() {
        Leaf a = new Leaf("A"), b = new Leaf("B"), c = new Leaf("C"), d = new Leaf("D");
        DefaultFocusManager g = group("group", b, c);
        root(a, g, d);
        click(c);
        log.clear();

        press(Keys.TAB_KEYSTROKE);          // C -> D

        assertEquals("[C.unFocus(new=D), D.focus(old=group)]", log.toString());
    }

    /** entering a nested group: the leaf receives focus(old) with the previous sibling at that level */
    public void testFocusArgumentEnteringGroup() {
        Leaf a = new Leaf("A"), b = new Leaf("B"), c = new Leaf("C");
        DefaultFocusManager g = group("group", b, c);
        root(a, g);
        click(a);
        log.clear();

        press(Keys.TAB_KEYSTROKE);          // A -> group (-> B)

        assertEquals("[A.unFocus(new=group), B.focus(old=A)]", log.toString());
    }

    /**
     * Clicking B while the group remembers C: focus goes straight to B. (Before moveFocus's two
     * passes, C briefly got focus first - C.focus then C.unFocus - a visible flicker.)
     * The click still first re-sends setFocus(true) down the focused path (D here) so windows that
     * stay focused re-hilite / come to front.
     */
    public void testClickIntoGroup_noFlickerOfRememberedChild() {
        Leaf a = new Leaf("A"), b = new Leaf("B"), c = new Leaf("C"), d = new Leaf("D");
        DefaultFocusManager g = group("group", b, c);
        root(a, g, d);
        click(c);                            // group now remembers C
        click(d);
        log.clear();

        click(b);

        assertEquals("[D.setFocus(true), D.unFocus(new=group), B.focus(old=D)]", log.toString());
    }

    /**
     * If the root has lost its current child (e.g. the focused dropdown list was removed from it),
     * a click must not first hand focus to the root's first child. (It used to: that is how the
     * wrong window came to the front in ImageSplitterTest.)
     */
    public void testClickWhenRootLostItsCurrentChild_doesNotFocusFirstChild() {
        Leaf a = new Leaf("A"), b = new Leaf("B"), c = new Leaf("C");
        DefaultFocusManager root = root(a, b, c);
        click(b);
        root.removeFocusable(b);             // like hideListBox() removing the open list
        log.clear();

        click(c);

        assertFalse("A must not be touched: " + log, log.toString().contains("A."));
        assertEquals("C", focused(a, c));
    }

    /**
     * Same situation, but the next thing to arrive is an ordinary (non-key) event - e.g. a canvas
     * refresh right after a dropdown list closed. It must not make the root pick and focus its first
     * child either. (It used to: the root's routing picked a "current child" for every event.)
     */
    public void testNonKeyEventWhenRootLostItsCurrentChild_doesNotFocusFirstChild() {
        Leaf a = new Leaf("A"), b = new Leaf("B"), c = new Leaf("C");
        DefaultFocusManager root = root(a, b, c);
        click(b);
        root.removeFocusable(b);
        log.clear();

        bus.deliver(customEvent());

        assertEquals("[]", log.toString());
    }

    // ------------------------------------------------------------------ traversal edge cases

    /**
     * Nothing has focus yet: TAB focuses the first component, SHIFT-TAB the last (as browsers do).
     * (Before: TAB did nothing until something was clicked.)
     */
    public void testNothingFocused_tabFocusesFirst() {
        Leaf a = new Leaf("A"), b = new Leaf("B");
        root(a, b);

        press(Keys.TAB_KEYSTROKE);
        String afterOne = focused(a, b);
        press(Keys.TAB_KEYSTROKE);

        assertEquals("A B", afterOne + " " + focused(a, b));
    }

    public void testNothingFocused_shiftTabFocusesLast() {
        Leaf a = new Leaf("A"), b = new Leaf("B");
        root(a, b);

        press(Keys.SHIFT_TAB_KEYSTROKE);

        assertEquals("B", focused(a, b));
    }

    /** disabled focusables are skipped by TAB */
    public void testDisabledIsSkipped() {
        Leaf a = new Leaf("A"), b = new Leaf("B"), c = new Leaf("C");
        b.enabled = false;
        root(a, b, c);
        click(a);

        press(Keys.TAB_KEYSTROKE);
        String afterOne = focused(a, b, c);
        press(Keys.TAB_KEYSTROKE);

        assertEquals("C A", afterOne + " " + focused(a, b, c));
    }

    /**
     * A nested group as the root's only child wraps around. (Before the navigator it dead-ended:
     * the group had cycling off and the root needed >= 2 children to move: A B B B.)
     */
    public void testSingleNestedGroupUnderRoot_wrapsAround() {
        Leaf a = new Leaf("A"), b = new Leaf("B");
        root(group("group", a, b));
        click(a);

        StringBuilder seq = new StringBuilder(focused(a, b));
        for (int i = 0; i < 3; i++) { press(Keys.TAB_KEYSTROKE); seq.append(' ').append(focused(a, b)); }

        assertEquals("A B A B", seq.toString());
    }

    // ------------------------------------------------------------------ non-key event delivery

    private TerminalEvent<Object> customEvent() {
        return new TerminalEvent<Object>("test", TerminalEvent.TYPE.CUSTOM_APPLICATION_EVENT, null);
    }

    /**
     * Non-key event the focused child does not claim: only the FIRST other child that claims it
     * gets it - at the root too. (Before: the root, NAVKEYS_LAST, delivered it to EVERY claimer;
     * none of the examples ever had more than one claimer.)
     */
    public void testNonKeyEvent_root_deliversToFirstClaimerOnly() {
        Leaf current = new Leaf("current"), x = new Leaf("X"), y = new Leaf("Y");
        x.claims = y.claims = TerminalEvent.TYPE.CUSTOM_APPLICATION_EVENT;
        root(current, x, y);
        click(current);

        bus.deliver(customEvent());

        assertEquals("X=1 Y=0", "X=" + x.handled + " Y=" + y.handled);
    }

    /** same inside a nested group: only the FIRST other child that claims it gets it */
    public void testNonKeyEvent_group_deliversToFirstClaimerOnly() {
        Leaf current = new Leaf("current"), x = new Leaf("X"), y = new Leaf("Y");
        x.claims = y.claims = TerminalEvent.TYPE.CUSTOM_APPLICATION_EVENT;
        DefaultFocusManager g = group("group", current, x, y);
        root(new Leaf("other"), g);
        click(current);

        bus.deliver(customEvent());

        assertEquals("X=1 Y=0", "X=" + x.handled + " Y=" + y.handled);
    }

    /** the focused child that claims a non-key event gets it, and nobody else does */
    public void testNonKeyEvent_focusedClaimerWins() {
        Leaf current = new Leaf("current"), x = new Leaf("X");
        current.claims = x.claims = TerminalEvent.TYPE.CUSTOM_APPLICATION_EVENT;
        root(current, x);
        click(current);

        bus.deliver(customEvent());

        assertEquals("current=1 X=0", "current=" + current.handled + " X=" + x.handled);
    }
}
