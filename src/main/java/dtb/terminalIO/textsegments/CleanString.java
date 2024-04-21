package dtb.terminalIO.textsegments;

/**
 * A clean string is a string without control codes -- chars <= ascii 27
 * 
 * ScreenLineSegment objects need to deal with CleanString's because
 * if it contains any control-codes this wrecks havoc with the rendering logic
 * 
 */
public interface CleanString {
    
    String getText();
    
    default char charAt(int index) {
        return getText().charAt(index);
    }
    
    default CleanString append(CleanString s) {
        return new KnownCleanString(getText()+s.getText());
    }

    default CleanString substring(int beginIndex, int endIndex) {
        String alreadyCleanedString = getText().substring(beginIndex, endIndex);
        return new KnownCleanString(alreadyCleanedString);
    }
    
    default CleanString substring(int beginIndex) {
        String alreadyCleanedString = getText().substring(beginIndex);
        return new KnownCleanString(alreadyCleanedString);
    }

    default boolean contains(String s) {
        return getText().contains(s);
    }
    
    
    default int length() {
        return getText().length();
    }
    
    
    public static class KnownCleanString implements CleanString {
        
        String cleanedStr;
        
        public KnownCleanString(String s) {
            this.cleanedStr = s;
        }

        @Override
        public String getText() {
            return cleanedStr;
        }

        public String toString() {
            return getText();
        }
    }
    
    
    public static class UTIL {
        
        public static CleanString EMPTY_CLEAN_STRING = new Simple("");
        public static CleanString ONE_SPACE_CLEAN_STRING = new Simple(" ");
        
        public static boolean isClean(char ch) {
            return (ch > 27);
        }
        
        public static boolean isClean(String s) {
            if (s == null) {
                return true;
            }
            int sLen = s.length();
            for (int i = 0; i < sLen; i++) {
                char ch = s.charAt(i);
                if (ch <= 27) {
                    return false;
                }
            }
            return true;
        }
        
        public static String cleanSLOW(String s) {
            if (s == null) {
                return null;
            }
            int sLen = s.length();
            StringBuilder sb = new StringBuilder(sLen);
            for (int i = 0; i < sLen; i++) {
                char ch = s.charAt(i);
                if (ch > 27) {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
        
        /**
         * Objective is to return original string if at all possible
         */
        public static String clean(String s) {
            if (s == null) {
                return null;
            }
            int sLen = s.length();
            int start=-1;
            int end=-1;
            StringBuilder sb = null;
            for (int i = 0; i < sLen; i++) {
                char ch = s.charAt(i);
                if (ch > 27) {
                    // char good
                    if (sb == null) {
                       if (start == -1) {
                           start = i;
                       }
                       end = i;
                    } else {
                      sb.append(ch);
                    }
                } else { 
                    // char invalid
                    if (sb == null) {
                        sb = new StringBuilder(s.length());
                        if (end != -1) {
                            sb.append(s.substring(start, end+1));
                        }
                    }
                }
            }
            return sb == null ? s
                              : sb.toString();
        }

    }
    
    public static class Simple implements CleanString {
        
        private String cleanedStr;

        public Simple(String s) {
            super();
            this.cleanedStr = UTIL.clean(s);
        }

        public Simple(char ch) {
            this(ch+"");
        }
        
        public Simple(CleanString cs) {
            this.cleanedStr = cs.getText();
        }
        
        public Simple(Object o) {
            super();
            this.cleanedStr = UTIL.clean(o.toString());
        }
        
        public Simple append(CleanString cs) {
            this.cleanedStr = cleanedStr + cs.getText();
            return this;
        }
        
        public String getText() {
            return cleanedStr;
        }
        
        public String toString() {
            return getText();
        }
        
    }


}
