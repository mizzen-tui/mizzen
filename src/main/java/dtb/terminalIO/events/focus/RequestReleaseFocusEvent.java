package dtb.terminalIO.events.focus;

import dtb.terminalIO.events.TerminalEvent;

public class RequestReleaseFocusEvent extends TerminalEvent<Focusable> {

    private static final long serialVersionUID = 8012171668797262898L;

    public RequestReleaseFocusEvent(Focusable source) {
        super(source, TYPE.REQUEST_RELEASE_FOCUS, source);
    }
    
    
}
