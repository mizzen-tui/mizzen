package dtb.terminalIO.events;

public interface Focusable extends ConditionalEventHandler {


    boolean hasFocus();

    void setFocus(boolean focus, EventBus eventBus);

    // Position getPosition();
    
    
    class FocusableDelegate implements Focusable {
        
        Focusable delegate ;
        public FocusableDelegate(Focusable delegate) {
            super();
            this.delegate = delegate;
        }
    
        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            return delegate.canHandle(te);
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