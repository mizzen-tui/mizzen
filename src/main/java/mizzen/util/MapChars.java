package mizzen.util;

import java.util.HashMap;
import java.util.Map;

import mizzen.components.BlockChars;
import mizzen.components.plot.braille.BrailleUtils;

/**
 * Utility class to map characters from one character to another
 */
public class MapChars {
    
    private Map<Character, Character> charMAP = new HashMap<Character, Character>();
    
    public void map(char key, char value) {
        charMAP.put(key, value);
    }

    public void map(char[] mappings) {
        if (mappings == null) {
            throw new NullPointerException("mappings array was null!");
        }
        if (mappings.length<2 || mappings.length % 2 != 0) {
            throw new IllegalArgumentException("mappings array is not the correct length: length = " + mappings.length);
        }
        for (int i=0 ; i<mappings.length; i+= 2) {
            map(mappings[i], mappings[i+1]);
        }
    }

    public void mapAll(Map<Character, Character> mappedChars) {
        charMAP.putAll(mappedChars);
    }

    public void mapAll(MapChars mapChars) {
        mapAll(mapChars.charMAP);
    }
    
    public char get(char key) {
        Character value = charMAP.get(key);
        if (value != null) {
            return value;
        }
        return key;
    }
    
    public String getMappedString(String s) {
        if (s == null) {
            return null;
        }
        StringBuilder sb = null;
        int len = s.length();
        int i = 0;
        for ( ; i < len; i++) {
            char ch = s.charAt(i);
            char mappedCH = get(ch);
            if (mappedCH != ch) {
                sb = new StringBuilder(len);
                if (i>0) {
                   sb.append(s.substring(0, i));
                }
                sb.append(mappedCH);
                break;
            }
        }
        if (sb == null) {
            return s;
        }
        i++;
        while (i<len) {
            sb.append(get(s.charAt(i)));
            i++;
        }
        return sb.toString();
    }
    
    public static void main(String args[]) {
        
        MapChars mapChars = new MapChars();

        mapChars.map(  new char[] {
                         'A','B',
                         'C','D',
                         'x','Y'
                         }
                    );


        mapChars.mapAll(BlockChars.UTIL.getNICE_TO_BASIC()  );
        mapChars.mapAll(BrailleUtils.getBrailleToAsciiMapChars());


        System.out.println(" A = "   + mapChars.get('A'));
        System.out.println(" B = "   + mapChars.get('B'));
        System.out.println(" C = "   + mapChars.get('D'));
        System.out.println(" D = "   + mapChars.get('D'));


        System.out.println(mapChars.getMappedString("Hello there David"));

        System.out.println(mapChars.getMappedString("Hello there Charles"));
        System.out.println(mapChars.getMappedString("Hello there Charles"));
        System.out.println(mapChars.getMappedString("⠆⠆⠁CCCAT in the HAT"));
        System.out.println(mapChars.getMappedString( BlockChars.NICE.getHORIZONTAL_DOWN() +  " Hello there Charles"));
    }

}
