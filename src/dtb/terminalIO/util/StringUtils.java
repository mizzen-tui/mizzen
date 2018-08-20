package dtb.terminalIO.util;

public class StringUtils {

    public static String repeat(char ch, int count) {
        StringBuffer sb = new StringBuffer();
        if (count>0) {
            for (int i=0; i<count;i++) {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
    
    public static String truncate(String source, int len) {
	if (source!=null && source.length()>len) {
	    source = source.substring(0, len);
	}
	return source;
    }
    
    public static String center(String source, int len) {
	if (source == null) return source;
	source = truncate(source, len);
	int slen = source.length();
	if (slen==len) return source;
	int oneHalfDiff = (len - slen) /2;
	source = repeat(' ', oneHalfDiff) + source;
	source = source + repeat(' ', len - source.length());
	return source;
    }
    
    
}
