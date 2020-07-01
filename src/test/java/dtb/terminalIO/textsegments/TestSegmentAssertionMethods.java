package dtb.terminalIO.textsegments;

import java.util.TreeSet;

import junit.framework.TestCase;


/**
 * Ensure that the utility methods
 *  
 * assertUniquePositionsInSegments and assertNoOverlapsInSegments actually work
 * 
 * @author dbalme
 *
 */
public class TestSegmentAssertionMethods extends TestCase {

    
    // verify that assertUniquePositionsInSegments() works as expected
    public void testAssertUniquePositionsInSegmentsNotFound() {
        
        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegmentTestHelper.addElementTreeSegment(null, 0, 0, "Its was a hard day");
        
        ScreenLineSegmentTestHelper.addElementTreeSegment(ts1, 1, 0, "Oh yes it was!");
        
        ScreenLineSegment.UTIL.assertUniquePositionsInSegments(ts1);
        
        
    }

    public void testAssertUniquePositionsInSegmentsFound_1() {
        
        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegmentTestHelper.addElementTreeSegment(null, 0, 0, "Its was a hard day");
        
        ScreenLineSegmentTestHelper.addElementTreeSegment(ts1, 0, 0, "Oh yes it was!");
        
        expectingPositionIllegalStateException( ()-> ScreenLineSegment.UTIL.assertUniquePositionsInSegments(ts1) ) ;
        
    }
    
    public void testAssertNoOverlapsInSegmentsNotFound() {
        
        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegmentTestHelper.addElementTreeSegment(null, 0, 0, "Its was a hard day");
        
        ScreenLineSegmentTestHelper.addElementTreeSegment(ts1, 1, 0, "Oh yes it was!");
        
        ScreenLineSegment.UTIL.assertNoOverlapsInSegments(ts1);
        
    }

    public void testAssertOverlapsFoundInSegmentsFound_1() {
        
        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegmentTestHelper.addElementTreeSegment(null, 0, 0, "Its was a hard day");
        
        ScreenLineSegmentTestHelper.addElementTreeSegment(ts1, 0, 5, "Oh yes it was!");
        
        ScreenLineSegment.UTIL.assertUniquePositionsInSegments(ts1);

        expectingOverlappingSegmentsIllegalStateException( ()-> ScreenLineSegment.UTIL.assertNoOverlapsInSegments(ts1) ) ;
        
    }
    
    
    
    public static void expectingPositionIllegalStateException(Runnable myTestLet) {
        expectingIllegalStateException(myTestLet, "Expected a duplicate position Exception - assert failed!");
    }

    public static void expectingOverlappingSegmentsIllegalStateException(Runnable myTestLet) {
        expectingIllegalStateException(myTestLet, "Expected a overlapping segments exception - assert failed!");
    }
    
    private static void expectingIllegalStateException(Runnable myTestLet, String failMsg) {
        try {
            myTestLet.run();
            TestCase.fail(failMsg);
       } catch (IllegalStateException e) {
           // getting here means success!
       }
   }
    
    
}
