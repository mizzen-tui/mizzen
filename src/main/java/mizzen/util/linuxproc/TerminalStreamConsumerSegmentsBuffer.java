package mizzen.util.linuxproc;

import mizzen.controlcodes.ControlCode;
import mizzen.controlcodes.ControlCode.CursorBackward;
import mizzen.controlcodes.ControlCode.CursorDown;
import mizzen.controlcodes.ControlCode.CursorForward;
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

        if (controlCode.equals( ControlCode.CURSOR_LEFT )) {
            CURSOR_LEFT();
            return;
        }

        if (controlCode.equals( ControlCode.ERASE_END_OF_LINE )) {
            ERASE_END_OF_LINE();
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

    private void CURSOR_LEFT() {
        cursor = new Position.Simple(cursor.getRow(), 0);
    }

    private void ERASE_END_OF_LINE() {
        if (segments.containsPosition(cursor)) {
            Rectangle rectangleToRemove = new Rectangle.Simple(cursor, new Dimension.Simple(1, segments.getWidth() - cursor.getCol()));
            segments.removeRect(rectangleToRemove);
        }
    }


    private void ERASE_START_OF_LINE() {
        
    }

    private void ERASE_LINE() {
        if (segments.containsPosition(cursor)) {
            segments.removeRow(cursor.getRow());
        }
    }
    
    private void ERASE_DOWN() {
        
    }
    
    private void ERASE_UP() {
        
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
