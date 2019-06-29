package dtb.terminalIO.events;

/**
 * Something that can add and remove ConditionalEventHandler objects
 */
public interface EventHandlerContainer {

    public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler);

    public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler);
    
    
    public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler);
    
    
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
