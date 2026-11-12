package mizzen.util.formatters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Python coloured a line at a time: keywords, strings, comments, numbers, and decorators and defined names as names.
 * <pre>
 *     String coloured = TextLineFormatter.UTIL.format(source, new PythonHighlighter());
 * </pre>
 * <pre>
 *     @dataclass                    a decorator, in the names colour
 *     def total(items):             the name a def or class defines, in the names colour
 *     """Adds them up.              a triple-quoted string may span lines
 *     """
 *     s = f"{n} items" + r'\d' + b'x'   strings with their prefixes (f r b u, either case)
 *     return None  # done           keywords include True, False and None
 * </pre>
 */
public class PythonHighlighter extends CodeHighlighter {

    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(
            "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class", "continue", "def",
            "del", "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in", "is",
            "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while", "with", "yield",
            "match", "case"));

    private String openTriple;                          // a """ or ''' string that started on an earlier line

    @Override
    protected void highlight(String text, StringBuilder out) {
        int i = 0, n = text.length();
        if (openTriple != null) {
            int end = quoteEnd(text, 0, openTriple, true);
            i = end < 0 ? n : end;
            colour(out, stringColor, text.substring(0, i));
            openTriple = end < 0 ? openTriple : null;
        }
        boolean defines = false;                                        // the last word was def or class
        while (i < n) {
            char c = text.charAt(i);
            int quote = stringStart(text, i);
            if (c == '#') {
                colour(out, commentColor, text.substring(i));
                break;
            } else if (quote >= 0) {
                i = string(text, i, quote, out);
            } else if (c == '@' && text.substring(0, i).trim().isEmpty() && i + 1 < n && isWordChar(text.charAt(i + 1))) {
                int j = i + 1;                                          // @name or @a.b.c, at the line's start
                while (j < n && (isWordChar(text.charAt(j)) || text.charAt(j) == '.')) {
                    j++;
                }
                colour(out, nameColor, text.substring(i, j));
                i = j;
            } else if (numberStartsAt(text, i)) {
                int j = numberEnd(text, i);
                colour(out, numberColor, text.substring(i, j));
                i = j;
            } else if (isWordChar(c)) {
                int j = wordEnd(text, i);
                String word = text.substring(i, j);
                if (KEYWORDS.contains(word)) {
                    colour(out, keywordColor, word);
                } else if (defines) {
                    colour(out, nameColor, word);
                } else {
                    out.append(word);
                }
                defines = word.equals("def") || word.equals("class");
                i = j;
                continue;
            } else {
                out.append(c);
                i++;
            }
            if (!Character.isWhitespace(c)) {
                defines = false;
            }
        }
    }

    // where the quotes of a string starting at i begin: just past its prefix (f r b u rb br ...); -1 when none starts
    private static int stringStart(String text, int i) {
        if (i > 0 && isWordChar(text.charAt(i - 1))) {
            return -1;                                                  // inside a word: "if'..'" isn't a prefix
        }
        int j = i;
        while (j < text.length() && j - i < 2 && "rRbBuUfF".indexOf(text.charAt(j)) >= 0) {
            j++;
        }
        return j < text.length() && (text.charAt(j) == '"' || text.charAt(j) == '\'') ? j : -1;
    }

    // the string from start (its prefix) whose quotes begin at quote; returns where it ends. A triple-quoted one may
    // go on to the next lines; a single-quoted one ends at the line's end if unclosed.
    private int string(String text, int start, int quote, StringBuilder out) {
        char q = text.charAt(quote);
        String triple = new String(new char[] { q, q, q });
        boolean isTriple = text.startsWith(triple, quote);
        String closing = isTriple ? triple : String.valueOf(q);
        int end = quoteEnd(text, quote + closing.length(), closing, true);
        int to = end < 0 ? text.length() : end;
        if (end < 0 && isTriple) {
            openTriple = triple;
        }
        colour(out, stringColor, text.substring(start, to));
        return to;
    }
}
