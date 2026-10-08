package dtb.terminalIO.components.plot.shape;

import java.util.ArrayList;
import java.util.Collection;
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
    List<List<PointPlot>> holes = new ArrayList<List<PointPlot>>();     // more outlines, inside the shape: left empty when filled
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

    /**
     * A hole: another outline, inside the shape (always closed). Filled, the shape is filled between
     * its outline and its holes - a ring, a gear's axle hole, the inside of an "O".
     */
    public T addHole(Collection<? extends Vec2<?>> holePoints) {
        List<PointPlot> hole = new ArrayList<PointPlot>();
        for (Vec2<?> p : holePoints) {
            hole.add(new PointPlot(p));
        }
        holes.add(hole);
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
        drawOutline(points, closedShape, pointPlotter, positionTransform);
        for (List<PointPlot> hole : holes) {
            drawOutline(hole, true, pointPlotter, positionTransform);
        }
    }

    private static void drawOutline(List<PointPlot> points, boolean closedShape, PointPlotter pointPlotter, PositionTransform positionTransform) {
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
     * Horizontal edges cross no row (the outline draws them). The edges of the holes count too, so
     * a hole's inside falls between two crossings that are not filled.
     */
    protected void drawfilled(PointPlotter pointPlotter, PositionTransform positionTransform) {
        // every outline's corners, transformed: the shape's own, then its holes
        List<List<PointPlot>> all = new ArrayList<List<PointPlot>>();
        all.add(points);
        all.addAll(holes);

        List<float[][]> outlines = new ArrayList<float[][]>();       // per outline: { xs, ys }
        float yMin = Float.MAX_VALUE;
        float yMax = -Float.MAX_VALUE;
        for (List<PointPlot> outline : all) {
            if (outline.size() < 3) {
                continue;
            }
            float[][] xy = new float[2][outline.size()];
            for (int i = 0; i < outline.size(); i++) {
                Vec2<?> p = positionTransform.transform(outline.get(i));
                xy[0][i] = p.getX();
                xy[1][i] = p.getY();
                yMin = Math.min(yMin, xy[1][i]);
                yMax = Math.max(yMax, xy[1][i]);
            }
            outlines.add(xy);
        }

        List<Float> crossings = new ArrayList<Float>();
        for (int y = (int) Math.ceil(yMin); y <= (int) Math.floor(yMax); y++) {
            crossings.clear();
            for (float[][] xy : outlines) {
                addCrossings(xy[0], xy[1], y, crossings);
            }
            Collections.sort(crossings);
            for (int k = 0; k + 1 < crossings.size(); k += 2) {
                for (int x = Math.round(crossings.get(k)); x <= Math.round(crossings.get(k + 1)); x++) {
                    pointPlotter.plot(x, y);
                }
            }
        }

        drawOutlineWithTransform(pointPlotter, positionTransform);
    }

    /** where the edges of one closed outline (corners xs, ys) cross row y */
    private static void addCrossings(float[] xs, float[] ys, int y, List<Float> crossings) {
        int n = xs.length;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;                      // the edge from corner i to the next (the last closes the outline)
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
    }

    @SuppressWarnings("unchecked")
    @Override
    public T setFilled(boolean filled) {
        this.filled = filled;
        return (T) this;
    }

    @Override
    public Rectangle2D getRectangle2D() {
        PointPlotterAsRectangle calculatedRectanglePlotter = new PointPlotterAsRectangle();

        for (PointPlot point : points) {
            calculatedRectanglePlotter.plot(positionTransform.transform(point));
        }
        return calculatedRectanglePlotter.getRectangle2D();
    }

}