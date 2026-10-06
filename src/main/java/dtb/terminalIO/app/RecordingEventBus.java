package dtb.terminalIO.app;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;

import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.OnTerminateEventHandler;
import dtb.terminalIO.events.TerminalEvent;

/**
 * The host's event bus as an embedded app sees it: every handler the app adds goes on the host's
 * bus and is remembered here, so removeAll() takes the app off the bus completely when it closes -
 * key handlers, timers, terminate handlers, whatever it added.
 * <p/>
 * A one-shot task (asynch, asynchAfter) removes itself from the bus once it has run. It goes on the
 * bus inside a wrapper that also forgets it here then, so what is remembered is what is still on
 * the bus (an app running asynch tasks for hours does not pile them up here).
 * <p/>
 * Not seen here: a handler added to the bus an event carries - te.getEventBus() is the host's own
 * bus. A dialog opened that way (MessageBox.show..(te.getEventBus(), ..)) and still open at close
 * keeps its handler on the host's bus.
 */
final class RecordingEventBus extends EventBus.Delegate {

    // handler the app added -> what went on the host's bus for it (the same handler, or a one-shot task's wrapper)
    private final Map<ConditionalEventHandler, ConditionalEventHandler> added = new IdentityHashMap<>();

    RecordingEventBus(EventBus hostBus) {
        super(hostBus);
    }

    @Override
    public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
        delegate.prependEventHandler(track(eventHandler));
        return this;
    }

    @Override
    public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
        delegate.appendEventHandler(track(eventHandler));
        return this;
    }

    @Override
    public EventBus appendIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {
        delegate.appendIdleEventHandler((OnIdleEventEventHandler) track(onIdleEventHandler));
        return this;
    }

    @Override
    public EventBus appendTerminateEventHandler(OnTerminateEventHandler terminateHandler) {
        delegate.appendTerminateEventHandler((OnTerminateEventHandler) track(terminateHandler));
        return this;
    }

    @Override
    public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler) {
        ConditionalEventHandler onBus = added.remove(eventHandler);
        delegate.removeEventHandler(onBus != null ? onBus : eventHandler);
        return this;
    }

    /** take everything the app added off the host's bus */
    void removeAll() {
        for (ConditionalEventHandler onBus : new ArrayList<>(added.values())) {
            delegate.removeEventHandler(onBus);
        }
        added.clear();
    }

    /** how many of the app's handlers are on the host's bus */
    int size() {
        return added.size();
    }

    private ConditionalEventHandler track(ConditionalEventHandler eventHandler) {
        ConditionalEventHandler onBus = eventHandler instanceof OnIdleEventEventHandler.OneTime
                                        ? new ForgetWhenRun((OnIdleEventEventHandler.OneTime) eventHandler)
                                        : eventHandler;
        added.put(eventHandler, onBus);
        return onBus;
    }

    /** a one-shot task as it goes on the host's bus: runs when the task would, and is forgotten here then */
    private final class ForgetWhenRun extends OnIdleEventEventHandler.OneTime {

        private final OnIdleEventEventHandler.OneTime task;

        ForgetWhenRun(OnIdleEventEventHandler.OneTime task) {
            super((EventHandler) task);
            this.task = task;
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            return task.canHandle(te) != null ? this : null;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            added.remove(task);
            super.handle(te);           // runs the task, then takes this wrapper off the bus
        }

        @Override
        public String toString() {
            return "embedded:" + task;
        }
    }
}
