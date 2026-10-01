package dtb.terminalIO.events.focus;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.HasName;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Characterisation tests for the focus *protocol*: which focus / unFocus / setFocus calls a
 * component receives, in which order and with which argument; plus traversal edge cases.
 * Records TODAY's behaviour (including known bugs, marked _today_) so the focus redesign
 * (docs/ARCHITECTURE.md §4) cannot change it silently. A failure means "behaviour changed":
 * decide whether that is wanted, then update the expectation deliberately.
 *
 * Uses real DefaultFocusManagers; the root is configured like TUIApp's (NAVKEYS_LAST), nested
 * groups use the DefaultFocusManager default (NAVKEYS_FIRST, cycling inferred off).
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
        DefaultFocusManager root = new DefaultFocusManager().setNavKeysFirst(false).setName("root");
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
     * TODAY (click focus flicker, docs/ARCHITECTURE.md §4.6): clicking B while the group
     * remembers C briefly focuses C (C.focus then C.unFocus) before B gets focus.
     * Also: the request-focus handler first calls setFocus(true) on the root, which re-sends
     * setFocus(true) to the currently focused leaf (D) just before it is unfocused.
     */
    public void testClickIntoGroup_today_flickersRememberedChild() {
        Leaf a = new Leaf("A"), b = new Leaf("B"), c = new Leaf("C"), d = new Leaf("D");
        DefaultFocusManager g = group("group", b, c);
        root(a, g, d);
        click(c);                            // group now remembers C
        click(d);
        log.clear();

        click(b);

        assertEquals("[D.setFocus(true), D.unFocus(new=group), C.focus(old=D), C.unFocus(new=B), B.focus(old=C)]",
                     log.toString());
    }

    // ------------------------------------------------------------------ traversal edge cases

    /** TODAY: nothing has focus until something is clicked; TAB alone focuses nothing */
    public void testNoInitialFocus_today_tabDoesNothing() {
        Leaf a = new Leaf("A"), b = new Leaf("B");
        root(a, b);

        press(Keys.TAB_KEYSTROKE);
        press(Keys.TAB_KEYSTROKE);

        assertEquals("-", focused(a, b));
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
     * TODAY (known bug): a nested group as the root's only child dead-ends - the group has
     * cycling off, the root needs >= 2 children to move, so focus sticks on the last leaf.
     */
    public void testSingleNestedGroupUnderRoot_today_getsStuck() {
        Leaf a = new Leaf("A"), b = new Leaf("B");
        root(group("group", a, b));
        click(a);

        StringBuilder seq = new StringBuilder(focused(a, b));
        for (int i = 0; i < 3; i++) { press(Keys.TAB_KEYSTROKE); seq.append(' ').append(focused(a, b)); }

        assertEquals("A B B B", seq.toString());
    }

    // ------------------------------------------------------------------ non-key event delivery

    private TerminalEvent<Object> customEvent() {
        return new TerminalEvent<Object>("test", TerminalEvent.TYPE.CUSTOM_APPLICATION_EVENT, null);
    }

    /**
     * Non-key event the focused child does not claim: a NAVKEYS_LAST manager (root, as TUIApp
     * builds it) delivers it to EVERY other child that claims it.
     */
    public void testNonKeyEvent_navKeysLast_deliversToAllClaimers() {
        Leaf current = new Leaf("current"), x = new Leaf("X"), y = new Leaf("Y");
        x.claims = y.claims = TerminalEvent.TYPE.CUSTOM_APPLICATION_EVENT;
        root(current, x, y);
        click(current);

        bus.deliver(customEvent());

        assertEquals("X=1 Y=1", "X=" + x.handled + " Y=" + y.handled);
    }

    /**
     * Same, but under a NAVKEYS_FIRST manager (the DefaultFocusManager / ComponentWithFocusManager
     * default): only the FIRST other child that claims it gets it.
     */
    public void testNonKeyEvent_navKeysFirst_deliversToFirstClaimerOnly() {
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
