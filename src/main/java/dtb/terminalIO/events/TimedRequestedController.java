package dtb.terminalIO.events;

import dtb.terminalIO.events.OnIdleEventEventHandler.OneTimeAfterInterval;

/**
 * Runs an action once requests stop coming - a debouncer, for actions too slow to run on every
 * request (redrawing a preview while the user holds an arrow key, searching while they type).
 * <p/>
 * Call onRequest(eventBus) as often as you like. The action runs once no new request has come for
 * timerDelayMillis (default 250 ms) - so a burst of requests gives one run, after it. While requests
 * keep coming it still runs at least every maxStarvationTime (default 750 ms), so the screen never
 * looks frozen. Override isRequestDifferentFromPrevious to ignore requests that change nothing.
 * <p/>
 * Example (TreeViewTest):
 * <pre>
 *     TimedRequestedController redraw = new TimedRequestedController((te) -> drawSelected(grid, tree.getValue()));
 *     tree.onChange((te) -> redraw.onRequest(te.getEventBus()));     // every move asks; the drawing catches up
 * </pre>
 * The action runs on the event bus (as an idle-time event), not on the caller's thread.
 */
public class TimedRequestedController {

    private static final int DEFAULT_timerDelayMillis = 250;

    private int timerDelayMillis = DEFAULT_timerDelayMillis;
    private int maxStarvationTime = DEFAULT_timerDelayMillis*3;
    private long lastTimeRequestChange = -1;
    private long lastInvocationTime = -1;
    private OneTimeAfterInterval timerInterval;
    private EventHandler onRequestCommand;
    private Object mutex = new Object();

    public TimedRequestedController(EventHandler onChangeAction) {
        this.onRequestCommand = onChangeAction;
    }

    public TimedRequestedController(Runnable onChangeAction) {
        this.onRequestCommand = (te)-> onChangeAction.run();
    }

    /**
     * Override this method if you need something smarter
     * @return true if Request is Different From Previous
     */
    protected boolean isRequestDifferentFromPrevious() {
        return true;
    }

    public int getTimerDelayMillis() {
        return timerDelayMillis;
    }

    public TimedRequestedController setTimerDelayMillis(int timerDelayMillis) {
        this.timerDelayMillis = timerDelayMillis;
        return this;
    }

    public int getMaxStarvationTime() {
        return maxStarvationTime;
    }

    public TimedRequestedController setMaxStarvationTime(int maxStarvationTime) {
        this.maxStarvationTime = maxStarvationTime;
        return this;
    }

    /**
     * Signal a request to do some action.
     */
    public void onRequest(EventBus eventBus) {
        if (!isRequestDifferentFromPrevious())
            return;

        lastTimeRequestChange = System.currentTimeMillis();
        if (timerInterval == null) {
            synchronized(mutex) {
                if (timerInterval == null) {
                    
                    createAndAppendTimerHandler(eventBus);

                }
            }
        }
    }
    
    private void createAndAppendTimerHandler(EventBus eventBus) {
        timerInterval = new OnChangeInterval();
        boolean appendSuccess = eventBus.appendEventHandler(timerInterval) != null;
        if (!appendSuccess) {
            timerInterval = null;  // EventBus could be temporarily disabled ( Terminal resize causes this )
        }
        return;
    }

    private final class OnChangeInterval extends OneTimeAfterInterval {

        OnChangeInterval() {
            super(new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    
                    final long currentTime = System.currentTimeMillis();
                    // check if last request was too recent..
                    if ((currentTime - lastTimeRequestChange) < timerDelayMillis &&
                        (currentTime - lastInvocationTime) < maxStarvationTime) {

                        // last request was too recent and we have not hit starvation,
                        // so lets re-add a new timerHandler onto the eventBus
                        createAndAppendTimerHandler(te.getEventBus());
                        
                        return;
                    }
                    // last request was too long ago .. so finally
                    // lets invoke the request
                    synchronized (mutex) {
                        try {
                             onRequestCommand.handle(te);
                        } finally {
                            timerInterval = null;
                            lastTimeRequestChange = -1;
                            lastInvocationTime = System.currentTimeMillis();
                        }
                    }
                }

            }, TimedRequestedController.this.timerDelayMillis);
        }

        public String toString() {
            return "[OnChangeInterval]";
        }

    }
    
    

}
