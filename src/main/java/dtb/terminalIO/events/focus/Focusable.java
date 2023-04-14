package dtb.terminalIO.events.focus;

import dtb.terminalIO.components.Component;
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
public interface Focusable extends FocusBehaviour, ConditionalEventHandler {

    FocusManager getParentFocusManager();

    void setParentFocusManager(FocusManager focusManager);
    
    public static class NOOPFocusable implements Focusable {

        @Override
        public boolean hasFocus() {
            return false;
        }

        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            return null;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
        }

        @Override
        public FocusManager getParentFocusManager() {
            return null;
        }

        @Override
        public void setParentFocusManager(FocusManager focusManager) {
        }
        
    }
    
    public static class FocusableFromParts extends NOOPFocusable {
        FocusManager parentContainer;
        FocusBehaviour behaviour;
        ConditionalEventHandler eventHandler;
        Component<?> component;
        
        public FocusableFromParts(Component<?> component, FocusBehaviour behaviour, ConditionalEventHandler eventHandler) {
            super();
            this.component = component;
            this.behaviour = behaviour;
            this.eventHandler = eventHandler;
        }

        public FocusBehaviour getBehaviour() {
            return behaviour;
        }

        public void setBehaviour(FocusBehaviour behaviour) {
            this.behaviour = behaviour;
        }

        public ConditionalEventHandler getEventHandler() {
            return eventHandler;
        }

        public void setEventHandler(ConditionalEventHandler eventHandler) {
            this.eventHandler = eventHandler;
        }

        @Override
        public boolean hasFocus() {
            return behaviour != null ? behaviour.hasFocus() : false;
        }

        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
            if (behaviour != null) {
                behaviour.setFocus(focus, eventBus);
            }
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if (eventHandler == null) {
                return null;
            }
            if ( eventHandler.canHandle(te) != null ) {
                return this;
            }
            return null;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            if (eventHandler != null) {
                eventHandler.handle(te);
            }
        }

        @Override
        public FocusManager getParentFocusManager() {
            return parentContainer;
        }

        @Override
        public void setParentFocusManager(FocusManager focusManager) {
            this.parentContainer = focusManager;
        }
        
        public Component<?> getComponent() {
            return this.component;
        }
        
    }

    
    public static class FocusableFromBehaviour extends NOOPFocusable {

        FocusBehaviour behaviour;
        
        
        @Override
        public boolean hasFocus() {
            return behaviour.hasFocus();
        }

        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
            behaviour.setFocus(focus, eventBus);
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public FocusManager getParentFocusManager() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setParentFocusManager(FocusManager focusManager) {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    
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

        @Override
        public FocusManager getParentFocusManager() {
            return delegate.getParentFocusManager();
        }

        @Override
        public void setParentFocusManager(FocusManager focusManager) {
            delegate.setParentFocusManager(focusManager);
        }
    
        
        
    }


}