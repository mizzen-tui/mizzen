package mizzen.components.input;

import mizzen.components.BoundedComponent;
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
import mizzen.util.StringUtils;

/**
 * A whole number between a least and a most, picked by moving a knob along a line; the number shows to its right.
 * <pre>
 *     Slider grey = new Slider(0, 255).setValue(125);
 *     grey.onChange((te) -> plotLighterThan(grey.getValue()));
 *
 *     ──────●──────────────  125
 * </pre>
 * Keys while focused: LEFT / RIGHT one less / more; PgDn / PgUp a tenth of the range; HOME / END the least / most.
 * The mouse: a click on the line moves the knob there, and dragging along it moves it with the mouse. Focused, the
 * knob is in the theme's accent colour.
 * <p/>
 * It is a value you watch: onChange takes any number of listeners, told on every move by the user (not on setValue).
 */
public class Slider extends BoundedComponent<Slider> implements FormInput<Integer>, ToFocusable, Clickable, AsKeyboardHandler {

    {
        defaultThemeColor(Theme::inputColor);                // given no colour: the theme's - an input
    }

    static final char LINE = '─';
    static final char KNOB = '●';

    private final int least;
    private final int most;
    private final int lineWidth;
    private final int numberWidth;                            // as wide as the widest number it can show
    private int value;

    private final OnChangeListeners<Integer> changeListeners = new OnChangeListeners<Integer>(this);   // onChange: any number

    // focus shows on the knob (accent), drawn here; the keys move it
    private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this)
            .look(LeafFocusBehaviour.Look.NONE)
            .whenFocusArrives(this::repaint)
            .whenFocusLeaves(this::repaint)
            .handleKeys(this::key)
            .handleEvents(new ConditionalEventHandler.Impl(
                    (te) -> changeListeners.canHandle(te) != null,
                    (te) -> changeListeners.handle(te)));

    /** least to most (most above least), with a line 20 wide; it starts at least */
    public Slider(int least, int most) {
        this(least, most, 20);
    }

    /** least to most (most above least), with a line lineWidth wide (at least 2); it starts at least */
    public Slider(int least, int most, int lineWidth) {
        super((Color) null, new Dimension.Simple(1, lineWidth + 2 + numberWidth(least, most)));
        if (most <= least) {
            throw new IllegalArgumentException("Slider: most (" + most + ") must be above least (" + least + ")");
        }
        if (lineWidth < 2) {
            throw new IllegalArgumentException("Slider: the line must be at least 2 wide, not " + lineWidth);
        }
        this.least = least;
        this.most = most;
        this.lineWidth = lineWidth;
        this.numberWidth = numberWidth(least, most);
        this.value = least;
    }

    private static int numberWidth(int least, int most) {
        return Math.max(String.valueOf(least).length(), String.valueOf(most).length());
    }

    @Override
    public Integer getValue() {
        return value;
    }

    /** moves the knob to value, kept between least and most; null: least. Listeners aren't told: they hear the user. */
    @Override
    public void setValue(Integer value) {
        int v = value == null ? least : Math.max(least, Math.min(most, value));
        if (v != this.value) {
            this.value = v;
            markAsDirty();
        }
    }

    /** as setValue, for chaining: new Slider(0, 255).value(125) */
    public Slider value(int value) {
        setValue(value);
        return this;
    }

    public int getLeast() {
        return least;
    }

    public int getMost() {
        return most;
    }

    @Override
    public Registration onChange(EventHandler handler) {
        return changeListeners.add(handler);
    }

    // ---- the user moves it --------------------------------------------------------------------

    private boolean key(KeyEvent ke) {
        int bigStep = Math.max(1, (most - least) / 10);
        if (ke.isKeyStroke(Keys.LEFT_KEYSTROKE)) {
            return userMoves(value - 1, ke.getEventBus());
        } else if (ke.isKeyStroke(Keys.RIGHT_KEYSTROKE)) {
            return userMoves(value + 1, ke.getEventBus());
        } else if (ke.isKeyStroke(Keys.PGDN_KEYSTROKE)) {
            return userMoves(value - bigStep, ke.getEventBus());
        } else if (ke.isKeyStroke(Keys.PGUP_KEYSTROKE)) {
            return userMoves(value + bigStep, ke.getEventBus());
        } else if (ke.isKeyStroke(Keys.HOME_KEYSTROKE)) {
            return userMoves(least, ke.getEventBus());
        } else if (ke.isKeyStroke(Keys.END_KEYSTROKE)) {
            return userMoves(most, ke.getEventBus());
        }
        return false;
    }

    /** a press or a drag on the line moves the knob to it; on the number, nothing. Position: relative to the slider. */
    @Override
    public void onClick(ApplicationMouseEvent appMouseEvent) {
        MOUSE_ACTION action = appMouseEvent.getMouseAction();
        if (!MOUSE_ACTION.DOWN_LEFT.equals(action) && !MOUSE_ACTION.DOWN_LEFT_DRAG.equals(action)) {
            return;
        }
        EventBus bus = appMouseEvent.getEventBus();
        focus.requestFocus(bus);
        int col = appMouseEvent.getMousePosition().getCol();
        if (col >= 0 && col < lineWidth) {
            userMoves(valueAt(col), bus);
        }
    }

    @Override
    public void onClick(EventBus eventBus) {
        focus.requestFocus(eventBus);
    }

    // true: the key is used (at an end, it still is: the knob stays put rather than focus moving on)
    private boolean userMoves(int to, EventBus bus) {
        int before = value;
        setValue(to);
        if (value != before) {
            bus.raiseEvent(new HasValue.ValueChangedEvent<Integer>(this));
            repaint(bus);
        }
        return true;
    }

    private void repaint(EventBus bus) {
        markAsDirty();
        RequestUpdateCanvasEvent.invoke(bus, this);
    }

    // the knob's column for a value, and the value at a column: the ends of the line are least and most
    int knobColumn() {
        return (int) Math.round((double) (value - least) * (lineWidth - 1) / (most - least));
    }

    int valueAt(int col) {
        return least + (int) Math.round((double) col * (most - least) / (lineWidth - 1));
    }

    // ---- drawing --------------------------------------------------------------------------------

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        int knob = knobColumn();
        Color knobColor = focus.hasFocus() ? Color.onBackgroundOf(getTheme().accentColor().getForeGroundColor(), color).bold()
                                           : color.bold();
        String number = String.valueOf(value);
        segments.merge(ScreenLineSegment.UTIL.segmentsOf(StringUtils.repeat(LINE, knob), color,
                                                         new Rectangle.Simple(0, 0, 1, knob)));
        segments.merge(ScreenLineSegment.UTIL.segmentsOf(String.valueOf(KNOB), knobColor, new Rectangle.Simple(0, knob, 1, 1)));
        segments.merge(ScreenLineSegment.UTIL.segmentsOf(StringUtils.repeat(LINE, lineWidth - knob - 1) + "  "
                                                         + StringUtils.repeat(' ', numberWidth - number.length()) + number,
                                                         color, new Rectangle.Simple(0, knob + 1, 1, getWidth() - knob - 1)));
    }

    // ---- focus ----------------------------------------------------------------------------------

    @Override
    public Focusable asFocusable() {
        return focus.asFocusable();
    }

    @Override
    public KeyboardHandler asKeyboardHandler() {
        return focus.asKeyboardHandler();
    }
}
