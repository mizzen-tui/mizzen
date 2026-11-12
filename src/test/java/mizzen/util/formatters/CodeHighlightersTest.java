package mizzen.util.formatters;

import junit.framework.TestCase;
import mizzen.controlcodes.color.Colors;

/**
 * The bash, SQL, Python, JavaScript (and TypeScript) and markup (XML, HTML) formatters, and finding one for a file. Coloured text is read back as letters, so a line's
 * expected colouring reads as text: k[keyword] s[string] c[comment] n[number] v[name], the rest plain.
 */
public class CodeHighlightersTest extends TestCase {

    // ---- bash ----

    public void testBashKeywordsStringsComments() {
        assertEquals("k[if] [ -f s[\"x\"] ]; k[then] echo s['a b'] c[# done]",
                marked(new BashHighlighter(), "if [ -f \"x\" ]; then echo 'a b' # done"));
    }

    public void testBashHashOnlyStartsACommentAtAWord() {
        assertEquals("echo v[$#] v[${#list[@]}] a#b c[#real]", marked(new BashHighlighter(), "echo $# ${#list[@]} a#b #real"));
    }

    public void testBashVariablesInDoubleQuotesNotSingle() {
        assertEquals("echo s[\"hi ]v[$USER]s[ in ]v[${HOME}]s[\"] s['$USER']",
                marked(new BashHighlighter(), "echo \"hi $USER in ${HOME}\" '$USER'"));
    }

    public void testBashKeywordsOnlyAsWords() {
        assertEquals("/usr/bin/time ls done.txt --in k[done]", marked(new BashHighlighter(), "/usr/bin/time ls done.txt --in done"));
    }

    public void testBashStringAcrossLines() {
        BashHighlighter bash = new BashHighlighter();
        assertEquals("msg=s[\"two]", marked(bash, "msg=\"two"));
        assertEquals("s[lines\"] k[fi]", marked(bash, "lines\" fi"));
    }

    public void testBashHeredocLinesAreAString() {
        BashHighlighter bash = new BashHighlighter();
        assertEquals("cat s[<<'EOF'] >out", marked(bash, "cat <<'EOF' >out"));
        assertEquals("s[if $x then # not code]", marked(bash, "if $x then # not code"));
        assertEquals("s[EOF]", marked(bash, "EOF"));
        assertEquals("k[fi]", marked(bash, "fi"));
    }

    public void testBashIndentedHeredocEnd() {
        BashHighlighter bash = new BashHighlighter();
        marked(bash, "cat <<-END");
        assertEquals("s[    text]", marked(bash, "\ttext"));
        assertEquals("s[    END]", marked(bash, "\tEND"));
        assertEquals("k[done]", marked(bash, "done"));
    }

    public void testBashHereStringIsNotAHeredoc() {
        BashHighlighter bash = new BashHighlighter();
        marked(bash, "read x <<< word");
        assertEquals("k[fi]", marked(bash, "fi"));
    }

    public void testBashNumbers() {
        assertEquals("k[exit] n[2]; sleep n[10]; cd 2d", marked(new BashHighlighter(), "exit 2; sleep 10; cd 2d"));
    }

    // ---- SQL ----

    public void testSqlKeywordsInAnyCase() {
        assertEquals("k[SELECT] name k[from] users k[Where] id = n[42]",
                marked(new SqlHighlighter(), "SELECT name from users Where id = 42"));
    }

    public void testSqlDoubledQuoteStaysInTheString() {
        assertEquals("k[select] s['it''s'] k[as] x", marked(new SqlHighlighter(), "select 'it''s' as x"));
    }

    public void testSqlComments() {
        SqlHighlighter sql = new SqlHighlighter();
        assertEquals("k[select] n[1] c[-- one]", marked(sql, "select 1 -- one"));
        assertEquals("c[/* spans]", marked(sql, "/* spans"));
        assertEquals("c[lines */] k[select]", marked(sql, "lines */ select"));
    }

    public void testSqlQuotedNamesAreNotKeywords() {
        assertEquals("k[select] \"order\", `from`, [select] k[from] t",
                marked(new SqlHighlighter(), "select \"order\", `from`, [select] from t"));
    }

    public void testSqlParametersButNotCasts() {
        assertEquals("k[where] id = v[:id] k[and] x = v[@x] k[and] y::k[int]",
                marked(new SqlHighlighter(), "where id = :id and x = @x and y::int"));
    }

    // ---- Python ----

    public void testPythonDefAndDecorator() {
        PythonHighlighter py = new PythonHighlighter();
        assertEquals("v[@dataclass]", marked(py, "@dataclass"));
        assertEquals("k[def] v[total](items): c[# sum]", marked(py, "def total(items): # sum"));
    }

    public void testPythonStringPrefixes() {
        assertEquals("s = s[f\"{n} items\"] + s[r'\\d'] + s[b'x']", marked(new PythonHighlighter(), "s = f\"{n} items\" + r'\\d' + b'x'"));
    }

    public void testPythonTripleQuotedAcrossLines() {
        PythonHighlighter py = new PythonHighlighter();
        assertEquals("    s[\"\"\"Adds them up.]", marked(py, "    \"\"\"Adds them up."));
        assertEquals("s[    # not a comment]", marked(py, "    # not a comment"));
        assertEquals("s[    \"\"\"]", marked(py, "    \"\"\""));
        assertEquals("    k[return] k[None]", marked(py, "    return None"));
    }

    public void testPythonNumbersAndNames() {
        assertEquals("x2 = n[0x1F] + n[1.5e3]", marked(new PythonHighlighter(), "x2 = 0x1F + 1.5e3"));
    }

    // ---- JavaScript and TypeScript ----

    public void testJavaScriptKeywordsStringsComments() {
        assertEquals("k[const] x = s['a'] + s[\"b\\\"c\"]; c[// done]",
                marked(new JavaScriptHighlighter(), "const x = 'a' + \"b\\\"c\"; // done"));
    }

    public void testJavaScriptDefinedNamesAndProperties() {
        assertEquals("k[function] v[total](items) { k[return] items.length; }",
                marked(new JavaScriptHighlighter(), "function total(items) { return items.length; }"));
        assertEquals("k[export] k[default] obj.default", marked(new JavaScriptHighlighter(), "export default obj.default"));
    }

    public void testJavaScriptTemplateAcrossLines() {
        JavaScriptHighlighter js = new JavaScriptHighlighter();
        assertEquals("s = s[`]v[${n}]s[ items]", marked(js, "s = `${n} items"));
        assertEquals("s[and ]v[${f({a: 1})}]s[`]; k[let] y", marked(js, "and ${f({a: 1})}`; let y"));
    }

    public void testJavaScriptBlockCommentAcrossLines() {
        JavaScriptHighlighter js = new JavaScriptHighlighter();
        assertEquals("n[1] c[/* two]", marked(js, "1 /* two"));
        assertEquals("c[lines */] k[null]", marked(js, "lines */ null"));
    }

    public void testTypeScriptWordsOnlyInTypeScript() {
        assertEquals("k[interface] v[Item] { name: k[string]; }",
                marked(JavaScriptHighlighter.typeScript(), "interface Item { name: string; }"));
        assertEquals("type = string", marked(new JavaScriptHighlighter(), "type = string"));
        assertEquals("v[@Component]({})", marked(JavaScriptHighlighter.typeScript(), "@Component({})"));
    }

    // ---- XML and HTML ----

    public void testMarkupTagsAttributesEntities() {
        assertEquals("k[<a] v[href]=s[\"x.html\"]k[>]Fish n[&amp;] chipsk[</a]k[>]",
                marked(new MarkupHighlighter(), "<a href=\"x.html\">Fish &amp; chips</a>"));
        assertEquals("k[<?xml] v[version]=s[\"1.0\"]k[?>]", marked(new MarkupHighlighter(), "<?xml version=\"1.0\"?>"));
        assertEquals("k[<br]k[/>] a < b", marked(new MarkupHighlighter(), "<br/> a < b"));
    }

    public void testMarkupTagAcrossLines() {
        MarkupHighlighter xml = new MarkupHighlighter();
        assertEquals("k[<dependency]", marked(xml, "<dependency"));
        assertEquals("    v[scope]=s['test]", marked(xml, "    scope='test"));
        assertEquals("s[ed']k[>]text", marked(xml, "ed'>text"));
    }

    public void testMarkupCommentAndCdataAcrossLines() {
        MarkupHighlighter xml = new MarkupHighlighter();
        assertEquals("k[<p]k[>] c[<!-- one]", marked(xml, "<p> <!-- one"));
        assertEquals("c[<b> -->] k[<b]k[>]", marked(xml, "<b> --> <b>"));
        assertEquals("s[<![CDATA[ <x>]", marked(xml, "<![CDATA[ <x>"));
        assertEquals("s[]]>]k[</p]k[>]", marked(xml, "]]></p>"));
    }

    public void testHtmlScriptIsJavaScript() {
        MarkupHighlighter html = new MarkupHighlighter();
        assertEquals("k[<script]k[>]", marked(html, "<script>"));
        assertEquals("  k[let] n = n[1]; c[// <b>]", marked(html, "  let n = 1; // <b>"));
        assertEquals("k[if] (a<b) x(); k[</script]k[>] if", marked(html, "if (a<b) x(); </script> if"));
    }

    // ---- finding one ----

    public void testByLanguageName() {
        assertTrue(CodeHighlighters.forLanguage("Bash") instanceof BashHighlighter);
        assertTrue(CodeHighlighters.forLanguage("sh") instanceof BashHighlighter);
        assertTrue(CodeHighlighters.forLanguage("SQL") instanceof SqlHighlighter);
        assertTrue(CodeHighlighters.forLanguage("py") instanceof PythonHighlighter);
        assertTrue(CodeHighlighters.forLanguage("java") instanceof JavaSyntaxHilighterTextFormatter);
        assertTrue(CodeHighlighters.forLanguage("js") instanceof JavaScriptHighlighter);
        assertTrue(CodeHighlighters.forLanguage("html") instanceof MarkupHighlighter);
        assertNull(CodeHighlighters.forLanguage("cobol"));
        assertNull(CodeHighlighters.forLanguage(null));
    }

    public void testByFileName() {
        assertTrue(CodeHighlighters.forFileName("/opt/run.sh") instanceof BashHighlighter);
        assertTrue(CodeHighlighters.forFileName("home/.bashrc") instanceof BashHighlighter);
        assertTrue(CodeHighlighters.forFileName("Report.SQL") instanceof SqlHighlighter);
        assertTrue(CodeHighlighters.forFileName("tool.py") instanceof PythonHighlighter);
        assertTrue(CodeHighlighters.forFileName("A.java") instanceof JavaSyntaxHilighterTextFormatter);
        assertTrue(CodeHighlighters.forFileName("app.mjs") instanceof JavaScriptHighlighter);
        assertTrue(CodeHighlighters.forFileName("package.json") instanceof JavaScriptHighlighter);
        assertEquals("k[enum]", marked(CodeHighlighters.forFileName("a.ts"), "enum"));
        assertEquals("enum", marked(CodeHighlighters.forFileName("a.js"), "enum"));
        assertTrue(CodeHighlighters.forFileName("pom.xml") instanceof MarkupHighlighter);
        assertTrue(CodeHighlighters.forFileName("index.HTML") instanceof MarkupHighlighter);
        assertTrue(CodeHighlighters.forFileName("logo.svg") instanceof MarkupHighlighter);
        assertNull(CodeHighlighters.forFileName("notes.txt"));
        assertNull(CodeHighlighters.forFileName("deploy"));
    }

    public void testByShebangWhenTheNameSaysNothing() {
        assertTrue(CodeHighlighters.forFile("deploy", "#!/bin/bash") instanceof BashHighlighter);
        assertTrue(CodeHighlighters.forFile("deploy", "#!/bin/sh -e") instanceof BashHighlighter);
        assertTrue(CodeHighlighters.forFile("tool", "#!/usr/bin/env python3") instanceof PythonHighlighter);
        assertTrue(CodeHighlighters.forFile("tool", "#!/usr/bin/env -S bash -e") instanceof BashHighlighter);
        assertTrue(CodeHighlighters.forFile("serve", "#!/usr/bin/env node") instanceof JavaScriptHighlighter);
        assertNull(CodeHighlighters.forFile("tool", "#!/usr/bin/perl"));
        assertNull(CodeHighlighters.forFile("tool", "echo hi"));
        assertNull(CodeHighlighters.forFile("tool", null));
        assertTrue("the name wins", CodeHighlighters.forFile("q.sql", "#!/bin/bash") instanceof SqlHighlighter);
    }

    public void testEachCallIsANewFormatter() {
        assertNotSame(CodeHighlighters.forLanguage("bash"), CodeHighlighters.forLanguage("bash"));
    }

    public void testTabsBecomeSpaces() {
        assertEquals("    k[fi]", marked(new BashHighlighter(), "\tfi"));
    }

    // the formatted line with its colours as letters: k[..] keyword, s[..] string, c[..] comment, n[..] number, v[..] name
    private static String marked(TextLineFormatter formatter, String line) {
        return formatter.getFormattedText(line)
                .replace(Colors.BPur.asString(), "k[")
                .replace(Colors.Gre.asString(), "s[")
                .replace(Colors.IBla.asString(), "c[")
                .replace(Colors.Cya.asString(), "n[")
                .replace(Colors.Yel.asString(), "v[")
                .replace(Colors.RCol.asString(), "]");
    }
}
