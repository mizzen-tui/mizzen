package dtb.terminalIO.textsegments;

import java.util.TreeSet;

import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;

/**
 * Some helper methods for creating test ScreenLineSegment used in
 * various ScreenLineSegment tests
 *   
 * @author dbalme
 *
 */

public class ScreenLineSegmentTestHelper {

    public static TreeSet<ScreenLineSegment> addElementTreeSegment(TreeSet<ScreenLineSegment> ts, int row, int col, String text) {
        if (ts == null) {
            ts = ScreenLineSegment.UTIL.makeCollection(null);
        }
    
        SegmentImpl segment = new SegmentImpl(Colors.BBlu, row, col, 1, text);
    
        ts.add(segment);
    
        return ts;
    }

    public static TreeSet<ScreenLineSegment> simpleOneElementTreeSegment(String text) {
        return addElementTreeSegment(null, 0, 0, text);
    }

}
