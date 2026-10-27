package mizzen.components.plot.shape;

import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.shape.Shape.AbstractShape;
import mizzen.controlcodes.color.Color;
import mizzen.geom.position.Position;
import mizzen.geom.position.PositionTransform;
import mizzen.geom.position.Vec2;

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
