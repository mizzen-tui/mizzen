package mizzen.util.formatters;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bash (and sh) coloured a line at a time: keywords, strings, comments, numbers, and variables as names.
 * <pre>
 *     String coloured = TextLineFormatter.UTIL.format(script, new BashHighlighter());
 * </pre>
 * What bash makes unusual, and how it is coloured:
 * <pre>
 *     echo $# ${#list[@]} a#b     # only a # that starts a word starts a comment
 *     msg="two                    a quoted string may span lines: the next line stays a string
 *     lines"
 *     cat &lt;&lt;EOF                    a heredoc: its lines are a string, up to the EOF line
 *     $USER ${HOME} $1 $? $(date)  variables (and $( )) in the names colour, inside "..." too; never in '...'
 * </pre>
 * Keywords are bash's own (if, then, for, do, case, function, local, export ...), not commands like echo or grep.
 */
public class BashHighlighter extends CodeHighlighter {

    private static final Set<String> KEYWORDS = new HashSet<String>(Arrays.asList(
            "if", "then", "else", "elif", "fi", "case", "esac", "for", "select", "while", "until", "do", "done", "in",
            "function", "time", "coproc", "return", "break", "continue", "exit", "local", "declare", "typeset",
            "export", "readonly", "unset", "shift", "source", "trap", "eval", "exec", "let", "set"));

    // <<EOF, <<-EOF, << 'EOF', <<"EOF" - not <<< (a here-string)
    private static final Pattern HEREDOC = Pattern.compile("<<(-?)\\s*(['\"]?)([A-Za-z_][A-Za-z0-9_]*)\\2");

    private char openQuote = 0;                         // a ' or " string that started on an earlier line
    private String heredocEnd;                          // inside a heredoc: the line that ends it
    private boolean heredocTabs;                        // <<- : the end line may start with tabs (made spaces here)
    private String pendingHeredocEnd;                   // a heredoc opened on this line: it starts on the next
    private boolean pendingHeredocTabs;

    @Override
    protected void highlight(String text, StringBuilder out) {
        if (heredocEnd != null) {
            colour(out, stringColor, text);
            if ((heredocTabs ? text.replaceAll("^ +", "") : text).equals(heredocEnd)) {
                heredocEnd = null;
            }
            return;
        }
        int i = 0, n = text.length();
        if (openQuote != 0) {
            i = string(text, 0, out, openQuote);
        }
        int heredoc;
        while (i < n) {
            char c = text.charAt(i);
            if (c == '\\') {                                         // \" \$ \# : the character itself, nothing more
                out.append(text, i, Math.min(n, i + 2));
                i += 2;
            } else if (c == '#' && startsWord(text, i)) {
                colour(out, commentColor, text.substring(i));
                break;
            } else if (c == '\'' || c == '"') {
                i = string(text, i + 1, out, c);
            } else if (c == '$') {
                i = variable(text, i, out);
            } else if (c == '<' && (heredoc = heredocAt(text, i)) > 0) {
                colour(out, stringColor, text.substring(i, heredoc));
                i = heredoc;
            } else if (numberStartsAt(text, i)) {                     // a number is all digits: 2d and 1.5.tar aren't
                int j = wordEnd(text, i);
                boolean digits = text.substring(i, j).chars().allMatch(Character::isDigit);
                if (digits && (j == n || text.charAt(j) != '.' && text.charAt(j) != '/' && text.charAt(j) != '-')) {
                    colour(out, numberColor, text.substring(i, j));
                } else {
                    out.append(text, i, j);
                }
                i = j;
            } else if (isWordChar(c)) {
                int j = wordEnd(text, i);
                String word = text.substring(i, j);
                boolean alone = (i == 0 || !isPathChar(text.charAt(i - 1))) && (j == n || !isPathChar(text.charAt(j)));
                if (alone && KEYWORDS.contains(word)) {
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
        if (pendingHeredocEnd != null) {
            heredocEnd = pendingHeredocEnd;
            heredocTabs = pendingHeredocTabs;
            pendingHeredocEnd = null;
        }
    }

    // a word starts at i: the line's start, or after a space or one of ; | & ( ) - so $# and a#b aren't comments
    private static boolean startsWord(String text, int i) {
        return i == 0 || " ;|&()".indexOf(text.charAt(i - 1)) >= 0;
    }

    // part of a path or option, where a keyword is only a word: /usr/bin/time, --in, done.txt
    private static boolean isPathChar(char c) {
        return isWordChar(c) || c == '/' || c == '.' || c == '-';
    }

    // the string from `from` (just past its opening quote, or the line's start if it began earlier); returns where it
    // ends. "..." colours the variables in it; '...' takes everything as it is.
    private int string(String text, int from, StringBuilder out, char quote) {
        int start = openQuote != 0 && from == 0 ? 0 : from - 1;
        int end = quoteEnd(text, from, String.valueOf(quote), quote == '"');
        int to = end < 0 ? text.length() : end;
        openQuote = end < 0 ? quote : 0;
        if (quote == '\'') {
            colour(out, stringColor, text.substring(start, to));
            return to;
        }
        int i = start, plain = start;                            // "...": a string with its $variables picked out
        while (i < to) {
            char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == '$' && i + 1 < to) {
                colour(out, stringColor, text.substring(plain, i));
                i = variable(text.substring(0, to), i, out);
                plain = i;
            } else {
                i++;
            }
        }
        colour(out, stringColor, text.substring(plain, Math.min(to, text.length())));
        return to;
    }

    // $NAME, $1, $@ $# $? $$ $! $* $-, ${...} to its brace, $( or $(( as the opening only; returns where it ends
    private int variable(String text, int i, StringBuilder out) {
        int n = text.length();
        int j = i + 1;
        if (j < n && text.charAt(j) == '{') {
            int close = text.indexOf('}', j);
            j = close < 0 ? n : close + 1;
        } else if (j < n && text.charAt(j) == '(') {
            j += text.startsWith("((", j) ? 2 : 1;
        } else if (j < n && (Character.isLetter(text.charAt(j)) || text.charAt(j) == '_')) {
            j = wordEnd(text, j);
        } else if (j < n && "0123456789@#?$!*-".indexOf(text.charAt(j)) >= 0) {
            j++;
        } else {
            out.append('$');                                     // a lone $: just the character
            return i + 1;
        }
        colour(out, nameColor, text.substring(i, j));
        return j;
    }

    // a heredoc's opening at i (<<EOF, not <<< a here-string): where it ends, its end line remembered for the lines after this one; -1: none
    private int heredocAt(String text, int i) {
        if (!text.startsWith("<<", i) || text.startsWith("<<<", i) || i > 0 && text.charAt(i - 1) == '<') {
            return -1;
        }
        Matcher m = HEREDOC.matcher(text);
        if (!m.find(i) || m.start() != i) {
            return -1;
        }
        pendingHeredocEnd = m.group(3);
        pendingHeredocTabs = !m.group(1).isEmpty();
        return m.end();
    }
}
