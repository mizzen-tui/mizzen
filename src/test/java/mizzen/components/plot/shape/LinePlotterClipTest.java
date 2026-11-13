package mizzen.components.plot.shape;

import java.util.HashSet;
import java.util.Set;

import mizzen.components.plot.PointPlotter;
import mizzen.geom.Rectangle;
import junit.framework.TestCase;

/**
 * Given a plotter with a clip, a line takes only the steps inside it: the same dots as without the clip, and a line to
 * a point billions of dots off finishes at once (it once froze the event thread).
 */
public class LinePlotterClipTest extends TestCase {

    private static final Rectangle GRID = new Rectangle.Simple(0, 0, 48, 200);      // rows 0..47, columns 0..199

    // the dots that land on the grid, as the plot's own plotter rounds them; clipped or not
    private static Set<String> drawn(float x1, float y1, float x2, float y2, boolean clipped) {
        Set<String> dots = new HashSet<String>();
        PointPlotter plotter = new PointPlotter.PointPlotterList() {
            @Override
            public void plot(float x, float y) {
                int col = Math.round(x), row = Math.round(y);
                if (col >= 0 && col < 200 && row >= 0 && row < 48) {
                    dots.add(row + "," + col);
                }
            }

            @Override
            public Rectangle clip() {
                return clipped ? GRID : null;
            }
        };
        new LinePlotter().draw(x1, y1, x2, y2, plotter);
        return dots;
    }

    public void testTheSameDotsAsWithoutTheClip() {
        float[][] lines = { { -50, 10, 250, 30 }, { 10, -20, 30, 90 }, { 5, 5, 60, 40 }, { 199.6f, -3.3f, -7.1f, 51.4f }, { 3, 3, 3, 3 } };
        for (float[] l : lines) {
            assertEquals(l[0] + "," + l[1] + " to " + l[2] + "," + l[3],
                         drawn(l[0], l[1], l[2], l[3], false), drawn(l[0], l[1], l[2], l[3], true));
        }
    }

    public void testWhollyOutsideDrawsNothing() {
        assertTrue(drawn(-500, -500, -10, -400, true).isEmpty());
    }

    /** the console's case: a line from the grid to a row 170 billion dots up */
    public void testAFarOffEndFinishesAndDrawsItsPartOnTheGrid() {
        long start = System.nanoTime();
        Set<String> dots = drawn(100, -1.7e11f, 103, 40, true);
        assertTrue("took " + (System.nanoTime() - start) / 1000000 + "ms", System.nanoTime() - start < 2000000000L);
        assertFalse("its part on the grid is drawn", dots.isEmpty());
        assertTrue(dots.toString(), dots.contains("40,103"));
    }

    public void testPartInside() {
        double[] t = LinePlotter.partInside(0, 0, 10, 0, new Rectangle.Simple(0, 0, 1, 4));
        assertEquals(0.0, t[0], 1e-9);
        assertEquals(0.4, t[1], 1e-9);                   // to column 4: the clip's 0..3 and one to spare
        assertNull(LinePlotter.partInside(0, 10, 10, 10, new Rectangle.Simple(0, 0, 1, 4)));
    }

    /** a filled shape far off the grid: only its rows on the grid are scanned */
    public void testAFarOffFillFinishes() {
        ConnectedPoints shape = new ConnectedPoints(true);
        shape.addPoint(10, 10);
        shape.addPoint(20, -3e10f);
        shape.addPoint(30, 10);
        shape.setFilled(true);
        Set<String> dots = new HashSet<String>();
        long start = System.nanoTime();
        shape.draw(new PointPlotter.PointPlotterList() {
            @Override
            public void plot(float x, float y) {
                dots.add(Math.round(y) + "," + Math.round(x));
            }

            @Override
            public Rectangle clip() {
                return GRID;
            }
        });
        assertTrue("took " + (System.nanoTime() - start) / 1000000 + "ms", System.nanoTime() - start < 2000000000L);
        assertTrue(dots.toString(), dots.contains("5,20"));
    }
}
