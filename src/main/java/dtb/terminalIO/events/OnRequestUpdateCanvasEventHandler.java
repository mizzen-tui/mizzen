package dtb.terminalIO.events;

import java.util.function.Predicate;

import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;

public class OnRequestUpdateCanvasEventHandler extends ConditionalEventHandler.Impl {

        static final Predicate<TerminalEvent<?>> canHandleCanvasUpdate = new IS_Event_TYPE(TerminalEvent.TYPE.REQUEST_UPDATE_CANVAS);
        
        public OnRequestUpdateCanvasEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
            super(canHandleCanvasUpdate.and(canHandlePredicate), handler);
        }

        public OnRequestUpdateCanvasEventHandler(EventHandler handler) {
            super(canHandleCanvasUpdate, handler);
        }

        public OnRequestUpdateCanvasEventHandler(Runnable canvasUpdater) {
            super(canHandleCanvasUpdate, new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    canvasUpdater.run();
                }
            });
        }

}
