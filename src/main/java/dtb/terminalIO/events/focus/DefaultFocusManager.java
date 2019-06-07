package dtb.terminalIO.events.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.traverse.HasChildren;

public class DefaultFocusManager implements FocusManager,  HasChildren<Focusable> {

    private List<Focusable> eventConsumers;
    private Focusable current;
    
    private KeyStroke prevKeyStroke = Keys.UP_KEYSTROKE;
    private KeyStroke nextKeyStroke = Keys.DOWN_KEYSTROKE;
    private Consumer<Focusable> onFocusChanged;
    
    ConditionalEventHandler meAsEventHandler;
    Focusable               meAsFocusable;

    public DefaultFocusManager() {
        eventConsumers = new ArrayList<Focusable>();
    }

    public DefaultFocusManager(KeyStroke prevKeyStroke, KeyStroke nextKeyStroke) {
	this();
	this.prevKeyStroke = prevKeyStroke;
        this.nextKeyStroke = nextKeyStroke;
    }
    
    public DefaultFocusManager(KeyStroke nextKeyStroke) {
        this(null, nextKeyStroke);
    }

    public DefaultFocusManager setOnFocusChanged(Consumer<Focusable> onFocusChanged) {
        this.onFocusChanged = onFocusChanged;
        return this;
    }

    public Focusable getCurrentChildFocus() {
        return this.current;
    }

    private Focusable getCurrentOrSet(EventBus eventBus) {
	if (current == null && eventConsumers.size()>0) {
	    setChildAsCurrentFocus(eventConsumers.get(0), eventBus);
	}
	return this.current;
    }
    
    public void setChildAsCurrentFocus(Focusable focusable, EventBus eventBus) {

        if ( ! eventConsumers.contains(focusable) ) {
            throw new IllegalArgumentException("DefaultFocusManager:  Cannot set a focusable that has not been first aded to the DefaultFocusManager. ");
        }

        FocusManager.UTIL.setNewFocusable(focusable, this.current, eventBus, onFocusChanged);
	this.current = focusable;
    }

    public int size() {
        return eventConsumers.size();
    }

    private Focusable getPrev(EventBus eventBus) {
        if (size()<1) {
            return null;
        }
        int index = eventConsumers.indexOf( getCurrentOrSet(eventBus)  );
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
        int index = eventConsumers.indexOf( getCurrentOrSet(eventBus)  );
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

        chainedEventHandler.appendEventHandler(buildRequestFocusEventHandler());
        chainedEventHandler.appendEventHandler(buildKeyStrokeEventHandler());

        return chainedEventHandler.asConditionalEventHandler();
    }

    /**
     * Builds a focus event handler that looks for object within this focusManager's
     * descendant objects
     */
    private ConditionalEventHandler buildRequestFocusEventHandler() {

        ConditionalEventHandler requestFocusEventHandler = FocusManager.UTIL.buildRequestFocusEventHandler(this);

        return requestFocusEventHandler;
    }

    /**
     * Builds a keyStroke eventHandler.
     */
    private ConditionalEventHandler buildKeyStrokeEventHandler() {

        IS_Event_TYPE IS_KEYPRESS = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);

        Predicate<TerminalEvent<?>> canHandlePredicate = new Predicate<TerminalEvent<?>>() {

            @Override
            public boolean test(TerminalEvent<?> te) {
                if (IS_KEYPRESS.test(te)) {
                    KeyEvent ke = (KeyEvent) te;
                    if (prevKeyStroke != null && ke.isKeyStroke(prevKeyStroke)) {
                        return true;
                    } else if (nextKeyStroke != null && ke.isKeyStroke(nextKeyStroke)) {
                        return true;
                    } 
                }

                Focusable focusable = getCurrentOrSet(te.getEventBus());
                if (focusable != null) {
                    return focusable.canHandle(te) != null;
                }
                return false;
            }
        };

	return new ConditionalEventHandler.Impl(canHandlePredicate, (te) -> {
            if (IS_KEYPRESS.test(te)) {
        	    KeyEvent ke = (KeyEvent) te;
        	    
        	    if (prevKeyStroke != null && ke.isKeyStroke(prevKeyStroke)) {
        		Focusable prev = getPrev(te.getEventBus());
        		if (prev != null) {
        		    setChildAsCurrentFocus(prev, te.getEventBus());
        		    return;
        		}
        	    } else if (nextKeyStroke != null && ke.isKeyStroke(nextKeyStroke)) {
        		Focusable next = getNext(te.getEventBus());
        		if (next != null) {
        		    setChildAsCurrentFocus(next, te.getEventBus());
        		    return;
        		}
        	    } 
            }

	    Focusable focusable = getCurrentOrSet(te.getEventBus());
	    if (focusable != null) {
		focusable.handle(te);
	    }
	});
    }

    public void addFocusable(Focusable focusable, EventBus eventBus) {
	eventConsumers.add(focusable);
	if (eventConsumers.size() == 1) {
	    setChildAsCurrentFocus(focusable, eventBus);
	}
    }

    public void addFocusable(ToFocusable focusable, EventBus eventBus) {
        addFocusable( focusable.asFocusable(), eventBus );
    }
    
    
    private Focusable getAsFocusable() {
        
        if (meAsFocusable == null) {
            
            ConditionalEventHandler event_handler = DefaultFocusManager.this;
            
            meAsFocusable = new DefaultFocusableBehaviour() {
    
                @Override
                public void setFocus(boolean focus, EventBus eventBus) {
                    super.setFocus(focus, eventBus);
                    getCurrentOrSet(eventBus).setFocus(focus, eventBus);
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
