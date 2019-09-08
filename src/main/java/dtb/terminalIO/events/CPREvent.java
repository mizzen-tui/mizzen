package dtb.terminalIO.events;

import dtb.terminalIO.geom.position.Position;

public class CPREvent extends TerminalEvent<Position> {

    private static final long serialVersionUID = -7991119354059041232L;

    public CPREvent(Object source, Position eventData) {
        super(source, TYPE.CPR_REPORT, eventData);
    }
    
    public CPREvent(Object source, int row, int col) {
        this(source, new Position.Simple(row, col));
    }
    
    
    public Position getPosition() {
        return getEventData();
    }

}
