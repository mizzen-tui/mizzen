package dtb.terminalIO.events.keyboard;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import junit.framework.TestCase;

/**
 * Reads of the terminal that keep failing (the terminal went away, or the app is in the
 * background) are reported once, not once per retry - every retry used to raise an event, flooding
 * the bus and the log.
 */
public class InputEventsProducerFailingReadTest extends TestCase {

    /** a terminal that is gone: every read fails */
    static class Gone extends InputStream {
        final AtomicInteger tries = new AtomicInteger();

        @Override
        public int available() throws IOException {
            tries.incrementAndGet();
            throw new IOException("Input/output error");
        }

        @Override
        public int read() throws IOException {
            throw new IOException("Input/output error");
        }
    }

    /** a bus that is running, and counts the exception events raised on it */
    static class Counting extends EventBus.STUBBED {
        volatile boolean active = true;
        final AtomicInteger exceptions = new AtomicInteger();

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void stopConsuming() {
            active = false;
        }

        @Override
        public EventBus raiseEvent(TerminalEvent<?> te) {
            if (te.getType() == TYPE.EXCEPTION) {
                exceptions.incrementAndGet();
            }
            return this;
        }

        @Override
        public EventBus raiseEvent(Object source, TYPE type, Object eventData) {
            if (type == TYPE.EXCEPTION) {
                exceptions.incrementAndGet();
            }
            return this;
        }
    }

    public void testAFailingTerminalIsReportedOnceAndRetriedSlowly() throws Exception {
        Gone gone = new Gone();
        Counting bus = new Counting();
        InputEventsProducer producer = new InputEventsProducer(gone, bus);
        Thread.sleep(1500);
        bus.active = false;
        producer.getReaderThread().join(3000);

        assertEquals("reported once", 1, bus.exceptions.get());
        assertTrue("retried, but slowly: " + gone.tries.get() + " tries in 1.5 s", gone.tries.get() <= 15);
        assertTrue("still retrying (it may come back - e.g. fg)", gone.tries.get() >= 2);
    }
}
