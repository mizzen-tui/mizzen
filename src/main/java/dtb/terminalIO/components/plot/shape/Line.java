package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.position.Position;

/**
 * Defines a line between two points.
 * 
 * <p/>
 * 
 * Uses a {@link LinePlotter} to draw itself.
 *
 */
public class Line implements Shape {

    int x1;
    int y1;
    int x2;
    int y2;

    Color color;

    public Line() {
    }

    public Line(int x1, int y1, int x2, int y2, Color color) {
        super();
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.color = color;
    }

    public Line(int x1, int y1, int x2, int y2) {
        this(x1, y1, x2, y2, null);
    }

    public Line(Position p1, Position p2, Color color) {
        this(p1.getCol(), p1.getRow(), p2.getCol(), p2.getRow(), color);
    }

    public Line(Position p1, Position p2) {
        this(p1, p2, null);
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public int getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getY2() {
        return y2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }

    @Override
    public void draw(PointPlotter pointPlotter) {
        Color lastColor = null;
        if (color != null) {
            lastColor = pointPlotter.setColor(color);
        }
        new LinePlotter().draw(this.getX1(), this.getY1(), this.getX2(), this.getY2(), pointPlotter);

        if (color != null) {
            pointPlotter.setColor(lastColor);
        }

    }

}
