package mizzen.components.plot.shape;

import java.util.Collection;

import mizzen.components.plot.PointPlotter.PointPlot;
import mizzen.geom.position.Vec2;

/**
 * Given a set of points (defined in {@link PointPlot} objects),
 * This draws a shape that is a set of connected lines.
 */
public class Polygon extends ConnectedPoints<Polygon> {

    public Polygon() {
        super(true); // true means last point and first point are connected, causing a closed shape
    }

    public Polygon(int[][] dots) {
        super(true);
        addPoints(dots);
    }

    public Polygon(Collection<Vec2<?>> points) {
        super(true);
        addPoints(points);
    }

    public void addPoints(int[][] dots) {
        for (int[] p : dots) {
            float x = p[0];
            float y = p[1];
            addPoint( x,y );
        }
    }

    public void addPoints(Collection<Vec2<?>> points) {
        for (Vec2<?> p : points) {
            addPoint(p);
        }
    }

}
