package mizzen.geom.position;

import mizzen.geom.Rectangle;

public class PositionLeftAlignedRelative implements Position {

    Rectangle anchorComponent;
    Rectangle alignedComponent;
    int horizontalSpacer;

    public PositionLeftAlignedRelative(Rectangle anchorComponent, Rectangle alignedComponent, int horizontalSpacer) {
        this.anchorComponent = anchorComponent;
        this.alignedComponent = alignedComponent;
        this.horizontalSpacer = horizontalSpacer;
    }
    
    public PositionLeftAlignedRelative(Rectangle anchorComponent, Rectangle alignedComponent) {
        this(anchorComponent, alignedComponent, 1);
    }
    
    Rectangle getAnchor() {
        if (anchorComponent == null) {
            return null;
        }
        return anchorComponent;
    }
    
    @Override
    public int getCol() {
        Rectangle anchor = getAnchor();
        if (anchor != null) {
            return anchor.getCol() + this.horizontalSpacer;
        }
        return Position.ZERO.getCol();
    }

    @Override
    public int getRow() {
        Rectangle anchor = getAnchor();
        if (anchor != null) {
            return anchor.getRow();
        }
        return Position.ZERO.getRow();
    }

}
