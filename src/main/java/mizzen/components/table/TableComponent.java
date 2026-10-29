package mizzen.components.table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import mizzen.components.BlockChars;
import mizzen.components.BoundedComponent;
import mizzen.components.HasName;
import mizzen.components.input.DisplayableValue.SimpleDisplayValue;
import mizzen.components.input.LeafFocusBehaviour;
import mizzen.components.input.UnfocusedSelection;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.ChainedEventHandler;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.Registration;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.TerminalEvent;
import mizzen.events.TerminalEvent.IS_Event_TYPE;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.AsKeyboardHandler;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.events.keyboard.Keys;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.events.mouse.Clickable;
import mizzen.events.mouse.MOUSE_ACTION;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegment.SegmentImpl;
import mizzen.textsegments.ScreenLineSegmentsBuffer;
import mizzen.util.ElapsedTime;
/**
 * A table: a header row and data rows from a TableModel, painted cell by cell (no component per
 * cell - each cell's text is copied in; only the rows on screen are measured and painted).
 * <p/>
 * One TAB stop. A selected row: UP / DOWN, HOME / END, PgUp / PgDn move it (the table scrolls to
 * keep it on screen); a click selects a row; ENTER - or a click on the selected row - activates it
 * (onRowActivated, e.g. "open this file"). onPick hears every selection move (keys and clicks) and
 * hands over the row itself; onRowSelected hears the same as a row number; onCellSelected hears clicks
 * on a cell. pick(row) picks by code, as a click would.
 * <p/>
 * T is what a row is - the model's: a Sale (ObjectTableModel&lt;Sale&gt;), an Object[] (DefaultTableModel,
 * columns only known at run time), ...
 * <pre>
 *     TableComponent&lt;Sale&gt; sales = new TableComponent&lt;Sale&gt;(Colors.Cya, salesModel);
 *     sales.onPick((sale, te) -&gt; saleForm.show(sale));
 * </pre>
 * <p/>
 * The selected row is hilited while the table has focus (setSelectedColor, inverted); without focus
 * it shows in the setUnfocusedSelection style (DIM by default, or NONE).
 */
public class TableComponent<T> extends BoundedComponent<TableComponent<T>> implements ToFocusable, AsKeyboardHandler, Clickable {


    TableModelWidthData tableModel;
    TableModel<T> rows;                       // the model as given: its rows are T (tableModel measures and caches its cells)
    // List<TableColumn> tableColumns;
    boolean hasBorder = true;
    Color headerColor;
    int rowsDisplayed = 0;
    TableLinesStyle linesStyle = TableLinesStyle.ALL;
    boolean spacedRows = true;

    ConditionalEventHandler eventHandler;
    ChainedEventHandler chainedEventHandler;
    int selectedRow = -1;                     // -1: no row selected yet
    int topRow = 0;                           // the first data row on screen
    final List<Integer> rowEnds = new ArrayList<Integer>();   // where each data row drawn ends on screen - the line under it goes there
    Rectangle selectedRowArea;                // where the selected row was painted (for scrolling it into view)
    Color selectedColor = new Colors.FGBGColors(Colors.IYel);     // as ListBox / TreeView
    UnfocusedSelection unfocusedSelection = UnfocusedSelection.DIM;
    BiConsumer<Integer, EventBus> onRowActivated = (row, bus) -> { };

    /** focus: the table shows it itself (its selected row); keys see TableKeys; its own events (onRowSelected ...) */
    private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this)
            .look(LeafFocusBehaviour.Look.NONE)
            .showOnFocus(this::selectedRowRectangle)
            .handleKeys(new TableKeys())
            .whenFocusArrives(this::repaint)
            .whenFocusLeaves(this::repaint)
            .handleEvents(new ConditionalEventHandler.Impl((te) -> getEventHandler().canHandle(te) != null,
                                                          (te) -> getEventHandler().handle(te)));
    TableCellAtPositions tableCellAtPositions;
    TableLayout layout = new TableLayout(new ArrayList<Integer>(), new int[0], new int[0], 0, 0);   // what the last draw showed: the lines and clicks read it

    /** sized by what holds it (a splitter's rules, a resize): it starts at no size */
    public TableComponent(Color color, TableModel<T> tableModel) {
        this(color, Dimension.ZERO, tableModel);
    }

    public TableComponent(Color color, Dimension d, TableModel<T> tableModel) {
        super(color, d);
        this.headerColor = color.getBoldTransform().transform(color);
        this.rows = tableModel;
        this.tableModel = new TableModelWidthData(tableModel);
        this.tableCellAtPositions = new TableCellAtPositions();
    }

    public void setTableModel(TableModel<T> tableModel) {
        clearData();
        this.rows = tableModel;
        this.tableModel = new TableModelWidthData(tableModel);
        this.tableCellAtPositions = new TableCellAtPositions();
    }


    public TableLinesStyle getLinesStyle() {
        return linesStyle;
    }

    public void setLinesStyle(TableLinesStyle linesStyle) {
        this.linesStyle = linesStyle;
        clearData();
    }

    /** the cell's segments within width x height (the border or the bottom can cut a cell), moved to offset - in color if given */
    private TreeSet<ScreenLineSegment> copyCellSegments(IScreenLineSegmentsBuffer cellSegmentsBuffer, Position offset, Color color,
            int width, int height) {
        ScreenLineSegmentsBuffer within = new ScreenLineSegmentsBuffer();
        for (ScreenLineSegment segment : cellSegmentsBuffer.getSegments()) {
            if (segment.getRow() < height && segment.getCol() < width) {
                String text = segment.getText();
                within.add(segment.getCol() + text.length() <= width ? segment
                        : new SegmentImpl(segment.getColors(), new Position.Simple(segment.getRow(), segment.getCol()), text.substring(0, width - segment.getCol())));
            }
        }
        return copyCellSegments(within, offset, color);
    }

    /** the cell's segments moved to offset - in color if given (the selected row), else as they are */
    private TreeSet<ScreenLineSegment> copyCellSegments(IScreenLineSegmentsBuffer cellSegmentsBuffer, Position offset, Color color) {

        //ElapsedTime elapsedTime = new ElapsedTime();

        TreeSet<ScreenLineSegment> destination = ScreenLineSegment.UTIL.makeCollection(null);

        cellSegmentsBuffer.getSegments().stream().map(s -> s.fromOffset(offset))
                          .map(s -> color == null ? s : new SegmentImpl(color, new Position.Simple(s.getRow(), s.getCol()), s.getId(), s.getText()))
                          .forEach(s -> destination.add(s));

        //Logging.info("cell copied " + elapsedTime + "   count " + destination.size());

        return destination;
    }

    public void resize(Dimension d) {
        if (d != null && d.isSameDimension(this)) {
            return;
        }
        super.resize(d);
        // clear table model data so that it forces recalc
        // of its data's dimensions
        clearData();
    }

    public void clearData() {
        this.tableModel.clearData();
        this.tableCellAtPositions.clear();
        this.markAsDirty();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        //Logging.setEnabled(false);

        //Logging.info("Table-draw start--------------------");
        // tableModel.clearData();
       // Logging.info("Table-draw elapsed : " + ElapsedTime.onRunOf(() -> __draw(color, mySegments)));
        ElapsedTime.onRunOf(() -> __draw(color, mySegments));
    }

    public int getRowsDisplayed() {
        return this.rowsDisplayed;
    }

    /**
     * Lays the table out (TableModelWidthData.layOut: which columns, how wide; which rows, how tall)
     * and draws just that - each cell where the layout puts it, cut at the border and the bottom -
     * then the lines.
     */
    protected void __draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        this.clear();
        boolean bottomBorder = TableLinesStyle.ALL.equals(linesStyle) || TableLinesStyle.HEADER_ALL_AND_BORDER.equals(linesStyle);
        layout = tableModel.layOut(getWidth(), getHeight(), topRow, selectedRow, rowGap(), bottomBorder);
        topRow = layout.topRow;
        rowsDisplayed = layout.rowsWhole;
        int bottom = bottomBorder ? getHeight() - 1 : getHeight();

        this.tableCellAtPositions.clear();
        rowEnds.clear();
        selectedRowArea = null;
        for (int i = 0; i < layout.columnCount(); i++) {
            Position at = new Position.Simple(1, layout.columnStart(i));
            IScreenLineSegmentsBuffer header = tableModel.header(layout.columns.get(i), layout.fitted[i], layout.headerHeight);
            mySegments.merge(copyCellSegments(header, at, null, layout.shown[i], layout.headerHeight));
            tableCellAtPositions.addHeaderCell(at, tableModel.modelColumn(layout.columns.get(i)), layout.shown[i], layout.headerHeight);
        }
        int lastRow = -1;
        for (TableLayout.Row row : layout.rows) {
            int linesShown = Math.min(row.height, bottom - row.top);           // the last row can be cut by the bottom
            rowEnds.add(row.top + row.height);
            Color rowColor = row.row == selectedRow ? selectedRowColor() : null;
            if (row.row == selectedRow) {
                selectedRowArea = new Rectangle.Simple(row.top, 0, Math.max(1, linesShown), getWidth());
            }
            for (int i = 0; i < layout.columnCount(); i++) {
                Position at = new Position.Simple(row.top, layout.columnStart(i));
                IScreenLineSegmentsBuffer cell = tableModel.cell(row.row, layout.columns.get(i), layout.fitted[i], row.height);
                mySegments.merge(copyCellSegments(cell, at, rowColor, layout.shown[i], linesShown));
                tableCellAtPositions.addDataCell(at, row.row, tableModel.modelColumn(layout.columns.get(i)), layout.shown[i], linesShown);
            }
            lastRow = row.row;
        }

        drawLines(mySegments, lastRow, layout.columnCount() - 1);
    }

    interface TableLineDraw {
        void drawLines(IScreenLineSegmentsBuffer mySegments, int maxRowRendered, int maxColRendered);
    }

    class DrawLines_NONE implements TableLineDraw {
        public void drawLines(IScreenLineSegmentsBuffer mySegments, int maxRowRendered, int maxColRendered) {
            // do nothing
        }
    }

    BiConsumer<Integer, char[]> makeDrawHorizontalLineFunction(IScreenLineSegmentsBuffer mySegments,
            int maxColRendered, Color borderColor, int boxWidth, Set<Integer> rowsDrawn) {
        BiConsumer<Integer, char[]> drawHorizontalLine = (row, blockChars) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(blockChars[0]);
            // int i = 1;
            for (int c = 0; c <= maxColRendered && sb.length() < (boxWidth - 1); c++) {
                int colWidth = layout.widthOf(c);
                for (int j = 0; j < colWidth && sb.length() < (boxWidth - 1); j++) {
                    sb.append(blockChars[1]);
                }
                if (c != maxColRendered && sb.length() < (boxWidth - 1)) {
                    sb.append(blockChars[2]);
                } else {
                    sb.append(blockChars[3]);
                }
            }
            mySegments.add(new SegmentImpl(borderColor, new Position.Simple(row, 0), sb.toString()));
            rowsDrawn.add(row);
        };
        return drawHorizontalLine;
    }

    class DrawLines_HEADER_ONLY_ALL implements TableLineDraw {
        public void drawLines(IScreenLineSegmentsBuffer mySegments, int maxRowRendered, int maxColRendered) {
            Rectangle.Simple meAsRectangle = new Rectangle.Simple(TableComponent.this);

            Function<Integer, Integer> colVerticalLineOffset = (col) -> layout.widthUpTo(col) + col + 1;
            meAsRectangle.setWidth(Math.min(meAsRectangle.getWidth(), colVerticalLineOffset.apply(maxColRendered) + 1));

            Color borderColor = getColor();

            int boxWidth = meAsRectangle.getWidth();

            Set<Integer> rowsDrawn = new HashSet<Integer>();

            // internal function to draw a line blockChars = (LEFT, HORIZONTAL, INTERSECT AND RIGHT) characters
            BiConsumer<Integer, char[]> drawHorizontalLine = makeDrawHorizontalLineFunction(mySegments, maxColRendered,
                    borderColor, boxWidth, rowsDrawn);

            ElapsedTime timer = new ElapsedTime();

            // Draw top of table
            drawHorizontalLine.accept(0, new char[] { BlockChars.NICE.getTOP_LEFT(), 
                                                      BlockChars.NICE.getHORIZONTAL_LINE(),
                                                      BlockChars.NICE.getHORIZONTAL_DOWN(), 
                                                      BlockChars.NICE.getTOP_RIGHT() });


            // verticals
            String verticalLineCharAsStr = BlockChars.NICE.getVERTICAL_LINE() + "";
            for (int row = 1; row <= 1; row++) {
                if (rowsDrawn.contains(row)) {
                    continue;
                }
                mySegments.add(new SegmentImpl(borderColor, new Position.Simple(row, 0), verticalLineCharAsStr));

                for (int c = 0; c < maxColRendered; c++) {
                    int col = colVerticalLineOffset.apply(c);
                    mySegments.add(new SegmentImpl(borderColor, new Position.Simple(row, col), verticalLineCharAsStr));
                }
                mySegments.add(new SegmentImpl(borderColor, new Position.Simple(row, boxWidth - 1), verticalLineCharAsStr));

            }
            
            drawHorizontalLine.accept(layout.headerHeight + 1,
                    new char[] { BlockChars.NICE.getBOTTOM_LEFT(), 
                                 BlockChars.NICE.getHORIZONTAL_LINE(),
                                 BlockChars.NICE.getHORIZONTAL_UP(), 
                                 BlockChars.NICE.getBOTTOM_RIGHT() });            

        }

    }

    class DrawLines_HEADER_ALL_AND_BORDER implements TableLineDraw {
        public void drawLines(IScreenLineSegmentsBuffer mySegments, int maxRowRendered, int maxColRendered) {
            Rectangle.Simple meAsRectangle = new Rectangle.Simple(TableComponent.this);

            Function<Integer, Integer> colVerticalLineOffset = (col) -> layout.widthUpTo(col) + col + 1;
            meAsRectangle.setWidth(Math.min(meAsRectangle.getWidth(), colVerticalLineOffset.apply(maxColRendered) + 1));

            Color borderColor = getColor();

            int boxWidth = meAsRectangle.getWidth();
            int boxHeight = meAsRectangle.getHeight();
            
            Set<Integer> rowsDrawn = new HashSet<Integer>();

            // internal function to draw a line blockChars = (LEFT, HORIZONTAL, INTERSECT AND RIGHT) characters
            BiConsumer<Integer, char[]> drawHorizontalLine = makeDrawHorizontalLineFunction(mySegments, maxColRendered,
                    borderColor, boxWidth, rowsDrawn);

            // Draw top of table
            drawHorizontalLine.accept(0, new char[] { BlockChars.NICE.getTOP_LEFT(), 
                                                      BlockChars.NICE.getHORIZONTAL_LINE(),
                                                      BlockChars.NICE.getHORIZONTAL_DOWN(), 
                                                      BlockChars.NICE.getTOP_RIGHT() });

            drawHorizontalLine.accept(layout.headerHeight + 1,
                    new char[] { BlockChars.NICE.getVERTICAL_RIGHT(), 
                                 BlockChars.NICE.getHORIZONTAL_LINE(),
                                 BlockChars.NICE.getINTERSECT(), 
                                 BlockChars.NICE.getVERTICAL_LEFT() });
            // verticals
            String verticalLineCharAsStr = BlockChars.NICE.getVERTICAL_LINE() + "";
            for (int row = 1; row < boxHeight; row++) {
                if (rowsDrawn.contains(row)) {
                    continue;
                }
                mySegments.add(new SegmentImpl(borderColor, new Position.Simple(row, 0), verticalLineCharAsStr));

                for (int c = 0; c < maxColRendered; c++) {
                    int col = colVerticalLineOffset.apply(c);
                    mySegments.add(new SegmentImpl(borderColor, new Position.Simple(row, col), verticalLineCharAsStr));
                }
                mySegments.add(new SegmentImpl(borderColor, new Position.Simple(row, boxWidth - 1), verticalLineCharAsStr));
            }

            drawHorizontalLine.accept(meAsRectangle.getHeight() - 1,
                    new char[] { BlockChars.NICE.getBOTTOM_LEFT(), 
                                 BlockChars.NICE.getHORIZONTAL_LINE(),
                                 BlockChars.NICE.getHORIZONTAL_UP(), 
                                 BlockChars.NICE.getBOTTOM_RIGHT() }); 
        }

    }

    class DrawLines_ALL implements TableLineDraw {
        public void drawLines(IScreenLineSegmentsBuffer mySegments, int maxRowRendered, int maxColRendered) {
            Rectangle.Simple meAsRectangle = new Rectangle.Simple(TableComponent.this);

            Function<Integer, Integer> colVerticalLineOffset = (col) -> layout.widthUpTo(col) + col + 1;
            meAsRectangle.setWidth(Math.min(meAsRectangle.getWidth(), colVerticalLineOffset.apply(maxColRendered) + 1));

            Color borderColor = getColor();

            int boxWidth = meAsRectangle.getWidth();
            int boxHeight = meAsRectangle.getHeight();

            Set<Integer> rowsDrawn = new HashSet<Integer>();

            // internal function to draw a line blockChars = (LEFT, HORIZONTAL, INTERSECT AND RIGHT) characters
            BiConsumer<Integer, char[]> drawHorizontalLine = makeDrawHorizontalLineFunction(mySegments, maxColRendered,
                    borderColor, boxWidth, rowsDrawn);

            ElapsedTime timer = new ElapsedTime();

            // Draw top of table
            drawHorizontalLine.accept(0, new char[] { BlockChars.NICE.getTOP_LEFT(), 
                                                      BlockChars.NICE.getHORIZONTAL_LINE(),
                                                      BlockChars.NICE.getHORIZONTAL_DOWN(), 
                                                      BlockChars.NICE.getTOP_RIGHT() });

            // Logging.info("Top line  : " + timer);

            timer.reset();

            /*  need to draw header line */
            drawHorizontalLine.accept(layout.headerHeight + 1,
                    new char[] { BlockChars.NICE.getVERTICAL_RIGHT(), 
                                 BlockChars.NICE.getHORIZONTAL_LINE(),
                                 BlockChars.NICE.getINTERSECT(), 
                                 BlockChars.NICE.getVERTICAL_LEFT() });

            // a line under each row that ends above the bottom border - where the layout put that row's end (rows
            // from topRow, each as high as its tallest cell), so a line never lands inside a row
            for (int r = 0; r < rowEnds.size() && rowEnds.get(r) < boxHeight - 1; r++) {
                drawHorizontalLine.accept(rowEnds.get(r),
                        new char[] { BlockChars.NICE.getVERTICAL_RIGHT(), 
                                     BlockChars.NICE.getHORIZONTAL_LINE(),
                                     BlockChars.NICE.getINTERSECT(), 
                                     BlockChars.NICE.getVERTICAL_LEFT() });
            }
            //Logging.info("Middle intersecting lines  : " + timer);

            timer.reset();
            drawHorizontalLine.accept(meAsRectangle.getHeight() - 1,
                    new char[] { BlockChars.NICE.getBOTTOM_LEFT(), 
                                 BlockChars.NICE.getHORIZONTAL_LINE(),
                                 BlockChars.NICE.getHORIZONTAL_UP(), 
                                 BlockChars.NICE.getBOTTOM_RIGHT() });

            // Logging.info("Bottom line  : " + timer);

            timer.reset();
            String verticalLineCharAsStr = BlockChars.NICE.getVERTICAL_LINE() + "";
            for (int row = 1; row < boxHeight; row++) {
                if (rowsDrawn.contains(row)) {
                    continue;
                }
                mySegments.add(new SegmentImpl(borderColor, new Position.Simple(row, 0), verticalLineCharAsStr));

                for (int c = 0; c < maxColRendered; c++) {
                    int col = colVerticalLineOffset.apply(c);
                    mySegments.add(new SegmentImpl(borderColor, new Position.Simple(row, col), verticalLineCharAsStr));
                }
                mySegments.add(new SegmentImpl(borderColor, new Position.Simple(row, boxWidth - 1), verticalLineCharAsStr));

            }
            // Logging.info("Verticals   : " + timer); 
       }

    }
    
    /** the line under each data row: the line drawn there (ALL), else a blank line unless rows are packed */
    int rowGap() {
        return TableLinesStyle.ALL.equals(getLinesStyle()) || spacedRows ? 1 : 0;
    }

    public boolean isSpacedRows() {
        return spacedRows;
    }

    /**
     * true (the default): a blank line under each row - easier to read; false: rows one under the
     * other, twice as many on show. With lines between rows (ALL) there is always the line.
     */
    public TableComponent<T> setSpacedRows(boolean spacedRows) {
        this.spacedRows = spacedRows;
        clearData();
        return this;
    }

    TableLineDraw getLineDraw() {
        if (this.getLinesStyle() == null || TableLinesStyle.NONE.equals(this.getLinesStyle()) ) {
            return new DrawLines_NONE();
        }
        if ( TableLinesStyle.ALL.equals(this.getLinesStyle()) ) {
            return new DrawLines_ALL();
        }
        if ( TableLinesStyle.HEADER_FULL.equals(this.getLinesStyle()) ) {
            return new DrawLines_HEADER_ONLY_ALL();
        }
        if ( TableLinesStyle.HEADER_ALL_AND_BORDER.equals(this.getLinesStyle()) ) {
            return new DrawLines_HEADER_ALL_AND_BORDER();
        }
        throw new IllegalStateException("Not valid TableLinesStyle" + this.getLinesStyle());
    }

    /**
     * Draw lines for table .. TODO: Need to add options for: - don't draw lines -
     * draw only horizontal lines - draw only vertical lines - draw only lines for
     * header - draw only underlines for header
     * 
     * TODO: I tried using my BlockChars.UTIL.drawHorizontalLine(),
     * BlockChars.UTIL.drawVerticalLine(), and BlockChars.UTIL.drawBox(). Although
     * very convenient, they are so slow as to be unusable
     * 
     * So instead adding the line segments manually - which is faster ( but actually
     * not fast enough )
     *
     * @param mySegments
     * @param maxRowRendered
     * @param maxColRendered
     */
    private void drawLines(IScreenLineSegmentsBuffer mySegments, int maxRowRendered, int maxColRendered) {
        getLineDraw().drawLines(mySegments, maxRowRendered, maxColRendered);

    }

    public void setColumnAt(int colIdx, TableColumn tableColumn) {
        this.tableModel.setColumnAt(colIdx, tableColumn);
    }

    public void addColumn(TableColumn tableColumn) {
        this.tableModel.addColumn(tableColumn);
    }
    
    
    /** work the column widths out again from the rows on screen - as after a resize */
    public void recomputeColumnWidthsFromData() {
        clearData();
    }

    /**
     * false (the default): each column is as wide as the rows on screen need - scrolling can widen or
     * narrow it; true: scrolling only ever widens a column, until the table is resized.
     */
    public TableComponent<T> setWidthsOnlyGrow(boolean widthsOnlyGrow) {
        tableModel.widthsOnlyGrow = widthsOnlyGrow;
        clearData();
        return this;
    }

    private CellSelectedEventHandler cellSelectedHandler = null;

    public void onCellSelected(CellSelectedCustomHandler handler) {
        if (cellSelectedHandler != null) {
            getChainedEventHandler().removeEventHandler(cellSelectedHandler);
        }
        cellSelectedHandler = new CellSelectedEventHandler(this, handler);
        getChainedEventHandler().appendEventHandler(cellSelectedHandler);
    }

    // everyone told of a row selection - onRowSelected's and onPick's listeners, side by side: one
    // handler on the table's chain claims the event and runs them all (a chain runs only the first
    // handler that claims an event)
    private final ChainedEventHandler rowSelectedListeners = ChainedEventHandler.useHandleAllStrategy("rowSelected");
    private boolean rowSelectedListenersOnChain = false;

    private Registration addRowSelectedListener(RowSelectedCustomHandler handler) {
        if (!rowSelectedListenersOnChain) {
            rowSelectedListenersOnChain = true;
            getChainedEventHandler().appendEventHandler(new RowSelectedEventHandler(this, rowSelectedListeners::handle));
        }
        ConditionalEventHandler listener = new ConditionalEventHandler.Impl((te) -> true, (te) -> handler.handle((RowSelectedEvent) te));
        rowSelectedListeners.appendEventHandler(listener);
        return () -> rowSelectedListeners.removeEventHandler(listener);
    }

    /** handler hears every selection move as a row number - alongside any others; the Registration takes it out again */
    public Registration onRowSelected(RowSelectedCustomHandler handler) {
        return addRowSelectedListener(handler);
    }

    /**
     * picked hears every selection move (keys, clicks, pick) with the row itself - alongside any
     * others; the Registration takes it out again. It needs a model that gives its rows (getRow):
     * ObjectTableModel, DefaultTableModel; one that doesn't fails here, not at the first pick.
     * <pre>
     *     sales.onPick((sale, te) -&gt; saleForm.show(sale));
     * </pre>
     */
    public Registration onPick(BiConsumer<T, TerminalEvent<?>> picked) {
        if (rows.getRowCount() > 0) {
            rows.getRow(0);                                  // fail now if the model gives no rows
        }
        return addRowSelectedListener((te) -> picked.accept(rows.getRow(te.getRowSelected().getTableRow()), te));
    }

    /**
     * Pick row by code: selected, scrolled to, and onPick / onRowSelected told - as a click would. It
     * must be one of the rows (as equals finds it); one that isn't fails. setSelectedRow moves the
     * selection without telling anyone.
     */
    public TableComponent<T> pick(T row, EventBus bus) {
        int index = indexOf(row);
        if (index < 0) {
            throw new IllegalArgumentException(HasName.UTIL.getName(this) + ": pick(" + row + ") - not one of the table's rows");
        }
        select(index, bus);
        return this;
    }

    /** the picked row; null if none */
    public T getPicked() {
        return selectedRow < 0 ? null : rows.getRow(selectedRow);
    }

    private int indexOf(T row) {
        for (int i = 0; i < rows.getRowCount(); i++) {
            if (Objects.equals(rows.getRow(i), row)) {
                return i;
            }
        }
        return -1;
    }
    
    public static class CellSelected {

        TableDataCellAtPosition cell;

        public CellSelected(TableDataCellAtPosition cell) {
            this.cell = cell;
        }
        
        public int getTableRow() {
            return cell.tableRow;
        }

        /** the model's column - with getTableRow, what getValueAt takes, whatever columns were dropped */
        public int getTableCol() {
            return cell.tableColumn;
        }

        public boolean isHeader() {
            return cell.isHeader();
        }
        
    }

    public static class RowSelected {
        
        TableDataCellAtPosition cell;

        public RowSelected(TableDataCellAtPosition cell) {
            this.cell = cell;
        }
        
        public int getTableRow() {
            return cell.tableRow;
        }

        public boolean isHeader() {
            return cell.isHeader();
        }
    }

    public static final Predicate<TerminalEvent<?>>  canHandle_TABLE_ROW_SELECTED = new IS_Event_TYPE(TerminalEvent.TYPE.TABLE_ROW_SELECTED);
    public static final Predicate<TerminalEvent<?>>  canHandle_TABLE_CELL_SELECTED = new IS_Event_TYPE(TerminalEvent.TYPE.TABLE_CELL_SELECTED);

    @FunctionalInterface
    public static interface CellSelectedCustomHandler {
        void handle(CellSelectedEvent te);
    }

    @FunctionalInterface
    public static interface RowSelectedCustomHandler {
        void handle(RowSelectedEvent te);
    }

    public static class CellSelectedEvent extends TerminalEvent<CellSelected> {
        private static final long serialVersionUID = -6811173196070099896L;
        public CellSelectedEvent(TableComponent<?> tableComponent, CellSelected cellSelected) {
            super(tableComponent, TerminalEvent.TYPE.TABLE_CELL_SELECTED, cellSelected);
        }
        public CellSelected getCellSelected() {
            return this.getEventData();
        }
    }

    public static class RowSelectedEvent extends TerminalEvent<RowSelected> {
        private static final long serialVersionUID = -7941222548975911844L;

        public RowSelectedEvent(TableComponent<?> tableComponent, RowSelected rowSelected) {
            super(tableComponent, TerminalEvent.TYPE.TABLE_ROW_SELECTED, rowSelected);
        }
        public RowSelected getRowSelected() {
            return this.getEventData();
        }
    }

    public static class CellSelectedEventHandler extends ConditionalEventHandler.Impl {
        public CellSelectedEventHandler(TableComponent<?> src, CellSelectedCustomHandler cellSelectedCustomHandler) {
            super(canHandle_TABLE_CELL_SELECTED.and((te)-> te.getSource() == src), 
                    (te) -> {
                        cellSelectedCustomHandler.handle((CellSelectedEvent)te);
                    });
        }
    }

    public static class RowSelectedEventHandler extends ConditionalEventHandler.Impl {
        public RowSelectedEventHandler(TableComponent<?> src, RowSelectedCustomHandler rowSelectedCustomHandler) {
            super(canHandle_TABLE_ROW_SELECTED.and((te)-> te.getSource() == src), 
                    (te) -> {
                        rowSelectedCustomHandler.handle((RowSelectedEvent)te);
                    });
        }
    }

    private ConditionalEventHandler buildEventHandler() {

        this.chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);

        ConditionalEventHandler complexKeyHandler = chainedEventHandler.asConditionalEventHandler();

//        appendEventHandler(new KeyInputEventHandler(Keys.BACKSPACE_KEYSTROKE,
//                                                    (te) -> Logging.info( TableComponent.this.getName() +  ": Backspace clicked")));


        return complexKeyHandler;
    }

    private TableComponent<T> appendEventHandler(ConditionalEventHandler eventHandler) {
        this.getChainedEventHandler().appendEventHandler(eventHandler);
        return this;
    }

    private ConditionalEventHandler getEventHandler() {
        if (this.eventHandler == null) {
            this.eventHandler = buildEventHandler();
        }
        return this.eventHandler;
    }

    private ChainedEventHandler getChainedEventHandler() {
        if (this.chainedEventHandler == null) {
            getEventHandler();
        }
        return this.chainedEventHandler;
    }

    // ---- focus, keys, selection -------------------------------------------------------------

    @Override
    public Focusable asFocusable() {
        return focus.asFocusable();
    }

    @Override
    public KeyboardHandler asKeyboardHandler() {
        return focus.asKeyboardHandler();
    }

    public boolean hasFocus() {
        return focus.hasFocus();
    }

    public void requestFocus(TerminalEvent<?> te) {
        focus.requestFocus(te.getEventBus());
    }

    /** the selected data row; -1 if none */
    public int getSelectedRow() {
        return selectedRow;
    }

    /** select row (by code: onRowSelected is not told); the table scrolls to show it */
    public TableComponent<T> setSelectedRow(int row) {
        if (row >= -1 && row < tableModel.getRowCount()) {
            selectedRow = row;
            scrollToSelectedRow();
            markAsDirty();
        }
        return this;
    }

    /** the colour of the selected row (inverted while the table has focus) */
    public TableComponent<T> setSelectedColor(Color selectedColor) {
        this.selectedColor = selectedColor;
        markAsDirty();
        return this;
    }

    /** how the selected row shows without focus: DIM (default) or NONE (BRACKETS / ARROW show as DIM) */
    public TableComponent<T> setUnfocusedSelection(UnfocusedSelection unfocusedSelection) {
        this.unfocusedSelection = unfocusedSelection;
        markAsDirty();
        return this;
    }

    /** ENTER, or a click on the selected row: (row, eventBus) */
    public TableComponent<T> onRowActivated(BiConsumer<Integer, EventBus> onRowActivated) {
        this.onRowActivated = onRowActivated;
        return this;
    }

    /** the table's keys - see the class comment. It wants only these, so other keys go on to its parents. */
    private class TableKeys implements KeyboardHandler {
        private final KeyStroke[] mine = { Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE, Keys.HOME_KEYSTROKE, Keys.END_KEYSTROKE,
                                           Keys.PGUP_KEYSTROKE, Keys.PGDN_KEYSTROKE, Keys.ENTER_KEYSTROKE };

        @Override
        public boolean wantsKey(KeyEvent ke) {
            if (tableModel.getRowCount() == 0) {
                return false;
            }
            for (KeyStroke key : mine) {
                if (ke.isKeyStroke(key)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onKey(KeyEvent ke) {
            if (!wantsKey(ke)) {
                return false;
            }
            EventBus bus = ke.getEventBus();
            int page = Math.max(1, rowsDisplayed > 2 ? rowsDisplayed - 1 : rowsDisplayed);   // a screenful, keeping one row in view
            if (ke.isKeyStroke(Keys.UP_KEYSTROKE)) {
                select(selectedRow < 0 ? 0 : selectedRow - 1, bus);
            } else if (ke.isKeyStroke(Keys.DOWN_KEYSTROKE)) {
                select(selectedRow + 1, bus);
            } else if (ke.isKeyStroke(Keys.HOME_KEYSTROKE)) {
                select(0, bus);
            } else if (ke.isKeyStroke(Keys.END_KEYSTROKE)) {
                select(tableModel.getRowCount() - 1, bus);
            } else if (ke.isKeyStroke(Keys.PGUP_KEYSTROKE)) {
                select(Math.max(0, selectedRow - page), bus);
            } else if (ke.isKeyStroke(Keys.PGDN_KEYSTROKE)) {
                select(Math.min(tableModel.getRowCount() - 1, Math.max(0, selectedRow) + page), bus);
            } else if (ke.isKeyStroke(Keys.ENTER_KEYSTROKE) && selectedRow >= 0) {
                onRowActivated.accept(selectedRow, bus);
            }
            return true;     // the table's keys are used up even at the first / last row
        }
    }

    /** select row (if it is one), scroll to it, and tell onRowSelected */
    private void select(int row, EventBus bus) {
        if (row < 0 || row >= tableModel.getRowCount() || row == selectedRow) {
            return;
        }
        selectedRow = row;
        scrollToSelectedRow();
        bus.raiseEvent(new RowSelectedEvent(this, new RowSelected(new TableDataCellAtPosition(Position.ZERO, row, -1, 0, 0, false))));
        repaint(bus);
    }

    /** keep the selected row on screen: move the first row shown if the selected one is above or below the screen */
    private void scrollToSelectedRow() {
        if (selectedRow < 0) {
            return;
        }
        if (selectedRow < topRow) {
            topRow = selectedRow;
        } else if (rowsDisplayed > 0 && selectedRow >= topRow + rowsDisplayed) {
            topRow = selectedRow - rowsDisplayed + 1;           // a guess with rows this tall: the layout shows it whole
        }
        markAsDirty();
    }

    private Color selectedRowColor() {
        if (focus.hasFocus()) {
            return selectedColor.invert();
        }
        return unfocusedSelection == UnfocusedSelection.NONE ? null : selectedColor;
    }

    /** where the selected row is on the table (for scrolling it into view); the whole table if it isn't painted yet */
    private Rectangle selectedRowRectangle() {
        return selectedRowArea != null ? selectedRowArea : new Rectangle.Simple(0, 0, getHeight(), getWidth());
    }

    private void repaint(EventBus bus) {
        markAsDirty();
        RequestUpdateCanvasEvent.invoke(bus, this);
    }

    public void onClick(EventBus eventBus) {
        // place-holder - not used see onClick(ApplicationMouseEvent appMouseEvent)
    }
    
    public void onClick(ApplicationMouseEvent appMouseEvent) {
        if (MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction())) {

            Position positionWithinTableComponent = appMouseEvent.getMousePosition();
            TableDataCellAtPosition cell = this.tableCellAtPositions.findDataCellFromPosition(positionWithinTableComponent);
            EventBus bus = appMouseEvent.getEventBus();
            focus.requestFocus(bus);
            if (cell != null) {
                bus.raiseEvent(new CellSelectedEvent(this, new CellSelected(cell)));
                if (cell.isHeader()) {
                    return;                                         // a heading: not a row to select or activate
                }
                if (cell.tableRow == selectedRow) {
                    onRowActivated.accept(selectedRow, bus);     // a click on the selected row activates it
                } else {
                    selectedRow = cell.tableRow;
                    bus.raiseEvent(new RowSelectedEvent(this, new RowSelected(cell)));
                    repaint(bus);
                }
            }
        }
    }

    private static class TableCellAtPositions {
        TreeSet<TableDataCellAtPosition> cellPositions = makeCellPositionsSet();

        public void clear() {
            this.cellPositions = makeCellPositionsSet();
        }

        private TreeSet<TableDataCellAtPosition> makeCellPositionsSet() {
            return new TreeSet<TableDataCellAtPosition>(Position.UTIL.POS_COMPARE);
        }

        void addHeaderCell(Position cellOffset, int col, int width, int height) {
            this.cellPositions.add(new TableDataCellAtPosition(cellOffset, -1, col, width, height, true));
        }

        void addDataCell(Position cellOffset, int row, int col, int width, int height) {
            this.cellPositions.add(new TableDataCellAtPosition(cellOffset, row, col, width, height, false));
        }

        public TableDataCellAtPosition findDataCellFromPosition(Position positionOnTable) {
            for (TableDataCellAtPosition cell : cellPositions) {
                if (cell.containsPosition(positionOnTable)) {
                    return cell;
                }
            }
            return null;
        } 

    }

    private static class TableDataCellAtPosition implements Rectangle {
        Position positionOnView;
        int tableRow;
        int tableColumn;
        int width;
        int height;
        boolean header;

        /** a cell on screen, by the model's row and column (what getValueAt takes) - wherever scrolling and dropped columns put it */
        public TableDataCellAtPosition(Position positionOnView, int tableRow, int tableColumn, int width, int height, boolean isHeader) {
            this.positionOnView = positionOnView;
            this.tableRow = tableRow;
            this.tableColumn = tableColumn;
            this.width = width;
            this.height = height;
            this.header = isHeader;
        }

        @Override
        public int getRow() {
            return positionOnView.getRow();
        }

        @Override
        public int getCol() {
            return positionOnView.getCol();
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public void moveTo(int row, int col) {
            // do nothing
        }

        public boolean isHeader() {
            return this.header;
        }

        @Override
        public String toString() {
            return "TableDataCellAtPosition [positionOnView=" + positionOnView + ",\n    tableRow=" + tableRow
                    + ", tableColumn=" + tableColumn + ", \n    header=" + header + "]";
        }

    }

    public static enum TableLinesStyle {
        NONE,
        HEADER_FULL,
        HEADER_ALL_AND_BORDER,
        ALL
    }

    public static class TableLinesStyleOption extends SimpleDisplayValue<TableLinesStyle> {
            public TableLinesStyleOption(String displayValue, TableLinesStyle value) {
                super(displayValue, value);
            }
    }

    public static TableLinesStyleOption[] tableLinesStyleOptions() {
        TableLinesStyleOption[] options = new TableLinesStyleOption[] {
                new TableLinesStyleOption("Lines - None", TableLinesStyle.NONE),
                new TableLinesStyleOption("Lines - HEADER" , TableLinesStyle.HEADER_FULL),
                new TableLinesStyleOption("Lines - HEADER+BORDER" , TableLinesStyle.HEADER_ALL_AND_BORDER),
                new TableLinesStyleOption("Lines - ALL" , TableLinesStyle.ALL),
        };
        return options;
    }

}
