package mizzen.components.card;

import java.util.function.Function;

import mizzen.components.AbstractComponentWrappper;
import mizzen.components.TextComponent;
import mizzen.components.input.LeafFocusBehaviour;
import mizzen.components.progressmeter.Counter;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.CustomApplicationEvent;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.AsKeyboardHandler;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.events.mouse.Clickable;
import mizzen.events.keyboard.Keys;

/**
 * A tile for a dashboard: a card with a title, a value that reads at a glance, an optional note and an optional Counter
 * bar. Its state colours the value - OK the theme's success colour, WARNING its warning, ALERT its error; NORMAL its
 * strong text. Given a width, tiles side by side line up as a grid.
 * <pre>
 *     Tile site = new Tile("Site", 22).value("HTTP 200").note("store.example.com");
 *     Tile disk = new Tile("Disk", 22).value("52% used").bar(52, 100);
 *     site.state(Tile.State.ALERT);                   the value turns red - the site check failed
 *
 *     ╭ Site ──────────────╮  ╭ Disk ──────────────╮
 *     │ HTTP 200           │  │ 52% used           │
 *     │ store.example.com  │  │ 52 ⣿⣿⣿⣿⣿⣀⣀⣀⣀⣀      │
 *     ╰────────────────────╯  ╰────────────────────╯
 * </pre>
 * Rows show in the order they are first set: the value, then a note or a bar.
 * <p/>
 * Given an action (onActivate), a tile is a TAB stop: focused, its frame and title are in the theme's accent colour
 * (as an outlined button's), and ENTER or a click activates it - the action gets a Tile.ActivateEvent. With none it
 * only shows. One action, as a tree's or a table's: a new one replaces it.
 * <pre>
 *     site.onActivate((te) -> showSiteDetails());
 * </pre>
 */
public class Tile extends AbstractComponentWrappper<Card> implements ToFocusable, Clickable, AsKeyboardHandler {

    /** a tile activated: ENTER on it, or a click. getTile() is the one. */
    public static class ActivateEvent extends CustomApplicationEvent<Tile> {
        private static final long serialVersionUID = 1L;

        ActivateEvent(Tile tile) {
            super(tile, tile);
        }

        public Tile getTile() {
            return getEventData();
        }
    }

    /** what the value's colour says */
    public enum State {
        NORMAL(Theme::strongColor), OK(Theme::successColor), WARNING(Theme::warningColor), ALERT(Theme::errorColor);

        final Function<Theme, Color> color;

        State(Function<Theme, Color> color) {
            this.color = color;
        }
    }

    private final int inside;                           // the rows' width: the tile's, less frame and room; 0: as they are
    private TextComponent value;
    private TextComponent note;
    private Counter bar;
    private State state = State.NORMAL;
    private EventHandler action;                        // what activating it does; null: it only shows (not focusable)

    // focus shows on the frame (accent), so the value keeps its state's colour; ENTER activates
    private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this).look(LeafFocusBehaviour.Look.NONE)
            .whenFocusArrives((bus) -> getWrappedComponent().frameColor(Theme::accentColor))
            .whenFocusLeaves((bus) -> getWrappedComponent().frameColor(Theme::textColor))
            .handleKeys(AsKeyboardHandler.UTIL.toKeyboardHandler(
                    new KeyInputEventHandler((te) -> activate(te.getEventBus()), Keys.ENTER_KEYSTROKE)))
            .handleEvents(new ConditionalEventHandler.Impl(
                    (te) -> te instanceof ActivateEvent && te.getSource() == this && action != null,
                    (te) -> action.handle(te)));

    /** a tile as wide as what it holds */
    public Tile(String title) {
        this(title, 0);
    }

    /** a tile width columns wide (frame included) - tiles of one width line up */
    public Tile(String title, int width) {
        super((Color) null, new Card(title));
        this.inside = Math.max(0, width - 4);
    }

    /** the main figure: "HTTP 200", "52% used", "9 h ago" - in the state's colour */
    public Tile value(String text) {
        if (value == null) {
            value = new TextComponent(fit(text));
            value.setThemeColor((theme) -> state.color.apply(theme));
            getWrappedComponent().add(value);
        } else {
            value.setTextAndResize(fit(text));
        }
        return this;
    }

    /** a line under the value, dimmed: "store.example.com", "1904 MB" */
    public Tile note(String text) {
        if (note == null) {
            note = new TextComponent(fit(text)).setThemeColor(Theme::dimColor);
            getWrappedComponent().add(note);
        } else {
            note.setTextAndResize(fit(text));
        }
        return this;
    }

    /** a Counter bar: done of total (total 0 or less: the count alone) */
    public Tile bar(long done, long total) {
        if (bar == null) {
            bar = new Counter(10);
            getWrappedComponent().add(bar);
        }
        bar.set(done, total);
        return this;
    }

    /** the value's colour: NORMAL (strong text), OK, WARNING or ALERT */
    public Tile state(State state) {
        this.state = state;
        if (value != null) {
            value.markAsDirty();
        }
        return this;
    }

    public State getState() {
        return state;
    }

    /**
     * What ENTER on the focused tile, or a click, does: action gets a Tile.ActivateEvent. One action - a new one replaces
     * it. Given one, the tile is a TAB stop; null: it only shows again.
     */
    public Tile onActivate(EventHandler action) {
        this.action = action;
        return this;
    }

    private void activate(EventBus eventBus) {
        eventBus.raiseEvent(new ActivateEvent(this));
    }

    @Override
    public void onClick(EventBus eventBus) {
        if (action != null) {
            focus.requestFocus(eventBus);
            activate(eventBus);
        }
    }

    /** null - not focusable - while it has no action */
    @Override
    public Focusable asFocusable() {
        return action != null ? focus.asFocusable() : null;
    }

    @Override
    public KeyboardHandler asKeyboardHandler() {
        return action != null ? focus.asKeyboardHandler() : null;
    }

    // text at the rows' width: padded to it, or cut with … - so every row, and every tile of a width, lines up
    private String fit(String text) {
        if (inside == 0) {
            return text;
        }
        if (text.length() > inside) {
            return text.substring(0, inside - 1) + "…";
        }
        StringBuilder padded = new StringBuilder(text);
        while (padded.length() < inside) {
            padded.append(' ');
        }
        return padded.toString();
    }
}
