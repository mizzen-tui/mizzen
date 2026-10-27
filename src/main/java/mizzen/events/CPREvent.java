package mizzen.events;

import mizzen.geom.position.Position;

/**
 * Cursor Position Report (CPR) event
 * 
 * <p/>
 * 
 * Created when stdin is parsed to have an incoming CPR byte sequence
 *
 */
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
