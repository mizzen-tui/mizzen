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

    /** data from x 0: the axes cross on the first point, and in their own row its label fits */
    public void testLabelsBelowLabelTheFirstPoint() {
        String[] below = page(chart().xLabelsBelow(true).axisOverhang(false).xLabel((x) -> "d" + (int) x));
        assertTrue(below[below.length - 1], below[below.length - 1].contains("d0"));
        String[] numbers = page(chart().xLabelsBelow(true).axisOverhang(false));
        assertTrue(numbers[numbers.length - 1], numbers[numbers.length - 1].matches(".*\\b0\\b.*"));
    }

    /** labels inside the plot: the y axis labels that corner, as before - also after turning below off again */
    public void testLabelsInsideLeaveTheCorner() {
        assertFalse(String.join("\n", page(chart().xLabel((x) -> "d" + (int) x))).contains("d0"));
        LineChart off = chart().xLabelsBelow(true);
        page(off);
        assertFalse(String.join("\n", page(off.xLabelsBelow(false).xLabel((x) -> "d" + (int) x))).contains("d0"));
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

    // the console's Visitor stats: three series, the busiest one highest, its top on the last day
    private static LineChart visits() {
        LineChart chart = new LineChart(Colors.Cya).series("sessions").series("paywall hits").series("payments");
        for (int day = 0; day < 12; day++) {
            chart.add("sessions", day, 30 + day * 3).add("paywall hits", day, 4 + day % 8).add("payments", day, day % 2);
        }
        return chart.yRange(0.0, null);
    }

    // a line in the plot: braille right of the y axis (the first thing on the row, itself braille)
    private static boolean drawnOn(String line) {
        return line.replaceFirst("^ *[\u2800-\u28FF]", "").chars().anyMatch((c) -> c >= 0x2801 && c <= 0x28FF);
    }

    public void testLegendClearLeavesTheLegendsRows() {
        String[] over = page(visits());
        assertTrue("by default the line runs under the legend:\n" + String.join("\n", over), drawnOn(over[0]));
        String[] clear = page(visits().legendClear(true));
        for (int row = 0; row < 3; row++) {
            assertFalse("row " + row + ":\n" + String.join("\n", clear), drawnOn(clear[row]));
        }
        assertTrue(clear[1], clear[1].contains("paywall hits"));
        assertTrue("the data right under it:\n" + String.join("\n", clear), drawnOn(clear[3]));
    }

    public void testLegendClearFollowsTheShownSeries() {
        String[] hidden = page(visits().legendClear(true).visible("sessions", false));
        assertTrue("hidden, its legend line stays", hidden[0].contains("sessions"));
        assertFalse(String.join("\n", hidden), drawnOn(hidden[2]));
        assertTrue("the others fill the rest:\n" + String.join("\n", hidden), drawnOn(hidden[3]));
    }

    public void testLegendClearKeepsAGivenTop() {
        assertEquals(String.join("\n", page(visits().yRange(0.0, 100.0))),
                     String.join("\n", page(visits().yRange(0.0, 100.0).legendClear(true))));
        assertEquals("no legend, nothing to clear", String.join("\n", page(visits().legend(false))),
                     String.join("\n", page(visits().legend(false).legendClear(true))));
    }

    /** a label cut at the right edge is left out: "Nov" alone would read as a date */
    public void testAnXLabelThatDoesNotFitIsLeftOut() {
        String[] lines = page(visits().xLabelsBelow(true).axisOverhang(false).xLabel((x) -> "Nov " + (3 + (int) x)));
        String labels = lines[lines.length - 1];
        assertTrue(labels, labels.contains("Nov 3"));
        assertTrue("nothing but labels: [" + labels + "]", labels.replaceAll("Nov \\d+", "").trim().replaceAll("[ │]", "").isEmpty());
        int day = 0;
        for (String label : labels.trim().split(" {2,}")) {                // cut, "Nov 14" would read "Nov 1": the days must rise
            int next = Integer.parseInt(label.substring(4));
            assertTrue("whole, in order: [" + labels + "]", next > day);
            day = next;
        }
    }
}
