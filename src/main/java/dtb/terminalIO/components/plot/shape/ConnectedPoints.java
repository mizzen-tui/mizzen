package dtb.terminalIO.components.plot.shape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.PointPlotter.PointPlot;
import dtb.terminalIO.components.plot.PointPlotterAsRectangle;
import dtb.terminalIO.components.plot.shape.Shape.AbstractShape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.AsRectangle2D;
import dtb.terminalIO.geom.Rectangle2D;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;

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

    public T addPoint(int x, int y, Color color) {
        points.add(new PointPlot(x,y,color));
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

    /**
     * Fills the shape, then draws its outline on top. Scan-line fill over the (transformed) corner
     * points: for each row, where does each edge cross it - fill between the 1st and 2nd crossing,
     * the 3rd and 4th, ... An edge counts on a row if the row is in [its top y, its bottom y) - top
     * end in, bottom end out - so a corner where the outline turns back (a peak or a valley) counts
     * 0 or 2 times and a corner it passes through counts once: concave shapes fill right.
     * Horizontal edges cross no row (the outline draws them).
     */
    protected void drawfilled(PointPlotter pointPlotter, PositionTransform positionTransform) {
        int n = points.size();
        if (n >= 3) {
            float[] xs = new float[n];
            float[] ys = new float[n];
            float yMin = Float.MAX_VALUE;
            float yMax = -Float.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                Vec2<?> p = positionTransform.transform(points.get(i));
                xs[i] = p.getX();
                ys[i] = p.getY();
                yMin = Math.min(yMin, ys[i]);
                yMax = Math.max(yMax, ys[i]);
            }

            List<Float> crossings = new ArrayList<Float>();
            for (int y = (int) Math.ceil(yMin); y <= (int) Math.floor(yMax); y++) {
                crossings.clear();
                for (int i = 0; i < n; i++) {
                    int j = (i + 1) % n;                      // the edge from corner i to the next (the last closes the shape)
                    float y0 = ys[i], y1 = ys[j];
                    float x0 = xs[i], x1 = xs[j];
                    if (y0 == y1) {
                        continue;                             // horizontal
                    }
                    if (y0 > y1) {                            // top end first
                        float t = y0; y0 = y1; y1 = t;
                        t = x0; x0 = x1; x1 = t;
                    }
                    if (y >= y0 && y < y1) {
                        crossings.add(x0 + (y - y0) * (x1 - x0) / (y1 - y0));
                    }
                }
                Collections.sort(crossings);
                for (int k = 0; k + 1 < crossings.size(); k += 2) {
                    for (int x = Math.round(crossings.get(k)); x <= Math.round(crossings.get(k + 1)); x++) {
                        pointPlotter.plot(x, y);
                    }
                }
            }
        }

        drawOutlineWithTransform(pointPlotter, positionTransform);
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