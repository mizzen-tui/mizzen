package mizzen.events;

public class CustomApplicationEvent<T> extends TerminalEvent<T> {

    private static final long serialVersionUID = 8064835919960851468L;

    public CustomApplicationEvent(Object source, T eventData) {
        super(source, TYPE.CUSTOM_APPLICATION_EVENT, eventData);
    }
    
    
}
