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
        assertEquals(" [7] [30] [90] ", new TextSnapshot(1, 15).render(days));    // Legacy marks button focus with brackets
        assertEquals("7", days.getValue());
        assertEquals(15, days.getWidth());
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

    /** raised: filled chips on the band, the same width - a theme changed while it shows moves nothing */
    public void testRaisedIsTheSameWidth() {
        days.look(ChipGroup.Look.RAISED);
        assertEquals("  7   30   90  ", new TextSnapshot(1, 15).render(days));
        assertEquals(15, days.getWidth());
    }

    /** the parts' colours: a fixed Color, or one that follows the theme - asked as it draws */
    public void testColoursOfYourOwn() {
        days.setChosenColor(mizzen.controlcodes.color.Colors.IGre);
        String green = new TextSnapshot(1, 15).ansiColors().render(days);
        assertTrue(green.replace("\u001b", "ESC"), green.contains("92m[7]"));     // IGre: 92 - focused, its brackets too
        days.setChosenColor(days.themeColor(mizzen.components.theme.Theme::errorColor));
        String red = new TextSnapshot(1, 15).ansiColors().render(days);
        assertTrue(red.replace("\u001b", "ESC"), red.contains("91m[7]"));             // Legacy's error: BIRed, 91
        days.setChosenColor(null);
        assertFalse("back to the theme's accent", new TextSnapshot(1, 15).ansiColors().render(days).contains("91m[7]"));
    }

    /** a click on a chip chooses it; on a gap, nothing */
    public void testClicks() {
        click(5);                                                    // " [7] [30] [90] ": the 3 of 30
        assertEquals("30", days.getValue());
        click(9);                                                    // the gap after [30]
        assertEquals("30", days.getValue());
        assertEquals("[30]", heard.toString());
    }

    private void click(int col) {
        days.onClick(mizzen.events.mouse.MouseEventsForTesting.click(0, col, bus));       // relative to the group
        bus.drain();                                                 // the change it raised, to the listeners
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
