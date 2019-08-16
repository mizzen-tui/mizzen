package dtb.terminalIO.events.mouse;

import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Position;

public class MouseEvent extends TerminalEvent<Position> {

    private static final long serialVersionUID = 36283975010494908L;

    public static enum MOUSE_ACTION {
        DOWN_LEFT, DOWN_RIGHT, UP
    }

    private MOUSE_ACTION mouseAction;

    public MouseEvent(Object source, MOUSE_ACTION action, Position eventData) {
        super(source, TYPE.MOUSE, eventData);
        this.mouseAction = action;
    }

    public MOUSE_ACTION getMouseActionI() {
        return this.mouseAction;
    }
 
    @Override
    public String toString() {
        return "HELLO THERE " + super.toString().replace("eventData",( mouseAction + ", eventData"));
        //return super.toString().replace("eventData=,","mouseAction="+mouseAction+", XXeventData=,");
        
        // "TerminalEvent [type=" + type + ", eventData=" + eventData + "]";
    }

}
