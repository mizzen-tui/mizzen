package dtb.terminalIO.geom;


import dtb.terminalIO.geom.position.Position;
import junit.framework.TestCase;

public class RectangleTest extends TestCase {
    
    
    public void testCentered() {
        
        Rectangle outer = new Rectangle.Simple(10,10,100,100);
        
        Position centerPoint = outer.centerPosition();
        
        assertEquals(60, centerPoint.getCol());
        
        
    }

}
