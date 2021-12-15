package dtb.terminalIO.geom.position;

import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.VAlign;

public class PositionRelativeFactory {
    
    public static Position fromAlign(Align align, Rectangle anchorComponent, Rectangle alignedComponent, int horizontalSpacer) {
        return new PositionAlignedRelative(anchorComponent, 
                                           alignedComponent, 
                                           (anchor, aligned)-> anchor.getRow(), 
                                           ColumnAlignRelativeTransForm.Factory.fromAlign(align, horizontalSpacer));

    }

    public static Position fromAlign(Align align, Rectangle anchorComponent, Rectangle alignedComponent) {
        return fromAlign(align, anchorComponent, alignedComponent, 0);
        
    }

    public static Position fromVAlign(VAlign valign, Rectangle anchorComponent, Rectangle alignedComponent, int verticalSpacer) {
        return new PositionAlignedRelative(anchorComponent, 
                                           alignedComponent, 
                                           RowAlignRelativeTransForm.Factory.fromAlign(valign, verticalSpacer), 
                                           (anchor, aligned)-> anchor.getCol());
    }

    public static Position fromVAlign(VAlign valign, Rectangle anchorComponent, Rectangle alignedComponent) {
        return fromVAlign(valign, anchorComponent, alignedComponent, 0);
    }

    public static Position fromAlign(VAlign valign, int verticalSpacer, Align align, int horizontalSpacer, Rectangle anchorComponent, Rectangle alignedComponent) {

        return new PositionAlignedRelative(anchorComponent, 
                                           alignedComponent, 
                                           RowAlignRelativeTransForm.Factory.fromAlign(valign, verticalSpacer), 
                                           ColumnAlignRelativeTransForm.Factory.fromAlign(align, horizontalSpacer));

    }

    public static Position fromAlign(VAlign valign, Align align, Rectangle anchorComponent, Rectangle alignedComponent) {
        return fromAlign(valign, 0, align, 0, anchorComponent, alignedComponent);
    }
    

}
