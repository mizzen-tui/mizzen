package dtb.terminalIO.components.scroll;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.HorizontalLine;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;


/**
 * A ScrollBar in the Horizontal orientation.
 */
public class ScrollBarHorizontal  extends BoundedComponent<ScrollBarVertical> {

    Position offset;
    Dimension viewedSize;
    Dimension viewerSize;

    // ▒
    // '░'

    public ScrollBarHorizontal(Color color, Position pos, int len) {
        super(color, pos, new Dimension.Simple(1, len));
    }
    
    public ScrollBarHorizontal(Color color, int row, int col, int len) {
        super(color, new Position.Simple(row, col), new Dimension.Simple(1, len));
    }
    
    Position calcOffset() {
        if (viewedSize == null || offset == null) {
            return null;
        }
        float percentage = (float)this.getWidth() / (float)viewedSize.getWidth();
        if (percentage >= 1.0) {
            return null;
        }
        
        return new Position.Simple( (int)(  ((float)offset.getRow()) * percentage) , 
                                    (int)(  ((float)offset.getCol()) * percentage) );
    }
    
    public void setOffset(Position offset) {
        this.offset = offset;
        this.markAsDirty();
    }

    public void setViewedSize(Dimension viewedSize, Dimension viewerSize) {
        this.viewedSize = viewedSize;
        this.viewerSize = viewerSize;
        this.markAsDirty();
    }
    
    
    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        
        Position scrollBarOffset = calcOffset();

        if (scrollBarOffset==null) {
            segments.merge(makeChildComponent(()->new HorizontalLine(color, this.getRow(), this.getCol(), this.getWidth() )).getSegmentsBuffer());
            return;
        }

        //  --------XXXXXXXX--------
        //    [A]     [B]      [C]
        
        float percentage = (float)viewerSize.getWidth() / (float)viewedSize.getWidth();
        
        
        int a_width = scrollBarOffset.getCol();
        int b_width = Math.max((int)(percentage * (float)this.getWidth()),1);
        int c_width = this.getWidth() - ( a_width + b_width );
        
        if (b_width >= this.getWidth()) {
            segments.merge(makeChildComponent(()->new HorizontalLine(color, this.getRow(), this.getCol(), this.getWidth() )).getSegmentsBuffer());
            return;
        }
        
        if (a_width>0) {
            segments.merge(makeChildComponent(()->new HorizontalLine(color, this.getRow(), this.getCol(), a_width )).getSegmentsBuffer());
        }
        
        if (b_width>0) {
            segments.merge(makeChildComponent(()->new HorizontalLine(color.bold(), this.getRow(), this.getCol()+a_width, b_width ).setHorizontalLineChar('▒')).getSegmentsBuffer());
        }
        
        if (c_width>0) {
            segments.merge(makeChildComponent(()->new HorizontalLine(color, this.getRow(), this.getCol()+a_width+b_width, c_width )).getSegmentsBuffer());
        }
    }
    
}
