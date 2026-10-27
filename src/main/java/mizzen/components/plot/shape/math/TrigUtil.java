package mizzen.components.plot.shape.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import mizzen.components.plot.PointPlotter.PointPlot;
import mizzen.geom.position.Position;

public class TrigUtil {

    static final int MIN_ANGLE = 0;
    static final int MAX_ANGLE = 360;

    public static PointPlot circlePointAtAngleForTraditionalYAxisOrientation(double degrees, Position mid, float radius) {
        double angle = NumbersUtil.degreesToRadians(degrees);
        double r = (double) radius;
        float x = (float) (mid.getX() +  r * Math.cos(angle));
        float y = (float) (mid.getY() +  r * Math.sin(angle));
        return new PointPlot(x, y);
    }

    public static PointPlot circlePointAtAngleAdjust_for_inverted_Y_axis(int degrees, Position mid, float radius) {
        double angle = NumbersUtil.degreesToRadians(degrees);
        double r = (double) radius;
        float x = (float) (mid.getX() + r * Math.cos(angle));  // X is correct
        float y = (float) (mid.getY() - r * Math.sin(angle));  // Adjust for inverted Y-axis
        return new PointPlot(x, y);
    }

    public static PointPlot circlePointAtAngle(double degrees, Position mid, float radius) {
        return circlePointAtAngleForTraditionalYAxisOrientation(degrees, mid, radius);
    }

    public static double circleAngleAtPointForTraditionalYAxisOrientation(Position point, Position mid) {
        double adj = (double) point.getX() - (double) mid.getX();
        double opp = (double) point.getY() - (double) mid.getY();  // Traditional Y-axis
        if (opp != 0) {
            double angle = Math.atan2(opp, adj);
            return NumbersUtil.radiansToDegrees(angle);
        } else if (adj >= 0) {
            return 0;
        } else {
            return 180;
        }
    }

    public static double circleAngleAtPoint_for_inverted_Y_axis(Position point, Position mid) {
        double adj = (double) point.getX() - (double) mid.getX();
        double opp = (double) mid.getY()   - (double) point.getY();
        if (opp != 0) {
            double angle = Math.atan2(opp, adj);
            return NumbersUtil.radiansToDegrees(angle);
        } else if (adj >= 0) {
            return 0;
        } else {
            return 180;
        }
    }

    public static double circleAngleAtPoint(PointPlot point, Position mid) {
        return circleAngleAtPointForTraditionalYAxisOrientation(point, mid);
    }

    public static double circleAngleAtPoint(Position point, Position mid) {
        return circleAngleAtPoint(new PointPlot(point), new PointPlot(mid));
    }

    public static double circleAngleAtPoint(float pointX, float pointY, float midX, float midY) {
        return circleAngleAtPoint( new PointPlot(pointX, pointY),
                                   new PointPlot(midX, midY)); 
    }

    // FIXME : not even close to complete -- and current implementation tracking to be way too expensive!!!
    //         Re-think this!
    public static boolean isPointInside(PointPlot p, Collection<PointPlot> points) {

        if (points == null) {
            throw new NullPointerException("points null");
        }
        if (p == null) {
            throw new NullPointerException("p null");
        }

        if (points.size() == 0) {
            return false;
        }

        float maxX = Integer.MIN_VALUE;
        float minX = Integer.MAX_VALUE;
        float maxY = Integer.MIN_VALUE;
        float minY = Integer.MAX_VALUE;

        for (PointPlot sp : points) {
            if (sp.isSameLocation(p)) {
                return false; // not inside because 'on' the shape
            }
            maxX = (maxX > sp.x) ? sp.x : maxX;
            minX = (minX < sp.x) ? sp.x : minX;
            maxY = (maxY > sp.y) ? sp.y : maxY;
            minY = (minY < sp.y) ? sp.y : minY;
        }

        if (p.x > maxX || p.x < minX || p.y > maxY || p.y < minY) {
            return false;
        }

        Map<Float,List<Float>> byY = new HashMap<Float,List<Float>>();
        for (PointPlot sp : points) {
            if (byY.get(sp.y)==null) {
                byY.put(sp.y, new ArrayList<Float>());
            }
            byY.get(sp.y).add(sp.x);
        }

        for (Float y : byY.keySet()) {
            Collections.sort(byY.get(y));
        }

        return false;
    }

}
