package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter.PointPlot;
import dtb.terminalIO.components.plot.PointPlotter.PointPlotterList;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;
import junit.framework.TestCase;

/**
 * An ellipse is drawn its size, wide or tall, follows a rotation, and can be filled.
 */
public class EllipseTest extends TestCase {

    /** the box around what was plotted: { minX, maxX, minY, maxY } */
    private static int[] box(PointPlotterList plots) {
        int[] b = { Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE };
        for (PointPlot p : plots.getPlots()) {
            b[0] = Math.min(b[0], p.getCol());
            b[1] = Math.max(b[1], p.getCol());
            b[2] = Math.min(b[2], p.getRow());
            b[3] = Math.max(b[3], p.getRow());
        }
        return b;
    }

    private static boolean plottedAt(PointPlotterList plots, int x, int y) {
        for (PointPlot p : plots.getPlots()) {
            if (p.getCol() == x && p.getRow() == y) {
                return true;
            }
        }
        return false;
    }

    public void testATallEllipseIsItsSize() {
        PointPlotterList plots = new PointPlotterList();
        new Ellipse(50, 50, 10, 30).draw(plots);
        int[] b = box(plots);
        assertEquals("width", 10, b[1] - b[0], 1);
        assertEquals("height", 30, b[3] - b[2], 1);
        assertFalse("outline only: the centre is empty", plottedAt(plots, 50, 50));
    }

    public void testItRotates() {
        PointPlotterList plots = new PointPlotterList();
        Ellipse wide = new Ellipse(50, 50, 30, 10);
        wide.setPositionTransform(new PositionTransform.RotateAroundFixedPoint(90.0, new Vec2.BaseVec2(50, 50)));
        wide.draw(plots);
        int[] b = box(plots);
        assertEquals("turned a quarter: now tall", 10, b[1] - b[0], 1);
        assertEquals(30, b[3] - b[2], 1);
    }

    public void testItCanBeFilled() {
        PointPlotterList plots = new PointPlotterList();
        new Ellipse(50, 50, 10, 30).setFilled(true).draw(plots);
        assertTrue("filled: the centre too", plottedAt(plots, 50, 50));
        assertFalse("but not outside", plottedAt(plots, 58, 50));
    }
}
