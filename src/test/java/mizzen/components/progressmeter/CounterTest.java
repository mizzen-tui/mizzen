package mizzen.components.progressmeter;

import mizzen.components.composite.VerticalPanel;
import mizzen.components.theme.Theme;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * A count with a short bar: no total, no bar; past the total, full; never full before the total, never empty once
 * started; in its theme's colours; as text too.
 */
public class CounterTest extends TestCase {

    // what it shows, characters only
    private static String shown(Counter counter) {
        StringBuilder b = new StringBuilder();
        for (ScreenLineSegment segment : counter.getSegmentsBuffer().getSegments()) {
            b.append(segment.getText());
        }
        return b.toString();
    }

    public void testCountLabelAndBar() {
        Counter counter = new Counter(10).label("passed").set(18, 30);
        assertEquals("18 passed ⣿⣿⣿⣿⣿⣿⣀⣀⣀⣀", shown(counter));
        assertEquals("as wide as it shows", shown(counter).length(), counter.getWidth());
    }

    public void testNoTotalNoBar() {
        assertEquals("7 passed", shown(new Counter(10).label("passed").set(7)));
        assertEquals("7 passed", shown(new Counter(10).label("passed").set(7, 0)));
        assertEquals("7", shown(new Counter(10).set(7)));
    }

    public void testPastTheTotalFullAndTheCountTrue() {
        assertEquals("31 passed ⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿", shown(new Counter(10).label("passed").set(31, 30)));
    }

    public void testNeverFullBeforeTheTotalNeverEmptyOnceStarted() {
        assertEquals(9, Counter.filledCells(28, 29, 10));
        assertEquals(10, Counter.filledCells(29, 29, 10));
        assertEquals(1, Counter.filledCells(1, 100, 10));
        assertEquals(0, Counter.filledCells(0, 100, 10));
        assertEquals(-1, Counter.filledCells(5, 0, 10));
    }

    public void testOtherGlyphs() {
        assertEquals("3 #####-----", shown(new Counter(10).glyphs('#', '-').set(3, 6)));
    }

    public void testWidthFollowsTheCount() {
        Counter counter = new Counter(4).set(9, 100);
        int before = counter.getWidth();
        counter.set(100, 100);
        assertEquals(before + 2, counter.getWidth());
    }

    public void testThemeColours() {
        VerticalPanel panel = new VerticalPanel();
        Counter counter = new Counter(4).set(2, 4);
        panel.add(counter);
        panel.setTheme(Theme.DARK);
        boolean filled = false, empty = false;
        for (ScreenLineSegment segment : counter.getSegmentsBuffer().getSegments()) {
            filled |= segment.getText().startsWith("⣿") && segment.getColors() == Theme.DARK.headingColor();
            empty |= segment.getText().startsWith("⣀") && segment.getColors() == Theme.DARK.dimColor();
        }
        assertTrue("filled in the heading colour", filled);
        assertTrue("empty dim", empty);
    }

    public void testAsText() {
        assertEquals("18 passed ⣿⣿⣿⣿⣿⣿⣀⣀⣀⣀", Counter.text(18, 30, 10, "passed"));
        assertEquals("7 passed", Counter.text(7, 0, 10, "passed"));
        String coloured = Counter.text(Theme.DARK, 18, 30, 10, "passed");
        assertTrue(coloured.contains(Theme.DARK.headingColor().asString() + "⣿"));
    }

    public void testAtLeastTwoCells() {
        try {
            new Counter(1);
            fail("one cell can't be both not done and not empty");
        } catch (IllegalArgumentException expected) {
        }
    }
}
