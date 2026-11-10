package mizzen.components.modal;

import java.util.function.Predicate;

import mizzen.events.ChainedEventHandler;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.EventHandlerContainer;
import mizzen.events.TerminalEvent;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.geom.position.Position;

/**
 * Makes a dialog modal: while the dialog is open, keys and mouse clicks reach only the dialog, and
 * nothing else on the screen reacts until it closes.
 * <p/>
 * <b>How it gets in first.</b> The event bus offers each event to its handlers in order. This handler is
 * put at the front of the bus (prepended), so it sees every event before the rest of the app does.
 * <p/>
 * <b>Keys.</b> Every key goes to the dialog's own handlers (appendEventHandler). A key the dialog doesn't
 * use is swallowed by a catch-all at the end, so it never reaches the app behind the dialog.
 * <p/>
 * <b>Mouse.</b> For each mouse event it asks its dialog "is this spot part of you?" - the question given to
 * stopsMouseOutside. Part of the dialog is the dialog itself and anything a component inside it opened,
 * such as a dropdown's list, even where that sticks out past the dialog's frame:
 * <pre>
 *   +------------------------- screen --------------------------+
 *   |   background: clicks here are stopped                     |
 *   |        +-------------- dialog --------------+             |
 *   |        |  Colour  [ red          v]         |  <- part    |
 *   |        |          +--------------+          |             |
 *   |        |          | red          |          |             |
 *   |        +----------| green        |----------+             |
 *   |                   | blue         |  <- part: the list     |
 *   |                   +--------------+     is the dropdown's  |
 *   +-----------------------------------------------------------+
 * </pre>
 * Part of the dialog: the event is marked as on a dialog and goes on to the normal mouse handling (focus,
 * clicks, drags, as anywhere else). Not part of it: the event stops here.
 * <p/>
 * <b>Stacked dialogs.</b> A dialog opened from another dialog puts its own handler in front of the older
 * one's. The newer one judges each mouse event first; the older one lets marked events through without
 * judging them, and never sees the rest (the newer one stopped them). So all of the newest dialog works,
 * and the older dialog and the app behind it stay frozen until the newest closes:
 * <pre>
 *   mouse event
 *        |
 *        v
 *   +----------------------+   part of my dialog?  yes: mark it, pass it on
 *   | newest dialog's gate |                       no:  stop it
 *   +----------------------+
 *        |  only marked events
 *        v
 *   +----------------------+   marked?  pass it on
 *   | older dialog's gate  |
 *   +----------------------+
 *        |
 *        v
 *   +----------------------+   finds the topmost component at the spot, as usual
 *   | mouse handling       |   (MouseEventOrchestrator)
 *   +----------------------+
 * </pre>
 */
class ModalEventHandler implements ConditionalEventHandler {

    private ChainedEventHandler modalEventHandler;
    private ConditionalEventHandler modalEventHandlerAsConditionalEventHandler;
    
    private ChainedEventHandler     innerEventHandler;
    private ConditionalEventHandler innerEventHandlerAsConditionalEventHandler;

    public ModalEventHandler(EventHandlerContainer parentEventsContainer) {
        modalEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
        modalEventHandlerAsConditionalEventHandler = modalEventHandler.asConditionalEventHandler();
        
        innerEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
        innerEventHandlerAsConditionalEventHandler = innerEventHandler.asConditionalEventHandler();
        
        modalEventHandler.appendEventHandler(innerEventHandlerAsConditionalEventHandler);

        // now build an eventHandler that accepts all keyboard events
        // which prevents other keyboard events from ever seeing the key press (this is how we make it modal)
        KeyInputEventHandler keyboardSINKHOLE = new KeyInputEventHandler((te)->true,  (te)->{});
        
        // now add it to the tail end of this event handler
        modalEventHandler.appendEventHandler(keyboardSINKHOLE); // <-- this must always be the last
                                                                //     eventHandler in modalEventHandler
        
        parentEventsContainer.prependEventHandler(this);

    }

    private Predicate<Position> isOnDialog;              // null: the mouse is not judged
    private static final EventHandler STOP = (te) -> { };

    /**
     * Makes this handler judge the mouse too: for each mouse event it calls isOnDialog with the event's position
     * on the canvas, and stops the event when the answer is false (see the class comment). Dialog's answer: is
     * the component drawn at that spot part of the dialog? Never called: mouse events pass through unjudged.
     */
    public ModalEventHandler stopsMouseOutside(Predicate<Position> isOnDialog) {
        this.isOnDialog = isOnDialog;
        return this;
    }

    // a mouse event: null if it goes on (on the dialog, or on a newer one), STOP if it stops here
    private EventHandler judgeMouse(ApplicationMouseEvent mouse) {
        if (mouse.isOnADialog()) {
            return null;                                  // a newer dialog let it through
        }
        if (isOnDialog.test(mouse.getMousePosition())) {
            mouse.markOnADialog();                        // asking is also marking: the bus asks each handler once, in order
            return null;
        }
        return STOP;
    }

    public void appendEventHandler(ConditionalEventHandler eventHandler) {
        innerEventHandler.appendEventHandler(eventHandler);
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        modalEventHandlerAsConditionalEventHandler.handle(te);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (isOnDialog != null && te instanceof ApplicationMouseEvent) {
            return judgeMouse((ApplicationMouseEvent) te);
        }
        if (modalEventHandlerAsConditionalEventHandler.canHandle(te) != null) {
            return this;
        }
        return null;
    }
}