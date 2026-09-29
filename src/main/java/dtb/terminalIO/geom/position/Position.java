package dtb.terminalIO.geom.position;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

import dtb.terminalIO.geom.Movable;
import dtb.terminalIO.geom.Relation.LeftRight;
import dtb.terminalIO.geom.Relation.OverUnder;

/**
 * Represents a location ( row/col ) on a Canvas, or component.
 */
public interface Position extends Vec2<Position>, Row, Column{

    public default boolean isSameLocation(Position other) {
        if (other == null) {
            return false;
        }
        return this.getY() == other.getY() && 
               this.getX() == other.getX();
    }

    @SuppressWarnings("unchecked")
    public default <T extends Position> LeftRight<T> leftRight(T right) {
        return Position.UTIL.leftRight((T) this, (T) right);
    }

    @SuppressWarnings("unchecked")
    public default <T extends Position> OverUnder<T> overUnder(T under) {
        return Position.UTIL.overUnder((T) this, (T) under);
    }
    
    public default Position newFrom(float x, float y) {
        return new Position.Simple(y, x);
    }

    public default Position offsetToRowCol(int row, int col) {
        return new Position.Simple(row - this.getY(), col - this.getX());
    }

    public default Position offsetAdd(int row, int col) {
        return add((float)col, (float)row);
    }

    public default Position offsetAdd(Position offset) {
        return offsetAdd(offset.getRow(), offset.getCol());
    }

    public default Position addCols(int cols) {
        return new Position.Simple(this.getY(), this.getX()+cols);
    }

    public default Position addRows(int rows) {
        return new Position.Simple(this.getY() + rows, this.getX());
    }

    public default float getX() {
        return getCol();
    }

    public default float getY() {
        return getRow();
    }

    public default Position negate() {
        return new Position.Simple(this.getY()*-1, this.getX()*-1);
    }

    /** Position offset 0  (0, 0)*/
    public static final Position ZERO = new Simple(0, 0);

    /** Position offset to the left by one position (0, -1) */
    public static final Position LEFT_BY_1 = new Simple(0, -1);

    /** Position offset to the right by one position (0, 1) */
    public static final Position RIGHT_BY_1 = new Simple(0, 1);

    /** Position offset down by one position (1, 0) */
    public static final Position DOWN_BY_1 = new Simple(1, 0);

    /** Position offset up by one position (-1, 0) */
    public static final Position UP_BY_1 = new Simple(-1, 0);

    /** Position offset down by one and right by one position (1, 1) */
    public static final Position DOWN_BY_1_AND_RIGHT_BY_1 = new Simple(1, 1);

    /**
     * Simple implementation of {@link Position}
     */
    public static class Simple implements Position, Movable {
        public float y;
        public float x;

        // for historical API reasons row, col (y,x) is reversed from the more traditional (x,y)
        public Simple(float row, float col) {
            super();
            this.y = row;
            this.x = col;
        }
        public Simple(Position pos) {
            this(pos.getRow(), pos.getCol());
        }

        public Simple(int row, int col) {
            this((float)row,(float) col);
        }
        /**
         * Creates a new (simple) position from pos parameter
         * @param pos
         */
        public Simple(Vec2<?> pos) {
            this(pos.getY(), pos.getX());
        }
    
        public int getRow() {
            return (int)y;
        }
    
        public int getCol() {
            return (int)x;
        }
    
        public float getX() {
            return x;
        }
        
        public float getY() {
            return y;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int)x;
            result = prime * result + (int)y;
            return result;
        }
    
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
            return true;
            if (obj == null)
            return false;
            if (getClass() != obj.getClass())
            return false;
            Simple other = (Simple) obj;
            if (x != other.x)
            return false;
            if (y != other.y)
            return false;
            return true;
        }

        @Override
        public void moveTo(int row, int col) {
            this.y = row;
            this.x = col;
        }

        @Override
        public void moveDelta(int rowOffset, int colOffset) {
            this.y += rowOffset;
            this.x += colOffset;
        }

        @Override
        public String toString() {
            return UTIL.asString(this);
        }

        public static Position withOffset(Position position, Position offset) {
            Position.Simple newPos = new Simple(position);
            newPos.moveDelta(offset.getRow(), offset.getCol());
            return newPos;
        }

    }

    public static class PositionComparator implements Comparator<Position> {
        @Override
        public int compare(Position pos1, Position pos2) {
            if (pos1 == pos2)
                return 0;
            // beware trying to compare using getY (float) - results are not desirable
            //   so use getRow() ( int ) ... do the same thing for getCol()
            int cmp = Integer.compare(pos1.getRow(), pos2.getRow());
            if (cmp != 0)
                return cmp;
            return Integer.compare(pos1.getCol(), pos2.getCol());
        }
    }

    public static class UTIL {

        public static final PositionComparator POS_COMPARE = new PositionComparator();

        public static String asString(Position position) {
            if (position == null) {
                return "Position[null, null]";
            }
            return String.format("Position[%4d, %4d]", position.getRow(), position.getCol());
        }

        public static String asCoordinateString(Vec2<?> position) {
            if (position == null) {
                return "Position[null, null]";
            }
            return String.format("Position[%4.2f, %4.2f]", position.getX(), position.getY());
        }

        public static <T extends Position> LeftRight<T> leftRight(T p1, T p2) {
            return new LeftRight<T>(p1, p2, (left, right) -> left.getCol() <= right.getCol());
        }

        public static <T extends Position> OverUnder<T> overUnder(T p1, T p2) {
            return new OverUnder<T>(p1, p2, (over, under) -> over.getRow() <= under.getRow());
        }
    
        public static TreeSet<Position> makeCollection(Collection<Position> positions) {
            TreeSet<Position> ts = new TreeSet<Position>(POS_COMPARE);
            if (positions != null && positions.size() > 0) {
                ts.addAll(positions);
            }
            return ts;
        }
    } /* UTIL */

    public static Position fromXY(float x, float y) {
        return new Position.Simple(y, x);
    }

    public static Position fromRowCol(int row, int col) {
        return new Position.Simple(row, col);
    }

}
