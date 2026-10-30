package mizzen.components.markdown;

import java.util.List;

import mizzen.components.markdown.MarkdownDocument.Block;
import mizzen.components.markdown.MarkdownDocument.BlockKind;
import mizzen.components.markdown.MarkdownDocument.Run;
import mizzen.components.markdown.MarkdownDocument.RunStyle;
import junit.framework.TestCase;

/** Markdown as blocks and runs: what a README uses. */
public class MarkdownParserTest extends TestCase {

    private static Block only(String markdown) {
        MarkdownDocument document = MarkdownParser.parse(markdown);
        assertEquals(markdown, 1, document.blocks.size());
        return document.blocks.get(0);
    }

    // "text/STYLE" a run: only what isn't plain says its style
    private static String shown(List<Run> runs) {
        StringBuilder s = new StringBuilder();
        for (Run run : runs) {
            s.append('<').append(run.text);
            if (run.style != RunStyle.PLAIN) {
                s.append('/').append(run.style);
            }
            if (run.target != null) {
                s.append("->").append(run.target);
            }
            s.append('>');
        }
        return s.toString();
    }

    public void testHeading() {
        Block heading = only("## Quick **start**");
        assertEquals(BlockKind.HEADING, heading.kind);
        assertEquals(2, heading.level);
        assertEquals("<Quick ><start/BOLD>", shown(heading.runs));
    }

    public void testParagraphLinesJoin() {
        assertEquals("<one two>", shown(only("one\ntwo").runs));
    }

    public void testInlineStyles() {
        assertEquals("<a ><b/BOLD>< ><c/ITALIC>< ><d/CODE>", shown(only("a **b** *c* `d`").runs));
    }

    public void testNotEmphasis() {
        assertEquals("<2 * 3 and snake_case_name>", shown(only("2 * 3 and snake_case_name").runs));
    }

    public void testLinkAndImage() {
        assertEquals("<See ><the code/LINK->A.java>", shown(only("See [the code](A.java)").runs));
        assertEquals("<[a cube]/IMAGE->c.gif>", shown(only("![a cube](c.gif)").runs));
    }

    public void testALinkAroundAnImageReadsAsItsWords() {
        assertEquals("<a cube/LINK->A.java>", shown(only("[![a cube](c.gif)](A.java)").runs));
    }

    public void testHtmlTagsGoTheirTextStays() {
        assertEquals("<small>", shown(only("<sub>small</sub>").runs));
        assertTrue(MarkdownParser.parse("<p align=\"center\">").blocks.isEmpty());
    }

    public void testList() {
        MarkdownDocument list = MarkdownParser.parse("- one\n  more\n- two\n  - deep\n1. first");
        assertEquals(4, list.blocks.size());
        assertEquals("<one more>", shown(list.blocks.get(0).runs));
        assertEquals("• ", list.blocks.get(0).marker);
        assertEquals(1, list.blocks.get(2).level);
        assertEquals("1. ", list.blocks.get(3).marker);
    }

    public void testCodeKeepsItsLinesAndMarks() {
        Block code = only("```java\nint a = 2 * 3;\n\n**not bold**\n```");
        assertEquals("java", code.language);
        assertEquals(3, code.codeLines.size());
        assertEquals("**not bold**", code.codeLines.get(2));
    }

    public void testTable() {
        Block table = only("| A | B |\n|---|---|\n| [x](y) | 2 |");
        assertEquals(BlockKind.TABLE, table.kind);
        assertEquals(2, table.tableRows.size());
        assertEquals("<x/LINK->y>", shown(table.tableRows.get(1).get(0)));
    }

    public void testRuleAndQuote() {
        assertEquals(BlockKind.RULE, only("---").kind);
        Block quote = only("> said\n> this");
        assertEquals(BlockKind.QUOTE, quote.kind);
        assertEquals("<said this>", shown(quote.runs));
    }
}
