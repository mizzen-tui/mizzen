package mizzen.textsegments;

import junit.framework.TestCase;

public class TestCleanString extends TestCase {
    
    public void testCleanStringSimple() {
        
        assertEquals("ABC",CleanString.UTIL.cleanSLOW("ABC"));

        assertEquals("XYZ",CleanString.UTIL.cleanSLOW("XYZ\u001B"));

        assertEquals("IJK",CleanString.UTIL.cleanSLOW("\u001BIJK"));
        
        assertEquals("LMN",CleanString.UTIL.cleanSLOW("LM\u001BN"));
        
    }

    public void testCleanStringFAST() {
        
        assertEquals("ABC",CleanString.UTIL.clean("ABC"));
        
        assertEquals("XYZ",CleanString.UTIL.clean("XYZ\u001B"));

        assertEquals("IJK",CleanString.UTIL.clean("\u001BIJK"));
        
        assertEquals("LMN",CleanString.UTIL.clean("LM\u001BN"));
        
        assertEquals("11111",CleanString.UTIL.clean("111\u001B11"));
        
    }

}
