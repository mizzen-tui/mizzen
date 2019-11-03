package dtb.terminalIO.components.input;

import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.MOUSE_ACTION;

public interface Clickable {

    public void onClick(EventBus eventBus);
    
    default public void onClick(ApplicationMouseEvent appMouseEvent) {
        if (MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction())) {
            onClick( appMouseEvent.getEventBus());
        }
    }
    
}
