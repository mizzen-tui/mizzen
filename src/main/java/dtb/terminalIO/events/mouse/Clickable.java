package dtb.terminalIO.events.mouse;

import dtb.terminalIO.events.EventBus;

@FunctionalInterface
public interface Clickable {

    public void onClick(EventBus eventBus);
    
    default public void onClick(ApplicationMouseEvent appMouseEvent) {
        if (MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction())) {
            onClick( appMouseEvent.getEventBus());
        }
    }
    
}
