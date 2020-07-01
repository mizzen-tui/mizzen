package dtb.terminalIO.events;

import dtb.terminalIO.events.OnIdleEventEventHandler.OneTimeAfterInterval;

public abstract class AbstractTimedRequestedController {

    private static final int DEFAULT_timerDelayMillis = 250;

    private int timerDelayMillis = DEFAULT_timerDelayMillis;
    private long lastTimeRequestChange = -1;
    private OneTimeAfterInterval timerInterval;
    private Runnable onRequestCommand;

    public AbstractTimedRequestedController(Runnable onChangeAction) {
        this.onRequestCommand = onChangeAction;
    }

    abstract protected boolean isRequestDifferentFromPrevious();

    public int getTimerDelayMillis() {
        return timerDelayMillis;
    }

    public void setTimerDelayMillis(int timerDelayMillis) {
        this.timerDelayMillis = timerDelayMillis;
    }

    /**
     * Signal a request to do some action.
     */
    public void onRequest(EventBus eventBus) {
        if (!isRequestDifferentFromPrevious())
            return;

        lastTimeRequestChange = System.currentTimeMillis();
        if (timerInterval == null) {
            timerInterval = new OnChangeInterval();
            eventBus.appendEventHandler(timerInterval);
        }
    }

    private final class OnChangeInterval extends OneTimeAfterInterval {

        OnChangeInterval() {
            super(new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    final long currentTime = System.currentTimeMillis();
                    // check if last request was too recent..
                    if ((currentTime - lastTimeRequestChange) < timerDelayMillis) {
                        // last request was too recent so lets re-add the
                        // handler onto the eventBus
                        timerInterval = new OnChangeInterval();
                        te.getEventBus().appendEventHandler(timerInterval);
                        return;
                    }
                    // last request was not too long ago .. so finally
                    // lets invoke the request
                    onRequestCommand.run();
                    timerInterval = null;
                }

            }, timerDelayMillis);
        }
    }

}
