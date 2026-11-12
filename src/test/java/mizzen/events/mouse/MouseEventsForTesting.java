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

    /** a drag with the left button down, at row, col - as click(..) */
    public static ApplicationMouseEvent drag(int row, int col, EventBus eventBus) {
        ApplicationMouseEvent drag = new ApplicationMouseEvent("test", MOUSE_ACTION.DOWN_LEFT_DRAG, new Position.Simple(row, col));
        drag.setEventBus(eventBus);
        return drag;
    }

    /** the left button coming up at row, col - as click(..) */
    public static ApplicationMouseEvent up(int row, int col, EventBus eventBus) {
        ApplicationMouseEvent up = new ApplicationMouseEvent("test", MOUSE_ACTION.UP, new Position.Simple(row, col));
        up.setEventBus(eventBus);
        return up;
    }
}
