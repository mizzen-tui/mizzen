package mizzen.util.formatters;

import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;

/**
 * Source code coloured a line at a time - what the language formatters (BashHighlighter, SqlHighlighter,
 * PythonHighlighter, JavaScriptHighlighter, MarkupHighlighter) share: the colours and their setters, tabs made spaces, and the scanning helpers. A language
 * formatter says only how its lines split into keywords, strings, comments, numbers and names.
 * <pre>
 *     String coloured = TextLineFormatter.UTIL.format(script, new BashHighlighter());
 *     new SqlHighlighter().keywords(Colors.BBlu).comments(Colors.Gre)          // colours of one's own
 *     TextLineFormatter f = CodeHighlighters.forFileName("backup.sh");         // the right one for a file
 * </pre>
 * A formatter may remember earlier lines (a comment or string that spans them), so give it a file's lines in order,
 * one formatter per file. Text it doesn't colour is left as the component shows it.
 * <p/>
 * The default colours are JavaSyntaxHilighterTextFormatter's: they suit a dark background. Tabs become 4 spaces (one
 * character is one column).
 */
public abstract class CodeHighlighter implements TextLineFormatter {

    protected Color keywordColor = Colors.BPur;
    protected Color stringColor = Colors.Gre;
    protected Color commentColor = Colors.IBla;
    protected Color numberColor = Colors.Cya;
    protected Color nameColor = Colors.Yel;             // a language's own kind of name: $VAR in bash, @decorator in Python

    public CodeHighlighter keywords(Color color) {
        keywordColor = color;
        return this;
    }

    public CodeHighlighter strings(Color color) {
        stringColor = color;
        return this;
    }

    public CodeHighlighter comments(Color color) {
        commentColor = color;
        return this;
    }

    public CodeHighlighter numbers(Color color) {
        numberColor = color;
        return this;
    }

    /** a language's own kind of name: variables in bash, decorators and defined names in Python, @params in SQL */
    public CodeHighlighter names(Color color) {
        nameColor = color;
        return this;
    }

    @Override
    public final String getFormattedText(String line) {
        if (line == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        highlight(line.replace("\t", "    "), out);
        return out.toString();
    }

    /** text, one line with no tabs, coloured onto out; state carried to the next line is the formatter's own */
    protected abstract void highlight(String text, StringBuilder out);

    protected static void colour(StringBuilder out, Color color, String text) {
        if (!text.isEmpty()) {
            out.append(color.asString()).append(text).append(Colors.RCol.asString());
        }
    }

    /** where the word starting at from ends: letters, digits and _ */
    protected static int wordEnd(String text, int from) {
        int j = from;
        while (j < text.length() && isWordChar(text.charAt(j))) {
            j++;
        }
        return j;
    }

    protected static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    /**
     * Where a quoted run ends: just past the quote that closes it, or -1 when this line doesn't close it. from is just
     * past the opening quote. backslash: a \ hides the character after it (as in "a\"b").
     * <pre>
     *     quoteEnd("say 'hi' now", 5, "'", false)    is 8
     * </pre>
     */
    protected static int quoteEnd(String text, int from, String quote, boolean backslash) {
        int j = from;
        while (j < text.length()) {
            if (backslash && text.charAt(j) == '\\') {
                j += 2;
            } else if (text.startsWith(quote, j)) {
                return j + quote.length();
            } else {
                j++;
            }
        }
        return -1;
    }

    /** whether a number starts at i: a digit not inside a word ("x2" is a name, "2" a number) */
    protected static boolean numberStartsAt(String text, int i) {
        return Character.isDigit(text.charAt(i)) && (i == 0 || !isWordChar(text.charAt(i - 1)));
    }

    /** where the number starting at i ends: digits, letters (0x1F, 1e5, 10L), dots and _ */
    protected static int numberEnd(String text, int i) {
        int j = i;
        while (j < text.length() && (isWordChar(text.charAt(j)) || text.charAt(j) == '.')) {
            j++;
        }
        return j;
    }
}
