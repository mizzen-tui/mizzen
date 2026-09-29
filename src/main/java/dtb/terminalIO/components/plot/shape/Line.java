package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.shape.Shape.AbstractShape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;

/**
 * Defines a line between two points.
 * 
 * <p/>
 * 
 * Uses a {@link LinePlotter} to draw itself.
 *
 */
public class Line extends AbstractShape<Line> {

    float x1;
    float y1;
    float x2;
    float y2;

    Color color;

    public Line() {
    }

    public Line(float x1, float y1, float x2, float y2, Color color) {
        super();
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.color = color;
    }

    public Line(int x1, int y1, int x2, int y2, Color color) {
        this((float)x1,(float)y1, (float) x2, (float) y2, color);
    }

    public Line(int x1, int y1, int x2, int y2) {
        this(x1, y1, x2, y2, null);
    }

    public Line(Vec2<?> p1, Vec2<?> p2, Color color) {
        this(p1.getX(), p1.getY(), p2.getX(), p2.getY(), color);
    }

    public Line(Vec2<?> p1, Vec2<?> p2) {
        this(p1, p2, null);
    }

    public float getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public float getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public float getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public float getY2() {
        return y2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }
    
    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform) {
        Color lastColor = null;
        if (color != null) {
            lastColor = pointPlotter.setNextPlotColor(color);
        }
        
        Vec2<?> p1 = positionTransform.transform(this.getX1(), this.getY1());
        Vec2<?> p2 = positionTransform.transform(this.getX2(), this.getY2());
        
        new LinePlotter().draw(p1.getX(), p1.getY(), p2.getX(), p2.getY(), pointPlotter);

        if (color != null) {
            pointPlotter.setNextPlotColor(lastColor);
        }

    }

    public Rectangle asRectangle() {
        Position p1 = new Position.Simple(y1,x1);
        Position p2 = new Position.Simple(y2,x2);
        return new Rectangle.Simple(p1, Dimension.ZERO)
                   .mergeAndExpandDimensionsWith(p2);
    }
}
