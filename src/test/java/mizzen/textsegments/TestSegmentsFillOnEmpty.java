package mizzen.textsegments;

import java.util.TreeSet;

import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Rectangle;
import mizzen.textsegments.ScreenLineSegment.SegmentImpl;
import mizzen.util.StringUtils;
import junit.framework.TestCase;

/***
 * Test basic Empty char function works . 
 * 
 * <p/>
 * 
 * Specifically testing that ScreenLineSegment.UTIL.fillEmptySpacesInRectWithChar(fillColor, fillChar, bounds, ts)
 * fills in the empty segment areas with the defined empty char
 * 
 * @author dbalme
 *
 */
public class TestSegmentsFillOnEmpty  extends TestCase {


    public static Color fillColor = Colors.IBla;
    public static char fillChar = 'X';

    
    private TreeSet<ScreenLineSegment>  buildTreeSetForTestSimple() {
        TreeSet<ScreenLineSegment> ts = ScreenLineSegment.UTIL.makeCollection(null);

        ts.add(new SegmentImpl(Colors.IWhi, 0, 0, -1, CleanString.UTIL.clean("0:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 1, 0, -1, CleanString.UTIL.clean("1:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 2, 0, -1, CleanString.UTIL.clean("2:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 3, 0, -1, CleanString.UTIL.clean("3:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 4, 0, -1, CleanString.UTIL.clean("4:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 5, 0, -1, CleanString.UTIL.clean("5:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 6, 0, -1, CleanString.UTIL.clean("6:1234567890")));
        return ts;
    }

    private TreeSet<ScreenLineSegment>  buildTreeSetForTestWithEmptyGapInMiddle() {
        TreeSet<ScreenLineSegment> ts = ScreenLineSegment.UTIL.makeCollection(null);

        ts.add(new SegmentImpl(Colors.IWhi, 0, 0, -1, CleanString.UTIL.clean("0:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 1, 0, -1, CleanString.UTIL.clean("1:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 2, 0, -1, CleanString.UTIL.clean("2:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 3, 0, -1, CleanString.UTIL.clean("3:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 4, 0, -1, CleanString.UTIL.clean("4:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 5, 0, -1, CleanString.UTIL.clean("5:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 6, 0, -1, CleanString.UTIL.clean("6:1234567890")));

        ts.add(new SegmentImpl(Colors.IWhi, 0, 30, -1, CleanString.UTIL.clean("0:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 1, 30, -1, CleanString.UTIL.clean("1:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 2, 30, -1, CleanString.UTIL.clean("2:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 3, 30, -1, CleanString.UTIL.clean("3:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 4, 30, -1, CleanString.UTIL.clean("4:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 5, 30, -1, CleanString.UTIL.clean("5:1234567890")));
        ts.add(new SegmentImpl(Colors.IWhi, 6, 30, -1, CleanString.UTIL.clean("6:1234567890")));
        
        
        return ts;
    }
    
    public static void assertCharsEverywhere(TreeSet<ScreenLineSegment> ts, Rectangle bounds) {
        assertCharsEverywhere(ts, bounds, false);
    }
    public static void assertCharsEverywhere(TreeSet<ScreenLineSegment> ts, Rectangle bounds, boolean checkEmpty) {
        assertCharsEverywhere(ts, bounds, checkEmpty, fillChar);
    }
    
    public static void assertCharsEverywhere(TreeSet<ScreenLineSegment> ts, Rectangle bounds, boolean checkEmpty, char fillChar) {
        for (int row = bounds.getRow(); row <= bounds.getBottomMostRow(); row++) {
            for (int col = bounds.getCol(); col <= bounds.getRightMostCol(); col++) {
                String chStr = ScreenLineSegment.UTIL.getCharAt(ts,row, col);
                assertNotNull( "row "+row+", col "+col+ " was null",chStr );
                if (checkEmpty) {
                    assertTrue("row "+row+", col "+col+ " was not fill char ("+chStr+") rect="+bounds, chStr.equals(fillChar+""));
                }
            }
        }
    }
    
    public static void printTS(TreeSet<ScreenLineSegment> ts, Rectangle bounds) {
        printTS(null, ts, bounds);
    }
    
    public static void printTS(String label, TreeSet<ScreenLineSegment> ts, Rectangle bounds) {
        StringBuilder sb = new StringBuilder();
        if (!StringUtils.isEmptyStr(label)) {
            sb.append(label).append("\n");
        }
        sb.append("+").append(StringUtils.repeat('-', bounds.getWidth())).append("+").append("\n");
        for (int row = bounds.getRow(); row <= bounds.getBottomMostRow(); row++) {
            sb.append("|");
            for (int col = bounds.getCol(); col <= bounds.getRightMostCol(); col++) {
                String chStr = ScreenLineSegment.UTIL.getCharAt(ts,row, col);
                sb.append(chStr!=null?chStr:"*");
            }
            sb.append("|\n");
        }
        sb.append("+").append(StringUtils.repeat('-', bounds.getWidth())).append("+").append("\n");
        System.out.println(sb);
    }
    
    public void testFillEmptySpacesInRectWithEmptyTop() {
        __testFillEmptySpacesInRectWithEmptyTop(buildTreeSetForTestSimple());
    }
    public void testFillEmptySpacesInRectWithEmptyTop_withGAP() {
        __testFillEmptySpacesInRectWithEmptyTop(buildTreeSetForTestWithEmptyGapInMiddle());
    }
    private void __testFillEmptySpacesInRectWithEmptyTop(TreeSet<ScreenLineSegment> ts) {

        // get segments size (as a rectangle) and add (merge) row to top 
        Rectangle bounds = Rectangle.UTIL.calcDimensionsWith(ts);
        
        Rectangle emptySectionAtTop = new Rectangle.Simple(bounds.getRow()-1,0,1,bounds.getWidth());
        
        bounds = bounds.mergeAndExpandDimensionsWith(emptySectionAtTop);
        
        ScreenLineSegment.UTIL.fillEmptySpacesInRectWithChar(fillColor, fillChar, bounds, ts);

        printTS("__testFillEmptySpacesInRectWithEmptyTop", ts, bounds);
        assertCharsEverywhere(ts, bounds);
        assertCharsEverywhere(ts, emptySectionAtTop, true);
    }

    public void testFillEmptySpacesInRectWithEmptyLeft() {
        __testFillEmptySpacesInRectWithEmptyLeft(buildTreeSetForTestSimple());
    }
    public void testFillEmptySpacesInRectWithEmptyLeft_withGAP() {
        __testFillEmptySpacesInRectWithEmptyLeft(buildTreeSetForTestWithEmptyGapInMiddle());
    }
    private void __testFillEmptySpacesInRectWithEmptyLeft(TreeSet<ScreenLineSegment> ts) {

        // get segments size (as a rectangle)
        Rectangle bounds = Rectangle.UTIL.calcDimensionsWith(ts);
        //and add (merge) column to left 
        Rectangle emptySectionAtLeft = new Rectangle.Simple(bounds.getRow(),-1,bounds.getHeight(),1);
        bounds = bounds.mergeAndExpandDimensionsWith(emptySectionAtLeft);

        ScreenLineSegment.UTIL.fillEmptySpacesInRectWithChar(fillColor, fillChar, bounds, ts);

        printTS("__testFillEmptySpacesInRectWithEmptyLeft", ts, bounds);
        assertCharsEverywhere(ts, bounds);
        assertCharsEverywhere(ts, emptySectionAtLeft, true);
    }
    public void testFillEmptySpacesInRectWithEmptyRight() {

        TreeSet<ScreenLineSegment> ts = buildTreeSetForTestSimple();

        // get segments size (as a rectangle)
        Rectangle bounds = Rectangle.UTIL.calcDimensionsWith(ts);
        //and add (merge) column to right
        Rectangle emptySectionAtRight = new Rectangle.Simple(bounds.getRow(),bounds.getRightMostCol()+1,bounds.getHeight(),1);
        bounds = bounds.mergeAndExpandDimensionsWith(emptySectionAtRight);

        ScreenLineSegment.UTIL.fillEmptySpacesInRectWithChar(fillColor, fillChar, bounds, ts);

        printTS("testFillEmptySpacesInRectWithEmptyRight", ts, bounds);
        assertCharsEverywhere(ts, bounds);
        assertCharsEverywhere(ts, emptySectionAtRight, true);
    }

    public void testFillEmptySpacesInRectWithEmptyBottom() {

        TreeSet<ScreenLineSegment> ts = buildTreeSetForTestSimple();


        // get segments size (as a rectangle)
        Rectangle bounds = Rectangle.UTIL.calcDimensionsWith(ts);
        //and add (merge) row at bottom 
        Rectangle emptySectionAtBottom = new Rectangle.Simple(bounds.getBottomMostRow()+1,0,1,bounds.getWidth());

        bounds = bounds.mergeAndExpandDimensionsWith(emptySectionAtBottom);

        ScreenLineSegment.UTIL.fillEmptySpacesInRectWithChar(fillColor, fillChar, bounds, ts);

        printTS("testFillEmptySpacesInRectWithEmptyBottom", ts, bounds);
        assertCharsEverywhere(ts, bounds);
        assertCharsEverywhere(ts, emptySectionAtBottom, true);
    }

    public void testFillEmptySpacesInRectWithEmptyBoundery() {
        __testFillEmptySpacesInRectWithEmptyBoundery(buildTreeSetForTestSimple());
    }
    public void testFillEmptySpacesInRectWithEmptyBoundery_withGAP() {
        __testFillEmptySpacesInRectWithEmptyBoundery(buildTreeSetForTestWithEmptyGapInMiddle());
    }
    private void __testFillEmptySpacesInRectWithEmptyBoundery(TreeSet<ScreenLineSegment> ts) {

        // get segments size (as a rectangle)
        Rectangle bounds = Rectangle.UTIL.calcDimensionsWith(ts);
        //and add (merge) row at bottom 
        Rectangle emptySectionAtBottom = new Rectangle.Simple(bounds.getBottomMostRow()+1,0,1,bounds.getWidth());
        Rectangle emptySectionAtRight = new Rectangle.Simple(bounds.getRow(),bounds.getRightMostCol()+1,bounds.getHeight(),1);
        Rectangle emptySectionAtLeft = new Rectangle.Simple(bounds.getRow(),-1,bounds.getHeight(),1);
        Rectangle emptySectionAtTop = new Rectangle.Simple(bounds.getRow()-1,0,1,bounds.getWidth());

        bounds = bounds.mergeAndExpandDimensionsWith(emptySectionAtBottom)
                       .mergeAndExpandDimensionsWith(emptySectionAtTop)
                       .mergeAndExpandDimensionsWith(emptySectionAtLeft)
                       .mergeAndExpandDimensionsWith(emptySectionAtRight);

        ScreenLineSegment.UTIL.fillEmptySpacesInRectWithChar(fillColor, fillChar, bounds, ts);

        printTS("__testFillEmptySpacesInRectWithEmptyBoundery", ts, bounds);
        assertCharsEverywhere(ts, bounds);

        assertCharsEverywhere(ts, emptySectionAtBottom, true);
        assertCharsEverywhere(ts, emptySectionAtTop, true);
        assertCharsEverywhere(ts, emptySectionAtLeft, true);
        assertCharsEverywhere(ts, emptySectionAtRight, true);
    }

}
