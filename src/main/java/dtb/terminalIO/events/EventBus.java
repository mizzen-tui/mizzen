package dtb.terminalIO.events;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandler;
import dtb.terminalIO.events.TerminalEvent.TYPE;

/**
 * Warning:  This object will spawn a new thread in its constructor.
 * 
 * <p/>
 * 
 * Use this object very sparingly..  Usually only one per application.
 * 
 * <p/>
 * 
 * Very simple minded EventBus.
 * 
 * <p/>
 * 
 * The eventBus 'consumes' {@link TerminalEvent} objects.
 * 
 * <p/>
 * 
 * Usually TerminalEvent objects are generated for every Keystroke that has
 * been pressed by the user.  See {@link KeyInputEventsProducer}.
 * 
 * <p/>
 * 
 * Events can also be generated for ( see {@link TerminalEvent} )
 *         KEYPRESS,
 *         TIMER,
 *         EXCEPTION,
 *         DEBUG
 *
 * 
 * <p/>
 * 
 * This object maintains two {@link ChainedEventHandler} 'chains'.    
 * 
 * <p/>
 * 
 * One handler chain is used for idle events.  When an idle event is generated
 * the event is passed to all idle event handlers and then each can decide to perform
 * it operation based on elapsed time since last invoked.
 * 
 * <p/>
 * 
 * The other 'main' handler is used to handle all other events ( usually KeyStroke events).
 * 
 * <p/>
 * 
 * This chained handler works in a Chain-of-Responsibility style of handling.
 * The first eventHandler that can handle the event (usually a KeyStroke) is the eventHandler
 * that handles the event.   See {@link ChainedEventHandler} for more details on how it works.
 * 
 */
public class EventBus {

    private static final int EVENT_Q_CAPACITY = 5;  // number picked out of my hat!
                                                    //  -- increase as needed
    
    private ChainedEventHandler handler = ChainedEventHandler.useChainOfResponsibilityStrategy();

    private ChainedEventHandler idleEventsHandler;  // we build this in lazy fashion on purpose

    private BlockingQueue<TerminalEvent<?>> queue = new ArrayBlockingQueue<TerminalEvent<?>>(EVENT_Q_CAPACITY);

    private AtomicBoolean stopConsuming = new AtomicBoolean(false);

    private Runnable consumerTask;

    public EventBus() {
	consumerTask = new Runnable() {
	    @Override
	    public void run() {
		while (isActive()) {
		    try {
			// poll for an event with a short blocking timeout so that 
			// we can loop and test (isActive()) to see if we need to terminate
			TerminalEvent<?> te = queue.poll(1, TimeUnit.SECONDS);
			if (te != null) {
			    handler.handle(te);
			}
		    } catch (Exception e) {
			handler.handle(new TerminalEvent<Exception>(EventBus.this, TerminalEvent.TYPE.EXCEPTION, e));
			// hmm...  should I really be looping after an exception.. or should I terminate..
			//         for now .. letting the application make the decision.
		    }
		}
	    }
	};

	new Thread(consumerTask).start();
    }

    public void stopConsuming() {
	stopConsuming.set(true);
    }

    public boolean isActive() {
	return !stopConsuming.get();
    }

    public boolean isEmptyQueue() {
	return queue.isEmpty();
    }

    public EventBus addEvent(TerminalEvent<?> te) {
	if (queue.size()<EVENT_Q_CAPACITY) {  // todo:  make configurable - or block?
	   queue.add(te);
	}
	return this;
    }

    public <T> EventBus addEvent(Object source, TYPE type, T eventData) {
	TerminalEvent<T> te = new TerminalEvent<T>(source, type, eventData);
	return addEvent(te);
    }

    private ConditionalEventHandler master_idlEventHandler = null;
    private boolean idleEventsSuppressed = false;
    
    public EventBus addIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {
        if (idleEventsSuppressed) return this;
	if (this.idleEventsHandler == null) {
	    synchronized (this) {
		if (this.idleEventsHandler == null) {
		    // build a handler for all idle events
		    this.idleEventsHandler = ChainedEventHandler.useHandleAllStrategy();

		    // now build a handler on the main eventbus to dispatch to the 
		    // the newly built idleEventsHandler
		    // this.addEventHandler(master_idlEventHandler);
		    
		    enableIdleEvents();
		    
		}
	    }
	}
	this.idleEventsHandler.addEventHandler(onIdleEventHandler);
	return this;
    }
    
    public void enableIdleEvents() {
        
        if (master_idlEventHandler != null) return;
        
        disableIdleEvents();
        
        master_idlEventHandler = new ConditionalEventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {
                EventBus.this.idleEventsHandler.handle(te);
            }

            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                if (te.getType().equals(TerminalEvent.TYPE.TIMER)) {
                    return this;
                }
                return null;
            }
        };
        
        this.addEventHandler(master_idlEventHandler);
        idleEventsSuppressed = false;
        
    }
    
    public void disableIdleEvents() {
        
        idleEventsSuppressed = true;

        if (master_idlEventHandler == null) return;
        
        this.remove(master_idlEventHandler);
        
        master_idlEventHandler = null;
                
    }
    
    
    

    public EventBus removeIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {
	if (this.idleEventsHandler == null) {
	    return this;
	}
	this.idleEventsHandler.remove(onIdleEventHandler);
	return this;
    }

    public EventBus addEventHandler(ConditionalEventHandler eventHandler) {
	if (eventHandler instanceof OnIdleEventEventHandler) {	 

	    addIdleEventHandler((OnIdleEventEventHandler) eventHandler);
	    return this;
	}
	handler.addEventHandler(eventHandler);
	return this;
    }

    public EventBus remove(ConditionalEventHandler eventHandler) {
	if (eventHandler instanceof OnIdleEventEventHandler) {
	    removeIdleEventHandler((OnIdleEventEventHandler) eventHandler);
	    return this;
	}
	handler.remove(eventHandler);
	return this;
    }

    public EventBus addEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler eventHandler) {
	handler.addEventHandler(canHandlePredicate, eventHandler);
	return this;
    }

    public String statsAsStr() {
	StringBuffer sb = new StringBuffer();
	sb.append("Event Bus : size=");
	sb.append(this.queue.size());
	sb.append("  idleEventsHandler " + (idleEventsHandler==null? "empty" : idleEventsHandler.statsAsStr()));
	sb.append("  handler " + handler.statsAsStr()); 
	return sb.toString();
    }
    
    
}
