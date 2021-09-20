package dtb.terminalIO.util.linuxproc;

import java.util.HashMap;
import java.util.Map;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.CursorBackward;
import dtb.terminalIO.controlcodes.ControlCode.CursorDown;
import dtb.terminalIO.controlcodes.ControlCode.CursorForward;
import dtb.terminalIO.controlcodes.ControlCode.CursorUp;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.ColorParsingUtils;
import dtb.terminalIO.controlcodes.color.ColorParsingUtils.ColorColorSegmentBuilder;
import dtb.terminalIO.textsegments.CleanString;

public class TerminalStreamOutputParser {
    

    
    static Map<String, ControlCode> knownControlCodes = new HashMap<String, ControlCode>();
    
    private static void addToKnownControlCodes(ControlCode controlCode) {
        knownControlCodes.put(controlCode.asString(), controlCode);
    }
    
    static {
        addToKnownControlCodes(ControlCode.ERASE_END_OF_LINE);
        addToKnownControlCodes(ControlCode.ERASE_START_OF_LINE);
        addToKnownControlCodes(ControlCode.ERASE_LINE);
        addToKnownControlCodes(ControlCode.ERASE_DOWN);
        addToKnownControlCodes(ControlCode.ERASE_UP);
        addToKnownControlCodes(ControlCode.ERASE_SCREEN);
        addToKnownControlCodes(ControlCode.HIDE_CURSOR);
        addToKnownControlCodes(ControlCode.SHOW_CURSOR);
        addToKnownControlCodes(ControlCode.CURSOR_LEFT);
    }
    
    
    
    private static final int STATE_START = 1;
    private static final int STATE_ESCAPE_INTERMEDIATE = 2;
    private static final int STATE_ESCAPE = 3;
    private static final int STATE_CONTROL_CODE_START = 4;
    private static final int STATE_CONTROL_CODE_END = 5;

    private int parse_STATE = STATE_START;
    private int parsed_row = 0;
    private int parsed_col = 0;
    private String accumulatedDigits;
    private char lastChar = Character.MIN_VALUE;

    private StringBuilder buffer = new StringBuilder();
    
    TerminalStreamConsumer streamConsumer;

    public TerminalStreamOutputParser(TerminalStreamConsumer streamConsumer) {
        super();
        this.streamConsumer = streamConsumer;
    }

    public void parseInput(String input) {
        parseInput( input.getBytes(), input.getBytes().length );
    }
   
    public void parseInput(byte[] buffer, int readIn) {
        for (int i=0; i< readIn; i++) {
            char ch =  (char) buffer[i];   // what about extended chars???  need to work this out
            parseInput(ch);
        }
    }

    public void parseInput(char[] buffer, int readIn) {
        for (int i=0; i< readIn; i++) {
            char ch =  buffer[i];
            parseInput(ch);
        }
    }

    private void addToBuffer(char ch) {
        buffer.append(ch);
    }

    private void clearBuffer() {
        buffer = new StringBuilder(); // expensive
    }
    
    private void flushBufferToText() {
        int bufferLen = buffer.length();
        for (int i=0; i < bufferLen; i++) {
            streamConsumer.consumeTextChar(buffer.charAt(i));
        }
        clearBuffer();
    }
    
    private boolean bufferIsKnownControlCode() {
        return (knownControlCodes.containsKey(buffer.toString()));
    }

    private void flushBufferAsControlCode() {
        ControlCode controlCode = knownControlCodes.get(buffer.toString());
        if (controlCode == null) {
            return;
        }
        streamConsumer.consumeControlCode(controlCode);
        clearBuffer();
    }
    
    public void parseInput(char ch) {
        
        switch (parse_STATE) {
        case STATE_START:
            if (ch == 0x1B) {
                parse_STATE = STATE_ESCAPE;
                addToBuffer(ch);
                return;
            }
            streamConsumer.consumeTextChar(ch);
            return;
        case STATE_ESCAPE:
            if (ch == '[') {
                addToBuffer(ch);
                parse_STATE = STATE_CONTROL_CODE_START;
                return;
            }
            resetTo_STATE_START();
            flushBufferToText();
            return;

        case STATE_CONTROL_CODE_START:
            addToBuffer(ch);
            if (Character.isLetter(ch)) {
                handleControlCodeEnd(ch);
                return;
            }
            return;

         default:
             flushBufferToText();
             resetTo_STATE_START();
             return;
        }

    }
    
    private void handleControlCodeEnd(char lastChar) {
        // lastChar is a letter
        if (bufferIsKnownControlCode()) {
            flushBufferAsControlCode();
            resetTo_STATE_START();
            return;
        }
        
        if (lastChar == 'm') {  // COLOR CODE
            // could be color code
            Color color = parseColor(buffer.toString());
            if (color != null) {
                streamConsumer.consumeControlCode(color);
                clearBuffer();
            } else {
                flushBufferToText();
            }
            resetTo_STATE_START();
            return ;
        }

        if (lastChar == 'A') {  // CURSOR UP
            try {
               int nLines = Integer.parseInt( buffer.substring(2, buffer.length()-1) );
               streamConsumer.consumeControlCode(new CursorUp(nLines));
               clearBuffer();
            } catch (NumberFormatException | StringIndexOutOfBoundsException e ) {
                flushBufferToText();
            }
            resetTo_STATE_START();
            return ;
        }

        if (lastChar == 'B') {  // CURSOR DOWN
            try {
               int nLines = Integer.parseInt( buffer.substring(2, buffer.length()-1) );
               streamConsumer.consumeControlCode(new CursorDown(nLines));
               clearBuffer();
            } catch (NumberFormatException | StringIndexOutOfBoundsException e ) {
                flushBufferToText();
            }
            resetTo_STATE_START();
            return ;
        }

        if (lastChar == 'C') {  // CURSOR FORWARD
            try {
               int nLines = Integer.parseInt( buffer.substring(2, buffer.length()-1) );
               streamConsumer.consumeControlCode(new CursorForward(nLines));
               clearBuffer();
            } catch (NumberFormatException | StringIndexOutOfBoundsException e ) {
                flushBufferToText();
            }
            resetTo_STATE_START();
            return ;
        }

        if (lastChar == 'D') {  // CURSOR BACKWARD
            try {
               int nLines = Integer.parseInt( buffer.substring(2, buffer.length()-1) );
               streamConsumer.consumeControlCode(new CursorBackward(nLines));
               clearBuffer();
            } catch (NumberFormatException | StringIndexOutOfBoundsException e ) {
                flushBufferToText();
            }
            resetTo_STATE_START();
            return ;
        }

        flushBufferToText();
        resetTo_STATE_START();
        return;
    }

    private Color parseColor(String colorCodedStr) {
        Color code[] = new Color[] {null};
        // adding the 'X' (below) is a stupid HACK which needs to be fixed
        // its to make ColorParsingUtils.parseColorsAndBuildResult() work 
        // it normally expects colors + plaintext.. in this case I only have a color
        // TODO: Fix ColorParsingUtils.parseColorsAndBuildResult() so that this hack is 
        //       not necessary
        ColorParsingUtils.parseColorsAndBuildResult(colorCodedStr+"X", new ColorColorSegmentBuilder(){
            @Override
            public boolean newColorSegment(Color color, CleanString simpleText) {
                code[0] = color;
                return true;
            }
        });
        return code[0];
    }
    
    
    private void resetTo_STATE_START() {
        parsed_row = -1;
        parsed_col = -1;
        accumulatedDigits = null;
        parse_STATE = STATE_START;
        lastChar = Character.MIN_VALUE;
    }
    
    
    public static interface TerminalStreamConsumer {
        void consumeTextChar(char ch);
        void consumeControlCode(ControlCode controlCode);
    }
    
}
