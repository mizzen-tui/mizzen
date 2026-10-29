package mizzen.components.table;

import java.util.ArrayList;
import java.util.List;

/**
 * What one draw of a table shows, worked out before anything is drawn: the columns (which, how
 * wide), the header's height, and the rows (which, where, how tall). TableModelWidthData.layOut
 * makes it; the table draws from it and nothing else.
 * <p/>
 * Positions are on the table: column 0 and row 0 are its border (or blank); the header starts at
 * row 1, column 1, and one column (a line, or blank) parts each column from the next.
 */
final class TableLayout {

    /** a row shown: which (in the model), its first line on the table, how many lines it has */
    static final class Row {
        final int row;
        final int top;
        final int height;

        Row(int row, int top, int height) {
            this.row = row;
            this.top = top;
            this.height = height;
        }
    }

    /** the columns shown, left to right: their index among the table's columns */
    final List<Integer> columns;
    /** each one's width as fitted - its cells are drawn this wide */
    final int[] fitted;
    /** each one's width on the table: the fitted width, less what the border cuts off the last */
    final int[] shown;
    final int headerHeight;
    final List<Row> rows = new ArrayList<Row>();
    /** the first row shown - moved down, if need be, so the selected row shows whole */
    final int topRow;
    /** rows shown whole: the last can be cut off at the bottom */
    int rowsWhole = 0;

    TableLayout(List<Integer> columns, int[] fitted, int[] shown, int headerHeight, int topRow) {
        this.columns = columns;
        this.fitted = fitted;
        this.shown = shown;
        this.headerHeight = headerHeight;
        this.topRow = topRow;
    }

    int columnCount() {
        return columns.size();
    }

    /** the width column c takes on the table */
    int widthOf(int c) {
        return shown[c];
    }

    /** the widths of columns 0 to c, without what parts them */
    int widthUpTo(int c) {
        int sum = 0;
        for (int i = 0; i <= c && i < shown.length; i++) {
            sum += shown[i];
        }
        return sum;
    }

    /** where column c starts on the table */
    int columnStart(int c) {
        return 1 + widthUpTo(c - 1) + c;
    }

    /** the first line of the rows, under the header and its line */
    int dataTop() {
        return 1 + headerHeight + 1;
    }

    boolean showsWhole(int row) {
        for (int i = 0; i < rowsWhole; i++) {
            if (rows.get(i).row == row) {
                return true;
            }
        }
        return false;
    }
}
