package dtb.terminalIO;

import java.util.Collection;
import java.util.Iterator;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

/**
 * Container for {@link ScreenLineSegment} objects
 * 
 * 
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

	public abstract ScreenLineSegment getAtCol(int row, int col);

	public abstract String getTextAt(int row, int col);

	public abstract String getCharAt(int row, int col);

	void setCharAt(int row, int col, char ch, Color color);
	
	public abstract boolean removeRow(int row);

	public abstract boolean removeRect(Rectangle r);

	public abstract Iterator<ScreenLineSegment> iterator();

	public Collection<ScreenLineSegment> getSegments();
	
	
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
	 * This is different form removeSegments(..)!! .. This will remove any
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
        public Collection<ScreenLineSegment> getSegments() {
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
	

}