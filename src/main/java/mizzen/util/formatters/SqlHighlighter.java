package mizzen.util.formatters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * SQL coloured a line at a time: keywords in any case, strings, comments, numbers, and parameters as names.
 * <pre>
 *     String coloured = TextLineFormatter.UTIL.format(query, new SqlHighlighter());
 * </pre>
 * <pre>
 *     select name from users      keywords: SELECT, select and Select alike
 *     where note = 'it''s'        a string: '' inside it is a quote, not its end; it may span lines
 *     -- a comment                to the line's end; /* .. *&#47; may span lines
 *     "Order" `order` [order]     quoted names stay as they are: never keywords
 *     :id ?id @id $1              parameters in the names colour
 * </pre>
 * The keywords are the common ones across SQL dialects (ANSI, PostgreSQL, MySQL, SQLite), types included, and
 * PL/pgSQL's function words (DECLARE, LOOP, RETURNS ...).
 */
public class SqlHighlighter extends CodeHighlighter {

    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(
            "select", "from", "where", "and", "or", "not", "in", "is", "null", "like", "ilike", "between", "exists",
            "as", "distinct", "all", "any", "some", "join", "inner", "left", "right", "full", "outer", "cross",
            "natural", "on", "using", "group", "by", "order", "having", "limit", "offset", "fetch", "first", "next",
            "rows", "only", "union", "intersect", "except", "asc", "desc", "nulls", "last", "case", "when", "then",
            "else", "end", "insert", "into", "values", "update", "set", "delete", "returning", "create", "alter",
            "drop", "table", "view", "index", "unique", "primary", "foreign", "key", "references", "constraint",
            "default", "check", "if", "cascade", "restrict", "database", "schema", "sequence", "trigger",
            "function", "procedure", "begin", "commit", "rollback", "transaction", "savepoint", "grant", "revoke",
            "with", "recursive", "over", "partition", "window", "true", "false", "cast", "add", "column",
            "rename", "to", "temporary", "temp", "replace", "explain", "analyze", "vacuum", "truncate",
            "returns", "return", "language", "declare", "loop", "for", "while", "elsif", "raise", "notice",
            "exception", "perform", "do", "call", "void", "immutable", "stable", "volatile",
            "int", "integer", "smallint", "bigint", "decimal", "numeric", "real", "double", "precision", "float",
            "char", "varchar", "text", "boolean", "bool", "date", "time", "timestamp", "interval", "serial",
            "blob", "json", "jsonb", "uuid"));

    private boolean inBlockComment = false;             // a /* .. */ that started on an earlier line
    private boolean inString = false;                   // a '...' that started on an earlier line

    @Override
    protected void highlight(String text, StringBuilder out) {
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
            if (inString) {
                i = string(text, i, i, out);
                continue;
            }
            char c = text.charAt(i);
            if (text.startsWith("--", i)) {
                colour(out, commentColor, text.substring(i));
                break;
            } else if (text.startsWith("/*", i)) {
                int end = text.indexOf("*/", i + 2);
                int to = end < 0 ? n : end + 2;
                colour(out, commentColor, text.substring(i, to));
                inBlockComment = end < 0;
                i = to;
            } else if (c == '\'') {
                i = string(text, i, i + 1, out);
            } else if (c == '"' || c == '`' || c == '[') {           // a quoted name: as it is, whatever its words
                int end = text.indexOf(c == '[' ? ']' : c, i + 1);
                int to = end < 0 ? n : end + 1;
                out.append(text, i, to);
                i = to;
            } else if ((c == ':' || c == '?' || c == '@' || c == '$') && i + 1 < n && isWordChar(text.charAt(i + 1))
                    && !text.startsWith("::", i) && (i == 0 || text.charAt(i - 1) != ':')) {
                int j = wordEnd(text, i + 1);
                colour(out, nameColor, text.substring(i, j));
                i = j;
            } else if (numberStartsAt(text, i)) {
                int j = numberEnd(text, i);
                colour(out, numberColor, text.substring(i, j));
                i = j;
            } else if (isWordChar(c)) {
                int j = wordEnd(text, i);
                String word = text.substring(i, j);
                if (KEYWORDS.contains(word.toLowerCase(Locale.ROOT))) {
                    colour(out, keywordColor, word);
                } else {
                    out.append(word);
                }
                i = j;
            } else {
                out.append(c);
                i++;
            }
        }
    }

    // a '...' string from start (its opening quote, or where this line continues it) - '' inside is a quote; returns
    // where it ends, or the line's end when it goes on to the next line
    private int string(String text, int start, int from, StringBuilder out) {
        int j = from;
        while (true) {
            int quote = text.indexOf('\'', j);
            if (quote < 0) {
                colour(out, stringColor, text.substring(start));
                inString = true;
                return text.length();
            }
            if (quote + 1 < text.length() && text.charAt(quote + 1) == '\'') {
                j = quote + 2;                                        // '' : a quote inside the string
                continue;
            }
            colour(out, stringColor, text.substring(start, quote + 1));
            inString = false;
            return quote + 1;
        }
    }
}
