package mizzen.components.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * A table of objects: each row is one object, each column a name and how to get its value from the
 * object - no converting the objects to Object[] rows by hand.
 * <pre>
 *     ObjectTableModel&lt;Device&gt; model = new ObjectTableModel&lt;Device&gt;()
 *             .column("Name",   Device::getName)
 *             .column("Status", Device::getStatus)
 *             .setRows(devices);
 *     TableComponent&lt;Device&gt; table = new TableComponent&lt;Device&gt;(color, model);
 *     table.onPick((device, te) -&gt; show(device));
 * </pre>
 * getRow(index) gives back the object behind a row; the table's onPick hands you the picked one.
 * <p/>
 * After setRows on a model a TableComponent already shows, call the table's clearData() so it
 * measures the new values.
 */
public class ObjectTableModel<T> implements TableModel<T> {

    private final List<String> columnNames = new ArrayList<String>();
    private final List<Function<? super T, ?>> columnValues = new ArrayList<Function<? super T, ?>>();
    private List<T> rows = Collections.emptyList();

    /** a column: its heading, and its value for a row's object (e.g. Device::getName) */
    public ObjectTableModel<T> column(String name, Function<? super T, ?> value) {
        columnNames.add(name);
        columnValues.add(value);
        return this;
    }

    /** the objects to show, one per row, in this order (the list is used as is, not copied) */
    public ObjectTableModel<T> setRows(List<T> rows) {
        this.rows = rows == null ? Collections.<T>emptyList() : rows;
        return this;
    }

    public List<T> getRows() {
        return rows;
    }

    /** the object shown in row rowIndex */
    @Override
    public T getRow(int rowIndex) {
        return rows.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames.get(columnIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return columnValues.get(columnIndex).apply(rows.get(rowIndex));
    }
}
