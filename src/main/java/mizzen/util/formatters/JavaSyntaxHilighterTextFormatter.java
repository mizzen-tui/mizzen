package mizzen.util.formatters;

import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;

/**
 * Java source, coloured a line at a time: keywords, strings and chars, comments, numbers, annotations;
 * the rest as the component shows it. A /* comment *&#47; may span lines - the formatter remembers it,
 * so give it the lines in order (one formatter per file).
 * <pre>
 *     String coloured = TextLineFormatter.UTIL.format(source, new JavaSyntaxHilighterTextFormatter());
 *     new JavaSyntaxHilighterTextFormatter().keywords(Colors.BBlu).comments(Colors.Gre)   // colours of one's own
 * </pre>
 * The defaults suit a dark background. Tabs become 4 spaces (one character is one column).
 */
public class JavaSyntaxHilighterTextFormatter implements TextLineFormatter {

    private Color keywordColor = Colors.BPur;
    private Color stringColor = Colors.Gre;
    private Color commentColor = Colors.IBla;
    private Color numberColor = Colors.Cya;
    private Color annotationColor = Colors.Yel;
    private boolean inBlockComment = false;             // a /* .. */ that started on an earlier line

    public JavaSyntaxHilighterTextFormatter keywords(Color color) {
        keywordColor = color;
        return this;
    }

    public JavaSyntaxHilighterTextFormatter strings(Color color) {
        stringColor = color;
        return this;
    }

    public JavaSyntaxHilighterTextFormatter comments(Color color) {
        commentColor = color;
        return this;
    }

    public JavaSyntaxHilighterTextFormatter numbers(Color color) {
        numberColor = color;
        return this;
    }

    public JavaSyntaxHilighterTextFormatter annotations(Color color) {
        annotationColor = color;
        return this;
    }

    @Override
    public String getFormattedText(String line) {
        if (line == null) {
            return null;
        }
        String text = line.replace("\t", "    ");
        StringBuilder out = new StringBuilder();
        int i = 0, n = text.length();
        while (i < n) {
            if (inBlockComment) {
                int end = text.indexOf("*/", i);
                int to = end < 0 ? n : end + 2;
                colour(out, commentColor, text.substring(i, to));
                inBlockComment = end < 0;
                i = to;
                continue;
            }
            char c = text.charAt(i);
            if (text.startsWith("//", i)) {
                colour(out, commentColor, text.substring(i));
                break;
            }
            if (text.startsWith("/*", i)) {
                inBlockComment = true;
                int end = text.indexOf("*/", i + 2);
                int to = end < 0 ? n : end + 2;
                colour(out, commentColor, text.substring(i, to));
                inBlockComment = end < 0;
                i = to;
                continue;
            }
            if (c == '"' || c == '\'') {                     // a string or char, to its closing quote (escapes skipped)
                int j = i + 1;
                while (j < n && text.charAt(j) != c) {
                    j += text.charAt(j) == '\\' ? 2 : 1;
                }
                int to = Math.min(n, j + 1);
                colour(out, stringColor, text.substring(i, to));
                i = to;
                continue;
            }
            if (c == '@' && i + 1 < n && Character.isJavaIdentifierStart(text.charAt(i + 1))) {
                int j = wordEnd(text, i + 1);
                colour(out, annotationColor, text.substring(i, j));
                i = j;
                continue;
            }
            if (Character.isDigit(c) && (i == 0 || !Character.isJavaIdentifierPart(text.charAt(i - 1)))) {
                int j = i;
                while (j < n && (Character.isLetterOrDigit(text.charAt(j)) || text.charAt(j) == '.' || text.charAt(j) == '_')) {
                    j++;
                }
                colour(out, numberColor, text.substring(i, j));
                i = j;
                continue;
            }
            if (Character.isJavaIdentifierStart(c)) {
                int j = wordEnd(text, i);
                String word = text.substring(i, j);
                if (JavaKeywords.isKeyword(word) || word.equals("true") || word.equals("false") || word.equals("null")) {
                    colour(out, keywordColor, word);
                } else {
                    out.append(word);
                }
                i = j;
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static int wordEnd(String text, int from) {
        int j = from;
        while (j < text.length() && Character.isJavaIdentifierPart(text.charAt(j))) {
            j++;
        }
        return j;
    }

    private static void colour(StringBuilder out, Color color, String text) {
        out.append(color.asString()).append(text).append(Colors.RCol.asString());
    }
}
