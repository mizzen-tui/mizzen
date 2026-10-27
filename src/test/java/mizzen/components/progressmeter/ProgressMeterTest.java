package mizzen.components.progressmeter;

import java.util.TreeMap;

import mizzen.components.progressmeter.ProgressMeter.Layout;
import mizzen.components.progressmeter.ProgressMeter.Status;
import mizzen.geom.Dimension;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * ProgressMeter: two rows or one, its rows as wide as the meter, and a resize gives the bar and the
 * message the new width - in both layouts.
 */
public class ProgressMeterTest extends TestCase {

    /** how wide each row is drawn: row -> visible characters */
    private static TreeMap<Integer, Integer> rowWidths(ProgressMeter meter) {
        TreeMap<Integer, Integer> widths = new TreeMap<Integer, Integer>();
        for (ScreenLineSegment s : meter.getSegmentsBuffer().getSegments()) {
            widths.merge(s.getRow(), s.getText().length(), Integer::sum);
        }
        return widths;
    }

    /** the row's text, colours left out */
    private static String rowText(ProgressMeter meter, int row) {
        StringBuilder text = new StringBuilder();
        for (ScreenLineSegment s : meter.getSegmentsBuffer().getSegments()) {
            if (s.getRow() == row) {
                text.append(s.getText());
            }
        }
        return text.toString();
    }

    public void testTwoRowsByDefaultBothAsWideAsTheMeter() {
        ProgressMeter meter = new ProgressMeter(20);
        assertEquals(Layout.TWO_ROWS, meter.getLayout());
        assertEquals(2, meter.getHeight());
        assertEquals(26, meter.getWidth());                       // the bar and " 100% "
        assertEquals("{0=26, 1=26}", rowWidths(meter).toString());
    }

    public void testOneRowHasTheMessageFirstThenTheBar() {
        ProgressMeter meter = new ProgressMeter(20).setLayout(Layout.ONE_ROW);
        meter.setMessage("Copying");
        meter.setProgress(50, 100);
        assertEquals(1, meter.getHeight());
        assertEquals("{0=26}", rowWidths(meter).toString());
        String row = rowText(meter, 0);
        assertTrue(row, row.startsWith("Copying "));
        assertTrue(row, row.endsWith(" 50% "));
    }

    public void testTheBarIsDrawnWithCharactersSoItShowsWithoutColour() {
        ProgressMeter meter = new ProgressMeter(10);
        meter.setProgress(30, 100);
        assertTrue(rowText(meter, 0), rowText(meter, 0).startsWith("███░░░░░░░"));
    }

    public void testALongMessageLeavesASpaceBeforeTheBar() {
        ProgressMeter meter = new ProgressMeter(20).setLayout(Layout.ONE_ROW);
        meter.setMessage("a message far too long to fit in its share of the row");
        String row = rowText(meter, 0);
        assertEquals(' ', row.charAt(row.indexOf('░') - 1));
    }

    public void testResizeGivesTheNewWidthInBothLayouts() {
        for (Layout layout : Layout.values()) {
            ProgressMeter meter = new ProgressMeter(20).setLayout(layout);
            meter.setProgress(50, 100);
            meter.setMessage("Backing up database");
            meter.resize(new Dimension.Simple(5, 60));             // the height is the layout's own
            assertEquals(layout + " width", 60, meter.getWidth());
            assertEquals(layout + " height", layout == Layout.TWO_ROWS ? 2 : 1, meter.getHeight());
            for (int width : rowWidths(meter).values()) {
                assertEquals(layout + " every row as wide as the meter", 60, width);
            }
            meter.resize(new Dimension.Simple(2, 30));             // and narrower again
            for (int width : rowWidths(meter).values()) {
                assertEquals(layout + " narrower", 30, width);
            }
        }
    }

    public void testSwitchingLayoutLiveKeepsWidthProgressAndMessage() {
        ProgressMeter meter = new ProgressMeter(20);
        meter.setProgress(50, 100);
        meter.setMessage("Copying");
        meter.resize(new Dimension.Simple(2, 40));
        meter.setLayout(Layout.ONE_ROW);
        assertEquals(1, meter.getHeight());
        assertEquals("{0=40}", rowWidths(meter).toString());
        assertTrue(rowText(meter, 0), rowText(meter, 0).startsWith("Copying "));
        assertTrue(rowText(meter, 0), rowText(meter, 0).endsWith(" 50% "));
        meter.setLayout(Layout.TWO_ROWS);
        assertEquals(2, meter.getHeight());
        assertEquals("{0=40, 1=40}", rowWidths(meter).toString());
        assertTrue(rowText(meter, 1), rowText(meter, 1).startsWith("Copying"));
    }

    public void testStatusShowsBeforeTheMessage() {
        ProgressMeter meter = new ProgressMeter(20);
        meter.setMessage("done");
        meter.setStatus(Status.CANCELLED);
        assertTrue(rowText(meter, 1), rowText(meter, 1).startsWith("CANCELLED - done"));
    }
}
