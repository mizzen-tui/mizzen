package dtb.terminalIO.components.input;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusDirection;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.FocusPolicy;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.HasHotKey;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.KeyboardHandler;

/**
 * Focus for a component with components inside it - a radio group's options, a dropdown's button
 * and list, a panel of buttons. (A component that is a TAB stop itself holds a LeafFocusBehaviour.)
 * <p/>
 * By default its children are ordinary TAB stops: there is nothing to set. Otherwise say how it
 * should behave:
 * <pre>
 *   singleTabStop()                     TAB treats the whole component as one stop ...
 *   arrowsMoveInside(ArrowKeys.UP_DOWN) ... and these keys move between its children
 *   wrapAround()                        past the last child goes back to the first
 *   enterAt(Entry.FIRST_OR_LAST)        where focus lands when it arrives (or enterAt(() -> child))
 *   skippedByTab()                      TAB passes it by (the mouse and hot keys still reach it)
 *   keepsFocusInside()                  TAB never leaves it - goes round its children (a dialog)
 *   takeFocusItselfWhenNoChildCan()     with nothing focusable inside, it is the stop itself
 *   handleKeys(..) / handleEvents(..)   its own keys (those its children didn't use) and events
 *   whenChildFocused / whenFocusArrives / whenFocusLeaves
 * </pre>
 * Example (RadioGroup):
 * <pre>
 *     focus = ParentFocusBehaviour.of(this)
 *                                 .singleTabStop()
 *                                 .arrowsMoveInside(ArrowKeys.UP_DOWN)
 *                                 .wrapAround()
 *                                 .enterAt(() -> selectedOption());
 * </pre>
 * The usual way to get one is to extend ComponentWithFocus (its focus() is this); a component that
 * holds one itself hands it out with asFocusable() / asKeyboardHandler().
 * <p/>
 * Underneath it is a DefaultFocusManager working for the component; if the component has a hot key
 * (HasHotKey), pressing it anywhere moves focus into the component and gives it the key (onKey).
 */
public final class ParentFocusBehaviour {

    /** no keys of its own */
    private static final KeyboardHandler NO_KEYS = new KeyboardHandler() {
        @Override
        public boolean onKey(KeyEvent ke) {
            return false;
        }

        @Override
        public boolean wantsKey(KeyEvent ke) {
            return false;
        }
    };

    private final ParentContainer component;
    private final DefaultFocusManager focusManager;
    private KeyboardHandler keys = NO_KEYS;
    private ConditionalEventHandler otherEvents = ConditionalEventHandler.NOOP;
    private Function<FocusDirection, Component<?>> childOnEntry = (dir) -> null;
    private BiConsumer<Component<?>, EventBus> onChildFocused = (child, bus) -> { };
    private Consumer<EventBus> onFocusGained = (bus) -> { };
    private Consumer<EventBus> onFocusLost = (bus) -> { };
    private boolean takesFocusItselfWhenNoChildCan = false;

    private ParentFocusBehaviour(ParentContainer component) {
        this.component = component;
        this.focusManager = new ManagerForComponent();
        DefaultFocusManager.setDefaultNameAndActualFromParentObject(focusManager, component);
        focusManager.setCustomExtendedEventHandler(keysAndOtherEvents());
    }

    /** focus behaviour for component (usually: this) */
    public static ParentFocusBehaviour of(ParentContainer component) {
        return new ParentFocusBehaviour(component);
    }

    /** where focus lands when it arrives in the component (TAB / SHIFT-TAB) */
    public enum Entry {
        /** TAB from before lands on the first child, SHIFT-TAB from after on the last */
        FIRST_OR_LAST,
        /** back on the child that had focus last (the first time: first / last as above) */
        LAST_FOCUSED
    }

    /** TAB treats the whole component as one stop (arrowsMoveInside: the keys that move inside it) */
    public ParentFocusBehaviour singleTabStop() {
        focusManager.setFocusPolicy(FocusPolicy.ONE_STOP);
        return this;
    }

    /** the keys that move focus between its children, e.g. ArrowKeys.UP_DOWN (with singleTabStop) */
    public ParentFocusBehaviour arrowsMoveInside(ArrowKeys keys) {
        focusManager.setPrevKeyStroke(keys.previous())
                    .setNextKeyStroke(keys.next());
        return this;
    }

    /** past the last child goes back to the first, and the other way round */
    public ParentFocusBehaviour wrapAround() {
        return wrapAround(true);
    }

    public ParentFocusBehaviour wrapAround(boolean wrap) {
        focusManager.setCycling(wrap);
        return this;
    }

    /** where focus lands when it arrives (default: FIRST_OR_LAST; with singleTabStop: LAST_FOCUSED) */
    public ParentFocusBehaviour enterAt(Entry entry) {
        this.childOnEntry = entry == Entry.FIRST_OR_LAST ? this::firstOrLastChild : (dir) -> currentChild();
        return this;
    }

    /** focus always lands on this child when it arrives, e.g. a radio group's selected option */
    public ParentFocusBehaviour enterAt(Supplier<Component<?>> child) {
        this.childOnEntry = (dir) -> child.get();
        return this;
    }

    private Component<?> firstOrLastChild(FocusDirection dir) {
        List<? extends Focusable> children = new ArrayList<Focusable>(focusManager.getChildFocusables());
        if (children.isEmpty()) {
            return null;
        }
        Focusable child = dir == FocusDirection.FORWARD ? children.get(0) : children.get(children.size() - 1);
        return child.getFocusComponent();
    }

    /** TAB / SHIFT-TAB never take focus out of it - they go round its children: a modal dialog */
    public ParentFocusBehaviour keepsFocusInside() {
        focusManager.setFocusPolicy(FocusPolicy.TRAP);
        return this;
    }

    /** TAB / SHIFT-TAB pass the component by; the mouse and hot keys still reach what is inside */
    public ParentFocusBehaviour skippedByTab() {
        return skippedByTab(true);
    }

    public ParentFocusBehaviour skippedByTab(boolean skipped) {
        focusManager.setTabStop(!skipped);
        return this;
    }

    /**
     * When nothing inside the component can take focus, the component takes it itself - it is then a
     * TAB stop like a button: its hilite (Hiliter) shows the focus, its keys (handleKeys) work, other
     * keys (TAB...) go on to the container around it. When something inside can take focus, focus goes
     * there as usual. Example: a scroll pane - showing text it is the stop and its arrow keys scroll;
     * showing a form, focus goes to the form's fields.
     */
    public ParentFocusBehaviour takeFocusItselfWhenNoChildCan() {
        this.takesFocusItselfWhenNoChildCan = true;
        return this;
    }

    /**
     * Its own keys: those the focused child didn't use (this::key, or an adapted event handler - see
     * AsKeyboardHandler.UTIL.toKeyboardHandler). Also gets the component's hot key, if it has one.
     */
    public ParentFocusBehaviour handleKeys(KeyboardHandler keys) {
        this.keys = keys;
        return this;
    }

    /** its own events, other than keys - e.g. its "value changed" event, for onChange */
    public ParentFocusBehaviour handleEvents(ConditionalEventHandler events) {
        this.otherEvents = events;
        return this;
    }

    /** focus moved to another of its children - by arrows, TAB or a click */
    public ParentFocusBehaviour whenChildFocused(BiConsumer<Component<?>, EventBus> onChildFocused) {
        this.onChildFocused = onChildFocused;
        return this;
    }

    /**
     * Focus arrived in the component (it had none inside it before) - e.g. a dropdown opens its list.
     * Moving between its own children (and a popup it owns) doesn't count.
     */
    public ParentFocusBehaviour whenFocusArrives(Consumer<EventBus> onFocusGained) {
        this.onFocusGained = onFocusGained;
        return this;
    }

    /** focus left the component and everything in it (and a popup it owns) - e.g. a dropdown closes its list */
    public ParentFocusBehaviour whenFocusLeaves(Consumer<EventBus> onFocusLost) {
        this.onFocusLost = onFocusLost;
        return this;
    }

    /** the child that has focus inside the component, or had it last; null if none yet */
    public Component<?> currentChild() {
        Focusable child = focusManager.getCurrentChildFocus();
        return child == null ? null : child.getFocusComponent();
    }

    /** focus to the next child, as its "next" arrow does; false if there is none (wraps only with wrapAround) */
    public boolean focusNext(EventBus eventBus) {
        return focusManager.setNextAsCurrentFocus(eventBus);
    }

    /** focus to the previous child, as its "previous" arrow does; false if there is none */
    public boolean focusPrevious(EventBus eventBus) {
        return focusManager.setPreviousAsCurrentFocus(eventBus);
    }

    public boolean hasFocus() {
        return focusManager.hasFocus();
    }

    /** for the component's ToFocusable.asFocusable() */
    public Focusable asFocusable() {
        return focusManager;
    }

    /** for the component's AsKeyboardHandler.asKeyboardHandler() */
    public KeyboardHandler asKeyboardHandler() {
        return new KeyboardHandler() {
            @Override
            public boolean onKey(KeyEvent ke) {
                return keys.onKey(ke);
            }

            @Override
            public boolean wantsKey(KeyEvent ke) {
                return keys.wantsKey(ke);
            }
        };
    }

    /**
     * Its keys and other events, as the focus manager's custom handler - the way a hot key (and an
     * event that isn't a key) reaches the component. Looked up when asked, so they may be set later.
     */
    private ConditionalEventHandler keysAndOtherEvents() {
        ChainedEventHandler chain = ChainedEventHandler.useChainOfResponsibilityStrategy(this);
        chain.appendEventHandler(new ConditionalEventHandler.Impl(
                (te) -> te instanceof KeyEvent && keys.wantsKey((KeyEvent) te),
                (te) -> keys.onKey((KeyEvent) te)));
        chain.appendEventHandler(new ConditionalEventHandler.Impl(
                (te) -> otherEvents.canHandle(te) != null,
                (te) -> otherEvents.handle(te)));
        return chain.asConditionalEventHandler();
    }

    /** the focus manager: the entry child, "a child got focus", and the component's hot key */
    private class ManagerForComponent extends DefaultFocusManager implements HasHotKey {

        /**
         * The component it works for: its children are worked out from it. (The usual way -
         * setActual - only takes a component that is itself a focus manager; this one isn't, it
         * holds the behaviour.)
         */
        @Override
        public Component<?> getFocusComponent() {
            return component instanceof Component ? (Component<?>) component : null;
        }

        /** its component's name - so focus logs and errors name the component (it may be named after this is built) */
        @Override
        public String getName() {
            return HasName.UTIL.getName(component);
        }

        // takeFocusItselfWhenNoChildCan: the component itself has focus (no child of it does)
        private boolean focusedItself = false;

        // ---- the component as its own stop (takeFocusItselfWhenNoChildCan) ----

        private boolean takesFocusItself() {
            return takesFocusItselfWhenNoChildCan && FocusManager.UTIL.enter(this, FocusDirection.FORWARD) == null;
        }

        /** focus on the component itself, or off it: its hilite shows it */
        private void setFocusedItself(boolean focus, EventBus eventBus) {
            if (focusedItself == focus) {
                return;
            }
            focusedItself = focus;
            showFocusOnItself(focus, eventBus);
        }

        private void showFocusOnItself(boolean focus, EventBus eventBus) {
            if (component instanceof Hiliter) {
                if (focus) {
                    ((Hiliter) component).hilite();
                } else {
                    ((Hiliter) component).unHilite();
                }
            }
            if (component instanceof Component) {
                ((Component<?>) component).markAsDirty();
                eventBus.raiseEvent(new RequestUpdateCanvasEvent(component));
            }
        }

        /** a component that takes focus itself can be focused with no focusable children (hidden ones are not in the focus tree anyway) */
        @Override
        public boolean isEnabled() {
            return takesFocusItselfWhenNoChildCan || super.isEnabled();
        }

        @Override
        public Focusable enter(FocusDirection dir) {
            if (takesFocusItself()) {
                return isTabStop() ? this : null;
            }
            Focusable chosen = enterableChild(childOnEntry.apply(dir), dir);
            return chosen != null ? chosen : super.enter(dir);
        }

        @Override
        public boolean hasFocus() {
            return focusedItself || super.hasFocus();
        }

        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
            boolean had = hasFocus();
            if (focusedItself || (focus && takesFocusItself())) {
                setFocusedItself(focus, eventBus);
            } else {
                super.setFocus(focus, eventBus);
            }
            afterChange(had, eventBus);
        }

        @Override
        public void focus(Focusable oldFocusable, EventBus eventBus) {
            boolean had = hasFocus();
            if (takesFocusItself()) {
                setFocusedItself(true, eventBus);
            } else {
                super.focus(oldFocusable, eventBus);
            }
            afterChange(had, eventBus);
        }

        @Override
        public void unFocus(Focusable newFocusable, EventBus eventBus) {
            boolean had = hasFocus();
            if (focusedItself) {
                setFocusedItself(false, eventBus);
            } else {
                super.unFocus(newFocusable, eventBus);
            }
            afterChange(had, eventBus);
        }

        /**
         * With focus on the component itself, a key is claimed only if its own keys (onKey) use it;
         * the rest - TAB, SHIFT-TAB - go on to the container around it (as a focus manager it would
         * take them for moving among children it doesn't have).
         */
        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if (focusedItself && te instanceof KeyEvent) {
                return keys.wantsKey((KeyEvent) te) ? this : null;
            }
            return super.canHandle(te);
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            if (focusedItself && te instanceof KeyEvent) {
                keys.onKey((KeyEvent) te);
                return;
            }
            super.handle(te);
        }

        /** tell onFocusGained / onFocusLost when the component's focus really changed */
        private void afterChange(boolean had, EventBus eventBus) {
            boolean has = hasFocus();
            if (!had && has) {
                onFocusGained.accept(eventBus);
            } else if (had && !has) {
                onFocusLost.accept(eventBus);
            }
        }

        @Override
        public void setChildAsCurrentFocus(Focusable child, EventBus eventBus) {
            Focusable before = getCurrentChildFocus();
            if (focusedItself && child != null) {
                // focus moves from the component itself to a child (e.g. a hot key inside it): from now
                // on the manager holds it the usual way - record the child, then switch the manager's
                // focus on, which passes it to the child
                focusedItself = false;
                super.setChildAsCurrentFocus(child, eventBus);
                super.setFocus(true, eventBus);
                showFocusOnItself(false, eventBus);
            } else {
                super.setChildAsCurrentFocus(child, eventBus);
            }
            Focusable now = getCurrentChildFocus();
            if (now != null && now != before && now.getFocusComponent() != null) {
                onChildFocused.accept(now.getFocusComponent(), eventBus);
            }
        }

        @Override
        public KeyStroke getHotKey() {
            return component instanceof HasHotKey ? ((HasHotKey) component).getHotKey() : null;
        }

        /** where focus lands for child: its focusable, or inside it if it is a focus manager; null if it can't take focus */
        private Focusable enterableChild(Component<?> child, FocusDirection dir) {
            if (child == null || !ToFocusable.UTIL.isFocusable(child)) {
                return null;
            }
            Focusable focusable = ToFocusable.UTIL.toFocusable(child);
            if (!focusable.isEnabled() || !getChildFocusables().contains(focusable)) {
                return null;
            }
            return focusable instanceof FocusManager ? ((FocusManager) focusable).enter(dir) : focusable;
        }
    }
}
