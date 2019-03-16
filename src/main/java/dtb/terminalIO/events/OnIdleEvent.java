package dtb.terminalIO.events;


/**
 * A special {@link TerminalEvent} that is raised when no activity is happening by the user.
 * 
 * <p/>
 * 
 * ie If there is not keyboard activity then an OnIdleEvent can be
 * raised to do something in the "background".
 * 
 * @author david
 *
 */
public class OnIdleEvent extends TerminalEvent<Long> {
    
    private static final long serialVersionUID = -4671742188094575966L;
    
    public OnIdleEvent(Object source) {
	super(source, TYPE.TIMER, System.currentTimeMillis());
    }
    
    public long getMilliseconds() {
	return this.getEventData();
    }

}
