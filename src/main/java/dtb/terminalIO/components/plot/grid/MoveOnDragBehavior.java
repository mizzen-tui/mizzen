package dtb.terminalIO.components.plot.grid;

import java.util.function.Supplier;

import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.plot.grid.drawable.PlotShapeController;
import dtb.terminalIO.geom.Movable;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.Rectangle.Simple;
import dtb.terminalIO.geom.position.Position;

/**
 * Dragging moves something: its box follows the mouse, kept inside the grid's bounds. Works for
 * anything that can move (Movable) and say where its box is now - a shape on a grid, a sprite, a
 * chart marker.
 * <pre>
 *     new MoveOnDragBehavior(sprite, sprite::asRectangle)
 * </pre>
 */
public class MoveOnDragBehavior implements DraggableOnGrid {

    private final Movable target;
    private final Supplier<Rectangle> box;

    /** target: what moves; box: where it is now (its bounding box on the grid) */
    public MoveOnDragBehavior(Movable target, Supplier<Rectangle> box) {
        this.target = target;
        this.box = box;
    }

    /** a shape on the grid that can also move (it must implement Movable) */
    public MoveOnDragBehavior(PlotShapeController shapeController) {
        this(asMovable(shapeController), shapeController::asRectangle);
    }

    private static Movable asMovable(PlotShapeController shapeController) {
        if (!(shapeController instanceof Movable)) {
            throw new IllegalArgumentException(HasName.UTIL.getName(shapeController) + " does not implement Movable, so dragging cannot move it.\n"
                                               + "Implement Movable on " + HasName.UTIL.getName(shapeController) + ", or use MoveOnDragBehavior(Movable, box).");
        }
        return (Movable) shapeController;
    }

    @Override
    public boolean onDragStart(Position gridPosition, Rectangle bounds) {
        return true;
    }

    @Override
    public boolean onDrag(Position gridPosition, Rectangle bounds) {

        Rectangle currentBox = box.get();
        Position midpoint = currentBox.centerPosition();
        Position offsetTo = midpoint.offsetTo(gridPosition);

        Rectangle newBBox = new Rectangle.Simple(currentBox);
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
             target.moveTo(newOffset);
         } else {
             // does not fit - let's try halfway there
             newOffset = currentPosition.offsetAdd(offsetTo.mul(.5));
             testNewBBox = new Rectangle.Simple(newBBox);
             testNewBBox.moveTo(newOffset.getRow(),  newOffset.getCol());
             if (bounds.contains(testNewBBox)) {
                 target.moveTo(newOffset);
             }
         }

        return true;
    }

    @Override
    public boolean onDragStop(Position gridPosition, Rectangle bounds) {
        return true;
    }

}
