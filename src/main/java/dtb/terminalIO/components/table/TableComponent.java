package dtb.terminalIO.components.table;

import java.util.ArrayList;
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
    List<TableColumn> tableColumns;
    boolean hasBorder = true;
    
    public TableComponent(Color color, Dimension d, TableModel tableModel) {
        super(color, d);
        this.tableModel = new TableModelWidthData(tableModel);
        this.tableColumns = new ArrayList<TableColumn>();
    }

    private TreeSet<ScreenLineSegment> copyCellSegments(IScreenLineSegmentsBuffer cellSegmentsBuffer, Position offset) {
        
        ElapsedTime elapsedTime = new ElapsedTime();
        
        TreeSet<ScreenLineSegment> destination =   ScreenLineSegment.UTIL.makeCollection(null);
        
        cellSegmentsBuffer.getSegments().stream()
                                        .map(s->s.fromOffset(offset))
                                        .forEach(s->destination.add(s));

        Logging.info("cell copied "+ elapsedTime + "   count " + destination.size());
        
        return destination;
    }

    public void resize(Dimension d) {
        if (d != null && d.isSameDimension(this)) {
            return;
        }
        super.resize(d);
        // TODO:  Might want to clear tableModel segments data 
        //        - need to assess - because it does have some smarts - and thus might not be necessary - we will see 
    }
    

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        Logging.setEnabled(false);
        Logging.info("Table-draw start--------------------");
        //tableModel.clearData();
       Logging.info("Table-draw elapsed : "  +  ElapsedTime.onRunOf(()->__draw(color, mySegments)));
    }

    protected void __draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        long totalRenderTime = 0;
        ElapsedTime renderCellTime = new ElapsedTime();
        // TODO Auto-generated method stub
        this.clear();
        int rowCount = tableModel.getRowCount();
        int colCount = tableModel.getColumnCount();
        int rowpos = 1;
        int maxCellHeight = this.getHeight() -2 ; // TODO:  need to adjust for header height ( later )
        int maxCol = Integer.MAX_VALUE;
        int mmCol = -1;
        int maxRow = -1;
ElapsedTime loopTime = new ElapsedTime();
// FIRST LOOP -- GRAB THE DATA
        for (int row = 0;row < rowCount; row++) {
            int maxRowHeight = -1;
            int colOffset = 1;
            for (int col = 0; col < colCount; col++) {
                TableColumn tc = this.tableColumns.get(col);
                int cellHeight = maxCellHeight;
                int preferredHeight = tc.getPreferredHeight();
                if (preferredHeight > 0) {
                    cellHeight = Math.min(preferredHeight, maxCellHeight);
                }
                TableColumnRenderer renderer = tc.getRenderer();
                int cellWidth = tc.getPreferredWidth();
                if (this.getWidth() <  (cellWidth + colOffset + 1) ) {
                    cellWidth = (cellWidth + colOffset) - this.getWidth();
                    if (cellWidth < 1) {
                        break;
                    }
                }
Logging.info("("+row+"/"+col+")cell START :  ");
renderCellTime = new ElapsedTime();
                IScreenLineSegmentsBuffer cellSegmentsBuffer =   tableModel.getOrSetSegmentsDataAt(row, col, cellHeight, cellWidth, renderer);
Logging.info("("+row+"/"+col+")cell-renderXXX  :  "+renderCellTime);
totalRenderTime+= renderCellTime.elapsed();
ElapsedTime dimCalc = new ElapsedTime();
                int segmentHeight = cellSegmentsBuffer.getHeight();
                maxRowHeight = Math.max(segmentHeight, maxRowHeight);
                int curWidth = Math.max(cellSegmentsBuffer.getWidth(),cellWidth);
                Position cellOffset = new Position.Simple(rowpos, colOffset); 
Logging.info("dimCalc = " + dimCalc);
//                Logging.info("("+row+"/"+col+")mySegments.merge :  "+ElapsedTime.onRunOf(()->mySegments.merge(copyCellSegments(cellSegmentsBuffer, cellOffset))));

                //mySegments.merge(copyCellSegments(cellSegmentsBuffer, cellOffset));
                colOffset += curWidth;
                if ( ! this.containsPosition(cellOffset.addCols(curWidth))) {
                    break;
                }
                //mySegments.add(new SegmentImpl(Colors.FGBGColors.NO_COLOR_SPECIFIED, new Position.Simple(row,col*8), -1, CleanString.UTIL.clean(row+"x"+col)));
                mmCol = col;
            }
            maxCol = Math.min(maxCol, mmCol);
            rowpos += maxRowHeight + 1;
            maxCellHeight = this.getHeight() - 2 - rowpos;
            if ( ! this.containsPosition( new Position.Simple(rowpos,0) )) {
                break;
            }
            maxRow = Math.max(maxRow, row);
        }
        maxCol = Math.min(maxCol, colCount-1);
        // getWidthForRange(int minCol, int maxCol)
        int tableComponentWidth = this.getWidth();
        
        Function<Integer, Integer> colToWidthNeeded = (col)->  this.tableModel.getWidthColumnForRange(0,col) + col+2;
        
        if (tableComponentWidth < colToWidthNeeded.apply(maxCol)) {
            // does not fit -- so let's squeeze last maxcol -- later make this flexible as to what gets squeezed
            int offset = colToWidthNeeded.apply(maxCol-1); // this.tableModel.getWidthColumnForRange(0,maxCol-1)+(maxCol);
            while (maxCol > 0 && offset>0 && offset > (tableComponentWidth -1)) {
                maxCol--;
                offset = maxCol == 0 ? 0 : colToWidthNeeded.apply(maxCol-1);
            } 
            TableColumn tc = this.tableColumns.get(maxCol);
            TableColumnRenderer renderer = tc.getRenderer();
            int cellWidth = tc.getPreferredWidth();
            if (tableComponentWidth < (cellWidth + offset + 1)) {
                cellWidth = tableComponentWidth - (offset + 1);
                if (cellWidth < 1) {
                    maxCol --;
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
        maxCellHeight = this.getHeight() -2 ; // TODO:  need to adjust for header height ( later )
        int maxColRendered = -1;
        int maxRowRendered = -1;
        for (int row = 0;row <= maxRow; row++) {
            int rowHeight = tableModel.getMaxHeightForRow(row);
            for (int col = 0; col <= maxCol; col++) {
                int colWidth = tableModel.getMaxWidthForColumn(col);
                IScreenLineSegmentsBuffer cellSegmentsBuffer = tableModel.getSegmentsDataAt(row, col);
                if (cellSegmentsBuffer == null) {
                    throw new NullPointerException("NULL SEGMENTSBUFFER AT ("+row+"/"+col+")- cannot render!");
                }
                Position cellOffset = new Position.Simple(rowpos, colOffset);
                Logging.info("("+row+"/"+col+")mySegments.merge :  "+ElapsedTime.onRunOf(()->mySegments.merge(copyCellSegments(cellSegmentsBuffer, cellOffset))));
                maxColRendered = Math.max(maxColRendered, col); 
                colOffset += colWidth + 1;
                if ( ! this.containsPosition(cellOffset.addCols(colWidth + 2))) {
                    break;
                }
            }
            rowpos += rowHeight + 1;
            if (maxColRendered>=0) {
                maxRowRendered = Math.max(maxRowRendered, row);
            }
            maxCellHeight = this.getHeight() - 2 - rowpos;
            if ( ! this.containsPosition( new Position.Simple(rowpos,0) )) {
                break;
            }
            colOffset = 1;
        }

        // LINE DRAWING -- SLOW!!!!
        ElapsedTime lineDrawingTime = new ElapsedTime();
        loopTime.elapsed();
        Logging.setEnabled(true);

        drawLines(mySegments, maxRowRendered, maxColRendered);
        
        /*
        Rectangle.Simple meAsRectangle = new Rectangle.Simple(this);


        Function<Integer, Integer> colVerticalLineOffset = (col)->  this.tableModel.getWidthColumnForRange(0,col) + col+1;
        Function<Integer, Integer> rowHorizontalLineOffset = (row)->  this.tableModel.getHeightRowForRange(0, row) + row + 1;

        meAsRectangle.setWidth( Math.min(meAsRectangle.getWidth(), colVerticalLineOffset.apply(maxColRendered)));

        BlockChars.UTIL.drawBox(mySegments, getColor(), BlockChars.NICE, meAsRectangle);

        for (int col = 0; col < maxColRendered; col++) {
            BlockChars.UTIL.drawVerticalLine(mySegments, getColor(), BlockChars.NICE, new Position.Simple(0, colVerticalLineOffset.apply(col) ),meAsRectangle.getHeight());
        }

        for (int row = 0; row < maxRowRendered; row++) {
            BlockChars.UTIL.drawHorizontalLine(mySegments, getColor(), BlockChars.NICE, new Position.Simple(rowHorizontalLineOffset.apply(row), 0),meAsRectangle.getWidth());
        }
*/



Logging.info("Total-cell-renderXXX  : " + totalRenderTime+ "    Loop time: " + loopTime + "  Line draw time: " + lineDrawingTime);
    }
    /**
     * Draw lines for table .. 
     * TODO:  Need to add options for:
     *      - don't draw lnes
     *      - draw only horizontal lines
     *      - draw only vertical lines
     *      - draw only lines for header
     *      - draw only underlines for header
     *      
     *      TODO:  I tried using my BlockChars.UTIL.drawHorizontalLine(), BlockChars.UTIL.drawVerticalLine(), and BlockChars.UTIL.drawBox().
     *             Although very convenient, they are so slow as to be unusable
     *             
     *      So instead adding the line segments manually - which is faster ( but actually not fast enough )
     *
     * @param mySegments
     * @param maxRowRendered
     * @param maxColRendered
     */
    private void drawLines(IScreenLineSegmentsBuffer mySegments, int maxRowRendered, int maxColRendered) {
        Rectangle.Simple meAsRectangle = new Rectangle.Simple(this);
        
        Function<Integer, Integer> colVerticalLineOffset = (col)->  this.tableModel.getWidthColumnForRange(0,col) + col+1;
        Function<Integer, Integer> rowHorizontalLineOffset = (row)->  this.tableModel.getHeightRowForRange(0, row) + row + 1;
        meAsRectangle.setWidth( Math.min(meAsRectangle.getWidth(), colVerticalLineOffset.apply(maxColRendered)+1));

        Color borderColor = getColor();

        int boxWidth = meAsRectangle.getWidth();
        int boxHeight = meAsRectangle.getHeight();

        Set<Integer> rowsDrawn = new HashSet<Integer>();

        // internal function to draw a line blockChars = (LEFT, HORIZONTAL, INTERSECT AND RIGHT) characters
        BiConsumer<Integer, char[]> drawHorizontalLine = (row, blockChars) -> {
            StringBuilder sb = new StringBuilder();
            sb.append(blockChars[0]);
            //int i = 1;
            for (int c = 0; c <= maxColRendered && sb.length() < (boxWidth -1) ; c++) {
                int colWidth = this.tableModel.getMaxWidthForColumn(c);
                for (int j = 0; j < colWidth && sb.length()<(boxWidth -1); j++) {
                    sb.append(blockChars[1]);
                }
                if (c != maxColRendered && sb.length() < (boxWidth -1)) {
                    sb.append(blockChars[2]);
                } else {
                    sb.append(blockChars[3]);
                }
            }
            mySegments.add( new SegmentImpl(borderColor, new Position.Simple(row, 0), sb.toString()) );
            rowsDrawn.add(row);
        }; 

        ElapsedTime timer = new ElapsedTime();

        drawHorizontalLine.accept(0, new char[] {BlockChars.NICE.getTOP_LEFT(),
                                                 BlockChars.NICE.getHORIZONTAL_LINE(), 
                                                 BlockChars.NICE.getHORIZONTAL_DOWN(),
                                                 BlockChars.NICE.getTOP_RIGHT()}); 

        Logging.info("Top line  : " + timer);

        timer.reset();
        for (int r = 0; r < maxRowRendered; r++) {
            drawHorizontalLine.accept(rowHorizontalLineOffset.apply(r), new char[] {BlockChars.NICE.getVERTICAL_RIGHT(),
                    BlockChars.NICE.getHORIZONTAL_LINE(), 
                    BlockChars.NICE.getINTERSECT(),
                    BlockChars.NICE.getVERTICAL_LEFT()}); 
        }
        Logging.info("Middle intersecting lines  : " + timer);

        timer.reset();
        drawHorizontalLine.accept(meAsRectangle.getHeight()-1, new char[] {BlockChars.NICE.getBOTTOM_LEFT(),
                BlockChars.NICE.getHORIZONTAL_LINE(), 
                BlockChars.NICE.getHORIZONTAL_UP(),
                BlockChars.NICE.getBOTTOM_RIGHT()}); 

        Logging.info("Bottom line  : " + timer);

        timer.reset();
        String verticalLineCharAsStr = BlockChars.NICE.getVERTICAL_LINE()+"";
        for (int row = 1; row < boxHeight; row++) {
            if (rowsDrawn.contains(row)) {
                continue;
            }
            mySegments.add( new SegmentImpl(borderColor, new Position.Simple(row, 0), verticalLineCharAsStr) );

            for (int c=0; c < maxColRendered; c++) {
                int col = colVerticalLineOffset.apply(c);
                mySegments.add( new SegmentImpl(borderColor, new Position.Simple(row, col), verticalLineCharAsStr) );
            }
            mySegments.add( new SegmentImpl(borderColor, new Position.Simple(row, boxWidth-1), verticalLineCharAsStr) );

        }
        Logging.info("Verticals   : " + timer);

    }

    public void addColumn(TableColumn tableColumn) {
        this.tableColumns.add(tableColumn);
    }

    static class TableModelWidthData extends TableModel.Delegate {

        List<List<IScreenLineSegmentsBuffer>> data;
        List<Integer> maxWidths;
        List<Integer> maxHeights;

        public TableModelWidthData(TableModel delegate) {
            super(delegate);
            data = new ArrayList<List<IScreenLineSegmentsBuffer>>();
            maxWidths = new ArrayList<Integer>();
            maxHeights = new ArrayList<Integer>();
        }

        public void clearData() {
            data = new ArrayList<List<IScreenLineSegmentsBuffer>>();
            maxWidths = new ArrayList<Integer>();
            maxHeights = new ArrayList<Integer>();
        }

        public IScreenLineSegmentsBuffer getSegmentsDataAt(int row, int col) {
            if (row >= data.size() ) {
                return null;
            }
            List<IScreenLineSegmentsBuffer> theRow = data.get(row);
            int curRowSize = theRow.size();
            if (curRowSize<=col) {
                return null;
            }
            return theRow.get(col);
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
            maxCol = Math.min(maxWidths.size()-1, maxCol);
            int sum = 0;
            for (int col = minCol; col <= maxCol; col++ ) {
                int maxForCol = getMaxWidthForColumn(col);
                sum = sum + maxForCol;
            }
            return sum;
        }

        public int getHeightRowForRange(int minRow, int maxRow) {
            maxRow = Math.min(maxHeights.size()-1, maxRow);
            int sum = 0;
            for (int row = minRow; row <= maxRow; row++ ) {
                int maxForRow = getMaxHeightForRow(row);
                sum = sum + maxForRow;
            }
            return sum;
        }

        public IScreenLineSegmentsBuffer getOrSetSegmentsDataAt(int row, int col, int desiredHeight, int desiredWidth, TableColumnRenderer renderer) {
            IScreenLineSegmentsBuffer segmentsBufferAtRowCol = getSegmentsDataAt(row, col);
            if (segmentsBufferAtRowCol == null || (segmentsBufferAtRowCol.getHeight() != desiredHeight || segmentsBufferAtRowCol.getWidth()!=desiredWidth)) {
                Object cellValue = this.getValueAt(row, col);
                IScreenLineSegmentsBuffer segmentsBufferForRowCol = renderer.render(cellValue, desiredWidth, desiredHeight);
                // making a copy because renderer does not preserve its segments object ( and not thread safe )
                IScreenLineSegmentsBuffer segmentsCopy = new ScreenLineSegmentsBuffer();
                segmentsCopy.clearAndMerge(ScreenLineSegment.UTIL.makeCollection(segmentsBufferForRowCol.getSegments()));
                setSegmentsDataAt(row, col, segmentsCopy);
                return segmentsBufferForRowCol;
            }
            return segmentsBufferAtRowCol;
        }

        public void setSegmentsDataAt(int row, int col, IScreenLineSegmentsBuffer cellSegmentsBuffer) {
            if (row >= data.size() ) {
                addRow(row);
            }
            if (col >= data.get(row).size()) {
                addColAtRow(row, col);
            }
            data.get(row).set(col, cellSegmentsBuffer);
            int width = cellSegmentsBuffer.getWidth();
            int curMaxWidth = maxWidths.get(col);
            if (width>curMaxWidth) {
                maxWidths.set(col, width);
            }
            int height = cellSegmentsBuffer.getHeight();
            int curMaxHeight = maxHeights.get(row);
            if (height>curMaxHeight) {
                maxHeights.set(row, height);
            }

        }

        private void addColAtRow(int row, int col) {
            List<IScreenLineSegmentsBuffer> theRow = data.get(row);
            int curRowSize = theRow.size();
            if (curRowSize>col) {
                return;
            }
            
            for (int i = curRowSize; i<=col; i++ ) {
                theRow.add( null );
                if (theRow.size()> maxWidths.size()) {
                    maxWidths.add(Integer.MIN_VALUE);
                }
            }
            
        }

        private void addRow(int row) {
            int rowCount = data.size();
            if (rowCount>row) {
                return;
            }
            for (int i = rowCount; i<=row; i++ ) {
                data.add( new ArrayList<IScreenLineSegmentsBuffer>() );
                if (data.size()> maxHeights.size()) {
                    maxHeights.add(Integer.MIN_VALUE);
                }
                
            }
        }

        Dimension asDimension() {
            return new Dimension.Simple(getHeight(), getWidth());
        }

    }

}
