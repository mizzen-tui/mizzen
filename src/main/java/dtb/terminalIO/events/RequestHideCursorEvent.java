package dtb.terminalIO.events;

import dtb.terminalIO.geom.Position;

public class RequestHideCursorEvent extends TerminalEvent<Position> {

    private static final long serialVersionUID = 6414374218181140855L;

    public RequestHideCursorEvent(Object source) {
        super(source, TYPE.REQUEST_HIDE_CURSOR, null);
    }

}
