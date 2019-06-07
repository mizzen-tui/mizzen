package dtb.terminalIO.events;

import dtb.terminalIO.geom.Rectangle;

public class RequestScrollToView extends TerminalEvent<Rectangle> {

    private static final long serialVersionUID = 6131045992655925913L;

    public RequestScrollToView(Object source, Rectangle eventData) {
        super(source, TYPE.REQUEST_SCROLL_TO_VIEW, eventData);
    }

}
