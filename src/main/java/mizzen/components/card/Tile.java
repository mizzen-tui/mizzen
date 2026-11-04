package mizzen.components.card;

import java.util.function.Function;

import mizzen.components.AbstractComponentWrappper;
import mizzen.components.TextComponent;
import mizzen.components.progressmeter.Counter;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;

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
 */
public class Tile extends AbstractComponentWrappper<Card> {

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
