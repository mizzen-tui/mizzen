package mizzen.components.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import mizzen.components.TextComponent;
import mizzen.components.theme.Theme;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * Markdown for reports: tables keep their ---: alignment and cut a word too wide for its column with … ; a table with
 * empty headings has no heading row; GitHub alerts in theme colours; a registered fence drawn by the app's component;
 * setMarkdown keeps the scroll position.
 */
public class MarkdownReportsTest extends TestCase {

    private static List<String> screen(MarkdownView view) {
        TreeMap<Integer, StringBuilder> byRow = new TreeMap<Integer, StringBuilder>();
        for (ScreenLineSegment segment : view.getSegmentsBuffer().getSegments()) {
            StringBuilder line = byRow.computeIfAbsent(segment.getRow(), (r) -> new StringBuilder());
            while (line.length() < segment.getCol() + segment.getText().length()) {
                line.append(' ');
            }
            line.replace(segment.getCol(), segment.getCol() + segment.getText().length(), segment.getText());
        }
        List<String> lines = new ArrayList<String>();
        for (StringBuilder line : byRow.values()) {
            lines.add(line.toString().replaceAll("\\s+$", ""));
        }
        return lines;
    }

    private static List<String> at(String markdown, int width) {
        MarkdownView view = new MarkdownView(markdown);
        view.fitTo(width);
        return screen(view);
    }

    public void testRightAlignedColumn() {
        List<String> table = at("| Name | Size |\n|---|---:|\n| a | 7 |\n| b | 1904 |", 30);
        assertEquals(" a    │    7", table.get(2));
        assertEquals(" b    │ 1904", table.get(3));
    }

    public void testCentredColumn() {
        List<String> table = at("| N | Mid |\n|---|:---:|\n| a | x |", 30);
        assertEquals(" a │  x", table.get(2));
    }

    public void testAWordTooWideIsCutNotBroken() {
        List<String> table = at("| Backup | Age |\n|---|---|\n| wp_backup_20261101_201702.tar.gz.gpg | 9 h |", 24);
        String all = table.toString();
        assertTrue(all, all.contains("…"));
        assertFalse("not continued on the next line: " + all, all.contains(".gpg"));
    }

    public void testNoHeadingRowWhenEveryHeadingIsEmpty() {
        List<String> table = at("| | |\n|---|---|\n| Site | HTTP 200 |\n| Disk | 41% |", 30);
        assertEquals(" Site │ HTTP 200", table.get(0));
        assertEquals(" Disk │ 41%", table.get(1));
    }

    public void testAlertKindAndColour() {
        MarkdownView view = new MarkdownView("> [!CAUTION]\n> site answered HTTP 500");
        view.setTheme(Theme.DARK);
        view.fitTo(40);
        assertEquals("[│ Caution, │ site answered HTTP 500]", screen(view).toString());
        boolean red = false;
        for (ScreenLineSegment segment : view.getSegmentsBuffer().getSegments()) {
            red |= segment.getText().contains("Caution") && segment.getColors() == Theme.DARK.errorColor();
        }
        assertTrue("its kind in the theme's error colour", red);
    }

    public void testARegisteredFenceIsTheAppsComponent() {
        MarkdownView view = new MarkdownView("```chart\nsize 1.90 1.91\n```\n\n```other\nkept as text\n```");
        view.embed("chart", (block, width) -> new TextComponent("CHART of " + block.codeLines.get(0)));
        view.fitTo(40);
        List<String> lines = screen(view);
        assertEquals("CHART of size 1.90 1.91", lines.get(0));
        assertTrue(lines.toString(), lines.toString().contains("kept as text"));
    }

    public void testSetMarkdownKeepsTheScrollPosition() {
        StringBuilder long1 = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            long1.append("line ").append(i).append("\n\n");
        }
        MarkdownView view = new MarkdownView(long1.toString());
        ScrollableComponent pane = MarkdownView.inScrollPane(null, view);
        pane.resize(new mizzen.geom.Dimension.Simple(10, 30));
        pane.getSegmentsBuffer();
        pane.scrollToView(new mizzen.geom.Rectangle.Simple(40, 0, 1, 1));
        int before = pane.getViewPortAsRectangle().getRow();
        assertTrue("scrolled down first: " + before, before > 20);
        view.setMarkdown(long1.toString().replace("line", "row"));
        pane.getSegmentsBuffer();
        assertEquals("the same place in the new text", before, pane.getViewPortAsRectangle().getRow());
    }
}
