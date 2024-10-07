package dtb.terminalIO.components.plot.grid.drawable;

import dtb.terminalIO.components.ColorHiliter;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.XYTranslate;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Movable;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

public class PlotTriangleController implements PlotShapeController , Movable, Hiliter {

    Triangle triangleShape;
    Position offset;
    ColorHiliter  colorHiliter;

    public PlotTriangleController(Position p1, Position p2, Position p3, Color color) {

        Rectangle bbox = new  Triangle(p1, p2, p3).asRectangle();
        this.offset = new Position.Simple(bbox.getPostion_TopLeft());


        Position offsetAdjustment = this.offset.negate();

        p1 = p1.offsetAdd(offsetAdjustment);
        p2 = p2.offsetAdd(offsetAdjustment);
        p3 = p3.offsetAdd(offsetAdjustment);

        this.triangleShape = new Triangle(p1, p2, p3, color);
        this.colorHiliter = new ColorHiliter(color);
    }

    @Override
    public void plot(PointPlotter plotter, Rectangle bounds) {
        XYTranslate plotterWithOffset = new XYTranslate(plotter, this.offset);
        this.triangleShape.setColor(this.colorHiliter.getColor());
        this.triangleShape.setFilled(this.colorHiliter.isHilited());
        
        if (this.colorHiliter.isHilited()) {
            RectangleShape rectShape = new RectangleShape(this.triangleShape.asRectangle(), Colors.BBla);
            rectShape.draw(plotterWithOffset);
        }
        this.triangleShape.draw(plotterWithOffset);
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
        Rectangle rectangle = this.triangleShape.asRectangle();
        rectangle.moveTo(offset);
        return rectangle;
    }
    
}
