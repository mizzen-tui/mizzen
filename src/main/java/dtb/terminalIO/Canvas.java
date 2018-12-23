package dtb.terminalIO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import dtb.terminalIO.ControlCode.BgColor;
import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.ControlCode.FgColor;
import dtb.terminalIO.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.ScreenLineSegment.UTIL.SegmentsFromStringResult;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

public class Canvas implements Dimension, IScreenLineSegmentsBuffer.Provider {

    private CanvasBackend    backend;
    private Color            color;                                // current foregound/background color
    private CursorPos        cursorPos        = new CursorPos();

    private int              height;
    private int              width;
    private int              sc_current_id    = 0;

    private char             emptyChar        = '░';
    private FGBGColors       emptyColor       = FGBGColors.DEFAULT;
    private static final int EMPTY_SEGMENT_ID = -67233;

    public void setEmptyChar(char emptyChar) {
        this.emptyChar = emptyChar;
    }

    public void setEmptyColors(FGBGColors emptyColor) {
        this.emptyColor = emptyColor;
    }

    Rectangle meAsARectangle = null;

    public Rectangle asRectangle() {
        if (meAsARectangle == null) {
            meAsARectangle = new Rectangle.Simple(Position.ZERO, this);
        }
        return meAsARectangle;
    }

    public void setEmptyAttributes(FGBGColors emptyColor, char emptyChar) {
        setEmptyColors(emptyColor);
        setEmptyChar(emptyChar);
    }

    public static interface CanvasBackend {
        void writeToBackend(ScreenLineSegment sc);

        void signal_eol();

        void refresh();

        void cr();
    }
    
    public static class NullCanvasBackEnd implements CanvasBackend {

        @Override
        public void writeToBackend(ScreenLineSegment sc) {
        }

        @Override
        public void signal_eol() {
        }

        @Override
        public void refresh() {
        }

        @Override
        public void cr() {
        }
    }
    

    IScreenLineSegmentsBuffer segmentsBuffer = new ScreenLineSegmentsBuffer();

    public Canvas(CanvasBackend backend, FGBGColors color) {
        this.backend = backend;
        this.color = color;
    }

    public Canvas(CanvasBackend backend) {
        this(backend, FGBGColors.DEFAULT);
    }

    public Canvas(TerminalScreen screen, FGBGColors color) {
        this(new ScreenCanvasBackend(screen), color);
    }

    public Canvas(TerminalScreen screen) {
        this(new ScreenCanvasBackend(screen));
    }

    public Canvas setSize(int height, int width) {
        synchronized(this) {
           this.height = height;
           this.width = width;
           meAsARectangle = null;
        }
        // TODO - if already rendered -- should reflect by changing buffer and by
        // re-rendering on backend
        return this;
    }

    public IScreenLineSegmentsBuffer getSegmentsBuffer() {
        return this.segmentsBuffer;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    private void segmentsClear() {
        segmentsBuffer.clear();
        sc_current_id = 1;
    }

    public Canvas clearCanvas() {
        segmentsClear();
        return this;
    }

    public Canvas moveToCursor(Position pos) {
        return moveToCursor(pos.getRow(), pos.getCol());
    }

    public Canvas moveToCursor(int row, int col) {
        if (row < 0)
            row = 0;
        if (row >= height)
            row = height - 1;
        if (col < 0)
            col = 0;
        if (col >= width)
            col = width - 1;

        this.cursorPos = new CursorPos(row, col);
        return this;
    }

    public String getTextAt(int row, int col) {
        return segmentsBuffer.getTextAt(row, col);
    }

    public String getCharAt(int row, int col) {
        return segmentsBuffer.getCharAt(row, col);
    }

    public Canvas t(int row, int col, String s) {
        moveToCursor(row, col);
        t(s);
        return this;
    }

    public Canvas t(String s) {

        SegmentsFromStringResult result = ScreenLineSegment.UTIL.textSegmentsFromString(s, cursorPos.getRow(), cursorPos.getCol(), this.asRectangle(),
                (p, txt) -> createScreenObject(color, p, txt));
        if (result.size() == 0) {
            return this;
        }
        segmentsBuffer.merge(result.ts);
        moveToCursor(result.pos.offsetAdd(0, 1));
        return this;
    }

    public Canvas addTextHandleColors(String s) {
        boolean hasColorCodes = ControlCode.hasColorCodes(s);
        if (! hasColorCodes) {
            return t(s);
        }
        
        SegmentsFromStringResult result = ScreenLineSegment.UTIL.textSegmentsFromStringWithEmbeddedColor(s, cursorPos.getRow(), cursorPos.getCol(), this.asRectangle(),
                (strColor, p, txt) -> new SegmentImpl( strColor != null ? strColor : color, p, -1, txt));

        if (result.size() == 0) {
            return this;
        }
        segmentsBuffer.merge(result.ts);
        moveToCursor(result.pos.offsetAdd(0, 1));
        return this;
    }

    public void writeToScreen(int rowOffset, int colOffset, ScreenLineSegment sc) {

        int newCol = sc.getCol() + colOffset;
        if (newCol >= this.getWidth())
            return;
        int newRow = sc.getRow() + rowOffset;
        if (newRow >= this.getHeight())
            return;
        int len = sc.getLength();
        String txt;
        if (newCol + len <= this.getWidth()) {
            txt = sc.getText();
        } else {
            int newLen = this.getWidth() - newCol;
            txt = sc.getText().substring(newLen);
        }
        Color curColor = this.getTextColor();
        if (!sc.getColors().equals(curColor)) {
            this.setTextColor(sc.getColors().mergeWith(curColor));
        }
        this.t(newRow, newCol, txt);
        this.setTextColor(curColor);
    }

    public void addComponentSegments(int rowOffset, int colOffset, Component<?> component) {
        addComponentSegments(new Position.Simple(rowOffset, colOffset), component);
    }

    public void addComponentSegments(Position offset, Component<?> component) {
        Rectangle bounds = this.asRectangle();

        ScreenLineSegment.UTIL.addSegmentsWithOffsetBounded(offset, bounds, 
                                                           this.getSegmentsBuffer().getSegments(),
                                                           component.getSegmentsBuffer().getSegments());
    }

    public void addComponentSegments(Component<?> component) {
        addComponentSegments(Position.ZERO, component);
    }
    
    public int segmentsSize() {
        return getSegmentsBuffer().getSegments().size();
    }

    public Canvas refresh() {

        if (segmentsBuffer.isEmpty()) {
            outputBlankLines(0, this.height);
            backend.refresh();
            return this;
        }

        List<ScreenLineSegment> mergedRow = new ArrayList<ScreenLineSegment>();
        Iterator<ScreenLineSegment> sortedI = segmentsBuffer.iterator();
        ScreenLineSegment prev_sc = sortedI.next();
        ScreenLineSegment scs;

        int lastRow = 0;
        while (sortedI.hasNext()) {
            scs = sortedI.next();
            if (prev_sc.getRow() != scs.getRow()) {
                outputBlankLines(lastRow, prev_sc.getRow() - 1);
                mergedRow.add(prev_sc);
                outputRow(mergedRow);
                lastRow = prev_sc.getRow() + 1;
                mergedRow.clear();
                prev_sc = scs;
            } else { // on same row
                mergedRow.add(prev_sc);
                prev_sc = scs;
            }
        }
        if (prev_sc != null) {
            mergedRow.add(prev_sc);
            outputBlankLines(lastRow, prev_sc.getRow() - 1);
            outputRow(mergedRow);
            lastRow = prev_sc.getRow() + 1;
        }
        outputBlankLines(lastRow, this.height - 1);
        backend.refresh();
        return this;
    }

    private ScreenLineSegment blankSegment(int row, int col, int len) {
        return ScreenLineSegment.UTIL.blankScreenObject(emptyColor, emptyChar, row, col, EMPTY_SEGMENT_ID, len);
    }

    private void outputBlankLines(int lowRow, int highRow) {
        for (int r = lowRow; r <= highRow; r++) {
            outputRow(Collections.singletonList(blankSegment(r, 0, this.width)));
        }
    }

    private void outputRow(List<ScreenLineSegment> scsOnSameRow) {
        int rowWidth = ScreenLineSegment.UTIL.totalWidth(scsOnSameRow);
        if (rowWidth > this.width)
            throw new IllegalStateException(String.format("big width! %d (rowWidth)> %d (canvas width)",rowWidth,this.width));
        int row = -1;

        int lastRightMostPlus1 = 0;

        for (ScreenLineSegment scs : scsOnSameRow) {
            if (row == -1)
                row = scs.getRow();
            if (lastRightMostPlus1 < scs.getCol()) {
                backend.writeToBackend(blankSegment(row, lastRightMostPlus1, scs.getCol() - lastRightMostPlus1));
            }
            mergeColorsAndwriteSegmentToBackEnd(scs);
            lastRightMostPlus1 = scs.getRightMostCol() + 1;
        }
        if (row != -1 && lastRightMostPlus1 < this.width) {
            backend.writeToBackend(blankSegment(row, lastRightMostPlus1, this.width - lastRightMostPlus1));
        }
        backend.signal_eol();
        if (row != this.height)
            backend.cr();
    }

    private void mergeColorsAndwriteSegmentToBackEnd(ScreenLineSegment scs) {
        // check modify color if necessary
        Color scsColor = scs.getColors();
        if (scsColor == null || scsColor.isMergeable()) {
            if (scsColor == null) {
                scsColor = this.getTextColor();
            } else {
//                int x = 0;
//                if (scs.getCol() == 54 || scs.getCol() == 51) {
//                    x = 2;
//                }
                scsColor = scsColor.mergeWith(this.getTextColor());
            }
            // recreate with new color
            scs = createScreenObject(scsColor, scs, scs.getText());
        }
        backend.writeToBackend(scs);
    }

    public Canvas fgc(FgColor fgc) {
        this.color = color.newFrom(fgc);
        return this;
    }

    public Canvas bgc(BgColor bgc) {
        this.color = color.newFrom(bgc);
        return this;
    }

    public Color getTextColor() {
        return this.color;
    }

    public Canvas setTextColor(Color color) {
        this.color = color;
        return this;
    }

    public FGBGColors getEmptyColor() {
        return this.emptyColor;
    }

    public Canvas setEmptyColor(FGBGColors color) {
        this.emptyColor = color;
        return this;
    }

    public ScreenLineSegment createScreenObject(Color color, Position cursorPos, String simpleText) {
        sc_current_id++;
        SegmentImpl so = new SegmentImpl(color, cursorPos, sc_current_id, simpleText);
        return so;
    }
}
