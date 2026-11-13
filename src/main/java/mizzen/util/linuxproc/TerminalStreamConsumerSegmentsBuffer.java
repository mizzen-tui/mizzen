package mizzen.util.linuxproc;

import mizzen.controlcodes.ControlCode;
import mizzen.controlcodes.ControlCode.CursorBackward;
import mizzen.controlcodes.ControlCode.CursorDown;
import mizzen.controlcodes.ControlCode.CursorForward;
import mizzen.controlcodes.ControlCode.CursorTo;
import mizzen.controlcodes.ControlCode.CursorUp;
import mizzen.controlcodes.color.Color;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.textsegments.ScreenLineSegment.SegmentImpl;
import mizzen.util.linuxproc.TerminalStreamOutputParser.TerminalStreamConsumer;

public class TerminalStreamConsumerSegmentsBuffer implements TerminalStreamConsumer {

    StreamConsumerSegmentsAdapter segments;
    Position cursor;
    Color currentColor;
    StringBuilder charBuffer = new StringBuilder();
    
    public static interface StreamConsumerSegmentsAdapter {

        boolean containsPosition(Position cursor);

        void removeRow(int row);

        int getWidth();

        /**
         * Removes what is written inside the rectangle. It may run past what is written (TO_THE_END: to the end of the
         * line or of the screen): the adapter cuts it to what is there when it applies it, since a viewer that queues
         * its changes knows its size only then.
         */
        void removeRect(Rectangle rectangleToRemove);

        void add(ScreenLineSegment newSegment);

        void clear();
        
    }
    

    public TerminalStreamConsumerSegmentsBuffer(StreamConsumerSegmentsAdapter segments, Color currentColor) {
        super();
        this.segments = segments;
        this.currentColor = currentColor;
        this.cursor = new Position.Simple(0,0);
        //sizeOfSegments = new Rectangle.Simple(0, 0, 0, 0);
    }

//    public Rectangle getPreCalulatedRectangle() {
//        return sizeOfSegments;
//    }
    
    
    @Override
    public void consumeTextChar(char ch) {
        
        // TODO:  handle \r \n \t   
        if (ch == '\n') {
            handleCR();
            return;
        }
        
        if (ch == '\r') {
            handleLF();
            return;
        }

        if (ch == '\t') {
            handleTAB();
            return;
        }
        
        
        charBuffer.append(ch);
    }
    
    private void handleCR() {
        consumeControlCode(ControlCode.CURSOR_LEFT);
        consumeControlCode(new CursorDown(1));
    }

    private void handleLF() {
        consumeControlCode(ControlCode.CURSOR_LEFT);
    }

    private void handleTAB() {
        // \t requires a concept of tab stops  
        
        handleSimpleChar(' ');
        int curColumn = positionAfterText().getCol();
        while (curColumn % 8 != 0) {
            handleSimpleChar(' ');
            curColumn = positionAfterText().getCol();
        }
    }

    private void handleSimpleChar(char ch) {
        charBuffer.append(ch);
    }


    @Override
    public void consumeControlCode(ControlCode controlCode) {
        flushCharsToSegmentsBuffer();
        if (controlCode instanceof Color) {
            this.currentColor = (Color) controlCode;
            return;
        }
        if (controlCode instanceof CursorUp) {
            cursorUp((CursorUp) controlCode);
            return;
        }
        if (controlCode instanceof CursorDown) {
            cursorDown((CursorDown) controlCode);
            return;
        }

        if (controlCode instanceof CursorForward) {
            cursorForward((CursorForward) controlCode);
            return;
        }

        if (controlCode instanceof CursorBackward) {
            cursorBackward((CursorBackward) controlCode);
            return;
        }

        if (controlCode instanceof CursorTo) {
            cursorTo((CursorTo) controlCode);
            return;
        }

        if (controlCode.equals( ControlCode.CURSOR_LEFT )) {
            CURSOR_LEFT();
            return;
        }

        if (controlCode.equals( ControlCode.ERASE_END_OF_LINE )) {
            ERASE_END_OF_LINE();
            return;
        }

        if (controlCode.equals( ControlCode.ERASE_START_OF_LINE )) {
            ERASE_START_OF_LINE();
            return;
        }

        if (controlCode.equals( ControlCode.ERASE_DOWN )) {
            ERASE_DOWN();
            return;
        }

        if (controlCode.equals( ControlCode.ERASE_UP )) {
            ERASE_UP();
            return;
        }

        if (controlCode.equals( ControlCode.ERASE_LINE )) {
            ERASE_LINE();
            return;
        }

        if (controlCode.equals( ControlCode.ERASE_SCREEN )) {
            ERASE_SCREEN();
            return;
        }

    }

    private void cursorUp(CursorUp cursorUp) {
        int deltaRow = cursorUp.deltaRow();
        if (cursor.getRow()+deltaRow <0) {
            cursor = new Position.Simple(0,cursor.getCol());
        } else {
            cursor = cursor.addRows(deltaRow);
        }
    }

    private void cursorDown(CursorDown cursorUp) {
        int deltaRow = cursorUp.deltaRow();
        // not sure about limits here..
        cursor = cursor.addRows(deltaRow);
    }

    private void cursorForward(CursorForward forward) {
        int deltaColumn = forward.getCursorDelta();
        // not sure about limits here..
        cursor = cursor.addCols(deltaColumn);
    }

    private void cursorBackward(CursorBackward backward) {
        int deltaColumn = backward.getCursorDelta();
        // not sure about limits here..
        cursor = cursor.addCols(deltaColumn);
    }

    /**
     * To a row and column. Row 0 is the first row kept since the last ESC[2J (which clears all and homes the cursor),
     * so a program that draws a whole screen (top, watch) clears once and then redraws in place from ESC[H.
     */
    private void cursorTo(CursorTo to) {
        int col = to.getCol() == CursorTo.SAME_COLUMN ? cursor.getCol() : to.getCol();
        cursor = new Position.Simple(Math.max(0, to.getRow()), Math.max(0, col));
    }

    private void CURSOR_LEFT() {
        cursor = new Position.Simple(cursor.getRow(), 0);
    }

    /** a width or height that reaches past anything written: removeRect cuts it to what is there */
    static final int TO_THE_END = Integer.MAX_VALUE / 4;

    // from the cursor to the end of its line. Not checked against what is written: a queued viewer's size is only
    // known as it applies the change (checked here, ESC[K erased nothing while a feed ran)
    private void ERASE_END_OF_LINE() {
        segments.removeRect(new Rectangle.Simple(cursor, new Dimension.Simple(1, TO_THE_END)));
    }


    // from the line's start to the cursor, the cursor's column too (as a terminal does)
    private void ERASE_START_OF_LINE() {
        segments.removeRect(new Rectangle.Simple(cursor.getRow(), 0, 1, cursor.getCol() + 1));
    }

    private void ERASE_LINE() {
        segments.removeRow(cursor.getRow());                // a row not written: nothing to remove
    }
    
    // from the cursor to the end of the screen: the rest of its line, and every row under it
    private void ERASE_DOWN() {
        ERASE_END_OF_LINE();
        segments.removeRect(new Rectangle.Simple(cursor.getRow() + 1, 0, TO_THE_END, TO_THE_END));
    }

    // from the top of the screen to the cursor: every row above it, and its line up to the cursor
    private void ERASE_UP() {
        if (cursor.getRow() > 0) {
            segments.removeRect(new Rectangle.Simple(0, 0, cursor.getRow(), TO_THE_END));
        }
        ERASE_START_OF_LINE();
    }
    
    private void ERASE_SCREEN() {
        this.cursor = new Position.Simple(0,0);
        this.segments.clear();
    }
    
    

    private void flushCharsToSegmentsBuffer() {
        if (charBuffer.length()>0) {
            addText(charBuffer.toString());
            charBuffer = new StringBuilder();
        }
    }
    
    Position positionAfterText() {
        return cursor.offsetAdd(0, charBuffer.length());
    }
    
    int countAddText = 0;
    
    private void addText(String text) {
        countAddText++;
        ScreenLineSegment newSegment = new SegmentImpl(currentColor, cursor, -1, text);
        segments.add(newSegment); // TODO:   This might need optimization to
                                  //         extend existing segment
        cursor = newSegment.getPostion_TopRight().offsetAdd(0, 1);
       // sizeOfSegments = sizeOfSegments.mergeAndExpandDimensionsWith(newSegment); 
    }

    public boolean hasTextInBuffer() {
        boolean hasTextToFlush = (charBuffer.length()>0);
        return hasTextToFlush;
    }
    
    public boolean flushText() {
        boolean hasTextToFlush = hasTextInBuffer();
        if (hasTextToFlush) {
           flushCharsToSegmentsBuffer();
        }
        return hasTextToFlush;
    }

    public void clear() {
        this.segments.clear();
        this.cursor = new Position.Simple(0,0);
        //sizeOfSegments = new Rectangle.Simple(0, 0, 0, 0);
    }

}
