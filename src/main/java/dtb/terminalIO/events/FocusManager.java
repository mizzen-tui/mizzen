package dtb.terminalIO.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.traverse.FindPathToTarget;
import dtb.terminalIO.traverse.HasChildren;
import dtb.terminalIO.traverse.Traverser;

public class FocusManager implements ConditionalEventHandler, Focusable, HasChildren<Focusable> {

    private List<Focusable> eventConsumers;
    private Focusable current;
    
    private KeyStroke prevKeyStroke = Keys.UP_KEYSTROKE;
    private KeyStroke nextKeyStroke = Keys.DOWN_KEYSTROKE;
    private Consumer<Focusable> onFocusChanged;
    
    ConditionalEventHandler meAsEventHandler;
    Focusable               meAsFocusable;

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

    private Focusable getCurrent(EventBus eventBus) {
	if (current == null && eventConsumers.size()>0) {
	    setCurrent(eventConsumers.get(0), eventBus);
	}
	return this.current;
    }
    
    private void setCurrent(Focusable focusable, EventBus eventBus) {
        Focusable oldCurrent = this.current;
	this.current = focusable;
	if (oldCurrent != null) {
	   oldCurrent.setFocus(false, eventBus);
	}
        if ( this.current != null) {	
            this.current.setFocus(true, eventBus);
        }
        if (onFocusChanged != null) {
            onFocusChanged.accept(this.current);
        }
    }
    
    public int size() {
        return eventConsumers.size();
    }
    
    
    private Focusable getPrev(EventBus eventBus) {
        if (size()<1) {
            return null;
        }
        int index = eventConsumers.indexOf( getCurrent(eventBus)  );
        if (index < 1) {
            return eventConsumers.get(size()-1);
        }
        return eventConsumers.get(index - 1);
    }

    private Focusable getNext(EventBus eventBus) {
        if (size()<1) {
            return null;
        }
        if (size()<2) {
            return null;
        }
        int index = eventConsumers.indexOf( getCurrent(eventBus)  );
        if (index < 0 || index == size()-1) {
            return eventConsumers.get(0);
        }
        return eventConsumers.get(index + 1);
    }
    
    

    @Override
    public void handle(TerminalEvent<?> te) {
        getEventHandler().handle(te);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (getEventHandler().canHandle(te) == null) {
            return null;
        }
        return this;
    }    
    
    private ConditionalEventHandler getEventHandler() {
        if ( this.meAsEventHandler == null) {
            this.meAsEventHandler = buildEventHandler();
        }
        return this.meAsEventHandler;
    }
    

    
    private ConditionalEventHandler buildEventHandler() {
        
        ChainedEventHandler chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();
        
        chainedEventHandler.addEventHandler(buildRequestFocusEventHandler());
        chainedEventHandler.addEventHandler(buildKeyStrokeEventHandler());
        
        return chainedEventHandler.asConditionalEventHandler();
    }
    
    /**
     * Traverse this object and all its child objects looking for
     * focusTarget.  
     * 
     * If focusTarget found,  then it returns path (a list of objects)
     * from this this focusManager to focusTarget
     */
    private List<Object> findTarget(Object focusTarget) {
        
        List<Object> path = new FindPathToTarget<Object>().findPath(this, focusTarget);
        
        return path;
    }

    /**
     * Builds a focus event handler that looks for object within this focusManager's
     * descendant objects
     */
    private ConditionalEventHandler buildRequestFocusEventHandler() {
        
        Predicate<TerminalEvent<?>> canHandlePredicate = new Predicate<TerminalEvent<?>>() {

            IS_Event_TYPE IS_REQUEST_FOCUS = new IS_Event_TYPE(TerminalEvent.TYPE.REQUEST_FOCUS);
            
            @Override
            public boolean test(TerminalEvent<?> te) {
                if ( ! IS_REQUEST_FOCUS.test(te)) {
                    return false;
                }
                RequestFocusEvent fe = (RequestFocusEvent) te;
                
                List<Object> result = findTarget(fe.getEventData());
                
                // TODO: is it possible to implement a child find?
                if (result == null) {
                    return false;
                }
                // what a waste .. I'm throwing result away
                //  to be recalc'd in the handle - oh well.
                return true;
            }
        };
            
        return new ConditionalEventHandler.Impl(canHandlePredicate, (te) -> {
            RequestFocusEvent fe = (RequestFocusEvent) te;

            List<Object> path = findTarget(te.getSource());

            if (path == null) {
                return;
            }

            // at this point target is within our tree
            // time to traverse the path in order to "select" (focus) it

            if (!FocusManager.this.hasFocus()) {
                // FIXME: dont know how to deal with this scenario yet
            }

            Object last = null;
            for (Object o : path) {
                // if (o != this) {
                if (o instanceof Focusable) {

                    if (last != null && last instanceof FocusManager) {
                        ((FocusManager) last).setCurrent(((Focusable) o), te.getEventBus());
                    } else {
                        ((Focusable) o).setFocus(true, te.getEventBus());
                    }
                }

                last = o;

                // }
            }
        });

    }
    
    /**
     * Builds a keyStroke eventHandler.
     */
    private ConditionalEventHandler buildKeyStrokeEventHandler() {
        
        Predicate<TerminalEvent<?>> canHandlePredicate = new Predicate<TerminalEvent<?>>() {

            IS_Event_TYPE IS_KEYPRESS = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);
            
            @Override
            public boolean test(TerminalEvent<?> te) {
                if ( ! IS_KEYPRESS.test(te)) {
                    return false;
                }
                KeyEvent ke = (KeyEvent) te;
                if (prevKeyStroke != null && ke.isKeyStroke(prevKeyStroke)) {
                    return true;
                } else if (nextKeyStroke != null && ke.isKeyStroke(nextKeyStroke)) {
                    return true;
                } else {
                    Focusable focusable = getCurrent(te.getEventBus());
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
		Focusable prev = getPrev(te.getEventBus());
		if (prev != null) {
		    setCurrent(prev, te.getEventBus());
		}
	    } else if (nextKeyStroke != null && ke.isKeyStroke(nextKeyStroke)) {
		Focusable next = getNext(te.getEventBus());
		if (next != null) {
		    setCurrent(next, te.getEventBus());
		}
	    } else {
		Focusable focusable = getCurrent(te.getEventBus());
		if (focusable != null) {
		    focusable.handle(te);
		}
	    }
	});
    }

    public void addFocusable(Focusable focusable, EventBus eventBus) {
	eventConsumers.add(focusable);
	if (eventConsumers.size() == 1) {
	    setCurrent(focusable, eventBus);
	}
    }

    public void addFocusable(ToFocusable focusable, EventBus eventBus) {
        addFocusable( focusable.asFocusable(), eventBus );
    }
    
    
    private Focusable getAsFocusable() {
        
        if (meAsFocusable == null) {
            
            ConditionalEventHandler event_handler = FocusManager.this;
            
            meAsFocusable = new DefaultFocusableBehaviour() {
    
                @Override
                public void setFocus(boolean focus, EventBus eventBus) {
                    super.setFocus(focus, eventBus);
                    getCurrent(eventBus).setFocus(focus, eventBus);
                }
                
                @Override
                public EventHandler canHandle(TerminalEvent<?> te) {
                    return event_handler.canHandle(te);
                }
    
                @Override
                public void handle(TerminalEvent<?> te) {
                    event_handler.handle(te);
                }
                
            };
        }
        return meAsFocusable;
        
    }

    @Override
    public Collection<? extends Focusable> getChildren() {
        return eventConsumers;
    }

    @Override
    public boolean hasFocus() {
        return getAsFocusable().hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        getAsFocusable().setFocus(focus, eventBus);
    }

    @Override
    public String toString() {
        return "FocusManager [eventConsumers size =" + eventConsumers.size() + "]";
    }
    
    

}
