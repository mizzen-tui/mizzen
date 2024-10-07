package dtb.terminalIO.components.table;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.util.StringUtils;

public class TableColumnWithDataAndWidths extends TableColumnImpl {

    // Map<String, Object> dataMap;
    // List<Map<String, Object>> dataArray;
    TableModel tableModel;
    List<String> columnData;
    // width stats
    Integer headerWidth = null;
    Integer maxDataLen = null;
    Integer column_size_to_use;
    boolean wordWrapOnDataNeeded = false;
    boolean truncateNeeded = false; // if true column_size_to_use is used to dictate truncate size

    TableColumnWithDataAndWidths(TableColumn base, TableModel tableModel) {
        super(base);
        this.tableModel = tableModel;

        int rowCount = getRowCount();
        columnData = new ArrayList<String>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            columnData.add(null);
        }
    }

    public void clearCalculatedData() {
        int rowCount = getRowCount();
        int currentPreferredWidth = this.getPreferredWidth();
        columnData = new ArrayList<String>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            columnData.add(null);
        }
        this.headerWidth = null;
        this.maxDataLen = null;
        this.column_size_to_use = null;
        this.wordWrapOnDataNeeded = false;
        this.truncateNeeded = false; // if true column_size_to_use is used to dictate truncate size
//        if (currentPreferredWidth != super.getPreferredWidth()) {
//            this.setPreferredWidth(currentPreferredWidth);
//        }
    }

    protected int getRowCount() {
        return this.tableModel.getRowCount();
    }

    public static TableColumnWithDataAndWidths asTableColumnWithDataAndWidths(TableColumn base, TableModel tableModel) {

        if (base instanceof TableColumnWithDataAndWidths
                && ((TableColumnWithDataAndWidths) base).tableModel == tableModel) {
            return (TableColumnWithDataAndWidths) base;
        }

        return new TableColumnWithDataAndWidths(base, tableModel);

    }

    public String getFormattedDataForRow(int row) {
        if (row < 0 || row >= getRowCount()) {
            throw new IllegalArgumentException("Trying to access row index " + row
                    + " row when there are only a total of " + getRowCount() + " row(s)");
        }

        if (columnData.size() <= row) {
            for (int i = columnData.size(); i <= row; i++) {
                columnData.add(null);
            }
        }
        String cellData = columnData.get(row);

        if (cellData != null) {
            return cellData;
        }

        String extractedCellDataAsStr = String.valueOf(tableModel.getValueAt(row, getPosition()));

        columnData.set(row, extractedCellDataAsStr);

        return extractedCellDataAsStr;
    }

    public int getMaxMinDataWidth(int row) {
        return __getMaxMinDataWidth(row, this.getPreferredWidth());
    }

    private int __getMaxMinDataWidth(int row, int desiredSized) {
        String cellData = getFormattedDataForRow(row);
        if (cellData == null) {
            return 0;
        }
        int cell_length = cellData.length();
        if (this.isWordWrappable() && cell_length > desiredSized) {
            this.wordWrapOnDataNeeded = true;
            return StringUtils.longestWord(cellData);
        }
        return cell_length;
    }

    private int getMaxWordSize() {
        int maxLen = 0;
        for (int row = 0; row < getRowCount(); row++) {
            String cellData = getFormattedDataForRow(row);
            int dLen = StringUtils.longestWord(cellData);
            maxLen = maxLen > dLen ? maxLen : dLen;
        }
        return maxLen;
    }

    public int getMaxMinDataWidth() {
        if (maxDataLen != null) {
            return maxDataLen;
        }
        int maxLen = 0;
        for (int row = 0; row < getRowCount(); row++) {
            boolean prev_wordWrapOnDataNeeded = this.wordWrapOnDataNeeded;
            int dLen = getMaxMinDataWidth(row);
            if (prev_wordWrapOnDataNeeded != this.wordWrapOnDataNeeded) {
                maxLen = dLen; // forget previous max because now we are word-wrapping
            } else {
                maxLen = maxLen > dLen ? maxLen : dLen;
            }
        }
        maxDataLen = maxLen;
        return maxDataLen;
    }

    public int getHeaderWidth() {
        if (headerWidth == null) {
            String header = getHeaderText();
            headerWidth = header == null ? 0 : StringUtils.strWidth(header);
        }
        return headerWidth;
    }

    public int getMaxMinWidthBetweenCellDataAndHeader()
    {
        int hWidth = getHeaderWidth();
        int dWidth = getMaxMinDataWidth();
        return hWidth > dWidth ? hWidth : dWidth;
    }

    public void setColumnSizeToUse(int size)
    {
        this.column_size_to_use = size;
    }

    @Override
    public Integer getPreferredWidth()
    {
        if (this.column_size_to_use != null)
        {
            return column_size_to_use;
        }
        return super.getPreferredWidth();
    }

    public int getSizeToAlignCellData()
    {
        if (wordWrapOnDataNeeded)
        {
            return this.getPreferredWidth();
        }
        int maxDataWidth = getMaxMinDataWidth();
        // getMaxMinDataWidth() has the nasty? side-effect of
        // potentially modifying wordWrapOnDataNeeded - so let's retest 
        // it's value again and act accordingly
        if (wordWrapOnDataNeeded)
        {
            return this.getPreferredWidth();
        }
        int col_size = this.getPreferredWidth();
        return maxDataWidth<col_size? maxDataWidth : col_size;
    }

    public void squeezeColSizeByOneIfPossible()
    {
        int desiredColSize = this.getPreferredWidth()-1;
        int minColSize = this.getMaxMinWidthBetweenCellDataAndHeader()+4;
        if (minColSize < desiredColSize)
           this.setColumnSizeToUse( this.getPreferredWidth() - 1);
        else {
            if (isWordWrappable() && !wordWrapOnDataNeeded)
            {
                maxDataLen = null;
                wordWrapOnDataNeeded = true;
                maxDataLen = getMaxWordSize();
                squeezeColSizeByOneIfPossible();
            }
        }
    }
    public void truncateIfPossible(int desiredCharacterCountToTruncate)
    {
        if (this.truncateNeeded) {
            return ; // trunc calculation already done.
        }
        int desiredColSize = this.getPreferredWidth()-desiredCharacterCountToTruncate;
        int hWidth = getHeaderWidth();
        int minColSize = Math.max(desiredColSize, hWidth);
        //int minColSize = desiredColSize;
        if (minColSize != this.getPreferredWidth()) {
            this.setColumnSizeToUse(minColSize);
            this.truncateNeeded = true;
        }
    }
}
