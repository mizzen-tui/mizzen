package dtb.terminalIO.components.plot;

import dtb.terminalIO.components.plot.grid.GridContext.GRID_TYPE;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * PlotArea.panByFraction moves by a share of the view, and navigationControls() shows scaling, zoom
 * and pan by default, each of which can be left out and brought back.
 */
public class PlotNavigationTest extends TestCase {

    private static PlotArea plot() {
        return new PlotArea(Colors.Gre, new Dimension.Simple(11, 11), GRID_TYPE.SIMPLE_GRID_CTX)
                .showing(0, 10, 0, 10);
    }

    /** the controls' row as drawn, colours left out */
    private static String text(PlotNavigation controls) {
        StringBuilder text = new StringBuilder();
        for (ScreenLineSegment s : controls.getSegmentsBuffer().getSegments()) {
            text.append(s.getText());
        }
        return text.toString();
    }

    public void testPanByFractionMovesAShareOfTheView() {
        PlotArea plot = plot().panByFraction(0.1, -0.5);
        assertEquals(1, plot.getXMin(), 1e-9);
        assertEquals(11, plot.getXMax(), 1e-9);
        assertEquals(-5, plot.getYMin(), 1e-9);
        assertEquals(5, plot.getYMax(), 1e-9);
    }

    public void testPanByFractionFollowsTheZoom() {
        PlotArea plot = plot().zoom(2);                          // shows 2.5..7.5: five units across
        plot.panByFraction(0.1, 0);
        assertEquals("a tenth of five", 3.0, plot.getXMin(), 1e-9);
    }

    public void testAllPartsShowByDefault() {
        String row = text(plot().navigationControls());
        assertTrue(row, row.contains("stretch"));
        assertTrue(row, row.contains("zoom"));
        assertTrue(row, row.contains("pan"));
    }

    public void testAPartCanBeLeftOutAndBroughtBack() {
        PlotNavigation controls = plot().navigationControls();
        int full = controls.getWidth();

        controls.showScaling(false);
        String row = text(controls);
        assertFalse(row, row.contains("stretch"));
        assertTrue(row, row.contains("zoom") && row.contains("pan"));
        assertTrue("narrower", controls.getWidth() < full);

        controls.showScaling(true);
        assertEquals("as before", full, controls.getWidth());
    }

    public void testScalingShowsThePlotsOwn() {
        PlotArea plot = plot().scaling(PlotArea.Scaling.EQUAL);
        String row = text(plot.navigationControls());
        assertTrue(row, row.contains("(•) equal"));
    }
}
