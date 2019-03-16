package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

/**
 * A Component that draws a rectangle
 */
public class RectangleComponent extends BoundedComponent<RectangleComponent> implements Hiliter {

    private boolean hilited = false;
    
    public RectangleComponent(Color color, int height, int width) {
        super(color, new Dimension.Simple(height, width));
    }

    public RectangleComponent(Color color, Position p, Dimension d) {
        super(color, p, d);
    }

    public RectangleComponent(Color color, Rectangle r) {
        super(color, r, r);
    }
    
    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {

        HorizontalLine topLine = makeChildComponent(()->new HorizontalLine(color, getRow(), getCol()+1, getWidth() - 2));
        HorizontalLine bottomLine = makeChildComponent(()->new HorizontalLine(color, getBottomMostRow(), getCol()+1, getWidth() - 2));

        VerticalLine vLeft = makeChildComponent(()->new VerticalLine(color, this.getRow()+1, this.getCol(), getHeight()-2));
        VerticalLine vRight = makeChildComponent(()->new VerticalLine(color, this.getRow()+1, this.getRightMostCol(), getHeight()-2));
        
        // verticals!
        segments.merge(topLine.getSegmentsBuffer());
        segments.merge(bottomLine.getSegmentsBuffer());
        segments.merge(vLeft.getSegmentsBuffer());
        segments.merge(vRight.getSegmentsBuffer());

        segments.merge(makeChildComponent(()->new CharComponent(color, this.getRow(), this.getCol(),this.getBlockChars().getTOP_LEFT())).getSegmentsBuffer());
        segments.merge(makeChildComponent(()->new CharComponent(color, this.getRow(), this.getRightMostCol(),this.getBlockChars().getTOP_RIGHT())).getSegmentsBuffer());
        segments.merge(makeChildComponent(()->new CharComponent(color, this.getBottomMostRow(), this.getCol(),this.getBlockChars().getBOTTOM_LEFT())).getSegmentsBuffer());
        segments.merge(makeChildComponent(()->new CharComponent(color, this.getBottomMostRow(), this.getRightMostCol(),this.getBlockChars().getBOTTOM_RIGHT())).getSegmentsBuffer());
        
    }

    @Override
    public Color getColor() {
        Color color = super.getColor();
        return this.hilited ? /*Colors.IYel  */ color.bold() : color; 
    }
    
    @Override
    public void hilite() {
        if ( ! this.hilited) {
            markAsDirty();
        }
        this.hilited = true;
    }

    @Override
    public void unHilite() {
        if (this.hilited) {
            markAsDirty();
        }
        this.hilited = false;
    }

}