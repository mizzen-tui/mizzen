package mizzen.components.input;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.theme.Theme;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.Keys;
import mizzen.events.mouse.MouseEventsForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * A Slider: keys and the mouse move the knob, kept between least and most; every move by the user tells the listeners,
 * setValue doesn't. It draws the line, the knob where the value is, and the number to the right.
 */
public class SliderTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();
    private final Slider grey = new Slider(0, 255, 11);          // 11 wide: each column is 25.5
    private final List<Integer> heard = new ArrayList<Integer>();

    @Override
    protected void setUp() {
        grey.onChange((te) -> heard.add(grey.getValue()));
        root.add(grey);
        root.setTheme(Theme.ROUNDED);
        bus.setRoot(root);
        bus.deliver(RequestFocusEvent.fromSource(grey));
    }

    private void key(mizzen.events.keyboard.KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    public void testKeysMoveItAndStopAtTheEnds() {
        key(Keys.RIGHT_KEYSTROKE);
        key(Keys.RIGHT_KEYSTROKE);
        key(Keys.LEFT_KEYSTROKE);
        key(Keys.PGUP_KEYSTROKE);                                  // a tenth of 255: 25
        key(Keys.END_KEYSTROKE);
        key(Keys.RIGHT_KEYSTROKE);                                 // at the end: stays, nobody told
        key(Keys.PGDN_KEYSTROKE);
        key(Keys.HOME_KEYSTROKE);
        key(Keys.LEFT_KEYSTROKE);
        assertEquals("[1, 2, 1, 26, 255, 230, 0]", heard.toString());
    }

    public void testSetValueIsKeptInRangeAndTellsNoOne() {
        grey.setValue(300);
        assertEquals(255, (int) grey.getValue());
        grey.setValue(-5);
        assertEquals(0, (int) grey.getValue());
        grey.setValue(null);
        assertEquals(0, (int) grey.getValue());
        assertEquals("[]", heard.toString());
    }

    public void testAClickAndADragMoveTheKnobThere() {
        grey.onClick(MouseEventsForTesting.click(0, 10, bus));    // the end of the line
        bus.drain();
        assertEquals(255, (int) grey.getValue());
        grey.onClick(MouseEventsForTesting.drag(0, 5, bus));      // the middle
        bus.drain();
        assertEquals(128, (int) grey.getValue());
        grey.onClick(MouseEventsForTesting.click(0, 13, bus));    // on the number: nothing
        bus.drain();
        assertEquals("[255, 128]", heard.toString());
    }

    public void testItDrawsTheLineTheKnobAndTheNumber() {
        grey.setValue(128);
        assertEquals("─────●─────  128", drawn());
        grey.setValue(0);
        assertEquals("●──────────    0", drawn());
        assertEquals("as wide as it draws", 16, grey.getWidth());
    }

    public void testFocusedTheKnobIsInTheAccentColour() {
        TextInput other = new TextInput(5);
        root.add(2, 0, other);
        String focused = knobColour();
        bus.deliver(RequestFocusEvent.fromSource(other));
        String unfocused = knobColour();
        assertFalse("focus gone: the knob changes back", focused.equals(unfocused));
        assertTrue("focused: the accent's foreground (" + focused + ")",
                   focused.contains(Theme.ROUNDED.accentColor().getForeGroundColor().asString().replace("\u001b[", "").replace("m", "")));
    }

    public void testMostMustBeAboveLeast() {
        try {
            new Slider(5, 5);
            fail("expected an IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("most (5)"));
        }
    }

    private String drawn() {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < grey.getWidth(); i++) {
            row.append(' ');
        }
        for (ScreenLineSegment s : grey.getSegmentsBuffer().getSegments()) {
            row.replace(s.getCol(), s.getCol() + s.getText().length(), s.getText());
        }
        return row.toString();
    }

    private String knobColour() {
        for (ScreenLineSegment s : grey.getSegmentsBuffer().getSegments()) {
            if (s.getText().equals(String.valueOf(Slider.KNOB))) {
                return s.getColors().asString();
            }
        }
        throw new AssertionError("no knob drawn");
    }
}
