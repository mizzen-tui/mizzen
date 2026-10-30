package mizzen.components.markdown;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/** Markdown laid out to a width: nothing past it, words whole, as tall as its text. */
public class MarkdownViewTest extends TestCase {

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

    public void testWordsWrapWhole() {
        assertEquals("[one two, three]", at("one two three", 8).toString());
    }

    public void testAWordWiderThanTheLineIsBroken() {
        assertEquals("[abcde, fgh]", at("abcdefgh", 5).toString());
    }

    public void testStylesKeepTheirSpaces() {
        assertEquals("[a b c]", at("a **b** c", 20).toString());
    }

    public void testHeadingUnderlined() {
        assertEquals("[Title, ═════]", at("# Title", 20).toString());
    }

    public void testListItemsWrapUnderTheirText() {
        assertEquals("[• one two, three]", at("- one two three", 9).toString().replace("  three", "three"));
        assertTrue(at("- one two three", 9).get(1).startsWith("  three"));
    }

    public void testTable() {
        List<String> table = at("| A | B |\n|---|---|\n| one | two |", 20);
        assertEquals("A   │ B", table.get(0));
        assertEquals("────┼────", table.get(1));
        assertEquals("one │ two", table.get(2));
    }

    public void testATableNarrowerThanItsTextWraps() {
        List<String> table = at("| A | B |\n|---|---|\n| one two three four | x |", 12);
        for (String line : table) {
            assertTrue(line, line.length() <= 12);
        }
        assertTrue(table.toString(), table.size() > 3);
    }

    /** the real README, at many widths: nothing outside the view, and it is as tall as its lines */
    public void testTheReadme() throws Exception {
        String readme = new String(Files.readAllBytes(Paths.get("README.md")), "UTF-8");
        for (int width = 10; width <= 200; width += 7) {
            MarkdownView view = new MarkdownView(readme);
            view.fitTo(width);
            assertEquals(view.lines.size(), view.getHeight());
            for (ScreenLineSegment segment : view.getSegmentsBuffer().getSegments()) {
                assertTrue("at " + width + ": " + segment.getText(), segment.getCol() + segment.getText().length() <= width);
            }
        }
    }

    public void testASecondFitToANewWidth() {
        MarkdownView view = new MarkdownView("one two three");
        view.fitTo(8);
        assertEquals(2, view.getHeight());
        view.fitTo(40);
        assertEquals(1, view.getHeight());
        assertEquals("[one two three]", screen(view).toString());
    }
}
