package dtb.terminalIO.components.input;

import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;

public interface Clickable {

    public void onClick(EventBus eventBus);
    
    default public void onClick(ApplicationMouseEvent appMouseEvent) {
        onClick( appMouseEvent.getEventBus());
    }
    
}
