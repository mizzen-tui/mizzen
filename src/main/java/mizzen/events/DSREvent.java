package mizzen.events;

/**
 * Device Status Report (DSR) event
 * 
 * <p/>
 * 
 * Created when stdin is parsed to have an incoming DSR byte sequence
 *
 */
public class DSREvent extends TerminalEvent<Integer> {

    private static final long serialVersionUID = -1219719615636946728L;

    public DSREvent(Object source, Integer eventData) {
        super(source, TYPE.DSR_REPORT, eventData);
    }

    public boolean isOK() {
        return this.getEventData()!= null && this.getEventData() == 0;
    }

}
