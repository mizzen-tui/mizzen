package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.RequestScrollToView;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Characterisation tests for VISIBLE side effects of focus changes: window frame border
 * hiliting, component recolouring, scroll-to-view requests. These are desired features driven
 * by the focus / unFocus / setFocus notifications; the focus redesign must keep them
 * (docs/ARCHITECTURE.md §4.4). A failure means "behaviour changed" - decide, then update.
 */
public class FocusSideEffectsCharacterisationTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final List<String> log = new ArrayList<>();

    /** records hilite / unHilite calls (the frame has no getter for its hilite state) */
    class SpyFrame extends WindowFrameFocusable {
        SpyFrame(String title, Component<?> child) {
            super(Colors.IYel, title, child);
        }
        @Override
        public void hilite() { log.add("frame.hilite"); super.hilite(); }
        @Override
        public void unHilite() { log.add("frame.unHilite"); super.unHilite(); }
    }

    private static TextInput field(String name) {
        TextInput field = new TextInput(Colors.IYel, 10);
        field.setName(name);
        return field;
    }

    private void click(Object component) {
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(component)));
    }

    private boolean hasFocus(Object component) {
        return ToFocusable.UTIL.toFocusable(component).hasFocus();
    }

    /** root[outside, frame[inside]] -- root as TUIApp builds it */
    private CompositeWithFocusManager frameLayout(TextInput outside, SpyFrame frame) {
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(outside);
        root.add(frame);
        bus.setRoot(root);
        return root;
    }

    /** focusing a component inside a window frame hilites the frame; leaving it unhilites it */
    public void testWindowFrameHilitesWhileFocusIsInside() {
        TextInput outside = field("outside"), inside = field("inside");
        VerticalPanel content = new VerticalPanel();
        content.add(inside);
        SpyFrame frame = new SpyFrame("frame", content);
        frameLayout(outside, frame);

        click(outside);
        log.clear();
        click(inside);
        String enter = log.toString();
        assertTrue("inside should have focus", hasFocus(inside));

        log.clear();
        click(outside);
        String leave = log.toString();

        assertTrue("entering the frame should hilite it, log: " + enter, enter.contains("frame.hilite"));
        assertFalse(enter.contains("frame.unHilite"));
        assertTrue("leaving the frame should unhilite it, log: " + leave, leave.contains("frame.unHilite"));
    }

    /**
     * TAB into and out of a frame drives the same hilite / unhilite. The frame's own parts are
     * focusables too: title bar (DraggableTextComponent) first, resize anchor last, so TAB goes
     * outside -> title -> inside -> resize anchor -> outside.
     */
    public void testWindowFrameHiliteFollowsTab() {
        TextInput outside = field("outside"), inside = field("inside");
        VerticalPanel content = new VerticalPanel();
        content.add(inside);
        SpyFrame frame = new SpyFrame("frame", content);
        CompositeWithFocusManager root = frameLayout(outside, frame);
        click(outside);
        log.clear();

        StringBuilder seq = new StringBuilder();
        List<String> hiliteAtStep = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            bus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));
            seq.append(i == 0 ? "" : " ").append(focusedName(root));
            hiliteAtStep.add(log.toString());
            log.clear();
        }

        assertEquals("DraggableTextComponent inside ResizeAnchor outside", seq.toString());
        assertTrue("TAB into frame should hilite it: " + hiliteAtStep, hiliteAtStep.get(0).contains("frame.hilite"));
        assertTrue("TAB out of frame should unhilite it: " + hiliteAtStep, hiliteAtStep.get(3).contains("frame.unHilite"));
    }

    /** name of the focused leaf: a component name, else the class part of its HasName */
    private static String focusedName(CompositeWithFocusManager root) {
        Object f = dtb.terminalIO.events.focus.FocusManager.UTIL.findCurrentFocus(root);
        String name = String.valueOf(HasName.UTIL.getName(f));
        if (name.startsWith("TextInput[")) {             // e.g. TextInput[inside] -> inside
            return name.substring("TextInput[".length(), name.indexOf(']'));
        }
        return name.replaceAll("\\[.*", "");            // e.g. DraggableTextComponent[frame] -> DraggableTextComponent
    }

    /** a focused TextInput is recoloured (hilite colour), and gets its own colour back when unfocused */
    public void testTextInputRecolouredWhileFocused() {
        TextInput a = field("a"), b = field("b");
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(a);
        root.add(b);
        bus.setRoot(root);
        click(b);

        Color unfocused = a.getColor();
        click(a);
        Color focused = a.getColor();
        click(b);
        Color unfocusedAgain = a.getColor();

        assertFalse("focused colour should differ from unfocused", focused.equals(unfocused));
        assertEquals(unfocused, unfocusedAgain);
    }

    /** focusing a TextInput raises a scroll-to-view request for it (ShowComponentOnFocus) */
    public void testFocusRaisesScrollToViewForFocusedComponent() {
        TextInput a = field("a"), b = field("b");
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(a);
        root.add(b);
        bus.setRoot(root);
        click(a);
        bus.clearHistory();

        bus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));

        List<TerminalEvent<?>> requests = bus.raised(TerminalEvent.TYPE.REQUEST_SCROLL_TO_VIEW);
        assertFalse("expected a scroll-to-view request", requests.isEmpty());
        assertSame(b, ((RequestScrollToView) requests.get(requests.size() - 1)).getScrollToComponent());
    }
}
