package dtb.terminalIO.util;

import java.util.Comparator;

public class IntArrayComparator implements Comparator<int[]> {

    @Override
    public int compare(int[] o1, int[] o2) {
        if (o1 == o2) {
            return 0;
        }
        int o1Len =  o1.length;
        if (o1Len != o2.length) {
            return o1Len - o2.length;
        }
        for (int i=0; i < o1Len; i++) {
            int cmp = o1[i] - o2[i];
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    public static IntArrayComparator INTARRAY_COMPARE = new IntArrayComparator();
    
}
