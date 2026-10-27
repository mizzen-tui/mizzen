package mizzen.events;

import java.util.function.Predicate;

import mizzen.events.TerminalEvent.IS_Event_TYPE;

public class OnTerminateEventHandler  extends ConditionalEventHandler.Impl {
    
    static Predicate<TerminalEvent<?>> canHandleTerminateEvent = new IS_Event_TYPE(TerminalEvent.TYPE.TERMINATE);

    public OnTerminateEventHandler(Runnable terminateHandler) {
        super(canHandleTerminateEvent, (te)-> terminateHandler.run());
    }
    
    @Override
    public void handle(TerminalEvent<?> te) {
        super.handle(te);
        removeMeFromEventBus(te.getEventBus());
    }

    private void removeMeFromEventBus(EventBus eventBus) {
        eventBus.removeEventHandler(  this  );
    }

}
