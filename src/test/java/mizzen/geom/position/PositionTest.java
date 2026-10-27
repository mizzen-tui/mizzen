package mizzen.geom.position;

import junit.framework.TestCase;

public class PositionTest extends TestCase {
    
    public void testPositionAdd() {

        Position p = new Position.Simple(10, 10);

        Position offset = new Position.Simple(10, 10);

        Position presult = p.offsetAdd(offset);

        assertEquals(20, presult.getRow()); 
        assertEquals(20, presult.getCol()); 

    }

}
