package dtb.terminalIO.components.input;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestHideCursorEvent;
import dtb.terminalIO.events.focus.FocusBehaviour;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.Focusable.FocusableFromParts;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyboardHandler;
import dtb.terminalIO.geom.position.Position;

/**
 * Focus for a leaf component: one that is a TAB stop itself and has no focusable components inside
 * it - a checkbox, a text field, a tree that draws its own rows. (A component whose focus goes to
 * components inside it - a radio group's options - is a parent instead; see ParentFocusBehaviour.)
 * <p/>
 * The component holds one and hands it out; that makes it focusable: it is hilited while focused,
 * scrolled into view when it gets focus, and gets keys first while focused (keys it doesn't use go
 * on to its parents - see KeyRouter). Optionally it shows the terminal cursor on itself.
 * <p/>
 * Example (CheckBoxInput):
 * <pre>
 *     private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this)
 *                                                                .showsCursorAt(new Position.Simple(0, 2))
 *                                                                .onKey(this::key);
 *     public Focusable       asFocusable()       { return focus.asFocusable(); }
 *     public KeyboardHandler asKeyboardHandler() { return focus.asKeyboardHandler(); }
 * </pre>
 */
public final class LeafFocusBehaviour {

    private final Component<?> component;
    private Position cursorAt;                                   // null: no cursor
    private KeyboardHandler keys = (ke) -> false;
    private ConditionalEventHandler otherEvents = ConditionalEventHandler.NOOP;
    private Focusable focusable;                                 // built on first use

    private LeafFocusBehaviour(Component<?> component) {
        this.component = component;
    }

    /** focus behaviour for component (usually: this) */
    public static LeafFocusBehaviour of(Component<?> component) {
        return new LeafFocusBehaviour(component);
    }

    /** show the terminal cursor at this position inside the component while it has focus */
    public LeafFocusBehaviour showsCursorAt(Position cursorAt) {
        this.cursorAt = cursorAt;
        return this;
    }

    /**
     * The component's keys, while it has focus. A method (this::key, returning true if it used the
     * key) or an adapted event handler (AsKeyboardHandler.UTIL.toKeyboardHandler) - the latter can
     * tell whether it wants a key without acting, so keys it doesn't want go on down the event bus.
     */
    public LeafFocusBehaviour onKey(KeyboardHandler keys) {
        this.keys = keys;
        return this;
    }

    /**
     * Events other than keys the component listens to while it is in the focus tree - e.g. its own
     * "value changed" event, for its onChange listener.
     */
    public LeafFocusBehaviour otherEvents(ConditionalEventHandler otherEvents) {
        this.otherEvents = otherEvents;
        return this;
    }

    public boolean hasFocus() {
        return focusable != null && focusable.hasFocus();
    }

    /** ask for focus (e.g. on a click), unless the component has it already */
    public void requestFocus(EventBus eventBus) {
        if (!hasFocus()) {
            eventBus.raiseEvent(new RequestFocusEvent(asFocusable()));
        }
    }

    /** for the component's ToFocusable.asFocusable() */
    public Focusable asFocusable() {
        if (focusable == null) {
            focusable = new FocusableFromParts(component, buildFocusBehaviour(), otherEventsHandler());
        }
        return focusable;
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

    /** hilite while focused; on getting focus: scroll into view, show the cursor; on losing it: hide the cursor */
    private FocusBehaviour buildFocusBehaviour() {
        return new DefaultFocusableBehaviour.ComponentHiliterFocusableBehaviour(component) {

            @Override
            public void setFocus(boolean focus, EventBus eventBus) {
                super.setFocus(focus, eventBus);
                if (focus) {
                    new ShowComponentOnFocus().onFocus(component, eventBus);
                    if (cursorAt != null) {
                        Component.UTIL.requestShowCursorAtComponent(component, cursorAt, eventBus);
                    }
                } else if (cursorAt != null) {
                    eventBus.raiseEvent(new RequestHideCursorEvent(component));
                }
            }
        };
    }

    /** the other events, looked up when asked - so otherEvents may be set after asFocusable() */
    private ConditionalEventHandler otherEventsHandler() {
        return new ConditionalEventHandler.Impl((te) -> otherEvents.canHandle(te) != null,
                                                (te) -> otherEvents.handle(te));
    }
}
