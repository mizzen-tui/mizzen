package dtb.terminalIO.events.focus;

import dtb.terminalIO.components.Enabled;
import dtb.terminalIO.events.EventBus;

public interface FocusBehaviour extends Enabled {

    boolean hasFocus();

    void setFocus(boolean focus, EventBus eventBus);
    
    default void unFocus(Focusable newFocusable, EventBus eventBus) {
        this.setFocus(false, eventBus);
    }
    
    default void focus(Focusable oldFocusable, EventBus eventBus) {
        this.setFocus(true, eventBus);
    }

    default void clearFocus(EventBus eventBus) {
        if (this.hasFocus()) {
            this.setFocus(false, eventBus);
        }
    }
    
    public static class Chained implements FocusBehaviour {

        FocusBehaviour f1;
        FocusBehaviour f2;

        public Chained(FocusBehaviour f1, FocusBehaviour f2) {
            super();
            this.f1 = f1;
            this.f2 = f2;
        }

        @Override
        public boolean isEnabled() {
            return f1.isEnabled() || f2.isEnabled();
        }

        @Override
        public boolean hasFocus() {
            return f1.hasFocus() || f2.hasFocus();
        }

        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
            f1.setFocus(focus, eventBus);
            f2.setFocus(focus, eventBus);
        }
        
    } 
    

}
