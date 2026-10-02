package dtb.terminalIO.events.focus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Enabled;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.keyboard.HasHotKey;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.util.Logging;

public class DefaultFocusManager implements FocusManager, 
                                             FocusablesContainer,
                                             Enabled,
                                             HasName<DefaultFocusManager>{

    private static final BiConsumer<Focusable, EventBus> DEFAULT_ONFOCUS_CHANGED_UPDATE_CANVAS = (f,e)-> e.raiseEvent(new RequestUpdateCanvasEvent(f)); 

    private FocusManager parentFocusManager;
    private FocusManager actualFocusmanager; // this is use
    private List<Focusable> eventConsumers;
    private List<Focusable> removeList;
    private Focusable current;

//    private KeyStroke prevKeyStroke = Keys.UP_KEYSTROKE;
//    private KeyStroke nextKeyStroke = Keys.DOWN_KEYSTROKE;
    private KeyStroke prevKeyStroke = Keys.SHIFT_TAB_KEYSTROKE;
    private KeyStroke nextKeyStroke = Keys.TAB_KEYSTROKE;
    private BiConsumer<Focusable, EventBus> onFocusChanged = DEFAULT_ONFOCUS_CHANGED_UPDATE_CANVAS;

    private ConditionalEventHandler meAsEventHandler;
    private ConditionalEventHandler customExtendedEventHandler;
    
    private FocusBehaviour meAsFocusable;
    private NAV_STYLE navKeysStyle = NAV_STYLE.NAVKEYS_LAST;
    private boolean tabStop = true;    // false: TAB / SHIFT-TAB skip this container (see FocusManager.isTabStop)
    private FocusPolicy focusPolicy = FocusPolicy.GROUP;  // what kind of container (see FocusPolicy); not yet used by the focus routing
    private boolean cycling = true;   // true means cycle thru focus object
    private boolean cyclingInferred = true;  

    private String name;

    public static enum NAV_STYLE {
        NAVKEYS_FIRST,
        NAVKEYS_LAST,
        NAVKEYS_NONE
    }

    public DefaultFocusManager() {
        eventConsumers = new ArrayList<Focusable>();
        removeList= new ArrayList<Focusable>();
        this.navKeysStyle = NAV_STYLE.NAVKEYS_FIRST;
    }

    public DefaultFocusManager(KeyStroke prevKeyStroke, KeyStroke nextKeyStroke, ConditionalEventHandler customExtendedEventHandler) {
        this();
        this.prevKeyStroke = prevKeyStroke;
        this.nextKeyStroke = nextKeyStroke;
        this.customExtendedEventHandler = customExtendedEventHandler;
    }

    public DefaultFocusManager(KeyStroke prevKeyStroke, KeyStroke nextKeyStroke) {
        this(prevKeyStroke, nextKeyStroke, null);
    }
    
    public ConditionalEventHandler getCustomExtendedEventHandler() {
        return customExtendedEventHandler;
    }

    public void setCustomExtendedEventHandler(ConditionalEventHandler customExtendedEventHandler) {
        this.customExtendedEventHandler = customExtendedEventHandler;
    }

    public String getName() {
        return name;
    }

    public DefaultFocusManager setName(String name) {
        this.name = name;
        return this;
    }
    
    public DefaultFocusManager setNameFromOwner(Object owner) {
        setDefaultNameFromParentObject(this, owner);
        return this;
    }


    public DefaultFocusManager setPrevKeyStroke(KeyStroke prevKeyStroke) {
        this.prevKeyStroke = prevKeyStroke;
        return this;
    }

    public DefaultFocusManager setNextKeyStroke(KeyStroke nextKeyStroke) {
        this.nextKeyStroke = nextKeyStroke;
        return this;
    }

    public DefaultFocusManager(KeyStroke nextKeyStroke) {
        this(null, nextKeyStroke);
    }

    public DefaultFocusManager setNavKeysFirst(boolean navKeysFirst) {
        this.navKeysStyle = navKeysFirst ? NAV_STYLE.NAVKEYS_FIRST : NAV_STYLE.NAVKEYS_LAST;
        return this;
    }

    public DefaultFocusManager setNavKeysNone() {
        this.navKeysStyle = NAV_STYLE.NAVKEYS_NONE;
        return this;
    }

    public DefaultFocusManager setOnFocusChanged(BiConsumer<Focusable, EventBus> onFocusChanged) {
        this.onFocusChanged = onFocusChanged;
        return this;
    }

    /** What kind of container this is: GROUP (default), WIDGET or TRAP - see FocusPolicy. Not yet used by the focus routing. */
    public DefaultFocusManager setFocusPolicy(FocusPolicy focusPolicy) {
        this.focusPolicy = focusPolicy;
        return this;
    }

    /** false: TAB / SHIFT-TAB skip this container and everything in it; clicks and hot keys still work */
    public DefaultFocusManager setTabStop(boolean tabStop) {
        this.tabStop = tabStop;
        return this;
    }

    @Override
    public boolean isTabStop() {
        return tabStop;
    }

    @Override
    public FocusPolicy getFocusPolicy() {
        return focusPolicy;
    }

    @Override
    public FocusDirection navDirectionFor(KeyStroke key) {
        if (navKeysStyle == NAV_STYLE.NAVKEYS_NONE || key == null) {
            return null;
        }
        if (key.equals(nextKeyStroke)) {
            return FocusDirection.FORWARD;
        }
        if (key.equals(prevKeyStroke)) {
            return FocusDirection.BACKWARD;
        }
        return null;
    }

    public DefaultFocusManager setCycling(boolean cycling) {
        this.cycling = cycling;
        cyclingInferred = false;  // has been programmatically set
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
            //throw new IllegalStateException("ERROR: Trying to set current focus. No focusable objects added to this FocusManager("+HasName.UTIL.getName(this)+").");
            return null;
        }
        
        return this.current;
    }

    public void setChildAsCurrentFocus(Focusable focusable, EventBus eventBus) {
        
        if (focusable != null && !focusable.isEnabled()) {
            Logging.info("Cannot set focus.  Focusable is null or disabled");
            return;
        }

        if (!eventConsumers.contains(focusable)) {
            FocusManager parent = focusable.getParentFocusManager();
            if (focusable != null && eventConsumers.contains(parent)) {
                //focusable = focusable.getParentFocusManager();
                setChildAsCurrentFocus(focusable.getParentFocusManager(), eventBus);
            } else {
                if (focusable != null && (focusable instanceof Component) && eventConsumers.contains(((Component)focusable).getParentContainer()) ) {
                    setChildAsCurrentFocus((Focusable)((Component)focusable).getParentContainer(), eventBus);
                } else {
                    String focusableName = focusable != null ? HasName.UTIL.getName(focusable) : "<NULL_FOCUSABLE>";
                    throw new IllegalArgumentException(
                            "DefaultFocusManager("+getName()+"):  Cannot set as focusable ("+
                                    focusableName+") that has not been first added to the DefaultFocusManager "+HasName.UTIL.getName(this)+". ");
                }
            }
        }

        if (this.hasFocus()) {
           FocusManager.UTIL.setNewFocusable(focusable, this.current, eventBus, onFocusChanged);
        }
        this.current = focusable;
    }

    public int size() {
        return eventConsumers.size();
    }

    private Focusable getPrevOLD(EventBus eventBus) {
        if (size() < 1) {
            return null;
        }
        Focusable theCurrent = getCurrentOrSet(eventBus);
        int index = (theCurrent == null) ? -1 : eventConsumers.indexOf(theCurrent);
        if (index < 1) {
            if (cycling) {
               return eventConsumers.get(size() - 1);
            } else {
                return null;
            }
        }
        return eventConsumers.get(index - 1);
    }

    private Focusable getPrev(EventBus eventBus) {
        if (size() < 1 || !isEnabled()) {
            return null; // Early exit if no focusables or all are disabled
        }

        Focusable theCurrent = getCurrentOrSet(eventBus);
        int index = (theCurrent == null) ? -1 : eventConsumers.indexOf(theCurrent);
        int startIndex = index;

        do {
            index = (index < 1) ? (cycling ? size() - 1 : -1) : index - 1;

            // Exit if we've completed a full cycle
            if (index == startIndex || index == -1) {
                return null;
            }
        } while (!eventConsumers.get(index).isEnabled());

        return eventConsumers.get(index);
    }    
    
    
    private Focusable getNextOLD(EventBus eventBus) {
        if (size() < 2) {
            return null;
        }
        Focusable theCurrent = getCurrentOrSet(eventBus);
        int index = (theCurrent == null) ? -1 : eventConsumers.indexOf(theCurrent);
        if (index < 0 || index == size() - 1) {
            if (cycling) {
                return eventConsumers.get(0);
            } else {
                return null;
            }
        }
        return eventConsumers.get(index + 1);
    }


    private Focusable getNext(EventBus eventBus) {
        if (size() < 2 || !isEnabled()) {
            return null; // Early exit if no focusables or all are disabled
        }
    
        Focusable theCurrent = getCurrentOrSet(eventBus);
        int index = (theCurrent == null) ? -1 : eventConsumers.indexOf(theCurrent);
        int startIndex = index;
    
        do {
            index = (index < 0 || index == size() - 1) ? (cycling ? 0 : -1) : index + 1;
    
            // Exit if we've completed a full cycle
            if (index == startIndex || index == -1) {
                return null;
            }
        } while (!eventConsumers.get(index).isEnabled());
    
        return eventConsumers.get(index);
    }
    
    
    
    @Override
    public void handle(TerminalEvent<?> te) {
        FocusNavigator.Decision navigation = rootNavigation(te);
        if (navigation != null) {
            FocusNavigator.act(this, navigation, (KeyEvent) te);
            return;
        }
        getEventHandler().handle(te);
    }

    /**
     * Keyboard navigation (TAB / SHIFT-TAB ...) is decided by the ROOT focus manager (no parent)
     * using the containers' enter / advance answers - see FocusNavigator. Returns null when this
     * is not the root, the event is not a key, or the key is not navigation: then the event goes
     * through the routing below as before.
     */
    private FocusNavigator.Decision rootNavigation(TerminalEvent<?> te) {
        if (parentFocusManager != null || !(te instanceof KeyEvent)) {
            return null;
        }
        return FocusNavigator.decide(this, (KeyEvent) te);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (rootNavigation(te) != null) {
            return this;
        }
        ConditionalEventHandler evtHandler = getEventHandler();
        if (te.getType() == TerminalEvent.TYPE.KEYPRESS) {
            if (evtHandler.canHandle(te) == null) {
                return null;
            }
        }
        if (evtHandler.canHandle(te) == null) {
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

        ChainedEventHandler chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);

        chainedEventHandler.appendEventHandler(HasName.UTIL.decorateWithParentsNamePrefix(buildRequestFocusEventHandler(),chainedEventHandler));
        chainedEventHandler.appendEventHandler(HasName.UTIL.decorateWithParentsNamePrefix(buildKeyStrokeEventHandler(),chainedEventHandler));

        ConditionalEventHandler.AbstractDelegate customHandlerWrapper = new ConditionalEventHandler.AbstractDelegate() {

            @Override
            protected ConditionalEventHandler getDelegate() {
                if (customExtendedEventHandler == null) {
                    return ConditionalEventHandler.NOOP;
                }
                return customExtendedEventHandler;
            }
        };
        customHandlerWrapper.setName((this.getName()!=null?this.getName():"DefaultFocusManager")+ "customHandlerWrapper");

       chainedEventHandler.appendEventHandler(HasName.UTIL.decorateWithParentsNamePrefix(customHandlerWrapper,chainedEventHandler));

        return HasName.UTIL.decorateWithParentsNamePrefix(chainedEventHandler.asConditionalEventHandler(),DefaultFocusManager.this);
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

        AbstractDelegate keyStrokeEventHandler = new ConditionalEventHandler.AbstractDelegate() {

            ConditionalEventHandler navFirstHandler = null;
            ConditionalEventHandler navLastHandler = null;

            @Override
            protected ConditionalEventHandler getDelegate() {
                if (navKeysStyle.equals(NAV_STYLE.NAVKEYS_FIRST)) {
                    if (navFirstHandler == null) { // todo: synchro this part
                        navFirstHandler = buildNavKeysFirstKeyStrokeEventHandler();
                    }
                    HasName.UTIL.setDefaultNameFromParentObject(navFirstHandler, this, "evtHandler-NAVKEYS_FIRST");
                    return navFirstHandler;
                } else if (navKeysStyle.equals(NAV_STYLE.NAVKEYS_LAST))  {
                    if (navLastHandler == null) { // todo: synchro this part
                        navLastHandler = buildNavKeysLastKeyStrokeEventHandler();
                    }
                    HasName.UTIL.setDefaultNameFromParentObject(navLastHandler, this, "evtHandler-NAVKEYS_LAST");
                    return navLastHandler;
                } else if (navKeysStyle.equals(NAV_STYLE.NAVKEYS_NONE))  {
                    if (navLastHandler == null) { // todo: synchro this part
                        navLastHandler = buildNavKeysNoneKeyStrokeEventHandler();
                    }
                    HasName.UTIL.setDefaultNameFromParentObject(navLastHandler, this, "evtHandler-NAVKEYS-NONE");
                    return navLastHandler;
                } else {
                    throw new IllegalStateException("Nav style not defined! for "+HasName.UTIL.getName(this));
                }
            }
        };
        keyStrokeEventHandler.setName((this.getName()!=null?this.getName():"DefaultFocusManager")+ ":keyStrokeNavHandlerWrapper");
        return keyStrokeEventHandler;
    }

    /**
     * Simultaneously detects if a hotkey has been pressed and if so,
     * sets the component containing the hotkey as currently focused child
     */
    private boolean onHotKeyPressedSetAsCurrent(KeyEvent te) {
        KeyStroke pressedKey = ((KeyEvent) te).getKeyStroke();

        Focusable focusable = getCurrentOrSet(te.getEventBus());

        for (Focusable focusableOther : eventConsumers) {
            if (focusableOther == focusable) {
                continue;
            }

            Collection<KeyStroke> hotKeys = HasHotKey.UTIL.getAllHotKeys(focusableOther);
            if (hotKeys.contains(pressedKey)) {
                Focusable foundFocusable = HasHotKey.UTIL.getFocusableFromHotKey(focusableOther, pressedKey);
                if (foundFocusable == null) {
                    return false;
                }

                setChildAsCurrentFocus(focusableOther, te.getEventBus());
                //foundFocusable.setFocus(true, te.getEventBus());
                if (foundFocusable.canHandle(te) != null) {
                    return true;
                }
            }
        }

        return false;
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
                    if (focusable.canHandle(te) != null) {
                        return true;
                    }
                }

                // at this point let's look at hot keys..

                if (!IS_KEYPRESS.test(te)) {
                    // not a key press so hunt for another focusable that might be able to handle this event
                    if (size()>0) {
                       for (Focusable otherFocusable : getChildFocusables()) {
                           if (otherFocusable != focusable) {
                               if (otherFocusable.canHandle(te) != null) {
                                   return true;
                               }
                           }
                       }
                    }

                    return false; // not a keyboard event, so not a hotkey and not handled by any child focusable
                }

                boolean handled = onHotKeyPressedSetAsCurrent((KeyEvent) te);

                return handled;

            }
        };

        return new ConditionalEventHandler.Impl(canHandlePredicate, (te) -> {
            if (IS_KEYPRESS.test(te)) {
                KeyEvent ke = (KeyEvent) te;

                if (prevKeyStroke != null && ke.isKeyStroke(prevKeyStroke)) {
                    if (setPreviousAsCurrentFocus(te.getEventBus())) {
                        return;
                    }
                } else if (nextKeyStroke != null && ke.isKeyStroke(nextKeyStroke)) {
                    if (setNextAsCurrentFocus(te.getEventBus())) {
                        return;
                    }
                }
            }

            // not a navigation key so forward event to current focusable
            Focusable focusable = getCurrentOrSet(te.getEventBus());
            if (focusable != null) {
                if (focusable.canHandle(te) != null) { 
                     focusable.handle(te);
                     return;
                }
            }

            if (!IS_KEYPRESS.test(te)) {

                // not a key press so hunt for another focusable that might be able to handle this event
                if (size()>0) {
                   for (Focusable otherFocusable : getChildFocusables()) {
                       if (otherFocusable != focusable) {
                           if (otherFocusable.canHandle(te) != null) {
                               otherFocusable.handle(te);
                               return;
                           }
                       }
                   }
                }
            }
        }).setName(DefaultFocusManager.this.getName()+"-NavKeysFirstKeyStrokeEventHandler");
    }

    public boolean setPreviousAsCurrentFocus(EventBus eventBus) {
        Focusable prev = getPrev(eventBus);
        if (prev != null) {
            setChildAsCurrentFocus(prev, eventBus);
            return true;
        }
        return false;
    }

    public boolean setNextAsCurrentFocus(EventBus eventBus) {
        Focusable next = getNext(eventBus);
        if (next != null) {
            setChildAsCurrentFocus(next, eventBus);
            return true;
        }
        return false;
    }


    /**
     * Builds a keyStroke eventHandler.
     */
    private ConditionalEventHandler buildNavKeysLastKeyStrokeEventHandler() {

        IS_Event_TYPE IS_KEYPRESS = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);

        Predicate<TerminalEvent<?>> canHandlePredicate = new Predicate<TerminalEvent<?>>() {

            @Override
            public boolean test(TerminalEvent<?> te) {
                
                Focusable focusable = null;
                focusable = getCurrentOrSet(te.getEventBus());
                if (focusable != null) {
                    if  (focusable.canHandle(te) != null) {
                        return true;
                    }
                }

                // not handled by current component - so check if a nav key

                if (IS_KEYPRESS.test(te)) {
                    KeyEvent ke = (KeyEvent) te;
                    
                    // at this point let's look at hot keys..
                    // onHotKeyPressedSetAsCurrent()
                    if ( onHotKeyPressedSetAsCurrent((KeyEvent) te)) {
                        return true;
                    }

                    if (prevKeyStroke != null && ke.isKeyStroke(prevKeyStroke) && getPrev(te.getEventBus()) != null) {
                        return true;
                    } else if (nextKeyStroke != null && ke.isKeyStroke(nextKeyStroke) && getNext(te.getEventBus()) != null) {
                        return true;
                    }
                } else {  // not a key press so hunt for another focusable that might be able to handle this event
                    if (size()>0) {
                       for (Focusable otherFocusable : getChildFocusables()) {
                           if (otherFocusable != focusable) {
                               if (otherFocusable.canHandle(te) != null) {
                                   return true;
                               }
                           }
                       }
                    }
                }

                return false; // not handled
            }
        };

        return new ConditionalEventHandler.Impl(canHandlePredicate, (te) -> {
            
            Focusable focusable = getCurrentOrSet(te.getEventBus());
            if (focusable != null) {
                // ARG have to test again (is this 3 tests now !?!?)
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
                    if (setPreviousAsCurrentFocus(te.getEventBus())) {
                        return;
                    }
                } else if (nextKeyStroke != null && ke.isKeyStroke(nextKeyStroke)) {
                    if (setNextAsCurrentFocus(te.getEventBus())) {
                        return;
                    }
                }
            } else {  // not a key press
                if (size()>0) {
                    List<Focusable> childFocusables = new ArrayList<Focusable>(getChildFocusables());
                    for (Focusable otherFocusable : childFocusables) {
                        if (otherFocusable != focusable) {
                            if (otherFocusable.canHandle(te) != null) {
                                otherFocusable.handle(te);
                            }
                        }
                    }
                }
            }

        }).setName(DefaultFocusManager.this.getName()+"-NavKeysLastKeyStrokeEventHandler");
    }

    /**
     * Builds a keyStroke eventHandler.
     */
    private ConditionalEventHandler buildNavKeysNoneKeyStrokeEventHandler() {

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

                // not handled by current component - so check if hot keys hit

                if (IS_KEYPRESS.test(te)) {
                    // at this point let's look at hot keys..
                    if ( onHotKeyPressedSetAsCurrent((KeyEvent) te)) {
                        return true;
                    }
                } else {  // not a key press so hunt for another focusable that might be able to handle this event
                    if (size()>0) {
                        for (Focusable otherFocusable : getChildFocusables()) {
                            if (otherFocusable != focusable) {
                                if (otherFocusable.canHandle(te) != null) {
                                    return true;
                                }
                            }
                        }
                     }
                 }

                return false; // not handled
            }
        };

        return new ConditionalEventHandler.Impl(canHandlePredicate, (te) -> {

            Focusable focusable = getCurrentOrSet(te.getEventBus());
            if (focusable != null) {
                // ARG have to test again (is this 3 tests now !?!?)
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
                    if (setPreviousAsCurrentFocus(te.getEventBus())) {
                        return;
                    }
                } else if (nextKeyStroke != null && ke.isKeyStroke(nextKeyStroke)) {
                    if (setNextAsCurrentFocus(te.getEventBus())) {
                        return;
                    }
                }
            } else {  // not a key press
                if (size()>0) {
                    for (Focusable otherFocusable : getChildFocusables()) {
                        if (otherFocusable != focusable) {
                            if (otherFocusable.canHandle(te) != null) {
                                otherFocusable.handle(te);
                            }
                        }
                    }
                }
            }

        }).setName(DefaultFocusManager.this.getName()+"-NavKeysNoneKeyStrokeEventHandler");
    }

    public void removeFocusable(Focusable focusable) {
        if (removeList.contains(focusable)) {
            // recursive situation - its ok (and sometimes expected) .. but let's get out
        }
        if (!eventConsumers.contains(focusable)) {
            throw new IllegalArgumentException(HasName.UTIL.getName(this)+".removeFocusable("+HasName.UTIL.getName(focusable)+"): Trying to remove a focusable that is not in the list of focusables!");
        }
        if (current == focusable) {
            current = null;
        }
        removeList.add(focusable);
        eventConsumers.remove(focusable);
        focusable.setParentFocusManager(null);
        removeList.remove(focusable);
    }

    private FocusManager meOrActual() {
        return this.actualFocusmanager != null ? this.actualFocusmanager : this;
    }

    public void setActual(FocusManager actualFocusmanager) {
        this.actualFocusmanager = actualFocusmanager;
    }
    
    public void insertFocusable(int index, Focusable focusable) {
        if (eventConsumers.contains(focusable)) {
            throw new IllegalArgumentException(HasName.UTIL.getName(this)+".insertFocusable("+HasName.UTIL.getName(focusable)+"): Duplicate add error: Focusable already added to this FocusManager.");
        }
        eventConsumers.add(index, focusable);
        focusable.setParentFocusManager(this.meOrActual());
    }
    
    public void addFocusable(Focusable focusable) {
        if (eventConsumers.contains(focusable)) {
            throw new IllegalArgumentException(HasName.UTIL.getName(this)+".addFocusable("+HasName.UTIL.getName(focusable)+"): Duplicate add error: Focusable already added to this FocusManager.");
        }
        eventConsumers.add(focusable);
        focusable.setParentFocusManager(this.meOrActual());
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
    
    public void addComponentAsFocusables(Component<?> component) {
        Collection<Focusable> focusablesFromComponent = FocusManager.UTIL.focusablesFromComponent(component);
        if (focusablesFromComponent != null) {
            focusablesFromComponent.forEach((f)->this.addFocusable(f));
        }
    }

    public void removeComponentFocusables(Component<?> component) {
        Collection<Focusable> focusablesFromComponent = FocusManager.UTIL.focusablesFromComponent(component);
        if (focusablesFromComponent != null) {
            focusablesFromComponent.forEach((f)->this.removeFocusable(f));
        }
    }
    
    private boolean applyOnCurrentFocus(EventBus eventBus, Consumer<Focusable> operation) {
        Focusable theCurrent = getCurrentOrSet(eventBus);
        if (theCurrent == null) {
            return false;
        }
        operation.accept(theCurrent);
        return true;
    }

    private FocusBehaviour getAsFocusable() {

        if (meAsFocusable == null) {

            meAsFocusable = new DefaultFocusableBehaviour(this) {

                @Override
                public void setFocus(boolean focus, EventBus eventBus) {
                    super.setFocus(focus, eventBus);
                    applyOnCurrentFocus(eventBus,(c)->c.setFocus(focus, eventBus));
                }
                
                @Override
                public void unFocus(Focusable newFocusable, EventBus eventBus) {
                    super.unFocus(newFocusable, eventBus);
                    applyOnCurrentFocus(eventBus,(c)->c.unFocus(newFocusable, eventBus));
                }

                @Override
                public void focus(Focusable oldFocusable, EventBus eventBus) {
                    super.focus(oldFocusable, eventBus);
                    applyOnCurrentFocus(eventBus,(c)->c.focus(oldFocusable, eventBus));
                }
                
            };
        }
        return meAsFocusable;

    }

    
    @Override
    public FocusManager getParentFocusManager() {
        return parentFocusManager;
    }

    @Override
    public void setParentFocusManager(FocusManager parentFocusManager) {
        if (parentFocusManager == this.meOrActual()) {
            throw new IllegalArgumentException("Attempted to assign parentFocusManager to itself.");
        }

        Focusable.UTIL.setParentFocusManager(this, parentFocusManager, (fm)-> this.parentFocusManager = fm);
        if (parentFocusManager != null && cyclingInferred && cycling) {
            this.setCycling(false);
            cyclingInferred= true;
        } else {
            if (cyclingInferred) {
                this.setCycling(true);
                cyclingInferred= true;
            }
        }
        
        //this.parentFocusManager = parentFocusManager;
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

    public void unFocus(Focusable newFocusable, EventBus eventBus) {
        getAsFocusable().unFocus(newFocusable, eventBus);
    }
    
    public void focus(Focusable oldFocusable, EventBus eventBus) {
        getAsFocusable().focus(oldFocusable, eventBus);
    }
    
    @Override
    public String toString() {
        return (this.getName()!=null?this.getName():"DefaultFocusManager")+
                ": [size =" + eventConsumers.size() + "]";
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        return eventConsumers;
    }

    public static boolean applyIfDefaultFocusManager(Object o, Consumer<DefaultFocusManager> apply) {
        if (o == null) {
            throw new NullPointerException("null object (o) passed to DefaultFocusManager.applyIfDefaultFocusManager()");
        }
        if (o instanceof DefaultFocusManager) {
            apply.accept((DefaultFocusManager)o);
            return true;
        }
        if (o instanceof FocusManagerProvider) {
            return applyIfDefaultFocusManager(((FocusManagerProvider)o).getFocusManager(), apply);
        }
        
        return false;
    }
    
    public static void setDefaultNameAndActualFromParentObject(FocusManager focusManager, Object parentContainer) {
        
        applyIfDefaultFocusManager(focusManager, (dfaltFocusManager)->{
            if (parentContainer instanceof FocusManager) {
                HasName.UTIL.setDefaultNameFromParentObject(dfaltFocusManager, parentContainer, "FocusManager");
                ((DefaultFocusManager)dfaltFocusManager).setActual((FocusManager)parentContainer);
            }
        });
        HasName.UTIL.setDefaultNameFromParentObject(focusManager, parentContainer, "FocusManager");
    }
    public static void setDefaultNameFromParentObject(FocusManager focusManager, Object parentContainer) {
        HasName.UTIL.setDefaultNameFromParentObject(focusManager, parentContainer, "FocusManager");
    }

    @Override
    public boolean isEnabled() {
        // if any child is enabled, then we are enabled.
        return getChildren().stream().anyMatch( f -> f.isEnabled());
    }

}
