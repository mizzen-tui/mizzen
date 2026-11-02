package mizzen.components.chart;

import mizzen.controlcodes.color.Colors;
import mizzen.core.TextSnapshot;
import junit.framework.TestCase;

/** The settings for a chart on a page: check boxes, legend, labels clear of the line, the axis's start, the y range, the x labels. */
public class LineChartReportSettingsTest extends TestCase {

    // 1.90 to 2.00, starting at the very bottom
    private static LineChart chart() {
        LineChart chart = new LineChart(Colors.Cya).series("size", Colors.BCya);
        for (int i = 1; i <= 12; i++) {
            chart.add("size", i - 1, (1900000000L + i * i * 700000L) / 1e9);
        }
        return chart;
    }

    private static String[] page(LineChart chart) {
        return new TextSnapshot(10, 100).render(chart.redraw()).split("\n");
    }

    public void testLegendCheckBoxes() {
        assertTrue("by default", String.join("\n", page(chart())).contains("[x]"));
        String names = String.join("\n", page(chart().legendCheckBoxes(false)));
        assertFalse(names, names.contains("[x]"));
        assertTrue(names, names.contains("size"));
    }

    public void testNoLegend() {
        String none = String.join("\n", page(chart().legend(false)));
        assertFalse(none, none.contains("size"));
    }

    public void testLabelsBelowHaveARowOfTheirOwn() {
        String[] lines = page(chart().xLabelsBelow(true).axisOverhang(false));   // overhang on, only the y axis would cross that row
        String labels = lines[lines.length - 1];
        assertTrue(labels, labels.contains("5"));
        assertTrue("nothing but labels: " + labels, labels.chars().noneMatch((c) -> c >= 0x2800 && c <= 0x28FF));
    }

    public void testNoLabels() {
        String[] lines = page(chart().xLabels(false));
        assertFalse(lines[8], lines[8].matches(".*\\b5\\b.*"));
    }

    public void testNoOverhang() {
        String bottom = page(chart())[9];
        assertTrue("by default it runs left of the y axis: " + bottom, bottom.charAt(0) != ' ');
        String without = page(chart().axisOverhang(false))[9];
        assertEquals("[" + without + "]", ' ', without.charAt(0));
    }

    public void testYRange() {
        String[] fromZero = page(chart().yRange(0.0, null));
        assertTrue(fromZero[9], fromZero[9].contains("0"));
        assertFalse("not from 1.90", String.join("\n", fromZero).contains("1.90"));
        String both = String.join("\n", page(chart().yRange(0.0, 4.0)));
        assertTrue(both, both.contains("4"));
    }

    public void testBadYRange() {
        try {
            chart().yRange(2.0, 1.0);
            fail("min above max");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("yRange"));
        }
    }

    public void testXLabels() {
        String text = String.join("\n", page(chart().xLabel((x) -> "W" + (int) x)));
        assertTrue(text, text.contains("W5"));
    }
}
