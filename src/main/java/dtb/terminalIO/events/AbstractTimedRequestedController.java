package dtb.terminalIO.events;

import dtb.terminalIO.events.OnIdleEventEventHandler.OneTimeAfterInterval;

public abstract class AbstractTimedRequestedController {

//    private static final int timerDelayMillis = 500;
    private static final int timerDelayMillis = 250;

    private long lastTimeRequestChange = -1;
    private OneTimeAfterInterval timerInterval;
    private Runnable onRequestCommand;

    public AbstractTimedRequestedController(Runnable onChangeAction) {
	this.onRequestCommand = onChangeAction;
    }

    abstract protected boolean isRequestDifferentFromPrevious();

    public void onRequest(EventBus eventBus) {
	if ( ! isRequestDifferentFromPrevious())
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
	            if ((currentTime - lastTimeRequestChange) < timerDelayMillis) {
	        	timerInterval = new OnChangeInterval();
	    	        te.getEventBus().appendEventHandler(timerInterval);
	    	        return;
	            }
	            onRequestCommand.run();
	            timerInterval = null;
		}
		
	    }, timerDelayMillis);
	}
  }
    
}
