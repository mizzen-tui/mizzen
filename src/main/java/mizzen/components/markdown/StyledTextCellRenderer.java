package mizzen.components.markdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import mizzen.components.markdown.MarkdownDocument.Run;
import mizzen.components.markdown.MarkdownDocument.RunStyle;
import mizzen.components.markdown.MarkdownView.Piece;
import mizzen.components.table.CellRenderer;
import mizzen.components.table.CellWidths;
import mizzen.components.theme.Theme;
import mizzen.controlcodes.color.Color;
import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment.SegmentImpl;
import mizzen.textsegments.ScreenLineSegmentsBuffer;

/**
 * A table cell of a markdown document: its runs in their colours, wrapped as the document's text is, a
 * column of room each side. The value is its runs (a heading's: a plain string).
 * <p/>
 * A cell is drawn from its value and size alone, so wrapping them again says what is where: linkAt,
 * from what a table reports about a click (TableComponent.CellSelected).
 */
final class StyledTextCellRenderer implements CellRenderer {

    private static final int ROOM = 1;

    private final Supplier<Color> forced;                // plain text in this colour; null from it: the document's
    private final Supplier<Theme> theme;                 // the document's: asked as cells are drawn, so they follow it
    private final Align align;                           // null: as the table says; else always this (a heading: its column's)

    StyledTextCellRenderer(Supplier<Color> forced, Supplier<Theme> theme) {
        this(forced, theme, null);
    }

    StyledTextCellRenderer(Supplier<Color> forced, Supplier<Theme> theme, Align align) {
        this.forced = forced;
        this.theme = theme;
        this.align = align;
    }

    @SuppressWarnings("unchecked")
    private static List<Run> runsOf(Object value) {
        return value instanceof String ? Collections.singletonList(new Run((String) value, RunStyle.PLAIN, null)) : (List<Run>) value;
    }

    // wrapped at spaces, a word too wide for the cell cut with …; then each line placed by align in the cell
    private List<List<Piece>> lines(Object value, int width, Align align) {
        List<List<Piece>> lines = MarkdownView.wrapped(runsOf(value), width - ROOM, ROOM, ROOM, forced.get(), theme.get(), true);
        if (align == null || align == Align.LEFT) {
            return lines;
        }
        List<List<Piece>> placed = new ArrayList<List<Piece>>();
        for (List<Piece> line : lines) {
            Piece last = line.get(line.size() - 1);
            int free = Math.max(0, width - ROOM - (last.col + last.text.length()));
            int shift = align == Align.RIGHT ? free : free / 2;
            List<Piece> moved = new ArrayList<Piece>();
            for (Piece piece : line) {
                moved.add(new Piece(piece.col + shift, piece.text, piece.color, piece.target));
            }
            placed.add(moved);
        }
        return placed;
    }

    @Override
    public CellWidths widths(Object value) {
        int most = 0, least = 0;
        StringBuilder text = new StringBuilder();
        for (Run run : runsOf(value)) {
            text.append(run.text);
        }
        most = text.length();
        for (String word : text.toString().split(" ")) {
            least = Math.max(least, word.length());
        }
        int cut = Math.min(1, least);                    // narrower than its longest word: the word is broken
        return new CellWidths(cut + 2 * ROOM, least + 2 * ROOM, most + 2 * ROOM);
    }

    @Override
    public int heightAt(Object value, int width) {
        return Math.max(1, lines(value, width, Align.LEFT).size());
    }

    @Override
    public IScreenLineSegmentsBuffer render(Object value, Dimension size, Align align) {
        IScreenLineSegmentsBuffer drawn = new ScreenLineSegmentsBuffer();
        List<List<Piece>> lines = lines(value, size.getWidth(), this.align != null ? this.align : align);
        for (int row = 0; row < lines.size() && row < size.getHeight(); row++) {
            for (Piece piece : lines.get(row)) {
                drawn.add(new SegmentImpl(piece.color, new Position.Simple(row, piece.col), piece.text));
            }
        }
        return drawn;
    }

    /** the link at offset in a cell of this value drawn at size: its target; null: no link there */
    String linkAt(Object value, Dimension size, Position offset, Align align) {
        List<List<Piece>> lines = lines(value, size.getWidth(), align);
        if (offset.getRow() < 0 || offset.getRow() >= lines.size()) {
            return null;
        }
        return MarkdownView.targetIn(lines.get(offset.getRow()), offset.getCol());
    }
}
