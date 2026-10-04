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
    // Only used on a ROOT focus manager (one with no parent): THE component that has keyboard focus.
    // The single answer to "who has focus?" - updated by every focus move (FocusManager.UTIL.moveFocus,
    // setChildAsCurrentFocus). Components' own hasFocus() flags are display state (hilite) kept in step.
    private Focusable focusedComponent;
    // ROOT only: the last few components that had focus, most recent first - so keyboard navigation
    // can carry on from where focus was when the focused component goes away (e.g. a dropdown's list
    // closing: focus was in the list, TAB carries on from the dropdown)
    private static final int RECENTLY_FOCUSED_MAX = 8;
    private final List<Focusable> recentlyFocused = new ArrayList<Focusable>();

    // initial focus (see setInitialFocus / setAutoFocus / giveInitialFocus)
    private boolean autoFocus = true;
    private Object initialFocus = null;
    private boolean tabStop = true;    // false: TAB / SHIFT-TAB skip this focus manager (see FocusManager.isTabStop)
    private FocusPolicy focusPolicy = FocusPolicy.GROUP;  // what kind of focus manager (see FocusPolicy)
    private boolean cycling = false;  // ONE_STOP only: wraps around from its last child to its first (see wrapsAround)

    private String name;


    public DefaultFocusManager() {
        eventConsumers = new ArrayList<Focusable>();
        removeList= new ArrayList<Focusable>();
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

    /** see setCycling */
    @Override
    public boolean wrapsAround() {
        return cycling;
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

    /** no nav keys: keys never move focus between this manager's children (they can still be clicked) */
    public DefaultFocusManager setNavKeysNone() {
        this.prevKeyStroke = null;
        this.nextKeyStroke = null;
        return this;
    }

    public DefaultFocusManager setOnFocusChanged(BiConsumer<Focusable, EventBus> onFocusChanged) {
        this.onFocusChanged = onFocusChanged;
        return this;
    }

    /** What kind of focus manager this is: GROUP (default), ONE_STOP or TRAP - see FocusPolicy. */
    public DefaultFocusManager setFocusPolicy(FocusPolicy focusPolicy) {
        this.focusPolicy = focusPolicy;
        return this;
    }

    /**
     * The component that has keyboard focus, as recorded by this ROOT focus manager (null if none
     * yet). Prefer FocusManager.UTIL.findCurrentFocus(root), which also checks it is still attached.
     */
    public Focusable getFocusedComponent() {
        return focusedComponent;
    }

    // called by FocusManager.UTIL after focus moved
    void recordFocusedComponent(Focusable focusable) {
        this.focusedComponent = focusable;
        if (focusable != null && focusable != this) {
            recentlyFocused.remove(focusable);
            recentlyFocused.add(0, focusable);
            if (recentlyFocused.size() > RECENTLY_FOCUSED_MAX) {
                recentlyFocused.remove(RECENTLY_FOCUSED_MAX);
            }
        }
    }

    /** the components that had focus recently, most recent first (ROOT only; some may be gone by now) */
    List<Focusable> getRecentlyFocused() {
        return new ArrayList<Focusable>(recentlyFocused);
    }

    /**
     * Which component giveInitialFocus() focuses. Default (null): the first focusable component.
     * Example: a form that should start on its "name" field, or a dialog on its OK button.
     */
    public DefaultFocusManager setInitialFocus(Object component) {
        this.initialFocus = component;
        return this;
    }

    /** false: giveInitialFocus() does nothing - nothing has focus until the user clicks something. */
    public DefaultFocusManager setAutoFocus(boolean autoFocus) {
        this.autoFocus = autoFocus;
        return this;
    }

    /**
     * Gives keyboard focus to the initial component (setInitialFocus, else the first focusable one)
     * - if nothing in here has focus yet, and autoFocus is on. Called by TUIApp when the app has
     * started (so TAB and typing work without clicking first); a dialog can call it when shown.
     */
    public void giveInitialFocus(EventBus eventBus) {
        if (!autoFocus || FocusManager.UTIL.findCurrentFocus(this) != null) {
            return;
        }
        Focusable target = initialFocus != null ? ToFocusable.UTIL.toFocusable(initialFocus)
                                                : enter(FocusDirection.FORWARD);
        if (target != null) {
            FocusManager.UTIL.moveFocus(this, target, eventBus, false);
        }
    }

    /** false: TAB / SHIFT-TAB skip this focus manager and everything in it; clicks and hot keys still work */
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
        if (key == null) {
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

    /**
     * For a ONE_STOP manager: true = its nav keys wrap around, from the last child to the first and back
     * (e.g. a tab strip). Default false: focus stays at the ends. Ignored for GROUP / TRAP (a GROUP
     * hands on to its parent at the ends; a TRAP and the root always wrap).
     */
    public DefaultFocusManager setCycling(boolean cycling) {
        this.cycling = cycling;
        return this;
    }

    /** the remembered current child - null if it is no longer one of the children (e.g. removed) */
    public Focusable getCurrentChildFocus() {
        return this.current != null && children().contains(this.current) ? this.current : null;
    }

    /**
     * The current child to offer event te to first. For key events: the current child, picking the
     * first one if there is none (keyboard behaviour as before). For any other event (canvas refresh,
     * value changed ...): only an existing current child - never pick one. Picking one there gave
     * focus to an arbitrary first child (e.g. a window title, bringing that window to front) whenever
     * the current child had just been removed, e.g. a dropdown's open list being closed.
     */
    private Focusable currentChildFor(TerminalEvent<?> te) {
        if (te.getType() == TerminalEvent.TYPE.KEYPRESS) {
            return getCurrentOrSet(te.getEventBus());
        }
        return getCurrentChildFocus();     // only while it is still a child (not removed since)
    }

    /**
     * The current child, or the first child if there is none yet - WITHOUT setting it.
     * Used where we only answer a question (canHandle, "is there a next one?").
     */
    private Focusable peekCurrent() {
        if (getCurrentChildFocus() != null) {
            return current;
        }
        List<Focusable> children = children();
        return children.isEmpty() ? null : children.get(0);
    }

    /** like currentChildFor(te), but never changes anything (for canHandle) */
    private Focusable peekCurrentChildFor(TerminalEvent<?> te) {
        return te.getType() == TerminalEvent.TYPE.KEYPRESS ? peekCurrent() : getCurrentChildFocus();
    }

    private Focusable getCurrentOrSet(EventBus eventBus) {
        List<Focusable> children = children();
        if (getCurrentChildFocus() == null && children.size() > 0) {
            setChildAsCurrentFocus(children.get(0), eventBus);
        }

        return getCurrentChildFocus();     // null if there are no children (or the remembered one was removed)
    }

    public void setChildAsCurrentFocus(Focusable focusable, EventBus eventBus) {
        
        if (focusable != null && !focusable.isEnabled()) {
            Logging.info("Cannot set focus.  Focusable is null or disabled");
            return;
        }

        List<Focusable> children = children();
        if (!children.contains(focusable)) {
            FocusManager parent = FocusManager.UTIL.parentOf(focusable);
            if (focusable != null && children.contains(parent)) {
                setChildAsCurrentFocus(parent, eventBus);
            } else {
                if (focusable != null && (focusable instanceof Component) && children.contains(((Component)focusable).getParentContainer()) ) {
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
        if (this.hasFocus()) {
            // focus moved inside a focus manager that has focus: keep the root's record of THE focused component current
            FocusManager.UTIL.recordFocusFromCurrentChildren(this);
        }
    }

    /**
     * This manager's children: worked out from its component when it has one (so they are never out
     * of date - see FocusManager.UTIL.derivedChildFocusables); a manager built by hand uses the
     * focusables added to it.
     */
    private List<Focusable> children() {
        List<Focusable> derived = FocusManager.UTIL.derivedChildFocusables(this);
        return derived != null ? derived : eventConsumers;
    }

    public int size() {
        return children().size();
    }


    /**
     * The next (FORWARD) or previous (BACKWARD) enabled child after the current one, wrapping around
     * if set to cycle; null if there is none. For moving between children by code, e.g. a tab strip's
     * "<-" / "->" buttons (see setNextAsCurrentFocus).
     */
    private Focusable neighbourChild(FocusDirection dir) {
        if (size() < 1 || !isEnabled()) {
            return null;
        }
        int step = dir == FocusDirection.FORWARD ? 1 : -1;
        List<Focusable> children = children();
        int from = children.indexOf(peekCurrent());
        int index = from;
        for (int n = 0; n < size(); n++) {
            index += step;
            if (index < 0 || index >= size()) {
                if (!cycling) {
                    return null;
                }
                index = (index + size()) % size();
            }
            if (index == from) {
                return null;
            }
            if (children.get(index).isEnabled()) {
                return children.get(index);
            }
        }
        return null;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        // 1. a navigation root (the application's root, or a modal dialog) delivers every key for
        //    everything below it: from the focused component upwards (see KeyRouter)
        if (isNavigationRoot() && te instanceof KeyEvent
                && KeyRouter.route(this, (KeyEvent) te, true) != KeyRouter.Outcome.NOT_ROUTED) {
            return;
        }
        // 2. everything else: this manager's routing chain - focus requests (clicks), then the current
        //    child / hot keys / other children, then the custom extra handler
        getEventHandler().handle(te);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        // only answers "is this event ours?" - changes nothing (same two steps as handle)

        // 1. a navigation root delivers every key for everything below it
        if (isNavigationRoot() && te instanceof KeyEvent) {
            KeyRouter.Outcome outcome = KeyRouter.route(this, (KeyEvent) te, false);
            if (outcome != KeyRouter.Outcome.NOT_ROUTED) {
                return outcome == KeyRouter.Outcome.HANDLED ? this : null;
            }
        }
        // 2. everything else: does this manager's routing chain take it?
        if (getEventHandler().canHandle(te) == null) {
            return null;
        }
        return this;
    }

    /**
     * Does this manager deliver keys (and so decide keyboard navigation) for everything below it? Yes for the application's root (no parent), and for a TRAP - a modal
     * dialog, which gets keys before the application's root does.
     */
    private boolean isNavigationRoot() {
        return FocusManager.UTIL.parentOf(this) == null || focusPolicy == FocusPolicy.TRAP;
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
        chainedEventHandler.appendEventHandler(HasName.UTIL.decorateWithParentsNamePrefix(buildChildrenEventHandler(),chainedEventHandler));

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

    /**
     * Offers an event to this manager's children (keys normally don't come here: a navigation root
     * delivers them from the focused component upwards - see KeyRouter; only when nothing has focus):
     *   1. the current child (the one on the focus path), e.g. a key typed into the focused field
     *   2. a key: a child whose hot key it is (e.g. F2 on a button) - it becomes current and gets the key
     *   3. any other event (value changed, canvas refresh ...): the first other child that claims it
     */
    private ConditionalEventHandler buildChildrenEventHandler() {

        IS_Event_TYPE IS_KEYPRESS = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);

        Predicate<TerminalEvent<?>> canHandlePredicate = (te) -> {
            Focusable focusable = peekCurrentChildFor(te);
            if (focusable != null && focusable.canHandle(te) != null) {
                return true;
            }
            if (IS_KEYPRESS.test(te)) {
                return hotKeyChildFor((KeyEvent) te) != null;
            }
            return otherChildClaiming(te, focusable) != null;
        };

        return new ConditionalEventHandler.Impl(canHandlePredicate, (te) -> {
            Focusable focusable = currentChildFor(te);
            if (focusable != null && focusable.canHandle(te) != null) {
                focusable.handle(te);
                return;
            }
            if (IS_KEYPRESS.test(te)) {
                handleHotKey((KeyEvent) te);
                return;
            }
            Focusable other = otherChildClaiming(te, focusable);
            if (other != null) {
                other.handle(te);
            }
        }).setName(DefaultFocusManager.this.getName()+"-ChildrenEventHandler");
    }

    /** the first child other than current that claims (non-key) event te; null if none */
    private Focusable otherChildClaiming(TerminalEvent<?> te, Focusable current) {
        for (Focusable other : new ArrayList<Focusable>(getChildFocusables())) {
            if (other != current && other.canHandle(te) != null) {
                return other;
            }
        }
        return null;
    }

    /**
     * Which child (other than the current one) has a hot key matching this key, and can handle it?
     * Only answers - changes nothing (used by canHandle and KeyRouter). null if none.
     */
    Focusable hotKeyChildFor(KeyEvent te) {
        KeyStroke pressedKey = te.getKeyStroke();
        Focusable focusable = peekCurrent();
        for (Focusable focusableOther : children()) {
            if (focusableOther == focusable) {
                continue;
            }
            if (hotKeyOfWrapperAbove(focusableOther, pressedKey) && focusableOther.canHandle(te) != null) {
                return focusableOther;
            }
            Collection<KeyStroke> hotKeys = HasHotKey.UTIL.getAllHotKeys(focusableOther);
            if (hotKeys.contains(pressedKey)) {
                Focusable foundFocusable = HasHotKey.UTIL.getFocusableFromHotKey(focusableOther, pressedKey);
                if (foundFocusable == null) {
                    continue;   // not usable here - another child may still have it
                }
                if (foundFocusable.canHandle(te) != null) {
                    return focusableOther;
                }
            }
        }
        return null;
    }

    /**
     * Is key the hot key of a component wrapped around child - one directly above it in the component
     * tree, up to the next focusable one? Example: a wrapper (AbstractComponentWrappper) with hot key '3'
     * around a focusable button bar - the bar is the child here, the hot key sits on the wrapper above it.
     */
    private static boolean hotKeyOfWrapperAbove(Focusable child, KeyStroke key) {
        if (!(child instanceof Component)) {
            return false;
        }
        Object above = ((Component<?>) child).getParentContainer();
        while (above instanceof Component && !ToFocusable.UTIL.isFocusable(above)) {
            if (above instanceof HasHotKey && key.equals(((HasHotKey) above).getHotKey())) {
                return true;
            }
            above = ((Component<?>) above).getParentContainer();
        }
        return false;
    }

    /**
     * If a child's hot key matches: move focus to that child and give it the key. Returns true if so.
     * Focus moves like a click or TAB (FocusManager.UTIL.moveFocus), so every focus manager on the
     * way is told and the previously focused component lets go.
     * (Used by handle and KeyRouter; asking without acting: hotKeyChildFor.)
     */
    boolean handleHotKey(KeyEvent te) {
        Focusable hotKeyChild = hotKeyChildFor(te);
        if (hotKeyChild == null) {
            return false;
        }
        FocusManager top = this;
        while (FocusManager.UTIL.parentOf(top) != null) {
            top = FocusManager.UTIL.parentOf(top);
        }
        if (!FocusManager.UTIL.moveFocus(top, hotKeyChild, te.getEventBus(), false)) {
            setChildAsCurrentFocus(hotKeyChild, te.getEventBus());     // not reachable from the top: as before
        }
        Focusable focusable = currentChildFor(te);
        if (focusable != null && focusable.canHandle(te) != null) {
            focusable.handle(te);
        }
        return true;
    }

    /** make the previous child current (see neighbourChild); false if there is none */
    public boolean setPreviousAsCurrentFocus(EventBus eventBus) {
        Focusable prev = neighbourChild(FocusDirection.BACKWARD);
        if (prev != null) {
            setChildAsCurrentFocus(prev, eventBus);
            return true;
        }
        return false;
    }

    /** make the next child current (see neighbourChild); false if there is none */
    public boolean setNextAsCurrentFocus(EventBus eventBus) {
        Focusable next = neighbourChild(FocusDirection.FORWARD);
        if (next != null) {
            setChildAsCurrentFocus(next, eventBus);
            return true;
        }
        return false;
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

    /** the wrapper component this manager works for (see setActual); null for a manager built by hand */
    @Override
    public Component<?> getFocusComponent() {
        FocusManager actual = meOrActual();
        return actual != this && actual instanceof Component ? (Component<?>) actual : null;
    }

    /** the wrapper component this manager works for (see setActual), else this manager itself */
    FocusManager meOrActual() {
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
        Focusable theCurrent = peekCurrent();
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
    }

    @Override
    public Collection<? extends Focusable> getChildren() {
        return children();
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
        return children();
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
