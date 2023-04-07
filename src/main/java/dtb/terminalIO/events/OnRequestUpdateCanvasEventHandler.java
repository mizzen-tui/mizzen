package dtb.terminalIO.events;

import java.util.function.Predicate;

import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;

/**
 * Handle Canvas update requests.
 * 
 * <p/>
 * 
 * A big part of this handler's job ( along with updating the canvas ) is to slow
 * down frequent canvas updates..  This is done by delegating to an {@link AbstractTimedRequestedController}
 * which will only invoke the canvasUpdater once the rate of requests have slowed down.
 * 
 * <p/>
 * 
 * This is to avoid executing canvas updates in scenarios where canvas updates occur in rapid fire which
 * will slow down the overall performance of the application.
 * 
 * @author dbalme
 *
 */
public class OnRequestUpdateCanvasEventHandler implements ConditionalEventHandler {

        static final Predicate<TerminalEvent<?>> canHandleCanvasUpdate = new IS_Event_TYPE(TerminalEvent.TYPE.REQUEST_UPDATE_CANVAS);

        static final int MAX_WAIT = 2500;
        static final int MIN_TIMER_DELAY = 50;
        static final int DEFAULT_TIMER_DELAY = MIN_TIMER_DELAY;
        
        AbstractTimedRequestedController timedOnChangeRequestController = null;

        Runnable timedCanvasUpdater = null;


        public OnRequestUpdateCanvasEventHandler(Runnable canvasUpdater) {

            timedOnChangeRequestController = buildTimedUpdateCanvasController(canvasUpdater);
            timedOnChangeRequestController.setTimerDelayMillis(DEFAULT_TIMER_DELAY);
        }

        AbstractTimedRequestedController buildTimedUpdateCanvasController(Runnable canvasUpdater) {
            
            timedCanvasUpdater = ()-> {
                
                // update canvas and time how long it takes
                long elapsed = ElapsedTime.onRunOf(canvasUpdater).elapsed();

                //Logging.info("OnRequestUpdateCanvasEventHandler.timedCanvasUpdater - elapsed: " + elapsed + "  tmrDELAY:  " + this.timedOnChangeRequestController.getTimerDelayMillis());
                //Logging.info(" ");
                
                if (OnRequestUpdateCanvasEventHandler.this.timedOnChangeRequestController != null &&
                    elapsed <= MAX_WAIT &&  // ignore really long times .. hopefully just an anomaly
                    (elapsed)>OnRequestUpdateCanvasEventHandler.this.timedOnChangeRequestController.getTimerDelayMillis()) {
                    
                    OnRequestUpdateCanvasEventHandler.this.timedOnChangeRequestController.setTimerDelayMillis(Math.max((int)elapsed,MIN_TIMER_DELAY));
                    int maxStarvationDelayTime = Math.min((int)elapsed * 2,1500);
                    OnRequestUpdateCanvasEventHandler.this.timedOnChangeRequestController.setMaxStarvationTime(maxStarvationDelayTime);
                    
                    
                } else {
                    if (elapsed*2 < OnRequestUpdateCanvasEventHandler.this.timedOnChangeRequestController.getTimerDelayMillis()) {
                        OnRequestUpdateCanvasEventHandler.this.timedOnChangeRequestController.setTimerDelayMillis(Math.max((int)elapsed,MIN_TIMER_DELAY));
                        int maxStarvationDelayTime = Math.min((int)elapsed * 2,MAX_WAIT);
                        OnRequestUpdateCanvasEventHandler.this.timedOnChangeRequestController.setMaxStarvationTime(maxStarvationDelayTime);
                    }
                }
            };
            
            return new AbstractTimedRequestedController(timedCanvasUpdater) {
                @Override
                protected boolean isRequestDifferentFromPrevious() {
                    return true;
                }
                
            };
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if (!canHandleCanvasUpdate.test(te)) {
                return null; // null means - can't handle.
            }
            return this;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            timedOnChangeRequestController.onRequest(te.getEventBus());
        }

}
