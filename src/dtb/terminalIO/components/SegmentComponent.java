package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode.FGBGColors;
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
    protected void draw(FGBGColors color, IScreenLineSegmentsBuffer segments) {
        segments.add(this.scs);
    }
    
    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return this.scs;
    } 

    
}
