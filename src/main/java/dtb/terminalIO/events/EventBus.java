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

    
    public static class Delegate implements EventBus {
        protected EventBus delegate;
        
        public Delegate(EventBus delegate) {
            this.delegate = delegate;
        }

        @Override
        public void stopConsuming() {
            delegate.stopConsuming();
        }

        @Override
        public boolean isActive() {
            return delegate.isActive();
        }

        @Override
        public boolean isEmptyQueue() {
            return delegate.isEmptyQueue();
        }

        @Override
        public EventBus raiseEvent(TerminalEvent<?> te) {
            delegate.raiseEvent(te);
            return this;
        }

        @Override
        public <T> EventBus raiseEvent(Object source, TYPE type, T eventData) {
            delegate.raiseEvent(source, type, eventData);
            return this;
        }

        @Override
        public EventBus appendIdleEventHandler(OnIdleEventEventHandler onIdleEventHandler) {
            delegate.appendIdleEventHandler(onIdleEventHandler);
            return this;
        }

        @Override
        public EventBus appendTerminateEventHandler(OnTerminateEventHandler terminateHandler) {
            delegate.appendEventHandler(terminateHandler);
            return this;
        }

        @Override
        public void enableIdleEvents() {
            delegate.enableIdleEvents();
        }

        @Override
        public void disableIdleEvents() {
            delegate.disableIdleEvents();
        }

        @Override
        public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
            delegate.prependEventHandler(eventHandler);
            return this;
        }

        @Override
        public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
            delegate.appendEventHandler(eventHandler);
            return this;
        }

        @Override
        public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler) {
            delegate.removeEventHandler(eventHandler);
            return this;
        }
    }
    
    /**
     * A do nothing eventBus
     */
    public static class STUBBED implements EventBus {

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

        boolean active = true;
        
        @Override
        public void stopConsuming() {
            active = false;
        }

        @Override
        public boolean isActive() {
            return active;
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
        
    }
    
}
