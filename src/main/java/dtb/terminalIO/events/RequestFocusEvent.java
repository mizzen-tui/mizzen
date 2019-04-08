package dtb.terminalIO.events;

public class RequestFocusEvent extends TerminalEvent<Focusable> {

    private static final long serialVersionUID = -6351549290745592241L;

    public RequestFocusEvent(Focusable source) {
        super(source, TYPE.REQUEST_FOCUS, source);
    }

}
