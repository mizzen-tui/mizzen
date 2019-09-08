package dtb.terminalIO.geom.position;

import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.VAlign;

/**
 * Works in close conjunction with a {@link PositionAlignedRelative} to 
 * modify the row attribute of a Position object relative to a rectangle
 * that acts as an anchor point.
 *
 */
@FunctionalInterface
public interface RowAlignRelativeTransForm {
    int getCol(Rectangle anchorComponent, Rectangle alignedComponent);

    
    public static class Factory {
        
        public static RowAlignRelativeTransForm fromAlign(VAlign valign, int verticalSpacer) {
            
            if (VAlign.TOP.equals(valign)) {
                return (anchorComponent, alignedComponent) ->  anchorComponent.getRow() + verticalSpacer;
            }

            if (VAlign.BOTTOM.equals(valign)) {
                return (anchorComponent, alignedComponent) -> {
                    int newRow = anchorComponent.getBottomMostRow() - (verticalSpacer + alignedComponent.getHeight()-1);
                    return newRow;
                };
            }
            
            if (VAlign.MIDDLE.equals(valign)) {
                return (anchorComponent, alignedComponent) -> {
                    if (anchorComponent.getHeight() <= alignedComponent.getHeight()) {
                        return anchorComponent.getCol();
                    }
                    int newRow = (anchorComponent.getHeight() - alignedComponent.getHeight())/2 + verticalSpacer + anchorComponent.getRow();
                    return newRow;
                };
            }
            
            throw new IllegalArgumentException("VAlign parameter not valid " + String.valueOf(valign));
            
        }
    }
 }