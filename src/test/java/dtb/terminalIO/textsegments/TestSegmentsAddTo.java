package dtb.terminalIO.textsegments;

import java.util.TreeSet;

import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;
import junit.framework.TestCase;

public class TestSegmentsAddTo  extends TestCase {

    public void testOverlap() {
        
        TreeSet<ScreenLineSegment> ts = ScreenLineSegment.UTIL.makeCollection(null);
    
        SegmentImpl segment1 = new SegmentImpl(Colors.BBlu, 10, 10, 1, "Hello there");
        SegmentImpl segment2 = new SegmentImpl(Colors.BBlu, 11, 10, 1, "I only have eyes, for you.. dear!");
        SegmentImpl segment3 = new SegmentImpl(Colors.BBlu, 10, 15, 1, "My friend");
    
        ts.add(segment1);
        ts.add(segment2);
        
        ScreenLineSegment.UTIL.addTo(ts, segment3);
        
        assertTrue( "segment3 should have been added to ts", ts.contains(segment3) );
        
        assertFalse( "segment3 should have overlapped segment1 causing segment1 to be removed", ts.contains(segment1));
        
        ScreenLineSegment.UTIL.assertNoOverlapsInSegments(ts);
    }
    
}
