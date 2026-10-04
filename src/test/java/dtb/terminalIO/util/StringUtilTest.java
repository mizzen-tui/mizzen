package dtb.terminalIO.util;

import junit.framework.TestCase;

public class StringUtilTest extends TestCase {
    
    public void testStrWidthSimple() {
        assertEquals(5, StringUtils.strWidth("12345"));
    }
    
    public void testStrWidthMultiLine() {
        assertEquals(5, StringUtils.strWidth("abc\n12345\nxyz"));
    }

    public void testStrWidthForWordWrap() {
        assertEquals(5, StringUtils.strWidth( StringUtils.wrap("abc 12345 xyz", 5, "\n", false, 10)));
        assertEquals(5, StringUtils.strWidth( StringUtils.wrap("abc 12345 xyz", 5)));
    }


}
