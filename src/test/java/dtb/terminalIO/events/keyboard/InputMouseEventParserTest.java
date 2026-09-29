package dtb.terminalIO.events.keyboard;

import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.mouse.MOUSE_ACTION;
import dtb.terminalIO.events.mouse.RawParsedFromConsoleMouseEvent;
import dtb.terminalIO.geom.position.Position;
import junit.framework.TestCase;


public class InputMouseEventParserTest extends TestCase {

    private InputBytesEventParserInterface parser;
    private MockEventBus eventBus;

    public void setUp() {
        //parser = new InputBytesEventParser(); // Switch to InputBytesEventParserNewNotWorking to test new parser
        parser = new SGRInputBytesEventParser(); // Switch to InputBytesEventParserNewNotWorking to test new parser
        eventBus = new MockEventBus();
    }

    public void testLegacyMouseDownLeft() {
        // X10 format: ESC [ M <button> <col+33> <row+33>  (0-based col=15, row=10)
        byte[] seq = new byte[] {27, 91, 77, 32, 15 + 33, 10 + 33};
        InputBytesEventParser parser = new InputBytesEventParser();
        parser.parseInput(seq, seq.length, eventBus);
        assertMouseEvent(MOUSE_ACTION.DOWN_LEFT, 10, 15);
    }

    public void testSGRMouseDownLeft() {
        String sgr = "\u001b[<0;16;11M"; // ESC [ <0;16;11M
        parser.parseInput(sgr.getBytes(), sgr.length(), eventBus);
        assertMouseEvent(MOUSE_ACTION.DOWN_LEFT, 10, 15);
    }

    public void testSGRMouseUp() {
        String sgr = "\u001b[<0;16;11m"; // lowercase 'm' means mouse UP
        parser.parseInput(sgr.getBytes(), sgr.length(), eventBus);
        assertMouseEvent(MOUSE_ACTION.UP, 10, 15);
    }

    public void testPartialEscapeSequenceShouldNotEmitKey() {
        String badSeq = "\u001b[<0;16;";  // incomplete SGR
        parser.parseInput(badSeq.getBytes(), badSeq.length(), eventBus);
        assertFalse("No events should be emitted for incomplete SGR", eventBus.eventEmitted());
    }

    public void testMouseAndKeyMix() {
        String mix = "\u001b[<0;16;11M" + "A";
        parser.parseInput(mix.getBytes(), mix.length(), eventBus);
        assertMouseEvent(MOUSE_ACTION.DOWN_LEFT, 10, 15);
        assertKeyEvent('A');
    }

    private void assertMouseEvent(MOUSE_ACTION expectedAction, int expectedRow, int expectedCol) {
        TerminalEvent<?> e = eventBus.poll();
        assertNotNull("Expected a mouse event", e);
        assertEquals(TerminalEvent.TYPE.RAW_MOUSE, e.getType());
        RawParsedFromConsoleMouseEvent me = (RawParsedFromConsoleMouseEvent) e;
        assertEquals(expectedAction, me.getMouseAction());
        Position.Simple p = (Position.Simple) me.getMousePosition();
        assertEquals(expectedRow, p.getRow());
        assertEquals(expectedCol, p.getCol());
    }

    private void assertKeyEvent(char c) {
        TerminalEvent<?> e = eventBus.poll();
        assertNotNull("Expected key event", e);
        assertEquals(TerminalEvent.TYPE.KEYPRESS, e.getType());
        KeyEvent ke = (KeyEvent) e;
        assertEquals(c, ke.getChar());
    }

    // --- Simple mock event bus to capture emitted events ---
    private static class MockEventBus extends EventBus.STUBBED {
        private final java.util.Queue<TerminalEvent<?>> queue = new java.util.LinkedList<>();

        public EventBus raiseEvent(TerminalEvent<?> te) {
            queue.offer(te);
            return this;
        }

        public TerminalEvent<?> poll() {
            return queue.poll();
        }

        public boolean eventEmitted() {
            return !queue.isEmpty();
        }

        public void clear() {
            queue.clear();
        }
    }
}
