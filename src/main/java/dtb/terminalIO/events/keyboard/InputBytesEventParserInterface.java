package dtb.terminalIO.events.keyboard;

import dtb.terminalIO.events.EventBus;

/**
 * Interface for pluggable InputBytes parsers that convert raw terminal byte input
 * into structured terminal events (key, mouse, etc).
 */
public interface InputBytesEventParserInterface {

    /**
     * Main method to parse terminal input from byte stream.
     *
     * @param buffer   The raw input buffer.
     * @param readIn   Number of bytes read into the buffer.
     * @param eventBus The event bus to emit parsed events to.
     */
    void parseInput(byte[] buffer, int readIn, EventBus eventBus);

    /**
     * Called periodically by the InputEventsProducer to allow the parser
     * to timeout or finalize incomplete sequences (like ESC).
     *
     * @param eventBus The event bus to emit timeout-driven events to.
     * @return true if the idle handler produced any events
     */
    boolean idle(EventBus eventBus);
}
