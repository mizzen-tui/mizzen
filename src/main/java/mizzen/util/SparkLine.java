package mizzen.util;

import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

// taken from : https://github.com/RichardWarburton/jspark and modified to allow for multi-line SparkLines

public class SparkLine {

    private static final List<Character> ticks = Arrays.asList('\u2581', 
                                                               '\u2582', 
                                                               '\u2583', 
                                                               '\u2584', 
                                                               '\u2585',
                                                               '\u2586', 
                                                               '\u2587', 
                                                               '\u2588');

    private static final int ticks_maxIndex = ticks.size() - 1;
    
    /**
     * Renders an ASCII graph.
     * 
     * @param values a collection of integers to be rendered
     * @return a String containing an ascii graph of the values
     */
    public static String render(final Collection<Integer> values) {
        return render(values, 1);
    }

    public static String render(final Collection<Integer> values, int height) {
        return render(values, height, Integer.MIN_VALUE, Integer.MAX_VALUE);

    }

    public static String render(final Collection<Integer> values, int height, int currentMax, int currentMin) {
        final int max = Math.max(Collections.max(values),currentMax), min = Math.min(Collections.min(values), currentMin);
        final float scaleForIndex = (max - min) / (((float)ticks_maxIndex+1) * height);
        final float scaleForRow = (float)(max - min) /  (float)height;
        StringBuilder accumulator = new StringBuilder();
        StringJoiner sj = new StringJoiner("\n");
        String debugStr = "height="+height+ "  scaleForIndex = " + scaleForIndex + " scaleForRow = " + scaleForRow + "\n";
        
        for (int row = height; row >= 1; row--) {
            int rowMin = Math.round((row-1) * scaleForRow) + min;
            debugStr +="row = "+row+" rowMin = "+rowMin +" \n";
            for (Integer value : values) {
                char ch = ' ';
                if (rowMin < value) {
                    final int index = Math.round((value - rowMin) / scaleForIndex);
                    if (index > ticks_maxIndex) {
                        ch = ticks.get(ticks_maxIndex);
                    } else {
                        ch = ticks.get(index);
                    }
                } else {
                    ch = ' ';
                }
                accumulator.append(ch);
            }
            sj.add(accumulator.toString());
            accumulator = new StringBuilder();
        }
        return sj.toString();
        //return debugStr+"\n"+sj.toString();
    }

    /**
     * Renders an ASCII sparkline graph.
     * 
     * @param values an arrays of integers to be rendered
     * @return a String containing an ascii graph of the values
     */
    public static String render(final Integer... values) {
        return render(asList(values));
    }

    public static class UTIL {

        private static MapChars SPARKLINECHAR_TO_ASCII = null;

        public synchronized static MapChars getSparklineToAsciiMapChars() {

            if (SPARKLINECHAR_TO_ASCII != null) {
                return SPARKLINECHAR_TO_ASCII;
            }

            SPARKLINECHAR_TO_ASCII = new MapChars();
            
            char[] asciiChars = new char[ticks_maxIndex+1];
            asciiChars[0] = '_';
            asciiChars[1] = '.';
            asciiChars[2] = ';';
            asciiChars[3] = ':';
            asciiChars[4] = 'i';
            asciiChars[5] = '1';
            asciiChars[6] = 'I';
            asciiChars[7] = '#';
            
            
            for (int i=0; i< asciiChars.length; i++) {
                SPARKLINECHAR_TO_ASCII.map(ticks.get(i), asciiChars[i]);
            }

            return SPARKLINECHAR_TO_ASCII;
        }

    }
    
    
    public static void main(String args[]) {
        System.out.println(Arrays.toString(ticks.toArray()));

        for (int height = 1; height <=20 ; height += 1) {
            System.out.println("\n\ntest: height="+height+" \n" + render( Arrays.asList(new Integer[] { 0, 1, 2, 3, 4, 5, 6,4 }), height));
        }
    }

}
