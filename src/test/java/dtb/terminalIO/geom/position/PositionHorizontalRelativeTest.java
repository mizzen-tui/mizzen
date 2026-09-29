package dtb.terminalIO.geom.position;

import dtb.terminalIO.geom.Rectangle;
import junit.framework.TestCase;

public class PositionHorizontalRelativeTest extends TestCase {
    
    public void testRelative1() {

        PositionHorizontalRelative pr = new PositionHorizontalRelative(new Rectangle.Simple(11,12,10,10));

        assertEquals(22f + pr.getHorizontalSpacer(), pr.getX());
        assertEquals(22  + pr.getHorizontalSpacer(), pr.getCol());

    }

}
