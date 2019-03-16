package dtb.terminalIO.events;

import dtb.terminalIO.geom.Position;

public class RequestShowCursorEvent extends TerminalEvent<Position> {

    private static final long serialVersionUID = -1373775889825384714L;

    public RequestShowCursorEvent(Object source, Position eventData) {
        super(source, TYPE.REQUEST_SHOW_CURSOR, eventData);
    }

}
