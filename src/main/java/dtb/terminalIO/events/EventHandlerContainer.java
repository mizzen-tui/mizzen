package dtb.terminalIO.events;

/**
 * Something that can add and remove ConditionalEventHandler objects
 */
public interface EventHandlerContainer {

    public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler);

    public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler);
    
    
    public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler);
    
    
}
