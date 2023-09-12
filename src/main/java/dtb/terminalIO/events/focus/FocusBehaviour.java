package dtb.terminalIO.events.focus;

import dtb.terminalIO.events.EventBus;

public interface FocusBehaviour {

    boolean hasFocus();

    void setFocus(boolean focus, EventBus eventBus);
    
    default void unFocus(Focusable newFocusable, EventBus eventBus) {
        this.setFocus(false, eventBus);
    }
    
    default void focus(Focusable oldFocusable, EventBus eventBus) {
        this.setFocus(true, eventBus);
    }

}
