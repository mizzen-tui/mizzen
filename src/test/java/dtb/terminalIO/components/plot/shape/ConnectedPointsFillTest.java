package dtb.terminalIO.components.plot.shape;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import dtb.terminalIO.components.plot.PointPlotter.PointPlot;
import dtb.terminalIO.components.plot.PointPlotter.PointPlotterList;
import dtb.terminalIO.geom.position.Vec2;
import junit.framework.TestCase;

/**
 * A filled shape fills its inside only - also for a concave shape, whose outline turns back on
 * itself (the old fill, which counted outline dots along a row, spilled out of the notch).
 */
public class ConnectedPointsFillTest extends TestCase {

    private static Set<String> filled(Polygon shape) {
        PointPlotterList plots = new PointPlotterList();
        shape.setFilled(true);
        shape.draw(plots);
        Set<String> at = new HashSet<String>();
        for (PointPlot p : plots.getPlots()) {
            at.add(p.getCol() + "," + p.getRow());
        }
        return at;
    }

    /** an arrowhead pointing right, with a notch on its left: (0,0) (20,10) (0,20) (8,10) */
    public void testAConcaveShapeLeavesItsNotchEmpty() {
        Set<String> at = filled(new Polygon(new int[][] { { 0, 0 }, { 20, 10 }, { 0, 20 }, { 8, 10 } }));
        assertTrue("inside the arrowhead", at.contains("14,10"));
        assertTrue("inside the arrowhead, upper wing", at.contains("6,5"));
        assertFalse("in the notch", at.contains("3,10"));
        assertFalse("in the notch", at.contains("5,10"));
        assertFalse("right of the tip", at.contains("21,10"));
    }

    /** the dinosaur from TestScale: lots of concave corners */
    private static int[][] dino() {
        int[][] dots = new int[][] { { 6, 4 }, { 3, 1 }, { 1, 2 }, { -1, 5 }, { -2, 5 }, { -3, 4 }, { -4, 4 }, { -5, 3 },
                                     { -5, 2 }, { -2, 2 }, { -5, 1 }, { -4, 0 }, { -2, 1 }, { -1, 0 }, { 0, -3 }, { -1, -4 }, { 1, -4 },
                                     { 2, -3 }, { 1, -2 }, { 3, -1 }, { 5, 1 } };
        for (int[] p : dots) {
            p[0] = (p[0] + 5) * 10;
            p[1] = (p[1] + 6) * 10;
        }
        return dots;
    }

    /** checked against an independent inside test (a ray to the right crosses the outline an odd number of times) */
    private static boolean inside(int[][] poly, double x, double y) {
        boolean in = false;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            double xi = poly[i][0], yi = poly[i][1], xj = poly[j][0], yj = poly[j][1];
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                in = !in;
            }
        }
        return in;
    }

    /** distance from a point to the nearest edge of the outline */
    private static double toOutline(int[][] poly, double x, double y) {
        double best = Double.MAX_VALUE;
        for (int i = 0, j = poly.length - 1; i < poly.length; j = i++) {
            double ax = poly[j][0], ay = poly[j][1], bx = poly[i][0], by = poly[i][1];
            double dx = bx - ax, dy = by - ay;
            double t = Math.max(0, Math.min(1, ((x - ax) * dx + (y - ay) * dy) / (dx * dx + dy * dy)));
            best = Math.min(best, Math.hypot(x - (ax + t * dx), y - (ay + t * dy)));
        }
        return best;
    }

    public void testTheDinoFillsItsInsideAndNothingElse() {
        int[][] poly = dino();
        Set<String> at = filled(new Polygon(dino()));
        int spilled = 0;
        int missed = 0;
        for (int x = 0; x <= 120; x++) {
            for (int y = 0; y <= 120; y++) {
                if (toOutline(poly, x, y) <= 1.0) {
                    continue;                                 // on or next to the outline: either is fine
                }
                boolean plotted = at.contains(x + "," + y);
                boolean in = inside(poly, x, y);
                if (plotted && !in) {
                    spilled++;
                }
                if (!plotted && in) {
                    missed++;
                }
            }
        }
        assertEquals("dots filled outside the shape", 0, spilled);
        assertEquals("dots inside the shape left empty", 0, missed);
    }

    /** a ring: a square with a square hole - filled between them, the hole left empty */
    public void testAHoleIsLeftEmpty() {
        Polygon ring = new Polygon(new int[][] { { 0, 0 }, { 40, 0 }, { 40, 40 }, { 0, 40 } });
        ring.addHole(Arrays.<Vec2<?>>asList(new Vec2.BaseVec2(10, 10), new Vec2.BaseVec2(30, 10),
                                            new Vec2.BaseVec2(30, 30), new Vec2.BaseVec2(10, 30)));
        Set<String> at = filled(ring);
        assertTrue("between the outlines", at.contains("5,20"));
        assertTrue("between the outlines", at.contains("35,20"));
        assertTrue("between the outlines", at.contains("20,5"));
        assertTrue("the hole's outline is drawn", at.contains("10,20"));
        assertFalse("in the hole", at.contains("20,20"));
        assertFalse("in the hole", at.contains("15,25"));
    }

    /** a convex shape fills solid: every row between top and bottom has its middle filled */
    public void testAConvexShapeFillsSolid() {
        Set<String> at = filled(new Polygon(new int[][] { { 10, 10 }, { 25, 10 }, { 35, 20 }, { 35, 30 }, { 25, 40 }, { 10, 40 }, { 0, 30 }, { 0, 20 } }));
        for (int y = 10; y <= 40; y++) {
            assertTrue("row " + y + " middle", at.contains("17," + y));
        }
        assertFalse("outside", at.contains("40,25"));
    }
}
