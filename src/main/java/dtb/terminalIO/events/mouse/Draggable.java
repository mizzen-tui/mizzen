package dtb.terminalIO.events.mouse;

public interface Draggable {
    
    boolean onDragStart(ApplicationMouseEvent appMouseEvent);
    boolean onDrag(ApplicationMouseEvent appMouseEvent);
    boolean onDragStop(ApplicationMouseEvent appMouseEvent);

}
