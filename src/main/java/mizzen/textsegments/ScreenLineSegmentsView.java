package mizzen.textsegments;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import mizzen.components.DirtyFlag;
import mizzen.geom.CanResize;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.util.ElapsedTime;

/**
 * IScreenLineSegmentsBuffer that has a view into a 'source'
 * IScreenLineSegmentsBuffer object
 */
public class ScreenLineSegmentsView extends IScreenLineSegmentsBuffer.Delegate implements CanResize, DirtyFlag {

    private IScreenLineSegmentsBuffer.Provider sourceSegments;

    private Position offsetIntoSource;
    private Dimension maxsize;

    public ScreenLineSegmentsView(IScreenLineSegmentsBuffer.Provider sourceSegments, 
                                  Position offsetIntoSource,
                                  Dimension maxsize) {
        super(new ClippedSegmentsBuffer());
        ((ClippedSegmentsBuffer) this.getDelegate()).segmentsView = this;
        this.sourceSegments = sourceSegments;
        this.offsetIntoSource = new Position.Simple(offsetIntoSource);
        this.resize( new Dimension.Simple(maxsize) );
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
        ((ClippedSegmentsBuffer) this.getDelegate()).fullrecalcDim();
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
        return !segmentsUpToDate;
    }

    public Position calculateScrollToViewOffset(Rectangle targetToView) {
        // sanity check input
        if (targetToView == null) {
            return null;
        }

        // target position is less than ZERO?
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

        if (offsetIntoSource.getRow() < targetToView.getRow()) {

            if (!viewerRectangle.containsRow(targetToView.getRow())
                    || !viewerRectangle.containsRow(targetToView.getBottomMostRow())) {

                if (viewerRectangle.getHeight() >= targetToView.getHeight()) {

                    int viewerRectangleBottomMostRow = viewerRectangle.getBottomMostRow();
                    int targetToViewBottomMostRow = targetToView.getBottomMostRow();

                    rowOffset = offsetIntoSource.getRow() - (viewerRectangleBottomMostRow - targetToViewBottomMostRow);

                } else {
                    rowOffset = targetToView.getRow();
                }

            }

        } else {

            if (!viewerRectangle.containsRow(targetToView.getRow())) {
                rowOffset = targetToView.getRow();
            }

        }

        int colOffset = offsetIntoSource.getCol();

        if (((Dimension) viewerRectangle).contains((Dimension) targetToView)) {

            if (targetToView.getCol() < viewerRectangle.getCol()) {
                colOffset = targetToView.getCol();
            } else if (targetToView.getRightMostCol() > viewerRectangle.getRightMostCol()) {
                colOffset = offsetIntoSource.getCol()
                        + (targetToView.getRightMostCol() - viewerRectangle.getRightMostCol());
            }

        } else {
            colOffset = targetToView.getCol();
        }

        return new Position.Simple(rowOffset, colOffset);
    }

    public boolean setOffsetIntoSource(Position offsetIntoSource) {
        if (offsetIntoSource!=null && offsetIntoSource.isSameLocation(this.offsetIntoSource)) {
            return false;
        }
        this.offsetIntoSource = offsetIntoSource;
        segmentsUpToDate = false;
        return true;
    }

    public static String DEBUG_STATS = "";
    
    private void populateViewFromSource() {

        DEBUG_STATS = "";
        
        this.clear();
        Rectangle bounds = rectangeFromOffsetAndDimension();

        Collection<ScreenLineSegment> segmentsFromSource = this.sourceSegments.getSegmentsBuffer().getSegments();
        
        int origSize = segmentsFromSource.size();

        if (segmentsFromSource instanceof TreeSet) {
            TreeSet<ScreenLineSegment> ts = (TreeSet<ScreenLineSegment>) segmentsFromSource;
            ScreenLineSegment first = ScreenLineSegment.UTIL.getAtRowCol(ts, bounds.getRow(), bounds.getCol());
            //ScreenLineSegment first = null;
            if (first != null) {
                ScreenLineSegment last = ScreenLineSegment.UTIL.getAtRowCol(ts, bounds.getBottomMostRow(),
                                                                            bounds.getRightMostCol());
                if (last != null) {
                    // really slow!!!! need to find another way to get subset
                    ElapsedTime e1 = new ElapsedTime();
                    segmentsFromSource = ts.subSet(first, true, last, true);
                    DEBUG_STATS += " ts size: " + ts.size()  + " ts.subset: " + segmentsFromSource.size();
                    DEBUG_STATS += " elapsed: " + e1.elapsed() ;
                }
            }
        }

        // TODO: This loop below is crazy inefficient
        //
        // Need to inspect segments type ( if treeset) then be smart about how to
        // iterate of the set segments that are within the bounds
        // use the floor and ceiling functions to find lower and upper bounds
        // that we need to traverse over .. this might help speed up the iteration

        int topRow = bounds.getRow();
        int bottomRow = bounds.getBottomMostRow();
        int leftCol = bounds.getCol();
        int rightCol = bounds.getRightMostCol();

        ElapsedTime e2 = new ElapsedTime();
        
        TreeSet<ScreenLineSegment> destinationSegmentsTree = super.getSegments();
        destinationSegmentsTree.clear();
        for (ScreenLineSegment sourceSegment : segmentsFromSource) {
            int segmentRow = sourceSegment.getRow();
            if (segmentRow < topRow) {
                continue;
            }
            if (segmentRow > bottomRow) {
                break;
            }

            if (sourceSegment.getCol() > rightCol) {
                continue;
            }

            if (sourceSegment.getRightMostCol() < leftCol) {
                continue;
            }

            // if (Rectangle.UTIL.hasOverlap(bounds, sourceSegment)) {
            destinationSegmentsTree.add(new ScreenLineSegmentView(sourceSegment));
            // (slow)this.add(new ScreenLineSegmentView(sourceSegment));
            // }
        }
        segmentsUpToDate = true;
        
        DEBUG_STATS += " | looptime : "+ e2.toString() + " | ORIG Size: " + origSize+"\n\nYYYYYYYYYYYYYYYYY\n";

        // Logging.info(DEBUG_STATS);
        
    }

    private void populateViewFromSourceIfNeeded() {
        if (!segmentsUpToDate) {
            populateViewFromSource();
        }
    }

    boolean segmentsUpToDate = false;

    @Override
    public TreeSet<ScreenLineSegment> getSegments() {
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

    class ScreenLineSegmentView extends AbstractSegmentView {

        ScreenLineSegmentView(ScreenLineSegment sourceSegment) {
            super(sourceSegment);
        }
        
        protected Position offset() {
            return ScreenLineSegmentsView.this.offsetIntoSource;
        }
        
        protected int getMaxRightMostCol() {
            return ScreenLineSegmentsView.this.getRightMostCol();
        }
        
    }
}
