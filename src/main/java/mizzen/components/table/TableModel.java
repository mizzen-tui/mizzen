package mizzen.components.table;

/**
 * What a TableComponent shows: rows of type T, each a number of columns. T is what a row is - your
 * own objects (ObjectTableModel&lt;Sale&gt;), an Object[] (DefaultTableModel: columns only known at run
 * time), a Map, ...
 * <p/>
 * getRow gives the row itself: what a table hands to onPick. A model that can't give it (one written
 * before it existed) still shows - picking a row as an object then fails at once.
 */
public interface TableModel<T> {

    public int getRowCount();
    public int getColumnCount();
    public String getColumnName(int columnIndex);
    public Object getValueAt(int rowIndex, int columnIndex);

    /**
     * When the table is too narrow: whether column columnIndex may be dropped, and in what order - lower
     * first (then right to left); null: never dropped (the default). Wrapping and truncating are tried first.
     */
    public default Integer getColumnDroppable(int columnIndex) {
        return null;
    }

    /** the row itself, e.g. the Sale behind row 3 */
    public default T getRow(int rowIndex) {
        throw new UnsupportedOperationException(getClass().getName()
                + " gives no row objects: implement getRow(rowIndex) to pick its rows as objects (onPick, pick, getPicked)");
    }

    public static class Delegate<T> implements TableModel<T> {

        TableModel<T> delegate;

        public Delegate(TableModel<T> delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public int getRowCount() {
            return delegate.getRowCount();
        }

        @Override
        public int getColumnCount() {
            return delegate.getColumnCount();
        }

        @Override
        public String getColumnName(int columnIndex) {
            return delegate.getColumnName(columnIndex);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return delegate.getValueAt(rowIndex,columnIndex);
        }

        @Override
        public T getRow(int rowIndex) {
            return delegate.getRow(rowIndex);
        }

        @Override
        public Integer getColumnDroppable(int columnIndex) {
            return delegate.getColumnDroppable(columnIndex);
        }
    }
}
