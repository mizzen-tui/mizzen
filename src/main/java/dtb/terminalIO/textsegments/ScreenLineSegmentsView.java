package dtb.terminalIO.textsegments;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import dtb.terminalIO.components.CanResize;
import dtb.terminalIO.components.DirtyFlag;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;

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
        if (offsetIntoSource!=null && offsetIntoSource.isSamePosition(this.offsetIntoSource)) {
            return false;
        }
        this.offsetIntoSource = offsetIntoSource;
        segmentsUpToDate = false;
        return true;
    }

    public static String DEBUG_STATS = "";
    
    private void populateViewFromSource() {

        DEBUG_STATS = "\n\nXXXXXXXXXXXXXXXXXXXXXXXXXXXX\npopulateViewFromSource: ";
        
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

    class ScreenLineSegmentView implements ScreenLineSegment {

        ScreenLineSegment sourceSegment;

        ScreenLineSegmentView(ScreenLineSegment sourceSegment) {
            this.sourceSegment = sourceSegment;
        }

        public Color getColor() {
            return sourceSegment.getColors();
        }

        int rememberedLength = -1; // will this become a problem?
        String _clippedText = null;

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
            return String.format("VS(row=%d,lcol=%d,rcol=%d,[id=%d]):'%s'", getRow(), getCol(), getRightMostCol(),
                    getId(), getText());
        }

        @Override
        public Color getColors() {
            return sourceSegment.getColors();
        }

        @Override
        public void moveTo(int row, int col) {
            rememberedLength = -1; // unremember my length
            _clippedText = null;
            throw new IllegalStateException("No implemented yet!");
        }

        @Override
        public int getId() {
            return sourceSegment.getId();
        }

        @Override
        public String getText() {

            if (_clippedText != null) {
                return _clippedText.substring(0,_clippedText.length()-1)+"X";
            }

            // ech!! and double-ech.. All of these checks make me see
            // a code smell.. When I get some time I
            // need to simplify

            //char[] DEBUGCHARS = new char[] {'.','.','.','.','|'}; 

            int start = 0;
            int src_col = sourceSegment.getCol();
            int offset_col = ScreenLineSegmentsView.this.offsetIntoSource.getCol();
            int rightColOffsetClipLen = 0;
            if (offset_col > src_col) {
                start = offset_col - src_col;
                rightColOffsetClipLen = start;
                //DEBUGCHARS[0] = 'X';
            }
            int rightCol = sourceSegment.getRightMostCol(); // - ScreenLineSegmentsView.this.offsetIntoSource.getCol();
            int viewerRightCol = ScreenLineSegmentsView.this.getRightMostCol() - rightColOffsetClipLen;
            String sourceText = sourceSegment.getText();

            if (rightCol >= sourceText.length()) {
                rightCol = sourceText.length();
                //DEBUGCHARS[1] = 'A';
            } else {
                if (rightCol > viewerRightCol + offset_col) {
                    rightCol = viewerRightCol + offset_col;
                    //DEBUGCHARS[2] = 'B';
                }
                rightCol += 1; // +1 to include rightmost char
                //DEBUGCHARS[1] = 'b';
            }
            if (start < 0 || start > rightCol)
                return "";
            
            //String debug = new String(DEBUGCHARS);
            
            String clippedText = sourceText.substring(start, rightCol);
            _clippedText = clippedText;
//            if (clippedText.length() > (debug.length() + 2)) {
//                return "["+debug+_clippedText.substring(0,_clippedText.length()-(debug.length() + 2))+"]";
//            }
//            if (clippedText.length()>1) {
//                return "["+_clippedText.substring(0,_clippedText.length()-2)+"]";
//            }
            return clippedText;
        }

        @Override
        public ScreenLineSegment fromOffset(Position offset) {
            return new SegmentImpl(getColor(), Position.Simple.withOffset((Position) this, offset), getId(),
                    this.getText());
        }

    }
}
