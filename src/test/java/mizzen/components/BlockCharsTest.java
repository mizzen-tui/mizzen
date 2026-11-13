package mizzen.components;

import mizzen.controlcodes.color.Colors;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.ScreenLineSegmentsBuffer;
import junit.framework.TestCase;

public class BlockCharsTest extends TestCase {

    public void testDrawHorizontalLine() {

        assertEquals(BlockChars.NICE.getHORIZONTAL_LINE(), BlockChars.UTIL.drawHorizontal(BlockChars.NICE, 'a'));

        assertEquals(BlockChars.NICE.getHORIZONTAL_LINE(), BlockChars.UTIL.drawHorizontal(BlockChars.NICE, BlockChars.NICE.getHORIZONTAL_LINE()));


        assertEquals(BlockChars.NICE.getHORIZONTAL_UP(), BlockChars.UTIL.drawHorizontal(BlockChars.NICE, BlockChars.NICE.getHORIZONTAL_UP()));
    }

    public void testDrawHorizontalEnd() {

        assertEquals(BlockChars.NICE.getHORIZONTAL_LINE(), BlockChars.UTIL.drawHorizontalEnd(BlockChars.NICE, 'a'));

        assertEquals(BlockChars.NICE.getHORIZONTAL_LINE(), BlockChars.UTIL.drawHorizontalEnd(BlockChars.NICE, BlockChars.NICE.getHORIZONTAL_LINE()));

        assertEquals(BlockChars.NICE.getHORIZONTAL_UP(), BlockChars.UTIL.drawHorizontalEnd(BlockChars.NICE, BlockChars.NICE.getHORIZONTAL_UP()));

        assertEquals(BlockChars.NICE.getVERTICAL_LEFT(), BlockChars.UTIL.drawHorizontalEnd(BlockChars.NICE, BlockChars.NICE.getVERTICAL_LINE()));
    }
    
    public void testDrawBoxAndHorizonal() {
        
        ScreenLineSegmentsBuffer segments = new ScreenLineSegmentsBuffer();
        
        BlockChars.UTIL.drawBox(segments, Colors.IBla, BlockChars.NICE, new Rectangle.Simple(10,10));
        
        char actualTopLeft = segments.getCharAt(0, 0).charAt(0);
        assertEquals(BlockChars.NICE.getTOP_LEFT(), actualTopLeft);
        
        char actualBottomRight = segments.getCharAt(9, 9).charAt(0);
        assertEquals(BlockChars.NICE.getBOTTOM_RIGHT(), actualBottomRight);

        BlockChars.UTIL.drawHorizontalLine(segments,Colors.IBla, BlockChars.NICE, new Position.Simple(5,0),10);
        
        char actualLineStart = segments.getCharAt(5, 0).charAt(0);
        assertEquals(BlockChars.NICE.getVERTICAL_RIGHT(),actualLineStart);

        char actualLineEnd = segments.getCharAt(5, 9).charAt(0);
        assertEquals(BlockChars.NICE.getVERTICAL_LEFT(),actualLineEnd);
        //assertEquals(BlockChars.NICE.getVERTICAL_LEFT(), BlockChars.UTIL.drawVerticalEnd(BlockChars.NICE, BlockChars.NICE.getVERTICAL_LINE()));
        
    }

    /** two line characters on a cell become one, with the arms of both - in either order */
    public void testJoinAddsTheArms() {
        BlockChars n = BlockChars.NICE;
        assertEquals('┼', BlockChars.UTIL.join('│', '─', n));
        assertEquals('┼', BlockChars.UTIL.join('─', '│', n));
        assertEquals("two boxes' corners", '┬', BlockChars.UTIL.join('┐', '┌', n));
        assertEquals('┬', BlockChars.UTIL.join('┌', '┐', n));
        assertEquals('├', BlockChars.UTIL.join('┌', '└', n));
        assertEquals("a corner on a line", '┤', BlockChars.UTIL.join('│', '┐', n));
        assertEquals('┌', BlockChars.UTIL.join('┌', '┌', n));
    }

    /** text over a line, or a line over text: the one drawn over wins, as always */
    public void testJoinLeavesTextAlone() {
        assertEquals('x', BlockChars.UTIL.join('│', 'x', BlockChars.NICE));
        assertEquals('─', BlockChars.UTIL.join('x', '─', BlockChars.NICE));
        assertEquals('⣿', BlockChars.UTIL.join('│', '⣿', BlockChars.NICE));
    }

    /** the result is in the style given, whatever sets the two came from */
    public void testJoinInTheStyleGiven() {
        assertEquals('╬', BlockChars.UTIL.join('│', '─', BlockChars.NICE_DOUBLE));
        assertEquals('┼', BlockChars.UTIL.join('║', '═', BlockChars.NICE));
        assertEquals('╭', BlockChars.UTIL.join('┌', '┌', BlockChars.ROUNDED));
        assertEquals('+', BlockChars.UTIL.join('|', '-', BlockChars.BASIC));
    }

    /** a divider drawn over a box joins it: its ends become ┬ and ┴ on the box's lines (as FrameSet draws them) */
    public void testADividerJoinsTheBoxUnderIt() {
        ScreenLineSegmentsBuffer segments = new ScreenLineSegmentsBuffer();
        BlockChars.UTIL.drawBox(segments, Colors.IBla, BlockChars.NICE, new Rectangle.Simple(5, 10));
        BlockChars.UTIL.drawVerticalLine(segments, Colors.IBla, BlockChars.NICE, new Position.Simple(0, 4), 5);
        assertEquals("┬", segments.getCharAt(0, 4));
        assertEquals("┴", segments.getCharAt(4, 4));
        assertEquals("┌", segments.getCharAt(0, 0));
    }

    // does not work: the horizontal line's end, on an empty cell, is a plain ─ and reads back as reaching both ways - a
    // line can't say where it stops. A box can (drawBox puts its corners as corners)
    public void xtestHorizonalThenVerticalAtEnd() {
        ScreenLineSegmentsBuffer segments = new ScreenLineSegmentsBuffer();
        BlockChars.UTIL.drawHorizontalLine(segments,Colors.IBla, BlockChars.NICE, new Position.Simple(5,0),10);
        BlockChars.UTIL.drawVerticalLine(segments, Colors.IBla, BlockChars.NICE, new Position.Simple(4, 9), 3);

        char actualLineEnd = segments.getCharAt(5, 9).charAt(0);
        assertEquals(BlockChars.NICE.getVERTICAL_LEFT(),actualLineEnd);
    }

    public void testVerticalThenHorizonalAtEnd() {
        ScreenLineSegmentsBuffer segments = new ScreenLineSegmentsBuffer();
        BlockChars.UTIL.drawVerticalLine(segments, Colors.IBla, BlockChars.NICE, new Position.Simple(0, 9), 10);
        BlockChars.UTIL.drawHorizontalLine(segments,Colors.IBla, BlockChars.NICE, new Position.Simple(5,0),10);

        char actualLineEnd = segments.getCharAt(5, 9).charAt(0);
        assertEquals(BlockChars.NICE.getVERTICAL_LEFT(),actualLineEnd);
    }

    public void testVerticalThenHorizonalAtStart() {
        ScreenLineSegmentsBuffer segments = new ScreenLineSegmentsBuffer();
        BlockChars.UTIL.drawVerticalLine(segments, Colors.IBla, BlockChars.NICE, new Position.Simple(0, 0), 10);
        BlockChars.UTIL.drawHorizontalLine(segments,Colors.IBla, BlockChars.NICE, new Position.Simple(5,0),5);

        char actualLineEnd = segments.getCharAt(5, 0).charAt(0);
        assertEquals(BlockChars.NICE.getVERTICAL_RIGHT(),actualLineEnd);
    }
    
    

}
