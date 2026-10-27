package mizzen.events;

import java.util.ArrayList;
import java.util.List;

/**
 * Objects (like an EventBus) that can add and remove ConditionalEventHandlers.  
 */
public interface EventHandlerContainer {

    public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler);

    public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler);
    
    public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler);
    
    public static class Simple implements EventHandlerContainer {

        protected List<ConditionalEventHandler> eventHandlers = new ArrayList<ConditionalEventHandler>();
        
        @Override
        public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
            eventHandlers.add(0, eventHandler);
            return this;
        }

        @Override
        public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
            eventHandlers.add(eventHandler);
            return this;
        }

        @Override
        public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler) {
            eventHandlers.remove(eventHandler);
            return this;
        }

        public void appendTo(EventHandlerContainer dest) {
            for (ConditionalEventHandler eventHandler : eventHandlers) {
                dest.appendEventHandler(eventHandler);
            }
        }

        /** put these handlers in front of dest's, keeping their order (the first one here ends up first there) */
        public void prependTo(EventHandlerContainer dest) {
            for (int i = eventHandlers.size() - 1; i >= 0; i--) {
                dest.prependEventHandler(eventHandlers.get(i));
            }
        }

        public void removeFrom(EventHandlerContainer dest) {
            for (ConditionalEventHandler eventHandler : eventHandlers) {
                dest.removeEventHandler(eventHandler);
            }
        }
    }
    
    public static class SimpleList extends Simple {
        public List<ConditionalEventHandler> getEventHandlers() {
            return this.eventHandlers;
        }
        public void copyTo(EventHandlerContainer dest) {
            for (ConditionalEventHandler handler : getEventHandlers()) {
                dest.appendEventHandler(handler);
            }
        }
    }

    public static class Delegate implements EventHandlerContainer {

        EventHandlerContainer delegate;
        
        @Override
        public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
            delegate.prependEventHandler(eventHandler);
            return this;
        }

        @Override
        public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
            delegate.appendEventHandler(eventHandler);
            return this;
        }

        @Override
        public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler) {
            delegate.removeEventHandler(eventHandler);
            return this;
        }
        
    }
    
}
