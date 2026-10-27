package mizzen.events;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.TextComponent;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.TextInput;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.controlcodes.ControlCode;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.DefaultFocusManager;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/** Small fixes in the event / focus infrastructure (ARCHITECTURE.md §5), each pinned by a test */
public class EventInfrastructureFixesTest extends TestCase {

    private static ConditionalEventHandler named(String name, List<String> heard) {
        return new ConditionalEventHandler.Impl((te) -> true, (te) -> heard.add(name)).setName(name);
    }

    /** EventHandlerContainer.Simple: prepend puts a handler first, append last; prependTo keeps the order */
    public void testSimpleContainerOrder() {
        List<String> heard = new ArrayList<String>();
        EventHandlerContainer.SimpleList list = new EventHandlerContainer.SimpleList();
        list.appendEventHandler(named("b", heard));
        list.appendEventHandler(named("c", heard));
        list.prependEventHandler(named("a", heard));
        assertEquals("[a, b, c]", names(list.getEventHandlers()));

        EventHandlerContainer.SimpleList dest = new EventHandlerContainer.SimpleList();
        dest.appendEventHandler(named("z", heard));
        list.prependTo(dest);
        assertEquals("[a, b, c, z]", names(dest.getEventHandlers()));
    }

    private static String names(List<ConditionalEventHandler> handlers) {
        List<String> names = new ArrayList<String>();
        for (ConditionalEventHandler handler : handlers) {
            names.add(mizzen.components.HasName.UTIL.getName(handler));
        }
        return names.toString();
    }

    /** ChainedEventHandler(notFoundHandler): a working chain (before: no strategy - NullPointerException on the first event) */
    public void testChainWithNotFoundHandlerWorks() {
        List<String> heard = new ArrayList<String>();
        ChainedEventHandler chain = new ChainedEventHandler((te) -> heard.add("not found"));
        chain.appendEventHandler(new ConditionalEventHandler.Impl((te) -> false, (te) -> heard.add("never")));

        chain.handle(new KeyEvent("test", new KeyStroke('x')));

        assertFalse(heard.contains("never"));
    }

    /** ConditionalEventHandler.AbstractDelegate.setName keeps the name (before: ignored) */
    public void testDelegateKeepsItsName() {
        ConditionalEventHandler.AbstractDelegate delegate = new ConditionalEventHandler.AbstractDelegate() {
            @Override
            protected ConditionalEventHandler getDelegate() {
                return ConditionalEventHandler.NOOP;
            }
        };
        delegate.setName("mine");
        assertEquals("mine", delegate.getName());
    }

    /** the mouse mode codes are the terminal's (?1002 ...), not five digits (?10021 ...) */
    public void testMouseControlCodes() {
        assertTrue(text(ControlCode.SET_SGR_EXT_MODE_MOUSE).endsWith("[?1006h"));
        assertTrue(text(ControlCode.DISABLE_BTN_EVENT_MOUSE).endsWith("[?1002l"));
        assertTrue(text(ControlCode.DISABLE_PIXEL_POSITION_MOUSE).endsWith("[?1016l"));
    }

    private static String text(ControlCode code) {
        return ((ControlCode.BaseTextControlCode) code).getControlCode();
    }

    /** setChildAsCurrentFocus(null) says what is wrong (before: NullPointerException) */
    public void testSetChildAsCurrentFocusNull() {
        DefaultFocusManager focusManager = new DefaultFocusManager();
        try {
            focusManager.setChildAsCurrentFocus((Focusable) null, new EventBus.STUBBED());
            fail("null must be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("NULL"));
        }
    }

    /**
     * ScrollableComponent: an arrow nothing inside uses scrolls the pane - focus stays where it is
     * (before: the footer button's hot key moved focus onto the button). Layout: root[pane[a, long text]].
     */
    public void testScrollKeysDoNotMoveFocusToTheFooter() throws Exception {
        TextInput a = new TextInput(Colors.IYel, 10);
        a.setName("a");
        VerticalPanel content = new VerticalPanel();
        content.add(a);
        for (int i = 0; i < 30; i++) {
            content.add(new TextComponent("line " + i));
        }
        ScrollableComponent pane = new ScrollableComponent(Colors.IGre, new Dimension.Simple(8, 60), content);   // wide enough for its footer
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(pane);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        SyncEventLoop bus = new SyncEventLoop();
        bus.setRoot(root);
        bus.deliver(RequestFocusEvent.fromSource(a));

        bus.deliver(new KeyEvent("test", Keys.DOWN_KEYSTROKE));

        Focusable focused = FocusManager.UTIL.findCurrentFocus(root);
        assertEquals("a", mizzen.components.HasName.UTIL.getName(focused.getFocusComponent()));
        assertEquals(1, pane.getOffsetIntoSource().getRow());
    }

    /** a scroll pane always shows its border: setBorder(false) is refused (before: it silently changed an invisible box) */
    public void testScrollPaneBorder() {
        ScrollableComponent pane = new ScrollableComponent(Colors.IGre, new Dimension.Simple(8, 30), new TextComponent("x"));
        assertTrue(pane.hasBorder());
        pane.setBorder(true);
        try {
            pane.setBorder(false);
            fail("a scroll pane always shows its border");
        } catch (IllegalStateException expected) {
            assertTrue(pane.hasBorder());
        }
    }
}
