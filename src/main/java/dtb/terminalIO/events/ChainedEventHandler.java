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
 *         eH = {@link ChainedEventHandler#useChainOfResponsibilityStrategy()}
 *      
 * </pre>     
 *      
 */
public class ChainedEventHandler implements EventHandler, EventHandlerContainer {

    private Collection<ConditionalEventHandler> eventHandlers = new ArrayList<>();

    private Collection<ConditionalEventHandler> addList = new ArrayList<>();
    private Collection<ConditionalEventHandler> removeList = new ArrayList<>();

    // this dictates how events are handled - either chain-of-responsibility
    // pattern or apply event to all handlers that can
    // accept the event type
    private EventHandler handleEventStrategy;

    private EventHandler notfound_event_handler = EventHandler.NULL_HANDLER;

    private ChainedEventHandler() {
	handleEventStrategy = chainOfResponsibilityHandler;
    }

    private boolean isChainOfResponsibility() {
        return handleEventStrategy == chainOfResponsibilityHandler;
    }
    
    
    /**
     * Builds a ChainedEventHandler that delegates incoming events
     * to its child event handlers in a Child-of_Responsibility style.
     * 
     * <p/>
     * 
     *    - Meaning that:  it passes the incoming event only to the first 
     *      child handler that claims it can handle the event.
     * 
     * <p/>
     * 
     * Useful for processing KeyEvents.
     *  
     * @return ChainedEventHandler
     */
    public static ChainedEventHandler useChainOfResponsibilityStrategy() {
	ChainedEventHandler chainedEventHandler = new ChainedEventHandler();
	return chainedEventHandler;
    }

    /**
     * Builds a ChainedEventHandler that delegates incoming events
     * to all its child event handlers that say they can handle
     * the event.  Useful for processing OnIdleEvents
     *  
     * @return ChainedEventHandler
     */
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
	    synchronized (addList) {
	        if (! addList.contains(eventHandler)) {
	            addList.add(eventHandler);
	        }
            }
	}
	return this;
    }

    public ChainedEventHandler remove(ConditionalEventHandler eventHandler) {
	if (eventHandlers.contains(eventHandler)) {
	    synchronized (removeList) {
                if (removeList.contains(eventHandler)) {
                    removeList.add(eventHandler);
                }
            }
	}
	return this;
    }

    public ChainedEventHandler addEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler eventHandler) {
	return addEventHandler(new ConditionalEventHandler.Impl(canHandlePredicate, eventHandler));
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        // while events are 'in-play' (being handled) we cannot add
        // or remove events - so do it now - before we handle in-coming event
	adjustEventHandlerListIfNeeded();
	
        // at this point event is "in-play"  (being handled)
	this.handleEventStrategy.handle(te);
	
        // while events are 'in-play' (being handled) we cannot add
        // or remove events - so do it now - after we handled in-coming event
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
    
    
    public ConditionalEventHandler asConditionalEventHandler() {
        
        // make sure our current handler list is up to date
        // hmmm .. will I need to do this in the test() function below?  Could be expensive to do that.. 
        this.adjustEventHandlerListIfNeeded();
        
        ConditionalEventHandler eventHandler = new ConditionalEventHandler.Impl(new Predicate<TerminalEvent<?>>() {

            @Override
            public boolean test(TerminalEvent<?> te) {
                
                adjustEventHandlerListIfNeeded(); 
                
                if (isChainOfResponsibility()) {
                   return ChainedEventHandler.this.findRequestHandler(te) != null;
                }
                // handle all strategy -- so return true if we have at least one handler
                return eventHandlers.size()>0;
            }
            
        }, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                // not happy that I am doing another findRequestHandler() 
                // - once in test() and then again here
                ChainedEventHandler.this.handle(te);
            }
            
        });
        
        return eventHandler;
    }
    
    public String statsAsStr() {
	return "ChainedEventHandler[eventHandlers size = "+ eventHandlers.size()+"]";
    }

}
