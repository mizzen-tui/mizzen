package dtb.terminalIO.events;

/**
 * Something that can add and remove ConditionalEventHandler objects
 */
public interface EventHandlerContainer {

    public EventHandlerContainer addEventHandler(ConditionalEventHandler eventHandler);
    
    
    public EventHandlerContainer remove(ConditionalEventHandler eventHandler);
    
    
}
