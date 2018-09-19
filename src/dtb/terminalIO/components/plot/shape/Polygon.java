package dtb.terminalIO.components.plot.shape;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.PointPlotter.PointPlot;
import dtb.terminalIO.geom.Position;

/**
 * Given a set of points (defined in Position objects),
 * This draws a shape that is a set of connected lines.
 */
public class Polygon implements Shape {
    
    List<PointPlot> points = new ArrayList<PointPlot>();

    
    public void addPoint(PointPlot point) {
	points.add(point);
    }
    
    public void addPoint(Position point) {
	points.add(new PointPlot(point));
    }
    
    public void addPoint(Position point, Color color) {
	points.add(new PointPlot(point, color));
    }
    
    @Override
    public void draw(PointPlotter pointPlotter) {
	PointPlot pointPrev = null;
	for (PointPlot point : points) {
	    if (pointPrev != null) {
		new Line(pointPrev, point, pointPrev.color).draw(pointPlotter);
	    }
	    pointPrev = point;
	}
	if (points.size()>1) {
	    new Line(points.get(0), pointPrev, pointPrev.color).draw(pointPlotter);
	}
	
    }
    

}
