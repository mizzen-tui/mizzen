package dtb.terminalIO.util.formatters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class JavaKeywords {

    static final String[] keywords = new String[] {
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "null",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while"
    };
    
    static Set<String> keywordsSet = new HashSet<String>();
    static {
        keywordsSet.addAll( Arrays.asList(keywords)  );
    }
    
    public static boolean isKeyword(String word) {
        if (word == null || word.trim().length() == 0) {
            return false;
        }
        return keywordsSet.contains(word);
    }
    
    
    
}
