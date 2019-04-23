package dtb.terminalIO.events;

import java.util.function.Predicate;

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

    public static class OneTime extends OnIdleEventEventHandler {

        public OneTime(EventHandler handler) {
            super(handler);
        }

	public OneTime(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
	    super(canHandlePredicate, handler);
	}

	@Override
	public void handle(TerminalEvent<?> te) {
	    getHandler().handle(te);
	    te.getEventBus().removeEventHandler(this);
	}
    }

    static class IntervalPredicate implements Predicate<TerminalEvent<?>> {

	long afterTime;
	
	IntervalPredicate(long interval) {
	    this.afterTime = System.currentTimeMillis() + interval;
	}
	
	@Override
	public boolean test(TerminalEvent<?> t) {
	    return System.currentTimeMillis()>afterTime;
	}
    }

    public static class OneTimeAfterInterval extends OneTime {

	public OneTimeAfterInterval(EventHandler handler, long interval) {
	    super(new IntervalPredicate(interval), handler);
	}
	@Override
	public void handle(TerminalEvent<?> te) {
	    getHandler().handle(te);
	    te.getEventBus().removeEventHandler(this);
	}
    }

    public static class RepeatingInterval extends OnIdleEventEventHandler {

	long interval;
	
	public RepeatingInterval(long interval, EventHandler handler) {
	    super(new IntervalPredicate(interval), handler);
	    this.interval = interval;
	}

	@Override
	public void handle(TerminalEvent<?> te) {
	    // First we handle the event
	    super.handle(te);
	    // Second we build a new Interval predicate specifying when to
	    // run next
	    this.setCanHandlePredicate(  new IntervalPredicate(interval)  );
	}
	
	
    }

}
