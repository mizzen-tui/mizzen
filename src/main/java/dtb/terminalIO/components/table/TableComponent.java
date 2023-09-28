package dtb.terminalIO.components.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

import dtb.terminalIO.components.BlockChars;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.textsegments.ScreenLineSegmentsBuffer;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;

public class TableComponent extends BoundedComponent<TableComponent> {

    TableModelWidthData tableModel;
    // List<TableColumn> tableColumns;
    boolean hasBorder = true;
    Color headerColor;
    int rowsDisplayed = 0;

    public TableComponent(Color color, Dimension d, TableModel tableModel) {
        super(color, d);
        this.tableModel = new TableModelWidthData(tableModel);
        this.headerColor = color.getBoldTransform().transform(color);
    }

    private TreeSet<ScreenLineSegment> copyCellSegments(IScreenLineSegmentsBuffer cellSegmentsBuffer, Position offset) {

        //ElapsedTime elapsedTime = new ElapsedTime();

        TreeSet<ScreenLineSegment> destination = ScreenLineSegment.UTIL.makeCollection(null);

        cellSegmentsBuffer.getSegments().stream().map(s -> s.fromOffset(offset)).forEach(s -> destination.add(s));

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
        tableModel.clearData();
        this.markAsDirty();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        //Logging.setEnabled(false);

        new TableColumnDataBuilderDropColumnsWhenNeeded(this);
        
        Logging.info("Table-draw start--------------------");
        // tableModel.clearData();
        Logging.info("Table-draw elapsed : " + ElapsedTime.onRunOf(() -> __draw(color, mySegments)));
    }

    public int getRowsDisplayed() {
        return this.rowsDisplayed;
    }

    protected void __draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        long totalRenderTime = 0;
        ElapsedTime renderCellTime = new ElapsedTime();
        // TODO Auto-generated method stub
        this.clear();

        ColumnsForOutput columnsForOutput = new TableColumnDataBuilderDropColumnsWhenNeeded(this).getColumnData(this.tableModel);

        int rowCount = tableModel.getRowCount();
        int colCount = columnsForOutput.getVisibleColumnCount();
        int rowpos = 1;
        int maxCellHeight = this.getHeight() - 2; // TODO: need to adjust for header height ( later )
        int maxCol = Integer.MAX_VALUE;
        int mmCol = -1;
        int maxRow = -1;
        {  // Calc header data
            int maxRowHeight = -1;
            int colOffset = 1;
            for (int col = 0; col < colCount; col++) {
                TableColumnWithDataAndWidths tc = columnsForOutput.visibleColumnDataList.get(col);
                int cellHeight = maxCellHeight;
                int preferredHeight = tc.getPreferredHeight();
                if (preferredHeight > 0) {
                    cellHeight = Math.min(preferredHeight, maxCellHeight);
                }
                //int cellWidth = tc.getSizeToAlignCellData();
                int cellWidth = tc.getPreferredWidth();
                if (this.getWidth() < (cellWidth + colOffset + 1)) {
                    cellWidth = (cellWidth + colOffset) - this.getWidth();
                    if (cellWidth < 1) {
                        break;
                    }
                }
                TableColumnRenderer renderer = tc.getHeaderRenderer();
                IScreenLineSegmentsBuffer cellSegmentsBuffer = tableModel.getOrSetHeaderSegmentsDataAt(col, cellHeight, cellWidth, renderer);
                int segmentHeight = cellSegmentsBuffer.getHeight();
                maxRowHeight = Math.max(segmentHeight, maxRowHeight);
                int curWidth = Math.max(cellSegmentsBuffer.getWidth(), cellWidth);
                Position cellOffset = new Position.Simple(rowpos, colOffset);
                colOffset += curWidth;
                if (!this.containsPosition(cellOffset.addCols(curWidth))) {
                    break;
                }
                mmCol = col;
            }
        }

        // FIRST LOOP -- generate column Data as Segments and store in TableModel
        // calculated row/col positions
        for (int row = 0; row < rowCount; row++) {
            int maxRowHeight = -1;
            int colOffset = 1;
            for (int col = 0; col < colCount; col++) {
                TableColumnWithDataAndWidths tc = columnsForOutput.visibleColumnDataList.get(col);
                int cellHeight = maxCellHeight;
                int preferredHeight = tc.getPreferredHeight();
                if (preferredHeight > 0) {
                    cellHeight = Math.min(preferredHeight, maxCellHeight);
                }
                //int cellWidth = tc.getSizeToAlignCellData();
                int cellWidth = tc.getPreferredWidth();
                if (this.getWidth() < (cellWidth + colOffset + 1)) {
                    cellWidth = (cellWidth + colOffset) - this.getWidth();
                    if (cellWidth < 1) {
                        break;
                    }
                }
                TableColumnRenderer renderer = tc.getRenderer();
                IScreenLineSegmentsBuffer cellSegmentsBuffer = tableModel.getOrSetSegmentsDataAt(row, col, cellHeight, cellWidth, renderer);
                int segmentHeight = cellSegmentsBuffer.getHeight();
                maxRowHeight = Math.max(segmentHeight, maxRowHeight);
                int curWidth = Math.max(cellSegmentsBuffer.getWidth(), cellWidth);
                Position cellOffset = new Position.Simple(rowpos, colOffset);
                colOffset += curWidth;
                if (!this.containsPosition(cellOffset.addCols(curWidth))) {
                    break;
                }
                mmCol = col;
            } /* eof-col-loop */

            maxCol = Math.min(maxCol, mmCol);
            rowpos += maxRowHeight + 1;
            maxCellHeight = this.getHeight() - 2 - rowpos;
            if (!this.containsPosition(new Position.Simple(rowpos, 0))) {
                break;
            }
            maxRow = Math.max(maxRow, row);
        } /* eof-row-loop */

        maxCol = Math.min(maxCol, colCount - 1);
        // getWidthForRange(int minCol, int maxCol)
        int tableComponentWidth = this.getWidth();

        Function<Integer, Integer> colToWidthNeeded = (col) -> this.tableModel.getWidthColumnForRange(0, col) + col + 2;

        if (tableComponentWidth < colToWidthNeeded.apply(maxCol)) {
            // does not fit -- so let's squeeze last maxcol -- later make this flexible as
            // to what gets squeezed
            int offset = colToWidthNeeded.apply(maxCol - 1); // this.tableModel.getWidthColumnForRange(0,maxCol-1)+(maxCol);
            while (maxCol > 0 && offset > 0 && offset > (tableComponentWidth - 1)) {
                maxCol--;
                offset = maxCol == 0 ? 0 : colToWidthNeeded.apply(maxCol - 1);
            }
            TableColumnWithDataAndWidths tc = (TableColumnWithDataAndWidths) this.tableModel.getTableColumnAt(maxCol);
            TableColumnRenderer renderer = tc.getRenderer();
            int cellWidth = tc.getSizeToAlignCellData();
            if (tableComponentWidth < (cellWidth + offset + 1)) {
                cellWidth = tableComponentWidth - (offset + 1);
                if (cellWidth < 1) {
                    maxCol--;
                } else {
                    for (int row = 0; row < maxRow; row++) {
                        int rowHeight = tableModel.getMaxHeightForRow(row);
                        tableModel.getOrSetSegmentsDataAt(row, maxCol, rowHeight, cellWidth, renderer);
                    }
                }
            }
        }

        // SECOND LOOP -- RENDER THE DATA
        rowpos = 1;
        int colOffset = 1;
        maxCellHeight = this.getHeight() - 2; // TODO: need to adjust for header height ( later )
        int maxColRendered = -1;
        int maxRowRendered = -1;
        this.rowsDisplayed = 0;

        {  // Calc header data
            int maxRowHeight = -1;
            int rowHeight = tableModel.maxHeaderHeight;
            for (int col = 0; col <= maxCol; col++) {
                int colWidth = tableModel.getMaxWidthForColumn(col);
                IScreenLineSegmentsBuffer cellSegmentsBuffer = tableModel.getHeaderSegmentsDataAt(col);
                if (cellSegmentsBuffer == null) {
                    throw new NullPointerException("NULL HEADER SEGMENTSBUFFER AT (" + col + ")- cannot render!");
                }
                Position cellOffset = new Position.Simple(rowpos, colOffset);
                mySegments.merge(copyCellSegments(cellSegmentsBuffer, cellOffset));
                maxColRendered = Math.max(maxColRendered, col);
                colOffset += colWidth + 1;
                if (!this.containsPosition(cellOffset.addCols(colWidth + 2))) {
                    break;
                }
            }
            colOffset = 1;
            rowpos += rowHeight + 1;
        }

        for (int row = 0; row <= maxRow; row++) {
            int rowHeight = tableModel.getMaxHeightForRow(row);
            for (int col = 0; col <= maxCol; col++) {
                int colWidth = tableModel.getMaxWidthForColumn(col);
                IScreenLineSegmentsBuffer cellSegmentsBuffer = tableModel.getSegmentsDataAt(row, col);
                if (cellSegmentsBuffer == null) {
                    throw new NullPointerException("NULL SEGMENTSBUFFER AT (" + row + "/" + col + ")- cannot render!");
                }
                Position cellOffset = new Position.Simple(rowpos, colOffset);
                Logging.info("(" + row + "/" + col + ")mySegments.merge :  " + ElapsedTime
                        .onRunOf(() -> mySegments.merge(copyCellSegments(cellSegmentsBuffer, cellOffset))));
                maxColRendered = Math.max(maxColRendered, col);
                colOffset += colWidth + 1;
                if (!this.containsPosition(cellOffset.addCols(colWidth + 2))) {
                    break;
                }
            }
            rowpos += rowHeight + 1;
            if (maxColRendered >= 0) {
                maxRowRendered = Math.max(maxRowRendered, row);
            }
            maxCellHeight = this.getHeight() - 2 - rowpos;
            if (!this.containsPosition(new Position.Simple(rowpos, 0))) {
                break;
            }
            this.rowsDisplayed++;
            colOffset = 1;
        }

        drawLines(mySegments, maxRowRendered, maxColRendered);

    }

    /**
     * Draw lines for table .. TODO: Need to add options for: - don't draw lnes -
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
        Rectangle.Simple meAsRectangle = new Rectangle.Simple(this);

        Function<Integer, Integer> colVerticalLineOffset = (col) -> this.tableModel.getWidthColumnForRange(0, col) + col + 1;
        Function<Integer, Integer> rowHorizontalLineOffset = (row) -> this.tableModel.getHeightRowForRange(0, row) + row + 3;
        meAsRectangle.setWidth(Math.min(meAsRectangle.getWidth(), colVerticalLineOffset.apply(maxColRendered) + 1));

        Color borderColor = getColor();

        int boxWidth = meAsRectangle.getWidth();
        int boxHeight = meAsRectangle.getHeight();

        Set<Integer> rowsDrawn = new HashSet<Integer>();

        // internal function to draw a line blockChars = (LEFT, HORIZONTAL, INTERSECT AND RIGHT) characters
        BiConsumer<Integer, char[]> drawHorizontalLine = (row, blockChars) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(blockChars[0]);
            // int i = 1;
            for (int c = 0; c <= maxColRendered && sb.length() < (boxWidth - 1); c++) {
                int colWidth = this.tableModel.getMaxWidthForColumn(c);
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

        ElapsedTime timer = new ElapsedTime();

        // Draw top of table
        drawHorizontalLine.accept(0, new char[] { BlockChars.NICE.getTOP_LEFT(), 
                                                  BlockChars.NICE.getHORIZONTAL_LINE(),
                                                  BlockChars.NICE.getHORIZONTAL_DOWN(), 
                                                  BlockChars.NICE.getTOP_RIGHT() });

        Logging.info("Top line  : " + timer);

        timer.reset();
        
        /*  need to draw header line */
        drawHorizontalLine.accept(this.tableModel.maxHeaderHeight + 1,
                new char[] { BlockChars.NICE.getVERTICAL_RIGHT(), 
                             BlockChars.NICE.getHORIZONTAL_LINE(),
                             BlockChars.NICE.getINTERSECT(), 
                             BlockChars.NICE.getVERTICAL_LEFT() });
        
        
        
        for (int r = 0; r < maxRowRendered; r++) {
            drawHorizontalLine.accept(rowHorizontalLineOffset.apply(r),
                    new char[] { BlockChars.NICE.getVERTICAL_RIGHT(), 
                                 BlockChars.NICE.getHORIZONTAL_LINE(),
                                 BlockChars.NICE.getINTERSECT(), 
                                 BlockChars.NICE.getVERTICAL_LEFT() });
        }
        Logging.info("Middle intersecting lines  : " + timer);

        timer.reset();
        drawHorizontalLine.accept(meAsRectangle.getHeight() - 1,
                new char[] { BlockChars.NICE.getBOTTOM_LEFT(), 
                             BlockChars.NICE.getHORIZONTAL_LINE(),
                             BlockChars.NICE.getHORIZONTAL_UP(), 
                             BlockChars.NICE.getBOTTOM_RIGHT() });

        Logging.info("Bottom line  : " + timer);

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
        Logging.info("Verticals   : " + timer);
    }

    public void setColumnAt(int colIdx, TableColumn tableColumn) {
        this.tableModel.setColumnAt(colIdx, tableColumn);
    }

    public void addColumn(TableColumn tableColumn) {
        this.tableModel.addColumn(tableColumn);
    }

    static class TableModelWidthData extends TableModel.Delegate {

        List<IScreenLineSegmentsBuffer> headerData;
        Integer maxHeaderHeight;
        List<List<IScreenLineSegmentsBuffer>> data;
        List<Integer> maxWidths;
        List<Integer> maxHeights;
        List<TableColumnWithDataAndWidths> tableColumns;

        public TableModelWidthData(TableModel delegate) {
            super(delegate);
            headerData = new ArrayList<IScreenLineSegmentsBuffer>();
            data = new ArrayList<List<IScreenLineSegmentsBuffer>>();
            maxWidths = new ArrayList<Integer>();
            maxHeights = new ArrayList<Integer>();
            maxHeaderHeight = Integer.MIN_VALUE;
            this.tableColumns = new ArrayList<TableColumnWithDataAndWidths>();
            //Collections.fill(maxWidths, 0);
            //Collections.fill(maxHeights, 0);
        }

        public void clearData() {
            headerData = new ArrayList<IScreenLineSegmentsBuffer>();
            data = new ArrayList<List<IScreenLineSegmentsBuffer>>();
            maxWidths = new ArrayList<Integer>();
            maxHeights = new ArrayList<Integer>();
            maxHeaderHeight = Integer.MIN_VALUE;
            //Collections.fill(maxWidths, 0);
            //Collections.fill(maxHeights, 0);
            for (TableColumnWithDataAndWidths tc : this.tableColumns) {
                tc.clearCalculatedData();
            }
        }

        public IScreenLineSegmentsBuffer getSegmentsDataAt(int row, int col) {
            if (row >= data.size()) {
                return null;
            }
            List<IScreenLineSegmentsBuffer> theRow = data.get(row);
            int curRowSize = theRow.size();
            if (curRowSize <= col) {
                return null;
            }
            return theRow.get(col);
        }
        
        public IScreenLineSegmentsBuffer getHeaderSegmentsDataAt(int col) {
            int curColSize = headerData.size();
            if (curColSize <= col) {
                return null;
            }
            return headerData.get(col);
        }



        public void addColumn(TableColumn tableColumn) {
            this.tableColumns.add(TableColumnWithDataAndWidths.asTableColumnWithDataAndWidths(tableColumn, this));
        }

        protected TableColumn getTableColumnAt(int colIdx) {
            if (colIdx >= this.tableColumns.size()) {
                this.addColumn(TableColumnImpl.makeDefaultColumnFromTable(colIdx, this));
            }
            return this.tableColumns.get(colIdx);
        }

        public void setColumnAt(int colIdx, TableColumn tableColumn) {
            if (colIdx < 0 || colIdx >= this.getColumnCount()) {
                throw new IllegalArgumentException(
                        "Error in setColumnAt().  colIdx is not in TableModel column range: " + colIdx);
            }
            if (colIdx > this.tableColumns.size()) {
                // add intervening columns
                int tcSize = this.tableColumns.size();
                for (int col = tcSize; col < colIdx; col++) {
                    this.addColumn(TableColumnWithDataAndWidths.asTableColumnWithDataAndWidths(
                            TableColumnImpl.makeDefaultColumnFromTable(col, this), this));
                }
            }
            if (colIdx == this.tableColumns.size()) {
                this.tableColumns.add(TableColumnWithDataAndWidths.asTableColumnWithDataAndWidths(tableColumn, this));
            } else {
                this.tableColumns.set(colIdx,
                        TableColumnWithDataAndWidths.asTableColumnWithDataAndWidths(tableColumn, this));
            }
        }

        public int getMaxWidthForColumn(int col) {
            return maxWidths.get(col);
        }

        public int getMaxHeightForRow(int row) {
            return maxHeights.get(row);
        }

        public int getHeight() {
            return maxHeights.stream().reduce(0, (a, b) -> a + b);
        }

        public int getWidth() {
            return maxWidths.stream().reduce(0, (a, b) -> a + b);
        }

        public int getWidthColumnForRange(int minCol, int maxCol) {
            maxCol = Math.min(maxWidths.size() - 1, maxCol);
            int sum = 0;
            for (int col = minCol; col <= maxCol; col++) {
                int maxForCol = getMaxWidthForColumn(col);
                sum = sum + maxForCol;
            }
            return sum;
        }

        public int getHeightRowForRange(int minRow, int maxRow) {
            maxRow = Math.min(maxHeights.size() - 1, maxRow);
            int sum = 0;
            for (int row = minRow; row <= maxRow; row++) {
                int maxForRow = getMaxHeightForRow(row);
                sum = sum + maxForRow;
            }
            return sum;
        }

        public IScreenLineSegmentsBuffer getOrSetSegmentsDataAt(int row, int col, int desiredHeight, int desiredWidth,
                TableColumnRenderer renderer) {
            IScreenLineSegmentsBuffer segmentsBufferAtRowCol = getSegmentsDataAt(row, col);
            if (segmentsBufferAtRowCol == null || (segmentsBufferAtRowCol.getHeight() != desiredHeight
                    || segmentsBufferAtRowCol.getWidth() != desiredWidth)) {
                Object cellValue = this.getValueAt(row, col);
                TableColumn tc = this.getTableColumnAt(col);
                IScreenLineSegmentsBuffer segmentsBufferForRowCol = renderer.render(tc, cellValue, desiredWidth, desiredHeight, tc.getAlign());
                // making a copy because renderer does not preserve its segments object ( and is not thread safe )
                IScreenLineSegmentsBuffer segmentsCopy = new ScreenLineSegmentsBuffer();
                segmentsCopy
                        .clearAndMerge(ScreenLineSegment.UTIL.makeCollection(segmentsBufferForRowCol.getSegments()));
                setSegmentsDataAt(row, col, segmentsCopy);
                return segmentsBufferForRowCol;
            }
            return segmentsBufferAtRowCol;
        }

        public void setSegmentsDataAt(int row, int col, IScreenLineSegmentsBuffer cellSegmentsBuffer) {
            if (row >= data.size()) {
                addRow(row);
            }
            if (col >= data.get(row).size()) {
                addColAtRow(row, col);
            }
            data.get(row).set(col, cellSegmentsBuffer);
            int width = cellSegmentsBuffer.getWidth();
            int curMaxWidth = maxWidths.get(col);
            if (width > curMaxWidth) {
                maxWidths.set(col, width);
            }
            int height = cellSegmentsBuffer.getHeight();
            int curMaxHeight = maxHeights.get(row);
            if (height > curMaxHeight) {
                maxHeights.set(row, height);
            }
        }

        public IScreenLineSegmentsBuffer getOrSetHeaderSegmentsDataAt(int col, int desiredHeight, int desiredWidth,
                TableColumnRenderer renderer) {
            IScreenLineSegmentsBuffer segmentsBufferAtRowCol = getHeaderSegmentsDataAt(col);
            if (segmentsBufferAtRowCol == null || (segmentsBufferAtRowCol.getHeight() != desiredHeight
                    || segmentsBufferAtRowCol.getWidth() != desiredWidth)) {
                String cellValue = this.getColumnName(col);
                IScreenLineSegmentsBuffer segmentsBufferForRowCol = renderer.renderHeader(this.getTableColumnAt(col), cellValue, desiredWidth, desiredHeight);
                // making a copy because renderer does not preserve its segments object ( and
                // not thread safe )
                IScreenLineSegmentsBuffer segmentsCopy = new ScreenLineSegmentsBuffer();
                segmentsCopy
                        .clearAndMerge(ScreenLineSegment.UTIL.makeCollection(segmentsBufferForRowCol.getSegments()));
                setHeaderSegmentsDataAt(col, segmentsCopy);
                return segmentsBufferForRowCol;
            }
            return segmentsBufferAtRowCol;
        }

        public void setHeaderSegmentsDataAt(int col, IScreenLineSegmentsBuffer cellSegmentsBuffer) {
            if (col >= headerData.size()) {
                addColAtRow(col, headerData);
            }
            headerData.set(col, cellSegmentsBuffer);
            int width = cellSegmentsBuffer.getWidth();
            int curMaxWidth = maxWidths.get(col);
            if (width > curMaxWidth) {
                maxWidths.set(col, width);
            }
            int height = cellSegmentsBuffer.getHeight();
            int curMaxHeight = maxHeaderHeight;
            if (height > curMaxHeight) {
                maxHeaderHeight = height;
            }
        }
        
        
        // ColumnsForOutput columnsForOutput
        public static int calcTableWidthFromColumns(ColumnsForOutput columnsForOutput) {
            return calcTableWidthFromColumns(columnsForOutput.visibleColumnDataList);
        }
        
        public static int calcTableWidthFromColumns(List<? extends TableColumn> tableColumns) {
            int columnsSizesTotal = 0;
            for (TableColumn item : tableColumns) {
                columnsSizesTotal += item.getPreferredWidth();
            }
            int tableWidth = columnsSizesTotal + tableColumns.size() - 1; // colData.size() - 1 accounts for 1 char
                                                                          // spaces between columns
            return tableWidth;
        }

        private void addColAtRow(int row, int col) {
            List<IScreenLineSegmentsBuffer> theRow = data.get(row);
            addColAtRow(col, theRow);
        }

        private void addColAtRow(int col, List<IScreenLineSegmentsBuffer> theRow) {
            int curRowSize = theRow.size();
            if (curRowSize > col) {
                return;
            }

            for (int i = curRowSize; i <= col; i++) {
                theRow.add(null);
                if (theRow.size() > maxWidths.size()) {
                    maxWidths.add(Integer.MIN_VALUE);
                }
            }
        }


        private void addRow(int row) {
            int rowCount = data.size();
            if (rowCount > row) {
                return;
            }
            for (int i = rowCount; i <= row; i++) {
                data.add(new ArrayList<IScreenLineSegmentsBuffer>());
                if (data.size() > maxHeights.size()) {
                    maxHeights.add(Integer.MIN_VALUE);
                }
            }
        }

        Dimension asDimension() {
            return new Dimension.Simple(getHeight(), getWidth());
        }

    }

    private static class ColumnsForOutput {

        List<TableColumnWithDataAndWidths> visibleColumnDataList;
        List<TableColumnWithDataAndWidths> hiddenColumns;

        ColumnsForOutput(List<TableColumnWithDataAndWidths> visibleColumnDataList,
                List<TableColumnWithDataAndWidths> hiddenColumns) {
            this.visibleColumnDataList = visibleColumnDataList;
            this.hiddenColumns = hiddenColumns;
        }

        ColumnsForOutput(List<TableColumnWithDataAndWidths> visibleColumnDataList) {
            this(visibleColumnDataList, null);
        }

        public boolean hasHiddenColumns() {
            return this.hiddenColumns != null && hiddenColumns.size() > 0;
        }

        int getVisibleColumnCount() {
            return visibleColumnDataList == null ? 0 : visibleColumnDataList.size();
        }

        public ColumnsForOutput expandColumnsToFitTableWidth(int currentTableDataWidth, int tableComponentInnerWidth) {

            int visibleColumnCount = getVisibleColumnCount(); 
            if (visibleColumnCount == 0 || currentTableDataWidth >= tableComponentInnerWidth) {
                return this; // do nothing - but this should not happen based on when this is called
            }

            float[] columnRatios = new float[visibleColumnCount];

            int allDataWidthMinusSpacers = currentTableDataWidth - (visibleColumnCount - 1);
            int allMaxDataWidthMinusSpacers = tableComponentInnerWidth - (visibleColumnCount - 1);

            for (int c = 0; c < visibleColumnCount; c++) {
                float cRatio = ((float)this.visibleColumnDataList.get(c).getPreferredWidth()) / ((float)allDataWidthMinusSpacers);
                columnRatios[c] = cRatio;
            }

            for (int c = (visibleColumnCount-1); c>=0; c--) {
                TableColumnWithDataAndWidths tc = this.visibleColumnDataList.get(c);
                int newPrefrerredColWidth = (int) Math.ceil(((float)allMaxDataWidthMinusSpacers) * columnRatios[c]);
                int currentPreferredWidth = tc.getPreferredWidth();
                if (newPrefrerredColWidth <= currentPreferredWidth) {
                    continue;
                }
                if (allMaxDataWidthMinusSpacers < ( (allDataWidthMinusSpacers - currentPreferredWidth) + newPrefrerredColWidth ) ) {
                    continue;
                }
                allDataWidthMinusSpacers = allDataWidthMinusSpacers - currentPreferredWidth + newPrefrerredColWidth;
                tc.setPreferredWidth(newPrefrerredColWidth);
            }

            while (allDataWidthMinusSpacers < allMaxDataWidthMinusSpacers) {
                for (int c = (visibleColumnCount-1); c>=0 && allDataWidthMinusSpacers < allMaxDataWidthMinusSpacers; c--) {
                    TableColumnWithDataAndWidths tc = this.visibleColumnDataList.get(c);
                    tc.setPreferredWidth(  tc.getPreferredWidth() + 1 );
                    allDataWidthMinusSpacers++;
                }
            }

            return this;
        }
    }

    /**
     * Builds a list of TableColumnWithDataAndWidths objects taking into account desired
     * table component width to determine what columns will be displayed.
     */
    static interface TableColumnDataBuilder {
        /**
         * Returns a list of TableColumnWithDataAndWidths objects used to dictate what columns
         * will be displayed in a table
         */
        ColumnsForOutput getColumnData(TableModelWidthData tableModel);
    }

    static class TableColumnDataBuilderDropColumnsWhenNeeded implements TableColumnDataBuilder {

        TableComponent tableComponent;

        public TableColumnDataBuilderDropColumnsWhenNeeded(TableComponent tableComponent) {
            super();
            this.tableComponent = tableComponent;
        }

        @Override
        public ColumnsForOutput getColumnData(TableModelWidthData tableModel) {


            List<TableColumnWithDataAndWidths> columnData = new ArrayList<TableColumnWithDataAndWidths>();
            for (int colIdx = 0; colIdx < tableModel.getColumnCount(); colIdx++) {
                columnData.add( (TableColumnWithDataAndWidths) tableModel.getTableColumnAt(colIdx) );
            }

            int tableDataWidth = calcTableWidthFromColumns(columnData);

            int componentWidth = tableComponent.getWidth() - 2; // account for border

            if (tableDataWidth <= componentWidth) {
                return new ColumnsForOutput(columnData).expandColumnsToFitTableWidth(tableDataWidth, componentWidth); // table fits in existing terminal
            }

            // look for table columns that can be shrunk via word wrapping
            List<TableColumnWithDataAndWidths> squeezableCandidates = new ArrayList<TableColumnWithDataAndWidths>(
                    columnData.size());
            for (TableColumnWithDataAndWidths col : columnData) {
                if (col.getMaxMinWidthBetweenCellDataAndHeader() + 4 < col.getPreferredWidth()
                        || col.isWordWrappable()) {
                    squeezableCandidates.add(col);
                }
            }
            // sort by columns that can be squeezed the most
            Collections.sort(squeezableCandidates, new Comparator<TableColumnWithDataAndWidths>() {
                @Override
                public int compare(TableColumnWithDataAndWidths c1, TableColumnWithDataAndWidths c2) {
                    int delta1 = c1.getMaxMinWidthBetweenCellDataAndHeader() + 4 - c1.getPreferredWidth();
                    int delta2 = c2.getMaxMinWidthBetweenCellDataAndHeader() + 4 - c2.getPreferredWidth();
                    return Integer.compare(delta1, delta2);
                }
            });

            // Now squeeze the columns
            int lastTableWidth = Integer.MAX_VALUE;
            int currentTableDataWidth = tableDataWidth;
            while (lastTableWidth != currentTableDataWidth) // we terminate loop when we can't squeeze anymore
            {
                lastTableWidth = currentTableDataWidth;
                for (TableColumnWithDataAndWidths columnToSqueeze : squeezableCandidates) {
                    columnToSqueeze.squeezeColSizeByOneIfPossible();
                    currentTableDataWidth = calcTableWidthFromColumns(columnData);
                    if (currentTableDataWidth <= componentWidth) {
                        // we were able to squeeze columns fit inside table!
                        // yippee.. yahoo.. miami beach!
                        return new ColumnsForOutput(columnData);
                    }
                }
            }

            // At this point table does not fit in terminal, let's try to truncate columns
            // if allowed
            // let's try to find table columns that we can truncate
            List<TableColumnWithDataAndWidths> truncatebleCandidates = new ArrayList<TableColumnWithDataAndWidths>(
                    columnData.size());
            for (TableColumnWithDataAndWidths col : columnData) {
                if (col.isTruncationAllowed()) {
                    truncatebleCandidates.add(col);
                }
            }

            if (truncatebleCandidates.size() > 0) {
                // truncate from right to left
                Collections.reverse(truncatebleCandidates);
                // truncateColSizeByOneIfPossible()
                // Now squeeze the columns
                lastTableWidth = Integer.MAX_VALUE;
                currentTableDataWidth = calcTableWidthFromColumns(columnData);
                while (lastTableWidth != currentTableDataWidth) // we terminate loop when we can't truncate anymore
                {
                    lastTableWidth = currentTableDataWidth;
                    int desiredCharacterCountToTruncate = currentTableDataWidth - componentWidth;
                    for (TableColumnWithDataAndWidths columnToTruncate : truncatebleCandidates) {
                        // TODO:: when does the actual truncate occur?.. Right after the wrapping code?
                        // -- look below
                        columnToTruncate.truncateIfPossible(desiredCharacterCountToTruncate);
                        currentTableDataWidth = calcTableWidthFromColumns(columnData);
                        if (currentTableDataWidth <= componentWidth) {
                            // we were able to truncate columns enough that
                            // all columns fit onto the terminal - well all right!
                            return new ColumnsForOutput(columnData);
                        }
                    }
                }
            }

            List<TableColumnWithDataAndWidths> removalCandidates = new ArrayList<TableColumnWithDataAndWidths>();
            for (TableColumnWithDataAndWidths col : columnData) {
                if (col.isColumnDroppable()) {
                    removalCandidates.add(col);
                }
            }

            if (removalCandidates.size() < 1) {
                return new ColumnsForOutput(columnData); // no candidate columns are droppable
                                                         // return original columnData
            }

            Collections.sort(removalCandidates, TableColumn.SORT_IN_ORDER_COLUMNS_SHOULD_BE_DROPPED);

            List<TableColumnWithDataAndWidths> trimmedcolumnData = new ArrayList<TableColumnWithDataAndWidths>(columnData);
            List<TableColumnWithDataAndWidths> hiddenColumns = new ArrayList<TableColumnWithDataAndWidths>();

            // Now remove columns
            for (TableColumnWithDataAndWidths columnToRemove : removalCandidates)
            {
                trimmedcolumnData.remove( columnToRemove );
                hiddenColumns.add( columnToRemove);
                if ( calcTableWidthFromColumns(trimmedcolumnData) <= componentWidth )
                {
                    return new ColumnsForOutput(trimmedcolumnData, hiddenColumns);
                }
            }
            return new ColumnsForOutput(trimmedcolumnData, hiddenColumns);
            
        }

        public static int calcTableWidthFromColumns(List<? extends TableColumn> colData) {
            int columnsSizesTotal = 0;
            for (TableColumn item : colData) {
                columnsSizesTotal += item.getPreferredWidth();
            }
            int tableWidth = columnsSizesTotal + colData.size() - 1; // colData.size() - 1 accounts for 1 char spaces
                                                                     // between columns
            return tableWidth;
        }

    }

}
