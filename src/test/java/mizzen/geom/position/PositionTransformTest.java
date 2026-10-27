package mizzen.geom.position;

import mizzen.components.plot.PointPlotter.PointPlot;
import mizzen.components.plot.shape.math.TrigUtil;
import mizzen.geom.position.PositionTransform.Rotate;
import junit.framework.TestCase;

public class PositionTransformTest extends TestCase {

    public void testRotateTransform() {
        Position p = new Position.Simple(0, 1);

        Rotate rotator00 = new Rotate(0);
        Rotate rotator90 = new Rotate(90);
        Rotate rotator180 = new Rotate(180);
        Rotate rotator270 = new Rotate(270);

        System.out.println( "  0 - " + Position.UTIL.asCoordinateString(rotator00.transform(p)));
        System.out.println( " 90 - " + Position.UTIL.asCoordinateString(rotator90.transform(p)));
        System.out.println( "180 - " + Position.UTIL.asCoordinateString(rotator180.transform(p)));
        System.out.println( "270 - " + Position.UTIL.asCoordinateString(rotator270.transform(p)));

    }

    void _testCircleAngleAtPointMatchesCirclePointAtAngle_helper(double testAngle) {
        PointPlot origin = new PointPlot(0,0);
        PointPlot ptAtZeroRadius1 = new PointPlot(1,0);
        PointPlot point_atAngle = TrigUtil.circlePointAtAngle(testAngle, origin, 1);
        double angle_pt_atAngle = TrigUtil.circleAngleAtPoint(point_atAngle, origin);
        Vec2<?> pRotated = new Rotate(testAngle).transform(ptAtZeroRadius1);
        System.out.println(String.format("%8.2f - %25s %8.2f  %25s",testAngle,Position.UTIL.asCoordinateString(point_atAngle),angle_pt_atAngle,Position.UTIL.asCoordinateString(pRotated)));
    }
    
    public void testCircleAngleAtPointMatchesCirclePointAtAngle() {
        _testCircleAngleAtPointMatchesCirclePointAtAngle_helper(0);
        _testCircleAngleAtPointMatchesCirclePointAtAngle_helper(90);
        _testCircleAngleAtPointMatchesCirclePointAtAngle_helper(180);
        _testCircleAngleAtPointMatchesCirclePointAtAngle_helper(270);
    }

}
