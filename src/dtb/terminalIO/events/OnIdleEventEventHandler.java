package dtb.terminalIO.events;

import java.util.function.Predicate;

import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandlerImpl;

public class OnIdleEventEventHandler extends ConditionalEventHandlerImpl {

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

	EventBus eventBus;

	public OneTime(EventHandler handler, EventBus eventBus) {
	    super(handler);
	    this.eventBus = eventBus;
	}

	public OneTime(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler, EventBus eventBus) {
	    super(canHandlePredicate, handler);
	    this.eventBus = eventBus;
	}

	@Override
	public void handle(TerminalEvent<?> te) {
	    
	    System.out.println(">>>>>GOiii HERE!!!");
	    
	    getHandler().handle(te);
	    this.eventBus.remove(this);
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

	public OneTimeAfterInterval(EventHandler handler, long interval, EventBus eventBus) {
	    super(new IntervalPredicate(interval), handler, eventBus);
	}
	@Override
	public void handle(TerminalEvent<?> te) {
	    getHandler().handle(te);
	    this.eventBus.remove(this);
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
	    super.handle(te);
	    this.setCanHandlePredicate(  new IntervalPredicate(interval)  );
	}
	
	
    }

}
