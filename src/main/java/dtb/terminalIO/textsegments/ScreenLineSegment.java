package dtb.terminalIO.textsegments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.ColorParsingUtils;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.CursorPos;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.Relation.LeftRight;
import dtb.terminalIO.geom.position.Position;

/**
 * This object is the fundamental building block for all text objects that can
 * be rendered on the terminal.
 * 
 * <p/>
 * 
 * A Screen line segment is a Rectangle[row,col,height,width] that always have a
 * height of one and width the length of its text.
 * 
 * <p/>
 * 
 * It has {@link Color} attribute that dictates default color to render the text
 * in.
 * 
 */
public interface ScreenLineSegment extends Rectangle, CleanString {

    public abstract int getId();

    public abstract String getText();

    public abstract Color getColors();

    public abstract int length();

    public abstract int getRow();

    public abstract int getCol();

    public default int getWidth() {
        return length();
    }

    public default int getHeight() {
        if (length() > 0)
            return 1;
        else
            return 0;
    }
    
    public default boolean overlapsWith(ScreenLineSegment other) {
        if (this.getRow() != other.getRow())
            return false;
        
        ScreenLineSegment left = other;
        ScreenLineSegment right = this;
        if (this.getCol()< other.getCol()) {
            left = this;
            right = other;
        }
        
        if (left.getRightMostCol() >= right.getCol()) {
            return true;
        }
        
        return false;
    }

    public default char charAtCol(int col) {
        if (col < getCol() || col > getRightMostCol())
            throw new IllegalArgumentException("bad column " + col);
        return getText().charAt(col - getCol());
    }

    public default CleanString subTextAtColumns(int beginColIndex, int endColIndexInclusive) {
        if (beginColIndex < getCol() || beginColIndex > getRightMostCol())
            throw new IllegalArgumentException("bad start column " + beginColIndex);
        if (endColIndexInclusive < beginColIndex || endColIndexInclusive > getRightMostCol())
            throw new IllegalArgumentException("bad end column " + endColIndexInclusive);
        // String theText = getText();
        int col = getCol();
        return this.substring(beginColIndex - col, endColIndexInclusive - col + 1);
    }

    @SuppressWarnings("unchecked")
    public default <T extends Position> LeftRight<T> leftRight(T right) {
        if (this.getRow() != right.getRow())
            return null;
        return Position.UTIL.leftRight((T) this, right);
    }

    public default <T extends Rectangle> boolean overlapsWith(T other) {
        if (this.getRow() != other.getRow())
            return false;
        return Rectangle.UTIL.hasOverlap(this, other);
    }

    // [left, right]
    public default Collection<ScreenLineSegment> split_on_overlap(ScreenLineSegment other) {
        Rectangle top;
        ScreenLineSegment under;
        top = this;
        under = other;

        if (!top.overlapsWith(under)) {
            return Collections.singleton(under);
        }

        List<ScreenLineSegment> chunks = new ArrayList<ScreenLineSegment>(2);
        if (under.getCol() < top.getCol()) {
            // tmp
            top.overlapsWith(under);
            // left part of under
            chunks.add(slice(under, 0, top.getCol() - under.getCol()));
        }

        int underRightMostCol = under.getRightMostCol();
        int topRightMostCol = top.getRightMostCol();
        if (underRightMostCol > topRightMostCol) {
            // right part of under
            chunks.add(slice(under, topRightMostCol - under.getCol() + 1, underRightMostCol - topRightMostCol));
        }
        return chunks;
    }

    // returns null if new position no longer fits
    public default ScreenLineSegment moveAndClip(ScreenLineSegment source, int newRow, int newCol,
            Rectangle boundingRectangle) {
        source.moveTo(newRow, newCol);
        return null;
    }

    public default ScreenLineSegment slice(ScreenLineSegment source, int offset, int len) {
        if (len < 1) {
            throw new IllegalArgumentException("Creating a zero len slice!");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Creating a negative offset slice!");
        }
        int lenOfOrig = source.length();
        if (offset >= lenOfOrig) {
            throw new IllegalArgumentException("Offset larger than original len");
        }
        if (len + offset > lenOfOrig) {
            throw new IllegalArgumentException("len+offset " + (len + offset) + " larger than original len " + lenOfOrig
                    + "\n" + "source = " + String.valueOf(source) + "\n" + "slice offset = " + offset + " len " + len);
        }

        CleanString txt = source.substring(offset, offset + len);
        SegmentImpl so = new SegmentImpl(source.getColors(), new CursorPos(source.getRow(), source.getCol() + offset),
                source.getId(), txt);
        return so;
    }

    public default ScreenLineSegment charSlice(ScreenLineSegment source, int offset) {
        return slice(source, offset, 1);
    }

    public static class SegmentComparator implements Comparator<ScreenLineSegment> {
        @Override
        public int compare(ScreenLineSegment so1, ScreenLineSegment so2) {
            if (so1 == so2)
                return 0;

            int cmp = Position.UTIL.POS_COMPARE.compare(so1, so2);
            if (cmp != 0)
                return cmp;
            if (so1.overlapsWith(so2)) {
                // NOTE: reverse sort on id !
                //       This way higher ids appear first (implies newer) and these
                //       will then be on top (TODO: I have yet to take advantage of this feature)
                cmp = Integer.compare(so2.getId(), so1.getId());
            }
            if (cmp != 0)
                return cmp;
            cmp = so1.getText().compareTo(so2.getText());
            if (cmp != 0)
                return cmp;
            
            cmp = Color.COLOR_COMPARE.compare(so1.getColors(), so2.getColors());
            return cmp;
        }
    }

    public static class UTIL {

        static ScreenLineSegment.SegmentComparator SO_COMPARE = new SegmentComparator();

        // used?
        public static void assert_no_Overlap(List<ScreenLineSegment> scs) {
            int len = scs.size();
            for (int i = 0; i < len - 1; i++) {
                ScreenLineSegment i_sc = scs.get(i);
                for (int j = i + 1; j < len; j++) {
                    if (i_sc.overlapsWith(scs.get(j))) {
                        throw new IllegalStateException(String.format("OVERLAP!!! row[%d] %d %d \n{%s}\n{%s}\n",
                                i_sc.getRow(), i, j, i_sc, scs.get(j)));
                    }
                }
            }
        }

        public static int totalWidth(ScreenLineSegment s1, ScreenLineSegment s2) {
            return Math.max(s1.getRightMostCol(), s2.getRightMostCol()) - Math.min(s1.getCol(), s2.getCol()) + 1;
        }

        public static int totalWidth(Collection<ScreenLineSegment> scs) {
            int lcol = Integer.MAX_VALUE;
            int rcol = Integer.MIN_VALUE;
            for (ScreenLineSegment sc : scs) {
                if (sc != null) {
                    lcol = Math.min(lcol, sc.getCol());
                    rcol = Math.max(rcol, sc.getRightMostCol());
                }
            }
            return rcol - lcol + 1;
        }

        public static Collection<ScreenLineSegment> sortByRowIDColumn(Collection<ScreenLineSegment> scs) {
            List<ScreenLineSegment> sorted = new ArrayList<ScreenLineSegment>(scs);
            Collections.sort(sorted, SO_COMPARE);
            return sorted;
        }

        /**
         * We make the collection of ScreenLineSegment's a TreeSet such that it
         * naturally sorts itself by row/col in order that when we traverse the
         * collection, we visit elements (ScreenLineSegment) from topLeft (0,0) to bottom
         * right (height,width) order.
         */
        public static TreeSet<ScreenLineSegment> makeCollection(Collection<ScreenLineSegment> scs) {
            TreeSet<ScreenLineSegment> ts = new TreeSet<ScreenLineSegment>(SO_COMPARE);
            /*
             * key to happiness (and screen alignment) is to ensure ScreenLineSegment
             * objects have a len > 0 
             *  -- keeping this around for posterity .. 
             *  public
             * boolean add(ScreenLineSegment scs) {
             * 
             * if (scs == null || scs.getText()==null || scs.getText().length()==0) { throw
             * new IllegalArgumentException(String.valueOf(scs)); }
             * 
             * return super.add(scs); } };
             */

            if (scs != null && scs.size() > 0) {
                ts.addAll(scs);
            }
            return ts;
        }

        public static ScreenLineSegment blankScreenObject(Color color, char emptyChar, int row, int col, int id, int width) {
            
            if (width < 0)
                throw new IllegalArgumentException("width=" + width);
            
            if (emptyChar <= 27) {
                throw new IllegalStateException("!!!!");
            }
            return __blankScreenObject(color, emptyChar, null, row, col, id, width);
        }
        
        private static ScreenLineSegment __blankScreenObject(Color color, char emptyChar, String emptyText, int row, int col, int id, int width) {

            return new AbstractSegment(color, new CursorPos(row, col)) {
                
                String theText = emptyText;
                
                @Override
                public int getId() {
                    return id;
                }

                public int length() {
                    return width;
                }

                @Override
                public String getText() {
                    if (theText != null) {
                        return theText;
                    }
                    this.theText = new String(new char[width]).replace("\0", emptyChar + "");
                    return theText;
                }

                @Override
                public ScreenLineSegment fromOffset(Position offset) {
                    Position newPos = Position.Simple.withOffset(getCursorPos(), offset);
                    // can you say recursive-ish? :-)
                    return __blankScreenObject(getColor(), emptyChar, theText, newPos.getRow(), newPos.getCol(), getId(), getWidth());
                }
            };
        }

        // [left].....[right]
        //
        // If there is a gap between left and right then return a gap ScreenLineSegment
        // object - otherwise null
        //
        // [left][gap][right]
        //
        public static ScreenLineSegment gapFrom(ScreenLineSegment so1, ScreenLineSegment so2, Color color,
                char emptyChar) {
            if (so1.overlapsWith(so2))
                return null;

            LeftRight<ScreenLineSegment> lr = so1.leftRight(so2);
            if (lr == null)
                return null; // returns null if not on the same line

            int len = lr.getRight().getCol() - lr.getLeft().getRightMostCol() - 1;
            if (len < 1)
                return null;

            return blankScreenObject(color, emptyChar, lr.getRight().getRow(), lr.getLeft().getRightMostCol() + 1, -1, len);
        }

        public static TreeSet<ScreenLineSegment> createBlankSegments(Color color, char emptyChar, Rectangle bounds) {
            TreeSet<ScreenLineSegment> ts = makeCollection(null);
            int maxRow = bounds.getBottomMostRow();
            int col = bounds.getCol();
            int width = bounds.getWidth();
            if (width > 0) {
                for (int row = bounds.getRow(); row <= maxRow; row++) {
                    ScreenLineSegment bsc = blankScreenObject(color, emptyChar, row, col, -1, width);
                    ts.add(bsc);
                }
            }
            return ts;
        }

        public static void fillRectWithChar(Color color, char fillChar, Rectangle bounds,
                                            TreeSet<ScreenLineSegment> destination) {
            int maxRow = bounds.getBottomMostRow();
            int col = bounds.getCol();
            int width = bounds.getWidth();
            if (width > 0) {
                for (int row = bounds.getRow(); row <= maxRow; row++) {
                    ScreenLineSegment bsc = blankScreenObject(color, fillChar, row, col, -1, width);
                    addTo(destination, bsc);
                }
            }
        }

        public static void addTo(TreeSet<ScreenLineSegment> ts, ScreenLineSegment sc) {
            TreeSet<ScreenLineSegment> treeS = (TreeSet<ScreenLineSegment>) ts;

            ScreenLineSegment start = treeS.floor(sc);
            if (start == null) {
                start = treeS.ceiling(sc);
                if (start == null || ! sc.overlapsWith(start)) {
                   treeS.add(sc);
                   return;
                }
            }

            ScreenLineSegment current = start;
            boolean overlapped = false;
            while (current != null) {
                if (sc.overlapsWith(current)) {
                    ts.remove(current);
                    Collection<ScreenLineSegment> chunks = sc.split_on_overlap(current);
                    ts.addAll(chunks);
                    overlapped = true;
                } else {
                    // no overlap
                    if (overlapped) {
                        // we had overlaps, now we do not, so we must be done
                        break;
                    }
                }
                current = ts.higher(current);
            }

            ts.add(sc);
        }

        public static void addToSLOW(TreeSet<ScreenLineSegment> ts, ScreenLineSegment sc) {
            // very inefficient - scans the full list
            
            List<ScreenLineSegment> overlaps = ts.stream().filter(s -> s.getRow() == sc.getRow() && sc.overlapsWith(s))
                    .collect(Collectors.toList());
            for (ScreenLineSegment overlap : overlaps) {
                ts.remove(overlap);
                Collection<ScreenLineSegment> chunks = sc.split_on_overlap(overlap);
                ts.addAll(chunks);
            }
            ts.add(sc);
        }

        public static void removeRect(Collection<ScreenLineSegment> ts, Rectangle r) {

            List<ScreenLineSegment> overlaps = ts.stream().filter(s -> r.containsRow(s.getRow()) && r.overlapsWith(s))
                    .collect(Collectors.toList());

            for (ScreenLineSegment overlap : overlaps) {
                ts.remove(overlap);
                Collection<ScreenLineSegment> chunks = split_on_overlap(r.rectangleAtRow(overlap.getRow()), overlap);
                ts.addAll(chunks);
            }
        }

        // remove all rectangles from ts
        public static void removeRectangles(Collection<ScreenLineSegment> ts, Collection<? extends Rectangle> rc) {
            for (Rectangle r : rc) {
                removeRect(ts, r);
            }
        }

        /**
         * returns a new TreeSet of ScreenLineSegment's that is the 
         *         
         *         equivalent to tree1 - tree2
         *         
         *         Such that elements are copied from tree1 iff the elements
         *         are *not* also in tree2.
         *         
         * @param tree1
         * @param tree2
         * @return
         */
        public static TreeSet<ScreenLineSegment> subtract(TreeSet<ScreenLineSegment> tree1, 
                                                          TreeSet<ScreenLineSegment> tree2) {

            TreeSet<ScreenLineSegment> diffResult = makeCollection(null);
            List<ScreenLineSegment> segmentsInTree1NotInTree2 = tree1.stream()
                                                                      .filter(s -> !tree2.contains(s))
                                                                      .collect(Collectors.toList());
            diffResult.addAll(segmentsInTree1NotInTree2);

            return diffResult;
        }


        // This method does not work properly .. I dont want to throw away - I would 
        // like to fix in the future.. but for now, use only the above subtract() method
        public static TreeSet<ScreenLineSegment> subtractAggressive(TreeSet<ScreenLineSegment> tree1, 
                                                                    TreeSet<ScreenLineSegment> tree2) {
            
            tree1 = subtract(tree1, tree2);

            // Above subtract is not aggressive enough.  It only subtracts like segments
            // Its does not subtract partial segments of text that are the same color, same text and same position
            // The following below does this.


            // The following loops thru remaining tree1 segments (which has already had  segments removed via above subtraction) and then
            // it builds another result (diffResult) that is a copy of tree1 - minus same text from tree2

            TreeSet<ScreenLineSegment> diffResult = makeCollection(null);
            if (tree1.isEmpty()) {
                return diffResult;
            }
            StringBuffer srcText = new StringBuffer();
            int srcCurCol  = -1;
            int srcCurRow  = -1;
            Color srcColor = null;
            int srcIDX     = -1;
            int seg2Col    = -1;
            int seg2Len    = -1;
            ScreenLineSegment tree2Seg  = null;
            boolean isSameColor = false;
            int seg2IDX = -1;

            for (ScreenLineSegment src : tree1) {
                srcCurCol  = src.getCol();
                srcCurRow  = src.getRow();
                srcColor   = src.getColors();
                srcIDX     = 0;
                int srcLen = src.getWidth();
                if (tree2Seg == null || srcCurRow != tree2Seg.getRow() || !tree2Seg.containsCol(srcCurCol)) {
                    tree2Seg = getAtRowCol(tree2, srcCurRow, srcCurCol);
                    if (tree2Seg != null) {
                        seg2Col = tree2Seg.getCol();
                        seg2Len = tree2Seg.getWidth();
                        seg2IDX = 0;
                    }
                }

                while (srcIDX < srcLen) { // loop thru all chars in src segment
                    if (tree2Seg == null) {
                        srcText.append(src.charAt(srcIDX));
                        srcIDX++;
                    } else {

                        isSameColor = Color.COLOR_COMPARE.compare(tree2Seg.getColors(), srcColor) == 0;

                        while ((srcCurCol+srcIDX)<seg2Col) {
                            srcText.append(src.charAt(srcIDX));
                            srcIDX++;
                        }

                        if (!isSameColor) {
                            while (seg2IDX < seg2Len && srcIDX < srcLen) {
                                char srcCHR = src.charAt(srcIDX)       ;  srcIDX++;
                                seg2IDX++;
                                srcText.append(srcCHR);
                            }
                        }

                        while (seg2IDX < seg2Len && srcIDX < srcLen) {
                            char srcCHR = src.charAt(srcIDX)       ;  srcIDX++;
                            char seg2CHR = tree2Seg.charAt(seg2IDX);  seg2IDX++;

                            if (srcCHR == seg2CHR) {
                                if (srcText.length()>0) {
                                    ScreenLineSegment srcSegment = new SegmentImpl(srcColor, srcCurRow, srcCurCol, -1, srcText.toString());
                                    diffResult.add(srcSegment);
                                    srcCurCol = srcCurCol + srcSegment.getWidth();
                                    srcText = new StringBuffer();
                                } 
                                srcCurCol++;  // skip past srcCHR
                            } else {
                                srcText.append(srcCHR);
                            }
                        }
                        if (srcIDX < srcLen && seg2IDX >= seg2Len) {
                            tree2Seg = getAtRowCol(tree2,srcCurRow, srcCurCol);
                            if (tree2Seg!=null) {
                                seg2Col = tree2Seg.getCol();
                                seg2Len = tree2Seg.getWidth();
                                seg2IDX = 0;
                            }
                        }
                    }
                } /*while*/

                if (srcText.length()>0) {
                    ScreenLineSegment srcSegment = new SegmentImpl(srcColor, srcCurRow, srcCurCol, -1, srcText.toString());
                    diffResult.add(srcSegment);
                    srcText = new StringBuffer();
                }
            }
            return diffResult;
        }
        


        public static void addSegmentBounded(Rectangle bounds, TreeSet<ScreenLineSegment> ts, ScreenLineSegment sc) {
            if (bounds.contains(sc)) {
                addTo(ts, sc);
                return;
            }
            ScreenLineSegment sliced = sliceIfNeededToFitRect(sc, bounds);
            if (sliced != null) { // null means slice does not overlap the bounds - so no point in adding
                addTo(ts, sliced);
            }
        }

        public static void addSegmentsBounded(Rectangle bounds, 
                                              TreeSet<ScreenLineSegment> destination,
                                              Collection<ScreenLineSegment> segmentsToAdd) {
            for (ScreenLineSegment sc : segmentsToAdd) {
                addSegmentBounded(bounds, destination, sc);
                // todo: should I return a list of slices add so that I can later
                // remove them? <time passes ..> removeRect seems to be smart - might not need
                // to - need to test
            }
        }
        // MAX_SIZE

        public static void addSegmentWithOffsetBounded(Position offset, 
                                                       Rectangle bounds,
                                                       TreeSet<ScreenLineSegment> ts, 
                                                       ScreenLineSegment sc) {
            if (offset == null || offset.isSamePosition(Position.ZERO)) {
                addSegmentBounded(bounds, ts, sc);
                return;
            }
            addSegmentBounded(bounds, ts, sc.fromOffset(offset));
        }

        public static void addSegmentsWithOffsetBounded(Position offset, 
                                                        Rectangle bounds,
                                                        TreeSet<ScreenLineSegment> destination, 
                                                        TreeSet<ScreenLineSegment> segmentsToAdd) {
            if (destination ==  segmentsToAdd) {
                return;
            }
            if (offset == null || offset.isSamePosition(Position.ZERO)) {
                // no offset
                addSegmentsBounded(bounds, destination, segmentsToAdd);
                return;
            }
            for (ScreenLineSegment sc : segmentsToAdd) {
                addSegmentWithOffsetBounded(offset, bounds, destination, sc);
                // TODO: Should I return a list of slices add so that I can later
                //       remove them? <time passes ..> removeRect seems to be smart - might not need
                //       to - need to test
            }
        }

        //
        public static void addSegmentsWithOffsetUnBounded(Position offset, 
                                                          TreeSet<ScreenLineSegment> destination,
                                                          TreeSet<ScreenLineSegment> segmentsToAdd) {
            addSegmentsWithOffsetBounded(offset, Rectangle.MAX_SIZE, destination, segmentsToAdd);
        }

        /**
         * Returns a ScreenLineSegment within ts that overlaps row/col
         */
        public static ScreenLineSegment getAtRowCol(TreeSet<ScreenLineSegment> ts, int row, int col) {
            ScreenLineSegment srchKey = blankScreenObject(Colors.DEFAULT_COLOR, ' ', row, col, Integer.MIN_VALUE, 1);
            ScreenLineSegment siblingGreater = ts.ceiling(srchKey);
            if (siblingGreater != null && srchKey.overlapsWith(siblingGreater)) {
                return siblingGreater;
            }
            ScreenLineSegment siblingLess = ts.floor(srchKey);
            if (siblingLess != null && srchKey.overlapsWith(siblingLess)) {
                return siblingLess;
            }
            return null;
        }

        public static String getCharAt(TreeSet<ScreenLineSegment> ts, int row, int col) {
            ScreenLineSegment sc = getAtRowCol(ts, row, col);
            if (sc == null)
                return null;
            int offset = col - sc.getCol();
            return sc.getText().substring(offset, offset + 1);
        }

        public static void setCharAt(TreeSet<ScreenLineSegment> ts, int row, int col, char ch, Color color) {
            if (!CleanString.UTIL.isClean(ch)) {
                throw new IllegalStateException("Bad char to set in text segment.  Cannot be a control code");
            }
            ScreenLineSegment sc = getAtRowCol(ts, row, col);
            if (sc == null || !sc.getColors().equals(color) || !(sc instanceof SegmentImpl)) {
                ScreenLineSegment new_sc = new SegmentImpl(color, row, col, sc != null ? sc.getId() + 1 : 1,
                        new CleanString.Simple(ch));
                addTo(ts, new_sc);
                return;
            }
            // at this point we have an sc that is an SegmentImpl
            int offset = col - sc.getCol();
            String oldText = sc.getText();

            CleanString left = ((offset > 0) ? sc.substring(0, offset) : CleanString.UTIL.EMPTY_CLEAN_STRING);
            CleanString right = ((offset < sc.length()) ? sc.substring(offset + 1, oldText.length())
                    : CleanString.UTIL.EMPTY_CLEAN_STRING);

            CleanString newText = new CleanString.Simple(left).append(new CleanString.Simple(ch)).append(right);

            ((SegmentImpl) sc).setText(newText);
        }

        public static Collection<ScreenLineSegment> split_on_overlap(Rectangle top, ScreenLineSegment under) {
            if (!top.overlapsWith(under)) {
                return Collections.singleton(under);
            }

            List<ScreenLineSegment> chunks = new ArrayList<ScreenLineSegment>(2);
            if (under.getCol() < top.getCol()) {
                // tmp
                top.overlapsWith(under);
                // left part of under
                chunks.add(slice(under, 0, top.getCol() - under.getCol()));
            }

            int underRightMostCol = under.getRightMostCol();
            int topRightMostCol = top.getRightMostCol();
            if (underRightMostCol > topRightMostCol) {
                // right part of under
                chunks.add(slice(under, topRightMostCol - under.getCol() + 1, underRightMostCol - topRightMostCol));
            }
            return chunks;
        }

        public static ScreenLineSegment sliceIfNeededToFitRect(ScreenLineSegment source, Rectangle boundingRectangle) {
            if (boundingRectangle.contains(source)) {
                return source;
            }
            Rectangle clipped = new Rectangle.Clipped(boundingRectangle, source);
            if (clipped.getWidth() < 1 || clipped.getHeight() < 1) {
                return null;
            }
            CleanString text = source.subTextAtColumns(clipped.getCol(), clipped.getRightMostCol());
            SegmentImpl so = new SegmentImpl(source.getColors(), new CursorPos(clipped.getRow(), clipped.getCol()),
                                             source.getId(), text);
            return so;
        }

        public static ScreenLineSegment slice(ScreenLineSegment source, int offset, int len) {
            if (len < 1) {
                throw new IllegalArgumentException("Creating a zero len slice!");
            }
            if (offset < 0) {
                throw new IllegalArgumentException("Creating a negative offset slice!");
            }
            int lenOfOrig = source.length();
            if (offset >= lenOfOrig) {
                throw new IllegalArgumentException("Offset larger than original len");
            }
            if (len + offset > lenOfOrig) {
                throw new IllegalArgumentException(
                        "len+offset " + (len + offset) + " larger than original len " + lenOfOrig + "\n" + "source = "
                                + String.valueOf(source) + "\n" + "slice offset = " + offset + " len " + len);
            }

            CleanString slicedText = source.substring(offset, offset + len);
            SegmentImpl slicedSegment = new SegmentImpl(source.getColors(),
                                                        new CursorPos(source.getRow(), source.getCol() + offset), 
                                                        source.getId(), 
                                                        slicedText);
            return slicedSegment;
        }

        /**
         *  clip a string to width if length > width
         */
        private static String clip(String s, int width) {
            if (s.length() > width) {
                return s.substring(0, width);
            }
            return s;
        }

        public static class SegmentsFromStringResult {
            public Position pos = Position.ZERO;
            public Collection<ScreenLineSegment> ts;

            public SegmentsFromStringResult(Position pos, Collection<ScreenLineSegment> ts) {
                super();
                this.pos = pos;
                this.ts = ts;
            }

            public SegmentsFromStringResult(Collection<ScreenLineSegment> ts) {
                super();
                this.pos = Position.ZERO;
                this.ts = ts;
            }

            public int size() {
                return ts == null ? 0 : ts.size();
            }
        }

        @FunctionalInterface
        public static interface CreateSegmentWithColor {
            ScreenLineSegment create(Color color, Position position, CleanString simpleText);
        }

        public static SegmentsFromStringResult textSegmentsFromString(String multiLineString, int offSetRow,
                int offSetCol, Rectangle boundingRectangle,
                BiFunction<Position, CleanString, ScreenLineSegment> createSegment) {

            TreeSet<ScreenLineSegment> ts = makeCollection(null); // empty
            if (multiLineString == null || multiLineString.length() == 0) {
                return new SegmentsFromStringResult(ts);
            }
            Position.Simple position = new Position.Simple(offSetRow + boundingRectangle.getRow(),
                                                           offSetCol + boundingRectangle.getCol());
            Rectangle boundAndClipped = Rectangle.Clipped.fromPosition(position, boundingRectangle);
            if (boundAndClipped == null) {
                return new SegmentsFromStringResult(ts);
            }

            String lines[] = multiLineString.split("\n", -1);
            lines[0] = clip(lines[0], boundAndClipped.getWidth());
            ScreenLineSegment sc = null;
            if (lines[0] != null && lines[0].length() > 0) {
                sc = createSegment.apply(position, new CleanString.Simple(lines[0]));
                ts.add(sc);
            }
            int lineLimit = Math.min(lines.length, boundAndClipped.getHeight());
            int originalWidth = boundingRectangle.getWidth();
            for (int i = 1; i < lineLimit; i++) {
                position.moveTo(position.getRow() + 1, boundingRectangle.getCol()); // notice that we effectively
                                                                                    // carriage return
                                                                                    // to start of next line (moveTo)
                lines[i] = clip(lines[i], originalWidth); // and now we use width of original rectangle for subsequent
                                                          // clippings
                if (lines[i] != null && lines[i].length() > 0) {
                    sc = createSegment.apply(position, new CleanString.Simple(lines[i]));
                    ts.add(sc);
                }
            }
            if (sc == null)
                return new SegmentsFromStringResult(position, ts);
            return new SegmentsFromStringResult(new Position.Simple(sc.getBottomMostRow(), sc.getRightMostCol()), ts);
        }

        public static SegmentsFromStringResult textSegmentsFromString(String s, int offSetRow, int offSetCol,
                Rectangle boundingRectangle) {
            return textSegmentsFromString(s, offSetCol, offSetCol, boundingRectangle,
                    (p, txt) -> new SegmentImpl(Colors.FGBGColors.NO_COLOR_SPECIFIED, p, -1, txt));
        }

        private static int clipStrWithEmbeddedColors(final Collection<ScreenLineSegment> ts, 
                                                     String colorCodedStr,
                                                     int width, 
                                                     Position offset, 
                                                     CreateSegmentWithColor createSegment) {
            int[] curlengthCreated = new int[] { 0 };
            Color[] lastColor = new Color[] {Colors.DEFAULT_COLOR};
            CleanString[] lastSimpleText = new CleanString[] {null};

            ColorParsingUtils.parseColorsAndBuildResult(colorCodedStr, new ColorParsingUtils.ColorColorSegmentBuilder() {
                @Override
                public boolean newColorSegment(Color color, CleanString simpleText) {
                    color = color == null ? lastColor[0] : color;
                    if (simpleText == null) {
                        simpleText = CleanString.UTIL.EMPTY_CLEAN_STRING;
                    }

                    if (lastSimpleText[0] != null && 
                        lastSimpleText[0].length()>0 && 
                        Color.COLOR_COMPARE.compare(lastColor[0], color) != 0) {
                        ScreenLineSegment segment = createSegment.create(lastColor[0], 
                                                                         offset.offsetAdd(0, curlengthCreated[0]),
                                                                         lastSimpleText[0]);
                        curlengthCreated[0] += lastSimpleText[0].length();
                        ts.add(segment);
                    } else {
                        if (lastSimpleText[0] != null && 
                            lastSimpleText[0].length()>0) {
                            simpleText = lastSimpleText[0].append(simpleText);
                            lastSimpleText[0] = null;
                        }
                    }

                    boolean continueParsing = true;
                    if (curlengthCreated[0] + simpleText.length() > width) {  // need to clip simpleText
                        int clipLength = width - curlengthCreated[0];
                        if (clipLength>0) {
                           simpleText = simpleText.substring(0, clipLength);
                        } else {
                            simpleText = CleanString.UTIL.EMPTY_CLEAN_STRING;
                        }
                        continueParsing = false;
                    }

                    if (simpleText.length() > 0) {
                        if (!continueParsing) {
                            ScreenLineSegment segment = createSegment.create(color, 
                                                                             offset.offsetAdd(0, curlengthCreated[0]),
                                                                             simpleText);
                            curlengthCreated[0] += simpleText.length();
                            ts.add(segment);
                        } else {
                            lastColor[0] = color;
                            lastSimpleText[0] = simpleText;
                        }
                    }

                    return continueParsing;
                }
            });

            if (lastSimpleText[0] != null && lastSimpleText[0].length()>0) {
                ScreenLineSegment segment = createSegment.create(lastColor[0], 
                                                                 offset.offsetAdd(0, curlengthCreated[0]),
                                                                 lastSimpleText[0]);
                curlengthCreated[0] += lastSimpleText[0].length();
                ts.add(segment);
            }

            return curlengthCreated[0];
        }

        public static SegmentsFromStringResult textSegmentsFromStringWithEmbeddedColor(String multiLineString,
                                                                                       int offSetRow, 
                                                                                       int offSetCol, 
                                                                                       Rectangle boundingRectangle, 
                                                                                       CreateSegmentWithColor createSegment) {

            TreeSet<ScreenLineSegment> ts = makeCollection(null); // empty
            if (multiLineString == null || multiLineString.length() == 0) {
                return new SegmentsFromStringResult(ts);
            }
            Position.Simple position = new Position.Simple(offSetRow + boundingRectangle.getRow(),
                    offSetCol + boundingRectangle.getCol());
            Rectangle boundAndClipped = Rectangle.Clipped.fromPosition(position, boundingRectangle);
            if (boundAndClipped == null) {
                return new SegmentsFromStringResult(ts);
            }
            String lines[] = multiLineString.split("\n", -1);
            // lines[0] = clip(lines[0], boundAndClipped.getWidth());
            if (lines[0] != null && lines[0].length() > 0) {
                clipStrWithEmbeddedColors(ts, lines[0], boundAndClipped.getWidth(), position, createSegment);
            }
            int lineLimit = Math.min(lines.length, boundAndClipped.getHeight());
            int originalWidth = boundingRectangle.getWidth();
            for (int i = 1; i < lineLimit; i++) {
                position.moveTo(position.getRow() + 1, boundingRectangle.getCol()); // notice that we effectively
                                                                                    // carriage return
                                                                                    // to start of next line (moveTo)
                                                                                    // and now we use width of original
                                                                                    // rectangle for subsequent
                                                                                    // clippings
                if (lines[i] != null && lines[i].length() > 0) {
                    clipStrWithEmbeddedColors(ts, lines[i], originalWidth, position, createSegment);
                }
            }
            if (ts.size() == 0)
                return new SegmentsFromStringResult(position, ts);
            ScreenLineSegment lastSegment = ts.last();
            return new SegmentsFromStringResult(
                    new Position.Simple(lastSegment.getBottomMostRow(), lastSegment.getRightMostCol()), ts);
        }

        public static SegmentsFromStringResult textSegmentsFromStringWithEmbeddedColor(String s, int offSetRow,
                int offSetCol, Rectangle boundingRectangle) {
            return textSegmentsFromStringWithEmbeddedColor(s, offSetCol, offSetCol, boundingRectangle, (color, p,
                    txt) -> new SegmentImpl(color != null ? color : Colors.FGBGColors.NO_COLOR_SPECIFIED, p, -1, txt));
        }
        
        /**
         * this is not a for production use method .. It is a sanity test that can be intermitantly
         * injected into code to ensure that the segment contents are in 'good shape'.
         * 
         * If 2 equal positions are found in the collection, then an IllegalStateException is thrown 
         * 
         * Useful function for unit tests
         * 
         */
        public static void assertUniquePositionsInSegments(Collection<ScreenLineSegment> segments ) {
            Comparator<ScreenLineSegment> NoSamePositionComparator = new Comparator<ScreenLineSegment>() {

                @Override
                public int compare(ScreenLineSegment so1, ScreenLineSegment so2) {
                    if (so1 == so2)
                        return 0;

                    int cmp = Position.UTIL.POS_COMPARE.compare(so1, so2);
                    if (cmp != 0)
                        return cmp;
                    throw new IllegalStateException("so1:'"+so1 + "' same position with '"+so2+"'");
                }
                
            };
            
            List<ScreenLineSegment> myVerifyList = new ArrayList<ScreenLineSegment>( segments.size() );
            myVerifyList.addAll(segments);
            myVerifyList.sort(NoSamePositionComparator);
        }

        /**
         * this is not a for production use method .. It is a sanity test that can be intermitantly
         * injected into code to ensure that the segment contents are in 'good shape'
         * 
         * If 2 overlapping segments are found in the collection, then an IllegalStateException is thrown 
         * 
         */
        public static void assertNoOverlapsInSegments(Collection<ScreenLineSegment> segments ) {
            
            Comparator<ScreenLineSegment> NoSamePositionComparator = new Comparator<ScreenLineSegment>() {

                @Override
                public int compare(ScreenLineSegment so1, ScreenLineSegment so2) {
                    if (so1 == so2)
                        return 0;

                    int cmp = Position.UTIL.POS_COMPARE.compare(so1, so2);
                    if (cmp == 0)
                        throw new IllegalStateException("so1:'"+so1 + "' same position with '"+so2+"'");
                    
                    if (so1.overlapsWith(so2)) {
                        throw new IllegalStateException("so1:'"+so1 + "' overlaps with '"+so2+"'");
                    }
                    cmp = so1.getText().compareTo(so2.getText());
                    if (cmp != 0)
                        return cmp;
                    
                    cmp = Color.COLOR_COMPARE.compare(so1.getColors(), so2.getColors());
                    return cmp;
                }
                
            };
            
            List<ScreenLineSegment> myVerifyList = new ArrayList<ScreenLineSegment>( segments.size() );
            myVerifyList.addAll(segments);
            myVerifyList.sort(NoSamePositionComparator);
        }

    }

    public static class SegmentImpl extends AbstractSegment {
        int id;
        String simpleText;

        public SegmentImpl(Color color, Position position, int id, CleanString simpleText) {
            super(color, position);
            this.id = id;
            this.simpleText = simpleText.getText();
        }

        public SegmentImpl(Color color, Position position, int id, String simpleText) {
            this(color, position, id, new CleanString.Simple(simpleText));
        }

        public Object setText(CleanString newText) {
            return this.simpleText = newText.getText();
        }

        public SegmentImpl(Color color, int row, int col, int id, CleanString simpleText) {
            this(color, new Position.Simple(row, col), id, simpleText);
        }

        public SegmentImpl(Color color, int row, int col, int id, String simpleText) {
            this(color, new Position.Simple(row, col), id, simpleText);
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getText() {
            return simpleText;
        }

        @Override
        public ScreenLineSegment fromOffset(Position offset) {
            return new SegmentImpl(getColor(), Position.Simple.withOffset(getCursorPos(), offset), getId(),
                    this.simpleText);
        }
        

    }

    public abstract ScreenLineSegment fromOffset(Position offset);

}