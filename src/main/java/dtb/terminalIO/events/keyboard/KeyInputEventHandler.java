package dtb.terminalIO.events.keyboard;

import java.util.function.Predicate;

import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;

/**
 * 
 * Handles {@link KeyEvent}s
 * 
 * @author david
 *
 */
public class KeyInputEventHandler extends ConditionalEventHandler.Impl {

    IS_Event_TYPE filterOnKeyEvtType;
    
    public KeyInputEventHandler(Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
	super(canHandlePredicate, handler);
	this.filterOnKeyEvtType = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);
    }

    static class KeyStrokePredicate implements Predicate<TerminalEvent<?>> {

	KeyStroke onKeyStroke;
	
	public KeyStrokePredicate(KeyStroke onKeyStroke) {
	    this.onKeyStroke = onKeyStroke;
	}

	@Override
	public boolean test(TerminalEvent<?> te) {
            KeyEvent ke = (KeyEvent) te;
            if (ke.isKeyStroke( onKeyStroke )) {
        	return true;
            }
	    return false;
	}

	@Override
	public String toString() {
	    return "KeyStrokePredicate [onKeyStroke=" + onKeyStroke + "]";
	}
	
    }
    
    public KeyInputEventHandler(KeyStroke onKeyStroke, EventHandler handler) {
	super(new KeyStrokePredicate(onKeyStroke), handler);
	this.filterOnKeyEvtType = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);
    }
    
    public KeyInputEventHandler(KeyStroke onKeyStroke, Predicate<TerminalEvent<?>> canHandlePredicate, EventHandler handler) {
        super(new KeyStrokePredicate(onKeyStroke).and(canHandlePredicate), handler);
        this.filterOnKeyEvtType = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);
    }
    
    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
	if (filterOnKeyEvtType.test(te)) {
	    if (getCanHandlePredicate() == null) {
		return this;
	    }
	    return super.canHandle(te);
	}
	return null;
    }

    @Override
    public String toString() {
	return  super.toString().replace("Impl","KeyInputEventHandler");
    }

}
