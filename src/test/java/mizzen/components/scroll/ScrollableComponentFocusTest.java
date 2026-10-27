package mizzen.components.scroll;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.HasName;
import mizzen.components.TextComponent;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.Button;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * A ScrollableComponent whose content can't take focus (e.g. a long TextComponent) is itself a TAB
 * stop: TAB lands on the pane, its keys scroll it - and the scroll is repainted. A pane whose content
 * can take focus passes focus inside, as before.
 * Layout: root[before (text field), pane (40 lines of text, 8 high), after (text field)].
 */
public class ScrollableComponentFocusTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();
    private final TextInput before = new TextInput(Colors.IYel, 10);
    private final TextInput after = new TextInput(Colors.IYel, 10);
    private ScrollableComponent pane;

    @Override
    protected void setUp() throws Exception {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            text.append("line ").append(i).append(i < 39 ? "\n" : "");
        }
        pane = new ScrollableComponent(Colors.IGre, new Dimension.Simple(8, 60), new TextComponent(text.toString()));
        pane.setName("pane");
        before.setName("before");
        after.setName("after");
        root.add(before);
        root.add(1, 0, pane);
        root.add(10, 0, after);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        bus.setRoot(root);
        bus.deliver(RequestFocusEvent.fromSource(before));
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    private String focusedName() {
        Focusable focused = FocusManager.UTIL.findCurrentFocus(root);
        return focused == null ? "-" : HasName.UTIL.getName(focused.getFocusComponent());
    }

    public void testTabLandsOnThePane() {
        press(Keys.TAB_KEYSTROKE);

        assertEquals("pane", focusedName());
        assertTrue(pane.asFocusable().hasFocus());
    }

    /** with focus on the pane PgDn scrolls it - and asks for a repaint (before: scrolled, but the screen didn't change) */
    public void testKeysScrollThePaneAndRepaintIt() {
        press(Keys.TAB_KEYSTROKE);
        bus.history.clear();

        press(Keys.PGDN_KEYSTROKE);

        assertTrue("scrolled", pane.getOffsetIntoSource().getRow() > 0);
        assertTrue("repaint asked for", bus.history.stream().anyMatch(te -> te instanceof RequestUpdateCanvasEvent));
        assertEquals("pane", focusedName());
    }

    public void testTabMovesOnAndShiftTabComesBack() {
        press(Keys.TAB_KEYSTROKE);
        press(Keys.TAB_KEYSTROKE);
        assertEquals("after", focusedName());
        assertFalse(pane.asFocusable().hasFocus());

        press(Keys.SHIFT_TAB_KEYSTROKE);
        assertEquals("pane", focusedName());
    }

    /** a footer component with a hot key - added the way the search function adds itself (addFooterComponent) */
    private Button footerButtonWithHotKey(List<String> pressed) {
        Button button = new Button(Colors.IGre, "Find");
        button.setName("find");
        button.setHotKey(Keys.CTRL_F_KEYSTROKE);
        button.onChange((te) -> pressed.add("find"));
        pane.addFooterComponent(button);
        return button;
    }

    /** its hot key from outside the pane: focus goes to the footer button itself, which acts */
    public void testFooterHotKeyFromOutside() {
        List<String> pressed = new ArrayList<String>();
        footerButtonWithHotKey(pressed);

        press(Keys.CTRL_F_KEYSTROKE);

        assertEquals("find", focusedName());
        assertEquals("[find]", pressed.toString());
    }

    /** its hot key with focus on the pane (its own stop): the hot keys inside it come first */
    public void testFooterHotKeyWithThePaneFocused() {
        List<String> pressed = new ArrayList<String>();
        footerButtonWithHotKey(pressed);
        press(Keys.TAB_KEYSTROKE);
        assertEquals("pane", focusedName());

        press(Keys.CTRL_F_KEYSTROKE);

        assertEquals("find", focusedName());
        assertEquals("[find]", pressed.toString());
    }

    /**
     * Two components with the same hot key (CTRL-F): the one nearest the focus wins - here the
     * focused pane's own footer button, not the cousin's button in another pane.
     * Layout: root[before, pane (footer: find), after, other[button CTRL-F]].
     */
    public void testNearestHotKeyWinsOverACousin() {
        List<String> pressed = new ArrayList<String>();
        footerButtonWithHotKey(pressed);
        Button cousin = new Button(Colors.IGre, "left");
        cousin.setName("cousin");
        cousin.setHotKey(Keys.CTRL_F_KEYSTROKE);
        cousin.onChange((te) -> pressed.add("cousin"));
        mizzen.components.composite.Composite other = new mizzen.components.composite.Composite();
        other.add(cousin);
        root.add(12, 0, other);
        press(Keys.TAB_KEYSTROKE);
        assertEquals("pane", focusedName());

        press(Keys.CTRL_F_KEYSTROKE);

        assertEquals("find", focusedName());
        assertEquals("[find]", pressed.toString());
    }

    /** content that can take focus: focus goes inside the pane, not onto it */
    public void testFocusableContentStillGetsTheFocus() throws Exception {
        TextInput field = new TextInput(Colors.IYel, 10);
        field.setName("field");
        VerticalPanel content = new VerticalPanel();
        content.add(field);
        ScrollableComponent formPane = new ScrollableComponent(Colors.IGre, new Dimension.Simple(8, 60), content);
        CompositeWithFocusManager formRoot = new CompositeWithFocusManager();
        TextInput first = new TextInput(Colors.IYel, 10);
        formRoot.add(first);
        formRoot.add(1, 0, formPane);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(formRoot);
        SyncEventLoop formBus = new SyncEventLoop();
        formBus.setRoot(formRoot);
        formBus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(first)));

        formBus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));

        Focusable focused = FocusManager.UTIL.findCurrentFocus(formRoot);
        assertEquals("field", HasName.UTIL.getName(focused.getFocusComponent()));
    }
}
