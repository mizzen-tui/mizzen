package mizzen.components.modal;

import mizzen.components.Component;
import mizzen.components.CompositeWithFocusManager;
import mizzen.components.input.Button;
import mizzen.controlcodes.color.Colors;
import mizzen.core.Canvas;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventHandlerContainer;
import mizzen.events.mouse.Draggable;
import mizzen.events.mouse.MouseEventOrchestrator;
import mizzen.events.mouse.MouseEventsForTesting;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * A draggable() dialog moves with a drag on its top border (the title): by as much as the mouse, kept on the screen.
 * The drag is the dialog's wherever the mouse goes, until the button comes up, even off the dialog - the modal
 * handler would otherwise stop those events and leave the drag stuck. A dialog not made draggable doesn't move.
 */
public class DialogDragTest extends TestCase {

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

    private Component<?> open(Dialog dialog) {
        dialog.show(bus, screen);
        bus.drain();
        return screen.findComponentByNameOrFail("dialog", Component.class);
    }

    private static Position corner(Component<?> panel) {
        return Component.UTIL.getCanvasOffsetFor(panel);
    }

    // press at from, drag through each spot, let go at the last
    private void drag(Position from, Position... through) {
        bus.deliver(MouseEventsForTesting.click(from.getRow(), from.getCol(), bus));
        bus.drain();
        for (Position p : through) {
            bus.deliver(MouseEventsForTesting.drag(p.getRow(), p.getCol(), bus));
            bus.drain();
        }
        Position last = through[through.length - 1];
        bus.deliver(MouseEventsForTesting.up(last.getRow(), last.getCol(), bus));
        bus.drain();
    }

    private static Position at(int row, int col) {
        return new Position.Simple(row, col);
    }

    public void testADragOnTheTitleMovesItByAsMuchAsTheMouse() {
        Component<?> panel = open(Dialog.info("Options").draggable().ok());
        Position before = corner(panel);
        Position title = at(before.getRow(), before.getCol() + 3);
        drag(title, at(title.getRow(), title.getCol() + 1), at(title.getRow() + 5, title.getCol() + 10));
        assertEquals(before.getRow() + 5, corner(panel).getRow());
        assertEquals(before.getCol() + 10, corner(panel).getCol());
    }

    public void testItStaysOnTheScreen() {
        Component<?> panel = open(Dialog.info("Options").draggable().ok());
        Position title = at(corner(panel).getRow(), corner(panel).getCol() + 3);
        drag(title, at(title.getRow(), title.getCol() + 1), at(200, 300));
        assertEquals(40 - panel.getHeight(), corner(panel).getRow());
        assertEquals(120 - panel.getWidth(), corner(panel).getCol());
        title = at(corner(panel).getRow(), corner(panel).getCol() + 3);
        drag(title, at(title.getRow(), title.getCol() + 1), at(0, 0));
        assertEquals("the spot grabbed (3 in) stays under the mouse, but not off the screen", at(0, 0).toString(),
                     corner(panel).toString());
    }

    public void testAMouseThatGetsAheadAndLetsGoOffTheDialogStillEndsTheDrag() {
        Component<?> panel = open(Dialog.info("Options").draggable().ok());
        Position start = corner(panel);
        Position title = at(start.getRow(), start.getCol() + 3);
        drag(title, at(title.getRow(), title.getCol() + 1), at(1, 30));        // one jump, far off the dialog, let go there
        assertEquals("the drag arrived", 1, corner(panel).getRow());
        Position moved = corner(panel);
        drag(at(38, 2), at(38, 3), at(30, 10));                                // a drag off the dialog: not a dialog drag
        assertEquals("the drag ended at the button coming up", moved.toString(), corner(panel).toString());
        bus.deliver(MouseEventsForTesting.click(0, 1, bus));
        bus.drain();
        assertEquals("still modal: the screen behind ignores the mouse", "", pressed.toString());
    }

    public void testWithoutDraggableItStaysPut() {
        Component<?> panel = open(Dialog.info("Options").ok());
        assertFalse("not even Draggable: the mouse goes as before", panel instanceof Draggable);
        Position before = corner(panel);
        Position title = at(before.getRow(), before.getCol() + 3);
        drag(title, at(title.getRow(), title.getCol() + 1), at(title.getRow() + 5, title.getCol() + 10));
        assertEquals(before.toString(), corner(panel).toString());
    }

    public void testADragInsideIsNotADragOfTheDialog() {
        Component<?> panel = open(Dialog.info("Options").draggable().ok());
        Position before = corner(panel);
        Position inside = at(before.getRow() + 1, before.getCol() + 3);       // under the frame: an empty row
        drag(inside, at(inside.getRow(), inside.getCol() + 1), at(inside.getRow() + 5, inside.getCol() + 10));
        assertEquals(before.toString(), corner(panel).toString());
    }

    public void testDraggableNeedsATitle() {
        try {
            Dialog.question("No title").draggable().ok().show(bus, screen);
            fail("expected an IllegalStateException");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("title"));
        }
    }
}
