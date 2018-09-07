package dtb.terminalIO.events;

import java.util.function.Predicate;

import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandlerImpl;
import dtb.terminalIO.events.Keys.KeyStroke;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;

public class KeyInputEventHandler extends ConditionalEventHandlerImpl {

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
            Keys.KeyEvent ke = (Keys.KeyEvent) te;
            if (ke.isKeyStroke( onKeyStroke )) {
        	return true;
            }
	    return false;
	}
    }
    
    
    public KeyInputEventHandler(KeyStroke onKeyStroke, EventHandler handler) {
	super(new KeyStrokePredicate(onKeyStroke), handler);
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

}
