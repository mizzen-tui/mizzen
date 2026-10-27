package mizzen.util;

import java.util.Comparator;

public class CompareUtil {

    /**
     * Compares 2 objects handling null cases for either o1 or o2 or both
     * 
     * if o1 is null assumes o1 less than o2 if o2 is null assumes o2 less than o1
     * otherwise does a normal compare
     * 
     */
    @SuppressWarnings("unlikely-arg-type")
    public static <T extends Comparable<K>, K> int compareNullableObjects(T o1, K o2) {
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        if (o1.equals(o2)) {
            return 0;
        }
        return o1.compareTo(o2);
    }

    public static <T> Comparator<T> NullableObjectsComparator(Comparator<T> comparator) {
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return 1;
                }
                if (o1.equals(o2)) {
                    return 0;
                }
                return comparator.compare(o1, o2);
            }
        };
    }

    /**
     * Takes a comparator and reverses its result (making it a descending
     * comparator)
     */
    public static <T> Comparator<T> descendingComparator(final Comparator<T> comparator) {
        return new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return comparator.compare(o1, o2) * -1;
            }
        };
    }

    @SafeVarargs
    public static <T> int compositeCompare(T o1, T o2, Comparator<T>... comparators) {
        if (comparators == null || comparators.length < 1) {
            throw new IllegalArgumentException("comparators null or empty in compositeCompare()");
        }
        int result = 0;
        for (int i = 0; i < comparators.length; i++) {
            result = comparators[i].compare(o1, o2);
            if (result != 0) {
                return result;
            }
        }
        return result;
    }

}
