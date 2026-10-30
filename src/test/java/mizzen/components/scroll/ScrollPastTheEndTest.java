package mizzen.components.scroll;

import mizzen.components.TextComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import junit.framework.TestCase;

/** Paging or stepping down stops with the last line at the bottom of the view - never an empty view. */
public class ScrollPastTheEndTest extends TestCase {

    private ScrollableComponent pane(int lines) {
        StringBuilder text = new StringBuilder("line 0");
        for (int i = 1; i < lines; i++) {
            text.append("\nline ").append(i);
        }
        ScrollableComponent pane = new ScrollableComponent(Colors.IGre, new TextComponent(Colors.Whi, text.toString()));
        pane.resize(new Dimension.Simple(12, 30));
        pane.getSegmentsBuffer();
        return pane;
    }

    private int lastTop(ScrollableComponent pane, int lines) {
        return lines - pane.getViewPortAsRectangle().getHeight();
    }

    public void testPageDownStopsAtTheLastScreenful() {
        ScrollableComponent pane = pane(100);
        for (int i = 0; i < 50; i++) {
            pane.pagedown();
        }
        assertEquals(lastTop(pane, 100), pane.getOffsetIntoSource().getRow());
    }

    public void testDownStopsThereToo() {
        ScrollableComponent pane = pane(100);
        for (int i = 0; i < 200; i++) {
            pane.down(1);
        }
        assertEquals(lastTop(pane, 100), pane.getOffsetIntoSource().getRow());
    }

    public void testAPageLandsOnAPage() {
        ScrollableComponent pane = pane(100);
        pane.pagedown();
        assertEquals(pane.getViewPortAsRectangle().getHeight(), pane.getOffsetIntoSource().getRow());
    }

    public void testShorterThanTheViewNeverMoves() {
        ScrollableComponent pane = pane(3);
        pane.pagedown();
        pane.down(1);
        assertEquals(0, pane.getOffsetIntoSource().getRow());
    }

    public void testUpComesBack() {
        ScrollableComponent pane = pane(100);
        for (int i = 0; i < 50; i++) {
            pane.pagedown();
        }
        pane.pageup();
        assertEquals(lastTop(pane, 100) - pane.getViewPortAsRectangle().getHeight(), pane.getOffsetIntoSource().getRow());
    }
}
