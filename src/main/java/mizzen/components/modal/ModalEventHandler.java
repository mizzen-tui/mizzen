package mizzen.components.modal;

import java.util.function.Supplier;

import mizzen.events.ChainedEventHandler;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.EventHandlerContainer;
import mizzen.events.TerminalEvent;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.geom.Rectangle;

/**
 * A Modal eventHandler ( to be effective ) should be prepended to 
 * the {@link EventBus} so that it is in front of all the other eventHandlers.
 * 
 * <p/>
 * 
 * Its job is to handle all keyboard events that occur within a modal
 * dialog and block any keyboard eventHandlers that are previously and currently
 * attached to the EventBus - thereby making it modal.  Its for this reason that this
 * EventHandler must be prepended to the EventBus.
 * <p/>
 * Given where its dialog is (stopsMouseOutside), it does the same for the mouse: what lands outside the
 * dialog stops here, what lands on it is marked and goes on to the normal mouse handling - focus,
 * clicks, drags as anywhere else. A dialog opened from this one has its handler in front: it marks what
 * lands on it, and this one lets marked events through without judging them - so all of the newer
 * dialog works, even a part of it that sticks out past this one, and this one is frozen meanwhile.
 * <pre>
 *     newest dialog's handler:  on it? mark, pass on.  elsewhere? stop.
 *     older dialog's handler:   marked? pass on.  (unmarked never gets here while a newer one is open)
 *     MouseEventOrchestrator:   the topmost component at the spot, as usual
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

    private Supplier<Rectangle> dialogOnCanvas;          // null: the mouse is not judged
    private static final EventHandler STOP = (te) -> { };

    /** the mouse outside dialogOnCanvas - where the dialog is on the canvas now - stops here (see the class comment) */
    public ModalEventHandler stopsMouseOutside(Supplier<Rectangle> dialogOnCanvas) {
        this.dialogOnCanvas = dialogOnCanvas;
        return this;
    }

    // a mouse event: null if it goes on (on the dialog, or on a newer one), STOP if it stops here
    private EventHandler judgeMouse(ApplicationMouseEvent mouse) {
        if (mouse.isOnADialog()) {
            return null;                                  // a newer dialog let it through
        }
        Rectangle dialog = dialogOnCanvas.get();
        if (dialog != null && dialog.containsPosition(mouse.getMousePosition())) {
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
        if (dialogOnCanvas != null && te instanceof ApplicationMouseEvent) {
            return judgeMouse((ApplicationMouseEvent) te);
        }
        if (modalEventHandlerAsConditionalEventHandler.canHandle(te) != null) {
            return this;
        }
        return null;
    }
}