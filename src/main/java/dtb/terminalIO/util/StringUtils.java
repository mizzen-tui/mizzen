package dtb.terminalIO.util;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
	return Arrays.stream(str.split("\n",-1)).map( s -> __wrap(s, wrapLength, newLineStr, wrapLongWords)).collect(Collectors.joining("\n"));
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
    
    public static String addLineNumbers(String input) {
	if (input == null) {
	    return null;
	}
	
	StringJoiner sj = new StringJoiner("\n");
	String[] lines = input.split("\n");
	for (int i = 0; i< lines.length; i++) {
	    sj.add(String.format("%4d ",i) + lines[i]);
	}
	
	return sj.toString();
    }
    
    
    // This class take from : https://sourceforge.net/p/drjava/git_repo/ci/master/tree/drjava/src/edu/rice/cs/util/ArgumentTokenizer.java#l132
    static public class ArgumentTokenizer {
        private static final int NO_TOKEN_STATE = 0;
        private static final int NORMAL_TOKEN_STATE = 1;
        private static final int SINGLE_QUOTE_STATE = 2;
        private static final int DOUBLE_QUOTE_STATE = 3;
        
        /** Tokenizes the given String into String tokens
          * @param arguments A String containing one or more command-line style arguments to be tokenized.
          * @return A list of parsed and properly escaped arguments.
          */
        public static List<String> tokenize(String arguments) {
          return tokenize(arguments, false);
        }
        
        /** Tokenizes the given String into String tokens.
          * @param arguments A String containing one or more command-line style arguments to be tokenized.
          * @param stringify whether or not to include escape special characters
          * @return A list of parsed and properly escaped arguments.
          */
        public static List<String> tokenize(String arguments, boolean stringify) {
          
          LinkedList<String> argList = new LinkedList<String>();
          StringBuilder currArg = new StringBuilder();
          boolean escaped = false;
          int state = NO_TOKEN_STATE;  // start in the NO_TOKEN_STATE
          int len = arguments.length();
          
          // Loop over each character in the string
          for (int i = 0; i < len; i++) {
            char c = arguments.charAt(i);
            if (escaped) {
              // Escaped state: just append the next character to the current arg.
              escaped = false;
              currArg.append(c);
            }
            else {
              switch(state) {
                case SINGLE_QUOTE_STATE:
                  if (c == '\'') {
                  // Seen the close quote; continue this arg until whitespace is seen
                  state = NORMAL_TOKEN_STATE;
                }
                  else {
                    currArg.append(c);
                  }
                  break;
                case DOUBLE_QUOTE_STATE:
                  if (c == '"') {
                  // Seen the close quote; continue this arg until whitespace is seen
                  state = NORMAL_TOKEN_STATE;
                }
                  else if (c == '\\') {
                    // Look ahead, and only escape quotes or backslashes
                    i++;
                    char next = arguments.charAt(i);
                    if (next == '"' || next == '\\') {
                      currArg.append(next);
                    }
                    else {
                      currArg.append(c);
                      currArg.append(next);
                    }
                  }
                  else {
                    currArg.append(c);
                  }
                  break;
//                case NORMAL_TOKEN_STATE:
//                  if (Character.isWhitespace(c)) {
//                    // Whitespace ends the token; start a new one
//                    argList.add(currArg.toString());
//                    currArg = new StringBuffer();
//                    state = NO_TOKEN_STATE;
//                  }
//                  else if (c == '\\') {
//                    // Backslash in a normal token: escape the next character
//                    escaped = true;
//                  }
//                  else if (c == '\'') {
//                    state = SINGLE_QUOTE_STATE;
//                  }
//                  else if (c == '"') {
//                    state = DOUBLE_QUOTE_STATE;
//                  }
//                  else {
//                    currArg.append(c);
//                  }
//                  break;
                case NO_TOKEN_STATE:
                case NORMAL_TOKEN_STATE:
                  switch(c) {
                  case '\\':
                    escaped = true;
                    state = NORMAL_TOKEN_STATE;
                    break;
                  case '\'':
                    state = SINGLE_QUOTE_STATE;
                    break;
                  case '"':
                    state = DOUBLE_QUOTE_STATE;
                    break;
                  default:
                    if (!Character.isWhitespace(c)) {
                    currArg.append(c);
                    state = NORMAL_TOKEN_STATE;
                  }
                    else if (state == NORMAL_TOKEN_STATE) {
                      // Whitespace ends the token; start a new one
                      argList.add(currArg.toString());
                      currArg = new StringBuilder();
                      state = NO_TOKEN_STATE;
                    }
                }
                  break;
                default:
                  throw new IllegalStateException("ArgumentTokenizer state " + state + " is invalid!");
              }
            }
          }
          
          // If we're still escaped, put in the backslash
          if (escaped) {
            currArg.append('\\');
            argList.add(currArg.toString());
          }
          // Close the last argument if we haven't yet
          else if (state != NO_TOKEN_STATE) {
            argList.add(currArg.toString());
          }
          // Format each argument if we've been told to stringify them
          if (stringify) {
            for (int i = 0; i < argList.size(); i++) {
              argList.set(i, "\"" + _escapeQuotesAndBackslashes(argList.get(i)) + "\"");
            }
          }
          return argList;
        }
        
        /** Inserts backslashes before any occurrences of a backslash or
         * quote in the given string.  Also converts any special characters
         * appropriately.
         */
        protected static String _escapeQuotesAndBackslashes(String s) {
          final StringBuilder buf = new StringBuilder(s);
          
          // Walk backwards, looking for quotes or backslashes.
          //  If we see any, insert an extra backslash into the buffer at
          //  the same index.  (By walking backwards, the index into the buffer
          //  will remain correct as we change the buffer.)
          for (int i = s.length()-1; i >= 0; i--) {
            char c = s.charAt(i);
            if ((c == '\\') || (c == '"')) {
              buf.insert(i, '\\');
            }
            // Replace any special characters with escaped versions
            else if (c == '\n') {
              buf.deleteCharAt(i);
              buf.insert(i, "\\n");
            }
            else if (c == '\t') {
              buf.deleteCharAt(i);
              buf.insert(i, "\\t");
            }
            else if (c == '\r') {
              buf.deleteCharAt(i);
              buf.insert(i, "\\r");
            }
            else if (c == '\b') {
              buf.deleteCharAt(i);
              buf.insert(i, "\\b");
            }
            else if (c == '\f') {
              buf.deleteCharAt(i);
              buf.insert(i, "\\f");
            }
          }
          return buf.toString();
        }
        
    }
    
    public static String injectSpaces(String s) {
        if (s == null || s.length()==0) {
            return s;
        }
        
        StringBuffer sb = new StringBuffer(s.length()*2+30);
        
        int len = s.length();
        
        for (int i=0; i<len; i++) {
            char ch = s.charAt(i);
            sb.append(ch);
            sb.append( ch == '\n' ? '\n' : ' ' );
        }
        return sb.toString();
        
    }
    
    
    
    public static void main(String args[]) {
	
        
        ArgumentTokenizer tkn = new ArgumentTokenizer();
        System.out.println( Arrays.toString(tkn.tokenize("a b c").toArray()));
        
        System.out.println( Arrays.toString(tkn.tokenize("a 'b c' e").toArray()));

        System.out.println( Arrays.toString(tkn.tokenize("\"a is for 'apple jacks'\" 'b c' e").toArray()));
        
        System.out.println( Arrays.toString(tkn.tokenize("a 'b c' e").toArray()));
        
        
//	System.out.println( wrap("How now brown cow",6) );
//
//	System.out.println("-------------------------------------------------\n");
//
//	System.out.println( wrap("Here is some multiline text.\nI wonder how well it will wrap.",10) );
//	
//	System.out.println("-------------------------------------------------\n");
//	
//	System.out.println( wrap("Let's add some "+Colors.Red.asString()+"color"+ControlCode.RCol.asString()+" to the striing "+
//	                         Colors.Blu.asString()+"here"+ControlCode.RCol.asString()+" and then "+Colors.Cya.asString()+"here"+ControlCode.RCol.asString()+".. How did that work?",10) );
	
    }
    
}
