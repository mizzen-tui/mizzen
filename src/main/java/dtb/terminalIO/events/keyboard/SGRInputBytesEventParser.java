package dtb.terminalIO.events.keyboard;

import java.util.StringJoiner;

import dtb.terminalIO.events.CPREvent;
import dtb.terminalIO.events.DSREvent;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.Keys.KeyStrokeFindResult;
import dtb.terminalIO.events.mouse.MOUSE_ACTION;
import dtb.terminalIO.events.mouse.RawParsedFromConsoleMouseEvent;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;

public class SGRInputBytesEventParser  implements InputBytesEventParserInterface  {

    private static final int STATE_START = 1;
    private static final int STATE_ESCAPE_INTERMEDIATE = 2;
    private static final int STATE_ESCAPE = 3;
    private static final int STATE_NAVIGATION = 4;
    private static final int STATE_MOUSE_START = 5;
    private static final int STATE_MOUSE_DOWN_LEFT = 6;
    private static final int STATE_MOUSE_DOWN_RIGHT = 7;
    private static final int STATE_MOUSE_DOWN_LEFT_DRAG = 8;
    private static final int STATE_MOUSE_DOWN_RIGHT_DRAG = 9;
    private static final int STATE_MOUSE_UP = 10;
    private static final int STATE_REPORT_START = 11;
    private static final int STATE_REPORT_INTERMEDIATE = 12;
    private static final int STATE_SGR_MOUSE_REPORT = 13;

    private int parse_STATE = STATE_START;

    private MOUSE_ACTION mouse_action = null;
    private int parsed_row = -1;
    private int parsed_col = -1;
    private String accumulatedDigits;
    private StringBuilder sgrMouseSequence = null;

    private long last_ESC_PRESSED_TIME = Integer.MAX_VALUE;

    private static final boolean LOG_DEBUG_EVENTS = false;

    public class InputBytesDEBUGEvent extends TerminalEvent<String> {
        private static final long serialVersionUID = 1L;

        public InputBytesDEBUGEvent(byte[] buffer, int readIn) {
            super(SGRInputBytesEventParser.this, TYPE.DEBUG, null, Q_FULL_POLICY.OFFER);
            StringBuilder sb = new StringBuilder();
            StringJoiner sj = new StringJoiner(",");
            for (int i = 0; i < readIn; i++) {
                int theCHAR = buffer[i] & 0xff;
                if (theCHAR > 32 && theCHAR <= 127) {
                    sj.add("'" + ((char) theCHAR) + "'");
                } else {
                    sj.add((theCHAR) + "");
                }
            }
            sb.append("Bytes read in:  new KeySequence(" + sj.toString() + ")     ");
            setEventData(sb.toString());
        }
    }

    public void parseInput(byte[] buffer, int readIn, EventBus eventBus) {

        if (LOG_DEBUG_EVENTS) {
            eventBus.raiseEvent(new InputBytesDEBUGEvent(buffer, readIn));
        }

        boolean lookForKnownKeys = true;

        while (lookForKnownKeys && buffer.length >= 1) {
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

    private boolean handleESC_ifNeeded(EventBus eventBus) {
        if (parse_STATE == STATE_ESCAPE) {
            long now = System.currentTimeMillis();
            long elapsedSinceEscapePressed = now - last_ESC_PRESSED_TIME;
            if (elapsedSinceEscapePressed > 500) {
                eventBus.raiseEvent(new KeyEvent(this, Keys.ESC_KEYSTROKE));
                resetTo_STATE_START();
                return true;
            }
        }
        return false;
    }

    private void processChar(int ch, int index, byte[] buffer, int readIn, EventBus eventBus) {

        handleESC_ifNeeded(eventBus);

        switch (parse_STATE) {
        case STATE_START:
            if (ch == 0x1B) {
                parse_STATE = STATE_ESCAPE;
                last_ESC_PRESSED_TIME = System.currentTimeMillis();
                return;
            }

            if (ch <= 0x1F) {
                eventBus.raiseEvent(Keys.controlChar(this, (char) ch, false));
                resetTo_STATE_START();
                return;
            }

            if (ch >= 0x20) {
                eventBus.raiseEvent(Keys.normalKeyEvent(this, (char) ch));
                parse_STATE = STATE_START;
                return;
            }
            break;

        case STATE_ESCAPE:
            if (ch <= 0x1F) {
                eventBus.raiseEvent(Keys.controlChar(this, (char) ch, true));
                resetTo_STATE_START();
                return;
            }

            if (ch == '[') {
                parse_STATE = STATE_REPORT_START;
                parsed_row = -1;
                parsed_col = -1;
                accumulatedDigits = "";
                return;
            }

            if (ch == 'O') {
                parse_STATE = STATE_ESCAPE_INTERMEDIATE;
                return;
            }

            eventBus.raiseEvent(new KeyEvent(this, KeyStroke.ALTKey((char) ch)));
            resetTo_STATE_START();
            return;

        case STATE_ESCAPE_INTERMEDIATE:
            if ((ch >= 'P') && (ch <= 'S')) {
                switch (ch) {
                case 'P':
                    eventBus.raiseEvent(new KeyEvent(this, Keys.F1_KEYSTROKE)); break;
                case 'Q':
                    eventBus.raiseEvent(new KeyEvent(this, Keys.F2_KEYSTROKE)); break;
                case 'R':
                    eventBus.raiseEvent(new KeyEvent(this, Keys.F3_KEYSTROKE)); break;
                case 'S':
                    eventBus.raiseEvent(new KeyEvent(this, Keys.F4_KEYSTROKE)); break;
                }
                resetTo_STATE_START();
                return;
            }
            resetTo_STATE_START();
            return;

        case STATE_MOUSE_START:
            switch (ch) {
            case 32: mouse_action = MOUSE_ACTION.DOWN_LEFT; parse_STATE = STATE_MOUSE_DOWN_LEFT; return;
            case 34: mouse_action = MOUSE_ACTION.DOWN_RIGHT; parse_STATE = STATE_MOUSE_DOWN_RIGHT; return;
            case 35: mouse_action = MOUSE_ACTION.UP; parse_STATE = STATE_MOUSE_UP; return;
            case 64: mouse_action = MOUSE_ACTION.DOWN_LEFT_DRAG; parse_STATE = STATE_MOUSE_DOWN_LEFT_DRAG; return;
            case 66: mouse_action = MOUSE_ACTION.DOWN_RIGHT_DRAG; parse_STATE = STATE_MOUSE_DOWN_RIGHT_DRAG; return;
            default: resetTo_STATE_START(); return;
            }

        case STATE_MOUSE_DOWN_LEFT:
        case STATE_MOUSE_DOWN_RIGHT:
        case STATE_MOUSE_DOWN_LEFT_DRAG:
        case STATE_MOUSE_DOWN_RIGHT_DRAG:
        case STATE_MOUSE_UP:
            if (parsed_col == -1) {
                parsed_col = ch - 33;
                return;
            }
            parsed_row = ch - 33;
            eventBus.raiseEvent(new RawParsedFromConsoleMouseEvent(this, mouse_action, new Position.Simple(parsed_row, parsed_col)));
            resetTo_STATE_START();
            return;

        case STATE_REPORT_START:
            if (ch == 'M') {
                parse_STATE = STATE_MOUSE_START;
                mouse_action = null;
                parsed_row = -1;
                parsed_col = -1;
                accumulatedDigits = null;
                return;
            }
            if (ch == '<') {
                parse_STATE = STATE_SGR_MOUSE_REPORT;
                sgrMouseSequence = new StringBuilder();
                sgrMouseSequence.append((char) ch);
                return;
            }
            if (ch >= '0' && ch <= '9') {
                accumulatedDigits += (char) ch;
                return;
            }
            if (ch == ';') {
                parsed_row = Integer.parseInt(accumulatedDigits);
                accumulatedDigits = "";
                parse_STATE = STATE_REPORT_INTERMEDIATE;
                return;
            }
            if (ch == 'n') {
                eventBus.raiseEvent(new DSREvent(this, Integer.parseInt(accumulatedDigits)));
                resetTo_STATE_START();
                return;
            }
            resetTo_STATE_START();
            return;

        case STATE_REPORT_INTERMEDIATE:
            if (ch >= '0' && ch <= '9') {
                accumulatedDigits += (char) ch;
                return;
            }
            if (ch == 'R') {
                parsed_col = Integer.parseInt(accumulatedDigits);
                eventBus.raiseEvent(new CPREvent(this, parsed_row, parsed_col));
                resetTo_STATE_START();
                return;
            }
            resetTo_STATE_START();
            return;

        case STATE_SGR_MOUSE_REPORT:
            sgrMouseSequence.append((char) ch);
            if (ch == 'M' || ch == 'm') {
                parseSGRMouseSequence(sgrMouseSequence.toString(), ch, eventBus);
                sgrMouseSequence = null;
                resetTo_STATE_START();
            }
            return;

        default:
            resetTo_STATE_START();
        }
    }

    private void parseSGRMouseSequence(String sgr, int finalChar, EventBus eventBus) {
        try {
            String[] parts = sgr.substring(1, sgr.length() - 1).split(";");
            int buttonCode = Integer.parseInt(parts[0]);
            int col = Integer.parseInt(parts[1]) - 1;
            int row = Integer.parseInt(parts[2]) - 1;
            boolean isRelease = (finalChar == 'm');
            MOUSE_ACTION action = mapSGRMouseAction(buttonCode, isRelease);
            Logging.info("[SGR] Parsed: button=" + buttonCode + " col=" + col + " row=" + row + " isRelease=" + isRelease);

            eventBus.raiseEvent(new RawParsedFromConsoleMouseEvent(this, action, new Position.Simple(row, col)));
        } catch (Exception e) {
            // ignore bad sequences
        }
    }

    private MOUSE_ACTION mapSGRMouseAction(int buttonCode, boolean isRelease) {
        int base = buttonCode & 0b11;
        boolean isDrag = (buttonCode & 0b1000000) != 0;
        Logging.info("[SGR] map buttonCode=" + buttonCode + " base=" + base + " isDrag=" + isDrag + " isRelease=" + isRelease);

        if (isRelease) return MOUSE_ACTION.UP;
        switch (base) {
            case 0: return isDrag ? MOUSE_ACTION.DOWN_LEFT_DRAG : MOUSE_ACTION.DOWN_LEFT;
            case 1: return isDrag ? MOUSE_ACTION.DOWN_RIGHT_DRAG : MOUSE_ACTION.DOWN_RIGHT;
            default: return MOUSE_ACTION.UP;
        }
    }

    private void resetTo_STATE_START() {
        mouse_action = null;
        parsed_row = -1;
        parsed_col = -1;
        accumulatedDigits = null;
        sgrMouseSequence = null;
        parse_STATE = STATE_START;
    }

    public boolean idle(EventBus eventBus) {
        return handleESC_ifNeeded(eventBus);
    }
}
