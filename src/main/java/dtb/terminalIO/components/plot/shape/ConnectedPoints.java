package dtb.terminalIO.components.plot.shape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.PointPlotter.PointPlot;
import dtb.terminalIO.components.plot.PointPlotterAsRectangle;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.shape.Shape.AbstractShape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.AsRectangle2D;
import dtb.terminalIO.geom.Rectangle2D;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;
import dtb.terminalIO.textsegments.CleanString;

/**
 * Given a set of points (defined in {@link PointPlot} objects),
 * This draws a shape that is a set of connected lines.
 * 
 * <p/>
 * 
 * Makes heavy use of the {@link Line} shape.
 * 
 */
public class ConnectedPoints<T extends ConnectedPoints<?>> extends AbstractShape<T> implements Fillable<T> , AsRectangle2D.AsFloat2D  {

    List<PointPlot> points = new ArrayList<PointPlot>();
    boolean filled = false;
    
    // when true, connect last point with first point
    boolean closedShape = true;

    public ConnectedPoints() {
    }

    public ConnectedPoints(boolean closedShape) {
        this.closedShape = closedShape;
    }


    public T addPoint(int x, int y) {
        points.add(new PointPlot(x,y));
        return (T) this;
    }

    public T addPoint(float x, float y) {
        points.add(new PointPlot(x,y));
        return (T) this;
    }

    public T addPoint(float x, float y, Color color) {
        points.add(new PointPlot(x,y,color));
        return (T) this;
    }

    public T addPoint(PointPlot point) {
        points.add(point);
        return (T)this;
    }

    public T addPoint(Vec2<?> point) {
        points.add(new PointPlot(point));
        return (T) this;
    }

    public T addPoint(Vec2<?> point, Color color) {
        points.add(new PointPlot(point, color));
        return (T) this;
    }

    @Override
    protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform) {
        if (filled) {
            drawfilled(pointPlotter, positionTransform);
        } else {
            drawOutlineWithTransform(pointPlotter, positionTransform);
        }
    }

    protected void drawOutlineWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform) {
        PointPlot pointPrev = null;
        for (PointPlot point : points) {
            if (pointPrev != null) {
                Line line = new Line(pointPrev, point, pointPrev.color);
                line.setPositionTransform(positionTransform);
                line.draw(pointPlotter);
            }
            pointPrev = point;
        }
        if (points.size()>1 && closedShape) {
            Line line = new Line(points.get(0), pointPrev, pointPrev.color);
            line.setPositionTransform(positionTransform);
            line.draw(pointPlotter);
        }

    }

    protected void drawfilled(PointPlotter pointPlotter, PositionTransform positionTransform) {

        //TreeSet<PointPlot> sortedPoints = new TreeSet<PointPlot>(Vec2.UTIL.VEC2_COMPARE);

        TreeSet<PointPlot> sortedPoints = new TreeSet<PointPlot>(new Vec2_ScanLineComparator());
        //

        PointPlotter plotToColumnEndPointsArray = new PointPlotter() {

            @Override
            public void plot(float x, float y) {
                sortedPoints.add( new PointPlot(x, y) );
            }

            @Override
            public Color setNextPlotColor(Color color) {
                return color;
            }

            @Override
            public void clearPlots() {
                sortedPoints.clear();
            }

        };

        drawOutlineWithTransform(plotToColumnEndPointsArray, positionTransform);

        // outline has now been drawn in memory - lets do a "scan-line" fill into
        // the actual destination (real) plotter

        float currentY = Float.MIN_VALUE;
        boolean drawToggle = false;
        PointPlot lastPoint = null;
        int linePointCount = 0;
        int pointCount = 0;

        for ( PointPlot point :  sortedPoints ) { 
            pointCount++;
            
            
            boolean newLine = Math.round(point.y) != Math.round(currentY);
            if (newLine) {
                linePointCount = 0;
            }
            drawToggle = (linePointCount % 2) == 1; // only draw on odd numbers
            if (drawToggle) {
                float x2 = point.x;
                // draw the line..
                for (float x = lastPoint.x; x<= x2; x++) {
                    pointPlotter.plot(x, currentY);
                }
            }
            if (point.y > 40) {
                linePointCount = linePointCount + 1;
            } else {
                linePointCount++;
            }
            lastPoint = point;
            currentY = Math.round(point.y);
        }


        //Color preColor = pointPlotter.setNextPlotColor(Colors.BRed);
        drawOutlineWithTransform(pointPlotter,  positionTransform);
        
        for (PointPlot point : points) {
            Color prevColor = pointPlotter.setNextPlotColor(Colors.IYel);
            ((BrailleGrid)pointPlotter).plot(new Position.Simple(positionTransform.transform(point)));
            ((BrailleGrid)pointPlotter).plotPosition(new Position.Simple(point.y,point.x+2), new CleanString.Simple("(x="+point.x+",y="+point.y+")"));
            pointPlotter.setNextPlotColor(prevColor);
        }

        //pointPlotter.setNextPlotColor(preColor);

    }

    private static class Vec2_ScanLineComparator implements Comparator<Vec2<?>> {
        @Override
        public int compare(Vec2<?> vec1, Vec2<?> vec2) {
            if (vec1 == vec2)
                return 0;
            int cmp = Integer.compare(Math.round(vec1.getY()), Math.round(vec2.getY()));
            if (cmp != 0)
                return cmp;
            return Float.compare(vec1.getX(), vec2.getX());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T setFilled(boolean filled) {
        this.filled = filled;
        return (T) this;
    }

    @Override
    public Rectangle2D<Float> getRectangle2D() {
        PointPlotterAsRectangle calculatedRectanglePlotter = new PointPlotterAsRectangle();

        for (PointPlot point : points) {
            calculatedRectanglePlotter.plot(positionTransform.transform(point));
        }
        return calculatedRectanglePlotter.getRectangle2D();
    }

}