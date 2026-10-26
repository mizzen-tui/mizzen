package dtb.terminalIO.components.scroll;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.MultiLineTextInput;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * A scrollable's bottom line short of room: scroll buttons are dropped, least useful first (« »,
 * then PgUp PgDn, then ^ v) so other footer parts - e.g. an open search box - stay whole; they come
 * back with room; past that, the line is clipped keeping its right end.
 */
public class ScrollableFooterRoomTest extends TestCase {

    @Override
    protected void setUp() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;       // nothing may be drawn outside its component
    }

    @Override
    protected void tearDown() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = false;
    }

    private static ScrollableComponent pane(int width) {
        return new ScrollableComponent(Colors.Yel, new Dimension.Simple(8, width), new MultiLineTextInput(Colors.IYel));
    }

    private static String bottomRow(ScrollableComponent pane) {
        int bottom = pane.getHeight() - 1;
        StringBuilder row = new StringBuilder();
        for (ScreenLineSegment s : pane.getSegmentsBuffer().getSegments()) {
            if (s.getRow() == bottom) {
                row.append(s.getText());
            }
        }
        return row.toString();
    }

    /** like opening search on ListBoxTest2's 55 wide list: the box stays whole, the least useful buttons go */
    public void testAWideFooterPartStaysWholeAndTheLeastUsefulButtonsGo() {
        ScrollableComponent pane = pane(55);
        pane.addFooterComponent(new TextComponent("Find [search text here]"));
        String bottom = bottomRow(pane);
        assertTrue(bottom, bottom.contains("Find [search text here]"));
        assertFalse("« » went first: " + bottom, bottom.contains("«"));
        assertTrue("^ v kept: " + bottom, bottom.contains("^"));
    }

    public void testTheButtonsComeBackWithRoom() {
        ScrollableComponent pane = pane(55);
        pane.addFooterComponent(new TextComponent("Find [search text here]"));
        pane.resize(new Dimension.Simple(8, 120));
        String bottom = bottomRow(pane);
        assertTrue(bottom, bottom.contains("«") && bottom.contains("PgUp") && bottom.contains("Find [search text here]"));
        assertTrue("in their order: " + bottom, bottom.indexOf("^") < bottom.indexOf("PgUp") && bottom.indexOf("PgUp") < bottom.indexOf("«"));
    }

    /** the last buttons (« ») dropped with nothing after them come back at the end of the bar (it threw: CharacterViewerTest) */
    public void testButtonsDroppedFromTheEndComeBackAtTheEnd() {
        ScrollableComponent pane = pane(55);
        pane.setFooterText("a status text long enough to push the scroll buttons off");
        pane.setFooterText("short");                                     // room again
        String bottom = bottomRow(pane);
        assertTrue(bottom, bottom.contains("«") && bottom.contains("»"));
        assertTrue("in their order: " + bottom, bottom.indexOf("^") < bottom.indexOf("«") && bottom.indexOf("«") < bottom.indexOf("»"));
    }

    /**
     * squeezed to 2 rows, its content has no rows: the view and the vertical scrollbar draw nothing
     * (they drew a row over the bottom line - the border and footer came out garbled)
     */
    public void testSqueezedToTwoRowsTheBottomLineStaysWhole() {
        ScrollableComponent pane = new ScrollableComponent(Colors.Yel, new Dimension.Simple(10, 60), new dtb.terminalIO.components.StdoutViewer(Colors.BWhi));
        pane.getSegmentsBuffer();
        pane.resize(new Dimension.Simple(2, 60));
        String bottom = bottomRow(pane);                                // overlapping segments would throw (strict check on)
        assertTrue(bottom, bottom.startsWith("└") && bottom.endsWith("┘") && bottom.contains("PgUp"));
    }

    public void testPastAllThatTheLineIsClippedKeepingItsRightEnd() {
        ScrollableComponent pane = pane(20);
        pane.addFooterComponent(new TextComponent("a long footer part ending in SEARCH"));
        String bottom = bottomRow(pane);                                      // drawn - no exception - inside the pane
        assertTrue(bottom, bottom.contains("SEARCH"));
    }
}
