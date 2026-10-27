package mizzen.components;

import junit.framework.TestCase;

public class MakeTextComponentTest extends TestCase {

    
    public void testMakeBasicTextComponent() {
        
        Component.UTIL.COMPONENT_SEGMENTS_INTEGRITY_CHECK_FLAG = true;

        TextComponent testC = new TextComponent("Hello world!\nPleased to make your aquaintance!");

        testC.getSegmentsBuffer().getSegments();

    }

}
