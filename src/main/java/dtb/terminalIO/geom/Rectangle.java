package dtb.terminalIO.geom;

import java.util.Collection;
import java.util.Comparator;

import dtb.terminalIO.geom.Relation.LeftRight;
import dtb.terminalIO.geom.Relation.OverUnder;

/**
 * Represents a rectangle which means a location ({@link Position} (row/col) ) and
 * {@link Dimension} ( height/width)
 * 
 * <p/>
 * 
 * Components, canvas and ScreenLineSegments are all rectangles.
 * 
 * <p>
 * 
 * See {@link Dimension} and {@link Position}
 * 
 */
public interface Rectangle extends Position, Dimension, Movable {

    public default Position getPostion_TopLeft() {
	return this;
    }

    public default Position getPostion_TopRight() {
	return new Position.Simple(this.getRow(), this.getRightMostCol());
    }

    public default Position getPostion_BottomLeft() {
	return new Position.Simple(this.getBottomMostRow(), this.getRow());
    }

    public default Position getPostion_BottomRight() {
	return new Position.Simple(this.getBottomMostRow(), this.getRightMostCol());
    }
    
    public default int getRightMostCol() {
        int col = getCol();
        int rightMost = col + getWidth() - 1;
        if (rightMost < col) {
            rightMost = col;
        }
        return rightMost;
    }

    public default int getBottomMostRow() {
        int row = getRow();
        int bottomMost = row + getHeight() - 1;
        if (bottomMost < row) {
            bottomMost = row;
        }
        return bottomMost;
    }

    public default void moveDelta(int rowOffset, int colOffset) {
        moveTo(this.getRow() + rowOffset, this.getCol() + colOffset);
    }

    public default boolean containsPosition(Position pos) {
        if (pos.getCol() < this.getCol())
            return false;
        if (pos.getRow() < this.getRow())
            return false;
        if (pos.getCol() > this.getRightMostCol())
            return false;
        //int botomRow = this.getBottomMostRow();
        if (pos.getRow() > this.getBottomMostRow())
            return false;
        return true;
    }

    public default boolean contains(Rectangle other) {
        if (other.getCol() < this.getCol())
            return false;
        if (other.getRow() < this.getRow())
            return false;
        if (other.getRightMostCol() > this.getRightMostCol())
            return false;
        if (other.getBottomMostRow() > this.getBottomMostRow())
            return false;
        return true;
    }

    public default Position centered(Dimension d, Position defaultPos) {
         if (!contains(d)) {
             return defaultPos;
         }
         return new Position.Simple( (this.getHeight() - d.getHeight() )/2, (this.getWidth() - d.getWidth()) /2 );
    }
    
    public default Position centered(Dimension d) {
        return centered(d, null);
   }
    
    /**
     * Does rectangle contain row
     */
    public default boolean containsRow(int row) {
        if (row < this.getRow())
            return false;
        if (row > this.getBottomMostRow())
            return false;
        return true;
    }

    /**
     * Does rectangle contain column (col)
     */
    public default boolean containsCol(int col) {
        if (col < this.getCol())
            return false;
        if (col > this.getRightMostCol())
            return false;
        return true;
    }

    /**
     * Returns a row of a rectangle as a rectangle. Think of it as a "view" into one
     * row of a rectangle.. where a row is just a rectangle of height 1
     * 
     * @param row
     * @return
     */
    public default Rectangle rectangleAtRow(final int row) {
        if (!this.containsRow(row))
            throw new IllegalArgumentException("Bad row specified");
        final int rowOffset = row - this.getRow();
        return new Delegate(this) {
            @Override
            public int getRow() {
                return getRectangle().getRow() + rowOffset;
            }

            @Override
            public int getWidth() {
                return getRectangle().getWidth();
            }

            @Override
            public int getHeight() {
                return 1;
            }
        };
    }

    /**
     * TODO: This method is not well designed/implemented .. need to rethink it -
     * dont use until this is done - then erase this comment -- I forget what the issue is!  :-)
     * 
     * @return
     */
    public default Rectangle pad(int row, int col, int height, int width) {
        Simple newRect = new Simple(this.getRow() + row, this.getCol() + col, this.getHeight() + height, this.getWidth() + width);
        return newRect;
    }

    public default Rectangle padTop(int padAmount) {
        return this.pad(padAmount, 0, 0, 0);
    }

    public default Rectangle mergeAndExpandDimensionsWith(Rectangle other) {
        return new Simple(this).mergeAndExpandDimensionsWithMySelf(other);
    }

    public default Rectangle mergeAndExpandDimensionsWith(Collection<? extends Rectangle> others) {
        Rectangle.Simple result = new Simple(this);
        for (Rectangle other : others) {
            result.mergeAndExpandDimensionsWithMySelf(other);
        }
        return result;
    }

    public default <T extends Rectangle> boolean overlapsWith(T other) {
        return Rectangle.UTIL.hasOverlap(this, other);
    }

    public static final Simple ZERO = new Simple(0, 0, 0, 0);

    public static final Simple MAX_SIZE = new Simple(Short.MIN_VALUE, Short.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    /**
     * Simple implementation of {@link Dimension}
     */
    public static class Simple extends Dimension.Simple implements Rectangle {

        @Override
        public String toString() {
            return "Simple [row=" + row + ", lcol" + col + ", rcol=" + getRightMostCol() + " width=" + getWidth() + ", height=" + getHeight() + "]";
        }

        int col;
        int row;

        public Simple(int row, int col, int height, int width) {
            super(height, width);
            moveTo(row, col);
        }

        public Simple(int row, int col, Dimension d) {
            super(d.getHeight(), d.getWidth());
            moveTo(row, col);
        }

        public Simple(Rectangle r) {
            this(r.getRow(), r.getCol(), r.getHeight(), r.getWidth());
        }

        public Simple(Position p, Dimension d) {
            this(p.getRow(), p.getCol(), d.getHeight(), d.getWidth());
        }

        public Simple(Dimension d) {
            this(Position.ZERO.getRow(), Position.ZERO.getCol(), d.getHeight(), d.getWidth());
        }

        @Override
        public int getCol() {
            return col;
        }

        public void setCol(int col) {
            this.col = col;
        }

        @Override
        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public Rectangle mergeAndExpandDimensionsWithMySelf(Rectangle other) {
            
            int thisRow        = this.getRow();
            int thisCol        = this.getCol();
            int thisBottomRow  = this.getBottomMostRow();
            int thisRightCol   = this.getRightMostCol();
            
            int otherRow       = other.getRow();
            int otherCol       = other.getCol();
            int otherBottomRow = other.getBottomMostRow();
            int otherRightCol  = other.getRightMostCol();
            
            // get new corners
            int newRow       = Math.min(thisRow, otherRow);
            int newCol       = Math.min(thisCol, otherCol);
            int newBottomRow = Math.max(thisBottomRow, otherBottomRow);
            int newRightCol  = Math.max(thisRightCol, otherRightCol);

            // calc new dimensions
            int newHeight = newBottomRow - newRow + 1;
            int newWidth  = newRightCol - newCol  + 1;
            
            // now set
            setRow(newRow);
            setCol(newCol);
            setHeight(newHeight);
            setWidth(newWidth);
            
//            
//            setRow(Math.min(this.getRow(), other.getRow()));
//
//            setCol(Math.min(this.getCol(), other.getCol()));
//
//            setHeight(Math.max(this.getBottomMostRow(), other.getBottomMostRow()) - getRow() + 1);
//            setWidth(Math.max(this.getRightMostCol(), other.getRightMostCol()) - getCol() + 1);

            return this;
        }

        @Override
        public void moveTo(int row, int col) {
            this.setRow(row);
            this.setCol(col);
        }

    }
    
    public static abstract class AbstractDelegate implements Rectangle {

        public abstract Rectangle getRectangle();

        @Override
        public int getCol() {
            return getRectangle().getCol();
        }

        @Override
        public int getRow() {
            return getRectangle().getRow();
        }

        @Override
        public int getWidth() {
            return getRectangle().getWidth();
        }

        @Override
        public int getHeight() {
            return getRectangle().getHeight();
        }

        @Override
        public void moveTo(int row, int col) {
            getRectangle().moveTo(row, col);
        }

        @Override
        public void moveDelta(int rowOffset, int colOffset) {
            getRectangle().moveDelta(rowOffset, colOffset);
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + toStringDetails();
        }

        public String toStringDetails() {
            return "[row=" + getRow() + ", lcol" + getCol() + ", rcol=" + getRightMostCol() + " width="
                    + getWidth() + ", height=" + getHeight() + "]";
        }
    }
    

    public static class Delegate extends AbstractDelegate {
        Rectangle rectangle;

        public Delegate(Rectangle rectangle) {
            super();
            this.rectangle = rectangle;
        }

        public void setRectangle(Rectangle rectangle) {
            this.rectangle = rectangle;
        }

        public Rectangle getRectangle() {
            return rectangle;
        }

    }

    public static class Offset extends Delegate {
        Position position;

        public Offset(Position position, Rectangle rectangle) {
            super(rectangle);
            this.position = position;
        }

        public Offset(int row, int col, Rectangle rectangle) {
            this(new Position.Simple(row, col), rectangle);
        }

        @Override
        public int getCol() {
            return getRectangle().getCol() + position.getCol();
        }

        @Override
        public int getRow() {
            return getRectangle().getRow() + position.getCol();
        }

    }

    public static class Clipped extends Delegate {
        Rectangle clippingArea;

        public Clipped(Rectangle clippingArea, Rectangle rectangle) {
            super(rectangle);
            this.clippingArea = clippingArea;
        }

        public static Clipped fromPosition(Position pos, Rectangle rectangle) {
            if (!rectangle.containsPosition(pos)) {
                return null;
            }
            Rectangle clippingArea = new Simple(pos.getRow(), pos.getCol(), rectangle.getBottomMostRow() - pos.getRow() + 1,
                    rectangle.getRightMostCol() - pos.getCol() + 1);

            return new Clipped(clippingArea, rectangle);
        }

        public static Clipped fromPosition(int row, int col, Rectangle rectangle) {
            return fromPosition(new Position.Simple(row, col), rectangle);
        }

        @Override
        public int getRow() {
            int computed = getRectangle().getRow();
            if (computed < clippingArea.getRow())
                computed = clippingArea.getRow();
            return computed;
        }

        @Override
        public int getCol() {
            int computed = getRectangle().getCol();
            if (computed < clippingArea.getCol())
                computed = clippingArea.getCol();
            return computed;
        }

        private int __getRightMostCol() {
            int computed = getRectangle().getRightMostCol();
            if (computed > clippingArea.getRightMostCol())
                computed = clippingArea.getRightMostCol();
            return computed;
        }

        public int getRightMostCol() {
            return __getRightMostCol();
        }

        private int __getBottomMostRow() {
            int computed = getRectangle().getBottomMostRow();
            if (computed > clippingArea.getBottomMostRow())
                computed = clippingArea.getBottomMostRow();
            return computed;
        }

        public int getBottomMostRow() {
            return __getBottomMostRow();
        }

        @Override
        public int getWidth() {
            int computed = __getRightMostCol() - getCol() + 1;
            return computed >= 0 ? computed : 0;
        }

        @Override
        public int getHeight() {
            int computed = __getBottomMostRow() - getRow() + 1;
            return computed >= 0 ? computed : 0;
        }

    }

    public static class UTIL {

        /**
         * given a list of Rectangles ( usually in the form of components), return
         * a new Rectangle that encloses all of the Rectangles
         */
        public static Rectangle calcDimensionsWith(Collection<? extends Rectangle> others) {
            Rectangle.Simple result = new Simple(ZERO); // start at zero size
            for (Rectangle other : others) {
                result.mergeAndExpandDimensionsWithMySelf(other);  // now expand its size
            }
            return result;
        }

        /**
         * returns true if rectangle r1 overlaps r2
         * @param r1
         * @param r2
         * @return
         */
        public static <T extends Rectangle> boolean hasOverlap(T r1, T r2) {
            LeftRight<T> lr = Position.UTIL.leftRight(r1, r2);
            if (lr.getLeft().getRightMostCol() >= lr.getRight().getCol()) {
                OverUnder<T> ou = Position.UTIL.overUnder(r1, r2);
                boolean result = ou.getOver().getBottomMostRow() >= ou.getUnder().getRow();
                return result;
            }
            return false;
        }

        // returns true if r1 and r2 right beside each other with no gap
        public static <T extends Rectangle> boolean areBesideEachOther(T r1, T r2) {
            LeftRight<T> lr = Position.UTIL.leftRight(r1, r2);
            if (lr.getLeft().getRightMostCol() == lr.getRight().getCol() - 1) {
                OverUnder<T> ou = Position.UTIL.overUnder(r1, r2);
                boolean result = ou.getOver().getBottomMostRow() >= ou.getUnder().getRow();
                return result;
            }
            return false;
        }

    }

    public static class RectangleComparator implements Comparator<Rectangle> {
        @Override
        public int compare(Rectangle r1, Rectangle r2) {
            if (r1 == r2)
                return 0;
            int cmp = Integer.compare(r1.getRow(), r2.getRow());
            if (cmp != 0)
                return cmp;
            cmp = Integer.compare(r1.getCol(), r2.getCol());
            return cmp;
        }
    }
    
    public static RectangleComparator DEFAULT_COMPARATOR = new RectangleComparator();

}
