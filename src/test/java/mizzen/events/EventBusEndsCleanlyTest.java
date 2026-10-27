package mizzen.events;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import mizzen.events.TerminalEvent.TYPE;
import junit.framework.TestCase;

/**
 * However the event thread ends - here an Error thrown by a handler - the bus stops being active
 * and the terminate handlers still run. (Before, the thread died and the bus still said active: the
 * input thread went on raising events nobody handled, and kept the program alive for good.)
 */
public class EventBusEndsCleanlyTest extends TestCase {

    public void testAnErrorInAHandlerEndsTheBusCleanly() throws Exception {
        EventBusImpl bus = new EventBusImpl();
        CountDownLatch terminated = new CountDownLatch(1);
        bus.appendTerminateEventHandler(new OnTerminateEventHandler(terminated::countDown));
        bus.appendEventHandler(new ConditionalEventHandler.Impl(
                (te) -> te.getType() == TYPE.KEYPRESS,
                (te) -> { throw new StackOverflowError("a handler gone wrong"); }));

        bus.raiseEvent(this, TYPE.KEYPRESS, "x");

        assertTrue("the terminate handlers ran", terminated.await(5, TimeUnit.SECONDS));
        long end = System.currentTimeMillis() + 5000;
        while (bus.isActive() && System.currentTimeMillis() < end) {
            Thread.sleep(10);
        }
        assertFalse("the bus is no longer active", bus.isActive());
    }
}
