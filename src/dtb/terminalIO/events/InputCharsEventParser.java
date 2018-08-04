package dtb.terminalIO.events;

import dtb.terminalIO.events.Keys.KeyEvent;
import dtb.terminalIO.events.Keys.KeyStroke;

public class InputCharsEventParser {

    private static final int  STATE_START               = 1;
    private static final int  STATE_ESCAPE_INTERMEDIATE = 2;
    private static final int  STATE_ESCAPE              = 3;

    private int  parse_STATE               = STATE_START;

    private long last_ESC_PRESSED_TIME = Integer.MAX_VALUE;
    private char last_CHAR_PRESSED         = 0;  // TODO: needed?

    public void parseInput(char[] buffer, int readIn, EventBus eventBus) {
        for (int i = 0; i < readIn; i++) {
            int ch = buffer[i];
            processChar((char) ch, eventBus);
        }
    }

    private void handleESC_ifNeeded(EventBus eventBus) {
        
        if (parse_STATE == STATE_ESCAPE) {
            long now = System.currentTimeMillis();
            long elapsedSinceEscapePressed = now - last_ESC_PRESSED_TIME;
            if (elapsedSinceEscapePressed > 250) {
                eventBus.addEvent( new Keys.KeyEvent(this, Keys.ESC_KEYSTROKE) );
                parse_STATE = STATE_START;
            }
        }
    }

    private void processChar(char ch, EventBus eventBus) {
        
        handleESC_ifNeeded(eventBus);
        
        switch (parse_STATE) {
            case STATE_START :
                
                if (ch == 0x1B) {
                    parse_STATE = STATE_ESCAPE;
                    last_ESC_PRESSED_TIME = System.currentTimeMillis();
                    return;
                }

                if (ch <= 0x1F) {
                    // Control character
                    eventBus.addEvent( Keys.controlChar(this,ch, false));
                    parse_STATE = STATE_START;
                    return;
                }

                if (ch >= 0x20) {
                    // Normal character
                    eventBus.addEvent(Keys.normalKeyEvent(this, ch));
                    parse_STATE = STATE_START;
                    return;
                }
                
                
                break;
            case STATE_ESCAPE:
                if (ch <= 0x1F) {
                    // ALT-Control character
                    eventBus.addEvent( Keys.controlChar(this,ch, true));
                    parse_STATE = STATE_START;
                    return;
                }

                if (ch == 'O') {
                    // This will be one of the function keys
                    parse_STATE = STATE_ESCAPE_INTERMEDIATE;
                    return;
                }
                
                //  assume ALT-keystroke
                eventBus.addEvent( new KeyEvent(this, new KeyStroke(ch , true) ));
                
                parse_STATE = STATE_START;
                
                return;
                
            case STATE_ESCAPE_INTERMEDIATE:
                
                if ((ch >= 'P') && (ch <= 'S')) {
                    // Function key
                    switch (ch) {
                    case 'P':
                        eventBus.addEvent(new KeyEvent(this, Keys.F1_KEYSTROKE));
                        break;
                    case 'Q':
                        eventBus.addEvent(new KeyEvent(this, Keys.F2_KEYSTROKE));
                        break;
                    case 'R':
                        eventBus.addEvent(new KeyEvent(this, Keys.F3_KEYSTROKE));
                        break;
                    case 'S':
                        eventBus.addEvent(new KeyEvent(this, Keys.F4_KEYSTROKE));
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
                
            default :
                
                break;
                
        }

    }

}
