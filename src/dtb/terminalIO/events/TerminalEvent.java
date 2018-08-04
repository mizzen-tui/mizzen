package dtb.terminalIO.events;

import java.util.EventObject;

public class TerminalEvent<T> extends EventObject {

    private static final long serialVersionUID = -8117805332275225370L;
    
    public static enum TYPE {
        KEYPRESS,
        DRAW,
        EXCEPTION
    }
    
    TYPE type;
    T eventData;

    public TerminalEvent(Object source, TYPE type, T eventData) {
        super(source);
        this.type = type;
        this.eventData = eventData;
    }

    public TYPE getType() {
        return type;
    }

    public T getEventData() {
        return eventData;
    }


}
