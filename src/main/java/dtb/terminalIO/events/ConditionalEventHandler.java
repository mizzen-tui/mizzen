package dtb.terminalIO.events;

import java.util.function.Predicate;

/**
 * An {@link EventHandler} that processes an event only if 
 * the incoming {@link TerminalEvent} can be handled by this
 * EventHandler. 
 * 
 * @author david
 *
 */
public interface ConditionalEventHandler extends EventHandler {
    
    public EventHandler canHandle(TerminalEvent<?> te);
    
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
    
    public static class Delegate implements ConditionalEventHandler {

        private ConditionalEventHandler handler;
        
        public Delegate(ConditionalEventHandler handler) {
            super();
            this.handler = handler;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            handler.handle(te);
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if (handler.canHandle(te) == null) {
                return null;
            }
            return this;
        }
        
    }
    
    
    
}