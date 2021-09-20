package dtb.terminalIO.components;

import junit.framework.TestCase;

public class BlockCharsTest extends TestCase {

    public void testDrawHorizontalStart() {

        assertEquals(BlockChars.NICE.getHORIZONTAL_LINE(), BlockChars.UTIL.drawHorizontal(BlockChars.NICE, 'a'));

        assertEquals(BlockChars.NICE.getHORIZONTAL_LINE(), BlockChars.UTIL.drawHorizontal(BlockChars.NICE, BlockChars.NICE.getHORIZONTAL_LINE()));

        assertEquals(BlockChars.NICE.getHORIZONTAL_UP(), BlockChars.UTIL.drawHorizontal(BlockChars.NICE, BlockChars.NICE.getHORIZONTAL_UP()));
    }

}
