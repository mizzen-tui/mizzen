package dtb.terminalIO.geom;

public class PositionHorizontalRelative implements Position {

    Rectangle anchorComponent;
    int horizontalSpacer;
    
    public PositionHorizontalRelative(Rectangle anchorComponent, int horizontalSpacer) {
        this.anchorComponent = anchorComponent;
        this.horizontalSpacer = horizontalSpacer;
    }
    
    public PositionHorizontalRelative(Rectangle anchorComponent) {
        this(anchorComponent, 1);
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
            return anchor.getPostion_TopRight().getCol() + 1 + this.horizontalSpacer;
        }
        return Position.ZERO.getCol();
    }

    @Override
    public int getRow() {
        Rectangle anchor = getAnchor();
        if (anchor != null) {
            return anchor.getPostion_TopRight().getRow();
        }
        return Position.ZERO.getRow();
    }
    
}