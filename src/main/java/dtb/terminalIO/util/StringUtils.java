package dtb.terminalIO.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    // [0;37;5;47;107m
    
    static String colorCODE_patternStr = "\\e\\[[0-9,;]*m";
    static Pattern colorCODE_pattern = Pattern.compile(colorCODE_patternStr);
    
    public static boolean hasColorCodes(String source) {
	Matcher matcher = colorCODE_pattern.matcher(source);
	boolean matchFound = matcher.find();
	return matchFound;
    }
    public static String removeColorCodes(String source) {
	
	Matcher matcher = colorCODE_pattern.matcher(source);
	boolean matchFound = matcher.find();
	if (!matchFound) {
	    return source;
	}
	StringBuffer sb = new StringBuffer();
	while (matchFound) {
	    matcher.appendReplacement(sb, "");
	    matchFound = matcher.find();
	}
	matcher.appendTail(sb);
	
	return sb.toString();
    }
    
}
