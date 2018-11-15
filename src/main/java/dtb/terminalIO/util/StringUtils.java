package dtb.terminalIO.util;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode;

public class StringUtils {

    public static String repeat(char ch, int count) {
        StringBuffer sb = new StringBuffer(count);
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
    
    
    
    // Wrapping (taken from Apache Command lang )
    //-----------------------------------------------------------------------
    /**
     * <p>Wraps a single line of text, identifying words by <code>' '</code>.</p>
     * 
     * <p>New lines will be separated by the system property line separator.
     * Very long words, such as URLs will <i>not</i> be wrapped.</p>
     * 
     * <p>Leading spaces on a new line are stripped.
     * Trailing spaces are not stripped.</p>
     *
     * <pre>
     * WordUtils.wrap(null, *) = null
     * WordUtils.wrap("", *) = ""
     * </pre>
     *
     * @param str  the String to be word wrapped, may be null
     * @param wrapLength  the column to wrap the words at, less than 1 is treated as 1
     * @return a line with newlines inserted, <code>null</code> if null input
     */
    public static String wrap(String str, int wrapLength) {
        return wrap(str, wrapLength, null, false);
    }
    
    /**
     * <p>Wraps a single line of text, identifying words by <code>' '</code>.</p>
     * 
     * <p>Leading spaces on a new line are stripped.
     * Trailing spaces are not stripped.</p>
     * 
     * <pre>
     * WordUtils.wrap(null, *, *, *) = null
     * WordUtils.wrap("", *, *, *) = ""
     * </pre>
     *
     * @param str  the String to be word wrapped, may be null
     * @param wrapLength  the column to wrap the words at, less than 1 is treated as 1
     * @param newLineStr  the string to insert for a new line, 
     *  <code>null</code> uses the system property line separator
     * @param wrapLongWords  true if long words (such as URLs) should be wrapped
     * @return a line with newlines inserted, <code>null</code> if null input
     */
    public static String wrap(String str, int wrapLength, String newLineStr, boolean wrapLongWords) {
        if (str == null) {
            return null;
        }
	if (str.indexOf('\n')==-1) return __wrap(str, wrapLength, newLineStr, wrapLongWords);
	return Arrays.stream(str.split("\n")).map( s -> __wrap(s, wrapLength, newLineStr, wrapLongWords)).collect(Collectors.joining("\n"));
    }
    public static String __wrap(String str, int wrapLength, String newLineStr, boolean wrapLongWords) {
        if (str == null) {
            return null;
        }
        if (newLineStr == null) {
            newLineStr = System.lineSeparator();
        }
        if (wrapLength < 1) {
            wrapLength = 1;
        }
        int inputLineLength = str.length();
        int offset = 0;
        StringBuffer wrappedLine = new StringBuffer(inputLineLength + 32);
        
        while ((inputLineLength - offset) > wrapLength) {
            if (str.charAt(offset) == ' ') {
                offset++;
                continue;
            }
            int spaceToWrapAt = str.lastIndexOf(' ', wrapLength + offset);
            if (spaceToWrapAt >= offset) {
                // normal case
                wrappedLine.append(str.substring(offset, spaceToWrapAt));
                wrappedLine.append(newLineStr);
                offset = spaceToWrapAt + 1;
                
            } else {
                // really long word or URL
                if (wrapLongWords) {
                    // wrap really long word one line at a time
                    wrappedLine.append(str.substring(offset, wrapLength + offset));
                    wrappedLine.append(newLineStr);
                    offset += wrapLength;
                } else {
                    // do not wrap really long word, just extend beyond limit
                    spaceToWrapAt = str.indexOf(' ', wrapLength + offset);
                    if (spaceToWrapAt >= 0) {
                        wrappedLine.append(str.substring(offset, spaceToWrapAt));
                        wrappedLine.append(newLineStr);
                        offset = spaceToWrapAt + 1;
                    } else {
                        wrappedLine.append(str.substring(offset));
                        offset = inputLineLength;
                    }
                }
            }
        }
        // Whatever is left in line is short enough to just pass through
        wrappedLine.append(str.substring(offset));
        return wrappedLine.toString();
    }
    
    public static void main(String args[]) {
	
	System.out.println( wrap("How now brown cow",6) );

	System.out.println("-------------------------------------------------\n");

	System.out.println( wrap("Here is some multiline text.\nI wonder how well it will wrap.",10) );
	
	System.out.println("-------------------------------------------------\n");
	
	System.out.println( wrap("Let's add some "+Colors.Red.asString()+"color"+ControlCode.RCol.asString()+" to the striing "+
	                         Colors.Blu.asString()+"here"+ControlCode.RCol.asString()+" and then "+Colors.Cya.asString()+"here"+ControlCode.RCol.asString()+".. How did that work?",10) );
	
    }
    
}
