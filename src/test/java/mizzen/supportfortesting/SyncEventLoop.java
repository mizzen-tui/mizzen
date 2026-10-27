package mizzen.supportfortesting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandlerContainer;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.TerminalEvent;

/**
 * A synchronous, single-threaded stand-in for EventBusImpl, for tests.
 *
 * <p/>
 *
 * Raised events are queued and, on {@link #deliver} / {@link #drain}, handed to a single root
 * handler (usually the application's root FocusManager) in order - the way the real bus does once
 * nothing earlier on the bus claims them. Handlers prepended to the bus (e.g. a modal dialog's) get
 * each event before the root. After each event its {@code then(..)} callback runs; when
 * the queue is empty, one-shot idle tasks queued via {@code asynch(..)} run. Every raised event is
 * kept in {@link #history} so tests can assert on side-effect events (scroll-to-view, canvas refresh..).
 */
public class SyncEventLoop extends EventBus.STUBBED {

    private final Deque<TerminalEvent<?>> queue = new ArrayDeque<>();
    private final List<OnIdleEventEventHandler.OneTime> oneTimeTasks = new ArrayList<>();
    public final List<TerminalEvent<?>> history = new ArrayList<>();
    private ConditionalEventHandler root;
    // handlers put in front of the root (e.g. a modal dialog's ModalEventHandler), first one first
    private final List<ConditionalEventHandler> inFront = new ArrayList<>();

    public SyncEventLoop setRoot(ConditionalEventHandler root) {
        this.root = root;
        return this;
    }

    @Override
    public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
        inFront.add(0, eventHandler);
        return this;
    }

    @Override
    public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler) {
        inFront.remove(eventHandler);
        return this;
    }

    @Override
    public EventBus raiseEvent(TerminalEvent<?> te) {
        queue.add(te);
        history.add(te);
        return this;
    }

    @Override
    public EventBus appendIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {
        // only one-shot tasks (asynch / asynchAfter) are run; repeating timers are ignored
        if (onIdleEventHandler instanceof OnIdleEventEventHandler.OneTime) {
            oneTimeTasks.add((OnIdleEventEventHandler.OneTime) onIdleEventHandler);
        }
        return this;
    }

    /** raise the event and process everything that follows from it */
    public void deliver(TerminalEvent<?> te) {
        raiseEvent(te);
        drain();
    }

    /** process queued events, their then(..) callbacks and asynch tasks until nothing is left */
    public void drain() {
        int guard = 0;
        while (!queue.isEmpty() || !oneTimeTasks.isEmpty()) {
            if (++guard > 10_000) {
                StringBuilder last = new StringBuilder();
                for (int k = Math.max(0, history.size() - 8); k < history.size(); k++) {
                    TerminalEvent<?> e = history.get(k);
                    last.append("\n  ").append(e.getType()).append(" ").append(e.getClass().getSimpleName())
                        .append(" data=").append(String.valueOf(e.getEventData())).append(" source=").append(String.valueOf(e.getSource()));
                }
                throw new IllegalStateException("SyncEventLoop: events keep generating events (loop?) - last events:" + last);
            }
            if (!queue.isEmpty()) {
                TerminalEvent<?> te = queue.poll();
                te.setEventBus(this);
                ConditionalEventHandler first = null;
                for (ConditionalEventHandler h : new ArrayList<>(inFront)) {
                    if (h.canHandle(te) != null) {
                        first = h;
                        break;
                    }
                }
                if (first != null) {
                    first.handle(te);
                } else if (root != null && root.canHandle(te) != null) {
                    root.handle(te);
                }
                te.onEventProcessed();
                continue;
            }
            List<OnIdleEventEventHandler.OneTime> tasks = new ArrayList<>(oneTimeTasks);
            oneTimeTasks.clear();
            for (OnIdleEventEventHandler.OneTime task : tasks) {
                TerminalEvent<Object> tick = new TerminalEvent<Object>(this, TerminalEvent.TYPE.TIMER, null);
                tick.setEventBus(this);
                task.handle(tick);
            }
        }
    }

    public List<TerminalEvent<?>> raised(TerminalEvent.TYPE type) {
        return history.stream().filter(te -> te.getType() == type).collect(Collectors.toList());
    }

    public void clearHistory() {
        history.clear();
    }
}
