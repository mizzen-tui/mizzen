package dtb.terminalIO.events;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.geom.Rectangle;

public class RequestScrollToView extends TerminalEvent<Rectangle> {

    private static final long serialVersionUID = 6131045992655925913L;

    Rectangle offsetRectangle;
    
    public RequestScrollToView(Object source, Component<?> eventData) {
        super(source, TYPE.REQUEST_SCROLL_TO_VIEW, eventData);
    }

    public RequestScrollToView(Object source, Component<?> eventData, Rectangle offsetRectangle) {
        super(source, TYPE.REQUEST_SCROLL_TO_VIEW, eventData);
        this.offsetRectangle = offsetRectangle;
    }
    
    public Component<?> getScrollToComponent() {
        return (Component<?>)this.getEventData();
    }

    public Rectangle getScrollToOffsetRectangle() {
        return offsetRectangle;
    }

    @Override
    public String toString() {
       return "RequestScrollToView [ eventData ="+  getScrollToComponent() + "rect="+String.valueOf(offsetRectangle)+" ]";
    }

    
}
