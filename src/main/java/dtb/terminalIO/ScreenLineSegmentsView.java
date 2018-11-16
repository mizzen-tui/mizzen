package dtb.terminalIO;

import java.util.Collection;
import java.util.Iterator;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

/**
 * IScreenLineSegmentsBuffer that has a view into a 'source'
 * IScreenLineSegmentsBuffer object
 *
 */
public class ScreenLineSegmentsView extends IScreenLineSegmentsBuffer.Delegate {

    private IScreenLineSegmentsBuffer sourceSegments;
    //private ScreenLineSegmentsBuffer  viewSegments;

    private Position                  offsetIntoSource;
    private Dimension                 maxsize;

    public ScreenLineSegmentsView(IScreenLineSegmentsBuffer sourceSegments, Position offsetIntoSource, Dimension maxsize) {
        super(new ScreenLineSegmentsBuffer());
        this.sourceSegments = sourceSegments;
        //this.viewSegments = (ScreenLineSegmentsBuffer) this.getDelegate();
        this.offsetIntoSource = new Position.Simple(offsetIntoSource);
        this.maxsize = new Dimension.Simple(maxsize);
        popuateViewFromSource();
    }

    Rectangle rectangeFromOffsetAndDimension() {
        return new Rectangle.Simple(offsetIntoSource, maxsize);
    }
    

    public Position getOffsetIntoSource() {
        return offsetIntoSource;
    }

    public void setOffsetIntoSource(Position offsetIntoSource) {
        this.offsetIntoSource = offsetIntoSource;
        segmentsUpToDate = false;
    }

    private void popuateViewFromSource() {
        this.clear();
        Rectangle bounds = rectangeFromOffsetAndDimension();
        for (ScreenLineSegment sourceSegment : this.sourceSegments.getSegments()) {
            if (Rectangle.UTIL.hasOverlap(bounds, sourceSegment)) {
                this.add(new ScreenLineSegmentView(sourceSegment));
            }
        }
        segmentsUpToDate = true;
    }
    
    private void popuateViewFromSourceIfNeeded() {
        if (!segmentsUpToDate) {
            popuateViewFromSource();
        }
    }


    boolean segmentsUpToDate = false;
    
    @Override
    public Collection<ScreenLineSegment> getSegments() {
        popuateViewFromSourceIfNeeded();
        return super.getSegments();
    }

    
    @Override
    public Iterator<ScreenLineSegment> iterator() {
        popuateViewFromSourceIfNeeded();
        return super.iterator();
    }

    
    
    public ScreenLineSegmentsView(IScreenLineSegmentsBuffer sourceSegments, Rectangle bounds) {
        this(sourceSegments, bounds, bounds);
    }

    public ScreenLineSegmentsView(IScreenLineSegmentsBuffer sourceSegments, Dimension maxsize) {
        this(sourceSegments, Position.ZERO, maxsize);
    }

    class ScreenLineSegmentView implements ScreenLineSegment {

        ScreenLineSegment sourceSegment;

        ScreenLineSegmentView(ScreenLineSegment sourceSegment) {
            this.sourceSegment = sourceSegment;
        }

        public Color getColor() {
            return sourceSegment.getColors();
        }

        @Override
        public int getLength() {
            return this.getText().length();
        }

        @Override
        public int getRow() {
            int row = sourceSegment.getRow() - ScreenLineSegmentsView.this.offsetIntoSource.getRow();
            return row < 0 ? 0 : row;
        }

        @Override
        public int getCol() {
            int col = sourceSegment.getCol() - ScreenLineSegmentsView.this.offsetIntoSource.getCol();
            return col < 0 ? 0 : col;
        }

        public String toString() {
            return String.format("VS(row=%d,lcol=%d,rcol=%d,[id=%d]):%s", getRow(), getCol(), getRightMostCol(), getId(), getText());
        }

        @Override
        public Color getColors() {
            return sourceSegment.getColors();
        }

        @Override
        public void moveTo(int row, int col) {
            throw new IllegalStateException("No implemented yet!");
        }

        @Override
        public int getId() {
            return sourceSegment.getId();
        }

        @Override
        public String getText() {
            int start = 0;
            int src_col = sourceSegment.getCol();
            int offset_col = ScreenLineSegmentsView.this.offsetIntoSource.getCol();
            if (offset_col > src_col) {
                start = offset_col - src_col;
            }
            int src_width = sourceSegment.getWidth();
            int col = getCol();
            if (col + (src_width - start) > maxsize.getWidth()) {
                int endOffset = maxsize.getWidth() - getCol();
                return sourceSegment.getText().substring(start, endOffset);
            }
            return sourceSegment.getText().substring(start);
        }

        @Override
        public ScreenLineSegment fromOffset(Position offset) {
            return new SegmentImpl(getColor(), Position.Simple.withOffset((Position) this, offset), getId(), this.getText());
        }

    }

    

}
