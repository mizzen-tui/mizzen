package dtb.terminalIO;

import java.util.Collection;
import java.util.Iterator;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

/**
 * Container for {@link ScreenLineSegment} objects
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


	
	

}