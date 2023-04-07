package dtb.terminalIO.components.table;

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
    private TableColumnRenderer    renderer;
    
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
        this(base.getHeaderText(), base.getPreferredWidth(), base.getColumnName(), base.getPosition(), 
            base.getAlign(), base.getColumnDroppable(), base.isWordWrappable(), base.isTruncationAllowed());
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
    }

    @Override
    public TableColumnRenderer getRenderer() {
        return renderer;
    }

    public Integer getPreferredHeight() {
        return preferredHeight;
    }

    public void setPreferredHeight(Integer preferredHeight) {
        this.preferredHeight = preferredHeight;
    }


}
