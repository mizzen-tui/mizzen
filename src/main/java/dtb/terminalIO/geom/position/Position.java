package dtb.terminalIO.geom.position;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Movable;
import dtb.terminalIO.geom.Relation;
import dtb.terminalIO.geom.Relation.LeftRight;
import dtb.terminalIO.geom.Relation.OverUnder;

/**
 * Represents a location ( row/col ) on a Canvas, or component.
 */
public interface Position extends Row, Column{

    public default boolean isSamePosition(Position other) {
    if (other == null) {
        return false;
    }
	return this.getCol() == other.getCol() && 
	       this.getRow() == other.getRow();
    }
    
    @SuppressWarnings("unchecked")
    public default <T extends Position> LeftRight<T> leftRight(T right) {
	return Position.UTIL.leftRight((T) this, (T) right);
    }

    @SuppressWarnings("unchecked")
    public default <T extends Position> OverUnder<T> overUnder(T under) {
	return Position.UTIL.overUnder((T) this, (T) under);
    }
    

    public default Position offsetTo(Position target) {
        return offsetTo(target.getRow(), target.getCol());
    }
    
    public default Position offsetTo(int row, int col) {
        return new Position.Simple(row - this.getRow(), col - this.getCol());
    }

    public default Position offsetAdd(int row, int col) {
        return new Position.Simple(row + this.getRow(), col + this.getCol());
    }

    public default Position offsetAdd(Position offsetToAdd) {
        return new Position.Simple(offsetToAdd.getRow() + this.getRow(), offsetToAdd.getCol() + this.getCol());
    }

    public default Position negate() {
        return new Position.Simple(this.getRow()*-1, this.getCol()*-1);
    }

    public static final Position ZERO = new Simple(0, 0);

    /**
     * Simple implementation of {@link Dimension}
     */
    public static class Simple implements Position, Movable {
	int row;
	int col;

	public Simple(int row, int col) {
	    super();
	    this.row = row;
	    this.col = col;
	}

	public Simple(Position pos) {
	    this(pos.getRow(), pos.getCol());
	}

	public int getRow() {
	    return row;
	}

	public int getCol() {
	    return col;
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + col;
	    result = prime * result + row;
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
	    if (col != other.col)
		return false;
	    if (row != other.row)
		return false;
	    return true;
	}

	@Override
	public void moveTo(int row, int col) {
	    this.row = row;
	    this.col = col;
	}

	@Override
	public void moveDelta(int rowOffset, int colOffset) {
	    this.row += rowOffset;
	    this.col += colOffset;
	}

	@Override
	public String toString() {
	    return "Position [row=" + row + ", col=" + col + "]";
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
            int cmp = Integer.compare(pos1.getRow(), pos2.getRow());
            if (cmp != 0)
                return cmp;
            return Integer.compare(pos1.getCol(), pos2.getCol());
        }
    }
    
    public static class UTIL {
	
	public static final PositionComparator POS_COMPARE = new PositionComparator();

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
	
    }
    
    
    

}
