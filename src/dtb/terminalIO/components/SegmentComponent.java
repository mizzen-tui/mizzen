package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegment;
import dtb.terminalIO.geom.Rectangle;

/**
 * A Component that draws one ScreenLineSegment
 */
public class SegmentComponent extends Component.AbstractComponent<SegmentComponent> {
    
    ScreenLineSegment scs;
    
    public SegmentComponent(ScreenLineSegment scs) {
        super(scs.getColors());
        this.scs = scs;
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.add(this.scs);
    }
    
    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return this.scs;
    }

    @Override
    public int getCol() {
        return this.scs.getCol();
    }

    @Override
    public int getRow() {
        return this.scs.getRow();
    }

    @Override
    public int getWidth() {
        return this.scs.getWidth();
    }

    @Override
    public int getHeight() {
        return this.scs.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        this.scs.moveTo(row, col);
    } 

    
}
