package mizzen.components.input;

import mizzen.components.HasName;
import mizzen.events.ChainedEventHandler;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventHandler;
import mizzen.events.EventHandlerContainer;
import mizzen.events.Registration;
import mizzen.events.TerminalEvent;

/**
 * Everyone listening to one component's changes - what its onChange adds to: any number of listeners,
 * run in the order added, so an app and a form (or anything else) can listen side by side. A component
 * holds one:
 * <pre>
 *     private final OnChangeListeners&lt;String&gt; changeListeners = new OnChangeListeners&lt;String&gt;(this);
 *
 *     public Registration onChange(EventHandler listener) {
 *         return changeListeners.add(getChainedEventHandler(), listener);
 *     }
 * </pre>
 * On a chain, it claims the component's VALUE_CHANGED events once and runs every listener; it is on
 * the chain while it has listeners. One handler for all of them: a chain runs only the first handler
 * that claims an event. A component that hears its changes itself adds without a chain and calls
 * handle(..) or fire(..).
 * <p/>
 * add(.., null) - onChange(null) - takes every listener out. A listener that throws stops the rest:
 * the error isn't hidden.
 * <p/>
 * The listeners are a handle-all ChainedEventHandler (as the event bus keeps its idle handlers): each
 * runs in turn; one added or taken out while they run counts from the next change.
 */
public class OnChangeListeners<V> extends HasValue.OnValueChangedEventHandler<V> {

    private ChainedEventHandler listeners = ChainedEventHandler.useHandleAllStrategy("onChange");   // a fresh one on clear()
    private int count;                                   // listeners in it (the chain doesn't say)
    private EventHandlerContainer chain;                 // where this is, while it is on one

    /** source: the component whose changes these are (null: one that only fires by hand) */
    public OnChangeListeners(HasValue<V> source) {
        super(source);
    }

    /** a listener, heard through the component's chain (put on it with the first listener) */
    public Registration add(EventHandlerContainer componentChain, EventHandler listener) {
        if (listener == null) {
            clear();
            return Registration.NONE;
        }
        if (componentChain == null) {
            throw new NullPointerException("OnChangeListeners.add: no event handler chain for " + HasName.UTIL.getName(targetSource));
        }
        if (chain == null) {
            chain = componentChain;
            chain.appendEventHandler(this);
        }
        return addListener(listener);
    }

    /** a listener, for a component that calls handle(..) or fire(..) itself */
    public Registration add(EventHandler listener) {
        if (listener == null) {
            clear();
            return Registration.NONE;
        }
        return addListener(listener);
    }

    private Registration addListener(EventHandler listener) {
        if (getName() == null && targetSource != null) {
            HasName.UTIL.decorateWithParentsNamePrefix(this, targetSource);
        }
        ConditionalEventHandler always = new ConditionalEventHandler.Impl((te) -> true, listener);
        ChainedEventHandler joined = listeners;
        joined.appendEventHandler(always);
        count++;
        boolean[] in = { true };
        return () -> {
            if (in[0] && joined == listeners) {          // the second time, or after clear(): nothing left to take out
                in[0] = false;
                listeners.removeEventHandler(always);
                if (--count == 0) {
                    leaveChain();
                }
            }
        };
    }

    /** every listener out (and off the chain) */
    public void clear() {
        listeners = ChainedEventHandler.useHandleAllStrategy("onChange");
        count = 0;
        leaveChain();
    }

    private void leaveChain() {
        if (chain != null) {
            chain.removeEventHandler(this);
            chain = null;
        }
    }

    public boolean isEmpty() {
        return count == 0;
    }

    /** claims the component's change only while someone listens */
    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        return count == 0 ? null : super.canHandle(te);
    }

    @Override
    public void valueChanged(TerminalEvent<?> te, HasValue<V> source) {
        fire(te);
    }

    /** every listener hears te - for a component that fires its changes by hand */
    public void fire(TerminalEvent<?> te) {
        listeners.handle(te);
    }
}
