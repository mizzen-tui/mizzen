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

    private Position                  offsetIntoSource;
    private Dimension                 maxsize;

    public ScreenLineSegmentsView(IScreenLineSegmentsBuffer sourceSegments, Position offsetIntoSource, Dimension maxsize) {
        super(new ClippedSegmentsBuffer());
        ((ClippedSegmentsBuffer)this.getDelegate()).segmentsView = this;
        this.sourceSegments = sourceSegments;
        this.offsetIntoSource = new Position.Simple(offsetIntoSource);
        this.maxsize = new Dimension.Simple(maxsize);
        ((ClippedSegmentsBuffer)this.getDelegate()).fullrecalcDim();
        popuateViewFromSource();
    }

    // create a ScreenLineSegmentsBuffer with a custom fullrecalcDim() method 
    private static class ClippedSegmentsBuffer extends ScreenLineSegmentsBuffer {
        ScreenLineSegmentsView segmentsView;
        
        synchronized protected void fullrecalcDim() {
            this.preCalulatedRectangle = segmentsView.rectangeFromOffsetAndDimension();
            this.recalcNeeded = false;
        }
    }
    
    Rectangle rectangeFromOffsetAndDimension() {
        return new Rectangle.Simple(offsetIntoSource, maxsize);
    }
    

    public Position getOffsetIntoSource() {
        return offsetIntoSource;
    }

    public void markAsDirty() {
        segmentsUpToDate = false;
    }
    
    public void setOffsetIntoSource(Position offsetIntoSource) {
        this.offsetIntoSource = offsetIntoSource;
        segmentsUpToDate = false;
    }

    boolean foundBALME = false;
    String sTest;
    String sSourceTest;
    
    private void popuateViewFromSource() {
        this.clear();
        Rectangle bounds = rectangeFromOffsetAndDimension();
        for (ScreenLineSegment sourceSegment : this.sourceSegments.getSegments()) {
            if (sourceSegment.getText().contains("BALME")) {
                foundBALME = true;
                ScreenLineSegmentView test = new ScreenLineSegmentView(sourceSegment);
                sTest = test.getText();
                sSourceTest = sourceSegment.getText();
                sTest = test.getText();
            } else {
                foundBALME = false;
            }
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
            // ech!!  and double-ech.. All of these checks make me see
            //                         a code smell..  When I get some time I 
            //                         need to simplify
            int start = 0;
            int src_col = sourceSegment.getCol();
            int offset_col = ScreenLineSegmentsView.this.offsetIntoSource.getCol();
            if (offset_col > src_col) {
                start = offset_col - src_col;
            }
            int rightCol = sourceSegment.getRightMostCol(); // - ScreenLineSegmentsView.this.offsetIntoSource.getCol();
            int viewerRightCol = ScreenLineSegmentsView.this.getRightMostCol();
            String sourceText = sourceSegment.getText();
            if (rightCol > sourceText.length()) {
                rightCol = sourceText.length();
            } else {
                if (rightCol > viewerRightCol  + offset_col ) {
                    rightCol = viewerRightCol + offset_col;
                }
                rightCol +=1; // +1 to include rightmost char
            }
            if (start<0 || start>rightCol) return "";
            String clippedText = sourceSegment.getText().substring(start, rightCol  );
            return clippedText;
        }

        @Override
        public ScreenLineSegment fromOffset(Position offset) {
            return new SegmentImpl(getColor(), Position.Simple.withOffset((Position) this, offset), getId(), this.getText());
        }

    }

    

}
