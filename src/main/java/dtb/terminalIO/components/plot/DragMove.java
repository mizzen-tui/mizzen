package dtb.terminalIO.components.plot;

import java.util.function.Consumer;
import java.util.function.Supplier;

import dtb.terminalIO.geom.Movable;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;

/**
 * Moving a thing by dragging it: it goes by as much as the mouse went since the drag started. It
 * needs only where the thing is and how to put it somewhere - in any units, as long as they match
 * the mouse positions given (grid dots, a plot's own numbers):
 * <pre>
 *     DragMove move = DragMove.of(shapesOffset);          // or new DragMove(where, putAt)
 *     ... drag start:  move.start(mouseAt);
 *     ... each drag:   move.moveTo(mouseAt);  redraw
 * </pre>
 * Positions are x, y (Vec2) throughout; the factories turn them into what the thing uses.
 */
public class DragMove {

    private final Supplier<Vec2<?>> where;
    private final Consumer<Vec2<?>> putAt;
    private Vec2<?> from;                                  // the mouse at the start; null: not moving
    private Vec2<?> startedAt;                             // the thing at the start

    /** where: where the thing is now; putAt: put it there */
    public DragMove(Supplier<Vec2<?>> where, Consumer<Vec2<?>> putAt) {
        this.where = where;
        this.putAt = putAt;
    }

    /**
     * A thing moved by a Translate in its transform (a shape): the Translate's column / row deltas
     * are the x / y.
     */
    public static DragMove of(PositionTransform.Translate offset) {
        return new DragMove(() -> Vec2.xy(offset.getColDelta(), offset.getRowDelta()),
                            (to) -> {
                                offset.setColDelta(to.getX());
                                offset.setRowDelta(to.getY());
                            });
    }

    /** a thing that moves itself (moveTo); where: where it is (x is its column, y its row) */
    public static DragMove of(Movable thing, Supplier<Vec2<?>> where) {
        return new DragMove(where, (to) -> thing.moveTo(Position.rowCol(to.getY(), to.getX())));
    }

    /** the drag starts with the mouse at at */
    public void start(Vec2<?> at) {
        this.from = at;
        this.startedAt = where.get();
    }

    public boolean isMoving() {
        return from != null;
    }

    /** the mouse is now at at: the thing goes by as much as the mouse did since start */
    public void moveTo(Vec2<?> at) {
        if (from == null) {
            return;
        }
        putAt.accept(Vec2.xy(startedAt.getX() + at.getX() - from.getX(),
                             startedAt.getY() + at.getY() - from.getY()));
    }

    public void stop() {
        from = null;
    }
}
