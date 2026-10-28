package mizzen.components.table;

import java.util.ArrayList;
import java.util.List;

/** rows as Object[] - for columns only known at run time (a query result, a file); getRow gives a row's array */
public class DefaultTableModel implements TableModel<Object[]> {

    List<String> columnNames;
    List<Object[]> data;

    public DefaultTableModel() {
        this.columnNames = new ArrayList<String>();
        this.data = new ArrayList<Object[]>();
    }
    
    public DefaultTableModel(List<String> columnNames) {
        this.columnNames = columnNames;
        this.data = new ArrayList<Object[]>();
    }

    public DefaultTableModel(List<String> columnNames, List<Object[]> data) {
        this.columnNames = columnNames;
        this.data = data;
        for (int i=0; i < data.size(); i++) {
            Object[] row = data.get(i);
            if (row == null) {
                throw new NullPointerException("row was null");
            }
            if (row.length != this.columnNames.size()) {
                throw new IllegalArgumentException("row at index "+i+" (with length of "+row.length+") was not the same length as columnnames (with length of "+row.length+")");
            }
        }
    }

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
        this.data = new ArrayList<Object[]>();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return this.columnNames.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return this.columnNames.get(columnIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return this.data.get(rowIndex)[columnIndex];
    }

    @Override
    public Object[] getRow(int rowIndex) {
        return this.data.get(rowIndex);
    }
    
    private void appendRow() {
        this.data.add( new Object[getColumnCount()] );
    }
    
    public void setValueAt(int rowIndex, int columnIndex, Object value) {
        if (rowIndex == this.data.size()) {
            appendRow();
        }
        this.data.get(rowIndex)[columnIndex] = value;
    }

    
    

}
