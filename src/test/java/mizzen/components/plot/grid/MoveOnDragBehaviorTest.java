package mizzen.components.plot.grid;

import mizzen.geom.Movable;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import junit.framework.TestCase;

/**
 * MoveOnDragBehavior moves anything Movable: the box follows the drag, and stays inside the bounds.
 */
public class MoveOnDragBehaviorTest extends TestCase {

    /** a 2x2 box that can move - not a shape, just Movable */
    static class Box implements Movable {
        final Rectangle.Simple rect = new Rectangle.Simple(0, 0, 2, 2);

        @Override
        public void moveTo(int row, int col) {
            rect.moveTo(row, col);
        }

        @Override
        public void moveDelta(int rowOffset, int colOffset) {
            rect.moveTo(rect.getRow() + rowOffset, rect.getCol() + colOffset);
        }
    }

    private final Box box = new Box();
    private final Rectangle bounds = new Rectangle.Simple(0, 0, 10, 10);
    private final MoveOnDragBehavior drag = new MoveOnDragBehavior(box, () -> box.rect);

    public void testTheBoxFollowsTheDrag() {
        drag.onDrag(new Position.Simple(5, 5), bounds);
        assertTrue("box " + box.rect + " should be under the drag point", box.rect.contains(new Rectangle.Simple(5, 5, 1, 1)));
    }

    public void testTheBoxStaysInsideTheBounds() {
        drag.onDrag(new Position.Simple(50, 50), bounds);
        assertTrue("box " + box.rect + " left the bounds", bounds.contains(box.rect));
        assertEquals(8, box.rect.getRow());
        assertEquals(8, box.rect.getCol());
    }
}
