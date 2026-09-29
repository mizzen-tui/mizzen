package dtb.terminalIO.components.plot.shape;

import java.util.Objects;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.shape.CircleAlgorithm.CirclePlotCondition;
import dtb.terminalIO.components.plot.shape.Ellipse.DrawEllipseAlgorithm;

/**
 * Doesn't work yet.  sigh ..
 *
 */
public class EllipseAlgorithm implements DrawEllipseAlgorithm {

    
    @FunctionalInterface
    static interface EllipsePlotCondition {
        boolean test(int x, int y, double radius, double distance, int midX, int midY);
        
        default CirclePlotCondition and(CirclePlotCondition other) {
            Objects.requireNonNull(other);
            return (int x, int y, double radius, double distance, int midX, int midY) -> {
                return test(x, y, radius, distance, midX, midY) && 
                       other.test(x, y, radius, distance, midX, midY);
                };
        }        
    }

    public void draw(float x, float y, float width, float height, PointPlotter plot) {
        double a = width / 2d;
        double b = height / 2d;
        double c = Math.sqrt(Math.abs(Math.pow(a, 2) - Math.pow(b, 2)));
        double f1x = x - c;
        double f1y = y;
        double f2x = x + c;
        double f2y = y;
        for (int x1 = (int) (x - a); x1 <= x + a; x1++) {
            for (int y1 = (int) (y - b); y1 <= y + b; y1++) {
                double d1 = Math.sqrt(Math.pow(f1x - x1, 2) + Math.pow(f1y - y1, 2));
                double d2 = Math.sqrt(Math.pow(f2x - x1, 2) + Math.pow(f2y - y1, 2));
                if (compare(d1 + d2, 2 * a, 0.5d) == 0)
                    plot.plot(x1, y1);
            }
        }

    }

    protected int compare(double d1, double d2, double precision) {
        double diff = d1 - d2;
        if (Math.abs(diff) < precision)
            return 0;
        if (diff < 0d)
            return -1;
        else
            return 1;
    }

}
