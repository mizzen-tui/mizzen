package dtb.terminalIO.events;

public class DSREvent extends TerminalEvent<Integer> {

    private static final long serialVersionUID = -1219719615636946728L;

    public DSREvent(Object source, Integer eventData) {
        super(source, TYPE.DSR_REPORT, eventData);
    }

    public boolean isOK() {
        return this.getEventData()!= null && this.getEventData() == 0;
    }

}
