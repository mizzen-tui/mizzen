package dtb.terminalIO.textsegments;

/**
 * A clean string is a string without control codes -- chars <= ascii 27
 */
public interface CleanString {
    
    String getText();
    
    default char charAt(int index) {
        return getText().charAt(index);
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
    
    
    static class KnownCleanString implements CleanString {
        
        String cleanedStr;
        
        private KnownCleanString(String s) {
            this.cleanedStr = UTIL.clean(s);
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
        
        public static String clean(String s) {
            if (s == null) {
                return null;
            }
            int sLen = s.length();
            StringBuilder sb = new StringBuilder(sLen);
            
            for (int i = 0; i < sLen; i++) {
                char ch = s.charAt(i);
                if (sb == null) {
                    if (ch <= 27) {
                        sb = new StringBuilder(sLen);
                        if (i>0) {
                            sb.append(s.substring(0, i-1));
                        }
                    }
                } else {
                    if (ch > 27) {
                        sb.append(ch);
                    }
                }
            }
            return sb == null ? s : sb.toString();
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
