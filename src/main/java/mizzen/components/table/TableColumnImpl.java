package mizzen.components.table;

import mizzen.geom.Align;
import mizzen.util.StringUtils;

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
    private CellRenderer          renderer;
    private CellRenderer          headerRenderer;

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
    public void setColumnRenderer(CellRenderer renderer) {
        this.renderer = renderer;
        if (this.headerRenderer == null) {
            // default to normal renderer
            this.headerRenderer = renderer;
        }
    }

    @Override
    /** none set: text, wrapped and cut as this column says (TextCellRenderer.forColumn) */
    public CellRenderer getRenderer() {
        if (this.renderer == null) {
            this.renderer = TextCellRenderer.forColumn(this);
        }
        return renderer;
    }

    public void setHeaderRenderer(CellRenderer renderer) {
        this.headerRenderer = renderer;
    }
    @Override
    public CellRenderer getHeaderRenderer() {
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
        return makeDefaultColumnFromTable(colIdx, tableModel, null);
    }

    /** rows looked at to tell a column of numbers from one of text - never the whole model, which may be huge */
    static final int SAMPLED_ROWS = 100;

    /**
     * A column for the model's column colIdx: numbers (all of the first SAMPLED_ROWS rows) right
     * aligned, never wrapped or cut; text left aligned, wrapped, may be cut. No width: the table
     * measures the rows it shows. renderer: how its cells are drawn; null: text, as the column says
     * (getRenderer).
     */
    public static TableColumn makeDefaultColumnFromTable(int colIdx, TableModel tableModel, CellRenderer renderer) {
        if (colIdx >= tableModel.getColumnCount()) {
            throw new IllegalStateException("Error in call to makeColumnFromTable()! Number of columns in the tablemodel ("+tableModel.getColumnCount()+") does not match request column index ("+colIdx+")");
        }
        String headerText = tableModel.getColumnName(colIdx);
        if (headerText == null) {
            throw new IllegalStateException("tableModel.getColumnName("+colIdx+") returns null!");
        }
        Boolean columnIsNumber = null;
        for (int row = 0; row < Math.min(SAMPLED_ROWS, tableModel.getRowCount()); row++) {
             Object value = tableModel.getValueAt(row, colIdx);
             if (value != null && (columnIsNumber == null || columnIsNumber)) {
                 columnIsNumber = StringUtils.isNumber(String.valueOf(value));
             }
        }
        Align align = (columnIsNumber != null && columnIsNumber) ? Align.RIGHT : Align.LEFT;
        Integer columnDroppable = tableModel.getColumnDroppable(colIdx);     // the model says (ObjectTableModel.droppable); null: never
        Boolean             wordWrap            = (align == Align.LEFT);
        Boolean             truncationAllowed   = columnIsNumber == null || columnIsNumber == false;

        TableColumnImpl tc = new TableColumnImpl(headerText, 
                                   null, 
                                   tableModel.getColumnName(colIdx), 
                                   colIdx, 
                                   align, 
                                   columnDroppable, 
                                   wordWrap, 
                                   truncationAllowed);
        
        if (renderer != null) {
            tc.setColumnRenderer(renderer);
        }
        return tc; 
    }


}
