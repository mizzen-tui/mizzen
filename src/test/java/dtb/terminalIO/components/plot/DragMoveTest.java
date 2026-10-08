package dtb.terminalIO.components.plot;

import dtb.terminalIO.geom.Movable;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;
import junit.framework.TestCase;

/**
 * DragMove: the offset moves by as much as the mouse did since the drag started - from wherever
 * the offset was, in whatever units the positions are given.
 */
public class DragMoveTest extends TestCase {

    public void testTheOffsetFollowsTheMouseFromWhereItWas() {
        PositionTransform.Translate offset = new PositionTransform.Translate(10f, 20f);   // row 10, col 20
        DragMove move = DragMove.of(offset);
        assertFalse(move.isMoving());

        move.start(Vec2.xy(2, 3));
        move.moveTo(Vec2.xy(5, 1));                          // the mouse: 3 across, 2 up
        assertEquals(23f, offset.getColDelta(), 1e-6);
        assertEquals(8f, offset.getRowDelta(), 1e-6);

        move.moveTo(Vec2.xy(2, 3));                          // back where it started
        assertEquals(20f, offset.getColDelta(), 1e-6);
        assertEquals(10f, offset.getRowDelta(), 1e-6);
    }

    public void testNothingMovesOutsideADrag() {
        PositionTransform.Translate offset = new PositionTransform.Translate(0f, 0f);
        DragMove move = DragMove.of(offset);
        move.moveTo(Vec2.xy(9, 9));
        assertEquals(0f, offset.getColDelta(), 1e-6);
        move.start(Vec2.xy(0, 0));
        move.stop();
        move.moveTo(Vec2.xy(9, 9));
        assertEquals("stopped: no more moves", 0f, offset.getColDelta(), 1e-6);
    }

    /** a thing that moves itself: it is told row, column (y, x) */
    static class Thing implements Movable {
        int row = 4, col = 7;

        @Override
        public void moveTo(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void moveDelta(int rowOffset, int colOffset) {
            moveTo(row + rowOffset, col + colOffset);
        }
    }

    public void testAThingThatMovesItselfIsToldItsNewRowAndColumn() {
        Thing thing = new Thing();
        DragMove move = DragMove.of(thing, () -> Vec2.xy(thing.col, thing.row));
        move.start(Vec2.xy(0, 0));
        move.moveTo(Vec2.xy(3, -2));                         // 3 across, 2 up
        assertEquals(10, thing.col);
        assertEquals(2, thing.row);
    }
}
