package dtb.terminalIO.supportfortesting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;

/**
 * A synchronous, single-threaded stand-in for EventBusImpl, for tests.
 *
 * <p/>
 *
 * Raised events are queued and, on {@link #deliver} / {@link #drain}, handed to a single root
 * handler (usually the application's root FocusManager) in order - the way the real bus does once
 * nothing earlier on the bus claims them. After each event its {@code then(..)} callback runs; when
 * the queue is empty, one-shot idle tasks queued via {@code asynch(..)} run. Every raised event is
 * kept in {@link #history} so tests can assert on side-effect events (scroll-to-view, canvas refresh..).
 */
public class SyncEventLoop extends EventBus.STUBBED {

    private final Deque<TerminalEvent<?>> queue = new ArrayDeque<>();
    private final List<OnIdleEventEventHandler.OneTime> oneTimeTasks = new ArrayList<>();
    public final List<TerminalEvent<?>> history = new ArrayList<>();
    private ConditionalEventHandler root;

    public SyncEventLoop setRoot(ConditionalEventHandler root) {
        this.root = root;
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
                throw new IllegalStateException("SyncEventLoop: events keep generating events (loop?)");
            }
            if (!queue.isEmpty()) {
                TerminalEvent<?> te = queue.poll();
                te.setEventBus(this);
                if (root != null && root.canHandle(te) != null) {
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
