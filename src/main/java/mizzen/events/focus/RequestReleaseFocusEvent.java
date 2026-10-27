package mizzen.events.focus;

import mizzen.events.TerminalEvent;

/**
 * Raised by a Component when it no longer wants focus.
 *
 */
public class RequestReleaseFocusEvent extends TerminalEvent<Focusable> {

    private static final long serialVersionUID = 8012171668797262898L;

    public RequestReleaseFocusEvent(Focusable source) {
        super(source, TYPE.REQUEST_RELEASE_FOCUS, source, Q_FULL_POLICY.OFFER);
    }
    
    
}
