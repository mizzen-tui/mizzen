package mizzen.util.formatters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * JavaScript, or TypeScript, coloured a line at a time: keywords, strings, comments, numbers, and decorators and
 * defined names as names.
 * <pre>
 *     String coloured = TextLineFormatter.UTIL.format(source, new JavaScriptHighlighter());
 *     String typed    = TextLineFormatter.UTIL.format(source, JavaScriptHighlighter.typeScript());
 * </pre>
 * <pre>
 *     function total(items) {        the name a function, class, interface, type or enum defines, in the names colour
 *     const s = `${n} items           a template string may span lines; its ${ } parts in the names colour
 *     `;                             '...' and "..." end at the line's end
 *     /* a comment *&#47;  // and one     a /* .. *&#47; comment may span lines
 *     @Component                     a decorator (TypeScript), in the names colour
 * </pre>
 * TypeScript adds its own words (interface, type, enum, readonly, the basic types ...); plain JavaScript leaves them
 * as names, since code uses them so (a variable called type). A regular expression (/a+b/) is not told apart from
 * division: it shows plain.
 */
public class JavaScriptHighlighter extends CodeHighlighter {

    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(
            "break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do", "else",
            "export", "extends", "finally", "for", "function", "if", "import", "in", "instanceof", "let", "new",
            "return", "super", "switch", "this", "throw", "try", "typeof", "var", "void", "while", "with", "yield",
            "async", "await", "of", "static", "true", "false", "null", "undefined"));

    private static final Set<String> TYPESCRIPT_KEYWORDS = new HashSet<String>(Arrays.asList(
            "type", "interface", "enum", "implements", "private", "protected", "public", "readonly", "abstract",
            "declare", "namespace", "module", "as", "keyof", "is", "infer", "satisfies", "override",
            "any", "unknown", "never", "number", "string", "boolean", "symbol", "bigint", "object"));

    // the words after which a name is defined: function total, class Cart, interface Item ...
    private static final Set<String> DEFINING = new HashSet<String>(Arrays.asList(
            "function", "class", "interface", "type", "enum", "namespace"));

    private final boolean typeScript;
    private boolean inBlockComment = false;             // a /* .. */ that started on an earlier line
    private boolean inTemplate = false;                 // a `...` that started on an earlier line

    /** plain JavaScript */
    public JavaScriptHighlighter() {
        this(false);
    }

    private JavaScriptHighlighter(boolean typeScript) {
        this.typeScript = typeScript;
    }

    /** TypeScript: JavaScript with TypeScript's words as keywords too */
    public static JavaScriptHighlighter typeScript() {
        return new JavaScriptHighlighter(true);
    }

    @Override
    protected void highlight(String text, StringBuilder out) {
        int i = 0, n = text.length();
        boolean defines = false;                                        // the last word defines a name
        while (i < n) {
            if (inBlockComment) {
                int end = text.indexOf("*/", i);
                int to = end < 0 ? n : end + 2;
                colour(out, commentColor, text.substring(i, to));
                inBlockComment = end < 0;
                i = to;
                continue;
            }
            if (inTemplate) {
                i = template(text, i, out);
                continue;
            }
            char c = text.charAt(i);
            if (text.startsWith("//", i)) {
                colour(out, commentColor, text.substring(i));
                break;
            } else if (text.startsWith("/*", i)) {
                int end = text.indexOf("*/", i + 2);
                int to = end < 0 ? n : end + 2;
                colour(out, commentColor, text.substring(i, to));
                inBlockComment = end < 0;
                i = to;
            } else if (c == '\'' || c == '"') {
                int end = quoteEnd(text, i + 1, String.valueOf(c), true);
                int to = end < 0 ? n : end;
                colour(out, stringColor, text.substring(i, to));
                i = to;
            } else if (c == '`') {
                inTemplate = true;
                colour(out, stringColor, "`");
                i = template(text, i + 1, out);
            } else if (c == '@' && i + 1 < n && Character.isLetter(text.charAt(i + 1))) {
                int j = i + 1;                                          // @Component, @Input()
                while (j < n && (isWordChar(text.charAt(j)) || text.charAt(j) == '.')) {
                    j++;
                }
                colour(out, nameColor, text.substring(i, j));
                i = j;
            } else if (numberStartsAt(text, i)) {
                int j = numberEnd(text, i);
                colour(out, numberColor, text.substring(i, j));
                i = j;
            } else if (isWordChar(c) || c == '$') {
                int j = i + 1;
                while (j < n && (isWordChar(text.charAt(j)) || text.charAt(j) == '$')) {
                    j++;
                }
                String word = text.substring(i, j);
                boolean afterDot = i > 0 && text.charAt(i - 1) == '.';  // obj.default, x.type: a property
                if (!afterDot && isKeyword(word)) {
                    colour(out, keywordColor, word);
                } else if (defines) {
                    colour(out, nameColor, word);
                } else {
                    out.append(word);
                }
                defines = !afterDot && DEFINING.contains(word) && isKeyword(word);
                i = j;
                continue;
            } else {
                out.append(c);
                i++;
            }
            if (!Character.isWhitespace(c) && c != '*') {                // function* gen(): still defining
                defines = false;
            }
        }
    }

    private boolean isKeyword(String word) {
        return KEYWORDS.contains(word) || typeScript && TYPESCRIPT_KEYWORDS.contains(word);
    }

    // the inside of a template string from i: text in the strings colour, ${ .. } in the names colour, up to the closing
    // ` (inTemplate false) or the line's end (still true); returns where it stopped
    private int template(String text, int i, StringBuilder out) {
        int n = text.length(), plain = i;
        while (i < n) {
            char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == '`') {
                colour(out, stringColor, text.substring(plain, i + 1));
                inTemplate = false;
                return i + 1;
            } else if (text.startsWith("${", i)) {
                colour(out, stringColor, text.substring(plain, i));
                int j = i + 2, depth = 1;                               // to its own }, past any { } inside
                while (j < n && depth > 0) {
                    depth += text.charAt(j) == '{' ? 1 : text.charAt(j) == '}' ? -1 : 0;
                    j++;
                }
                colour(out, nameColor, text.substring(i, j));
                i = plain = j;
            } else {
                i++;
            }
        }
        colour(out, stringColor, text.substring(plain, Math.min(i, n)));
        return n;
    }
}
