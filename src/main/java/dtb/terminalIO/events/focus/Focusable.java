package dtb.terminalIO.events.focus;

import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;

/**
 * A ConditionalEventHandler that can have focus; which means
 * that it can have an outside entity (usually a {@link FocusManager}) direct
 * events to it.   
 * 
 * <p/>
 * 
 * This is a useful behaviour for Components that participate in
 * a data-form entry.  
 * 
 * <p/>
 * 
 * A good example is:  {@link dtb.terminalIO.components.input.TextInput}
 * 
 * 
 */
public interface Focusable extends ConditionalEventHandler {

    boolean hasFocus();

    void setFocus(boolean focus, EventBus eventBus);

    
    public class FocusableDelegate implements Focusable {
        
        Focusable delegate ;
        public FocusableDelegate(Focusable delegate) {
            super();
            this.delegate = delegate;
        }
    
        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if ( delegate.canHandle(te) == null ) {
        	return null;
            }
            return this;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            delegate.handle(te);
        }
    
        @Override
        public boolean hasFocus() {
            return delegate.hasFocus();
        }
    
        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
            delegate.setFocus(focus, eventBus);
        }
    
    }


}