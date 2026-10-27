package mizzen.events.mouse;

import java.util.function.Function;

import mizzen.components.Component;
import mizzen.geom.position.Position;

public class ApplicationMouseEvent extends AbstractMouseEvent {

    private static final long serialVersionUID = 7172651125203610138L;

    // leave constructor as package private
    ApplicationMouseEvent(Object source, MOUSE_ACTION action, Position eventData) {
        super(source, TYPE.APPLICATION_MOUSE, action, eventData);
    }
    
    public static ApplicationMouseEvent asRelativeToComponent(ApplicationMouseEvent appMouseEventRelativeToCanvas, Component<?> component) {
        Position positionOfComponentOnCanvas = Component.UTIL.getCanvasOffsetFor(component);
        Position mousePositionOffsetIntoFrameSet =  appMouseEventRelativeToCanvas.getEventData().offsetAdd(positionOfComponentOnCanvas.negate());
        ApplicationMouseEvent newMouseEvent = new ApplicationMouseEvent(appMouseEventRelativeToCanvas.getSource(), appMouseEventRelativeToCanvas.getMouseAction(), mousePositionOffsetIntoFrameSet);
        newMouseEvent.setEventBus(appMouseEventRelativeToCanvas.getEventBus());
        return newMouseEvent;
    }
    
    
    
    /** the same mouse event at another position - e.g. passed on to a component inside, in its terms */
    public ApplicationMouseEvent at(Position position) {
        ApplicationMouseEvent moved = new ApplicationMouseEvent(getSource(), getMouseAction(), position);
        moved.setEventBus(getEventBus());
        return moved;
    }

    static <T> boolean compareProperties(ApplicationMouseEvent ame1, ApplicationMouseEvent ame2, Function<ApplicationMouseEvent, T> property) {
        T property1 = property.apply(ame1);
        T property2 = property.apply(ame2);
        if (property1 != property2) {
            if (property1 != null) {
                if (!property1.equals(property2)) {
                    return false;
                } else if (property2!=null){
                    return false;
                }
            }
        }
        return true;
    }
    
    public boolean isSame(ApplicationMouseEvent other) {
        if (other == null) {
            return false;
        }
        if (this == other) {
            return true;
        }

        if ( ! compareProperties(this, other, (mouseEvent)->  mouseEvent.getType()) ) {
            return false;
        }

        if ( ! compareProperties(this, other, (mouseEvent)->  mouseEvent.getEventData()) ) {
            return false;
        }

        if ( ! compareProperties(this, other, (mouseEvent)->  mouseEvent.getSource()) ) {
            return false;
        }

        if ( ! compareProperties(this, other, (mouseEvent)->  mouseEvent.getMouseAction()) ) {
            return false;
        }
        
        return true;
    }
    

}
