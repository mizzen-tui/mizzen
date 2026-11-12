package mizzen.components.card;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.HasName;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.input.TextInput;
import mizzen.components.theme.Theme;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.Keys;
import mizzen.supportfortesting.SyncEventLoop;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * A tile with an action (onActivate) is a TAB stop: focused, its frame is in the accent colour; ENTER or a click
 * activates it.
 * A tile with none only shows: TAB passes it by. Layout: root[before, row[site (acts), disk (shows), mail (acts)]].
 */
public class TileActivateTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();
    private final TextInput before = new TextInput(10);
    private final Tile site = new Tile("Site", 16).value("HTTP 200");
    private final Tile disk = new Tile("Disk", 16).value("52% used");
    private final Tile mail = new Tile("Mail", 16).value("3 queued");
    private final List<String> activated = new ArrayList<String>();

    @Override
    protected void setUp() {
        before.setName("before");
        site.setName("site");
        disk.setName("disk");
        mail.setName("mail");
        site.onActivate((te) -> activated.add(((Tile.ActivateEvent) te).getTile().getName()));
        mail.onActivate((te) -> activated.add("mail"));
        root.add(before);
        root.add(2, 0, new HorizontalPanel().add(site).add(disk).add(mail));
        root.setTheme(Theme.ROUNDED);
        bus.setRoot(root);
        bus.deliver(RequestFocusEvent.fromSource(before));
    }

    private String focusedName() {
        Focusable focused = FocusManager.UTIL.findCurrentFocus(root);
        return focused == null ? "-" : HasName.UTIL.getName(focused.getFocusComponent());
    }

    public void testTabStopsOnlyAtTilesThatAct() {
        assertFalse("no handler: not focusable", ToFocusable.UTIL.isFocusable(disk));
        bus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));
        assertEquals("site", focusedName());
        bus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));
        assertEquals("mail", focusedName());
        bus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));
        assertEquals("before", focusedName());
    }

    public void testEnterActivatesTheFocusedTile() {
        bus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));
        bus.deliver(new KeyEvent("test", Keys.ENTER_KEYSTROKE));
        assertEquals("[site]", activated.toString());
    }

    public void testAClickFocusesAndActivates() {
        mail.onClick(bus);
        bus.drain();
        assertEquals("mail", focusedName());
        assertEquals("[mail]", activated.toString());
        disk.onClick(bus);                                   // shows only: nothing happens
        bus.drain();
        assertEquals("[mail]", activated.toString());
    }

    public void testFocusedTheFrameIsInTheAccentColour() {
        String accent = Theme.ROUNDED.surfaceTheme().accentColor().asString();
        assertFalse(frameColour(site).equals(accent));
        bus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));
        assertEquals(accent, frameColour(site));
        assertEquals("the value keeps its colour", Theme.ROUNDED.surfaceTheme().strongColor().asString(), colourOf(site, "HTTP 200"));
        bus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));
        assertFalse("focus gone: back to text", frameColour(site).equals(accent));
    }

    public void testOneActionANewOneReplacesItNullShowsOnly() {
        bus.deliver(new KeyEvent("test", Keys.TAB_KEYSTROKE));
        site.onActivate((te) -> activated.add("replaced"));
        bus.deliver(new KeyEvent("test", Keys.ENTER_KEYSTROKE));
        assertEquals("[replaced]", activated.toString());
        disk.onActivate((te) -> { });
        assertTrue(ToFocusable.UTIL.isFocusable(disk));
        disk.onActivate(null);
        assertFalse(ToFocusable.UTIL.isFocusable(disk));
    }

    // the colour of the tile's top-left corner
    private static String frameColour(Tile tile) {
        for (ScreenLineSegment s : tile.getSegmentsBuffer().getSegments()) {
            if (s.getRow() == 0 && s.getCol() == 0) {
                return s.getColors().asString();
            }
        }
        throw new AssertionError("no frame drawn");
    }

    private static String colourOf(Tile tile, String text) {
        for (ScreenLineSegment s : tile.getSegmentsBuffer().getSegments()) {
            if (s.getText().contains(text)) {
                return s.getColors().asString();
            }
        }
        throw new AssertionError("no '" + text + "' drawn");
    }
}
