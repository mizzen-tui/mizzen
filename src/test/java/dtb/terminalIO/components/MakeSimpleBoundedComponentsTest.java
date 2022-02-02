package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.color.Colors;
import junit.framework.TestCase;

public class MakeSimpleBoundedComponentsTest extends TestCase {

    public void testMakeBasicCharComponent() {

        CharComponent testC = new CharComponent(Colors.DEFAULT_COLOR, 'x');

        testC.getSegmentsBuffer().getSegments();

    }
    
    
}
