package dtb.terminalIO.events;

import dtb.terminalIO.events.OnIdleEventEventHandler.OneTimeAfterInterval;
import dtb.terminalIO.util.Logging;

public abstract class AbstractTimedRequestedController {

    private static final int DEFAULT_timerDelayMillis = 250;

    private int timerDelayMillis = DEFAULT_timerDelayMillis;
    private long lastTimeRequestChange = -1;
    private OneTimeAfterInterval timerInterval;
    private Runnable onRequestCommand;
    private Object mutex = new Object();

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
            synchronized(mutex) {
                if (timerInterval == null) {
                    
                    createAndAppendTimerHandler(eventBus);

                }
            }
        }
    }
    
    private void createAndAppendTimerHandler(EventBus eventBus) {
        timerInterval = new OnChangeInterval();
        //logBusStats("add(pre ) ", te.getEventBus());
        boolean appendSuccess = eventBus.appendEventHandler(timerInterval) != null;
        if (!appendSuccess) {
            timerInterval = null;  // EventBus could be temporarily disabled ( Terminal resize causes this )
        }
        // logBusStats("add(post) ", te.getEventBus()); 
        
        return;
    }

    private static void logBusStats(String prefix, EventBus eventBus) {
        String busStats = ((EventBusImpl)eventBus).statsAsStr();
        Logging.info(prefix+":"+busStats);

    }
    
    private final class OnChangeInterval extends OneTimeAfterInterval {

        OnChangeInterval() {
            super(new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    
                    //logBusStats("handle()1", te.getEventBus()); 
                    final long currentTime = System.currentTimeMillis();
                    // check if last request was too recent..
                    if ((currentTime - lastTimeRequestChange) < timerDelayMillis) {

                        // last request was too recent so lets re-add the
                        // a new timerHandler onto the eventBus
                        createAndAppendTimerHandler(te.getEventBus());
                        
                        return;
                    }
                    // last request was not too long ago .. so finally
                    // lets invoke the request
                    synchronized (mutex) {
                        try {
                             onRequestCommand.run();
                        } finally {
                            timerInterval = null;
                            lastTimeRequestChange = -1;
                        }
                    }
                }

            }, AbstractTimedRequestedController.this.timerDelayMillis);
        }

        public String toString() {
            return "[OnChangeInterval]";
        }

    }

}
