package mizzen.components.plot.grid.drawable;

import mizzen.components.ColorHiliter;
import mizzen.components.Hiliter;
import mizzen.components.plot.PointPlotter;
import mizzen.components.plot.XYTranslate;
import mizzen.components.plot.shape.Line;
import mizzen.components.plot.shape.RectangleShape;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Movable;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;

public class PlotLineController  implements PlotShapeController , Movable, Hiliter {

    Line lineShape;
    Position offset;
    ColorHiliter colorHiliter;

    public PlotLineController(Position p1, Position p2, Color color) {

        Rectangle bbox = new  Line(p1, p2).asRectangle();
        this.offset = new Position.Simple(bbox.getPostion_TopLeft());

        Position offsetAdjustment = this.offset.negate();

        // adjust to zero point, offset will move to desired location during plot
        p1 = p1.offsetAdd(offsetAdjustment);
        p2 = p2.offsetAdd(offsetAdjustment);

        this.lineShape = new Line(p1, p2, color);
        this.colorHiliter = new ColorHiliter(color);
    }

    @Override
    public void plot(PointPlotter plotter, Rectangle bounds) {
        XYTranslate plotterWithOffset = new XYTranslate(plotter, this.offset);
        this.lineShape.setColor(this.colorHiliter.getColor());
        
        if (this.colorHiliter.isHilited()) {
            RectangleShape rectShape = new RectangleShape(this.lineShape.asRectangle(), Colors.BBla);
            rectShape.draw(plotterWithOffset);
        }
        this.lineShape.draw(plotterWithOffset);
    }

    @Override
    public void hilite() {
        colorHiliter.hilite();
    }

    @Override
    public void unHilite() {
        colorHiliter.unHilite();
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
    public Rectangle asRectangle() {
        Rectangle rectangle = this.lineShape.asRectangle();
        rectangle.moveTo(offset);
        return rectangle;
    }
}
