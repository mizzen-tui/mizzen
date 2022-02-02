package dtb.terminalIO.textsegments;

import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

public class SegmentsViewUtils {

    public static void copyComponentParts(TreeSet<ScreenLineSegment> destinationSegmentsTree,
                                          Rectangle maxbounds,
                                          Component<?> sourceComponent,
                                          List<Rectangle> sourceParts, 
                                          Position offsetIntoDestination) {

        Rectangle bounds = new Rectangle.Simple(0,0,0,0).mergeAndExpandDimensionsWith(sourceParts);
        int topRow    = Math.max(bounds.getRow(),maxbounds.getRow());
        int bottomRow = Math.min(bounds.getBottomMostRow(),maxbounds.getBottomMostRow());
        int leftCol   = Math.max(bounds.getCol(),maxbounds.getCol());
        int rightCol  = Math.min(bounds.getRightMostCol(),maxbounds.getRightMostCol());

        TreeSet<ScreenLineSegment> segmentsFromSource = sourceComponent.getSegmentsBuffer().getSegments();
        
        // trim parts to be inside of maxBounds  
        sourceParts = sourceParts.stream().map(r-> r.intersectWith(maxbounds)).filter(r-> r!=null).collect(Collectors.toList());

        ScreenLineSegment first = ScreenLineSegment.UTIL.getAtRowCol(segmentsFromSource, 
                                                                     topRow, 
                                                                     leftCol);

        if (first != null) {
            ScreenLineSegment last = ScreenLineSegment.UTIL.getAtRowCol(segmentsFromSource, 
                                                                        bottomRow,
                                                                        rightCol);
            if (last != null) {
                segmentsFromSource = (TreeSet<ScreenLineSegment>)segmentsFromSource.subSet(first, true, last, true);
            }
        }

        for (ScreenLineSegment sourceSegment : segmentsFromSource) {
            sourceSegment = sourceSegment.fromOffset(offsetIntoDestination);
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

            for (Rectangle part : sourceParts) {
                if (part.overlapsWith(sourceSegment)) {
                    ScreenLineSegment srcSliced = ScreenLineSegment.UTIL.sliceIfNeededToFitRect(sourceSegment, part);
                    if (srcSliced != null) {
                        destinationSegmentsTree.add(srcSliced);
                    }
                }
            }
        }

    }

    static class SegmentClippedViewWithOffset implements ScreenLineSegment  {
        
        Position offset;
        ScreenLineSegment sourceSegment;
        Rectangle part;
        
        SegmentClippedViewWithOffset(ScreenLineSegment sourceSegment, Position offset, Rectangle part) {
            this.sourceSegment = sourceSegment;
            this.offset = offset;
            this.part = part;
        }

        @Override
        public int getRow() {
            int row = sourceSegment.getRow() + this.offset.getRow();
            return row;
        }

        @Override
        public int getCol() {
            int col = sourceSegment.getCol() + this.offset.getCol();
            return col;
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
        public String getText() {

            if (_clippedText != null) {
                return _clippedText;
            }

            String sourceText = sourceSegment.getText();
            int textBeginIndex = 0;
            int srcLeftCol = sourceSegment.getCol();
            int viewerLeftCol = part.getCol();
//            if (viewerLeftCol > 0) {
//                textBeginIndex = 0;
//            }
            if (viewerLeftCol > srcLeftCol) {
                textBeginIndex = viewerLeftCol - srcLeftCol;
                //rightColOffsetClipLen = textBeginIndex;
            }
            int textEndIndex = sourceSegment.getWidth();
            int viewerRightCol = part.getRightMostCol();

            if (srcLeftCol + textEndIndex > viewerRightCol) {
                textEndIndex = textEndIndex - ((srcLeftCol + textEndIndex) - viewerRightCol)+1;
            }


            if (textBeginIndex < 0 || textBeginIndex > textEndIndex)
                return " ";
            
            String clippedText = sourceText.substring(textBeginIndex, textEndIndex);
            _clippedText = clippedText;

            return clippedText;
        }

        @Override
        public void moveTo(int row, int col) {
            rememberedLength = -1; // unremember my length
            _clippedText = null;
            throw new IllegalStateException("Not implemented yet!");
        }

        @Override
        public int getId() {
            return sourceSegment.getId();
        }

        @Override
        public Color getColors() {
            return sourceSegment.getColors();
        }

        @Override
        public ScreenLineSegment fromOffset(Position offset) {
            return new SegmentImpl(getColors(),  new Position.Simple(this).offsetAdd(offset), getId(), this.getText());
        }
        
        public String toString() {
            return String.format("Clipped(row=%d,col=%d,width=%d,'%s',part=[%s],offset=[%s])", getRow(), getCol(), getWidth(), getText(),String.valueOf(part),String.valueOf(offset));
        }

    }
    
    
    
}
