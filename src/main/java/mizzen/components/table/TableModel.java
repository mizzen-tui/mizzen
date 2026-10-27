package mizzen.components.table;

public interface TableModel {

    public int getRowCount();
    public int getColumnCount();
    public String getColumnName(int columnIndex);
    public Object getValueAt(int rowIndex, int columnIndex);


    public static class Delegate implements TableModel {

        TableModel delegate;

        public Delegate(TableModel delegate) {
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

    }
}
