package dtb.terminalIO.events;

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

        private List<ConditionalEventHandler> eventHandlers = new ArrayList<ConditionalEventHandler>();
        
        @Override
        public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
            eventHandlers.add(eventHandler);                
            return this;
        }

        @Override
        public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
            eventHandlers.add(0, eventHandler);
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

        public void prependTo(EventHandlerContainer dest) {
            for (ConditionalEventHandler eventHandler : eventHandlers) {
                dest.prependEventHandler(eventHandler);
            }
        }
        
        public void removeFrom(EventHandlerContainer dest) {
            for (ConditionalEventHandler eventHandler : eventHandlers) {
                dest.removeEventHandler(eventHandler);
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
