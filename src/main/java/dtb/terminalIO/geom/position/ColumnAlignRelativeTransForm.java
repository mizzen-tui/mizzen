package dtb.terminalIO.geom.position;

import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Rectangle;

/**
 * Works in close conjunction with a {@link PositionAlignedRelative} to 
 * modify the col attribute of a Position object relative to a rectangle
 * that acts as an anchor point.
 *
 */
@FunctionalInterface
public interface ColumnAlignRelativeTransForm {
    int getCol(Rectangle anchorComponent, Rectangle alignedComponent);

    public static class Factory {
        
        public static ColumnAlignRelativeTransForm fromAlign(Align align, int horizontalSpacer) {
            
            if (Align.LEFT.equals(align)) {
                return (anchorComponent, alignedComponent) ->  anchorComponent.getCol() + horizontalSpacer;
            }
            
            if (Align.RIGHT.equals(align)) {
                return (anchorComponent, alignedComponent) -> {
                    int newColumn = anchorComponent.getRightMostCol() - (horizontalSpacer + alignedComponent.getWidth());
                    return newColumn;
                };
            }
            
            if (Align.MIDDLE.equals(align)) {
                return (anchorComponent, alignedComponent) -> {
                    if (anchorComponent.getWidth() <= alignedComponent.getWidth()) {
                        return anchorComponent.getCol();
                    }
                    return (anchorComponent.getWidth() - alignedComponent.getWidth())/2 + horizontalSpacer + anchorComponent.getCol();
                };
            }
            
            throw new IllegalArgumentException("Align parameter not valid " + String.valueOf(align));
            
        }
    }
    
}