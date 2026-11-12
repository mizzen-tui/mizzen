package mizzen.util.formatters;

import java.util.Locale;

/**
 * XML and HTML (and SVG, XSD ...) coloured a line at a time: tags as keywords, attribute names as names, their values
 * as strings, comments, and entities (&amp;amp;) as numbers. The text between tags stays plain.
 * <pre>
 *     String coloured = TextLineFormatter.UTIL.format(page, new MarkupHighlighter());
 * </pre>
 * <pre>
 *     &lt;?xml version="1.0"?&gt;          the declaration, like a tag
 *     &lt;a href="x.html"                a tag may span lines: its attributes stay attributes
 *        class="link"&gt;Fish &amp;amp; chips&lt;/a&gt;
 *     &lt;!-- a comment --&gt;              may span lines; so may &lt;![CDATA[ .. ]]&gt;, in the strings colour
 *     &lt;script&gt; let n = 1; &lt;/script&gt;   a script's inside is coloured as JavaScript
 * </pre>
 * A style's inside (CSS) stays plain.
 */
public class MarkupHighlighter extends CodeHighlighter {

    private boolean inComment = false;                  // a <!-- .. --> that started on an earlier line
    private boolean inCdata = false;                    // a <![CDATA[ .. ]]> that started on an earlier line
    private boolean inTag = false;                      // inside a tag, among its attributes
    private char valueQuote = 0;                        // inside an attribute's quoted value
    private String tagName = "";                        // the tag being read, in lower case; "" for a closing tag
    private JavaScriptHighlighter script;               // inside <script> .. </script>: its own formatter, or null

    @Override
    protected void highlight(String text, StringBuilder out) {
        int i = 0, n = text.length();
        while (i < n) {
            if (script != null) {
                int end = text.toLowerCase(Locale.ROOT).indexOf("</script", i);
                int to = end < 0 ? n : end;
                script.highlight(text.substring(i, to), out);
                script = end < 0 ? script : null;
                i = to;
            } else if (inComment) {
                int end = text.indexOf("-->", i);
                int to = end < 0 ? n : end + 3;
                colour(out, commentColor, text.substring(i, to));
                inComment = end < 0;
                i = to;
            } else if (inCdata) {
                int end = text.indexOf("]]>", i);
                int to = end < 0 ? n : end + 3;
                colour(out, stringColor, text.substring(i, to));
                inCdata = end < 0;
                i = to;
            } else if (inTag) {
                i = insideTag(text, i, out);
            } else {
                i = betweenTags(text, i, out);
            }
        }
    }

    // the text between tags from i: a comment, CDATA, a tag's start, an entity, or plain text; returns where it stopped
    private int betweenTags(String text, int i, StringBuilder out) {
        int n = text.length();
        if (text.startsWith("<!--", i)) {
            int at = text.indexOf("-->", i + 4);
            int to = at < 0 ? n : at + 3;
            colour(out, commentColor, text.substring(i, to));
            inComment = at < 0;
            return to;
        }
        if (text.startsWith("<![CDATA[", i)) {
            int at = text.indexOf("]]>", i + 9);
            int to = at < 0 ? n : at + 3;
            colour(out, stringColor, text.substring(i, to));
            inCdata = at < 0;
            return to;
        }
        char next = i + 1 < n ? text.charAt(i + 1) : 0;
        if (text.charAt(i) == '<' && (Character.isLetter(next) || next == '/' || next == '?' || next == '!')) {
            int j = i + 1;                                              // <name, </name, <?xml, <!DOCTYPE
            if (next == '/' || next == '?' || next == '!') {
                j++;
            }
            int nameStart = j;
            while (j < n && isNameChar(text.charAt(j))) {
                j++;
            }
            colour(out, keywordColor, text.substring(i, j));
            tagName = next == '/' ? "" : text.substring(nameStart, j).toLowerCase(Locale.ROOT);
            inTag = true;
            return j;
        }
        if (text.charAt(i) == '&') {                                    // &amp; &#169; &#x2014;
            int semi = text.indexOf(';', i);
            if (semi > i + 1 && semi - i <= 12 && text.substring(i + 1, semi).matches("#?[A-Za-z0-9]+")) {
                colour(out, numberColor, text.substring(i, semi + 1));
                return semi + 1;
            }
        }
        int j = i + 1;                                                  // plain text, up to the next < or &
        while (j < n && text.charAt(j) != '<' && text.charAt(j) != '&') {
            j++;
        }
        out.append(text, i, j);
        return j;
    }

    // inside a tag from i: attribute names, = , quoted values, up to its > (or /> or ?>); returns where it stopped
    private int insideTag(String text, int i, StringBuilder out) {
        int n = text.length();
        char c = text.charAt(i);
        if (valueQuote != 0 || c == '"' || c == '\'') {                 // a value, or the rest of one from a line before
            char quote = valueQuote != 0 ? valueQuote : c;
            int end = text.indexOf(quote, valueQuote != 0 ? i : i + 1);
            int to = end < 0 ? n : end + 1;
            colour(out, stringColor, text.substring(i, to));
            valueQuote = end < 0 ? quote : 0;
            return to;
        }
        if (c == '>' || text.startsWith("/>", i) || text.startsWith("?>", i)) {
            int to = c == '>' ? i + 1 : i + 2;
            colour(out, keywordColor, text.substring(i, to));
            inTag = false;
            if (c == '>' && tagName.equals("script")) {
                script = new JavaScriptHighlighter();
                script.keywordColor = keywordColor;
                script.stringColor = stringColor;
                script.commentColor = commentColor;
                script.numberColor = numberColor;
                script.nameColor = nameColor;
            }
            return to;
        }
        if (isNameChar(c)) {
            int j = i;
            while (j < n && isNameChar(text.charAt(j))) {
                j++;
            }
            colour(out, nameColor, text.substring(i, j));
            return j;
        }
        out.append(c);                                                  // spaces, =
        return i + 1;
    }

    // part of a tag's or attribute's name: letters, digits, - _ : . (xml:lang, data-id, v-if)
    private static boolean isNameChar(char c) {
        return isWordChar(c) || c == '-' || c == ':' || c == '.' || c == '@';
    }
}
