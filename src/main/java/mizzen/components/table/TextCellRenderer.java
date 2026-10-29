package mizzen.components.table;

import java.util.function.BooleanSupplier;
import java.util.function.Function;

import mizzen.components.TextComponent;
import mizzen.controlcodes.color.Color;
import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.textsegments.CleanString;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegmentsBuffer;
import mizzen.util.StringUtils;

/**
 * Text in a cell: the value as a string, wrapped at word breaks to the width (if it wraps), each
 * line aligned and padded to it - or cut short (if it may be cut). A word wider than the cell is
 * broken across lines.
 * <pre>
 *     new TextCellRenderer()                                  toString, wraps, may be cut
 *     new TextCellRenderer().text((v) -&gt; money((Float) v))    "$12.50"
 *     new TextCellRenderer().color((v) -&gt; overdue(v) ? Colors.BRed : null)
 * </pre>
 * A table's columns get one each, set from the column: numbers neither wrap nor are cut.
 * <p/>
 * Its widths: most is the longest line, least the longest word if it wraps (else most), cut 1 if
 * it may be cut (else least). One character is one column.
 */
public class TextCellRenderer implements CellRenderer {

    private Function<Object, String> text = (value) -> value == null ? "" : value.toString();
    private Function<Object, Color> color = null;          // null: the text component's own
    private BooleanSupplier wraps = () -> true;
    private BooleanSupplier mayBeCut = () -> true;

    // draws the text: one, reused - the table copies what it draws
    private final TextComponent drawn = new TextComponent(new CleanString.Simple(""));

    /** the cells of column, as it says: whether it wraps, whether it may be cut - asked each time, so a change shows */
    static TextCellRenderer forColumn(TableColumn column) {
        TextCellRenderer renderer = new TextCellRenderer();
        renderer.wraps = column::isWordWrappable;
        renderer.mayBeCut = column::isTruncationAllowed;
        return renderer;
    }

    /** the string shown for a value; the default: toString, "" for null */
    public TextCellRenderer text(Function<Object, String> text) {
        this.text = text;
        return this;
    }

    /** the color a value is shown in; null from it: the default */
    public TextCellRenderer color(Function<Object, Color> color) {
        this.color = color;
        return this;
    }

    public TextCellRenderer wraps(boolean wraps) {
        this.wraps = () -> wraps;
        return this;
    }

    public TextCellRenderer mayBeCut(boolean mayBeCut) {
        this.mayBeCut = () -> mayBeCut;
        return this;
    }

    @Override
    public CellWidths widths(Object value) {
        String shown = text.apply(value);
        int most = 0;
        for (String line : shown.split("\n")) {
            most = Math.max(most, line.length());
        }
        int least = wraps.getAsBoolean() ? Math.min(StringUtils.longestWord(shown), most) : most;
        int cut = mayBeCut.getAsBoolean() ? Math.min(1, least) : least;
        return new CellWidths(cut, least, most);
    }

    @Override
    public int heightAt(Object value, int width) {
        return Math.max(1, lines(value, width, -1, Align.LEFT).split("\n", -1).length);
    }

    @Override
    public IScreenLineSegmentsBuffer render(Object value, Dimension size, Align align) {
        if (size.getHeight() == 0 || size.getWidth() == 0) {
            return new ScreenLineSegmentsBuffer();
        }
        if (color != null) {
            Color shownIn = color.apply(value);
            if (shownIn != null) {
                drawn.setColor(shownIn);
            }
        }
        drawn.setTextAndResize(lines(value, size.getWidth(), size.getHeight(), align));
        return drawn.getSegmentsBuffer();
    }

    // the text as it is drawn: wrapped to width (at most maxHeight lines; -1: all), each line aligned to it
    private String lines(Object value, int width, int maxHeight, Align align) {
        String shown = text.apply(value);
        if (wraps.getAsBoolean()) {
            shown = StringUtils.wrap(shown, width, "\n", false, maxHeight);
        }
        String[] lines = shown.split("\n");
        for (int i = 0; i < lines.length; i++) {
            lines[i] = Align.UTIL.formatToAlignedData(lines[i], width, align, width, mayBeCut.getAsBoolean());
        }
        String aligned = String.join("\n", lines);
        if (aligned.length() > width) {
            aligned = StringUtils.wrap(aligned, width, "\n", true, maxHeight);   // a word wider than the cell: broken
        }
        return aligned;
    }
}
