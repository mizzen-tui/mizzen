package dtb.terminalIO.events;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.keyboard.KeyInputEventsProducer;

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
 * <pre>
 * 
 * Events can also be generated for ( see {@link TerminalEvent} )
 *         KEYPRESS,
 *         TIMER,
 *         EXCEPTION,
 *         DEBUG
 *
 * 
 * </pre>
 * 
 * This object maintains two {@link ChainedEventHandler} 'chains'.    
 * 
 * <p/>
 * 
 * One handler chain is used for idle events.  When an idle event is generated
 * the event is passed to all idle event handlers and then each can decide to perform
 * it's operation based on elapsed time since last invoked.
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
public class EventBus implements EventHandlerContainer {

    private static final int EVENT_Q_CAPACITY = 20;  // number picked out of my hat!
                                                     //  -- increase as needed
    
    // main events handler chain
    private ChainedEventHandler eventsHandlerChain = ChainedEventHandler.useChainOfResponsibilityStrategy();

    // idle events processed on a separate chain
    private ChainedEventHandler idleEventsHandlerChain = null;  // we build this in lazy fashion on purpose
    
    // When idleEventsHandler is created we need to bridge the main event handler (eventsHandlerChain) 
    // with the the idleEventsHandler (idleEventsHandlerChain).  
    // We do that by creating a single Idle event handler (master_idlEventHandlerBRIDGE) that
    // is injected into the main handler.
    // master_idlEventHandlerBRIDGE will forward all idle events to idleEventsHandlerChain
    private ConditionalEventHandler master_idlEventHandlerBRIDGE = null;

    private AtomicBoolean idleEventsEnabled = new AtomicBoolean(false);
    
    // idle events processing can be turned on and off - this was introduced when 
    // processing terminal resize events
    private boolean idleEventsSuppressed = false;

    // the single Queue in which all events are placed
    private BlockingQueue<TerminalEvent<?>> queue = new ArrayBlockingQueue<TerminalEvent<?>>(EVENT_Q_CAPACITY);

    // when set to true, the EventBus thread will terminate
    private AtomicBoolean stopConsuming = new AtomicBoolean(false);

    // The task used to consume enqueued events
    private Runnable consumerTask;

    
    /* For autonumbering eventBus threads. */
    private static int threadInitNumber;
    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }
    
    public EventBus() {
        enableIdleEvents();
	consumerTask = new Runnable() {
	    @Override
	    public void run() {
		while (isActive()) {
		    try {
			// poll for an event with a short blocking timeout so that 
			// we can loop and test (isActive()) to see if we need to terminate
			TerminalEvent<?> te = queue.poll(1, TimeUnit.SECONDS);
			if (te != null) {
			    eventsHandlerChain.handle(te);
			}
		    } catch (Exception e) {
			eventsHandlerChain.handle(new TerminalEvent<Exception>(EventBus.this, TerminalEvent.TYPE.EXCEPTION, e));
			// hmm...  should I really be looping after an exception.. or should I terminate..
			//         for now .. letting the application make the decision.
		    }
		}
	    }
	};

	new Thread(consumerTask,"EventBus-Thread-"+nextThreadNum()).start();
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

    // TODO: If queue is full event wont get enqueue.. I dont tell caller - isn't that a bad design?
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

    
    public EventBus addIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {
        
        if (idleEventsSuppressed) return this;
        
	if (this.idleEventsHandlerChain == null) {
	    synchronized (this) {
		if (this.idleEventsHandlerChain == null && !idleEventsSuppressed) {
		    // build a handler for all idle events
		    this.idleEventsHandlerChain = ChainedEventHandler.useHandleAllStrategy();

		    // now build a handler on the main eventbus to dispatch to the 
		    // the newly built idleEventsHandler
		    // this.addEventHandler(master_idlEventHandler);
		    
		    enableIdleEvents();
		}
	    }
	}
	this.idleEventsHandlerChain.addEventHandler(onIdleEventHandler);
	return this;
    }
    
    

    synchronized public void enableIdleEvents() {
        
        if (master_idlEventHandlerBRIDGE != null) return;
        
        disableIdleEvents(); // remove  master and noop idle handlers
        
        idleEventsEnabled.set(true);
        
        master_idlEventHandlerBRIDGE = new ConditionalEventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {
                
                // intentionally checking this flag only in the handle event.
                if ( ! idleEventsEnabled.get()) {
                    return ;
                }
                
                if (EventBus.this.idleEventsHandlerChain == null) { // means not handlers
                    return ;
                }
                EventBus.this.idleEventsHandlerChain.handle(te);
            }

            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                if (te.getType().equals(TerminalEvent.TYPE.TIMER)) {
                    return this;
                }
                return null;
            }
        };
        
        this.addEventHandler(master_idlEventHandlerBRIDGE);
        idleEventsSuppressed = false;
        
    }
    
    public synchronized void disableIdleEvents() {
        
        idleEventsSuppressed = true;
        idleEventsEnabled.set(false);

        if (master_idlEventHandlerBRIDGE == null) return;
        
        // The way to suppress idle events is to 
        // remove the master_idlEventHandlerBRIDGE handler from
        // eventBus
        this.remove(master_idlEventHandlerBRIDGE);
        
        master_idlEventHandlerBRIDGE = null;
        
                
    }
    
    
    public EventBus removeIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {
	if (this.idleEventsHandlerChain == null) {
	    return this;
	}
	this.idleEventsHandlerChain.remove(onIdleEventHandler);
	return this;
    }

    public EventBus addEventHandler(ConditionalEventHandler eventHandler) {
	if (eventHandler instanceof OnIdleEventEventHandler) {	 

	    addIdleEventHandler((OnIdleEventEventHandler) eventHandler);
	    return this;
	}
	// add non idleEventHandler
	eventsHandlerChain.addEventHandler(eventHandler);
	return this;
    }

    public EventBus remove(ConditionalEventHandler eventHandler) {
	if (eventHandler instanceof OnIdleEventEventHandler) {
	    removeIdleEventHandler((OnIdleEventEventHandler) eventHandler);
	    return this;
	}
	eventsHandlerChain.remove(eventHandler);
	return this;
    }

    public String statsAsStr() {
	StringBuffer sb = new StringBuffer();
	sb.append("Event Bus : size=");
	sb.append(this.queue.size());
	sb.append("  idleEventsHandler " + (idleEventsHandlerChain==null? "empty" : idleEventsHandlerChain.statsAsStr()));
	sb.append("  handler " + eventsHandlerChain.statsAsStr()); 
	return sb.toString();
    }
    
    
}
