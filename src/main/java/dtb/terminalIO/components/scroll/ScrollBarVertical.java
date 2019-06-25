package dtb.terminalIO.components.scroll;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.VerticalLine;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

/**
 * A ScrollBar in the Vertical orientation.
 */
public class ScrollBarVertical extends BoundedComponent<ScrollBarVertical> {

    Position offset;
    Dimension viewedSize;
    Dimension viewerSize;

    public ScrollBarVertical(Color color, Position pos, int len) {
        super(color, pos, new Dimension.Simple(len, 1));
    }
    
    public ScrollBarVertical(Color color, int row, int col, int len) {
        super(color, new Position.Simple(row, col), new Dimension.Simple(len, 1));
    }
    
    Position calcOffset() {
        if (viewedSize == null || offset == null) {
            return null;
        }
        float percentage = (float)viewerSize.getHeight() / (float)viewedSize.getHeight();
        if (percentage >= 1) {
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
            segments.merge(makeChildComponent(()->new VerticalLine(color, this.getRow(), this.getCol(), this.getHeight() )).getSegmentsBuffer());
            return;
        }
        //      |
        //      |  [A]
        //      X
        //      X  [B]
        //      X
        //      |  [C]
        //      |

        float percentage = (float)viewerSize.getHeight() / (float)viewedSize.getHeight();
        
        int a_height = scrollBarOffset.getRow();
        int b_height = Math.max((int)(percentage * (float)this.getHeight()),1);
        int c_height = this.getHeight() - ( a_height + b_height );
        
        if (b_height >= this.getHeight()) {
            segments.merge(makeChildComponent(()->new VerticalLine(color, this.getRow(), this.getCol(), this.getHeight() )).getSegmentsBuffer());
            return;
        }
        
        
        if (a_height>0) {
            segments.merge(makeChildComponent(()->new VerticalLine(color, this.getRow(), this.getCol(), a_height )).getSegmentsBuffer());
        }
        
        if (b_height>0) {
            segments.merge(makeChildComponent(()->new VerticalLine(color, this.getRow()+a_height, this.getCol(), b_height ).setVerticalLineChar('░')).getSegmentsBuffer());
        }
        
        if (c_height>0) {
            segments.merge(makeChildComponent(()->new VerticalLine(color, this.getRow()+a_height+b_height, this.getCol(), c_height )).getSegmentsBuffer());
        }
    }
}
