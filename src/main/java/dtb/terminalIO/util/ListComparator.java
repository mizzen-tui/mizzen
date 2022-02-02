package dtb.terminalIO.util;

import java.util.Comparator;
import java.util.List;

public class ListComparator<E> implements Comparator<List<E>>{

    Comparator<E> elementCompare;
    
    public ListComparator(Comparator<E> elementCompare) {
        this.elementCompare = elementCompare;
    }
    
    @Override
    public int compare(List<E> o1, List<E> o2) {
        if (o1 == o2) {
            return 0;
        }
        if (o1 == null || o2 == null) {
            return o2 == null ? -1 : 1;
        }
        int o1Len =  o1.size();
        int o2Len =  o2.size();
        int minLen = Math.min(o1Len, o2Len);
        for (int i = 0; i < minLen; i++) {
            int cmp = elementCompare.compare(o1.get(i), o2.get(i));
            if (cmp != 0) {
                return cmp;
            }
        }

        // so far equal - but are we different sizes?
        if (o1Len != o2Len) {
            return o1Len - o2Len;
        }

        // all equal
        return 0;
    }

//    @Override
//    public int compare(List<E> o1, List<E> o2) {
//        // TODO Auto-generated method stub
//        return 0;
//    }

}
