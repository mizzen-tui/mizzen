package dtb.terminalIO.events;

import java.util.EventObject;
import java.util.function.Predicate;

/**
 * A TerminalEvent is an object that captures 
 * some event ( usually ) a keystroke (or a timer 'idle') event.
 * 
 * <p/>
 * 
 * Data related to the event is attached to the event object.
 * 
 * <p/>
 * 
 * The Event gets sent to an {@link EventBus} which distributes the event
 * to the {@link EventHandler}(s) that can handle the event.
 * 
 * @param <T> type of data attached to event, ie a KeyStroke
 */
public class TerminalEvent<T> extends EventObject {

    private static final long serialVersionUID = -8117805332275225370L;
    
    public static enum TYPE {
        KEYPRESS,
        RAW_MOUSE,
        APPLICATION_MOUSE,
        TIMER,
        REQUEST_FOCUS,
        REQUEST_RELEASE_FOCUS,
        REQUEST_SHOW_CURSOR,
        REQUEST_HIDE_CURSOR,
        REQUEST_UPDATE_CANVAS,
        REQUEST_SCROLL_TO_VIEW,
        VALUE_CHANGED,
        DSR_REPORT,  // device status report
        CPR_REPORT,  // cursor position report
        RESIZE,
        EXCEPTION,
        TERMINATE,
        DEBUG
    }
    
    TYPE type;
    T eventData;
    transient EventBus eventBus;
    
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
    
    public TerminalEvent(Object source, TYPE type, T eventData, EventBus eventBus) {
        super(source);
        this.type = type;
        this.eventData = eventData;
        this.eventBus = eventBus;
    }
    
    public TerminalEvent(Object source, TYPE type, T eventData) {
        this(source, type, eventData, null);
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

    @Override
    public String toString() {
	return "TerminalEvent [type=" + type + ", eventData=" + eventData + "]";
    }

    public TerminalEvent<T> setEventBus(EventBus eventBus) {
        this.eventBus = eventBus;
        return this;
    }
    
    public EventBus getEventBus() {
        if (eventBus == null) {
            throw new NullPointerException("Trying to access eventBus from event object that is not in the active state.  Are you referencing the correct event object?");
        }
        return eventBus;
    }


}
