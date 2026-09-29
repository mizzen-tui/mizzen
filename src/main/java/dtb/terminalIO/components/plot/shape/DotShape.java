package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.shape.Shape.AbstractShape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;

public class DotShape extends AbstractShape<DotShape> {
    
    Position p;

    Color color;

    public DotShape(Position p, Color color) {
        this.p = p;
        this.color = color;
    }

    public DotShape(Position p) {
        this(p, null);
    }

    public DotShape(int x, int y, Color color) {
        this(new Position.Simple(y, x), color);
    }

    public DotShape(int x, int y) {
        this(x, y, null);
    }

    public int getX() {
        return p.getCol();
    }

    public int getY() {
        return p.getRow();
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

        Vec2<?> transformedPosition = positionTransform.transform(this.p);

        pointPlotter.plot(transformedPosition.getX(), transformedPosition.getY());

        if (color != null) {
            pointPlotter.setNextPlotColor(lastColor);
        }

    }
}
