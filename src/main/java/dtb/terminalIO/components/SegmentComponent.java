package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;

/**
 * A Component that draws one ScreenLineSegment (text with a color and position)
 */
public class SegmentComponent extends AbstractComponent<SegmentComponent> {
    
    ScreenLineSegment scs;
    
    public SegmentComponent(ScreenLineSegment scs) {
        super(scs.getColors());
        this.scs = scs;
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.add(this.scs);
    }
    
    public Component<?> getComponentAtOffset(Position offset) {
        if (this.containsPosition(offset)) {
            return this;
        }
        return null;
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
