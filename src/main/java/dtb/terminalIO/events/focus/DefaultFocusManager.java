package dtb.terminalIO.events.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.traverse.HasChildren;

public class DefaultFocusManager implements FocusManager, HasChildren<Focusable> {

    private static final BiConsumer<Focusable, EventBus> DEFAULT_ONFOCUS_CHANGED = (f,e)-> e.raiseEvent(new RequestUpdateCanvasEvent(f)); 
    
    private List<Focusable> eventConsumers;
    private Focusable current;

    private KeyStroke prevKeyStroke = Keys.UP_KEYSTROKE;
    private KeyStroke nextKeyStroke = Keys.DOWN_KEYSTROKE;
    private BiConsumer<Focusable, EventBus> onFocusChanged = DEFAULT_ONFOCUS_CHANGED;

    private ConditionalEventHandler meAsEventHandler;
    private Focusable meAsFocusable;
    private boolean navKeysFirst = false;
    private boolean cycling = true;   // true mean cycle thru focus object

    public DefaultFocusManager() {
        eventConsumers = new ArrayList<Focusable>();
        this.navKeysFirst = true;
    }

    public DefaultFocusManager(KeyStroke prevKeyStroke, KeyStroke nextKeyStroke) {
        this();
        this.prevKeyStroke = prevKeyStroke;
        this.nextKeyStroke = nextKeyStroke;
    }

    public DefaultFocusManager(KeyStroke nextKeyStroke) {
        this(null, nextKeyStroke);
    }

    public DefaultFocusManager setNavKeysFirst(boolean navKeysFirst) {
        this.navKeysFirst = navKeysFirst;
        return this;
    }
    
    public DefaultFocusManager setOnFocusChanged(BiConsumer<Focusable, EventBus> onFocusChanged) {
        this.onFocusChanged = onFocusChanged;
        return this;
    }

    
    
    public DefaultFocusManager setCycling(boolean cycling) {
        this.cycling = cycling;
        return this;
    }

    public Focusable getCurrentChildFocus() {
        return this.current;
    }

    private Focusable getCurrentOrSet(EventBus eventBus) {
        if (current == null && eventConsumers.size() > 0) {
            setChildAsCurrentFocus(eventConsumers.get(0), eventBus);
        }
        
        if (current == null) {
            throw new IllegalStateException("ERROR: Trying to set current focus. No focusable objects added to this FocusManager.");
        }
        
        return this.current;
    }

    public void setChildAsCurrentFocus(Focusable focusable, EventBus eventBus) {

        if (!eventConsumers.contains(focusable)) {
            throw new IllegalArgumentException(
                    "DefaultFocusManager:  Cannot set a focusable that has not been first aded to the DefaultFocusManager. ");
        }

        FocusManager.UTIL.setNewFocusable(focusable, this.current, eventBus, onFocusChanged);
        this.current = focusable;
    }

    public int size() {
        return eventConsumers.size();
    }

    private Focusable getPrev(EventBus eventBus) {
        if (size() < 1) {
            return null;
        }
        int index = eventConsumers.indexOf(getCurrentOrSet(eventBus));
        if (index < 1) {
            if (cycling) {
               return eventConsumers.get(size() - 1);
            } else {
                return null;
            }
        }
        return eventConsumers.get(index - 1);
    }

    private Focusable getNext(EventBus eventBus) {
        if (size() < 1) {
            return null;
        }
        if (size() < 2) {
            return null;
        } 
        int index = eventConsumers.indexOf(getCurrentOrSet(eventBus));
        if (index < 0 || index == size() - 1) {
            if (cycling) {
                return eventConsumers.get(0);
            } else {
                return null;
            }
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
        if (this.meAsEventHandler == null) {
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

    private ConditionalEventHandler buildKeyStrokeEventHandler() {
        
        return new ConditionalEventHandler.AbstractDelegate() {
            
            ConditionalEventHandler navFirstHandler = null;
            ConditionalEventHandler navLastHandler = null;
            
            @Override
            protected ConditionalEventHandler getDelegate() {
                if (navKeysFirst) {
                    if (navFirstHandler == null) { // todo: synchro this part
                        navFirstHandler = buildNavKeysFirstKeyStrokeEventHandler();
                    }
                    return navFirstHandler;
                } else {
                    if (navLastHandler == null) { // todo: synchro this part
                        navLastHandler = buildNavKeysLastKeyStrokeEventHandler();
                    }
                    return navLastHandler;
                }
            }
        };
    }

    /**
     * Builds a keyStroke eventHandler.
     */
    private ConditionalEventHandler buildNavKeysFirstKeyStrokeEventHandler() {

        IS_Event_TYPE IS_KEYPRESS = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);

        Predicate<TerminalEvent<?>> canHandlePredicate = new Predicate<TerminalEvent<?>>() {

            @Override
            public boolean test(TerminalEvent<?> te) {
                // is it a FocusManager navigation key?
                if (IS_KEYPRESS.test(te)) {
                    KeyEvent ke = (KeyEvent) te;
                    if (prevKeyStroke != null && ke.isKeyStroke(prevKeyStroke) && getPrev(te.getEventBus()) != null) {
                        return true;
                    } else if (nextKeyStroke != null && ke.isKeyStroke(nextKeyStroke) && getNext(te.getEventBus()) != null) {
                        return true;
                    }
                }

                // FocusManager cannot handle directly - what about its focus child?
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


    /**
     * Builds a keyStroke eventHandler.
     */
    private ConditionalEventHandler buildNavKeysLastKeyStrokeEventHandler() {

        IS_Event_TYPE IS_KEYPRESS = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);

        Predicate<TerminalEvent<?>> canHandlePredicate = new Predicate<TerminalEvent<?>>() {

            @Override
            public boolean test(TerminalEvent<?> te) {
                
                Focusable focusable = getCurrentOrSet(te.getEventBus());
                if (focusable != null) {
                    if  (focusable.canHandle(te) != null) {
                        return true;
                    }
                }

                // not handled by current component - so check if a nav key

                if (IS_KEYPRESS.test(te)) {
                    KeyEvent ke = (KeyEvent) te;
                    if (prevKeyStroke != null && ke.isKeyStroke(prevKeyStroke) && getPrev(te.getEventBus()) != null) {
                        return true;
                    } else if (nextKeyStroke != null && ke.isKeyStroke(nextKeyStroke) && getNext(te.getEventBus()) != null) {
                        return true;
                    }
                }

                return false; // not handled
            }
        };

        return new ConditionalEventHandler.Impl(canHandlePredicate, (te) -> {
            
            Focusable focusable = getCurrentOrSet(te.getEventBus());
            if (focusable != null) {
                // ARG have to test again (is this 3 tests now!!!!)
                //                         very inefficient)
                //                      - might have to fix this
                if (focusable.canHandle(te)!=null) {
                    focusable.handle(te);
                    return;
                }
            }

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

        });
    }

    public void addFocusable(Focusable focusable) {
        if (eventConsumers.contains(focusable)) {
            throw new IllegalArgumentException("Duplicate add error: Focusable already added to this FocusManager.");
        }
        eventConsumers.add(focusable);
        
    }
    
    public void addFocusable(Focusable focusable, EventBus eventBus) {
        addFocusable(focusable);
        if (eventConsumers.size() == 1 && eventBus != null) {
            setChildAsCurrentFocus(focusable, eventBus);
        }
    }

    public void addFocusable(ToFocusable focusable, EventBus eventBus) {
        addFocusable(focusable.asFocusable(), eventBus);
    }

    public void addFocusable(ToFocusable focusable) {
        addFocusable(focusable.asFocusable(), null);
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

    @Override
    public Collection<Focusable> getChildFocusables() {
        return eventConsumers;
    }
}
