package mizzen.events.mouse;

import mizzen.geom.position.Position;

/**
 * Do not use ( or handle ) this event directly.  Instead you should handle
 * ApplicationMouseEvent's. 
 * 
 * <p/>
 * 
 * This event is meant only to be handled by MouseEventOrchestrator which
 * translates this raw mouse event to an application mouse event - which 
 * means that it maps the raw console x.y coordinates to the canvas
 * coordinates.
 */
public class RawParsedFromConsoleMouseEvent extends AbstractMouseEvent {

    private static final long serialVersionUID = 36283975010494908L;

    public RawParsedFromConsoleMouseEvent(Object source, MOUSE_ACTION action, Position eventData) {
        super(source, TYPE.RAW_MOUSE, action, eventData);
    }

}
