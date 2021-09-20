package dtb.terminalIO.events;

import java.util.function.Predicate;

import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;

public class OnRequestScrollToViewEventHandler  extends ConditionalEventHandler.Impl {

    static final Predicate<TerminalEvent<?>> canHandleScrollToView = new IS_Event_TYPE(TerminalEvent.TYPE.REQUEST_SCROLL_TO_VIEW);
    
    public OnRequestScrollToViewEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
        super(canHandleScrollToView.and(canHandlePredicate), handler);
    }

    public OnRequestScrollToViewEventHandler(EventHandler handler) {
        super(canHandleScrollToView, handler);
    }

    
    

}
