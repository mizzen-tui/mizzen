package mizzen.components.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mizzen.components.table.TableComponent.TableLinesStyle;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * Rows have a line under each: blank by default (easier to read), drawn in the ALL style;
 * setSpacedRows(false) packs them one under the other, unless lines are drawn between them.
 */
public class TableRowSpacingTest extends TestCase {

    private TableComponent<Object[]> table(TableLinesStyle style, boolean spaced) {
        List<Object[]> rows = new ArrayList<Object[]>();
        for (int i = 1; i <= 6; i++) {
            rows.add(new Object[] { i, "name" + i });
        }
        TableComponent<Object[]> table = new TableComponent<Object[]>(Colors.Yel, new Dimension.Simple(12, 30), new DefaultTableModel(Arrays.asList("ID", "Name"), rows));
        table.setLinesStyle(style);
        table.setSpacedRows(spaced);
        return table;
    }

    /** the line a cell's text is on; -1 if not shown */
    private int lineOf(TableComponent<Object[]> table, String text) {
        for (ScreenLineSegment segment : table.getSegmentsBuffer().getSegments()) {
            if (segment.getText().trim().equals(text)) {
                return segment.getRow();
            }
        }
        return -1;
    }

    public void testRowsAreSpacedByDefault() {
        TableComponent<Object[]> table = new TableComponent<Object[]>(Colors.Yel, new Dimension.Simple(12, 30),
                new DefaultTableModel(Arrays.asList("ID", "Name"), Arrays.asList(new Object[] { 1, "name1" }, new Object[] { 2, "name2" })));
        table.setLinesStyle(TableLinesStyle.HEADER_ALL_AND_BORDER);
        assertTrue(table.isSpacedRows());
        assertEquals(lineOf(table, "name1") + 2, lineOf(table, "name2"));
    }

    public void testPackedRowsAreOnConsecutiveLines() {
        for (TableLinesStyle style : new TableLinesStyle[] { TableLinesStyle.NONE, TableLinesStyle.HEADER_FULL, TableLinesStyle.HEADER_ALL_AND_BORDER }) {
            TableComponent<Object[]> table = table(style, false);
            int first = lineOf(table, "name1");
            assertTrue(style + ": shown", first > 0);
            assertEquals(style + ": the next row on the next line", first + 1, lineOf(table, "name2"));
            assertEquals(style + ": all six fit", first + 5, lineOf(table, "name6"));
        }
    }

    public void testWithRowLinesThereIsALineBetweenRowsEvenPacked() {
        TableComponent<Object[]> table = table(TableLinesStyle.ALL, false);
        assertEquals(lineOf(table, "name1") + 2, lineOf(table, "name2"));
    }
}
