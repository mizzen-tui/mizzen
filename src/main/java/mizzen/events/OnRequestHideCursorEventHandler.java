package mizzen.events;

import java.util.function.Predicate;

import mizzen.events.TerminalEvent.IS_Event_TYPE;

public abstract class OnRequestHideCursorEventHandler  implements ConditionalEventHandler {

    static Predicate<TerminalEvent<?>> canHandleHideCursor = new IS_Event_TYPE(TerminalEvent.TYPE.REQUEST_HIDE_CURSOR);

    
    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        return canHandleHideCursor.test(te) ? this : null;
    }
    
    @Override
    abstract public void handle(TerminalEvent<?> te);    
    
}
