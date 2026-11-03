package mizzen.components.progressmeter;

import java.util.function.Function;

import mizzen.components.BoundedComponent;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment.SegmentImpl;

/**
 * A count with a short bar, for use inside a line - many to a screen, beside other text, in a list row or a table
 * cell. The count is the main thing; the bar only gives a sense of how far.
 * <pre>
 *     Counter tests = new Counter(10).label("passed");
 *     tests.set(18, 30);          18 passed ⣿⣿⣿⣿⣿⣿⣀⣀⣀⣀
 *     tests.set(7);               7 passed                  (no total known: no bar)
 *     tests.set(31, 30);          31 passed ⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿       (more than the total: full, the count still true)
 * </pre>
 * The bar is never full until done reaches the total, and never empty once done is past 0 - 28 of 29 doesn't look
 * finished. ProgressMeter is the other kind: one full-width bar for one job.
 * <p/>
 * Colours are its theme's: the count in the text colour (or its own), filled segments in the heading colour, empty ones
 * dim - change the bar's with barThemeColors. As a string (a row of text, a report): text(..).
 */
public class Counter extends BoundedComponent<Counter> {

    private final int cells;
    private String label = "";
    private char filledGlyph = '⣿';
    private char emptyGlyph = '⣀';
    private Function<Theme, Color> filledColor = Theme::headingColor;
    private Function<Theme, Color> emptyColor = Theme::dimColor;
    private long done;
    private long total;                                  // 0 or less: none known - no bar

    /** cells: how many segments the bar has (at least 2: one can't be both "not done" and "not empty") */
    public Counter(int cells) {
        super((Color) null, new Dimension.Simple(1, 1));
        if (cells < 2) {
            throw new IllegalArgumentException("a Counter's bar needs at least 2 cells, not " + cells);
        }
        this.cells = cells;
        this.setFillOnClear(false);
        resizeToText();
    }

    /** the word after the count, e.g. "passed"; none: "" */
    public Counter label(String label) {
        this.label = label == null ? "" : label;
        resizeToText();
        return this;
    }

    /**
     * the bar's two characters, filled and empty - e.g. ('■', '□') or ASCII ('#', '-'). Each must take one column on a
     * terminal (East Asian Width N or Na - the default braille ⣿⣀ is; ▰▱ show badly in PuTTY): the library counts one character as one column.
     */
    public Counter glyphs(char filled, char empty) {
        this.filledGlyph = filled;
        this.emptyGlyph = empty;
        markAsDirty();
        return this;
    }

    /** which of its theme's colours the bar's filled and empty segments take: by default Theme::headingColor, Theme::dimColor */
    public Counter barThemeColors(Function<Theme, Color> filled, Function<Theme, Color> empty) {
        this.filledColor = filled;
        this.emptyColor = empty;
        markAsDirty();
        return this;
    }

    /** done out of total; total 0 or less: none known - the count alone, no bar */
    public Counter set(long done, long total) {
        if (done < 0) {
            throw new IllegalArgumentException("a Counter's count can't be negative: " + done);
        }
        this.done = done;
        this.total = total;
        resizeToText();
        return this;
    }

    /** a count with no total: no bar */
    public Counter set(long done) {
        return set(done, 0);
    }

    public long getDone() {
        return done;
    }

    public long getTotal() {
        return total;
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        int filled = filledCells(done, total, cells);
        String head = filled < 0 ? head(done, label) : head(done, label) + " ";   // a space before a bar
        segments.add(new SegmentImpl(color, this, -1, head));
        if (filled < 0) {
            return;
        }
        Theme theme = getTheme();
        int col = getCol() + head.length();
        if (filled > 0) {
            segments.add(new SegmentImpl(filledColor.apply(theme), new Position.Simple(getRow(), col), -1, repeat(filledGlyph, filled)));
        }
        if (filled < cells) {
            segments.add(new SegmentImpl(emptyColor.apply(theme), new Position.Simple(getRow(), col + filled), -1, repeat(emptyGlyph, cells - filled)));
        }
    }

    private void resizeToText() {
        resize(new Dimension.Simple(1, text(done, total, cells, label).length()));
        markAsDirty();
    }

    // ---- as text

    /**
     * The counter as a plain string, for a row of text or a report:
     * <pre>
     *     Counter.text(18, 30, 10, "passed")      "18 passed ⣿⣿⣿⣿⣿⣿⣀⣀⣀⣀"
     *     Counter.text(7, 0, 10, "passed")        "7 passed"
     * </pre>
     */
    public static String text(long done, long total, int cells, String label) {
        int filled = filledCells(done, total, cells);
        return filled < 0 ? head(done, label) : head(done, label) + " " + repeat('⣿', filled) + repeat('⣀', cells - filled);
    }

    /**
     * As text(..), coloured in theme's colours (as a Counter draws itself). The colours are fixed in the string when it is
     * made - right for a report or a snapshot; on screen a Counter follows a later theme, a string doesn't.
     */
    public static String text(Theme theme, long done, long total, int cells, String label) {
        int filled = filledCells(done, total, cells);
        StringBuilder out = new StringBuilder(theme.textColor().asString()).append(head(done, label));
        if (filled >= 0) {
            out.append(" ").append(theme.headingColor().asString()).append(repeat('⣿', filled))
               .append(theme.dimColor().asString()).append(repeat('⣀', cells - filled));
        }
        return out.append(Colors.RCol.asString()).toString();
    }

    /**
     * How many of cells are filled: round(cells * done / total), but never all until done reaches total, and never none
     * once done is past 0. -1: no total known (0 or less) - no bar.
     * <pre>
     *     filledCells(28, 29, 10)   9     (rounds to 10, but it isn't finished)
     *     filledCells(1, 100, 10)   1     (rounds to 0, but it has started)
     *     filledCells(31, 30, 10)   10    (past the total: full)
     * </pre>
     */
    public static int filledCells(long done, long total, int cells) {
        if (total <= 0) {
            return -1;
        }
        int filled = (int) Math.min(cells, Math.round(cells * (double) done / total));
        if (done < total) {
            filled = Math.min(filled, cells - 1);
        }
        if (done > 0) {
            filled = Math.max(filled, 1);
        }
        return filled;
    }

    // the count and its label: "18 passed"
    private static String head(long done, String label) {
        return label == null || label.isEmpty() ? String.valueOf(done) : done + " " + label;
    }

    private static String repeat(char ch, int n) {
        StringBuilder b = new StringBuilder(Math.max(0, n));
        for (int i = 0; i < n; i++) {
            b.append(ch);
        }
        return b.toString();
    }
}
