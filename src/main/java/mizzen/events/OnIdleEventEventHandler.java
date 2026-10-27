package mizzen.events;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * An EventHandler that can handle OnIdleEvent events.
 * 
 * See the various incarnations of this handler .. 
 * 
 *    {@link OneTime } , 
 *    {@link OneTimeAfterInterval}
 *    {@link RepeatingInterval}
 *    {@link Countdown}
 *
 */
public class OnIdleEventEventHandler extends ConditionalEventHandler.Impl {

    public OnIdleEventEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
        super(canHandlePredicate, handler);
    }

    public OnIdleEventEventHandler(EventHandler handler) {
        super(null, handler);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (te.getType().equals(TerminalEvent.TYPE.TIMER)) {
            if (getCanHandlePredicate() == null) {
                return this;
            }
            return super.canHandle(te);
        }
        return null;
    }

    /**
     * Executes on an IdleEvent once and then removes itself from the eventBus.
     */
    public static class OneTime extends OnIdleEventEventHandler {

        public OneTime(EventHandler handler) {
            super(handler);
        }

        public OneTime(Runnable action) {
            this((te) -> action.run());
        }

        public OneTime(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
            super(canHandlePredicate, handler);
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            try {
                super.handle(te);
            } finally {
                te.getEventBus().removeEventHandler(this);
            }
        }
    }

    static class IntervalPredicate implements Predicate<TerminalEvent<?>> {

        protected long afterTimeInMilliSeconds;

        IntervalPredicate(long intervalInMilliSeconds) {
            this.afterTimeInMilliSeconds = System.currentTimeMillis() + intervalInMilliSeconds;
        }
        
        public void setInterval(long intervalInMilliSeconds) {
            this.afterTimeInMilliSeconds = System.currentTimeMillis() + intervalInMilliSeconds;
        }

        @Override
        public boolean test(TerminalEvent<?> t) {
            return System.currentTimeMillis() > afterTimeInMilliSeconds;
        }

        public String toString() {
            return "IntervalPredicate:"+afterTimeInMilliSeconds;
        }

    }

    /**
     * Executes handler one-time after a defined millisecond time-interval has passed
     */
    public static class OneTimeAfterInterval extends OneTime {

        public OneTimeAfterInterval(EventHandler handler, long intervalInMilliSeconds) {
            super(new IntervalPredicate(intervalInMilliSeconds), handler);
        }

    }

    /**
     * Executes on an IdleEvent repeatedly at every defined interval
     */
    public static class RepeatingInterval extends OnIdleEventEventHandler {

        long intervalInMilliSeconds;

        public RepeatingInterval(long intervalInMilliSeconds, EventHandler handler) {
            super(new IntervalPredicate(intervalInMilliSeconds), handler);
            this.intervalInMilliSeconds = intervalInMilliSeconds;
        }
        
        public void setInterval(long interval) {
            this.intervalInMilliSeconds = interval;
        }

        public long getInterval() {
            return this.intervalInMilliSeconds;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            // First we handle the event
            super.handle(te);
            // Second we build a new Interval predicate specifying when to
            // run next
            this.setCanHandlePredicate(new IntervalPredicate(intervalInMilliSeconds));
        }

    }

    /**
     * Executes on an IdleEvent repeatedly until continueConditional returns false
     */
    public static class RepeatingIntervalWhileCond extends RepeatingInterval {

        Supplier<Boolean> continueConditional;

        public RepeatingIntervalWhileCond(long intervalInMilliSeconds, Supplier<Boolean> continueConditional, EventHandler handler) {
            super(intervalInMilliSeconds, handler);
            this.continueConditional = continueConditional;
        }

        @Override
        public void handle(TerminalEvent<?> te) {

            if ( ! continueConditional.get() ) {
                // conditional did not pass so let's remove ourselves
                te.getEventBus().removeEventHandler(this);
                return;
            }

            // conditional passed to execute handler
            super.handle(te);

        }

    }

    
    /**
     * Executes on an IdleEvent a defined number of times and then removes itself
     * from the eventBus
     * Similar to the RepeatingInterval handler but only runs a limited number of times.
     */
    public static class Countdown extends OnIdleEventEventHandler {

        static class CounterHandler implements EventHandler {
            
            int countdown;
            BiConsumer<TerminalEvent<?>, Integer> handler;
            
            public CounterHandler(int countdown, BiConsumer<TerminalEvent<?>, Integer> handler) {
                super();
                this.countdown = countdown;
                this.handler = handler;
            }

            @Override
            public void handle(TerminalEvent<?> te) {
                handler.accept(te, countdown);
                countdown--;
            }
        }
        
        long intervalInMilliSeconds;

        public Countdown(long intervalInMilliSeconds, int count, BiConsumer<TerminalEvent<?>, Integer> handler) {
            super( new CounterHandler(count, handler));
            this.intervalInMilliSeconds = intervalInMilliSeconds;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            CounterHandler handler = (CounterHandler) this.getHandler();
            if (handler.countdown <= 0) {
                te.getEventBus().removeEventHandler(this);
                return;
            }

            // Now we handle the event
            super.handle(te);

            if (handler.countdown <= 0) {
                te.getEventBus().removeEventHandler(this);
                return;
            }

            // Second we build a new Interval predicate specifying when to
            // run next
            this.setCanHandlePredicate(new IntervalPredicate(intervalInMilliSeconds));
        }

    }
    
    
}
