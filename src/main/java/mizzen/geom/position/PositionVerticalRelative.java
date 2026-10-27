package mizzen.geom.position;

import mizzen.geom.Rectangle;

public class PositionVerticalRelative implements Position {

    Rectangle anchorComponent;
    int verticalSpacer;
    
    public PositionVerticalRelative(Rectangle anchorComponent, int verticalSpacer) {
        this.anchorComponent = anchorComponent;
        this.verticalSpacer = verticalSpacer;
    }

    public PositionVerticalRelative(Rectangle anchorComponent) {
        this(anchorComponent, 0);
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
            return anchor.getPostion_TopLeft().getCol();
        }
        return Position.ZERO.getCol();
    }

    @Override
    public int getRow() {
        Rectangle anchor = getAnchor();
        if (anchor != null) {
            return anchor.getPostion_BottomLeft().getRow() + 1 + this.verticalSpacer;
        }
        return Position.ZERO.getRow();
    }
    
}