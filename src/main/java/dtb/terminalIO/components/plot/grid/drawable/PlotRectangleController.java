package dtb.terminalIO.components.plot.grid.drawable;

import dtb.terminalIO.components.ColorHiliter;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.XYTranslate;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Movable;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

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
        if (this.colorHiliter.isHiltited()) {
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
