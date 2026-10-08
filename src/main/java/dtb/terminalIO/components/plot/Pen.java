package dtb.terminalIO.components.plot;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.DoubleUnaryOperator;

import dtb.terminalIO.components.plot.shape.LinePlotter;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;

/**
 * Draws in a PlotArea, in your numbers (x right, y up). Handed to onDraw for one redraw - don't
 * keep it. Every call returns the pen, so calls chain:
 * <pre>
 *     pen.color(Colors.BCya).function(Math::sin)
 *        .color(Colors.Blu).fillUnder(Math::sin, 0, Math.PI)
 *        .draw(new Circle(3, 0, 0.3f))
 *        .text(Math.PI, 1.2, "peak");
 * </pre>
 * Anything more than these is a shape: draw(shape) takes any library shape, in your numbers (it
 * is scaled at its corners, so a zoomed shape has no gaps). Text goes in the character nearest the
 * point and replaces what that character showed.
 * <p/>
 * As on a canvas, save / translate / rotate / scale / restore say where things go - shapes,
 * points, lines and text (function and fillUnder follow the axes):
 * <pre>
 *     pen.save().translate(shipX, shipY).rotate(heading).draw(ship).restore();
 * </pre>
 */
public class Pen {

    private final PlotArea area;
    private final PointPlotter dots;
    private final LinePlotter lines = new LinePlotter();
    private Color color;
    private PositionTransform transform = PositionTransform.NOOP;     // in your numbers: what save / translate / rotate / scale built
    private final Deque<PositionTransform> saved = new ArrayDeque<PositionTransform>();

    Pen(PlotArea area) {
        this.area = area;
        this.dots = area.dotsPlotter(null);
        this.color = area.getColor();
    }

    /** the colour of what is drawn next */
    public Pen color(Color color) {
        this.color = color;
        dots.setNextPlotColor(color);
        return this;
    }

    // ---- where things go: a transform, as on a canvas ----------------------------------------

    /** remembers where things go now (restore() comes back to it) */
    public Pen save() {
        saved.push(transform);
        return this;
    }

    /** back to where things went at the matching save() */
    public Pen restore() {
        if (saved.isEmpty()) {
            throw new IllegalStateException("Pen.restore() without a save()");
        }
        transform = saved.pop();
        return this;
    }

    /** what is drawn from now on is moved by dx, dy (your numbers) */
    public Pen translate(double dx, double dy) {
        return then(new PositionTransform.Translate((float) dy, (float) dx));
    }

    /** ... turned by degrees (counter-clockwise, y up) around the current origin */
    public Pen rotate(double degrees) {
        return then(new PositionTransform.Rotate(degrees));
    }

    /** ... made factor times bigger, from the current origin */
    public Pen scale(double factor) {
        return scale(factor, factor);
    }

    public Pen scale(double factorX, double factorY) {
        return then(new PositionTransform.Scale(factorY, factorX));   // Scale takes (row, col): y, then x
    }

    /** the new step is applied to points first, then what was there - as a canvas does */
    private Pen then(PositionTransform step) {
        transform = transform == PositionTransform.NOOP ? step : new PositionTransform.FofG(transform, step);
        return this;
    }

    private Vec2<?> placed(double x, double y) {
        return transform.transform((float) x, (float) y);
    }

    // ---- drawing --------------------------------------------------------------------------------

    public Pen point(double x, double y) {
        Vec2<?> p = placed(x, y);
        dots.plot((float) area.dotCol(p.getX()), (float) area.dotRow(p.getY()));
        return this;
    }

    public Pen points(Iterable<? extends Vec2<?>> points) {
        for (Vec2<?> p : points) {
            point(p.getX(), p.getY());
        }
        return this;
    }

    /** a straight line between two points */
    public Pen line(double x1, double y1, double x2, double y2) {
        Vec2<?> a = placed(x1, y1), b = placed(x2, y2);
        lineInDots(area.dotCol(a.getX()), area.dotRow(a.getY()), area.dotCol(b.getX()), area.dotRow(b.getY()));
        return this;
    }

    /** y = f(x) across the window: one point per dot column, joined (where f is a number) */
    public Pen function(DoubleUnaryOperator f) {
        Rectangle r = area.dots();
        double prevCol = 0, prevRow = 0;
        boolean havePrev = false;
        for (int col = r.getCol(); col < r.getCol() + r.getWidth(); col++) {
            double y = f.applyAsDouble(area.dataX(col));
            if (Double.isNaN(y) || Double.isInfinite(y)) {
                havePrev = false;
                continue;
            }
            double row = keepNear(area.dotRow(y), r);
            if (havePrev) {
                lineInDots(prevCol, prevRow, col, row);
            } else {
                dots.plot(col, (float) row);
            }
            prevCol = col;
            prevRow = row;
            havePrev = true;
        }
        return this;
    }

    /** fills between y = f(x) and the x axis (y 0), for x from fromX to toX */
    public Pen fillUnder(DoubleUnaryOperator f, double fromX, double toX) {
        Rectangle r = area.dots();
        double zero = keepNear(area.dotRow(0), r);
        int fromCol = (int) Math.ceil(area.dotCol(Math.min(fromX, toX)));
        int toCol = (int) Math.floor(area.dotCol(Math.max(fromX, toX)));
        for (int col = Math.max(fromCol, r.getCol()); col <= Math.min(toCol, r.getCol() + r.getWidth() - 1); col++) {
            double y = f.applyAsDouble(area.dataX(col));
            if (Double.isNaN(y) || Double.isInfinite(y)) {
                continue;
            }
            lineInDots(col, keepNear(area.dotRow(y), r), col, zero);
        }
        return this;
    }

    /** a shape, in your numbers; clicks find it (PlotArea.onClick) and it can be dragged (PlotArea.draggable) */
    public Pen draw(Shape<?> shape) {
        area.drawShape(shape, color, transform);
        dots.setNextPlotColor(color);
        return this;
    }

    /** text starting at the character nearest (x, y) */
    public Pen text(double x, double y, String text) {
        Vec2<?> p = placed(x, y);
        area.text(new Position.Simple((float) area.dotRow(p.getY()), (float) area.dotCol(p.getX())), text, color);
        return this;
    }

    /** text that clicks as a shape: a click on it reports clicksAs (e.g. a legend line, as its slice) */
    public Pen text(double x, double y, String text, Shape<?> clicksAs) {
        Vec2<?> p = placed(x, y);
        area.text(new Position.Simple((float) area.dotRow(p.getY()), (float) area.dotCol(p.getX())), text, color, clicksAs);
        return this;
    }

    /** one dot, in grid dots (for the axes' ticks) */
    Pen dot(int col, int row) {
        dots.plot(col, row);
        return this;
    }

    private void lineInDots(double col1, double row1, double col2, double row2) {
        Rectangle r = area.dots();
        lines.draw((float) col1, (float) keepNear(row1, r), (float) col2, (float) keepNear(row2, r), dots);
    }

    /** a row far off the grid (a steep curve, an asymptote) brought to just past its edge: lines stay short */
    private static double keepNear(double row, Rectangle r) {
        return Math.max(r.getRow() - 1, Math.min(r.getRow() + r.getHeight(), row));
    }
}
