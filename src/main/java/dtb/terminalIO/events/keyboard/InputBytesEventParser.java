package dtb.terminalIO.events.keyboard;

import java.util.StringJoiner;

import dtb.terminalIO.events.CPREvent;
import dtb.terminalIO.events.DSREvent;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.Keys.KeyStrokeFindResult;
import dtb.terminalIO.events.mouse.MouseEvent;
import dtb.terminalIO.events.mouse.MouseEvent.MOUSE_ACTION;
import dtb.terminalIO.geom.Position;

// TODO and CRITICAL!!!:   This code is not properly handling when keyboard input is coming in fast..

//  can't handle the following input that comes into the input buffer right away .. 
//  right arrow twice -- Chars read in:  new KeySequence(27,91,67,27,91,67) 
//  left arrow twice -- chars read in: new KeySequence(27,91,68,27,91,68)   - ends up interpreting as ESC , '[' , 'D'
// -- need to rework the parser - in order to break down the sequence correctly
//    logic currently currently assumes that what is read in from the buffer represents 1 key - which is incorrect.

/**
 * A helper class used primarily by {@link InputEventsProducer}.
 * 
 * <p/>
 * 
 * Its job is to process a byte[] buffer of characters (presumably read in from
 * stdin) and converts this into a KeyStroke object which then gets enqueued
 * into an EventBus to be later handled by the application.
 * 
 * <p/>
 * 
 * Main entry point method is: {@link #parseInput(byte[], int, EventBus)}
 * 
 */
public class InputBytesEventParser {

    private static final int STATE_START = 1;
    private static final int STATE_ESCAPE_INTERMEDIATE = 2;
    private static final int STATE_ESCAPE = 3;
    private static final int STATE_NAVIGATION = 4;
    private static final int STATE_MOUSE_START = 5;
    private static final int STATE_MOUSE_DOWN_LEFT = 6;
    private static final int STATE_MOUSE_DOWN_RIGHT = 7;
    private static final int STATE_MOUSE_UP = 8;

    private static final int STATE_REPORT_START = 9;
    private static final int STATE_REPORT_INTERMEDIATE = 10;

    
    private int parse_STATE = STATE_START;
    
    private MOUSE_ACTION mouse_action = null;
    private int parsed_row = 0;
    private int parsed_col = 0;
    private String accumulatedDigits;

    private long last_ESC_PRESSED_TIME = Integer.MAX_VALUE;

    public class InputBytesDEBUGEvent extends TerminalEvent<String> {
        private static final long serialVersionUID = 1L;

        public InputBytesDEBUGEvent(byte[] buffer, int readIn) {
            super(InputBytesEventParser.this, TYPE.DEBUG, null);
            StringBuffer sb = new StringBuffer();
            StringJoiner sj = new StringJoiner(",");
            for (int i = 0; i < readIn; i++) {
                int theCHAR = buffer[i] & 0xff;
                if (theCHAR>32 && theCHAR<=127) {
                    sj.add(theCHAR+"'" +   ((char)theCHAR) + "'");
                } else {
                    sj.add(((int) buffer[i] & 0xff) + "");
                }
            }
            sb.append("Bytes read in:  new KeySequence(" + sj.toString() + ")     ");
            setEventData(sb.toString());
        }
    }

    /**
     * read in
     * 
     * @param buffer
     * @param readIn
     * @param eventBus
     */
    public void parseInput(byte[] buffer, int readIn, EventBus eventBus) {

        eventBus.raiseEvent(new InputBytesDEBUGEvent(buffer, readIn));

        boolean lookForKnownKeys = true;

        while (lookForKnownKeys && buffer.length >= 1) {

            // we have a bunch of predefined keys (defined in Keys F1,F2 ESC etc etc) that might 
            // recognize this input.  We check there first before relying on the parser below
            KeyStrokeFindResult result = Keys.getKeyStrokeFromChars(buffer, readIn);

            if (result.foundKeyStroke != null) {
                eventBus.raiseEvent(new KeyEvent(this, result.foundKeyStroke));
                resetTo_STATE_START();
            } else {
                lookForKnownKeys = false;
            }
            buffer = result.remainingBuffer;
            readIn = result.remainingBuffer.length;
        }

        for (int i = 0; i < readIn; i++) {
            int ch = buffer[i] & 0xff;
            processChar(ch, i, buffer, readIn, eventBus);
        }
        handleESC_ifNeeded(eventBus);
    }

    // returns true if ESC key handled
    private boolean handleESC_ifNeeded(EventBus eventBus) {

        if (parse_STATE == STATE_ESCAPE) {
            long now = System.currentTimeMillis();
            long elapsedSinceEscapePressed = now - last_ESC_PRESSED_TIME;
            if (elapsedSinceEscapePressed > 500) { // 1/2 second ... might be a bit long - we'll see ..
                eventBus.raiseEvent(new KeyEvent(this, Keys.ESC_KEYSTROKE));
                resetTo_STATE_START();
                return true;
            }
        }
        return false;
    }

    private void processChar(int ch, int index, byte[] buffer, int readIn, EventBus eventBus) {

        handleESC_ifNeeded(eventBus);

        // mouse pattern = 27   ,91  ,77 ,    35    ,  45
        //                 ESC  , ]  , M , (col+33) ,  (row+33)
        
        // CPR pattern   = 27   ,91  , 49 ,    59   , 49 , 82
        //                 ESC  , ]  , 1  ,    ;    , 1  , R
        
        switch (parse_STATE) {
        case STATE_START:

            if (ch == 0x1B) {
                parse_STATE = STATE_ESCAPE;
                last_ESC_PRESSED_TIME = System.currentTimeMillis();
                return;
            }

            if (ch <= 0x1F) {
                // Control character
                eventBus.raiseEvent(Keys.controlChar(this, (char)ch, false));
                resetTo_STATE_START();
                return;
            }
            

            if (ch >= 0x20) {
                // Normal character
                eventBus.raiseEvent(Keys.normalKeyEvent(this, (char)ch));
                parse_STATE = STATE_START;
                return;
            }

            break;
        case STATE_ESCAPE:
            if (ch <= 0x1F) {
                // ALT-Control character
                eventBus.raiseEvent(Keys.controlChar(this, (char) ch, true));
                resetTo_STATE_START();
                return;
            }
            
            if ( ch == 91 /* same as '[' */) {
                parse_STATE = STATE_REPORT_START;
                parsed_row = -1;
                parsed_col = -1;
                accumulatedDigits = "";
                return;
            }

            if (ch == 'O') {
                // This will be one of the function keys
                parse_STATE = STATE_ESCAPE_INTERMEDIATE;
                return;
            }

            // assume ALT-keystroke
            eventBus.raiseEvent(new KeyEvent(this, KeyStroke.ALTKey((char)ch)));

            resetTo_STATE_START();

            return;

        case STATE_ESCAPE_INTERMEDIATE:

            if ((ch >= 'P') && (ch <= 'S')) {
                // Function key
                switch (ch) {
                case 'P':
                    eventBus.raiseEvent(new KeyEvent(this, Keys.F1_KEYSTROKE));
                    break;
                case 'Q':
                    eventBus.raiseEvent(new KeyEvent(this, Keys.F2_KEYSTROKE));
                    break;
                case 'R':
                    eventBus.raiseEvent(new KeyEvent(this, Keys.F3_KEYSTROKE));
                    break;
                case 'S':
                    eventBus.raiseEvent(new KeyEvent(this, Keys.F4_KEYSTROKE));
                    break;
                default:
                    break;
                }
                resetTo_STATE_START();
                return;
            }

            // Unknown keystroke, ignore
            resetTo_STATE_START();
            return;

//        case STATE_NAVIGATION:
//
//            if (ch == 77 /* same as 'M' */) {
//                parse_STATE = STATE_MOUSE_START;
//                mouse_action = null;
//                parsed_row = -1;
//                parsed_col = -1;
//
//                return;
//            }
//            // Unknown keystroke, ignore
//            parse_STATE = STATE_START;
//            return;
            
        case STATE_MOUSE_START:
            if (ch == 32) {
                parse_STATE = STATE_MOUSE_DOWN_LEFT;
                mouse_action = MOUSE_ACTION.DOWN_LEFT;
                return;
            }
            if (ch == 34) {
                parse_STATE = STATE_MOUSE_DOWN_RIGHT;
                mouse_action = MOUSE_ACTION.DOWN_RIGHT;
                return;
            }
            if (ch == 35) {
                parse_STATE = STATE_MOUSE_UP;
                mouse_action = MOUSE_ACTION.UP;
                return;
            }
            
        case STATE_MOUSE_DOWN_LEFT:
        case STATE_MOUSE_DOWN_RIGHT:
        case STATE_MOUSE_UP:
             if (parsed_col == -1) {
                 parsed_col = ch - 33;
                 return;
             }
             parsed_row = ch - 33;

             eventBus.raiseEvent( new MouseEvent(this, mouse_action, new Position.Simple(parsed_row, parsed_col)));

             mouse_action = null;
             parsed_row = -1;
             parsed_col = -1;
             accumulatedDigits = null;
             parse_STATE = STATE_START;
             return; 

        case STATE_REPORT_START:
            
            if (ch == 77 /* same as 'M' */) {
                parse_STATE = STATE_MOUSE_START;
                mouse_action = null;
                parsed_row = -1;
                parsed_col = -1;
                accumulatedDigits = null;
                return;
            }
            
            if (ch != ';' && ch != 'n' && ch!='R' && (ch <'0' || ch > '9') ) {
              // Unknown keystroke, ignore
              resetTo_STATE_START();
              return;
            }
            
            if (ch >= '0' && ch <='9') {
                accumulatedDigits += (char)ch;
                return;
            }

            if (ch == 'n') {
                try {
                    int deviceStatus = Integer.parseInt(accumulatedDigits);
                    eventBus.raiseEvent( new DSREvent(this, deviceStatus));
                } catch (NumberFormatException nfe) {
                    // do nothing - and this will be discarded..
                }
                resetTo_STATE_START();
                return;
            }
            
            if (ch == ';') {
                try {
                    parsed_row = Integer.parseInt(accumulatedDigits);
                    accumulatedDigits = "";
                } catch (NumberFormatException nfe) {
                    resetTo_STATE_START(); // should never get here .. maybe thro exception? ( IllegalStateException?)
                    return;
                }
                parse_STATE = STATE_REPORT_INTERMEDIATE;
                return;
            }
            
            // we're in the unknown - so reset parser
            resetTo_STATE_START();
            
            return ;
             
        case STATE_REPORT_INTERMEDIATE:
            
            if (ch >= '0' && ch <='9') {
                accumulatedDigits += (char)ch;
                return;
            }

            if (ch == 'R') {
                try {
                    parsed_col = Integer.parseInt(accumulatedDigits);
                    eventBus.raiseEvent( new CPREvent(this, parsed_row, parsed_col));
                } catch (NumberFormatException nfe) {
                    // do nothing - and this will be discarded..
                }
                resetTo_STATE_START();
                return;
            }
            
            // unknown state ..
            resetTo_STATE_START();
            return;
            
        default:
            break;
        }
    }
    
    private void resetTo_STATE_START() {
        mouse_action = null;
        parsed_row = -1;
        parsed_col = -1;
        accumulatedDigits = null;
        parse_STATE = STATE_START;
    }

    public boolean idle(EventBus eventBus) {
        return handleESC_ifNeeded(eventBus);

    }

}
