package mizzen.events;

import java.util.function.Predicate;

import mizzen.events.TerminalEvent.IS_Event_TYPE;
import mizzen.geom.position.Position;

public abstract class OnRequestShowCursorEventHandler implements ConditionalEventHandler {

    static Predicate<TerminalEvent<?>> canHandleShowCursor = new IS_Event_TYPE(TerminalEvent.TYPE.REQUEST_SHOW_CURSOR);
    
    public OnRequestShowCursorEventHandler() {
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        return canHandleShowCursor.test(te) ? this : null;
    }
    
    @Override
    final public void handle(TerminalEvent<?> te) {
        
      Position position = (Position) te.getEventData();

      handleRequestShowCursorAt(te.getSource(), position);
        
    }
    
    abstract protected void handleRequestShowCursorAt(Object source, Position eventData);
    


}
