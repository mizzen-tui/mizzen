package dtb.terminalIO.events;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.keyboard.InputEventsProducer;
import dtb.terminalIO.util.Logging;

/**
 * Warning: This object will spawn a new thread in its constructor.
 * 
 * <p/>
 * 
 * Use this object very sparingly.. Usually only one per application.
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
 * Usually TerminalEvent objects are generated for every Keystroke that has been
 * pressed by the user. See {@link InputEventsProducer}.
 * 
 * <pre>
 * 
 * Events can also be generated for ( see {@link TerminalEvent} )
 *         KEYPRESS,
 *         TIMER,
 *         EXCEPTION,
 *         DEBUG
 *         RAW_MOUSE
 *           :
 *          etc
 *
 * 
 * </pre>
 * 
 * This object maintains two {@link ChainedEventHandler} 'chains'.
 * 
 * <p/>
 * 
 * One handler chain is used for idle events. When an idle event is generated,
 * the event is passed to all registered idle event handlers and then each can decide to
 * perform it's operation based on elapsed time since last invoked.
 * 
 * <p/>
 * 
 * The other 'main' handler is used to handle all other events ( usually
 * KeyStroke or mouse events).
 * 
 * <p/>
 * 
 * This second chained handler works in a Chain-of-Responsibility style of handling.
 * The first eventHandler that can handle the event (usually a KeyStroke) is the
 * eventHandler that handles the event. See {@link ChainedEventHandler} for more
 * details on how it works.
 * 
 */
public class EventBusImpl implements EventHandlerContainer, EventBus {

    private static final int EVENT_Q_CAPACITY = 512; // number picked out of my hat!
                                                     // -- increase as needed

    // main events handler chain
    private ChainedEventHandler eventsHandlerChain = ChainedEventHandler.useChainOfResponsibilityStrategy("BUS:main-chainOfResponsibility");

    // main events handler chain
    private ChainedEventHandler terminateHandlerChain = null;

    private ConditionalEventHandler master_terminateEventHandlerBRIDGE = null;

    // idle events processed on a separate chain
    private ChainedEventHandler idleEventsHandlerChain = null; // we build this in lazy fashion on purpose

    // When idleEventsHandler is created we need to bridge the main event handler
    // (eventsHandlerChain)
    // with the the idleEventsHandler (idleEventsHandlerChain).
    // We do that by creating a single Idle event handler
    // (master_idlEventHandlerBRIDGE) that
    // is injected into the main handler.
    // master_idlEventHandlerBRIDGE will forward all idle events to
    // idleEventsHandlerChain
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

    public EventBusImpl() {
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
                            handleEvent(te);
                        }
                    } catch (Exception e) {
                        handleEvent(new TerminalEvent<Exception>(EventBusImpl.this, TerminalEvent.TYPE.EXCEPTION, e).setEventBus(EventBusImpl.this));
                        // hmm... Should I really be looping after an exception.. or should I
                        //        terminate?..
                        //        For now .. I'm letting the user application make the decision.
                    }
                }
                handleEvent(new TerminalEvent<Exception>(EventBusImpl.this, TerminalEvent.TYPE.TERMINATE, null, EventBusImpl.this).setEventBus(EventBusImpl.this));
            }
        };

        new Thread(consumerTask, "EventBus-Thread-" + nextThreadNum()).start();
    }
    
    protected void handleEvent(TerminalEvent<?> te) {
        te.setEventBus(EventBusImpl.this);
//        if (!(te instanceof OnIdleEvent)) {
//            Logging.info("EVENT IN QUEUE " + te.toString());
//        }
        eventsHandlerChain.handle(te);
        te.setEventBus(null);
        te.onEventProcessed();
    }

    /* (non-Javadoc)
     * @see dtb.terminalIO.events.IEventBus#stopConsuming()
     */
    @Override
    public void stopConsuming() {
        stopConsuming.set(true);
    }

    /* (non-Javadoc)
     * @see dtb.terminalIO.events.IEventBus#isActive()
     */
    @Override
    public boolean isActive() {
        return !stopConsuming.get();
    }

    /* (non-Javadoc)
     * @see dtb.terminalIO.events.IEventBus#isEmptyQueue()
     */
    @Override
    public boolean isEmptyQueue() {
        return queue.isEmpty();
    }

    // TODO: If queue is full event wont get enqueued.. I dont tell caller - isn't
    // that a bad design?
    /* (non-Javadoc)
     * @see dtb.terminalIO.events.IEventBus#raiseEvent(dtb.terminalIO.events.TerminalEvent)
     */
    @Override
    public EventBus raiseEvent(TerminalEvent<?> te) {
        if (queue.size() < EVENT_Q_CAPACITY) { // todo: make configurable - or block?
            queue.add(te);
        } else {
            try {
                if (!queue.offer(te, 25, TimeUnit.MILLISECONDS)) {
                    // still full after waiting: the event is lost - say so instead of dropping it silently
                    noteDropped(te);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    /* (non-Javadoc)
     * @see dtb.terminalIO.events.IEventBus#raiseEvent(java.lang.Object, dtb.terminalIO.events.TerminalEvent.TYPE, T)
     */
    private final AtomicInteger droppedSinceLogged = new AtomicInteger();
    private volatile long lastDropLogged = 0;

    /**
     * An event lost to a full queue: logged - the first at once, then at most a line a second with
     * how many were lost (one line per lost event would flood the log just when the app is busiest).
     */
    private void noteDropped(TerminalEvent<?> te) {
        int dropped = droppedSinceLogged.incrementAndGet();
        long now = System.currentTimeMillis();
        if (now - lastDropLogged >= 1000) {
            lastDropLogged = now;
            droppedSinceLogged.set(0);
            Logging.forceLogInfo("EventBus: queue full (" + EVENT_Q_CAPACITY + "), " + dropped + " event(s) dropped since the last report; the last: " + te);
        }
    }

    @Override
    public <T> EventBus raiseEvent(Object source, TYPE type, T eventData) {
        TerminalEvent<T> te = new TerminalEvent<T>(source, type, eventData);
        return raiseEvent(te);
    }

    /* (non-Javadoc)
     * @see dtb.terminalIO.events.IEventBus#appendIdleEventHandler(dtb.terminalIO.events.OnIdleEventEventHandler)
     */
    @Override
    public EventBus appendIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {

        if (idleEventsSuppressed) {
            return null;  // null is a signal to caller that handler was not appended
        }

        if (this.idleEventsHandlerChain == null) {
            synchronized (this) {
                if (this.idleEventsHandlerChain == null && !idleEventsSuppressed) {
                    // build a handler for all idle events
                    this.idleEventsHandlerChain = ChainedEventHandler.useHandleAllStrategy("BUS:idle");

                    // now build a handler on the main eventbus to dispatch to the
                    // the newly built idleEventsHandler
                    // this.appendEventHandler(master_idlEventHandler);

                    enableIdleEvents();
                }
            }
        }
        this.idleEventsHandlerChain.appendEventHandler(onIdleEventHandler);
        return this;
    }

    /* (non-Javadoc)
     * @see dtb.terminalIO.events.IEventBus#appendTerminateEventHandler(dtb.terminalIO.events.OnTerminateEventHandler)
     */
    @Override
    public EventBus appendTerminateEventHandler(OnTerminateEventHandler terminateHandler) {

        if (this.terminateHandlerChain == null) {
            synchronized (this) {
                if (this.terminateHandlerChain == null) {
                    // build a handler for all idle events
                    ChainedEventHandler terminateHandlerAll = ChainedEventHandler.useHandleAllStrategy("BUS:terminate");
                    terminateHandlerAll.appendEventHandler( new OnTerminateEventHandler(()-> stopConsuming()) );

                    this.master_terminateEventHandlerBRIDGE = new ConditionalEventHandler() {
                        
                        boolean terminateHandled = false;
                        
                        @Override
                        public void handle(TerminalEvent<?> te) {
                            if (terminateHandled == true) {
                                return;
                            }
                            terminateHandled = true;
                            if (EventBusImpl.this.terminateHandlerChain != null) {
                                // execute
                                EventBusImpl.this.terminateHandlerChain.handle(te);
                            }
                            // and now remove
                            if (EventBusImpl.this.master_terminateEventHandlerBRIDGE != null) {
                                EventBusImpl.this.removeEventHandler(EventBusImpl.this.master_terminateEventHandlerBRIDGE);
                            }
                            EventBusImpl.this.master_terminateEventHandlerBRIDGE = null;
                            EventBusImpl.this.terminateHandlerChain = null;
                        }

                        @Override
                        public EventHandler canHandle(TerminalEvent<?> te) {
                            if (te.getType().equals(TerminalEvent.TYPE.TERMINATE)) {
                                return this;
                            }
                            return null;
                        }
                        public String toString() {
                            return "BUS:master_terminateEventHandlerBRIDGE";
                        }
                    };
                    
                    this.appendEventHandler(master_terminateEventHandlerBRIDGE);

                    this.terminateHandlerChain = terminateHandlerAll;
                }
            }
        }
        this.terminateHandlerChain.appendEventHandler(terminateHandler);
        return this;
    }

    protected EventBus removeTerminateEventHandler(OnTerminateEventHandler terminateHandler) {
        if (this.terminateHandlerChain == null) {
            return this;
        }
        this.terminateHandlerChain.removeEventHandler(terminateHandler);
        return this;
    }
    
    /* (non-Javadoc)
     * @see dtb.terminalIO.events.IEventBus#enableIdleEvents()
     */
    @Override
    synchronized public void enableIdleEvents() {

        if (master_idlEventHandlerBRIDGE != null)
            return;

        disableIdleEvents(); // remove master and NOOP idle handlers

        idleEventsEnabled.set(true);

        master_idlEventHandlerBRIDGE = new ConditionalEventHandler() {

            AtomicBoolean busy = new AtomicBoolean(false);

            @Override
            public void handle(TerminalEvent<?> te) {

                // intentionally checking this flag only in the handle event.
                if (!idleEventsEnabled.get()) {
                    return;
                }

                if (EventBusImpl.this.idleEventsHandlerChain == null) { // means no handlers
                    return;
                }

                if (busy.get()) {
                    return; // no point in processing an idle event if we are already busy
                }

                if (!busy.compareAndSet(false, true)) {
                    return;
                }

                EventBusImpl.this.idleEventsHandlerChain.handle(te);

                busy.set(false);

            }

            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                if (te.getType().equals(TerminalEvent.TYPE.TIMER)) {
                    return this;
                }
                return null;
            }
            
            public String toString() {
                return "BUS:master_idlEventHandlerBRIDGE";
            }
        };

        this.prependEventHandler(master_idlEventHandlerBRIDGE);
        idleEventsSuppressed = false;

    }

    /* (non-Javadoc)
     * @see dtb.terminalIO.events.IEventBus#disableIdleEvents()
     */
    @Override
    public synchronized void disableIdleEvents() {

        idleEventsSuppressed = true;
        idleEventsEnabled.set(false);

        if (master_idlEventHandlerBRIDGE == null)
            return;

        // The way to suppress idle events is to
        // remove the master_idlEventHandlerBRIDGE handler from
        // eventBus
        this.removeEventHandler(master_idlEventHandlerBRIDGE);

        master_idlEventHandlerBRIDGE = null;

    }

    private EventBus removeIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {
        if (this.idleEventsHandlerChain == null) {
            return this;
        }
        this.idleEventsHandlerChain.removeEventHandler(onIdleEventHandler);
        return this;
    }

    /* (non-Javadoc)
     * @see dtb.terminalIO.events.IEventBus#prependEventHandler(dtb.terminalIO.events.ConditionalEventHandler)
     */
    @Override
    public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
        if (eventHandler instanceof OnIdleEventEventHandler) {

            // TODO: perform a prepend instead!!!
            appendIdleEventHandler((OnIdleEventEventHandler) eventHandler);
            return this;
        }

        if (eventHandler instanceof OnTerminateEventHandler) {

            // TODO: perform a prepend instead!!!
            appendTerminateEventHandler((OnTerminateEventHandler) eventHandler);
            return this;
        }

        // add non idleEventHandler
        eventsHandlerChain.prependEventHandler(eventHandler);
        return this;

    }

    /* (non-Javadoc)
     * @see dtb.terminalIO.events.IEventBus#appendEventHandler(dtb.terminalIO.events.ConditionalEventHandler)
     */
    @Override
    public EventBus appendEventHandler(ConditionalEventHandler eventHandler) {
        
        if (eventHandler == null) {
            throw new NullPointerException("appendEventHandler() error:  eventHandler null!");
        }
        
        if (eventHandler instanceof OnIdleEventEventHandler) {

            return appendIdleEventHandler((OnIdleEventEventHandler) eventHandler);
        }
        
        if (eventHandler instanceof OnTerminateEventHandler) {

            // TODO: perform a prepend instead!!!
            appendTerminateEventHandler((OnTerminateEventHandler) eventHandler);
            return this;
        }
        
        // add non idleEventHandler
        eventsHandlerChain.appendEventHandler(eventHandler);
        return this;
    }

    /* (non-Javadoc)
     * @see dtb.terminalIO.events.IEventBus#removeEventHandler(dtb.terminalIO.events.ConditionalEventHandler)
     */
    @Override
    public EventBus removeEventHandler(ConditionalEventHandler eventHandler) {
        if (eventHandler instanceof OnIdleEventEventHandler) {
            removeIdleEventHandler((OnIdleEventEventHandler) eventHandler);
            return this;
        }
        if (eventHandler instanceof OnTerminateEventHandler) {
            removeTerminateEventHandler((OnTerminateEventHandler) eventHandler);
            return this;
        }
        eventsHandlerChain.removeEventHandler(eventHandler);
        return this;
    }

    public String statsAsStr() {
        StringBuilder sb = new StringBuilder();
        sb.append("EvtBus: Qsize:");
        sb.append(this.queue.size());
        sb.append(" ");
        sb.append((idleEventsHandlerChain == null ? "BUS:idle: empty" : idleEventsHandlerChain.statsAsStr()));
        sb.append(" "+ eventsHandlerChain.statsAsStr());
        return sb.toString();
    }

    @Override
    public boolean hasEventHandlerOfType(Predicate<ConditionalEventHandler> eventHandlerTypeTester) {
        if (eventsHandlerChain == null) {
            return false;
        }
        return eventsHandlerChain.hasEventHandlerOfType(eventHandlerTypeTester);
    }

    @Override
    public ConditionalEventHandler findFirstEventHandlerOfType(
            Predicate<ConditionalEventHandler> eventHandlerTypeTester) {
        if (eventsHandlerChain == null) {
            return null;
        }
        return eventsHandlerChain.findFirstEventHandlerOfType(eventHandlerTypeTester);
    }

}
