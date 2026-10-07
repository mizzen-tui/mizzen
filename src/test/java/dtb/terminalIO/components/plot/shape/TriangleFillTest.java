package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter.PointPlotterList;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;
import junit.framework.TestCase;

/**
 * A filled triangle draws at any rotation or scale - its corners then have fractional coordinates
 * (the old fill kept a table of rows sized from the rounded height, and the outline could land a
 * row past its end).
 */
public class TriangleFillTest extends TestCase {

    public void testAFilledTriangleDrawsAtEveryRotationAndScale() {
        Triangle triangle = new Triangle(0, 0, 30, 100, 65, 50);         // the one in TestScale
        triangle.setFilled(true);
        Vec2<?> centre = new Vec2.BaseVec2(32.5f, 50f);
        for (double scale : new double[] { 0.37, 1.0, 1.7 }) {
            for (int degrees = 0; degrees < 360; degrees++) {
                PointPlotterList plots = new PointPlotterList();
                triangle.setPositionTransform(new PositionTransform.FofG(new PositionTransform.RotateAroundFixedPoint(degrees, centre),
                                                                         new PositionTransform.Scale(scale)));
                triangle.draw(plots);
                assertTrue("drew nothing at " + degrees + " degrees, scale " + scale, plots.size() > 0);
            }
        }
    }
}
