package mizzen.components.table;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import mizzen.controlcodes.color.Colors;
import mizzen.examples.Parts;
import mizzen.examples.Parts.Part;
import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * Every cell renderer keeps to its rectangle: drawn at any width from its cut to past its most (a
 * column widened to fill the table), at the height it says for that width, no segment falls
 * outside, drawn plain or as the selected row's - text, a drawing, an image, a meter, a sparkline. And each draws something: a renderer that
 * draws nothing would keep to any rectangle.
 */
public class CellRenderersTest extends TestCase {

    private static final int PAST_MOST = 6;

    private static void assertKeepsToItsRectangle(String what, CellRenderer renderer, Object value) {
        CellWidths widths = renderer.widths(value);
        boolean drewSomething = false;
        for (int width = Math.max(1, widths.cut); width <= widths.most + PAST_MOST; width++) {
            int height = renderer.heightAt(value, width);
            assertTrue(what + " at " + width + " wide: height " + height, height >= 1);
            IScreenLineSegmentsBuffer drawn = renderer.render(value, new Dimension.Simple(height, width), Align.LEFT);
            for (ScreenLineSegment segment : drawn.getSegments()) {
                assertTrue(what + " drawn " + height + " x " + width + " has a segment outside it: " + segment.getRow() + ","
                        + segment.getCol() + " [" + segment.getText() + "]",
                        segment.getRow() >= 0 && segment.getRow() < height && segment.getCol() >= 0
                                && segment.getCol() + segment.getText().length() <= width);
                drewSomething |= !segment.getText().trim().isEmpty();
            }
            // and as the selected row's cell - a frame drawn around a picture stays inside too
            for (ScreenLineSegment segment : renderer.renderSelected(value, new Dimension.Simple(height, width), Align.LEFT,
                    new Colors.FGBGColors(Colors.IYel).invert()).getSegments()) {
                assertTrue(what + " drawn selected " + height + " x " + width + " has a segment outside it: " + segment.getRow()
                        + "," + segment.getCol() + " [" + segment.getText() + "]",
                        segment.getRow() >= 0 && segment.getRow() < height && segment.getCol() >= 0
                                && segment.getCol() + segment.getText().length() <= width);
            }
        }
        if (value != null && !"".equals(value)) {
            assertTrue(what + " drew nothing at any width", drewSomething);
        }
    }

    // a one-line cell's text
    private static String lineOf(IScreenLineSegmentsBuffer drawn) {
        StringBuilder line = new StringBuilder();
        for (ScreenLineSegment segment : drawn.getSegments()) {
            while (line.length() < segment.getCol() + segment.getText().length()) {
                line.append(' ');
            }
            line.replace(segment.getCol(), segment.getCol() + segment.getText().length(), segment.getText());
        }
        return line.toString();
    }

    public void testText() {
        TextCellRenderer text = new TextCellRenderer();
        for (Object value : Arrays.asList("Ben is a pilot in Lyon, France.", "ben.brennan1@example.com", "", null,
                "two\nlines", "a-word-longer-than-its-cell short")) {
            assertKeepsToItsRectangle("text \"" + value + "\"", text, value);
        }
        assertKeepsToItsRectangle("a number", new TextCellRenderer().wraps(false).mayBeCut(false), 123456);
        assertKeepsToItsRectangle("cut, not wrapped", new TextCellRenderer().wraps(false), "Ben is a pilot in Lyon");
    }

    /** text's widths and height, as a table measures them */
    public void testTextMeasures() {
        TextCellRenderer text = new TextCellRenderer();
        CellWidths widths = text.widths("Ben is a pilot in Lyon");
        assertEquals("cut", 1, widths.cut);
        assertEquals("least: the longest word", 5, widths.least);
        assertEquals("most: all of it", 22, widths.most);
        assertEquals("Ben is a / pilot in / Lyon", 3, text.heightAt("Ben is a pilot in Lyon", 9));
        assertEquals(1, text.heightAt("Ben is a pilot in Lyon", 22));
        CellWidths number = new TextCellRenderer().wraps(false).mayBeCut(false).widths(123456);
        assertEquals("a number: all of it, always", "cut 6, least 6, most 6", number.toString());
    }

    public void testDrawing() {
        CellRenderer drawing = CellRenderers.drawing((Part part, mizzen.components.plot.Pen pen) -> pen.color(part.color)
                .draw(part.drawing.get()), -1.3, 1.3, -1.3, 1.3, 4, 20);
        for (Part part : Arrays.asList(Parts.GEAR_12, Parts.HEX_NUT, Parts.SPRING)) {
            assertKeepsToItsRectangle(part.name, drawing, part);
        }
        assertEquals("a square in braille, half as many rows as columns - 8 wide inside its margin: 4 rows, and 2 for the margin", 6, drawing.heightAt(Parts.GEAR_12, 10));
    }

    public void testImage() throws Exception {
        BufferedImage earth = ImageIO.read(getClass().getResourceAsStream("/mizzen/examples/sampledata/images/earth-messenger.jpg"));
        CellRenderer image = CellRenderers.image((BufferedImage picture) -> picture, 4, 24);
        assertKeepsToItsRectangle("the earth", image, earth);
    }

    public void testMeter() {
        CellRenderer meter = CellRenderers.meter(Colors.Gre, (Double fraction) -> fraction);
        for (double fraction : new double[] { 0, 0.4, 1 }) {
            assertKeepsToItsRectangle("meter at " + fraction, meter, fraction);
        }
        assertEquals("never narrower than its bar and percentage (12), and a column each side", 14, meter.widths(0.4).least);
        assertEquals("a line high, selected or not", 1, meter.heightAt(0.4, 20));
        String selected = lineOf(meter.renderSelected(0.4, new Dimension.Simple(1, 20), Align.LEFT, new Colors.FGBGColors(Colors.IYel).invert()));
        assertTrue("between solid bars: " + selected, selected.contains("▌") && selected.contains("▐"));
        String kept = lineOf(meter.renderSelected(0.4, new Dimension.Simple(1, 20), Align.LEFT, new Colors.FGBGColors(Colors.IYel)));
        assertTrue("the selection only kept, without focus - thin: " + kept, kept.startsWith("│") || kept.trim().startsWith("│"));
    }

    public void testATableInACell() {
        CellRenderer table = CellRenderers.table(Colors.Cya, (List<String> names) -> new ObjectTableModel<String>()
                .column("Name", (n) -> n).setRows(names));
        assertKeepsToItsRectangle("a small table", table, Arrays.asList("gear", "nut", "a spring"));
        assertEquals("margin 2, border 2, \"a spring\": 8", 12, table.widths(Arrays.asList("gear", "nut", "a spring")).most);
        assertEquals("margin 2; top, heading, line, 3 rows, bottom", 9, table.heightAt(Arrays.asList("gear", "nut", "a spring"), 12));
    }

    public void testSparkline() {
        CellRenderer sparkline = CellRenderers.sparkline(Colors.Gre, (List<Integer> values) -> values);
        assertKeepsToItsRectangle("sparkline", sparkline, Arrays.asList(3, 5, 2, 8, 6, 9, 4, 7, 1, 5));
    }
}
