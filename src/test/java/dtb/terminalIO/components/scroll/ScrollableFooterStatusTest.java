package dtb.terminalIO.components.scroll;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.input.MultiLineTextInput;
import dtb.terminalIO.components.progressmeter.ProgressMeter;
import dtb.terminalIO.components.progressmeter.ProgressMeter.Layout;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * ScrollableComponent.setFooterStatus: a one-row status first on the bottom line, before the scroll
 * buttons, kept as wide as the line allows as the pane is resized - the horizontal scrollbar keeps
 * its minimum.
 */
public class ScrollableFooterStatusTest extends TestCase {

    private final ProgressMeter meter = new ProgressMeter(10).setLayout(Layout.ONE_ROW);
    private ScrollableComponent pane;

    @Override
    protected void setUp() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;       // nothing may be drawn outside its component
        pane = new ScrollableComponent(Colors.Yel, new Dimension.Simple(10, 80), new MultiLineTextInput(Colors.IYel));
        meter.setMessage("Copying");
        pane.setFooterStatus(meter);
    }

    @Override
    protected void tearDown() {
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = false;
    }

    private String bottomRow() {
        int bottom = pane.getHeight() - 1;
        StringBuilder row = new StringBuilder();
        for (ScreenLineSegment s : pane.getSegmentsBuffer().getSegments()) {
            if (s.getRow() == bottom) {
                row.append(s.getText());
            }
        }
        return row.toString();
    }

    public void testTheStatusComesBeforeTheScrollButtons() {
        String bottom = bottomRow();
        assertTrue(bottom, bottom.indexOf("Copying") >= 0 && bottom.indexOf("Copying") < bottom.indexOf("PgUp"));
    }

    public void testItFollowsThePanesWidthBothWays() {
        int at80 = meter.getWidth();
        pane.resize(new Dimension.Simple(10, 110));
        assertEquals("30 wider", at80 + 30, meter.getWidth());
        pane.resize(new Dimension.Simple(10, 70));
        assertEquals("10 narrower than at 80", at80 - 10, meter.getWidth());
        bottomRow();                                                    // still drawn inside the pane
    }

    public void testTheHorizontalScrollbarKeepsItsMinimum() {
        pane.resize(new Dimension.Simple(10, 90));
        int buttons = pane.footerControls.getWidth() - meter.getWidth();
        assertEquals(90 - 2 - buttons - 10, meter.getWidth());
    }

    /** other footer parts - e.g. the search box - live alongside it: the status gives them room */
    public void testItMakesRoomForOtherFooterPartsSuchAsSearch() {
        int alone = meter.getWidth();
        SearchFunctionForScrollableComponent.addFinderToScrollable(pane.asSearchableView());
        assertTrue("narrower with the search box: " + meter.getWidth(), meter.getWidth() < alone);
        String bottom = bottomRow();                                    // all still drawn, inside the pane
        assertTrue(bottom, bottom.contains("Copying") && bottom.contains("PgUp"));
        int buttonsAndSearch = pane.footerControls.getWidth() - meter.getWidth();
        assertEquals(80 - 2 - buttonsAndSearch - 10, meter.getWidth());
    }

    public void testATwoRowStatusIsRefused() {
        try {
            pane.setFooterStatus(new ProgressMeter(10));                // two rows by default
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("one row"));
        }
    }
}
