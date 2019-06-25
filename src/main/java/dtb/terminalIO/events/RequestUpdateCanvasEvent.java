package dtb.terminalIO.events;

import dtb.terminalIO.geom.Position;

public class RequestUpdateCanvasEvent extends TerminalEvent<Position> {

    private static final long serialVersionUID = 6414374218181140855L;

    public RequestUpdateCanvasEvent(Object source) {
        super(source, TYPE.REQUEST_UPDATE_CANVAS, null);
    }

}
