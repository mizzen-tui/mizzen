package dtb.terminalIO.components.plot.shape;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.geom.Position;

/**
 * Given a set of points (defined in Position objects),
 * This draws a shape that is a set of connected lines.
 */
public class Polygon implements Shape {
    
    List<Position> points = new ArrayList<Position>();

    public void addPoint(Position point) {
	points.add(point);
    }
    
    @Override
    public void draw(PointPlotter pointPlotter) {
	Position pointPrev = null;
	for (Position point : points) {
	    if (pointPrev != null) {
		new Line(pointPrev, point).draw(pointPlotter);
	    }
	    pointPrev = point;
	}
	if (points.size()>1) {
	    new Line(points.get(0), pointPrev).draw(pointPlotter);
	}
	
    }
    

}
