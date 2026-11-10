package mizzen.geom.position;

import java.util.ArrayList;
import java.util.List;

/**
 * A Position that delegates to one of several registered positions.
 *
 * </p>
 * 
 * Positions are zero-indexed. The active position can be changed
 * without replacing this object in the parent component.
 */
public class PositionSelector implements Position {

    private final List<Position> positions = new ArrayList<>();
    private int activeIndex = -1;

    public PositionSelector add(Position position) {
        if (position == null) {
            throw new NullPointerException("position");
        }

        positions.add(position);

        if (activeIndex == -1) {
            activeIndex = 0;
        }

        return this;
    }

    public PositionSelector activate(int index) {
        if (index < 0 || index >= positions.size()) {
            throw new IndexOutOfBoundsException(
                    "Invalid position index: " + index);
        }

        activeIndex = index;
        return this;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public Position getActivePosition() {
        if (activeIndex == -1) {
            throw new IllegalStateException("No positions registered");
        }
        return positions.get(activeIndex);
    }

    public int size() {
        return positions.size();
    }

    @Override
    public int getRow() {
        return getActivePosition().getRow();
    }

    @Override
    public int getCol() {
        return getActivePosition().getCol();
    }
}