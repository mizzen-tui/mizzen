package dtb.terminalIO.components.plot.shape;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.PointPlotter.PointPlot;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.Position;

/**
 * Given a set of points (defined in {@link PointPlot} objects),
 * This draws a shape that is a set of connected lines.
 * 
 * <p/>
 * 
 * Makes heavy use of the {@link Line} shape.
 * 
 */
public class ConnectedPoints implements Shape {
    
    List<PointPlot> points = new ArrayList<PointPlot>();
    
    // when true, connect last point with first point
    boolean closedShape = true;

    public ConnectedPoints() {
    }

    public ConnectedPoints(boolean closedShape) {
        this.closedShape = closedShape;
    }


    public void addPoint(int x, int y) {
        points.add(new PointPlot(x,y));
    }

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
        if (points.size()>1 && closedShape) {
            new Line(points.get(0), pointPrev, pointPrev.color).draw(pointPlotter);
        }
        
    }
    

}