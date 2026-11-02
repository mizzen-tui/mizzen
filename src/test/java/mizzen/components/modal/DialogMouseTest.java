package mizzen.components.modal;

import mizzen.components.Component;
import mizzen.components.CompositeWithFocusManager;
import mizzen.components.input.Button;
import mizzen.controlcodes.color.Colors;
import mizzen.core.Canvas;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventHandlerContainer;
import mizzen.events.mouse.MouseEventOrchestrator;
import mizzen.events.mouse.MouseEventsForTesting;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * The mouse while a dialog is open: a click on the dialog works as anywhere else; a click elsewhere does
 * nothing. A dialog opened from it is the live one - all of it, even a part that sticks out past the
 * first - and the first is frozen until it closes.
 */
public class DialogMouseTest extends TestCase {

    // the orchestrator goes on the bus first; each dialog then goes in front of it, as in a running app
    private final SyncEventLoop bus = new SyncEventLoop() {
        @Override
        public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
            return prependEventHandler(eventHandler);
        }
    };
    private final CompositeWithFocusManager screen = new CompositeWithFocusManager();
    private final Button behind = new Button(Colors.Whi, "Behind");
    private final StringBuilder pressed = new StringBuilder();

    @Override
    protected void setUp() throws Exception {
        screen.resize(new Dimension.Simple(40, 120));
        screen.add(0, 0, behind);
        behind.onChange((te) -> pressed.append("behind "));
        Canvas canvas = CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput();
        canvas.addComponentSegments(screen);
        bus.setRoot(screen);
        new MouseEventOrchestrator(null, canvas, bus).appendApplicationMouseEventToClickable(screen, null);
    }

    private void clickOn(Component<?> component) {
        Position at = Component.UTIL.getCanvasOffsetFor(component);
        bus.deliver(MouseEventsForTesting.click(at.getRow() + 1, at.getCol() + 1, bus));   // inside its border
        bus.drain();
    }

    private void clickBehind() {
        clickAt(0, 1);                                    // on its text: it has no border
    }

    private void clickAt(int row, int col) {
        bus.deliver(MouseEventsForTesting.click(row, col, bus));
        bus.drain();
    }

    private Dialog open(Dialog dialog) {
        dialog.show(bus, screen);
        bus.drain();
        return dialog;
    }

    public void testWithNoDialogTheBackgroundTakesClicks() {
        clickBehind();
        assertEquals("behind ", pressed.toString());
    }

    public void testAClickOnTheDialogWorks() {
        Dialog one = open(Dialog.question("One").button("yes", "Yes", (d) -> pressed.append("yes ")).cancel());
        clickOn(one.button("yes"));
        assertEquals("yes ", pressed.toString());
        assertFalse(one.isOpen());
    }

    public void testAClickBehindTheDialogDoesNothing() {
        Dialog one = open(Dialog.question("One").ok());
        clickBehind();
        assertEquals("", pressed.toString());
        assertTrue(one.isOpen());
    }

    /** two (tall: its buttons below one's bottom) is opened from one; then one is closed by code */
    public void testTheNewestDialogIsTheLiveOneAllOfIt() {
        Dialog one = open(Dialog.question("One").button("first", "First", (d) -> pressed.append("first ")).staysOpen().cancel());
        Dialog two = open(Dialog.question("Two\n\n\n\n\n\n\n\n\n\n\n\nmany lines: taller than one")
                .button("far", "Far", (d) -> pressed.append("far ")).staysOpen()
                .cancel());
        int oneBottom = Component.UTIL.getCanvasOffsetFor(one.button("first")).getRow() + 3;
        assertTrue("two's buttons are below one's bottom", Component.UTIL.getCanvasOffsetFor(two.button("far")).getRow() > oneBottom);

        clickOn(two.button("far"));                                   // the part of two that sticks out
        assertEquals("far ", pressed.toString());

        Position oneTop = Component.UTIL.getCanvasOffsetFor(one.button("first"));
        clickOn(one.button("first"));                                 // under two: two's own button area is elsewhere
        clickAt(0, 0);                                                // the background
        assertEquals("one and the background frozen", "far ", pressed.toString());

        two.close();
        bus.drain();
        clickAt(oneTop.getRow() + 1, oneTop.getCol() + 1);
        assertEquals("two closed: one is live again", "far first ", pressed.toString());
        clickBehind();
        assertEquals("the background still frozen", "far first ", pressed.toString());
    }
}
