package mizzen.components.plot.grid.drawable;

import mizzen.components.ColorHiliter;
import mizzen.components.Hiliter;
import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.XYTranslate;
import mizzen.components.plot.shape.RectangleShape;
import mizzen.controlcodes.color.Color;
import mizzen.geom.Dimension;
import mizzen.geom.Movable;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;

public class PlotRectangleController implements PlotShapeController, Movable, Hiliter {
    Position offset;
    RectangleShape rectangleShape;
    ColorHiliter  colorHiliter;

    public PlotRectangleController(Position offset, Dimension size, Color color) {
        this.rectangleShape = new RectangleShape(new Rectangle.Simple(Position.ZERO,size), color);
        this.offset = offset != null ? offset : Position.ZERO;
        this.colorHiliter = new ColorHiliter(color);
    }

    @Override
    public void plot(PointPlotter plotter, Rectangle bounds) {
        XYTranslate plotterWithOffset = new XYTranslate(plotter, this.offset);
        this.rectangleShape.setColor(this.colorHiliter.getColor());
        this.rectangleShape.draw(plotterWithOffset);
        if (this.colorHiliter.isHilited()) {
            Rectangle innerRectangle = new Rectangle.Simple(this.rectangleShape.offsetAdd(1, 1), new Dimension.Simple(this.rectangleShape.getHeight()-2,this.rectangleShape.getWidth()-2));
            RectangleShape innerRectangleShape = new RectangleShape(new Rectangle.Simple(innerRectangle), this.colorHiliter.getColor());
            innerRectangleShape.draw(plotterWithOffset);
        }
    }

    @Override
    public Rectangle asRectangle() {
        return new Rectangle.Simple(this.offset, this.rectangleShape.getRectangle());
    }

    @Override
    public void moveTo(int row, int col) {
        this.offset = new Position.Simple(row, col);
    }

    @Override
    public void moveDelta(int rowOffset, int colOffset) {
        this.offset = this.offset.addRows(rowOffset).addCols(colOffset);
    }

    @Override
    public void hilite() {
        colorHiliter.hilite();
    }

    @Override
    public void unHilite() {
        colorHiliter.unHilite();
    }

}
