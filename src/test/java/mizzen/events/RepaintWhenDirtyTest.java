package mizzen.events;

import mizzen.components.TextComponent;
import mizzen.components.composite.VerticalPanel;
import mizzen.events.TerminalEvent.TYPE;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * RepaintWhenDirty asks for a canvas update on an idle check when something in the panel changed,
 * and stays quiet when nothing did - so code changing the screen need not ask itself.
 */
public class RepaintWhenDirtyTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final TextComponent status = new TextComponent("before");
    private final VerticalPanel panel = new VerticalPanel();
    private final RepaintWhenDirty repaint = new RepaintWhenDirty(panel, 1);

    @Override
    protected void setUp() {
        panel.add(status);
        panel.getSegmentsBuffer();                       // drawn: nothing dirty now
    }

    /** an idle event, once the check is due */
    private void idle() throws InterruptedException {
        Thread.sleep(5);
        TerminalEvent<?> te = new OnIdleEvent(this);
        te.setEventBus(bus);
        if (repaint.canHandle(te) != null) {
            repaint.handle(te);
        }
    }

    public void testNothingChangedNothingAsked() throws Exception {
        idle();
        assertEquals(0, bus.raised(TYPE.REQUEST_UPDATE_CANVAS).size());
    }

    public void testAChangeDeepInsideIsAskedFor() throws Exception {
        status.setText("after");                         // marks itself, and the panel, dirty
        idle();
        assertEquals(1, bus.raised(TYPE.REQUEST_UPDATE_CANVAS).size());
    }

    public void testOnceRedrawnItStopsAsking() throws Exception {
        status.setText("after");
        idle();
        panel.getSegmentsBuffer();                       // the canvas update draws it
        bus.clearHistory();
        idle();
        assertEquals(0, bus.raised(TYPE.REQUEST_UPDATE_CANVAS).size());
    }
}
