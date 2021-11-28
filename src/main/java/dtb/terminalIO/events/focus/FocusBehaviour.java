package dtb.terminalIO.events.focus;

import dtb.terminalIO.events.EventBus;

public interface FocusBehaviour {

    boolean hasFocus();

    void setFocus(boolean focus, EventBus eventBus);

    
    
}
