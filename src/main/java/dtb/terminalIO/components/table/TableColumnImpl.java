package dtb.terminalIO.components.table;

import dtb.terminalIO.geom.Align;
import dtb.terminalIO.util.StringUtils;

public class TableColumnImpl implements TableColumn {

    protected String              headerText          = null;
    protected Integer             preferredWidth      = null;
    protected Integer             preferredHeight     = -1;
    protected String              columnName          = null;
    protected Integer             position            = null;
    protected Align               align               = DEFAULT_ALIGN;
    protected Integer             columnDroppable     = null;
    protected Boolean             wordWrap            = null;
    protected Boolean             truncationAllowed   = null;
    private TableColumnRenderer   renderer;
    private TableColumnRenderer   headerRenderer;

    public TableColumnImpl(String headerText, Integer preferredWidth, String columnName, Integer position,
                           Align align, Integer columnDroppable, Boolean wordWrap, Boolean truncationAllowed) {
        super();
        this.headerText = headerText;
        this.preferredWidth = preferredWidth;
        this.columnName = columnName;
        this.position = position;
        this.align = align;
        this.columnDroppable = columnDroppable;
        this.wordWrap = wordWrap;
        this.truncationAllowed = truncationAllowed;
    }

    public TableColumnImpl(TableColumn base)
    {
        this(base.getHeaderText(), 
             base.getPreferredWidth(), 
             base.getColumnName(), 
             base.getPosition(), 
             base.getAlign(), 
             base.getColumnDroppable(), 
             base.isWordWrappable(), 
             base.isTruncationAllowed());
        this.setColumnRenderer( base.getRenderer() );
        this.setPreferredHeight( base.getPreferredHeight() );
    }

    public String getHeaderText() {
        return headerText;
    }
    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }
    public Integer getPreferredWidth() {
        return preferredWidth;
    }
    public void setPreferredWidth(Integer preferredWidth) {
        this.preferredWidth = preferredWidth;
    }
    public String getColumnName() {
        return columnName;
    }
    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
    public Integer getPosition() {
        return position;
    }
    public void setPosition(Integer position) {
        this.position = position;
    }
    public Align getAlign() {
        return align;
    }
    public void setAlign(Align align) {
        this.align = align;
    }
    public Integer getColumnDroppable() {
        return columnDroppable;
    }
    public void setColumnDroppable(Integer columnDroppable) {
        this.columnDroppable = columnDroppable;
    }
    public Boolean getWordWrap() {
        return wordWrap;
    }
    public void setWordWrap(Boolean wordWrap) {
        this.wordWrap = wordWrap;
    }
    public Boolean getTruncationAllowed() {
        return truncationAllowed;
    }
    public void setTruncationAllowed(Boolean truncationAllowed) {
        this.truncationAllowed = truncationAllowed;
    }

    public Boolean isColumnDroppable() {
        return getColumnDroppable() != null;
    }
    @Override
    public Boolean isWordWrappable() {
        return Boolean.TRUE.equals(getWordWrap());
    }
    @Override
    public Boolean isTruncationAllowed() {
        return Boolean.TRUE.equals(getTruncationAllowed());
    }

    @Override
    public void setColumnRenderer(TableColumnRenderer renderer) {
        this.renderer = renderer;
        if (this.headerRenderer == null) {
            // default to normal renderer
            this.headerRenderer = renderer;
        }
    }

    @Override
    public TableColumnRenderer getRenderer() {
        if (this.renderer == null) {
            return TableColumnRenderer.DEFAULT;
        }
        return renderer;
    }

    public void setHeaderRenderer(TableColumnRenderer renderer) {
        this.headerRenderer = renderer;
    }
    @Override
    public TableColumnRenderer getHeaderRenderer() {
        if (this.headerRenderer == null) {
            return getRenderer();
        }
        return headerRenderer;
    }

    public Integer getPreferredHeight() {
        return preferredHeight;
    }

    public void setPreferredHeight(Integer preferredHeight) {
        this.preferredHeight = preferredHeight;
    }

    public static TableColumn makeDefaultColumnFromTable(int colIdx, TableModel tableModel) {
        return makeDefaultColumnFromTable(colIdx, tableModel, TableColumnRenderer.DEFAULT);
    }

    // TableColumnRenderer    renderer = TableColumnRenderer.DEFAULT;
    public static TableColumn makeDefaultColumnFromTable(int colIdx, TableModel tableModel, TableColumnRenderer renderer) {
        if (colIdx >= tableModel.getColumnCount()) {
            throw new IllegalStateException("Error in call to makeColumnFromTable()! Number of columns in the tablemodel ("+tableModel.getColumnCount()+") does not match request column index ("+colIdx+")");
        }
        String headerText = tableModel.getColumnName(colIdx);
        if (headerText == null) {
            throw new IllegalStateException("tableModel.getColumnName("+colIdx+") returns null!");
        }
        int maxWidth = -1; //headerText.length();
        Boolean columnIsNumber = null;
        for (int row=0; row<tableModel.getRowCount(); row++) {
             Object value = tableModel.getValueAt(row, colIdx);
             if (value != null) {
                 String valueAsString = String.valueOf(value);
                 maxWidth = Math.max(maxWidth, valueAsString.trim().length());
                 if (columnIsNumber == null || columnIsNumber == true) {
                     columnIsNumber = StringUtils.isNumber(valueAsString);
                 }
             }
        }
        if (maxWidth <  headerText.length()) {
            maxWidth =  headerText.length();
        }
        Align align = (columnIsNumber != null && columnIsNumber) ? Align.RIGHT : Align.LEFT;
        Integer columnDroppable = null;
        Boolean             wordWrap            = (align == Align.LEFT);
        Boolean             truncationAllowed   = columnIsNumber == null || columnIsNumber == false;

        TableColumnImpl tc = new TableColumnImpl(headerText, 
                                   maxWidth, 
                                   tableModel.getColumnName(colIdx), 
                                   colIdx, 
                                   align, 
                                   columnDroppable, 
                                   wordWrap, 
                                   truncationAllowed);
        
        tc.setColumnRenderer(renderer);
 
        return tc; 
    }


}
