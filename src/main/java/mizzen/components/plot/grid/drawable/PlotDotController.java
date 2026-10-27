package mizzen.components.plot.grid.drawable;

import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.shape.DotShape;
import mizzen.components.plot.shape.Shape;
import mizzen.controlcodes.color.Color;
import mizzen.geom.Dimension;
import mizzen.geom.Movable;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;

public class PlotDotController implements PlotShapeController , Movable {

    Position offset = Position.ZERO;
    Color color;
    
    public PlotDotController(Position offset, Color color) {
        super();
        this.offset = offset;
        this.color = color;
    }
    
    public Position getOffset() {
        return offset;
    }

    public Color getColor() {
        return color;
    }

    @Override
    public void moveTo(int row, int col) {
        this.offset = new Position.Simple(row, col);
    }

    @Override
    public void moveDelta(int rowOffset, int colOffset) {
        this.offset = this.offset.offsetAdd(rowOffset, colOffset);
    }

    @Override
    public void plot(PointPlotter plotter, Rectangle bounds) {
        Shape<DotShape> dot = new DotShape(this.offset, this.color);
        dot.draw(plotter);
    }


    @Override
    public Rectangle asRectangle() {
        return new Rectangle.Simple(this.offset, Dimension.ONE);
    }


}
