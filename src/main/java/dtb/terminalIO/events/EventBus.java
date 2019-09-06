package dtb.terminalIO.events;

import dtb.terminalIO.events.TerminalEvent.TYPE;

public interface EventBus extends EventHandlerContainer {

    void stopConsuming();

    boolean isActive();

    boolean isEmptyQueue();

    // TODO: If queue is full event wont get enqueued.. I dont tell caller - isn't
    // that a bad design?
    EventBus raiseEvent(TerminalEvent<?> te);

    <T> EventBus raiseEvent(Object source, TYPE type, T eventData);

    EventBus appendIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler);

    EventBus appendTerminateEventHandler(OnTerminateEventHandler terminateHandler);

    void enableIdleEvents();

    void disableIdleEvents();

    /**
     * A do nothing eventBus
     */
    public static final EventBus STUBBED = new EventBus() {

        @Override
        public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
            return this;
        }

        @Override
        public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
            return this;
        }

        @Override
        public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler) {
            return this;
        }

        @Override
        public void stopConsuming() {
        }

        @Override
        public boolean isActive() {
            return false;
        }

        @Override
        public boolean isEmptyQueue() {
            return false;
        }

        @Override
        public EventBus raiseEvent(TerminalEvent<?> te) {
            return null;
        }

        @Override
        public <T> EventBus raiseEvent(Object source, TYPE type, T eventData) {
            return this;
        }

        @Override
        public EventBus appendIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {
            return this;
        }

        @Override
        public EventBus appendTerminateEventHandler(OnTerminateEventHandler terminateHandler) {
            return this;
        }

        @Override
        public void enableIdleEvents() {
        }

        @Override
        public void disableIdleEvents() {
        }
        
    };
    

}