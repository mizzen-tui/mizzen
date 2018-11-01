package dtb.terminalIO.events;


public class OnIdleEvent extends TerminalEvent<Long> {
    
    private static final long serialVersionUID = -4671742188094575966L;
    
    OnIdleEvent(Object source) {
	super(source, TYPE.TIMER, System.currentTimeMillis());
    }
    
    public long getMilliseconds() {
	return this.getEventData();
    }

}
