package mizzen.components.table;

import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import junit.framework.TestCase;

/**
 * A table reads only what it can show: a draw, a resize or a page move reads about a screen's worth
 * of cells, however many rows the model has - so a million rows (or a database behind the model)
 * costs no more than a hundred. The first draw also reads a sample of rows, once, to make the
 * columns.
 */
public class TableReadsTest extends TestCase {

    private static final int HEIGHT = 30, WIDTH = 120, COLUMNS = 4;

    // at most this many reads of each cell on screen in one draw: measured, then drawn - with room to spare
    private static final int READS_PER_CELL = 4;

    /** rows made up as they are asked for - nothing stored - counting every cell read */
    static class CountingModel implements TableModel<Object> {
        final int rows;
        long reads = 0;

        CountingModel(int rows) {
            this.rows = rows;
        }

        public int getRowCount() {
            return rows;
        }

        public int getColumnCount() {
            return COLUMNS;
        }

        public String getColumnName(int column) {
            return new String[] { "ID", "Name", "Email", "Notes" }[column];
        }

        public Object getValueAt(int row, int column) {
            reads++;
            switch (column) {
                case 0:  return row;
                case 1:  return "Name" + (row % 977);
                case 2:  return "person" + row + "@example.com";
                default: return "note " + row + " runs on a little, so it wraps in a narrow column";
            }
        }
    }

    // the reads one draw made, after action
    private static long readsFor(CountingModel model, TableComponent<Object> table, Runnable action) {
        action.run();
        model.reads = 0;
        table.getSegmentsBuffer().getSegments();
        return model.reads;
    }

    private static void assertReadsAScreen(int rows) {
        CountingModel model = new CountingModel(rows);
        TableComponent<Object> table = new TableComponent<Object>(Colors.Yel, new Dimension.Simple(HEIGHT, WIDTH), model);
        long most = (long) HEIGHT * COLUMNS * READS_PER_CELL;
        // the first draw also makes the columns: each looks at a sample of rows to tell numbers from text
        long sample = (long) Math.min(rows, TableColumnImpl.SAMPLED_ROWS) * COLUMNS;
        assertAtMost("first draw", rows, most + sample, readsFor(model, table, () -> { }));
        assertAtMost("resize", rows, most, readsFor(model, table, () -> table.resize(new Dimension.Simple(HEIGHT, 80))));
        assertAtMost("next page", rows, most, readsFor(model, table, () -> table.setSelectedRow(40)));
        assertAtMost("last row", rows, most, readsFor(model, table, () -> table.setSelectedRow(rows - 1)));
    }

    private static void assertAtMost(String what, int rows, long most, long reads) {
        assertTrue(what + " of " + rows + " rows read " + reads + " cells, more than a screen's " + most, reads <= most);
    }

    public void testAThousandRows() {
        assertReadsAScreen(1000);
    }

    public void testAMillionRows() {
        assertReadsAScreen(1000000);
    }
}
