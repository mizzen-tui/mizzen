package mizzen.events.mouse;

import java.util.function.Predicate;

import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.TerminalEvent.IS_Event_TYPE;

public class ApplicationMouseEventHandler extends ConditionalEventHandler.Impl {
	
	static IS_Event_TYPE filterOnMouseEvtType = new IS_Event_TYPE(TerminalEvent.TYPE.APPLICATION_MOUSE);

	static Predicate<TerminalEvent<?>> ANY_EVENT = e -> true;
	
	public ApplicationMouseEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
		super(filterOnMouseEvtType.and(canHandlePredicate), handler);
		// TODO Auto-generated constructor stub
	}

	public ApplicationMouseEventHandler(EventHandler handler) {
		this(ANY_EVENT, handler);
	}

}
