package dtb.terminalIO.components.plot.grid;

import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.plot.grid.drawable.PlotShapeController;
import dtb.terminalIO.geom.Movable;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.Rectangle.Simple;
import dtb.terminalIO.geom.position.Position;

public class MoveOnDragBehavior implements DraggableOnGrid {

    PlotShapeController shapeController;

    public MoveOnDragBehavior(PlotShapeController shapeController) {
        if ( !( shapeController instanceof Movable  ) ) {
            throw new IllegalArgumentException(HasName.UTIL.getName(shapeController)+" does not implement Movable.\n"+
                                              "Cannot apply move functions using "+HasName.UTIL.getName(this)+" object.\n"+
                                              "Implement Movable on "+HasName.UTIL.getName(shapeController)+" to resolve this issue.");
        }
        this.shapeController = shapeController;
    }

    @Override
    public boolean onDragStart(Position gridPosition, Rectangle bounds) {
        return true;
    }

    @Override
    public boolean onDrag(Position gridPosition, Rectangle bounds) {

        Position midpoint = this.shapeController.asRectangle().centerPosition();
        Position offsetTo = midpoint.offsetTo(gridPosition);

        Rectangle newBBox = new Rectangle.Simple(this.shapeController.asRectangle());
        Position currentPosition = new Position.Simple(newBBox);

        Position newOffset = currentPosition.offsetAdd(offsetTo);
        if (newOffset.getRow()<0) {
            newOffset = new Position.Simple(0, newOffset.getCol());
        } else if (newOffset.getRow()+newBBox.getHeight() > bounds.getHeight()) {
            newOffset = new Position.Simple(bounds.getHeight() - newBBox.getHeight(), newOffset.getCol());
        }

        if (newOffset.getCol()<0) {
            newOffset = new Position.Simple(newOffset.getRow(), 0);
        } else if (newOffset.getCol()+newBBox.getWidth() > bounds.getWidth()) {
            newOffset = new Position.Simple(newOffset.getRow(), bounds.getWidth() - newBBox.getWidth());
        }

        Simple testNewBBox = new Rectangle.Simple(newBBox);
        testNewBBox.moveTo(newOffset.getRow(),  newOffset.getCol());
         if (bounds.contains(testNewBBox)) {
             ((Movable)this.shapeController).moveTo(newOffset);
         } else {
             // does not fit - let's try halfway there
             newOffset = currentPosition.offsetAdd(offsetTo.mul(.5));
             testNewBBox = new Rectangle.Simple(newBBox);
             testNewBBox.moveTo(newOffset.getRow(),  newOffset.getCol());
             if (bounds.contains(testNewBBox)) {
                 ((Movable)this.shapeController).moveTo(newOffset);
             }
         }

        return true;
    }

    @Override
    public boolean onDragStop(Position gridPosition, Rectangle bounds) {
        return true;
    }

}
