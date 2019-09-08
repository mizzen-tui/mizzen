package dtb.terminalIO.events.mouse;

import dtb.terminalIO.geom.position.Position;

public class ApplicationMouseEvent extends AbstractMouseEvent {

    private static final long serialVersionUID = 7172651125203610138L;

    // leave constructor as package private
    ApplicationMouseEvent(Object source, MOUSE_ACTION action, Position eventData) {
        super(source, TYPE.APPLICATION_MOUSE, action, eventData);
    }

}
