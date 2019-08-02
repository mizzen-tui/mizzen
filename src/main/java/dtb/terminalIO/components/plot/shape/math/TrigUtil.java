package dtb.terminalIO.components.plot.shape.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dtb.terminalIO.components.plot.PointPlotter.PointPlot;

public class TrigUtil {

    
    static final int MIN_ANGLE = 0;
    static final int MAX_ANGLE = 360;
    
    
    public static PointPlot circlePointAtAngle(int degrees, PointPlot mid, int radius) {
        double angle = NumbersUtil.degreesToRadians(degrees);
        double r = (double) radius;
        int x = mid.x + ((int) Math.round( r * Math.cos( angle ) ) );
        int y = mid.y + ((int) Math.round( r * Math.sin( angle) ) );
        return new PointPlot(x, y);
    }
    
    public static int circleAngleAtPoint(PointPlot point, PointPlot mid) {
        double adj = (double) point.x - (double) mid.x;
        double opp = (double) mid.y   - (double) point.y;
        if (opp != 0) {
            double angle = Math.atan2(opp, adj);
            return NumbersUtil.radiansToDegrees(angle);
        } else if (adj >= 0) {
            return 0;
        } else {
            return 180;
        }
        
    }

    public static int circleAngleAtPoint(int pointX, int pointY, int midX, int midY) {
        double adj = (double) pointX - (double) midX;
        double opp = (double) midY   - (double) pointY;
        if (opp != 0) {
            double angle = Math.atan2(opp, adj);
            return NumbersUtil.radiansToDegrees(angle);
        } else if (adj >= 0) {
            return 0;
        } else {
            return 180;
        }
        
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
        
        int maxX = Integer.MIN_VALUE;
        int minX = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        
        for (PointPlot sp : points) {
            if (sp.isSamePosition(p)) {
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

        Map<Integer,List<Integer>> byY = new HashMap<Integer,List<Integer>>();
        for (PointPlot sp : points) {
            if (byY.get(sp.y)==null) {
                byY.put(sp.y, new ArrayList<Integer>());
            }
            byY.get(sp.y).add(sp.x);
        }
        
        for (Integer y : byY.keySet()) {
            Collections.sort(byY.get(y));
        }
        
        
        
        
        return false;
    }
    
    /*
    
    */
}
