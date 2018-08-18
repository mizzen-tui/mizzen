package dtb.terminalIO.util;

public class StringUtils {

    public static String repeat(char ch, int count) {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<count;i++) {
            sb.append(ch);
        }
        return sb.toString();
    }
    
    
}
