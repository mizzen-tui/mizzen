package dtb.terminalIO.events;

import java.util.EventObject;
import java.util.function.Predicate;

public class TerminalEvent<T> extends EventObject {

    private static final long serialVersionUID = -8117805332275225370L;
    
    public static enum TYPE {
        KEYPRESS,
        TIMER,
        EXCEPTION,
        DEBUG
    }
    
    TYPE type;
    T eventData;
    
    public static class IS_Event_TYPE implements Predicate<TerminalEvent<?>> {

	private TYPE type;

	public IS_Event_TYPE(TYPE type) {
	    this.type = type;
	}
	
	@Override
	public boolean test(TerminalEvent<?> te) {
	    return te.type.equals(type);
	}
	
    }
    

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
    
    protected void setEventData(T eventData) {
	this.eventData = eventData;
    }


}
