package dtb.terminalIO.textsegments;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.TreeSet;
import java.util.stream.Collectors;

import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.textsegments.ScreenLineSegment.UTIL;
import junit.framework.TestCase;

public class TestSegmentsSubtract  extends TestCase {


    public void testSubtractSimple1() {
        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("HELLO THERE");
        TreeSet<ScreenLineSegment> ts2 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("XXXXX THERE");

        TreeSet<ScreenLineSegment> diff1 = ScreenLineSegment.UTIL.subtractAggressive(ts1, ts2);

        assertEquals("diff1 size", 1, diff1.size());

        ScreenLineSegment s = diff1.stream().filter((sx)->true).findFirst().get();

        assertEquals("HELLO", s.getText());

    }

    public void testSubtractSimple2() {
        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("HELLO THERE");
        TreeSet<ScreenLineSegment> ts2 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("HELLO XXXXX");
        
        TreeSet<ScreenLineSegment> diff1 = ScreenLineSegment.UTIL.subtractAggressive(ts1, ts2);
        
        assertEquals("diff1 size", 1, diff1.size());

        ScreenLineSegment s = diff1.stream().filter((sx)->true).findFirst().get();

        assertEquals("THERE", s.getText());
        assertEquals(6, s.getCol());
    }

    public void testSubtractSimple3() {
        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("HELLO THERE");
        TreeSet<ScreenLineSegment> ts2 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("HELLO THERE");

        TreeSet<ScreenLineSegment> diff1 = ScreenLineSegment.UTIL.subtractAggressive(ts1, ts2);
        assertEquals("diff1 size", 0, diff1.size());

    }

    
    /**
     * 
     *      Its was a hard day
     *    Here on the west This is what
     *       Twiddle or twaddle
     *       
     *      Its was a soft day
     *    Here on the west This is what
     *       Twiddle or twaddle
     * 
     */
    public void testSubtractSimple3_5() {

        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegmentTestHelper.addElementTreeSegment(null, 0, 2, "Its was a hard day");
                                         ScreenLineSegmentTestHelper.addElementTreeSegment(null, 1, 0, "Here on the west This is what");
                                         ScreenLineSegmentTestHelper.addElementTreeSegment(null, 2, 3, "Twiddle or twaddle");

        TreeSet<ScreenLineSegment> ts2 = ScreenLineSegmentTestHelper.addElementTreeSegment(null, 0, 2, "Its was a soft day");
                                         ScreenLineSegmentTestHelper.addElementTreeSegment(null, 1, 0, "Here on the west This is what");
                                         ScreenLineSegmentTestHelper.addElementTreeSegment(null, 2, 3, "Twiddle or twaddle");

        TreeSet<ScreenLineSegment> diff1 = ScreenLineSegment.UTIL.subtractAggressive(ts1, ts2);
        assertEquals("diff1 size", 1, diff1.size());
        assertEquals("diff1 size", 1, diff1.size());
        ScreenLineSegment s = diff1.stream().filter((sx)->true).findFirst().get();
        assertEquals("hard", s.getText());

    }

    public void testSubtractSimple3_6() {

        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegmentTestHelper.addElementTreeSegment(null, 0, 2, "Its was a soft day");
                                         ScreenLineSegmentTestHelper.addElementTreeSegment(ts1 , 1, 0, "Here on the west This is what");
                                         ScreenLineSegmentTestHelper.addElementTreeSegment(ts1 , 2, 3, "Twiddle or twaddle");

        TreeSet<ScreenLineSegment> ts2 = ScreenLineSegmentTestHelper.addElementTreeSegment(null, 1, 2, "Its was a soft day");
                                         ScreenLineSegmentTestHelper.addElementTreeSegment(ts2 , 2, 0, "Here on the west This is what");
                                         ScreenLineSegmentTestHelper.addElementTreeSegment(ts2 , 3, 3, "Twiddle or twaddle");

        TreeSet<ScreenLineSegment> diff1 = ScreenLineSegment.UTIL.subtractAggressive(ts1, ts2);

        System.out.println( Arrays.toString(diff1.stream().sorted(UTIL.SO_COMPARE).map((s)->s.toString()+"\n").collect(Collectors.toSet()).toArray(new String[] {})));
        
        assertEquals("diff1 size", 5, diff1.size());

    }

    public void testSubtractSimple3_7() {

        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("Its was a soft day");

        TreeSet<ScreenLineSegment> ts2 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("Its was a soft day");

        TreeSet<ScreenLineSegment> diff1 = ScreenLineSegment.UTIL.subtractAggressive(ts1, ts2);
        assertEquals("diff1 size", 0, diff1.size());

    }

    public void testSubtractSimple3_8() {

        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("ABABABABABABABABABA");
        TreeSet<ScreenLineSegment> ts2 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("BABABABABABABABABAB");

        TreeSet<ScreenLineSegment> diff1 = ScreenLineSegment.UTIL.subtractAggressive(ts1, ts2);
        assertEquals("diff1 size", 1, diff1.size());

    }

    public void testSubtractSimple3_9() {

        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("ABABABABABABABABABA");
        TreeSet<ScreenLineSegment> ts2 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("AXAXAXAXAXAXAXAXAXA");

        TreeSet<ScreenLineSegment> diff1 = ScreenLineSegment.UTIL.subtractAggressive(ts1, ts2);
        assertEquals("diff1 size", 9, diff1.size());

    }

    public void testSubtractSimple4() {
        TreeSet<ScreenLineSegment> ts2 = ScreenLineSegmentTestHelper.simpleOneElementTreeSegment("HELLO THERE");

        TreeSet<ScreenLineSegment> ts1 = ScreenLineSegment.UTIL.makeCollection(null);
        int i=0;
        for (char ch : "HXXXX THERE".toCharArray()) {
            SegmentImpl segment = new SegmentImpl(Colors.BBlu, 0, i, 1, ch+"");
            i++;
            ts1.add(segment);
        }

        TreeSet<ScreenLineSegment> diff1 = ScreenLineSegment.UTIL.subtractAggressive(ts1, ts2);
        assertEquals("diff1 size", 4, diff1.size());

        int col =1;
        for (ScreenLineSegment s : diff1) {
            assertEquals("col"+col, "X", s.getText());
            assertEquals(col, s.getCol());
            col++;
        }
    }

}
