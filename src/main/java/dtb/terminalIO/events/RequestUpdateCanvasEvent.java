package dtb.terminalIO.events;

public class RequestUpdateCanvasEvent extends TerminalEvent<Object> {

    private static final long serialVersionUID = 6414374218181140855L;


    /**
     * An event to tell the app to update the Terminal with the latest
     * contents of its components
     * 
     * @param source
     */
    public RequestUpdateCanvasEvent(Object source) {
        super(source, TYPE.REQUEST_UPDATE_CANVAS, null, Q_FULL_POLICY.OFFER);
        //Logging.forceLogInfo("RequestUpdateCanvasEvent " + String.valueOf(source));
    }

    @Override
    public String toString() {
       return "RequestUpdateCanvasEvent [source=" + getSource() + "]";
    }

    public static void invoke(TerminalEvent<?> te, Object source) {
        invoke(te.getEventBus(), source);
    }

    public static void invoke(EventBus eventBus, Object source) {
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(source));
    }

    public static class UTIL {
        public static ConditionalEventHandler wrapEventHandlerWithCanvasUpdate(ConditionalEventHandler eventHandler) {
            ConditionalEventHandler handler = new ConditionalEventHandler.Delegate(eventHandler) {
                @Override
                public void handle(TerminalEvent<?> te) {
                    super.handle(te);
                    te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this) );
                }
            };
            return handler;
        }
    }

}
