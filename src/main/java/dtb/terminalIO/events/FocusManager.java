package dtb.terminalIO.events;

import java.util.TreeSet;

import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Position;

public class FocusManager {

    TreeSet<Focusable> eventConsumers;
    Focusable current;
    
    KeyStroke prevKeyStroke = Keys.UP_KEYSTROKE;
    KeyStroke nextKeyStroke = Keys.DOWN_KEYSTROKE;
    
    public FocusManager() {
        eventConsumers = new TreeSet<Focusable>(new Focusable.FocusableComparator());
    }

    public FocusManager(KeyStroke prevKeyStroke, KeyStroke nextKeyStroke) {
	this();
	this.prevKeyStroke = prevKeyStroke;
        this.nextKeyStroke = nextKeyStroke;
    }
    
    public FocusManager(KeyStroke nextKeyStroke) {
        this(null, nextKeyStroke);
    }
    
    Focusable getCurrent() {
	if (current == null && eventConsumers.size()>0) {
	    setCurrent(eventConsumers.first());
	}
	return this.current;
    }
    
    void setCurrent(Focusable focusable) {
        Focusable oldCurrent = this.current;
	this.current = focusable;
	if (oldCurrent != null) {
	   oldCurrent.setFocus(false);
	}
        if ( this.current != null) {	
            this.current.setFocus(true);
        }
    }
    
    public Focusable getPrev() {
        // todo: make it so that it rotates when it gets to the end
        Focusable f = eventConsumers.lower(getCurrent());
        if (f == null) {
            f = eventConsumers.last();
        }
        return f;
    }

    public Focusable getNext() {
        // todo: make it so that it rotates when it gets to the end
        Focusable f = eventConsumers.higher(getCurrent());
        if (f == null) {
            f = eventConsumers.first();
        }
        return f;
    }
    

    public ConditionalEventHandler asEventHandler() {
	return new KeyInputEventHandler(new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS), (te) -> {
	    KeyEvent ke = (KeyEvent) te;
	    
	    if (prevKeyStroke != null && ke.isKeyStroke(prevKeyStroke)) {
		Focusable prev = getPrev();
		if (prev != null) {
		    setCurrent(prev);
		}
	    } else if (nextKeyStroke != null && ke.isKeyStroke(nextKeyStroke)) {
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
	if (eventConsumers.size() == 1) {
	    setCurrent(focusable);
	}
    }
    
    public void addFocusableAtPosition(Focusable focusable, Position pos) {
        
        Focusable focusableAtPosition =  new Focusable.FocusableDelegate(focusable) {
            @Override
            public Position getPosition() {
                return pos;
            }
        };
        addFocusable(focusableAtPosition);
    }
    

    public Focusable asFocusable() {
        return new Focusable() {

            ConditionalEventHandler event_handler = asEventHandler();
            
            boolean hasFocus = false;
            
            @Override
            public void handle(TerminalEvent<?> te) {
                event_handler.handle(te);
            }

            @Override
            public boolean hasFocus() {
                return hasFocus;
            }

            @Override
            public void setFocus(boolean focus) {
                getCurrent().setFocus(focus);
                hasFocus = focus;
            }

            @Override
            public Position getPosition() {
                Position curPos = Position.ZERO;
                if (eventConsumers.size() > 0) {
                    curPos = eventConsumers.first().getPosition();
                }
                return curPos;
            }
            
            public String toString() {
                return "FocusManager.Focusable " + eventConsumers.size() + " pos = " + getPosition() ;
            }
            
        };
    }

}
