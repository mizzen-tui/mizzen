package dtb.terminalIO.events.focus;

import java.util.function.Consumer;

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

    /**
     * Focus is moving onto this (e.g. TAB from the previous field): which focusable should it
     * land on? A plain field lands on itself, or returns null ("skip me") if disabled.
     * Containers pick one of their children instead (see FocusManager.UTIL.enter).
     * Only answers - changes nothing. Not yet used by the focus routing (focus redesign).
     */
    default Focusable enter(FocusDirection dir) {
        return isEnabled() ? this : null;
    }

    /**
     * A nav key (e.g. TAB) was pressed while focusedLeaf, somewhere inside this, has focus:
     * where should focus go next? A plain field has nothing inside it, so it answers DONE
     * ("not me - ask my parent"). Containers decide among their children (see FocusManager.UTIL.advance).
     * Only answers - changes nothing. Not yet used by the focus routing (focus redesign).
     */
    default Navigation advance(Focusable focusedLeaf, FocusDirection dir) {
        return Navigation.DONE;
    }

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

        @Override
        public boolean isEnabled() {
            return false;
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
        public boolean isEnabled() {
            if (this.component != null) {
                return this.component.isEnabled();
            }
            return this.behaviour.isEnabled();
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
            Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> this.parentContainer = fm);
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
            Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> delegate.setParentFocusManager(fm));
        }

        @Override
        public boolean isEnabled() {
            return delegate.isEnabled();
        }
    
        
        
    }
    
    public static class UTIL {
        
        // default behaviour for setting parent is to remove it from old parent first (if we can and if not null)
        public static void setParentFocusManager(Focusable focusable, FocusManager parentFocusManager, Consumer<FocusManager> setParentAfterCleanUp) {
            
            // clean up
            if (focusable.getParentFocusManager() != null) {
                if (focusable.getParentFocusManager() instanceof FocusablesContainer) {
                    if (focusable.getParentFocusManager() instanceof FocusablesContainer) {
                        if (((FocusablesContainer)focusable.getParentFocusManager()).getChildren().contains(focusable)) {
                            ((FocusablesContainer)focusable.getParentFocusManager()).removeFocusable(focusable);
                        }
                    } else {
                        ((FocusablesContainer)focusable.getParentFocusManager()).removeFocusable(focusable);
                    }
                }
            }
            // now set parent
            setParentAfterCleanUp.accept(parentFocusManager);
            
        }
        
        public static void releaseFocusIfNeeded(Focusable focusable, EventBus eventBus) {
            if (focusable != null && focusable.hasFocus()) {
                focusable.clearFocus(eventBus);
            }
        }

    }


}