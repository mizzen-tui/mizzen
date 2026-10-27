package mizzen.events;

import java.util.function.Predicate;

import mizzen.events.TerminalEvent.IS_Event_TYPE;

public class OnCustomApplicationEventHandler  extends ConditionalEventHandler.Impl {

    public static final Predicate<TerminalEvent<?>> canHandleCustomApplicationEvent = new IS_Event_TYPE(TerminalEvent.TYPE.CUSTOM_APPLICATION_EVENT);

    public OnCustomApplicationEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
        super(canHandleCustomApplicationEvent.and(canHandlePredicate), handler);
    }

}
