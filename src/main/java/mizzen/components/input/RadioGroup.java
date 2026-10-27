package mizzen.components.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import mizzen.components.AbstractComponentWrappper;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.DisplayableValue.BooleanDisplayValue;
import mizzen.components.input.DisplayableValue.StringDisplayValue;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.Registration;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.AsKeyboardHandler;
import mizzen.events.keyboard.HasHotKey;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.events.keyboard.Keys;
import mizzen.events.mouse.Clickable;

/**
 * Pick one of several options - radio buttons.
 * <pre>
 *     vertical("Small", "Medium", "Large"):      horizontal(...).setLabel("Size"):
 *       (•) Small                                  Size  (•) Small  ( ) Medium  ( ) Large
 *       ( ) Medium
 *       ( ) Large
 * </pre>
 * One TAB stop: TAB lands on the chosen option; the arrow keys (UP / DOWN, or LEFT / RIGHT when
 * horizontal) move between the options and choose as they go, wrapping around; a click chooses.
 * A hot key (setHotKey) chooses the next option from anywhere - with two options, a toggle; it is
 * shown in front of the label ("F2: Wifi").
 * <p/>
 * Options are given like ListBox / DropdownListBox items: each a DisplayableValue (its value and
 * the text shown), or plain strings. The group's value is the chosen option's value - settable
 * (SettableHasValue), so forms can bind it. It starts on its first option. onOff(...) builds the
 * common two-option case: "label  (•) On  ( ) Off", with the value true / false.
 * <p/>
 * Looks: setMarkers (default "(•)" / "( )"; asciiStyle() for "(*)" / "( )"), setLabel,
 * textBeforeMarker ("Small (•)").
 */
public class RadioGroup<V> extends AbstractComponentWrappper<Composite>
                           implements SettableHasValueComponentMixen<AbstractComponentWrappper<Composite>, V>, FormInput<V>,
                                      ToFocusable, AsKeyboardHandler, HasHotKey {

    private final List<Option> options = new ArrayList<Option>();
    private final ParentFocusBehaviour focus;
    private TextComponent label;                        // null: no label
    private String labelText;
    private V value;
    private String chosenMarker;
    private String notChosenMarker;
    private boolean textBeforeMarker;
    private KeyStroke hotKey;
    private final OnChangeListeners<V> changeListeners = new OnChangeListeners<V>(this);   // onChange: any number of listeners
    /** options one under the other; UP / DOWN move between them */
    @SafeVarargs
    public static <V> RadioGroup<V> vertical(DisplayableValue<V>... options) {
        return new RadioGroup<V>(new VerticalPanel(), Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE, Arrays.asList(options));
    }

    /** options side by side; LEFT / RIGHT move between them */
    @SafeVarargs
    public static <V> RadioGroup<V> horizontal(DisplayableValue<V>... options) {
        return new RadioGroup<V>(new HorizontalPanel(Colors.DEFAULT_COLOR, 2), Keys.LEFT_KEYSTROKE, Keys.RIGHT_KEYSTROKE, Arrays.asList(options));
    }

    /** string options one under the other (the text shown is the value) */
    public static RadioGroup<String> vertical(String... options) {
        return vertical(StringDisplayValue.fromStringValues(options));
    }

    /** string options side by side (the text shown is the value) */
    public static RadioGroup<String> horizontal(String... options) {
        return horizontal(StringDisplayValue.fromStringValues(options));
    }

    /** "label  (•) onText  ( ) offText" side by side; the value is true for onText, false for offText */
    public static RadioGroup<Boolean> onOff(String label, String onText, String offText, boolean value) {
        RadioGroup<Boolean> group = RadioGroup.horizontal(new BooleanDisplayValue(onText, Boolean.TRUE),
                                                          new BooleanDisplayValue(offText, Boolean.FALSE))
                                              .setLabel(label);
        group.setValue(value);
        return group;
    }

    /** "label  (•) On  ( ) Off" */
    public static RadioGroup<Boolean> onOff(String label, boolean value) {
        return onOff(label, "On", "Off", value);
    }

    private RadioGroup(Composite layout, KeyStroke prev, KeyStroke next, List<? extends DisplayableValue<V>> items) {
        super(Colors.DEFAULT_COLOR, layout);
        setMarkers("(•)", "( )");
        setItems(items);
        this.focus = ParentFocusBehaviour.of(this)
                                         .singleTabStop()
                                         .arrowsMoveInside(ArrowKeys.of(prev, next))
                                         .wrapAround()
                                         .enterAt(() -> optionFor(this.value))
                                         .whenChildFocused((child, bus) -> choose(optionAt(child), bus))
                                         .handleKeys(new HotKeyChoosesNext())
                                         .handleEvents(new ConditionalEventHandler.Impl(
                                                 (te) -> changeListeners.canHandle(te) != null,
                                                 (te) -> changeListeners.handle(te)));
        refresh();
    }

    // ---- value ------------------------------------------------------------------------------

    @Override
    public V getValue() {
        return value;
    }

    /** choose the option with this value */
    @Override
    public void setValue(V value) {
        this.value = value;
        refresh();
    }

    /** the options (like ListBox.setItems); the value stays if it is still one of them, else the first */
    public RadioGroup<V> setItems(List<? extends DisplayableValue<V>> items) {
        options.forEach((option) -> getWrappedComponent().remove(option));
        options.clear();
        for (DisplayableValue<V> item : items) {
            Option option = new Option(item);
            options.add(option);
            getWrappedComponent().add(option);
        }
        if (optionFor(value) == null) {
            value = options.isEmpty() ? null : options.get(0).value;
        }
        refresh();
        return this;
    }

    public Registration onChange(EventHandler handler) {
        return changeListeners.add(handler);
    }

    // ---- looks ------------------------------------------------------------------------------

    /** the markers for the chosen option and the others, e.g. "(•)" and "( )" */
    public RadioGroup<V> setMarkers(String chosen, String notChosen) {
        this.chosenMarker = chosen;
        this.notChosenMarker = notChosen;
        refresh();
        return this;
    }

    /** plain ASCII markers, for terminals without the bullet: "(*)" and "( )" */
    public RadioGroup<V> asciiStyle() {
        return setMarkers("(*)", "( )");
    }

    /** text before the options, e.g. "Size" - with the hot key in front if there is one ("F2: Size") */
    public RadioGroup<V> setLabel(String text) {
        this.labelText = text;
        if (label == null) {
            label = new TextComponent(getColor(), "");
            getWrappedComponent().insert(0, label);
        }
        showLabel();
        return this;
    }

    private void showLabel() {
        if (label != null) {
            label.setTextAndResize(hotKey != null ? hotKey.getName() + ": " + labelText : labelText);
        }
    }

    /** true: "Small (•)" instead of "(•) Small" */
    public RadioGroup<V> textBeforeMarker(boolean before) {
        this.textBeforeMarker = before;
        refresh();
        return this;
    }

    @Override
    public RadioGroup<V> setColor(Color color) {
        super.setColor(color);
        if (label != null) {
            label.setColor(color);
        }
        if (options != null) {      // null while the super constructor sets the first color
            options.forEach((option) -> option.setColor(color));
        }
        return this;
    }

    // ---- hot key ----------------------------------------------------------------------------

    /** chooses the next option from anywhere (with two options: a toggle) */
    public RadioGroup<V> setHotKey(KeyStroke hotKey) {
        this.hotKey = hotKey;
        showLabel();
        return this;
    }

    @Override
    public KeyStroke getHotKey() {
        return hotKey;
    }

    /** the hot key: choose the option after the chosen one, wrapping around */
    private class HotKeyChoosesNext implements KeyboardHandler {
        @Override
        public boolean onKey(KeyEvent ke) {
            if (!wantsKey(ke)) {
                return false;
            }
            int chosen = options.indexOf(optionFor(value));
            choose(options.get((chosen + 1) % options.size()), ke.getEventBus());
            return true;
        }

        @Override
        public boolean wantsKey(KeyEvent ke) {
            return hotKey != null && !options.isEmpty() && ke.isKeyStroke(hotKey);
        }
    }

    // ---- focus ------------------------------------------------------------------------------

    @Override
    public Focusable asFocusable() {
        return focus.asFocusable();
    }

    @Override
    public KeyboardHandler asKeyboardHandler() {
        return focus.asKeyboardHandler();
    }

    // ---- inside -----------------------------------------------------------------------------

    private void choose(Option option, EventBus eventBus) {
        if (option == null || Objects.equals(option.value, value)) {
            return;
        }
        value = option.value;
        refresh();
        eventBus.raiseEvent(new HasValue.ValueChangedEvent<V>(this));
        RequestUpdateCanvasEvent.invoke(eventBus, this);
    }

    private Option optionFor(V v) {
        for (Option option : options) {
            if (Objects.equals(option.value, v)) {
                return option;
            }
        }
        return null;
    }

    private Option optionAt(Component<?> component) {
        for (Option option : options) {
            if (option == component) {
                return option;
            }
        }
        return null;
    }

    /** redraw every option's text (marker + display text) */
    private void refresh() {
        options.forEach(Option::refresh);
        markAsDirty();
    }

    /** one option, drawn "(•) Medium"; a leaf TAB stop inside the group; a click chooses it */
    private class Option extends TextComponent implements ToFocusable, Clickable {
        final V value;
        final String text;
        private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this);

        Option(DisplayableValue<V> item) {
            super(RadioGroup.this.getColor(), "");
            this.value = item.getValue();
            this.text = item.getDisplayValue();
        }

        void refresh() {
            if (chosenMarker == null) {
                return;     // still being built
            }
            String marker = Objects.equals(value, RadioGroup.this.value) ? chosenMarker : notChosenMarker;
            setTextAndResize(textBeforeMarker ? text + " " + marker : marker + " " + text);
        }

        @Override
        public Focusable asFocusable() {
            return focus.asFocusable();
        }

        @Override
        public void onClick(EventBus eventBus) {
            focus.requestFocus(eventBus);
            choose(this, eventBus);
        }
    }
}
