package dtb.terminalIO.events.focus;

import dtb.terminalIO.events.TerminalEvent;

public class RequestFocusEvent extends TerminalEvent<Focusable> {

    private static final long serialVersionUID = -6351549290745592241L;

    public RequestFocusEvent(Focusable source) {
        super(source, TYPE.REQUEST_FOCUS, source, Q_FULL_POLICY.OFFER);
    }

}
