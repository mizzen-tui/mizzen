package dtb.terminalIO.components.modal;

import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;

/**
 * A Modal eventHandler ( to be effective ) should be prepended to 
 * the {@link EventBusImpl} so that it is in front of all the other eventHandlers.
 * 
 * <p/>
 * 
 * Its job is to handle all keyboard events that occur within a modal
 * dialog and block any keyboard eventHandlers that are previously and currently
 * attached to the EventBus - thereby making it modal.  Its for this reason that this
 * EventHandler must be prepended to the EventBus.
 *
 */
class ModalEventHandler implements ConditionalEventHandler {

    private ChainedEventHandler modalEventHandler;
    private ConditionalEventHandler modalEventHandlerAsConditionalEventHandler;
    
    private ChainedEventHandler     innerEventHandler;
    private ConditionalEventHandler innerEventHandlerAsConditionalEventHandler;

    public ModalEventHandler(EventHandlerContainer parentEventsContainer) {
        modalEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
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

    public void appendEventHandler(ConditionalEventHandler eventHandler) {
        innerEventHandler.appendEventHandler(eventHandler);
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        modalEventHandlerAsConditionalEventHandler.handle(te);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (modalEventHandlerAsConditionalEventHandler.canHandle(te) != null) {
            return this;
        }
        return null;
    }
}