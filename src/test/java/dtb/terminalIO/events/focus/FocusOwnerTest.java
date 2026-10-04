package dtb.terminalIO.events.focus;

import java.util.StringJoiner;

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
 * Single focus owner (docs/ARCHITECTURE.md §4.3 A1): the ROOT focus manager records THE component
 * that has keyboard focus, every focus move keeps that record up to date, and a component that
 * missed its unFocus is switched off - so exactly one component claims focus.
 */
public class FocusOwnerTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();

    /** a field; a "sticky" one ignores unFocus (like a component that misses its notification) */
    static class Leaf implements Focusable, HasName<Leaf> {
        String name;
        boolean focus;
        boolean sticky;
        FocusManager parent;

        Leaf(String name) { this.name = name; }

        public boolean hasFocus() { return focus; }
        public void setFocus(boolean f, EventBus e) { focus = f; }
        public void focus(Focusable old, EventBus e) { focus = true; }
        public void unFocus(Focusable nu, EventBus e) { if (!sticky) focus = false; }
        public boolean isEnabled() { return true; }
        public EventHandler canHandle(TerminalEvent<?> te) { return null; }
        public void handle(TerminalEvent<?> te) { }
        public FocusManager getParentFocusManager() { return parent; }
        public void setParentFocusManager(FocusManager fm) { parent = fm; }
        public String getName() { return name; }
        public Leaf setName(String n) { name = n; return this; }
        public String toString() { return name; }
    }

    private DefaultFocusManager root(Focusable... children) {
        DefaultFocusManager root = new DefaultFocusManager().setName("root");
        for (Focusable c : children) root.addFocusable(c);
        bus.setRoot(root);
        return root;
    }

    private void click(Focusable target) { bus.deliver(new RequestFocusEvent(target)); }

    private void press(KeyStroke key) { bus.deliver(new KeyEvent("test", key)); }

    private static String claimingFocus(Leaf... leaves) {
        StringJoiner names = new StringJoiner("+");
        for (Leaf l : leaves) if (l.hasFocus()) names.add(l.name);
        return names.length() == 0 ? "-" : names.toString();
    }

    public void testOwnerFollowsClicksAndTab() {
        Leaf a = new Leaf("A"), b = new Leaf("B"), c = new Leaf("C");
        DefaultFocusManager root = root(a, b, c);

        click(b);
        Focusable afterClick = root.getFocusedComponent();
        press(Keys.TAB_KEYSTROKE);

        assertSame(b, afterClick);
        assertSame(c, root.getFocusedComponent());
        assertSame(c, FocusManager.UTIL.findCurrentFocus(root));
    }

    /** a one-stop component's own arrow keys still move focus the old way - the owner follows those moves too */
    public void testOwnerFollowsMovesMadeByTheOldRouting() {
        Leaf a = new Leaf("A"), b = new Leaf("B"), c = new Leaf("C");
        DefaultFocusManager list = new DefaultFocusManager(Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE).setFocusPolicy(FocusPolicy.ONE_STOP).setName("list");
        list.addFocusable(b);
        list.addFocusable(c);
        DefaultFocusManager root = root(a, list);

        click(b);
        press(Keys.DOWN_KEYSTROKE);

        assertSame(c, root.getFocusedComponent());
    }

    /** a component that misses its unFocus is switched off: only one component claims focus */
    public void testComponentThatMissedItsUnFocusIsSwitchedOff() {
        Leaf sticky = new Leaf("STICKY"), b = new Leaf("B");
        sticky.sticky = true;
        DefaultFocusManager root = root(sticky, b);

        click(sticky);
        click(b);

        assertEquals("B", claimingFocus(sticky, b));
        assertSame(b, root.getFocusedComponent());
    }

    /** once the focused component is removed, it is no longer reported as having focus */
    public void testRemovedOwnerIsNotReported() {
        Leaf a = new Leaf("A"), b = new Leaf("B");
        DefaultFocusManager root = root(a, b);
        click(b);

        root.removeFocusable(b);

        assertNotSame(b, FocusManager.UTIL.findCurrentFocus(root));
    }

    /**
     * The focused component goes away (e.g. a dropdown's open list closing): TAB carries on from the
     * component focused before it, not from the top.
     */
    public void testFocusedComponentRemoved_tabCarriesOnFromWhereFocusWas() {
        Leaf a = new Leaf("A"), b = new Leaf("B"), c = new Leaf("C"), popup = new Leaf("POPUP");
        DefaultFocusManager root = root(a, b, c, popup);
        click(b);
        click(popup);
        root.removeFocusable(popup);

        press(Keys.TAB_KEYSTROKE);

        assertSame(c, root.getFocusedComponent());
    }
}
