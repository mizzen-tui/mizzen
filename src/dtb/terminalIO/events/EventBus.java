package dtb.terminalIO.events;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandler;
import dtb.terminalIO.events.TerminalEvent.TYPE;

public class EventBus {

    private ChainedEventHandler handler = ChainedEventHandler.useChainOfResponsibilityStrategy();

    private ChainedEventHandler idleEventsHandler;

    private BlockingQueue<TerminalEvent<?>> queue = new ArrayBlockingQueue<TerminalEvent<?>>(100);

    private AtomicBoolean stopConsuming = new AtomicBoolean(false);

    private Runnable consumerTask;

    public EventBus() {

	consumerTask = new Runnable() {
	    @Override
	    public void run() {
		while (isActive()) {
		    try {
			TerminalEvent<?> te = queue.poll(1, TimeUnit.SECONDS);
			if (te != null) {
			    handler.handle(te);
			}
		    } catch (InterruptedException e) {
			handler.handle(new TerminalEvent<Exception>(EventBus.this, TerminalEvent.TYPE.EXCEPTION, e));
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
	queue.add(te);
	return this;
    }

    public <T> EventBus addEvent(Object source, TYPE type, T eventData) {
	TerminalEvent<T> te = new TerminalEvent<T>(source, type, eventData);
	return addEvent(te);
    }

    public EventBus addIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {
	if (this.idleEventsHandler == null) {
	    synchronized (this) {
		if (this.idleEventsHandler == null) {
		    // build a handler for all idle events
		    this.idleEventsHandler = ChainedEventHandler.useHandleAllStrategy();

		    // now build a handler on the main eventbus to dispatch to the 
		    // the newly built idleEventsHandler
		    this.addEventHandler(new ConditionalEventHandler() {

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
		    });
		}
	    }
	}
	this.idleEventsHandler.addEventHandler(onIdleEventHandler);
	return this;
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

}
