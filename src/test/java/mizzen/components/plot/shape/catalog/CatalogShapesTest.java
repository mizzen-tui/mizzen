package mizzen.components.plot.shape.catalog;

import java.util.HashSet;
import java.util.Set;

import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.shape.Shape;
import mizzen.controlcodes.color.Color;
import mizzen.geom.Rectangle2D;
import mizzen.geom.position.PositionTransform;
import junit.framework.TestCase;

/**
 * The catalog shapes: drawn to their settings, built again when a setting changes, holes left
 * empty when filled, the right way up either way y goes, and moved by their transform like any
 * shape.
 */
public class CatalogShapesTest extends TestCase {

    /** the dots a shape plots, rounded */
    static class Dots implements PointPlotter {
        final Set<String> plotted = new HashSet<String>();

        @Override
        public void plot(float x, float y) {
            plotted.add(Math.round(x) + "," + Math.round(y));
        }

        @Override
        public Color setNextPlotColor(Color color) {
            return color;
        }

        @Override
        public void clearPlots() {
            plotted.clear();
        }

        boolean has(int x, int y) {
            return plotted.contains(x + "," + y);
        }
    }

    static Dots dots(Shape<?> shape) {
        Dots dots = new Dots();
        shape.draw(dots);
        return dots;
    }

    public void testAStarIsAsBigAsItsSettingsWithItsFirstPointUp() {
        Rectangle2D box = new Star(50, 50, 10, 4).bounds();
        assertEquals("the top point, straight up", 40f, box.getY1(), 0.01);
        assertEquals("two points' tips apart", 2 * 10 * Math.sin(Math.toRadians(72)), box.getX2() - box.getX1(), 0.01);
    }

    public void testChangingASettingBuildsTheShapeAgain() {
        Gear gear = new Gear(0, 0, 12, 36);
        float before = gear.bounds().getWidth();
        gear.setOuterRadius(18).setRootRadius(14).setHoleRadius(5);
        assertTrue("half the size now: " + gear.bounds().getWidth(), gear.bounds().getWidth() < before * 0.6);
        assertEquals(8, gear.setTeeth(8).getTeeth());
    }

    public void testAFilledRingLeavesItsHoleEmpty() {
        Dots ring = dots(new Ring(0, 0, 20, 8).setFilled(true));
        assertFalse("the hole", ring.has(0, 0));
        assertTrue("the band", ring.has(14, 0));
        assertFalse("outside", ring.has(25, 0));
    }

    public void testAFilledGearAndHexNutLeaveTheirHolesEmpty() {
        assertFalse(dots(new Gear(0, 0, 12, 36).setFilled(true)).has(0, 0));
        Dots nut = dots(new RegularPolygon(0, 0, 30, 6).setHoleRadius(10).setFilled(true));
        assertFalse(nut.has(0, 0));
        assertTrue(nut.has(0, 20));
    }

    public void testYUpTurnsAShapeTheRightWayUpForAPlot() {
        Heart onScreen = new Heart(0, 0, 32);
        assertEquals("y down: the tip is at the bottom (big y)", 17f, onScreen.bounds().getY2(), 0.01);
        Heart inAPlot = new Heart(0, 0, 32).setYUp(true);
        assertEquals("y up: the tip is at the bottom (small y)", -17f, inAPlot.bounds().getY1(), 0.01);
    }

    public void testATransformMovesItLikeAnyShape() {
        House house = new House(30, 32, 60, 56);
        Rectangle2D at = house.bounds();
        house.setPositionTransform(new PositionTransform.Translate(10f, 100f));   // 10 down, 100 across
        assertEquals(at.getX1() + 100, house.bounds().getX1(), 0.01);
        assertEquals(at.getY1() + 10, house.bounds().getY1(), 0.01);
    }

    public void testEveryShapeDraws() {
        Shape<?>[] all = { new Star(40, 40, 36, 15), new RegularPolygon(40, 40, 30, 6), new Ring(40, 40, 30, 12),
                           new Gear(40, 40, 12, 36), new Spring(40, 20, 70, 16, 6), new Heart(40, 40, 70),
                           new House(30, 32, 60, 56), new Smiley(30, 30, 28), new Spirograph(40, 40, 20, 30, 10) };
        for (Shape<?> shape : all) {
            assertTrue(shape.getName(), dots(shape).plotted.size() > 20);
        }
    }

    public void testSettingsThatMakeNoShapeFailClearly() {
        try {
            new Star(0, 0, 10, 4).setPoints(2);
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("3 points"));
        }
        try {
            new Gear(0, 0, 2, 10);
            fail();
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("teeth"));
        }
    }
}
