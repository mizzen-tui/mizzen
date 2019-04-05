package dtb.terminalIO.events;

public class RequestFocusEvent extends TerminalEvent<Object> {

    private static final long serialVersionUID = -6351549290745592241L;

    public RequestFocusEvent(Object source) {
        super(source, TYPE.REQUEST_FOCUS, source);
    }

}
