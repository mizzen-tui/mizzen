package dtb.terminalIO.components.table;

import java.util.ArrayList;
import java.util.List;

public class DefaultTableModel implements TableModel {

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
        for (Object[] row : this.data) {
            if (row == null) {
                throw new NullPointerException("row was null");
            }
            if (row.length != this.columnNames.size()) {
                throw new IllegalArgumentException("row was not the same length as columnnames");
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
