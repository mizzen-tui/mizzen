package dtb.terminalIO.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * 
 * A ChainedEventHandler maintains a list of EventHandler objects - and more specifically
 * a list of ConditionalEventHandler objects.
 * 
 * <p/>
 * 
 * This object is an eventhandler itself - but it delegates the handling of an event to one 
 * or all of its internal eventHandlers.
 * 
 * <p/>
 * 
 * This objects operates in 2 behaviour styles when delegating to its internal eventHandlers 
 * 
 * <p/>
 *
 * <pre>
 * 1) Handle-All
 *      - when an event comes in 
 *         - loop thru all internal eventHandlers and
 *           tell each event to handler the event
 *             -- good example :  Idle (Timer) events are handled 
 *                                this way by the EventBus
 *                                
 *    to instantiate this type of behaviour:
 *         eH = ChainedEventHandler.useHandleAllStrategy()
 *      
 * 
 * 1) Chain-of-Responsibility
 *      - when an event comes in 
 *         - find the first internal eventhandler that says it can
 *           handle the event then give the event only to it.
 *             -- good example :  KeyStroke events are handled 
 *                                this way by the EventBus
 *                                
 *           NEEDS IMPROVEMENT :  Need to have a concept of
 *                                a focus Manager - either here or
 *                                                  somewhere else
 *                                this becomes increasingly important
 *                                as the UI becomes more complex with
 *                                multiple components wanting the keyboard                     
 *                                
 *    to instantiate this type of behaviour:
 *         eH = ChainedEventHandler.useChainOfResponsibilityStrategy()
 *      
 * </pre>     
 *      
 */
public class ChainedEventHandler implements EventHandler {

    private Collection<ConditionalEventHandler> eventHandlers = new ArrayList<>();

    private Collection<ConditionalEventHandler> addList = new ArrayList<>();
    private Collection<ConditionalEventHandler> removeList = new ArrayList<>();

    // this dictates how events are handled - either chain-of-responsibility
    // pattern or apply event to all handlers that can
    // accept the event type
    private EventHandler handleEventStrategy;

    private EventHandler notfound_event_handler = EventHandler.NULL_HANDLER;

    public static interface ConditionalEventHandler extends EventHandler {
	public EventHandler canHandle(TerminalEvent<?> te);
    }

    public static class ConditionalEventHandlerImpl implements ConditionalEventHandler {

	private Predicate<TerminalEvent<?>> canHandlePredicate;
	private EventHandler handler;

	protected EventHandler getHandler() {
	    return this.handler;
	}

	protected Predicate<TerminalEvent<?>> getCanHandlePredicate() {
	    return this.canHandlePredicate;
	}

	protected void setCanHandlePredicate(Predicate<TerminalEvent<?>> canHandlePredicate) {
	    this.canHandlePredicate = canHandlePredicate;
	}

	@Override
	public void handle(TerminalEvent<?> te) {
	    handler.handle(te);
	}

	@Override
	public EventHandler canHandle(TerminalEvent<?> te) {
	    if (canHandlePredicate.test(te)) {
		return this;
	    }
	    return null;
	}

	public ConditionalEventHandlerImpl(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
	    this.canHandlePredicate = canHandlePredicate;
	    this.handler = handler;
	}
    }

    private ChainedEventHandler() {
	handleEventStrategy = chainOfResponsibilityHandler;
    }

    public static ChainedEventHandler useChainOfResponsibilityStrategy() {
	ChainedEventHandler chainedEventHandler = new ChainedEventHandler();
	return chainedEventHandler;
    }

    public static ChainedEventHandler useHandleAllStrategy() {
	ChainedEventHandler chainedEventHandler = new ChainedEventHandler();
	chainedEventHandler.handleEventStrategy = chainedEventHandler.handleAllHandler;
	return chainedEventHandler;
    }

    public ChainedEventHandler(EventHandler notfound_event_handler) {
	this.notfound_event_handler = notfound_event_handler;
    }

    public ChainedEventHandler addEventHandler(ConditionalEventHandler eventHandler) {
	if (!eventHandlers.contains(eventHandler)) {
	    addList.add(eventHandler);
	    //eventHandlers.add(eventHandler);
	}
	return this;
    }

    public ChainedEventHandler remove(ConditionalEventHandler eventHandler) {
	if (eventHandlers.contains(eventHandler)) {
	    removeList.add(eventHandler);
	}
	return this;
    }

    public ChainedEventHandler addEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler eventHandler) {
	return addEventHandler(new ConditionalEventHandlerImpl(canHandlePredicate, eventHandler));
    }

    @Override
    public void handle(TerminalEvent<?> te) {
	adjustEventHandlerListIfNeeded();

	this.handleEventStrategy.handle(te);
	
	adjustEventHandlerListIfNeeded();
    }
    
    private void adjustEventHandlerListIfNeeded() {
	// remove from removeList
	if (removeList.size() > 0) {
	    synchronized (removeList) {
		for (ConditionalEventHandler eventHandler : this.removeList) {
		    eventHandlers.remove(eventHandler);
		}
		this.removeList.clear();
	    }
	}
	// add from addList
	if (addList.size() > 0) {
	    synchronized (addList) {
		for (ConditionalEventHandler eventHandler : this.addList) {
		    if ( ! eventHandlers.contains(eventHandler)) {
		       eventHandlers.add(eventHandler);
		    }
		}
		this.addList.clear();
	    }
	}
    }

    public EventHandler findRequestHandler(TerminalEvent<?> te) {
	EventHandler targetHandler = null;
	for (ConditionalEventHandler eH : eventHandlers) {
	    EventHandler foundHandler = eH.canHandle(te);
	    if (foundHandler != null) {
		targetHandler = foundHandler;
		break;
	    }
	}
	return targetHandler;
    }

    EventHandler chainOfResponsibilityHandler = new EventHandler() {

	@Override
	public void handle(TerminalEvent<?> te) {
	    EventHandler targetHandler = findRequestHandler(te);
	    if (targetHandler == null) {
		targetHandler = notfound_event_handler;
	    }
	    targetHandler.handle(te);
	}

    };

    EventHandler handleAllHandler = new EventHandler() {
	@Override
	public void handle(TerminalEvent<?> te) {
	    for (ConditionalEventHandler eH : eventHandlers) {
		EventHandler foundHandler = eH.canHandle(te);
		if (foundHandler != null) {
		    foundHandler.handle(te);
		}
	    }
	}
    };
    
    public String statsAsStr() {
	return "ChainedEventHandler[eventHandlers size = "+ eventHandlers.size()+"]";
    }

}
