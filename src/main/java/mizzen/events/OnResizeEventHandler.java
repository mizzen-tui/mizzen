package mizzen.events;

import java.util.function.Predicate;

import mizzen.events.TerminalEvent.IS_Event_TYPE;
import mizzen.geom.Dimension;

/**
 * An EventHandler that accepts  terminal resize events ( TerminalEvent.TYPE.RESIZE )
 */
public abstract class OnResizeEventHandler implements ConditionalEventHandler {

    static Predicate<TerminalEvent<?>> canHandleResize = new IS_Event_TYPE(TerminalEvent.TYPE.RESIZE);
    
    public OnResizeEventHandler() {
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
	return canHandleResize.test(te) ? this : null;
    }
    
    @Override
    final public void handle(TerminalEvent<?> te) {
        
      Dimension size = (Dimension) te.getEventData();

      handleResize(size);
        
    }
    
    abstract protected void handleResize(Dimension size);
    
}
