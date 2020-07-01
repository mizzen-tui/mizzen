package dtb.terminalIO.events;

public class RequestUpdateCanvasEvent extends TerminalEvent<Object> {

    private static final long serialVersionUID = 6414374218181140855L;

    public RequestUpdateCanvasEvent(Object source) {
        super(source, TYPE.REQUEST_UPDATE_CANVAS, null, Q_FULL_POLICY.OFFER);
    }

    @Override
    public String toString() {
       return "RequestUpdateCanvasEvent [source=" + getSource() + "]";
    }
    
}
