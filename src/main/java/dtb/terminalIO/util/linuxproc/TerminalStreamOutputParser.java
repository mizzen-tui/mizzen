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
    private static final int STATE_ESCAPE = 3;
    private static final int STATE_CONTROL_CODE_START = 4;
    private static final int STATE_OSC = 6;               // ESC ] ... - ends with BEL or ESC \
    private static final int STATE_SKIP_ONE = 7;          // ESC ( B and friends: one more char, then done

    private int parse_STATE = STATE_START;

    private StringBuilder buffer = new StringBuilder();
    
    TerminalStreamConsumer streamConsumer;

    public TerminalStreamOutputParser(TerminalStreamConsumer streamConsumer) {
        super();
        this.streamConsumer = streamConsumer;
    }

    public void parseInput(String input) {
        parseInput(input.toCharArray(), input.length());
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
    
    /**
     * One char of the stream: text goes to the consumer, control codes are collected and passed on.
     * <p/>
     * A control code it doesn't know is dropped, never shown as text - e.g. ESC[3J (clear scrollback,
     * sent by "clear"), ESC(B (from "tput sgr0"), ESC]0;title BEL (window title), ESC[?2004h.
     */
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
            clearBuffer();
            if (ch == ']') {
                parse_STATE = STATE_OSC;
            } else if (ch == '(' || ch == ')' || ch == '*' || ch == '+' || ch == '#' || ch == '%') {
                parse_STATE = STATE_SKIP_ONE;
            } else {
                resetTo_STATE_START();      // ESC 7, ESC 8, ESC =, ESC > ...: nothing to show
            }
            return;

        case STATE_CONTROL_CODE_START:
            addToBuffer(ch);
            if (ch >= 0x40 && ch <= 0x7E) {   // the final char of ESC [ ... (a letter, @, ~ ...)
                handleControlCodeEnd(ch);
            }
            return;

        case STATE_OSC:
            if (ch == 0x07) {
                resetTo_STATE_START();
            } else if (ch == 0x1B) {
                parse_STATE = STATE_SKIP_ONE;   // ESC \ ends it
            }
            return;

        case STATE_SKIP_ONE:
        default:
            resetTo_STATE_START();
            return;
        }

    }
    
    /** ESC [ params final: lastChar is the final char */
    private void handleControlCodeEnd(char lastChar) {
        if (bufferIsKnownControlCode()) {
            flushBufferAsControlCode();
            resetTo_STATE_START();
            return;
        }

        String params = buffer.substring(2, buffer.length() - 1);
        switch (lastChar) {
        case 'm':   // colour
            Color color = parseColor(buffer.toString());
            if (color != null) {
                streamConsumer.consumeControlCode(color);
            }
            break;
        case 'A':
            streamConsumer.consumeControlCode(new CursorUp(count(params)));
            break;
        case 'B':
            streamConsumer.consumeControlCode(new CursorDown(count(params)));
            break;
        case 'C':
            streamConsumer.consumeControlCode(new CursorForward(count(params)));
            break;
        case 'D':
            streamConsumer.consumeControlCode(new CursorBackward(count(params)));
            break;
        case 'G':   // to column n (1 = the first)
            streamConsumer.consumeControlCode(ControlCode.CURSOR_LEFT);
            if (count(params) > 1) {
                streamConsumer.consumeControlCode(new CursorForward(count(params) - 1));
            }
            break;
        case 'K':
            if (params.equals("0")) {
                streamConsumer.consumeControlCode(ControlCode.ERASE_END_OF_LINE);
            }
            break;
        default:    // not shown (e.g. ESC[3J, ESC[?2004h, ESC[5;10H)
            break;
        }
        clearBuffer();
        resetTo_STATE_START();
    }

    /** the count in ESC [ n A ...; none (or 0) means 1 */
    private static int count(String params) {
        try {
            int n = Integer.parseInt(params);
            return n > 0 ? n : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
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
        parse_STATE = STATE_START;
    }
    
    
    public static interface TerminalStreamConsumer {
        void consumeTextChar(char ch);
        void consumeControlCode(ControlCode controlCode);
    }
    
}
