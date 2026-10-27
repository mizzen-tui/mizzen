package mizzen.events.mouse;

import mizzen.events.EventBus;
import mizzen.geom.position.Position;

/** mouse events for tests (their constructor is the package's own) */
public class MouseEventsForTesting {

    /** a left click at row, col - relative to whatever the receiver expects (a component's own corner, the canvas) */
    public static ApplicationMouseEvent click(int row, int col, EventBus eventBus) {
        ApplicationMouseEvent click = new ApplicationMouseEvent("test", MOUSE_ACTION.DOWN_LEFT, new Position.Simple(row, col));
        click.setEventBus(eventBus);
        return click;
    }
}
