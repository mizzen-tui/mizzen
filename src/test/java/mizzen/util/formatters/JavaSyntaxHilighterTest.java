package mizzen.util.formatters;

import mizzen.controlcodes.color.Colors;
import mizzen.util.SourceFiles;
import junit.framework.TestCase;

/**
 * The Java highlighter colours keywords, strings, comments, numbers and annotations without changing
 * the text; a block comment carries over to the next lines. SourceFiles finds a class's source.
 */
public class JavaSyntaxHilighterTest extends TestCase {

    private static String plain(String formatted) {
        return formatted.replaceAll("\u001b\\[[0-9;]*m", "");
    }

    private static String in(mizzen.controlcodes.color.Color color, String text) {
        return color.asString() + text + Colors.RCol.asString();
    }

    public void testTheTextIsUnchangedOnlyColoured() {
        String line = "    public Integer quantity = 12;    // how many \"so far\"";
        assertEquals(line, plain(new JavaSyntaxHilighterTextFormatter().getFormattedText(line)));
    }

    public void testEachKindGetsItsColour() {
        String out = new JavaSyntaxHilighterTextFormatter().getFormattedText("@Override public int x = 42; String s = \"hi\"; // done");
        assertTrue(out.contains(in(Colors.Yel, "@Override")));
        assertTrue(out.contains(in(Colors.BPur, "public")));
        assertTrue(out.contains(in(Colors.BPur, "int")));
        assertTrue(out.contains(in(Colors.Cya, "42")));
        assertTrue(out.contains(in(Colors.Gre, "\"hi\"")));
        assertTrue(out.contains(in(Colors.IBla, "// done")));
    }

    /** a keyword inside a longer name, and a digit inside a name, are just names */
    public void testOnlyWholeWords() {
        String out = new JavaSyntaxHilighterTextFormatter().getFormattedText("classic = x1;");
        assertEquals("classic = x1;", out);
    }

    public void testAnEscapedQuoteDoesNotEndTheString() {
        String out = new JavaSyntaxHilighterTextFormatter().getFormattedText("s = \"a \\\" b\" + c;");
        assertTrue(out, out.contains(in(Colors.Gre, "\"a \\\" b\"")));
    }

    public void testABlockCommentCarriesOverLines() {
        JavaSyntaxHilighterTextFormatter java = new JavaSyntaxHilighterTextFormatter();
        java.getFormattedText("/* starts here");
        assertEquals(in(Colors.IBla, "   still the comment if public"), java.getFormattedText("   still the comment if public"));
        String after = java.getFormattedText("ends */ int y;");
        assertTrue(after, after.startsWith(in(Colors.IBla, "ends */")) && after.contains(in(Colors.BPur, "int")));
    }

    public void testColoursCanBeSet() {
        String out = new JavaSyntaxHilighterTextFormatter().keywords(Colors.BBlu).getFormattedText("return");
        assertEquals(in(Colors.BBlu, "return"), out);
    }

    public void testTabsBecomeFourSpaces() {
        assertEquals("    x", plain(new JavaSyntaxHilighterTextFormatter().getFormattedText("\tx")));
    }

    /** this test's own source: under src/test/java - not in the jar, not under src/main/java: none */
    public void testSourceFilesFindsAnExampleAndNothingElse() {
        assertNotNull("an example: in target/classes (the build copies them), or under src/main/java",
                SourceFiles.find(mizzen.examples.PartsShopDemo.class));
        assertTrue(SourceFiles.find(mizzen.examples.PartsShopDemo.Sale.class).contains("class PartsShopDemo"));   // nested: its outer file
        assertNull(SourceFiles.find(JavaSyntaxHilighterTest.class));
    }
}
