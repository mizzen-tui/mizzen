package dtb.terminalIO.events;

/**
 * A special {@link TerminalEvent} that is raised when no activity (no keyboard, no mouse activity) 
 * is happening by the user.
 * 
 * <p/>
 * 
 * ie If there is no keyboard activity then an OnIdleEvent can be
 * raised to do something in the "background".
 * 
 * @author david
 *
 */
public class OnIdleEvent extends TerminalEvent<Long> {

    private static final long serialVersionUID = -4671742188094575966L;

    public OnIdleEvent(Object source) {
        super(source, TYPE.TIMER, System.currentTimeMillis(),Q_FULL_POLICY.OFFER);
    }

    public long getMilliseconds() {
        return this.getEventData();
    }

}
