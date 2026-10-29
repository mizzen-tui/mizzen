package mizzen.components.table;

import java.util.Comparator;

import mizzen.geom.Align;
import mizzen.util.CompareUtil;


/** 
 * Used by {@link TableComponent}.  
 * 
 * Defines characteristics of a Column in TableComponent
 * 
 **/
public interface TableColumn {

//    public enum Align
//    {
//        LEFT, CENTER, RIGHT
//    }

    public static Align          DEFAULT_ALIGN    = Align.CENTER;
    
    public static Align alignFromString(String align)
    {
        if (align == null || align.trim().equals(""))
        {
            return DEFAULT_ALIGN;
        }
        try
        {
            return Align.valueOf(align.trim().toUpperCase());
        } catch (IllegalArgumentException e)
        {
            return DEFAULT_ALIGN;
        }
    }
    

    String getHeaderText();
    Integer getPreferredWidth();
    default Integer getPreferredHeight() {
        return -1;
    } 

    String getColumnName();
    Integer getPosition();

    Align getAlign();

    Integer getColumnDroppable();
    Boolean isWordWrappable();
    Boolean isTruncationAllowed();

    /**
     *  This is a composite sorter that
     *  sorts by defined droppable columns first,
     *  and then once all droppable columns removed, then by
     *  columns from right to left
     */
    public static Comparator<TableColumn> SORT_IN_ORDER_COLUMNS_SHOULD_BE_DROPPED = new Comparator<TableColumn>()
    {
        @Override
        public int compare(TableColumn o1, TableColumn o2)
        {
                                                   // sort by droppable order first and then descending by column position (ie from right to left)
            return CompareUtil.compositeCompare(o1, o2, SORT_BY_DROPPABLE_COLUMNS, CompareUtil.descendingComparator(SORT_BY_COLUMN_POSTION));
        }
    };

    /**
     * sort by droppable columns
     */
    public static Comparator<TableColumn> SORT_BY_DROPPABLE_COLUMNS = new Comparator<TableColumn>()
    {
        int getColumnDroppable(TableColumn c)
        {
            if (c.getColumnDroppable()==null)
            {
                return Integer.MAX_VALUE;
            }
            return c.getColumnDroppable();
        }
        @Override
        public int compare(TableColumn o1, TableColumn o2)
        {
            return CompareUtil.compareNullableObjects(getColumnDroppable(o1), getColumnDroppable(o2));
        }
    };

    /**
     * sort by column position
     */
    public static Comparator<TableColumn> SORT_BY_COLUMN_POSTION = new Comparator<TableColumn>()
    {
        int getPositionAsInt(TableColumn c)
        {
            if (c.getPosition()==null)
            {
                return Integer.MAX_VALUE;
            }
            return (int) c.getPosition();
        }

        @Override
        public int compare(TableColumn o1, TableColumn o2)
        {
            return Integer.compare(getPositionAsInt(o1), getPositionAsInt(o2));
        }
    };

    /** how the column's cells are drawn and measured - e.g. new TextCellRenderer(), CellRenderers.meter(..) */
    void setColumnRenderer(CellRenderer renderer);

    CellRenderer getRenderer();

    /** how its header is drawn; by default as its cells are */
    void setHeaderRenderer(CellRenderer renderer);

    default CellRenderer getHeaderRenderer() {
        return getRenderer();
    }

    public static class Delegate implements TableColumn {

        private TableColumn delegate;
        
        public Delegate(TableColumn delegate) {
            super();
            this.delegate = delegate;
        }

        @Override
        public String getHeaderText() {
            return delegate.getHeaderText();
        }

        @Override
        public Integer getPreferredWidth() {
            return delegate.getPreferredWidth();
        }

        @Override
        public String getColumnName() {
            return delegate.getColumnName();
        }

        @Override
        public Integer getPosition() {
            return delegate.getPosition();
        }

        @Override
        public Align getAlign() {
            return delegate.getAlign();
        }

        @Override
        public Integer getColumnDroppable() {
            return delegate.getColumnDroppable();
        }

        @Override
        public Boolean isWordWrappable() {
            return delegate.isWordWrappable();
        }

        @Override
        public Boolean isTruncationAllowed() {
            return delegate.isTruncationAllowed();
        }

        @Override
        public void setColumnRenderer(CellRenderer renderer) {
            delegate.setColumnRenderer(renderer);
        }

        @Override
        public CellRenderer getRenderer() {
            return delegate.getRenderer();
        }
        
        @Override
        public void setHeaderRenderer(CellRenderer renderer) {
            delegate.setHeaderRenderer(renderer);
        }

        @Override
        public CellRenderer getHeaderRenderer() {
            return delegate.getHeaderRenderer();
        }
    }
}
