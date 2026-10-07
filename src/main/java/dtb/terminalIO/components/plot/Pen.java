package dtb.terminalIO.components.plot;

import java.util.function.DoubleUnaryOperator;

import dtb.terminalIO.components.plot.shape.LinePlotter;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
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
 */
public class Pen {

    private final PlotArea area;
    private final PointPlotter dots;
    private final LinePlotter lines = new LinePlotter();
    private Color color;

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

    public Pen point(double x, double y) {
        dots.plot((float) area.dotCol(x), (float) area.dotRow(y));
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
        lineInDots(area.dotCol(x1), area.dotRow(y1), area.dotCol(x2), area.dotRow(y2));
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
        area.drawShape(shape, color);
        dots.setNextPlotColor(color);
        return this;
    }

    /** text starting at the character nearest (x, y) */
    public Pen text(double x, double y, String text) {
        area.text(new Position.Simple((float) area.dotRow(y), (float) area.dotCol(x)), text, color);
        return this;
    }

    /** text that clicks as a shape: a click on it reports clicksAs (e.g. a legend line, as its slice) */
    public Pen text(double x, double y, String text, Shape<?> clicksAs) {
        area.text(new Position.Simple((float) area.dotRow(y), (float) area.dotCol(x)), text, color, clicksAs);
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
