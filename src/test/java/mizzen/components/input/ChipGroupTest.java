package mizzen.components.input;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.TextComponent;
import mizzen.core.TextSnapshot;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * ChipGroup: a row of chips, one chosen. One TAB stop; LEFT / RIGHT choose and wrap, SPACE the next, HOME / END the
 * ends; the listeners hear the user, not setValue. Layout: root[days (7, 30, 90), after (a text)].
 */
public class ChipGroupTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();
    private final ChipGroup<String> days = ChipGroup.of("7", "30", "90");
    private final List<String> heard = new ArrayList<String>();

    @Override
    protected void setUp() throws Exception {
        root.add(days);
        root.add(new TextComponent("after"));
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        bus.setRoot(root);
        days.onChange((te) -> heard.add(days.getValue()));
        bus.deliver(new RequestFocusEvent(days.asFocusable()));
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    public void testDrawnAsChipsTheFirstChosen() {
        assertEquals("[7][30][90]", new TextSnapshot(1, 11).render(days));
        assertEquals("7", days.getValue());
        assertEquals(11, days.getWidth());
    }

    public void testArrowsChooseAndWrap() {
        press(Keys.RIGHT_KEYSTROKE);
        assertEquals("30", days.getValue());
        press(Keys.LEFT_KEYSTROKE);
        press(Keys.LEFT_KEYSTROKE);
        assertEquals("round to the last", "90", days.getValue());
        press(Keys.SPACE_KEYSTROKE);
        assertEquals("the next, round again", "7", days.getValue());
        press(Keys.END_KEYSTROKE);
        assertEquals("90", days.getValue());
        press(Keys.HOME_KEYSTROKE);
        assertEquals("7", days.getValue());
        assertEquals("[30, 7, 90, 7, 90, 7]", heard.toString());
    }

    /** setValue: the program's choice - the listeners aren't told; one not among the options fails at once */
    public void testSetValue() {
        days.setValue("90");
        assertEquals("90", days.getValue());
        assertTrue(heard.isEmpty());
        days.setValue(null);
        assertEquals("nothing: the first", "7", days.getValue());
        try {
            days.setValue("365");
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("365"));
        }
    }
}
