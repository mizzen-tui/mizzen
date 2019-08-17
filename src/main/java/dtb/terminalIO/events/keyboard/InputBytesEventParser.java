package dtb.terminalIO.events.keyboard;

import java.util.StringJoiner;

import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.Keys.KeyStrokeFindResult;
import dtb.terminalIO.events.mouse.MouseEvent;
import dtb.terminalIO.events.mouse.MouseEvent.MOUSE_ACTION;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Position.Simple;

// TODO and CRITICAL!!!:   This code is not properly handling when keyboard input is coming in fast..

//  can't handle the following input that comes into the input buffer right away .. 
//  right arrow twice -- Chars read in:  new KeySequence(27,91,67,27,91,67) 
//  left arrow twice -- chars read in: new KeySequence(27,91,68,27,91,68)   - ends up interpreting as ESC , '[' , 'D'
// -- need to rework the parser - in order to break down the sequence correctly
//    logic currently currently assumes that what is read in from the buffer represents 1 key - which is incorrect.

/**
 * A helper class used primarily by KeyInputEventsProducer.
 * 
 * Its job is to process a byte[] buffer of characters (presumably read in from
 * stdin) and converts this into a KeyStroke object which then gets enqueued
 * into an EventBus to be later handled by the application.
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

    private int parse_STATE = STATE_START;
    
    private MOUSE_ACTION mouse_action = null;
    private int mouse_row = 0;
    private int mouse_col = 0;
    

    private long last_ESC_PRESSED_TIME = Integer.MAX_VALUE;

    public class InputBytesDEBUGEvent extends TerminalEvent<String> {
        private static final long serialVersionUID = 1L;

        public InputBytesDEBUGEvent(byte[] buffer, int readIn) {
            super(InputBytesEventParser.this, TYPE.DEBUG, null);
            StringBuffer sb = new StringBuffer();
            StringJoiner sj = new StringJoiner(",");
            for (int i = 0; i < readIn; i++) {
                sj.add(((int) buffer[i] & 0xff) + "");
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
            // recognize this input check there first
            KeyStrokeFindResult result = Keys.getKeyStrokeFromChars(buffer, readIn);

            if (result.foundKeyStroke != null) {
                eventBus.raiseEvent(new KeyEvent(this, result.foundKeyStroke));
            } else {
                lookForKnownKeys = false;
            }
            buffer = result.remainingBuffer;
            readIn = result.remainingBuffer.length;
        }

        for (int i = 0; i < readIn; i++) {
            int ch = buffer[i] & 0xff;
            processChar(ch, eventBus);
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
                parse_STATE = STATE_START;
                return true;
            }
        }
        return false;
    }

    private void processChar(int ch, EventBus eventBus) {

        handleESC_ifNeeded(eventBus);

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
                parse_STATE = STATE_START;
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
                parse_STATE = STATE_START;
                return;
            }
            
            if (ch == 91) {
                parse_STATE = STATE_NAVIGATION;
                return;
            }

            if (ch == 'O') {
                // This will be one of the function keys
                parse_STATE = STATE_ESCAPE_INTERMEDIATE;
                return;
            }

            // assume ALT-keystroke
            eventBus.raiseEvent(new KeyEvent(this, KeyStroke.ALTKey((char)ch)));

            parse_STATE = STATE_START;

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
                parse_STATE = STATE_START;
                return;
            }
            

            // Unknown keystroke, ignore
            parse_STATE = STATE_START;
            return;

        case STATE_NAVIGATION:
            
            if (ch == 77) {
                parse_STATE = STATE_MOUSE_START;
                mouse_action = null;
                mouse_row = -1;
                mouse_col = -1;

                return;
            }
            // Unknown keystroke, ignore
            parse_STATE = STATE_START;
            return;
            
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
             if (mouse_col == -1) {
                 mouse_col = ch - 33;
                 return;
             }
             mouse_row = ch - 33;
             
             eventBus.raiseEvent( new MouseEvent(this, mouse_action, new Position.Simple(mouse_row, mouse_col)));
             
             mouse_action = null;
             mouse_row = -1;
             mouse_col = -1;
             parse_STATE = STATE_START;
             return; 
            
        default:
            break;
        }
    }

    public boolean idle(EventBus eventBus) {
        return handleESC_ifNeeded(eventBus);

    }

}
