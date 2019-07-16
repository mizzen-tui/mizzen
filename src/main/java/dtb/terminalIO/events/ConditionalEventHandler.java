package dtb.terminalIO.events;

import java.util.function.Predicate;

/**
 * An {@link EventHandler} that processes an event only if 
 * the incoming {@link TerminalEvent} can be handled by this
 * EventHandler. 
 *
 */
public interface ConditionalEventHandler extends EventHandler {
    
    public static interface AsAConditionalEventHandler {
        ConditionalEventHandler asEventHandler();
    }
    
    public EventHandler canHandle(TerminalEvent<?> te);
    
    public default Predicate<TerminalEvent<?>>  asPredicate() {
        return new Predicate<TerminalEvent<?>>() {
            @Override
            public boolean test(TerminalEvent<?> te) {
                return ConditionalEventHandler.this.canHandle(te) != null;
            }
        };
    }
    
    
    class Impl implements ConditionalEventHandler {
    
        private Predicate<TerminalEvent<?>> canHandlePredicate;
        private EventHandler handler;
    
        protected EventHandler getHandler() {
            return this.handler;
        }
    
        protected Predicate<TerminalEvent<?>> getCanHandlePredicate() {
            return this.canHandlePredicate;
        }
    
        protected void setCanHandlePredicate(Predicate<TerminalEvent<?>> canHandlePredicate) {
            this.canHandlePredicate = canHandlePredicate;
        }
    
        public void setHandler(EventHandler handler) {
            this.handler = handler;
        }
        
        @Override
        public void handle(TerminalEvent<?> te) {
            handler.handle(te);
        }
    
        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if (canHandlePredicate.test(te)) {
        	return this;
            }
            return null;
        }
    
        public Impl(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
            this.canHandlePredicate = canHandlePredicate;
            this.handler = handler;
        }

	@Override
	public String toString() {
	    return "Impl [canHandlePredicate=" + canHandlePredicate + ", handler=" + handler + "]";
	}
        
        
    }
    
    public static abstract class AbstractDelegate implements ConditionalEventHandler {
        
        protected abstract ConditionalEventHandler getDelegate();
        
        @Override
        public void handle(TerminalEvent<?> te) {
            getDelegate().handle(te);
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if (getDelegate().canHandle(te) == null) {
                return null;
            }
            return this;
        }   
    }
    
    public static class Delegate extends AbstractDelegate {

        private ConditionalEventHandler handler;
        
        public Delegate(ConditionalEventHandler handler) {
            super();
            this.handler = handler;
        }

        protected ConditionalEventHandler getDelegate() {
            return handler;
        }

    }
}