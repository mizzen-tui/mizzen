package mizzen.util;

import junit.framework.TestCase;

public class IntArrayComparatorTest  extends TestCase {

    public void testArrayEquals() {
        int[] a1 = new int[] {1,2};
        int[] a2 = new int[] {1,2};
        
        assertEquals(0, IntArrayComparator.INTARRAY_COMPARE.compare(a1, a2));
        assertEquals(0, IntArrayComparator.INTARRAY_COMPARE.compare(a2, a1));

        assertEquals(0, IntArrayComparator.INTARRAY_COMPARE.compare(a1, a1));
        assertEquals(0, IntArrayComparator.INTARRAY_COMPARE.compare(a2, a2));
        
    }

    public void testArraylessThan() {
        int[] a1   = new int[] {1,2};
        int[] a2   = new int[] {1,3};
        int[] aBig = new int[] {1,2,3,4};
        assertEquals(-1, IntArrayComparator.INTARRAY_COMPARE.compare(a1, a2));
        assertEquals(-2, IntArrayComparator.INTARRAY_COMPARE.compare(a2, aBig));
    }

    public void testArrayGreaterThan() {
        int[] a1   = new int[] {1,2};
        int[] a2   = new int[] {1,3};
        int[] aBig = new int[] {1,2,3,4};
        assertEquals(1, IntArrayComparator.INTARRAY_COMPARE.compare(a2, a1));
        assertEquals(2, IntArrayComparator.INTARRAY_COMPARE.compare(aBig, a2));
    }
    
}
