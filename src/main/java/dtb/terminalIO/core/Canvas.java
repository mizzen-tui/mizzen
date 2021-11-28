package dtb.terminalIO.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.ColorParsingUtils;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.OnRequestHideCursorEventHandler;
import dtb.terminalIO.events.OnRequestShowCursorEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.CleanString;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.textsegments.ScreenLineSegment.SegmentImpl;
import dtb.terminalIO.textsegments.ScreenLineSegment.UTIL.SegmentsFromStringResult;
import dtb.terminalIO.textsegments.ScreenLineSegmentsBuffer;
import dtb.terminalIO.util.ElapsedTime;

/*
 * A canvas is a data structure in which all components's contents will be copied into.
 * 
 * <p/>
 * 
 * When you invoke this canvas's refresh method, its contents will be "flushed" to the canvas backend which is
 * typically a terminal.
 * 
 * 
 */
public class Canvas implements Dimension, IScreenLineSegmentsBuffer.Provider {

    private CanvasBackend    backend;
    private Color            color;                                // current foregound/background color
    private CursorPos        cursorPos        = new CursorPos();
    
    private Position         last_RequestShowCursorPosition;
    private boolean          hideCursorRequested = false;
    private boolean          showCursorRequested = false;

    private int              height;
    private int              width;
    private int              sc_current_id    = 0;

    private char             emptyChar        = '░';
    private Color            emptyColor       = Colors.DEFAULT_COLOR;
    private static final int EMPTY_SEGMENT_ID = -67233;

    public void setEmptyChar(char emptyChar) {
        if (emptyChar <= 27) {
            throw new IllegalArgumentException("a CTRL character (character <= 27) is not allowed for an empty character! Illegal ascii value "+ ((int) emptyChar ));
        }
        this.emptyChar = emptyChar;
    }

    public void setEmptyColors(Color emptyColor) {
        this.emptyColor = emptyColor;
    }

    Rectangle meAsARectangle = null;

    public Rectangle asRectangle() {
        if (meAsARectangle == null) {
            meAsARectangle = new Rectangle.Simple(Position.ZERO, this);
        }
        return meAsARectangle;
    }

    public void setEmptyAttributes(Color emptyColor, char emptyChar) {
        setEmptyColors(emptyColor);
        setEmptyChar(emptyChar);
    }

    IScreenLineSegmentsBuffer segmentsBuffer = new ScreenLineSegmentsBuffer();

    public Canvas(CanvasBackend backend, Color color) {
        this.backend = backend;
        this.color = color;
    }

    public Canvas(CanvasBackend backend) {
        this(backend, Colors.DEFAULT_COLOR);
    }

    public Canvas(TerminalScreen screen, Colors.FGBGColors color) {
        this(new ScreenCanvasBackend(screen), color);
    }

    public Canvas(TerminalScreen screen) {
        this(new ScreenCanvasBackend(screen));
    }

    public CanvasBackend getCanvasBackend() {
        return this.backend;
    }
    
    public Canvas setSize(Dimension canvasSize) {
        return setSize(canvasSize.getHeight(), canvasSize.getWidth());
    }
    public Canvas setSize(int height, int width) {
        synchronized(this) {
           // TODO:  should not set size to be bigger than actual terminal
           // FIXME: add code to check actual screen size and max out to that size
            
           if (getSegmentsBuffer().getSegments().size()>0) {
               clearCanvas();
               
               
           }
           this.height = height;
           this.width = width;
           meAsARectangle = null;
           this.backend.setSize(height, width);
           
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
        moveToCursor(0, 0);
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

        if (s == null || s.length() ==0) {
            return this;
        }
        
        SegmentsFromStringResult result = ScreenLineSegment.UTIL.textSegmentsFromString(s, cursorPos.getRow(), cursorPos.getCol(), this.asRectangle(),
                (p, txt) -> createScreenObject(color, p, txt));

        if (result.size() == 0) {
            // special case trailing \n because ScreenLineSegment.UTIL.textSegmentsFromString does not handle it
            moveCURSORForTrailingCRs(s);
            return this;
        }
        segmentsBuffer.merge(result.ts);
        moveToCursor(result.pos.offsetAdd(0, 1));
        // special case trailing \n because ScreenLineSegment.UTIL.textSegmentsFromString does not handle it
        moveCURSORForTrailingCRs(s);
        return this;
    }
    
    private void moveCURSORForTrailingCRs(String s) {
        int i=s.length()-1;
        while (i>=0 && s.charAt(i)=='\n') {
            moveToCursor(cursorPos.getRow()+1,0);
            i--;
        }
    }

    public Canvas addTextHandleColors(String s) {
        boolean hasColorCodes = ColorParsingUtils.hasColorCodes(s);
        if (! hasColorCodes) {
            return t(s);
        }
        
        // TODO: not handling trailing CR's
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
        int len = sc.length();
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

    public synchronized void addComponentSegments(Position offset, Component<?> component) {
        Rectangle bounds = this.asRectangle();
        
        component.setCanvasOffset(offset);
        
        TreeSet<ScreenLineSegment> destinationCanvasSegments = this.getSegmentsBuffer().getSegments();
        TreeSet<ScreenLineSegment> componentSegmentsToAdd = component.getSegmentsBuffer().getSegments();
/*         
        Logging.info("addComponentSegments: addSegmentsWithOffsetBounded : elapsed " +
                        ElapsedTime.onRunOf(() -> {
                                                ScreenLineSegment.UTIL.addSegmentsWithOffsetBounded(offset, 
                                                                                                    bounds, 
                                                                                                    destinationCanvasSegments,
                                                                                                    componentSegmentsToAdd);
                                                }
                                          ).elapsed()
            );
*/
        ScreenLineSegment.UTIL.addSegmentsWithOffsetBounded(offset, 
                bounds, 
                destinationCanvasSegments,
                componentSegmentsToAdd);
    
    
    }

    public void addComponentSegments(Component<?> component) {
        addComponentSegments(Position.ZERO, component);
    }

    public void clearAndAddComponentSegments(Component<?> component) {
        
        boolean canvasContainsComponent = this.asRectangle().contains(component);

        if (canvasContainsComponent) {
            // fast case!! 
            component.setCanvasOffset(Position.ZERO);

            ElapsedTime componentDrawTime = ElapsedTime.start();
            TreeSet<ScreenLineSegment> componentSegments = component.getSegmentsBuffer().getSegments();
            //Logging.info("componentsDraw: "+ HasName.UTIL.getName(component) + ": segmenents-size: "+ componentSegments.size() + "  t:" + componentDrawTime);

            ElapsedTime elapsed = ElapsedTime.onRunOf( ()-> getSegmentsBuffer().clearAndMerge(component.getSegmentsBuffer().getSegments()));

            //Logging.info("clearAndAddComponentSegments: fast! "+ elapsed);

            return;
        }
        // we clear segmentsBuffer in this manner (new) to ensure that if it was pointing to a components
        // treeSet, that it is not doing so now. 
        segmentsBuffer = new ScreenLineSegmentsBuffer();
        //this.clearCanvas();
        //Logging.info("clearAndAddComponentSegments:slow-path: canvas size:"+ this.asRectangle()+ "  "+component);
        addComponentSegments(Position.ZERO, component);
    }
    
    public int segmentsSize() {
        return getSegmentsBuffer().getSegments().size();
    }

    protected void refreshBackend() {
        if (hideCursorRequested || showCursorRequested) {
            hideCursorRequested = false;
            backend.hideCursor(); // this takes effect on next refresh just below
        }
        
        if (showCursorRequested) {
            showCursorRequested = false;
            // append a show cursor on the next request
            backend.showCursor();
        }

        backend.refresh();

        if (last_RequestShowCursorPosition != null) {
            backend.showCursorAt(last_RequestShowCursorPosition);
        }
    }

    public boolean isEmpty() {
        return segmentsBuffer.isEmpty();
    }

    public synchronized Canvas refresh() {

        if (segmentsBuffer.isEmpty()) {
            outputBlankLines(0, this.height);
            refreshBackend();
            return this;
        }

        if (hideCursorRequested || showCursorRequested) {
            hideCursorRequested = false;
            backend.hideCursor(); // this takes effect on next back end refresh 
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
        refreshBackend();
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
                scsColor = scsColor.mergeWith(this.getTextColor());
            }
            // recreate with new color
            scs = createScreenObject(scsColor, scs, scs);
        }
        backend.writeToBackend(scs);
    }

    public Canvas fgc(Colors.FgColor fgc) {
        this.color = color.newFrom(fgc);
        return this;
    }

    public Canvas bgc(Colors.BgColor bgc) {
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

    public Color getEmptyColor() {
        return this.emptyColor;
    }

    public char getEmptyChar() {
        return this.emptyChar;
    }

    public Canvas setEmptyColor(Colors.FGBGColors color) {
        this.emptyColor = color;
        return this;
    }

    public ScreenLineSegment createScreenObject(Color color, Position cursorPos, CleanString simpleText) {
        sc_current_id++;
        SegmentImpl so = new SegmentImpl(color, cursorPos, sc_current_id, simpleText);
        return so;
    }

    public OnRequestShowCursorEventHandler asShowCursorEventHandler() {
        return new OnRequestShowCursorEventHandler() {
            @Override
            protected void handleRequestShowCursorAt(Position position) {
                Canvas.this.showCursorAt(position);
            }
        };
    }
    
    public void showCursorAt(Position position) {
        // we only move the cursor to the requested position
        // when we next do a canvas refresh().. so for now .. just 
        // remember the move cursor request.
        this.showCursorRequested = true;
        this.last_RequestShowCursorPosition = position;
        this.hideCursorRequested = false;
    }

    public OnRequestHideCursorEventHandler asHideCursorEventHandler() {
        return new OnRequestHideCursorEventHandler() {

            public void handle(TerminalEvent<?> te) {
                Canvas.this.showCursorRequested = false;
                Canvas.this.last_RequestShowCursorPosition = null;
                Canvas.this.hideCursorRequested = true;
            }
        };
    }
    
    public Position XXXgetLastRequestCursorPosition() {
        return !this.showCursorRequested ? null : this.last_RequestShowCursorPosition;
    }
    public Position getLastRequestCursorPosition() {
        return this.last_RequestShowCursorPosition;
    }
}
