package dtb.terminalIO.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import dtb.terminalIO.components.HasName;
import dtb.terminalIO.util.Logging;

/**
 * 
 * A ChainedEventHandler maintains a list of EventHandler objects - and more
 * specifically a list of ConditionalEventHandler objects.
 * 
 * <p/>
 * 
 * This object is an EventHandler itself - but it delegates the handling of an
 * event to one or all of its internally registered eventHandlers.
 * 
 * <p/>
 * 
 * This object operates in 2 behaviour styles when delegating to its internal
 * eventHandlers
 * 
 * <p/>
 *
 * <pre>
 * 1) Handle-All
 *      - when an event comes in 
 *         - loop thru all internal eventHandlers and
 *           tell each handler to handle the event
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
 *           NEEDS IMPROVEMENT :  Need to have a concept of  : NOTE:  This is almost fully implemented
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
public class ChainedEventHandler implements EventHandler, EventHandlerContainer, HasName<ChainedEventHandler> {

    
    private String name;
    
    private List<ConditionalEventHandler> eventHandlers = new ArrayList<ConditionalEventHandler>();

    private Collection<ConditionalEventHandler> preppendList = new ArrayList<>();
    private Collection<ConditionalEventHandler> appendList = new ArrayList<>();
    private Collection<ConditionalEventHandler> removeList = new ArrayList<>();

    // this dictates how events are handled - either chain-of-responsibility
    // pattern or apply event to all handlers that can
    // accept the event type
    private EventHandler handleEventStrategy;
    private boolean canHandleIdle = true;

    private EventHandler notfound_event_handler = EventHandler.NULL_HANDLER;

    private ChainedEventHandler(String name) {
        handleEventStrategy = chainOfResponsibilityHandler;
        this.setName(name);
    }

    private boolean isChainOfResponsibility() {
        return handleEventStrategy == chainOfResponsibilityHandler;
    }

    /**
     * Builds a {@link ChainedEventHandler} that delegates incoming events to its child
     * event handlers in a Child-of-Responsibility style.
     * 
     * <p/>
     * 
     * - Meaning that: it passes the incoming event only to the first child handler
     *   that claims it can handle the event.
     * 
     * <p/>
     * 
     * Useful for processing KeyEvents
     * 
     * @return ChainedEventHandler
     */
    public static ChainedEventHandler useChainOfResponsibilityStrategy(String name) {
        ChainedEventHandler chainedEventHandler = new ChainedEventHandler(name);
        chainedEventHandler.canHandleIdle = false;
        return chainedEventHandler;
    }

    /**
     * For details see {@link #useChainOfResponsibilityStrategy(String name)}
     * @return
     */
    public static ChainedEventHandler useChainOfResponsibilityStrategy() {
        return useChainOfResponsibilityStrategy("chainOfResponsibility");
    }

    /**
     * @see {@link #useChainOfResponsibilityStrategy(String)}
     */
    public static ChainedEventHandler useChainOfResponsibilityStrategy(Object parent) {
        return useChainOfResponsibilityStrategy(HasName.UTIL.getName(parent)+"chainOfResponsibility");
    }

    /**
     * Builds a ChainedEventHandler that delegates incoming events to all its child
     * event handlers that say they can handle the event. Useful for processing
     * OnIdleEvents
     * 
     * @return ChainedEventHandler
     */
    public static ChainedEventHandler useHandleAllStrategy(String name) {
        ChainedEventHandler chainedEventHandler = new ChainedEventHandler(name);
        chainedEventHandler.handleEventStrategy = chainedEventHandler.handleAllHandler;
        return chainedEventHandler;
    }

    public static ChainedEventHandler useHandleAllStrategy() {
        return useHandleAllStrategy("handleAll");
    }

    /** a chain-of-responsibility chain; notfound_event_handler gets the events no handler in it takes */
    public ChainedEventHandler(EventHandler notfound_event_handler) {
        this("ChainedEventHandler");
        this.notfound_event_handler = notfound_event_handler;
    }

    private void assertHandlerType(ConditionalEventHandler eventHandler) {
        if (this.canHandleIdle) {
            return;
        }
        if (eventHandler instanceof OnIdleEventEventHandler) {
            throw new IllegalArgumentException("Cannot attach an OnIdleEventHandler to this type of ChainedEventHandler!");
        }
    }
    
    private void notNull(String errMsg, Object o) {
        if (o == null) {
            throw new IllegalStateException("Null object! : " + errMsg);
        }
    }
    
    @Override
    public ChainedEventHandler prependEventHandler(ConditionalEventHandler eventHandler) {
        
        notNull("prependEventHandler()", eventHandler);
        
        if (!eventHandlers.contains(eventHandler)) {
            synchronized (preppendList) {
                if (!preppendList.contains(eventHandler)) {
                    assertHandlerType(eventHandler);
                    preppendList.add(eventHandler);
                }
            }
        }
        return this;
    }

    public ChainedEventHandler appendEventHandler(ConditionalEventHandler eventHandler) {
        
        notNull("appendEventHandler()", eventHandler);
        
        if (!eventHandlers.contains(eventHandler)) {
            synchronized (appendList) {
                if (!appendList.contains(eventHandler)) {
                    assertHandlerType(eventHandler);
                    appendList.add(eventHandler);
                }
            }
        }
        return this;
    }

    public ChainedEventHandler removeEventHandler(ConditionalEventHandler eventHandler) {
        
        notNull("removeEventHandler()", eventHandler);
        
        if (eventHandlers.contains(eventHandler)) {
            synchronized (removeList) {
                if (!removeList.contains(eventHandler)) {
                    removeList.add(eventHandler);
                }
            }
        } else {
            if (this.appendList.contains(eventHandler)) {
                synchronized (appendList) {
                    if (appendList.contains(eventHandler)) {
                        appendList.remove(eventHandler);
                    }
                }
            } else {
                if ( this.preppendList.contains(eventHandler) ) {
                    synchronized (preppendList) {
                        if (preppendList.contains(eventHandler)) {
                            preppendList.remove(eventHandler);
                        }
                    }
                }
            }
        }
        return this;
    }

    public ChainedEventHandler addEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate,
            EventHandler eventHandler) {
        return appendEventHandler(new ConditionalEventHandler.Impl(canHandlePredicate, eventHandler));
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        // while events are 'in-play' (being handled) we cannot add
        // or remove events - so do it now - before we handle in-coming event
        adjustEventHandlerListIfNeeded();

        // at this point event is "in-play" (being handled)
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
        if (appendList.size() > 0) {
            synchronized (appendList) {
                for (ConditionalEventHandler eventHandler : this.appendList) {
                    if (!eventHandlers.contains(eventHandler)) {
                        eventHandlers.add(eventHandler);
                    }
                }
                this.appendList.clear();
            }
        }
        // add from preppendList
        if (preppendList.size() > 0) {
            synchronized (preppendList) {
                for (ConditionalEventHandler eventHandler : this.preppendList) {
                    if (!eventHandlers.contains(eventHandler)) {
                        eventHandlers.add(0, eventHandler);
                    }
                }
                this.preppendList.clear();
            }
        }
    }

    public EventHandler findRequestHandler(TerminalEvent<?> te) {
        EventHandler targetHandler = null;
        for (ConditionalEventHandler eH : eventHandlers) {
            eventTracerLogger.tracePreTest(eH, te);
            EventHandler foundHandler = eH.canHandle(te);
            eventTracerLogger.tracePostTest(eH, te, foundHandler!=null);
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
                eventTracerLogger.tracePreTest(eH, te);
                EventHandler foundHandler = eH.canHandle(te);
                eventTracerLogger.tracePostTest(eH, te, foundHandler != null);
                if (foundHandler != null) {
                    foundHandler.handle(te);
                }
            }
        }
    };

    /**
     * An adapter that makes this ChainedHandler look like a {@link ConditionalEventHandler}
     * @return
     */
    public ConditionalEventHandler asConditionalEventHandler() {

        // make sure our current handler list is up to date
        // hmmm .. will I need to do this in the test() function below? Could be
        // expensive to do that..
        this.adjustEventHandlerListIfNeeded();

        ConditionalEventHandler.Impl eventHandler = new ConditionalEventHandler.Impl(new Predicate<TerminalEvent<?>>() {

            @Override
            public boolean test(TerminalEvent<?> te) {

                adjustEventHandlerListIfNeeded();

                if (isChainOfResponsibility()) {
                    return ChainedEventHandler.this.findRequestHandler(te) != null;
                }
                // handle all strategy -- so return true if we have at least one
                // handler
                return eventHandlers.size() > 0;
            }

        }, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                // not happy that I am doing another findRequestHandler()
                // - once in test() and then again here
                ChainedEventHandler.this.handle(te);
            }

        });

        HasName.UTIL.decorateWithParentsNamePrefix(eventHandler, ChainedEventHandler.this);
        
        return eventHandler;
    }

    public String statsAsStr() {
        return this.name+" [size = " + eventHandlers.size() +
                "  p : "+preppendList.size() +
                "  a : "+appendList.size() +
                "  r : "+removeList.size() +
                "]";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ChainedEventHandler setName(String name) {
        this.name = name;
        return this;
    }

    public boolean hasEventHandlerOfType(Predicate<ConditionalEventHandler> eventHandlerTypeTester) {
        return eventHandlers.stream().filter(eventHandlerTypeTester).findAny().isPresent();
    }

    public String toString() {
        return statsAsStr();
    }

    public ConditionalEventHandler findFirstEventHandlerOfType(
            Predicate<ConditionalEventHandler> eventHandlerTypeTester) {
        
        ConditionalEventHandler found = preppendList.stream().filter(eventHandlerTypeTester).findFirst().orElse(null);
        if (found != null) {
            return found;
        }
        found = eventHandlers.stream().filter(eventHandlerTypeTester).findFirst().orElse(null);
        if (found != null) {
            return found;
        }
        found = appendList.stream().filter(eventHandlerTypeTester).findFirst().orElse(null);
        if (found != null) {
            return found;
        }
        return null;

    }
    
    public static boolean TRACE_AND_LOG_EVENT_HANDLERS = false;     // true: logs every handler asked, for every event (huge logs)
    private EventTracerLogger eventTracerLogger = new EventTracerLogger();
    private static Set<TerminalEvent.TYPE> tracerEventTypeBlackList = new HashSet<TerminalEvent.TYPE>();
    static {
        tracerEventTypeBlackList.add(TerminalEvent.TYPE.RAW_MOUSE);
        tracerEventTypeBlackList.add(TerminalEvent.TYPE.TIMER);
        tracerEventTypeBlackList.add(TerminalEvent.TYPE.REQUEST_UPDATE_CANVAS);
        tracerEventTypeBlackList.add(TerminalEvent.TYPE.REQUEST_SCROLL_TO_VIEW);
        tracerEventTypeBlackList.add(TerminalEvent.TYPE.REQUEST_SCROLL_TO_VIEW);
    }
    
    private static class EventTracerLogger {
        void tracePreTest(ConditionalEventHandler handler, TerminalEvent<?> te) {
            if ( ! TRACE_AND_LOG_EVENT_HANDLERS || !te.getType().equals(TerminalEvent.TYPE.KEYPRESS) || (tracerEventTypeBlackList.contains(te.getType()))) {
                return;
            }
         //   Logging.info("PreTest:"+HasName.UTIL.getName(handler) + " --- "+HasName.UTIL.getName(te));
        }
        void tracePostTest(ConditionalEventHandler handler, TerminalEvent<?> te, boolean result) {
            if ( ! TRACE_AND_LOG_EVENT_HANDLERS || !te.getType().equals(TerminalEvent.TYPE.KEYPRESS)  || (tracerEventTypeBlackList.contains(te.getType()))) {
                return;
            }
            if (result) {
                Logging.info("!!");
            }
            Logging.info((result?"TRUE : ":"FALSE: ")+HasName.UTIL.getName(te)+" -- "+HasName.UTIL.getName(handler));
        }
    }

}
