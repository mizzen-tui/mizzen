package dtb.terminalIO.app;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Supplier;

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
 * Each handler goes on the host's bus inside a forwarder that, while the handler runs, makes this
 * bus the event's bus (te.getEventBus()). So what a handler adds through the event - a dialog's
 * handler, say: MessageBox.show..(te.getEventBus(), ..) - is remembered too, and a handler that
 * removes itself (a one-shot task once it ran, a dialog when it closes) is forgotten here as it goes.
 * <p/>
 * Not covered: events the host hands to the app's components directly - keys by focus (KeyRouter),
 * clicks by position (MouseEventOrchestrator). Those carry the host's own bus.
 */
final class RecordingEventBus extends EventBus.Delegate {

    // handler the app added -> its forwarder on the host's bus
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
        ConditionalEventHandler onBus = added.get(eventHandler);
        if (onBus == null) {
            onBus = forwarderFor(eventHandler);
            added.put(eventHandler, onBus);
        }
        return onBus;
    }

    // the host's bus sorts handlers by type (idle, terminate, the rest): the forwarder keeps the type
    private ConditionalEventHandler forwarderFor(ConditionalEventHandler handler) {
        if (handler instanceof OnIdleEventEventHandler) {
            return new IdleForwarder(handler);
        }
        if (handler instanceof OnTerminateEventHandler) {
            return new TerminateForwarder(handler);
        }
        return new Forwarder(handler);
    }

    /**
     * Run work - one of the app's handlers - with this bus as the event's bus, then give the event
     * its own bus back.
     * <p/>
     * Why: an event carries the bus it was raised on, the host's. Handlers commonly use that bus to
     * add more handlers - MessageBox.showInfoDialog(te.getEventBus(), ..) puts the dialog's handler
     * on it. Done on the host's bus directly, we would never see it, and close() would leave it
     * behind: an orphaned dialog handler swallowing every key in the host. While the app's handler
     * runs, te.getEventBus() is this bus instead, so what it adds is recorded like everything else -
     * and what removes itself (a dialog closing, a one-shot task) is forgotten here as it goes.
     * <p/>
     * The bus is put back afterwards (the host's handlers further along see the host's bus), even if
     * the handler throws.
     */
    private <R> R withThisBus(TerminalEvent<?> te, Supplier<R> work) {
        EventBus eventsOwnBus = te.getEventBus();
        te.setEventBus(this);
        try {
            return work.get();
        } finally {
            te.setEventBus(eventsOwnBus);
        }
    }

    private EventHandler canHandle(ConditionalEventHandler handler, TerminalEvent<?> te) {
        EventHandler found = withThisBus(te, () -> handler.canHandle(te));
        return found == null ? null : (e) -> handle(found, e);
    }

    private void handle(EventHandler handler, TerminalEvent<?> te) {
        withThisBus(te, () -> {
            handler.handle(te);
            return null;
        });
    }

    private final class Forwarder implements ConditionalEventHandler {
        private final ConditionalEventHandler handler;

        Forwarder(ConditionalEventHandler handler) {
            this.handler = handler;
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            return RecordingEventBus.this.canHandle(handler, te);
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            RecordingEventBus.this.handle(handler, te);
        }

        @Override
        public String toString() {
            return "embedded:" + handler;
        }
    }

    private final class IdleForwarder extends OnIdleEventEventHandler {
        private final ConditionalEventHandler handler;

        IdleForwarder(ConditionalEventHandler handler) {
            super(handler);
            this.handler = handler;
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            return RecordingEventBus.this.canHandle(handler, te);
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            RecordingEventBus.this.handle(handler, te);
        }

        @Override
        public String toString() {
            return "embedded:" + handler;
        }
    }

    private final class TerminateForwarder extends OnTerminateEventHandler {
        private final ConditionalEventHandler handler;

        TerminateForwarder(ConditionalEventHandler handler) {
            super(() -> {});
            this.handler = handler;
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            return RecordingEventBus.this.canHandle(handler, te);
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            RecordingEventBus.this.handle(handler, te);
        }

        @Override
        public String toString() {
            return "embedded:" + handler;
        }
    }
}
