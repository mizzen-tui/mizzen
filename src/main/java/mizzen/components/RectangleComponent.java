package mizzen.components;

import java.util.TreeSet;

import mizzen.components.BlockChars.BlockHiliter;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;

/**
 * A Component that draws a rectangle
 */
public class RectangleComponent extends BoundedComponent<RectangleComponent> implements Hiliter {

    private boolean hilited = false;
    private ColorHiliter colorHiliter;
    private BlockHiliter blockHiliter = new BlockChars.BlockHiliter();

    public RectangleComponent(Color color, int height, int width) {
        super(color, new Dimension.Simple(height, width));
        this.colorHiliter = makeColorHiliter();
        this.setFillOnClear(false);
    }

    public RectangleComponent(Color color, Position p, Dimension d) {
        super(color, p, d);
        this.colorHiliter = makeColorHiliter();
        this.setFillOnClear(false);
    }

    public RectangleComponent(Color color, Rectangle r) {
        super(color, r, r);
        this.colorHiliter = makeColorHiliter();
    }

    @Override
    public RectangleComponent setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        this.colorHiliter = makeColorHiliter();
        return this;
    }
    
    
    private ColorHiliter makeColorHiliter() {
        return new ColorHiliter(()->RectangleComponent.super.getColor());
    }

    public BlockChars getBlockChars() {
        return blockHiliter.getBlockChars();
    }


    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {

        HorizontalLine topLine = makeChildComponent(()->new HorizontalLine(color, getRow(), getCol()+1, getWidth() - 2));
        HorizontalLine bottomLine = makeChildComponent(()->new HorizontalLine(color, getBottomMostRow(), getCol()+1, getWidth() - 2));

        VerticalLine vLeft = makeChildComponent(()->new VerticalLine(color, this.getRow()+1, this.getCol(), getHeight()-2));
        VerticalLine vRight = makeChildComponent(()->new VerticalLine(color, this.getRow()+1, this.getRightMostCol(), getHeight()-2));
        
        // none of these components overlap so its safe to use the fast treeset add methods.
        segments.clear();

        // update existing TreeSet in-place
        TreeSet<ScreenLineSegment> treeSetOfSegments = segments.getSegments();
        
        // verticals!
        treeSetOfSegments.addAll(topLine.getSegmentsBuffer().getSegments());
        treeSetOfSegments.addAll(bottomLine.getSegmentsBuffer().getSegments());
        treeSetOfSegments.addAll(vLeft.getSegmentsBuffer().getSegments());
        treeSetOfSegments.addAll(vRight.getSegmentsBuffer().getSegments());

        
        treeSetOfSegments.addAll(makeChildComponent(()->new CharComponent(color, this.getRow(), this.getCol(),this.getBlockChars().getTOP_LEFT())).getSegmentsBuffer().getSegments());
        treeSetOfSegments.addAll(makeChildComponent(()->new CharComponent(color, this.getRow(), this.getRightMostCol(),this.getBlockChars().getTOP_RIGHT())).getSegmentsBuffer().getSegments());
        treeSetOfSegments.addAll(makeChildComponent(()->new CharComponent(color, this.getBottomMostRow(), this.getCol(),this.getBlockChars().getBOTTOM_LEFT())).getSegmentsBuffer().getSegments());
        treeSetOfSegments.addAll(makeChildComponent(()->new CharComponent(color, this.getBottomMostRow(), this.getRightMostCol(),this.getBlockChars().getBOTTOM_RIGHT())).getSegmentsBuffer().getSegments());
        
        
//        segments.merge(topLine.getSegmentsBuffer());
//        segments.merge(bottomLine.getSegmentsBuffer());
//        segments.merge(vLeft.getSegmentsBuffer());
//        segments.merge(vRight.getSegmentsBuffer());

//        segments.merge(makeChildComponent(()->new CharComponent(color, this.getRow(), this.getCol(),this.getBlockChars().getTOP_LEFT())).getSegmentsBuffer());
//        segments.merge(makeChildComponent(()->new CharComponent(color, this.getRow(), this.getRightMostCol(),this.getBlockChars().getTOP_RIGHT())).getSegmentsBuffer());
//        segments.merge(makeChildComponent(()->new CharComponent(color, this.getBottomMostRow(), this.getCol(),this.getBlockChars().getBOTTOM_LEFT())).getSegmentsBuffer());
//        segments.merge(makeChildComponent(()->new CharComponent(color, this.getBottomMostRow(), this.getRightMostCol(),this.getBlockChars().getBOTTOM_RIGHT())).getSegmentsBuffer());
        
    }

    @Override
    public Color getColor() {
        return colorHiliter.asColor();
    }
    
    @Override
    public void hilite() {
        if ( ! this.hilited) {
            markAsDirty();
        }
        colorHiliter.hilite();
        blockHiliter.hilite();
        this.hilited = true;
    }

    @Override
    public void unHilite() {
        if (this.hilited) {
            markAsDirty();
        }
        colorHiliter.unHilite();
        blockHiliter.unHilite();
        this.hilited = false;
    }

}