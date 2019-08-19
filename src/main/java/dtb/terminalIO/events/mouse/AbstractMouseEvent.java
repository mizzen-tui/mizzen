package dtb.terminalIO.events.mouse;

import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Position;

public class AbstractMouseEvent extends TerminalEvent<Position> {

    private static final long serialVersionUID = 4964184233422719L;

    private MOUSE_ACTION mouseAction;
    
    protected AbstractMouseEvent(Object source, TYPE theType, MOUSE_ACTION action, Position eventData) {
        super(source, theType, eventData);
        this.mouseAction = action;
    }
    
    public MOUSE_ACTION getMouseActionI() {
        return this.mouseAction;
    }
 
    @Override
    public String toString() {
        return super.toString().replace("TerminalEvent",this.getClass().getSimpleName()).replace("eventData",( mouseAction + ", eventData"));
    }
    
}
