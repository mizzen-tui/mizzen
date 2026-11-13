package mizzen.components.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import mizzen.components.BoundedComponent;
import mizzen.components.input.DisplayableValue.SimpleDisplayValue;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.ColorProvider;
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
 *      [7] [30] [90]         a theme that marks a focused button with brackets (Legacy, Dark)
 *     ▕ 7 ▕ 30 ▕ 90 ▕        one that marks it by colour (Rounded): filled chips on a band (Theme.buttonGroupColor)
 * </pre>
 * It looks like the theme's buttons: the look follows Theme.buttonFocus(), or look(..) sets one. Both looks are the
 * same width, so a theme changed while it shows doesn't move anything.
 * <p/>
 * The chosen chip's text is in the theme's accent colour. While the group has focus: with brackets, its brackets too,
 * bold - a light mark; raised, the chosen chip takes the focused-button fill, as a focused button does.
 * <p/>
 * One TAB stop: LEFT / RIGHT choose the one before / after, wrapping round; SPACE the next; HOME / END the first / last.
 * A click chooses the chip under it. Given no colour it takes the theme's button colour.
 * <p/>
 * Options are given as RadioGroup's are: each a DisplayableValue (its value and the text in the chip), or plain strings.
 * The text can be a word or a few characters - a little picture. setValue doesn't tell the listeners: they hear the user.
 */
public class ChipGroup<V> extends BoundedComponent<ChipGroup<V>> implements FormInput<V>, ToFocusable, Clickable, AsKeyboardHandler {

    {
        defaultThemeColor(Theme::buttonColor);               // given no colour: the theme's - buttons
    }

    /** how the chips are drawn: BRACKETS " [7] [30] ", RAISED filled chips on a band (see the class comment) */
    public enum Look { BRACKETS, RAISED }

    private final List<DisplayableValue<V>> options;
    private Look look;                                       // null: the theme's (its buttonFocus)

    // the parts' colours, asked each time it draws: a fixed Color is a ColorProvider too; null: the theme's (below)
    private ColorProvider chosenColor;                       // the chosen chip's text - laid on the chip
    private ColorProvider bandColor;                         // the band under the raised look
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

    // a gap (or band) column at each end and between; each chip its text and two more: [7] or " 7 "
    private int widthOfChips() {
        int width = 1;
        for (DisplayableValue<V> option : options) {
            width += chipWidth(option) + 1;
        }
        return width;
    }

    private static int chipWidth(DisplayableValue<?> option) {
        return option.getDisplayValue().length() + 2;
    }

    /** draws it so, whatever the theme's buttons do; null: as the theme's buttons (the default) */
    public ChipGroup<V> look(Look look) {
        this.look = look;
        markAsDirty();
        return this;
    }

    /**
     * The chosen chip's text colour: a Color, or a provider asked each time it draws. Only its text is used - it is laid
     * on the chip. null: the theme's accent (the default).
     * <pre>
     *     days.setChosenColor(Colors.IGre);
     *     days.setChosenColor(days.themeColor(Theme::successColor));     // follows the theme, whichever it is now
     * </pre>
     */
    public ChipGroup<V> setChosenColor(ColorProvider chosenColor) {
        this.chosenColor = chosenColor;
        markAsDirty();
        return this;
    }

    /** The band under the raised look: a Color or a provider, as setChosenColor. null: the theme's buttonGroupColor. */
    public ChipGroup<V> setBandColor(ColorProvider bandColor) {
        this.bandColor = bandColor;
        markAsDirty();
        return this;
    }

    /** any colour of its theme, asked for each time it is used - for setChosenColor and setBandColor */
    public ColorProvider themeColor(Function<Theme, Color> themeColor) {
        return () -> themeColor.apply(getTheme());
    }

    private boolean raised() {
        return look != null ? look == Look.RAISED : getTheme().buttonFocus() == Theme.ButtonFocus.ACCENT;
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
        int start = 1;                                       // past the first gap
        for (int i = 0; i < options.size(); i++) {
            int end = start + chipWidth(options.get(i));
            if (col >= start && col < end) {
                userChooses(i, bus);
                return;
            }
            start = end + 1;
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
        Theme theme = getTheme();
        boolean raised = raised();
        boolean focused = focus.hasFocus();
        Color chosenText = chosenColor != null ? chosenColor.getColor() : theme.accentColor();
        Color accent = Color.textOn(chosenText, color);         // its text on the chip, not on the page
        Color band = !raised ? null : bandColor != null ? bandColor.getColor() : theme.buttonGroupColor();
        Color gap = band != null ? band : theme.backgroundColor();
        int col = put(segments, " ", gap, 0);
        for (DisplayableValue<V> option : options) {
            boolean chosen = Objects.equals(option.getValue(), value);
            String text = option.getDisplayValue();
            if (raised) {
                // a filled chip, as the theme's buttons: chosen, the accent's text on it; focused, the focus fill
                Color chip = !chosen ? color : focused ? theme.buttonFocusedColor() : accent.bold();
                col = put(segments, " " + text + " ", chip, col);
            } else {
                // light: the chosen chip's text in the accent colour; focused, its brackets too, and bold
                Color brackets = chosen && focused ? accent.bold() : color;
                Color inside = !chosen ? color : focused ? accent.bold() : accent;
                col = put(segments, "[", brackets, col);
                col = put(segments, text, inside, col);
                col = put(segments, "]", brackets, col);
            }
            col = put(segments, " ", gap, col);
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
