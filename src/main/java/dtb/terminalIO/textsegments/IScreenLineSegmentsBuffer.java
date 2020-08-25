package dtb.terminalIO.textsegments;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

/**
 * Container for {@link ScreenLineSegment} objects
 * 
 * <p/>
 * 
 * This is the primary container for managing a collection of {@link ScreenLineSegment}'s.
 * 
 * <p/>
 * 
 * All more complex objects like the Canvas or any of the Component objects
 * will have a ScreenLineSegment objects encapsulated in this IScreenLineSegmentsBuffer.
 * 
 * <p/>
 * 
 * Most major operations on collections of ScreenLineSegments will happen through this
 * interface.
 * 
 */
public interface IScreenLineSegmentsBuffer extends Rectangle , Iterable<ScreenLineSegment>{

	public abstract boolean isEmpty();

	public abstract void clear();

	public abstract void add(ScreenLineSegment sc);

	public abstract void merge(IScreenLineSegmentsBuffer other);

	public abstract void mergeAt(int row, int col, IScreenLineSegmentsBuffer segmentsBuffer);

    public default void mergeAt(Position position, IScreenLineSegmentsBuffer segmentsBuffer) {
            mergeAt(position.getRow(), position.getCol(), segmentsBuffer);
    }

    public abstract void merge(Collection<ScreenLineSegment> segments);

    public abstract void clearAndMerge(Collection<ScreenLineSegment> segments);

	public abstract ScreenLineSegment getAtCol(int row, int col);

	public abstract String getTextAt(int row, int col);

	public abstract String getCharAt(int row, int col);

	void setCharAt(int row, int col, char ch, Color color);
	
	public abstract boolean removeRow(int row);

	public abstract boolean removeRect(Rectangle r);

	public abstract Iterator<ScreenLineSegment> iterator();

	public TreeSet<ScreenLineSegment> getSegments();
	
	/**
	 * A provider (or container) of a {@link IScreenLineSegmentsBuffer}
	 * Component objects and the Canvas are examples of IScreenLineSegmentsBuffer.Provider
	 * objects.
	 */
	public static interface Provider {
		IScreenLineSegmentsBuffer getSegmentsBuffer();
	}
	
	public static class SimpleProvider implements Provider {
		IScreenLineSegmentsBuffer segments;

		public SimpleProvider(IScreenLineSegmentsBuffer segments) {
			super();
			this.segments = segments;
		}

		@Override
		public IScreenLineSegmentsBuffer getSegmentsBuffer() {
			return segments;
		}

		public void setSegments(IScreenLineSegmentsBuffer segments) {
			this.segments = segments;
		}
		
	}

	/**
	 * Removes all segments in scs from this buffer 
	 */
	public abstract boolean removeSegments(Collection<ScreenLineSegment> scs);

	/***
	 * This is different from removeSegments(..)!! .. This will remove any
	 * rectangles from this buffer.  This is more expensive than removeSegments
	 * which will remove whole segments.
	 * 
	 * Lets say we have these list of segments ..
	 * 
	 *     (1)           (2)          (3)
	 * [01234567890][0123456789012][012345]
	 *         :                :
	 *         :                :   .. and we want to remove a rect that is located here
	 *         :                :  
	 *         [remove-this-rect]
	 *         :                :
	 *         :                :    After the remove of the rect, set of resulting
	 *         :                :    segments will look like this .. 
	 *         :                :  
	 *                               
	 * [0123456]                [2][012345]
	 *    (1)                   (2)   (3)   
	 *    
	 *    Notice how segment (1) and segment (2) have been clipped.
	 *    
	 * @param rectangles
	 */
	public abstract void removeRectangles(Collection<? extends Rectangle> rectangles);
	
	public abstract void fillRectWithChar(Color color, char fillChar,  Rectangle bounds);	
	
	public default void fillRectWithChar(Color color, char fillChar) {
		fillRectWithChar(color, fillChar, this);
	}

    void mergeAtBounded(int row, int col, Rectangle bounds, IScreenLineSegmentsBuffer segmentsBuffer);

    default void mergeAtBounded(Position pos, Rectangle bounds, IScreenLineSegmentsBuffer segmentsBuffer) {
        mergeAtBounded(pos.getRow(), pos.getCol(), bounds, segmentsBuffer);
    }

    
    
    public static class Delegate implements IScreenLineSegmentsBuffer {
        
        
        private IScreenLineSegmentsBuffer delegate;

        public Delegate(IScreenLineSegmentsBuffer delegate) {
            this.delegate = delegate;
        }
        
        public IScreenLineSegmentsBuffer getDelegate() {
            return delegate;
        }

        public void setDelegate(IScreenLineSegmentsBuffer delegate) {
            this.delegate = delegate;
        }

        @Override
        public int getCol() {
            return delegate.getCol();
        }

        @Override
        public int getRow() {
            return delegate.getRow();
        }

        @Override
        public int getWidth() {
            return delegate.getWidth();
        }

        @Override
        public int getHeight() {
            return delegate.getHeight();
        }

        @Override
        public void moveTo(int row, int col) {
            delegate.moveTo(row, col);
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public void add(ScreenLineSegment sc) {
            delegate.add(sc);
        }

        @Override
        public void merge(IScreenLineSegmentsBuffer other) {
            delegate.merge(other);
        }

        @Override
        public void mergeAt(int row, int col, IScreenLineSegmentsBuffer segmentsBuffer) {
            delegate.mergeAt(row, col, segmentsBuffer);
        }

        @Override
        public void merge(Collection<ScreenLineSegment> segments) {
            delegate.merge(segments);
        }

        @Override
        public void clearAndMerge(Collection<ScreenLineSegment> segments) {
            delegate.clearAndMerge(segments);
        }


        @Override
        public ScreenLineSegment getAtCol(int row, int col) {
            return delegate.getAtCol(row, col);
        }

        @Override
        public String getTextAt(int row, int col) {
            return delegate.getTextAt(row, col);
        }

        @Override
        public String getCharAt(int row, int col) {
            return delegate.getCharAt(row, col);
        }

        @Override
        public void setCharAt(int row, int col, char ch, Color color) {
            delegate.setCharAt(row, col, ch, color);
        }

        @Override
        public boolean removeRow(int row) {
            return delegate.removeRow(row);
        }

        @Override
        public boolean removeRect(Rectangle r) {
            return delegate.removeRect(r);
        }

        @Override
        public Iterator<ScreenLineSegment> iterator() {
            return delegate.iterator();
        }

        @Override
        public TreeSet<ScreenLineSegment> getSegments() {
            return delegate.getSegments();
        }

        @Override
        public boolean removeSegments(Collection<ScreenLineSegment> scs) {
            return delegate.removeSegments(scs);
        }

        @Override
        public void removeRectangles(Collection<? extends Rectangle> rectangles) {
            delegate.removeRectangles(rectangles);
        }

        @Override
        public void fillRectWithChar(Color color, char fillChar, Rectangle bounds) {
            delegate.fillRectWithChar(color, fillChar, bounds);
        }

        @Override
        public void mergeAtBounded(int row, int col, Rectangle bounds, IScreenLineSegmentsBuffer segmentsBuffer) {
            delegate.mergeAtBounded(row, col, bounds, segmentsBuffer);
            
        }
        
    }

    public static abstract class DelegateWithDirtyFlag extends Delegate {
        
        public DelegateWithDirtyFlag(IScreenLineSegmentsBuffer delegate) {
            super(delegate);
        }
        
        public abstract void markAsDirty();
        
        @Override
        public void moveTo(int row, int col) {
            super.moveTo(row, col);
            markAsDirty();
        }

        @Override
        public void clear() {
            super.clear();
            markAsDirty();
        }

        @Override
        public void add(ScreenLineSegment sc) {
            super.add(sc);
            markAsDirty();
        }

        @Override
        public void merge(IScreenLineSegmentsBuffer other) {
            super.merge(other);
            markAsDirty();
        }

        @Override
        public void mergeAt(int row, int col, IScreenLineSegmentsBuffer segmentsBuffer) {
            super.mergeAt(row, col, segmentsBuffer);
            markAsDirty();
        }

        @Override
        public void merge(Collection<ScreenLineSegment> segments) {
            super.merge(segments);
            markAsDirty();
        }

        @Override
        public void clearAndMerge(Collection<ScreenLineSegment> segments) {
            super.clearAndMerge(segments);
            markAsDirty();
        }

        @Override
        public void setCharAt(int row, int col, char ch, Color color) {
            super.setCharAt(row, col, ch, color);
            markAsDirty();
        }

        @Override
        public boolean removeRow(int row) {
            boolean result = super.removeRow(row);
            markAsDirty();
            return result;
        }

        @Override
        public boolean removeRect(Rectangle r) {
            boolean result = super.removeRect(r);
            markAsDirty();
            return result;
        }


        @Override
        public boolean removeSegments(Collection<ScreenLineSegment> scs) {
            boolean result = super.removeSegments(scs);
            markAsDirty();
            return result;
        }

        @Override
        public void removeRectangles(Collection<? extends Rectangle> rectangles) {
            super.removeRectangles(rectangles);
            markAsDirty();
        }

        @Override
        public void fillRectWithChar(Color color, char fillChar, Rectangle bounds) {
            super.fillRectWithChar(color, fillChar, bounds);
            markAsDirty();
        }

        @Override
        public void mergeAtBounded(int row, int col, Rectangle bounds, IScreenLineSegmentsBuffer segmentsBuffer) {
            super.mergeAtBounded(row, col, bounds, segmentsBuffer);
            markAsDirty();
        }
    }
    
    

}