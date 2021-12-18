package dtb.terminalIO.geom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.stream.Collectors;

import dtb.terminalIO.geom.Relation.LeftRight;
import dtb.terminalIO.geom.Relation.OverUnder;
import dtb.terminalIO.geom.position.Position;

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
	return new Position.Simple(this.getBottomMostRow(), this.getCol());
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
        return containsPosition(pos.getRow(), pos.getCol());
    }

    public default boolean containsPosition(int row, int col) {
        if (col < this.getCol())
            return false;
        if (row < this.getRow())
            return false;
        if (col > this.getRightMostCol())
            return false;
        if (row > this.getBottomMostRow())
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
         return new Position.Simple( ((this.getHeight() - d.getHeight() )/2) + this.getRow(), 
                                     ((this.getWidth()  - d.getWidth()  )/2) + this.getCol());
    }
    
    public default Position centered(Dimension d) {
        return centered(d, null);
   }
   
   public default Position centerPosition() {
        return centered(Dimension.ZERO, null);
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

    /**
     * returns intersection rectangle of this and other.
     * If no intersection, then return null
     */
    public default Rectangle intersectWith(Rectangle other) {
        int thisRow        = this.getRow();
        int thisCol        = this.getCol();
        int thisBottomRow  = this.getBottomMostRow();
        int thisRightCol   = this.getRightMostCol();
        
        int otherRow       = other.getRow();
        int otherCol       = other.getCol();
        int otherBottomRow = other.getBottomMostRow();
        int otherRightCol  = other.getRightMostCol();
        
        // get new corners
        int newRow       = Math.max(thisRow, otherRow);
        int newCol       = Math.max(thisCol, otherCol);
        int newBottomRow = Math.min(thisBottomRow, otherBottomRow);
        int newRightCol  = Math.min(thisRightCol, otherRightCol);

        // calc new dimensions
        int newHeight = newBottomRow - newRow + 1;
        int newWidth  = newRightCol - newCol  + 1;
        if (newHeight<1 || newWidth<1) {
            return null;
        }
        
        return new Simple(newRow, newCol, newHeight, newWidth);
    }
    
    /**
     * return a collection of "sub" rectangles of 'this' rectangle that
     * do not intersect with 'other' rectangle
     */
    public default Collection<Rectangle> difference(Rectangle other) {
        Rectangle intersection = this.intersectWith(other);
        if (intersection == null) {
           return Collections.singleton(this);
        }
        if (intersection.isSameDimension(this)) {
            return  Collections.emptyList();
        }

        Rectangle remainingPartOfMe = this;

        TreeSet<Rectangle> diffs = new TreeSet<Rectangle>(Position.UTIL.POS_COMPARE);

        // chop the top off
        if (remainingPartOfMe.getRow() < other.getRow()) {
            diffs.add(new Simple(remainingPartOfMe.getRow(), remainingPartOfMe.getCol(), other.getRow()-this.getRow(), this.getWidth()));
            remainingPartOfMe = new Simple(other.getRow(), remainingPartOfMe.getCol(), remainingPartOfMe.getBottomMostRow() - other.getRow() + 1, this.getWidth());
        }
        // chop the bottom off
        int otherBottomMostRow = other.getBottomMostRow();
        if (remainingPartOfMe.getBottomMostRow() > otherBottomMostRow) {
            diffs.add(new Simple(otherBottomMostRow+1, remainingPartOfMe.getCol(), remainingPartOfMe.getBottomMostRow() - otherBottomMostRow, this.getWidth()));
            remainingPartOfMe =  new Simple(remainingPartOfMe.getRow(), remainingPartOfMe.getCol(), other.getBottomMostRow() - remainingPartOfMe.getRow() + 1, remainingPartOfMe.getWidth());
        }
        // chop the left off
        if (remainingPartOfMe.getCol() < other.getCol()) {
            diffs.add(new Simple(remainingPartOfMe.getRow(), remainingPartOfMe.getCol(), remainingPartOfMe.getHeight(), other.getCol() - remainingPartOfMe.getCol() ));
            remainingPartOfMe =  new Simple(remainingPartOfMe.getRow(), other.getCol(), remainingPartOfMe.getHeight(), remainingPartOfMe.getRightMostCol() - other.getCol() + 1);
        }

        // chop the right off
        int otherRightMostCol = other.getRightMostCol();
        if (remainingPartOfMe.getRightMostCol() > otherRightMostCol) {
            diffs.add(new Simple(remainingPartOfMe.getRow(), otherRightMostCol + 1, remainingPartOfMe.getHeight(), remainingPartOfMe.getRightMostCol() - otherRightMostCol));
            remainingPartOfMe =  new Simple(remainingPartOfMe.getRow(), remainingPartOfMe.getCol(), remainingPartOfMe.getHeight(), otherRightMostCol - remainingPartOfMe.getCol()+1);
        }

        if (!intersection.isSameRectangle(remainingPartOfMe)) {
            
            StringJoiner sj = new StringJoiner("\n");
            for (Rectangle diff : diffs) {
                sj.add(String.valueOf(diff));
            }
            throw new IllegalStateException("Rectangle.difference(..) error\n"+String.valueOf(intersection) + "\n not the same as \n" + String.valueOf(remainingPartOfMe)+"\n"+
                                            "original input:\n"+String.valueOf(this) + "\n" + String.valueOf(other) +"\n\nDIFFS\n-----\n"+sj);
        }

        return diffs;

    }
    
    
    public default <T extends Rectangle> boolean overlapsWith(T other) {
        return Rectangle.UTIL.hasOverlap(this, other);
    }

    public default boolean isSameRectangle(Rectangle other) {
        return this.isSameLocation(other) && this.isSameDimension(other);
    }

    public static final Simple ZERO = new Simple(0, 0, 0, 0);

    public static final Simple MAX_SIZE = new Simple(Short.MIN_VALUE, Short.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);

    /**
     * Simple implementation of {@link Dimension}
     */
    public static class Simple extends Dimension.Simple implements Rectangle {

        @Override
        public String toString() {
            return "Rectangle [row=" + row + ", col=" + col  + ", height=" + getHeight() + ", width=" + getWidth() + "]";
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

    /**
     * A Rectangle that is clipped by another Rectangle
     */
    public static class Clipped extends Delegate {
        Rectangle clippingArea;

        public Clipped(Rectangle clippingArea, Rectangle rectangleToClip) {
            super(rectangleToClip);
            this.clippingArea = clippingArea;
        }

        public static Clipped fromPosition(Position pos, Rectangle rectangleToClip) {
            if (!rectangleToClip.containsPosition(pos)) {
                return null;
            }
            Rectangle clippingArea = new Simple(pos.getRow(), 
                                                pos.getCol(), 
                                                rectangleToClip.getBottomMostRow() - pos.getRow() + 1,
                                                rectangleToClip.getRightMostCol() - pos.getCol() + 1);

            return new Clipped(clippingArea, rectangleToClip);
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

        public static Rectangle calcDimensionsFromPositions(Collection<? extends Position> positions) {
            if (positions == null || positions.size() == 0) {
                return Rectangle.ZERO;
            }
            return calcDimensionsFromPositions(positions.toArray(new Position[] {}));
        }

        public static Rectangle calcDimensionsFromPositions(Position ... positions) {
            if (positions == null || positions.length == 0) {
                return Rectangle.ZERO;
            }

            int minCol = Integer.MAX_VALUE;
            int maxCol = Integer.MIN_VALUE;
            int minRow = Integer.MAX_VALUE;
            int maxRow = Integer.MIN_VALUE;

            for (Position position : positions) {
                minCol = Math.min(minCol, position.getCol());
                maxCol = Math.max(maxCol, position.getCol());
                minRow = Math.min(minRow, position.getRow());
                maxRow = Math.max(maxRow, position.getRow());
            }
            return new Rectangle.Simple(minRow,minCol, (maxRow - minRow)+1  ,(maxCol - minCol)+1);
        }

        public static <T extends Rectangle> boolean hasOverlap(T r1, T r2) {
            
            boolean overlap = r2.getCol() <= r1.getRightMostCol() &&
                              r1.getCol() <= r2.getRightMostCol() &&
                              r2.getBottomMostRow() >= r1.getRow() &&
                              r1.getBottomMostRow() >= r2.getRow() ;
            
            return overlap;
        }
        
        /**
         * returns true if rectangle r1 overlaps r2
         * @param r1
         * @param r2
         * @return
         */
        public static <T extends Rectangle> boolean hasOverlapSlower(T r1, T r2) {
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
        
        /**
         * return a list of "sub" rectangles of 'this' rectangle that
         * do not intersect with 'other' rectangle
         */
        public static Collection<RectangleAndSubRectangles> differences(List<Rectangle> rectangles) {
            
            if (rectangles == null) {
                throw new NullPointerException("bad param");
            }
            
            int size = rectangles.size();
            
            if (size <= 1) {
                return Collections.emptyList();
            }

            List<RectangleAndSubRectangles> result = rectangles.subList(1, size).stream().map(r -> new RectangleAndSubRectangles(r)).collect(Collectors.toList());

            for (int i=0; i<(size-1); i++) {
                Rectangle r1 = rectangles.get(i);
                for (int j=i; j<(size-1); j++) {
                    RectangleAndSubRectangles rs = result.get(j);
                    rs.splitAndAppendToSubsListUsing(r1);
                }
            }
            
            return result;
        }

    }
    
    public static class RectangleAndSubRectangles {
        Rectangle rectangle;
        List<Rectangle> subs;
        
        public RectangleAndSubRectangles(Rectangle rectangle) {
            super();
            this.rectangle = rectangle;
        }
        void add(Rectangle r) {
            subs.add(r);
        }
        
        public boolean hasSubs() {
            return (subs != null) && (subs.size()>0);
        }
        public Rectangle getRectangle() {
            return rectangle;
        }

        public List<Rectangle> getSubs() {
            if (subs == null) {
                throw new IllegalStateException("Do not call getSubs() before calling splitAndAppendToSubsListUsing()");
            }
            return subs;
        }
        
        public boolean isFullyHidden() {
            if (subs == null) {
                throw new IllegalStateException("Do not call isFullyHidden() before calling splitAndAppendToSubsListUsing()");
            }
            return subs.size() == 0;
        }
        
        public void splitAndAppendToSubsListUsing(Rectangle other) {
            
            if (subs == null) {
                Collection<Rectangle> diff = rectangle.difference(other);
                if (diff != null) {
                    subs = new ArrayList<Rectangle>(diff);
                }
                return;
            }
            
            List<Rectangle> newSubs = new ArrayList<Rectangle>(subs.size()+10);

            for (Rectangle sub : subs) {
                Collection<Rectangle> diff = sub.difference(other);
                if (diff != null && diff.size()>0) {
                    newSubs.addAll(diff);
                }
            }
            this.subs = newSubs;
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
