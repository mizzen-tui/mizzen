package dtb.terminalIO.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

public class ChainedEventHandler implements EventHandler {

    private Collection<ConditionalEventHandler> eventHandlers          = new ArrayList<>();

    private EventHandler                        notfound_event_handler = EventHandler.NULL_HANDLER;

    public static interface ConditionalEventHandler extends EventHandler {
        public EventHandler canHandle(TerminalEvent<?> te);
    }

    public static class ConditionalEventHandlerImpl implements ConditionalEventHandler {

        private Predicate<TerminalEvent<?>> canHandlePredicate;
        private EventHandler             handler;

        protected EventHandler getHandler() {
            return this.handler;
        }
        
        protected Predicate<TerminalEvent<?>> getCanHandlePredicate() {
            return this.canHandlePredicate;
        }
 
        protected void setCanHandlePredicate(Predicate<TerminalEvent<?>> canHandlePredicate) {
            this.canHandlePredicate = canHandlePredicate;
        }
        
        @Override
        public void handle(TerminalEvent<?> te) {
            handler.handle(te);
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if (canHandlePredicate.test(te)) {
                return this;
            }
            return null;
        }

        public ConditionalEventHandlerImpl(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
            this.canHandlePredicate = canHandlePredicate;
            this.handler = handler;
        }
    }

    public ChainedEventHandler() {
    }

    public ChainedEventHandler(EventHandler notfound_event_handler) {
        this.notfound_event_handler = notfound_event_handler;
    }

    public ChainedEventHandler addEventHandler(ConditionalEventHandler eventHandler) {
        if (!eventHandlers.contains(eventHandler)) {
            eventHandlers.add(eventHandler);
        }
        return this;
    }
    
    public ChainedEventHandler remove(ConditionalEventHandler eventHandler) {
        if (eventHandlers.contains(eventHandler)) {
            eventHandlers.remove(eventHandler);
        }
        
        System.out.println("Removed!");
        return this;
    }

    public ChainedEventHandler addEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler eventHandler) {
        return addEventHandler(new ConditionalEventHandlerImpl(canHandlePredicate, eventHandler));
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        EventHandler targetHandler = findRequestHandler(te);
        if (targetHandler == null) {
            targetHandler = notfound_event_handler;
        }
        targetHandler.handle(te);
    }

    public EventHandler findRequestHandler(TerminalEvent<?> te) {
        EventHandler targetHandler = null;
        for (ConditionalEventHandler eH : eventHandlers) {
            EventHandler foundHandler = eH.canHandle(te);
            if (foundHandler != null) {
                targetHandler = foundHandler;
                break;
            }
        }
        return targetHandler;
    }

}
