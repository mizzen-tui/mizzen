package dtb.terminalIO.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import dtb.terminalIO.events.ConditionalEventHandler.AsAConditionalEventHandler;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;

public class FocusManager implements AsAConditionalEventHandler {

    List<Focusable> eventConsumers;
    Focusable current;
    
    KeyStroke prevKeyStroke = Keys.UP_KEYSTROKE;
    KeyStroke nextKeyStroke = Keys.DOWN_KEYSTROKE;
    Consumer<Focusable> onFocusChanged;
    
    public FocusManager() {
        eventConsumers = new ArrayList<Focusable>();
    }

    public FocusManager(KeyStroke prevKeyStroke, KeyStroke nextKeyStroke) {
	this();
	this.prevKeyStroke = prevKeyStroke;
        this.nextKeyStroke = nextKeyStroke;
    }
    
    public FocusManager(KeyStroke nextKeyStroke) {
        this(null, nextKeyStroke);
    }

    public FocusManager setOnFocusChanged(Consumer<Focusable> onFocusChanged) {
        this.onFocusChanged = onFocusChanged;
        return this;
    }

    Focusable getCurrent() {
	if (current == null && eventConsumers.size()>0) {
	    setCurrent(eventConsumers.get(0));
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
        if (onFocusChanged != null) {
            onFocusChanged.accept(this.current);
        }
    }
    
    public int size() {
        return eventConsumers.size();
    }
    
    
    public Focusable getPrev() {
        if (size()<1) {
            return null;
        }
        int index = eventConsumers.indexOf( getCurrent()  );
        if (index < 1) {
            return eventConsumers.get(size()-1);
        }
        return eventConsumers.get(index - 1);
    }

    public Focusable getNext() {
        if (size()<1) {
            return null;
        }
        if (size()<2) {
            return null;
        }
        int index = eventConsumers.indexOf( getCurrent()  );
        if (index < 0 || index == size()-1) {
            return eventConsumers.get(0);
        }
        return eventConsumers.get(index + 1);
    }
    

    public ConditionalEventHandler asEventHandler() {
        
        Predicate<TerminalEvent<?>> canHandlePredicate = new Predicate<TerminalEvent<?>>() {

            IS_Event_TYPE IS_KEYPRESS = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);
            
            @Override
            public boolean test(TerminalEvent<?> te) {
                if ( ! IS_KEYPRESS.test(te) ) {
                    return false;
                }
                KeyEvent ke = (KeyEvent) te;
                if (prevKeyStroke != null && ke.isKeyStroke(prevKeyStroke)) {
                    return true;
                } else if (nextKeyStroke != null && ke.isKeyStroke(nextKeyStroke)) {
                    return true;
                } else {
                    Focusable focusable = getCurrent();
                    if (focusable != null) {
                        return focusable.canHandle(te) != null;
                    }
                }
                return false;
            }
            
        };
        
	return new KeyInputEventHandler(canHandlePredicate, (te) -> {
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
    
//    public void addFocusableAtPosition(Focusable focusable, Position pos) {
//        
//        Focusable focusableAtPosition =  new Focusable.FocusableDelegate(focusable) {
//            @Override
//            public Position getPosition() {
//                return pos;
//            }
//        };
//        addFocusable(focusableAtPosition);
//    }
    

    public Focusable asFocusable() {
        return new Focusable() {

            ConditionalEventHandler event_handler = asEventHandler();
            
            boolean hasFocus = false;
            
            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                return event_handler.canHandle(te);
            }
            
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

//            @Override
//            public Position getPosition() {
//                Position curPos = Position.ZERO;
//                if (eventConsumers.size() > 0) {
//                    curPos = eventConsumers.first().getPosition();
//                }
//                return curPos;
//            }
            
            public String toString() {
                return "FocusManager.Focusable " + eventConsumers.size()  ;
            }

        };
    }

}
