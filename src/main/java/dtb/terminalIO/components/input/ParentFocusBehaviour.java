package dtb.terminalIO.components.input;

import java.util.function.BiConsumer;
import java.util.function.Function;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
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
 * Focus for a parent component (a ParentContainer): one whose focus goes to the components inside
 * it - a radio group's options, a dropdown's button and list. (A component that is a TAB stop
 * itself is a leaf instead; see LeafFocusBehaviour.)
 * <p/>
 * By default its children are ordinary TAB stops. oneStop(...) makes the whole component a single
 * TAB stop instead, moving between its children with the given keys (a radio group: UP / DOWN).
 * It can also handle the keys its focused child didn't use, choose which child focus enters, and
 * hear when focus moves to another of its children.
 * <p/>
 * A component that needs none of that (e.g. a panel of fields) needs no ParentFocusBehaviour: its
 * fields are TAB stops anyway.
 * <p/>
 * Example (RadioGroup):
 * <pre>
 *     focus = ParentFocusBehaviour.of(this)
 *                                 .oneStop(Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE, true)
 *                                 .entry(dir -> optionFor(value))
 *                                 .onChildFocused((child, bus) -> choose(child, bus));
 *     public Focusable       asFocusable()       { return focus.asFocusable(); }
 *     public KeyboardHandler asKeyboardHandler() { return focus.asKeyboardHandler(); }
 * </pre>
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

    /** one TAB stop; prev / next move between its children; wrap: past the last goes to the first */
    public ParentFocusBehaviour oneStop(KeyStroke prev, KeyStroke next, boolean wrap) {
        focusManager.setPrevKeyStroke(prev)
                    .setNextKeyStroke(next)
                    .setCycling(wrap)
                    .setFocusPolicy(FocusPolicy.WIDGET);
        return this;
    }

    /**
     * Keys the focused child didn't use (this::key, or an adapted event handler - see
     * LeafFocusBehaviour.onKey). Also gets the component's hot key, if it has one.
     */
    public ParentFocusBehaviour onKey(KeyboardHandler keys) {
        this.keys = keys;
        return this;
    }

    /** events other than keys the component listens to - e.g. its own "value changed" event, for onChange */
    public ParentFocusBehaviour otherEvents(ConditionalEventHandler otherEvents) {
        this.otherEvents = otherEvents;
        return this;
    }

    /** which child focus goes to when it arrives (TAB / SHIFT-TAB); null: the default - the remembered child, else first / last */
    public ParentFocusBehaviour entry(Function<FocusDirection, Component<?>> childOnEntry) {
        this.childOnEntry = childOnEntry;
        return this;
    }

    /** focus moved to another of its children - by arrows, TAB or a click */
    public ParentFocusBehaviour onChildFocused(BiConsumer<Component<?>, EventBus> onChildFocused) {
        this.onChildFocused = onChildFocused;
        return this;
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

        @Override
        public Focusable enter(FocusDirection dir) {
            Focusable chosen = enterableChild(childOnEntry.apply(dir), dir);
            return chosen != null ? chosen : super.enter(dir);
        }

        @Override
        public void setChildAsCurrentFocus(Focusable child, EventBus eventBus) {
            Focusable before = getCurrentChildFocus();
            super.setChildAsCurrentFocus(child, eventBus);
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
