package dtb.terminalIO.events;

import java.util.function.Predicate;

import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;

/**
 * Handle Canvas update requests.
 * 
 * <p/>
 * 
 * A big part of this handler's job ( along with updating the canvas ) is to slow
 * down frequent canvas updates..  This is done by delegating to an AbstractTimedRequestedController
 * which will only invoke the canvasUpdater once the requests have slowed down.
 * 
 * <p/>
 * 
 * This is to avoid executing canvas updates in scenarios where canvas updates occur in rapid fire which
 * will slow down the over performance of the application.
 * 
 * @author dbalme
 *
 */
public class OnRequestUpdateCanvasEventHandler implements ConditionalEventHandler {

        static final Predicate<TerminalEvent<?>> canHandleCanvasUpdate = new IS_Event_TYPE(TerminalEvent.TYPE.REQUEST_UPDATE_CANVAS);
        
        AbstractTimedRequestedController timedOnChangeRequestController = null;

        public OnRequestUpdateCanvasEventHandler(Runnable canvasUpdater) {
            timedOnChangeRequestController = buildTimedUpdateCanvasController(canvasUpdater);
            timedOnChangeRequestController.setTimerDelayMillis(50);
        }
        
        AbstractTimedRequestedController buildTimedUpdateCanvasController(Runnable canvasUpdater) {
            return new AbstractTimedRequestedController(canvasUpdater) {
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
