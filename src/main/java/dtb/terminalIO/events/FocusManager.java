package dtb.terminalIO.events;

import java.util.Comparator;
import java.util.TreeSet;

import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandler;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.geom.Position;

public class FocusManager {

    TreeSet<Focusable> eventConsumers;
    Focusable current;
    
    public static interface Focusable extends EventHandler {

	boolean hasFocus();

	void setFocus(boolean focus);

	Position getPosition();

    }
    
    public static class FocusableComparator implements Comparator<Focusable> {
        @Override
        public int compare(Focusable f1, Focusable f2) {
            if (f1 == f2)
                return 0;
            return Position.UTIL.POS_COMPARE.compare(f1.getPosition(), f2.getPosition());
        }
    }
    
    public FocusManager() {
	eventConsumers = new TreeSet<Focusable>(new FocusableComparator());
    }
    
    Focusable getCurrent() {
	if (current == null) {
	    this.current = eventConsumers.first();
	}
	return this.current;
    }
    
    void setCurrent(Focusable focusable) {
	this.current = focusable;
    }
    
    public Focusable getPrev() {
	return eventConsumers.lower(getCurrent());
    }

    public Focusable getNext() {
	return eventConsumers.higher(getCurrent());
    }
    

    public ConditionalEventHandler asEventHandler() {
	return new KeyInputEventHandler(new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS), (te) -> {
	    Keys.KeyEvent ke = (Keys.KeyEvent) te;
	    
	    if (ke.isKeyStroke(Keys.SHIFT_TAB_KEYSTROKE)) {
		Focusable prev = getPrev();
		if (prev != null) {
		    setCurrent(prev);
		}
	    } else if (ke.isKeyStroke(Keys.TAB_KEYSTROKE)) {
		Focusable next = getNext();
		if (next != null) {
		    setCurrent(next);
		}
	    } else {
		Focusable focusable = getCurrent();
		if (focusable != null) {
		    focusable.handle(te);
		}
	    }
	});
    }

    public void addFocusable(Focusable focusable) {
	eventConsumers.add(focusable);
    }

}
