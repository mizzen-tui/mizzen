package mizzen.components.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import mizzen.components.BoundedComponent;
import mizzen.components.input.DisplayableValue.SimpleDisplayValue;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.Registration;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.AsKeyboardHandler;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.events.keyboard.Keys;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.events.mouse.Clickable;
import mizzen.events.mouse.MOUSE_ACTION;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;

/**
 * Pick one of a few options shown as a row of chips - small buttons side by side, the chosen one standing out (a
 * segmented control). Like a horizontal RadioGroup, in the button colour: for a short choice that sits on a panel
 * rather than in a form.
 * <pre>
 *     ChipGroup&lt;String&gt; days = ChipGroup.of("7", "30", "90").value("30");
 *     days.onChange((te) -> report(days.getValue()));
 *
 *     [7][30][90]
 * </pre>
 * One TAB stop: LEFT / RIGHT choose the one before / after, wrapping round; SPACE the next; HOME / END the first / last.
 * A click chooses the chip under it. Given no colour it takes the theme's button colour. The chosen chip's text is in
 * the theme's accent colour; while the group has focus, its brackets too, bold - a light mark, no filled bar.
 * <p/>
 * Options are given as RadioGroup's are: each a DisplayableValue (its value and the text in the chip), or plain strings.
 * The text can be a word or a few characters - a little picture. setValue doesn't tell the listeners: they hear the user.
 */
public class ChipGroup<V> extends BoundedComponent<ChipGroup<V>> implements FormInput<V>, ToFocusable, Clickable, AsKeyboardHandler {

    {
        defaultThemeColor(Theme::buttonColor);               // given no colour: the theme's - buttons
    }

    private final List<DisplayableValue<V>> options;
    private V value;
    private final OnChangeListeners<V> changeListeners = new OnChangeListeners<V>(this);   // onChange: any number

    private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this)
            .look(LeafFocusBehaviour.Look.NONE)                  // focus shows on the chosen chip, drawn here
            .whenFocusArrives(this::repaint)
            .whenFocusLeaves(this::repaint)
            .handleKeys(this::key)
            .handleEvents(new ConditionalEventHandler.Impl(
                    (te) -> changeListeners.canHandle(te) != null,
                    (te) -> changeListeners.handle(te)));

    /** chips for these options, the first chosen */
    @SafeVarargs
    public static <V> ChipGroup<V> of(DisplayableValue<V>... options) {
        return new ChipGroup<V>(Arrays.asList(options));
    }

    /** chips for these strings (the text shown is the value), the first chosen */
    public static ChipGroup<String> of(String... options) {
        List<DisplayableValue<String>> values = new ArrayList<DisplayableValue<String>>();
        for (String option : options) {
            values.add(new SimpleDisplayValue<String>(option, option));
        }
        return new ChipGroup<String>(values);
    }

    private ChipGroup(List<? extends DisplayableValue<V>> options) {
        super((Color) null, new Dimension.Simple(1, 1));
        if (options.isEmpty()) {
            throw new IllegalArgumentException("ChipGroup: no options");
        }
        this.options = new ArrayList<DisplayableValue<V>>(options);
        this.value = this.options.get(0).getValue();
        setName("chipGroup");
        resize(new Dimension.Simple(1, widthOfChips()));
    }

    private int widthOfChips() {
        int width = 0;
        for (DisplayableValue<V> option : options) {
            width += chip(option).length();
        }
        return width;
    }

    private static String chip(DisplayableValue<?> option) {
        return "[" + option.getDisplayValue() + "]";
    }

    @Override
    public V getValue() {
        return value;
    }

    /** chooses the option with this value; null: the first. A value not among the options fails at once. */
    @Override
    public void setValue(V value) {
        V v = value == null ? options.get(0).getValue() : value;
        if (indexOf(v) < 0) {
            throw new IllegalArgumentException("ChipGroup: " + value + " isn't one of its options");
        }
        if (!Objects.equals(v, this.value)) {
            this.value = v;
            markAsDirty();
        }
    }

    /** as setValue, for chaining */
    public ChipGroup<V> value(V value) {
        setValue(value);
        return this;
    }

    @Override
    public Registration onChange(EventHandler handler) {
        return changeListeners.add(handler);
    }

    private int indexOf(V v) {
        for (int i = 0; i < options.size(); i++) {
            if (Objects.equals(options.get(i).getValue(), v)) {
                return i;
            }
        }
        return -1;
    }

    // ---- the user chooses ---------------------------------------------------------------------------------------

    private boolean key(KeyEvent ke) {
        int at = indexOf(value);
        int n = options.size();
        if (ke.isKeyStroke(Keys.LEFT_KEYSTROKE)) {
            return userChooses((at + n - 1) % n, ke.getEventBus());
        } else if (ke.isKeyStroke(Keys.RIGHT_KEYSTROKE) || ke.isKeyStroke(Keys.SPACE_KEYSTROKE)) {
            return userChooses((at + 1) % n, ke.getEventBus());
        } else if (ke.isKeyStroke(Keys.HOME_KEYSTROKE)) {
            return userChooses(0, ke.getEventBus());
        } else if (ke.isKeyStroke(Keys.END_KEYSTROKE)) {
            return userChooses(n - 1, ke.getEventBus());
        }
        return false;
    }

    /** a press on a chip chooses it; position: relative to the group */
    @Override
    public void onClick(ApplicationMouseEvent appMouseEvent) {
        if (!MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction())) {
            return;
        }
        EventBus bus = appMouseEvent.getEventBus();
        focus.requestFocus(bus);
        int col = appMouseEvent.getMousePosition().getCol();
        int start = 0;
        for (int i = 0; i < options.size(); i++) {
            int end = start + chip(options.get(i)).length();
            if (col >= start && col < end) {
                userChooses(i, bus);
                return;
            }
            start = end;
        }
    }

    @Override
    public void onClick(EventBus eventBus) {
        focus.requestFocus(eventBus);
    }

    // true: the key is used
    private boolean userChooses(int index, EventBus bus) {
        V before = value;
        setValue(options.get(index).getValue());
        if (!Objects.equals(before, value)) {
            bus.raiseEvent(new HasValue.ValueChangedEvent<V>(this));
        }
        repaint(bus);
        return true;
    }

    private void repaint(EventBus bus) {
        markAsDirty();
        RequestUpdateCanvasEvent.invoke(bus, this);
    }

    // ---- drawing ------------------------------------------------------------------------------------------------

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        // light: the chosen chip's text in the accent colour; focused, its brackets too, and bold. No filled bar - it
        // floats on borders and charts, where a bar shouts
        Color accent = Color.onBackgroundOf(getTheme().accentColor().getForeGroundColor(), color);
        boolean focused = focus.hasFocus();
        int col = 0;
        for (DisplayableValue<V> option : options) {
            boolean chosen = Objects.equals(option.getValue(), value);
            String text = option.getDisplayValue();
            Color brackets = chosen && focused ? accent.bold() : color;
            Color inside = !chosen ? color : focused ? accent.bold() : accent;
            col = put(segments, "[", brackets, col);
            col = put(segments, text, inside, col);
            col = put(segments, "]", brackets, col);
        }
    }

    private static int put(IScreenLineSegmentsBuffer segments, String text, Color color, int col) {
        if (!text.isEmpty()) {
            segments.merge(ScreenLineSegment.UTIL.segmentsOf(text, color, new Rectangle.Simple(0, col, 1, text.length())));
        }
        return col + text.length();
    }

    // ---- focus --------------------------------------------------------------------------------------------------

    @Override
    public Focusable asFocusable() {
        return focus.asFocusable();
    }

    @Override
    public KeyboardHandler asKeyboardHandler() {
        return focus.asKeyboardHandler();
    }
}
