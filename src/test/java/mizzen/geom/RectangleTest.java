package mizzen.geom;


import mizzen.geom.position.Position;
import junit.framework.TestCase;

public class RectangleTest extends TestCase {


    public void testCentered() {
        Rectangle outer = new Rectangle.Simple(10,10,100,100);
        Position centerPoint = outer.centerPosition();
        assertEquals(60, centerPoint.getCol());
    }
    
    public void testMergeWithSmallerCausesNoSizeChange() {
        Rectangle outer = new Rectangle.Simple(10,10,100,100);
        assertTrue(outer.isSameRectangle(  outer.mergeAndExpandDimensionsWith(new Rectangle.Simple(11,11,99,99)) ));
    }

    public void testMergeWithLargerCausesSizeChange() {
        Rectangle outer = new Rectangle.Simple(10,10,100,100);
        Rectangle bigger = new Rectangle.Simple(9,9,101,101);
        assertTrue(bigger.isSameRectangle(  outer.mergeAndExpandDimensionsWith(bigger) ));
    }

    public void testMergeToRightCausesSizeChange() {
        Rectangle outer = new Rectangle.Simple(10,10,100,100);
        Rectangle onTheRight = new Rectangle.Simple(10,110,10,10);
        assertTrue(new Rectangle.Simple(10,10,100,110).isSameRectangle(  outer.mergeAndExpandDimensionsWith(onTheRight) ));
    }

    
}
