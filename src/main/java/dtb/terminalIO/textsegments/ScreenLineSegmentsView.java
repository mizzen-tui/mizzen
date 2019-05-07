package dtb.terminalIO.textsegments;

import java.util.Collection;
import java.util.Iterator;

import dtb.terminalIO.components.CanResize;
import dtb.terminalIO.components.DirtyFlag;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

/**
 * IScreenLineSegmentsBuffer that has a view into a 'source'
 * IScreenLineSegmentsBuffer object
 */
public class ScreenLineSegmentsView extends IScreenLineSegmentsBuffer.Delegate implements CanResize , DirtyFlag {

    private IScreenLineSegmentsBuffer.Provider sourceSegments;

    private Position                  offsetIntoSource;
    private Dimension                 maxsize;

    public ScreenLineSegmentsView(IScreenLineSegmentsBuffer.Provider sourceSegments, Position offsetIntoSource, Dimension maxsize) {
        super(new ClippedSegmentsBuffer());
        ((ClippedSegmentsBuffer)this.getDelegate()).segmentsView = this;
        this.sourceSegments = sourceSegments;
        this.offsetIntoSource = new Position.Simple(offsetIntoSource);
        this.maxsize = new Dimension.Simple(maxsize);
        ((ClippedSegmentsBuffer)this.getDelegate()).fullrecalcDim();
        populateViewFromSource();
    }

    // create a ScreenLineSegmentsBuffer with a custom fullrecalcDim() method 
    private static class ClippedSegmentsBuffer extends ScreenLineSegmentsBuffer {
        ScreenLineSegmentsView segmentsView;
        
        synchronized protected void fullrecalcDim() {
            this.preCalculatedRectangle = segmentsView.rectangeFromOffsetAndDimension();
            this.recalcNeeded = false;
        }
    }

    @Override
    public void resize(Dimension d) {
        this.maxsize = new Dimension.Simple(d);
        ((ClippedSegmentsBuffer)this.getDelegate()).fullrecalcDim();
        populateViewFromSource();
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
    
    public boolean isDirty() {
        return ! segmentsUpToDate;
    }

    
    public Position calculateScrollToViewOffset(Rectangle targetToView) {
	// sanity check input
	if (targetToView == null) {
	    return null;
	}

	if (Position.UTIL.POS_COMPARE.compare(Position.ZERO, targetToView) > 0) {
            return null;
	}

	if (!sourceSegments.getSegmentsBuffer().overlapsWith(targetToView)) {
	    return null;
	}

	Rectangle viewerRectangle = rectangeFromOffsetAndDimension();
	
	if (viewerRectangle.contains(targetToView)) {
	    return offsetIntoSource;
	}
	
	int rowOffset = offsetIntoSource.getRow();
	
	if ( ! viewerRectangle.containsRow(targetToView.getRow())) {
	    rowOffset = targetToView.getRow();
	}
	
	int colOffset = offsetIntoSource.getCol();
		
	if ( ((Dimension)viewerRectangle).contains((Dimension)targetToView) ) {
	    
	    if (targetToView.getCol() < viewerRectangle.getCol()) {
		colOffset = targetToView.getCol();
	    } else if (targetToView.getRightMostCol() > viewerRectangle.getRightMostCol()) {
		colOffset = offsetIntoSource.getCol() + (targetToView.getRightMostCol() - viewerRectangle.getRightMostCol());
	    }
	    
	} else {
	    colOffset = targetToView.getCol();
	}
	
	return new Position.Simple(rowOffset, colOffset);
    }    
    
    public void setOffsetIntoSource(Position offsetIntoSource) {
        this.offsetIntoSource = offsetIntoSource;
        segmentsUpToDate = false;
    }

    private void populateViewFromSource() {
        this.clear();
        Rectangle bounds = rectangeFromOffsetAndDimension();
        Collection<ScreenLineSegment> segments = this.sourceSegments.getSegmentsBuffer().getSegments();
       
        // TODO:   This loop below is crazy inefficient
        //
        //         Need to inspect segments type ( if treeset) then be smart about how to
        //         iterate of the set segments that are within the bounds
        //         use the floor and ceiling functions to find lower and upper bounds
        //         that we need to traverse over .. this might help speed up the iteration 
        
        int topRow = bounds.getRow();
        int bottomRow = bounds.getBottomMostRow();
        
        for (ScreenLineSegment sourceSegment : segments) {
            if (sourceSegment.getRow()< topRow) {
                continue;
            }
            if (Rectangle.UTIL.hasOverlap(bounds, sourceSegment)) {
                this.add(new ScreenLineSegmentView(sourceSegment));
            }
            if (sourceSegment.getRow()> bottomRow) {
                break;
            }
        }
        segmentsUpToDate = true;
    }
    
    private void populateViewFromSourceIfNeeded() {
        if (!segmentsUpToDate) {
            populateViewFromSource();
        }
    }

    boolean segmentsUpToDate = false;
    
    @Override
    public Collection<ScreenLineSegment> getSegments() {
        populateViewFromSourceIfNeeded();
        return super.getSegments();
    }
    
    @Override
    public Iterator<ScreenLineSegment> iterator() {
        populateViewFromSourceIfNeeded();
        return super.iterator();
    }
    
    public ScreenLineSegmentsView(IScreenLineSegmentsBuffer.Provider sourceSegments, Rectangle bounds) {
        this(sourceSegments, bounds, bounds);
    }

    public ScreenLineSegmentsView(IScreenLineSegmentsBuffer.Provider sourceSegments, Dimension maxsize) {
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

        int rememberedLength = -1; // will this become a problem?
        
        @Override
        public int length() {
            if (rememberedLength == -1) {
                rememberedLength = this.getText().length();
            }
            return rememberedLength;
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
            return String.format("VS(row=%d,lcol=%d,rcol=%d,[id=%d]):'%s'", getRow(), getCol(), getRightMostCol(), getId(), getText());
        }

        @Override
        public Color getColors() {
            return sourceSegment.getColors();
        }

        @Override
        public void moveTo(int row, int col) {
            rememberedLength = -1; // unremember my length
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
            if (rightCol >= sourceText.length()) {
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
