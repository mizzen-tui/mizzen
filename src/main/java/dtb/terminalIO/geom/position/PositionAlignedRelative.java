package dtb.terminalIO.geom.position;

import dtb.terminalIO.geom.Rectangle;

/**
 * This {@link Position} object aligns a Position (of a component/rectangle)
 * with an anchor (another component/rectangle ).
 * 
 * <p/>
 * 
 * Typically a position will be "aligned" to the anchor 
 * vertically via aligning (LEFT, MIDDLE, RIGHT )
 * and horizontally via aligning (TOP, MIDDLE, BOTTOM ).
 * 
 * <p/>
 * 
 * Use {@link PositionRelativeFactory} to build one of these Position objects
 * 
 */

// package scope - use PositionRelativeFactory to construct
class PositionAlignedRelative implements Position {

    Rectangle anchorComponent;
    Rectangle alignedComponent;
    
    RowAlignRelativeTransForm rowTransForm;
    ColumnAlignRelativeTransForm columnTransForm;
    
    public PositionAlignedRelative(Rectangle anchorComponent, Rectangle alignedComponent,
            RowAlignRelativeTransForm rowTransForm, ColumnAlignRelativeTransForm columnTransForm) {
        super();
        this.anchorComponent = anchorComponent;
        this.alignedComponent = alignedComponent;
        this.rowTransForm = rowTransForm;
        this.columnTransForm = columnTransForm;
    }
    
    @Override
    public int getCol() {
        if (anchorComponent != null && alignedComponent!=null && columnTransForm!=null) {
            return columnTransForm.getCol(anchorComponent, alignedComponent);
        }
        return Position.ZERO.getCol();
    }
    @Override
    public int getRow() {
        if (anchorComponent != null && alignedComponent!=null && rowTransForm!=null) {
            return rowTransForm.getCol(anchorComponent, alignedComponent);
        }
        return Position.ZERO.getRow();
    }

}
