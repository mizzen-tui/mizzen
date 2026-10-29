package mizzen.components.table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegmentsBuffer;

/**
 * Everything a table works out before it draws - so the table itself only draws what this lays out
 * (layOut, TableLayout).
 * <p/>
 * It looks at a window of rows, never the whole model: from the first row shown, as many rows as the
 * table has lines (each row is at least a line, so no more can show). A million rows cost what a
 * screenful does. In that window it reads each cell once, asks the column's renderer how wide the
 * cell can be (CellWidths), fits the columns (ColumnFitting), asks each cell how tall it is at its
 * column's width, and places the rows. Cells are drawn when the table asks, each kept until its size
 * changes or it leaves the window.
 * <pre>
 *     a 30-line table on 1,000,000 rows, top row 500:  rows 500 to 529 read and measured, nothing else
 * </pre>
 * Kept between draws, until clearData (a resize, a new model, new lines):
 * <pre>
 *     the columns dropped     scrolling never brings one back to drop it again
 *     the widest seen         only when widths only grow: scrolling never narrows a column
 * </pre>
 */
class TableModelWidthData extends TableModel.Delegate<Object> {

    private static final Object NOT_READ = new Object();

    /** a cell drawn, at the size it was drawn */
    private static final class Drawn {
        final int width;
        final int height;
        final IScreenLineSegmentsBuffer segments;

        Drawn(int width, int height, IScreenLineSegmentsBuffer segments) {
            this.width = width;
            this.height = height;
            this.segments = segments;
        }
    }

    private final List<TableColumn> tableColumns = new ArrayList<TableColumn>();
    private final Map<Integer, Object[]> values = new HashMap<Integer, Object[]>();       // by row: the window's cells, read once
    private final Map<Long, Drawn> cells = new HashMap<Long, Drawn>();
    private final Map<Integer, Drawn> headers = new HashMap<Integer, Drawn>();
    private final Set<Integer> dropped = new LinkedHashSet<Integer>();
    private final Map<Integer, CellWidths> widest = new HashMap<Integer, CellWidths>();
    boolean widthsOnlyGrow = false;

    @SuppressWarnings("unchecked")
    TableModelWidthData(TableModel<?> delegate) {
        super((TableModel<Object>) delegate);           // only its cells are read here, as Objects
    }

    /** forget everything worked out: the next draw starts afresh - after a resize, or when the rows changed */
    void clearData() {
        values.clear();
        cells.clear();
        headers.clear();
        dropped.clear();
        widest.clear();
    }

    // ---- the columns: as set, or made from the model (text, wrapped and cut as their data suggests)

    void addColumn(TableColumn tableColumn) {
        tableColumns.add(tableColumn);
    }

    void setColumnAt(int colIdx, TableColumn tableColumn) {
        if (colIdx < 0 || colIdx >= getColumnCount()) {
            throw new IllegalArgumentException("Error in setColumnAt().  colIdx is not in TableModel column range: " + colIdx);
        }
        getTableColumnAt(colIdx);                                   // the ones before it made
        tableColumns.set(colIdx, tableColumn);
    }

    TableColumn getTableColumnAt(int colIdx) {
        while (tableColumns.size() <= colIdx) {
            tableColumns.add(TableColumnImpl.makeDefaultColumnFromTable(tableColumns.size(), this));
        }
        return tableColumns.get(colIdx);
    }

    // the model column a column shows
    private int modelColumn(int c) {
        Integer position = getTableColumnAt(c).getPosition();
        return position == null ? c : position;
    }

    // ---- cells: read once while in the window, drawn once per size

    Object valueAt(int row, int c) {
        Object[] rowValues = values.computeIfAbsent(row, (r) -> {
            Object[] unread = new Object[getColumnCount()];
            java.util.Arrays.fill(unread, NOT_READ);
            return unread;
        });
        if (rowValues[c] == NOT_READ) {
            rowValues[c] = getValueAt(row, modelColumn(c));
        }
        return rowValues[c];
    }

    String headerOf(int c) {
        return getColumnName(modelColumn(c));
    }

    /** column c's cell in row, drawn width x height */
    IScreenLineSegmentsBuffer cell(int row, int c, int width, int height) {
        long key = ((long) row << 16) | c;
        Drawn drawn = cells.get(key);
        if (drawn == null || drawn.width != width || drawn.height != height) {
            TableColumn column = getTableColumnAt(c);
            drawn = new Drawn(width, height, copy(column.getRenderer().render(valueAt(row, c), new Dimension.Simple(height, width), column.getAlign())));
            cells.put(key, drawn);
        }
        return drawn.segments;
    }

    /** column c's header, drawn width x height */
    IScreenLineSegmentsBuffer header(int c, int width, int height) {
        Drawn drawn = headers.get(c);
        if (drawn == null || drawn.width != width || drawn.height != height) {
            drawn = new Drawn(width, height, copy(getTableColumnAt(c).getHeaderRenderer().render(headerOf(c), new Dimension.Simple(height, width), Align.CENTER)));
            headers.put(c, drawn);
        }
        return drawn.segments;
    }

    // a renderer may draw each cell into the same buffer: the table keeps its own copy
    private static IScreenLineSegmentsBuffer copy(IScreenLineSegmentsBuffer drawn) {
        IScreenLineSegmentsBuffer copy = new ScreenLineSegmentsBuffer();
        copy.clearAndMerge(ScreenLineSegment.UTIL.makeCollection(drawn.getSegments()));
        return copy;
    }

    // ---- the layout

    /**
     * What a table width x height shows from topRow: the columns fitted to its width, the rows that
     * fit under the header - moved down, if need be, so selectedRow shows whole. rowGap: lines
     * between rows; bottomBorder: the last line is the border's.
     */
    TableLayout layOut(int width, int height, int topRow, int selectedRow, int rowGap, boolean bottomBorder) {
        int rowCount = getRowCount();
        int bottom = bottomBorder ? height - 1 : height;
        int top = Math.max(0, Math.min(topRow, rowCount - 1));
        TableLayout layout = layOutFrom(top, width, height, rowGap, bottom);
        if (selectedRow >= top && selectedRow < rowCount && !layout.showsWhole(selectedRow)) {
            int showing = topToShow(selectedRow, layout, rowGap, bottom);
            if (showing != top) {
                layout = layOutFrom(showing, width, height, rowGap, bottom);
            }
        }
        forgetOutside(layout.topRow, layout.topRow + height);
        return layout;
    }

    private TableLayout layOutFrom(int top, int width, int height, int rowGap, int bottom) {
        int last = Math.min(getRowCount(), top + Math.max(0, height));     // a row is a line or more: no more than this can show
        List<ColumnFitting.Column> toFit = new ArrayList<ColumnFitting.Column>();
        for (int c = 0; c < getColumnCount(); c++) {
            toFit.add(new ColumnFitting.Column(c, widthsOf(c, top, last), getTableColumnAt(c).getColumnDroppable()));
        }
        int room = width - 2;                                               // inside the border
        ColumnFitting.Fit fit = ColumnFitting.fit(toFit, room, dropped);
        dropped.addAll(fit.dropped);                                        // held until clearData

        // the columns that start inside the border; the last of them cut at it
        List<Integer> columns = new ArrayList<Integer>();
        List<Integer> shownWidths = new ArrayList<Integer>();
        int start = 0;
        for (int i = 0; i < fit.shown.size() && start < room; i++) {
            columns.add(fit.shown.get(i));
            shownWidths.add(Math.min(fit.widths[i], room - start));
            start += fit.widths[i] + 1;
        }
        int[] fitted = new int[columns.size()];
        int[] shown = new int[columns.size()];
        for (int i = 0; i < fitted.length; i++) {
            fitted[i] = fit.widths[i];
            shown[i] = shownWidths.get(i);
        }

        int headerHeight = 0;
        for (int i = 0; i < fitted.length; i++) {
            int c = columns.get(i);
            headerHeight = Math.max(headerHeight, capped(c, getTableColumnAt(c).getHeaderRenderer().heightAt(headerOf(c), fitted[i])));
        }
        TableLayout layout = new TableLayout(columns, fitted, shown, headerHeight, top);

        int tallest = Math.max(1, bottom - layout.dataTop());                // a row taller than the room is cut to it
        int line = layout.dataTop();
        boolean whole = true;
        for (int row = top; row < getRowCount() && line < bottom; row++) {
            int rowHeight = Math.min(tallest, heightOf(row, layout));
            layout.rows.add(new TableLayout.Row(row, line, rowHeight));
            whole = whole && line + rowHeight <= bottom;
            if (whole) {
                layout.rowsWhole++;
            }
            line += rowHeight + rowGap;
        }
        return layout;
    }

    // how wide column c can be in rows from to last: its cells' widths, never narrower than its header's least
    private CellWidths widthsOf(int c, int from, int last) {
        TableColumn column = getTableColumnAt(c);
        int header = column.getHeaderRenderer().widths(headerOf(c)).least;
        int cut = header, least = header, most = header;
        CellRenderer renderer = column.getRenderer();
        for (int row = from; row < last; row++) {
            CellWidths cell = renderer.widths(valueAt(row, c));
            cut = Math.max(cut, cell.cut);
            least = Math.max(least, cell.least);
            most = Math.max(most, cell.most);
        }
        Integer given = column.getPreferredWidth();                          // a width set for the column: all it gets
        if (given != null && given > 0) {
            most = given;
            least = Math.min(least, given);
            cut = Math.min(cut, given);
        }
        if (widthsOnlyGrow) {
            CellWidths before = widest.get(c);
            if (before != null) {
                cut = Math.max(cut, before.cut);
                least = Math.max(least, before.least);
                most = Math.max(most, before.most);
            }
        }
        CellWidths widths = new CellWidths(cut, least, most);
        widest.put(c, widths);
        return widths;
    }

    // a row's height: its tallest cell at its column's width
    private int heightOf(int row, TableLayout layout) {
        int height = 1;
        for (int i = 0; i < layout.columnCount(); i++) {
            int c = layout.columns.get(i);
            height = Math.max(height, capped(c, getTableColumnAt(c).getRenderer().heightAt(valueAt(row, c), layout.fitted[i])));
        }
        return height;
    }

    // a height no more than the column allows (its preferred height, if it has one)
    private int capped(int c, int height) {
        Integer most = getTableColumnAt(c).getPreferredHeight();
        return most != null && most > 0 ? Math.min(most, height) : height;
    }

    // the first row from which selected shows whole: rows above it while they fit
    private int topToShow(int selected, TableLayout layout, int rowGap, int bottom) {
        int room = bottom - layout.dataTop();
        int used = Math.min(room, heightOf(selected, layout));
        int top = selected;
        for (int row = selected - 1; row >= 0; row--) {
            int more = rowGap + heightOf(row, layout);
            if (used + more > room) {
                break;
            }
            used += more;
            top = row;
        }
        return top;
    }

    // rows out of the window: read and drawn again if they come back
    private void forgetOutside(int from, int to) {
        values.keySet().removeIf((row) -> row < from || row >= to);
        cells.keySet().removeIf((key) -> (key >> 16) < from || (key >> 16) >= to);
    }
}
