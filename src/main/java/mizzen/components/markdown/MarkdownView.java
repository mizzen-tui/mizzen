package mizzen.components.markdown;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.BoundedComponent;
import mizzen.components.markdown.MarkdownDocument.Block;
import mizzen.components.markdown.MarkdownDocument.BlockKind;
import mizzen.components.markdown.MarkdownDocument.Run;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.textsegments.ScreenLineSegment;
import mizzen.util.formatters.JavaSyntaxHilighterTextFormatter;

/**
 * Markdown on the screen: headings, lists, quotes, tables and code in colours, words wrapped to the
 * width. It is as tall as its text, so it goes in a scroll pane - inScrollPane does that, and wraps
 * again when the pane is resized.
 * <pre>
 *     ScrollableComponent readme = MarkdownView.inScrollPane(Colors.IGre, markdownText);
 *     panel.add(readme);
 * </pre>
 * Pictures show as their words, in brackets. Each piece of a link keeps where it points (Piece.target),
 * but a click does nothing yet.
 */
public class MarkdownView extends BoundedComponent<MarkdownView> {

    /** text on one line, from col, in one colour - and where it points, if it is a link or picture */
    static final class Piece {
        final int col;
        final String text;
        final Color color;
        final String target;

        Piece(int col, String text, Color color, String target) {
            this.col = col;
            this.text = text;
            this.color = color;
            this.target = target;
        }
    }

    private final MarkdownDocument document;
    final List<List<Piece>> lines = new ArrayList<List<Piece>>();
    private int laidOutTo = -1;

    public MarkdownView(String markdown) {
        super(Colors.Whi, Dimension.ZERO);
        this.document = MarkdownParser.parse(markdown);
    }

    /** markdown, wrapped to the width inside a scroll pane, and again each time the pane is resized */
    public static ScrollableComponent inScrollPane(Color color, String markdown) {
        MarkdownView view = new MarkdownView(markdown);
        ScrollableComponent pane = new ScrollableComponent(color, view);
        pane.appendOnResizeRule((size) -> view.fitTo(pane.getViewPortAsRectangle().getWidth()));
        return pane;
    }

    /** lay the text out to width: this component becomes that wide, and as tall as the text then is */
    public void fitTo(int width) {
        if (width == laidOutTo || width < 1) {
            return;
        }
        laidOutTo = width;
        lines.clear();
        Block previous = null;
        for (Block block : document.blocks) {
            boolean listGoesOn = previous != null && previous.kind == BlockKind.LIST_ITEM && block.kind == BlockKind.LIST_ITEM;
            if (previous != null && !listGoesOn) {
                lines.add(new ArrayList<Piece>());                             // a blank line between blocks
            }
            layOut(block, width);
            previous = block;
        }
        resize(new Dimension.Simple(Math.max(1, lines.size()), width));
        markAsDirty();
    }

    private void layOut(Block block, int width) {
        switch (block.kind) {
            case HEADING:
                layOutHeading(block, width);
                break;
            case PARAGRAPH:
                lines.addAll(wrapped(block.runs, width, 0, 0, null));
                break;
            case LIST_ITEM:
                layOutListItem(block, width);
                break;
            case QUOTE:
                for (List<Piece> line : wrapped(block.runs, width, 2, 2, Colors.IBla)) {
                    line.add(0, new Piece(0, "│", Colors.IBla, null));
                    lines.add(line);
                }
                break;
            case CODE:
                layOutCode(block);
                break;
            case TABLE:
                layOutTable(block, width);
                break;
            case RULE:
                lines.add(line(new Piece(0, repeat('─', width), Colors.IBla, null)));
                break;
            default:
                throw new IllegalStateException("no layout for " + block.kind);
        }
    }

    private void layOutHeading(Block block, int width) {
        Color color = block.level == 1 ? Colors.BYel : block.level == 2 ? Colors.BCya : Colors.BGre;
        List<List<Piece>> title = wrapped(block.runs, width, 0, 0, color);
        lines.addAll(title);
        if (block.level <= 2) {
            int wide = 0;
            for (List<Piece> line : title) {
                Piece last = line.get(line.size() - 1);
                wide = Math.max(wide, last.col + last.text.length());
            }
            lines.add(line(new Piece(0, repeat(block.level == 1 ? '═' : '─', wide), color, null)));
        }
    }

    private void layOutListItem(Block block, int width) {
        int indent = block.level * 2;
        List<List<Piece>> item = wrapped(block.runs, width, indent + block.marker.length(), indent + block.marker.length(), null);
        if (!item.isEmpty()) {
            item.get(0).add(0, new Piece(indent, block.marker, Colors.BCya, null));
        }
        lines.addAll(item);
    }

    // not wrapped: code keeps its lines, and the view cuts them at its edge. Java in its colours.
    private void layOutCode(Block block) {
        JavaSyntaxHilighterTextFormatter java = "java".equalsIgnoreCase(block.language) ? new JavaSyntaxHilighterTextFormatter() : null;
        for (String code : block.codeLines) {
            lines.add(line(new Piece(2, java != null ? java.getFormattedText(code) : code, java != null ? Colors.Whi : Colors.Yel, null)));
        }
    }

    // ---- tables: columns as wide as their text, the widest narrowed until they fit; cells wrap

    private void layOutTable(Block table, int width) {
        int columns = 0;
        for (List<List<Run>> row : table.tableRows) {
            columns = Math.max(columns, row.size());
        }
        int[] widths = new int[columns];
        for (List<List<Run>> row : table.tableRows) {
            for (int c = 0; c < row.size(); c++) {
                widths[c] = Math.max(widths[c], lengthOf(row.get(c)));
            }
        }
        int room = width - 3 * Math.max(0, columns - 1);                       // " │ " between columns
        for (int total = sum(widths); total > room && narrowWidest(widths); total--) {
        }
        for (int r = 0; r < table.tableRows.size(); r++) {
            List<List<Run>> row = table.tableRows.get(r);
            if (r == 0 && isBlank(row)) {
                continue;                                                      // | | | : a table with no heading
            }
            layOutRow(row, widths, r == 0 ? Colors.BWhi : null);
            if (r == 0) {
                layOutHeadingLine(widths);
            }
        }
    }

    private void layOutRow(List<List<Run>> row, int[] widths, Color forced) {
        List<List<List<Piece>>> cells = new ArrayList<List<List<Piece>>>();
        int high = 1;
        int col = 0;
        for (int c = 0; c < widths.length; c++) {
            List<Run> runs = c < row.size() ? row.get(c) : new ArrayList<Run>();
            List<List<Piece>> cell = wrapped(runs, col + widths[c], col, col, forced);
            cells.add(cell);
            high = Math.max(high, cell.size());
            col += widths[c] + 3;
        }
        for (int l = 0; l < high; l++) {
            List<Piece> line = new ArrayList<Piece>();
            col = 0;
            for (int c = 0; c < widths.length; c++) {
                if (l < cells.get(c).size()) {
                    line.addAll(cells.get(c).get(l));
                }
                col += widths[c];
                if (c < widths.length - 1) {
                    line.add(new Piece(col + 1, "│", Colors.IBla, null));
                }
                col += 3;
            }
            lines.add(line);
        }
    }

    private void layOutHeadingLine(int[] widths) {
        StringBuilder rule = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            rule.append(repeat('─', widths[c]));
            if (c < widths.length - 1) {
                rule.append("─┼─");
            }
        }
        lines.add(line(new Piece(0, rule.toString(), Colors.IBla, null)));
    }

    private static boolean narrowWidest(int[] widths) {
        int widest = 0;
        for (int c = 1; c < widths.length; c++) {
            if (widths[c] > widths[widest]) {
                widest = c;
            }
        }
        if (widths.length == 0 || widths[widest] <= 1) {
            return false;
        }
        widths[widest]--;
        return true;
    }

    private static int sum(int[] widths) {
        int sum = 0;
        for (int w : widths) {
            sum += w;
        }
        return sum;
    }

    private static boolean isBlank(List<List<Run>> row) {
        for (List<Run> cell : row) {
            if (!cell.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static int lengthOf(List<Run> runs) {
        int length = 0;
        for (Run run : runs) {
            length += run.text.length();
        }
        return length;
    }

    // ---- wrapping

    /** runs as lines ending before column end; the first starts at firstCol, the rest at nextCol. forced: every plain word in this colour */
    private List<List<Piece>> wrapped(List<Run> runs, int end, int firstCol, int nextCol, Color forced) {
        List<List<Piece>> wrapped = new ArrayList<List<Piece>>();
        List<Piece> line = new ArrayList<Piece>();
        int col = firstCol;
        boolean spaceOwed = false;
        for (Run run : runs) {
            Color color = colorOf(run, forced);
            int i = 0;
            while (i < run.text.length()) {
                if (run.text.charAt(i) == ' ') {
                    spaceOwed = !line.isEmpty();
                    i++;
                    continue;
                }
                int wordEnd = run.text.indexOf(' ', i);
                String word = run.text.substring(i, wordEnd < 0 ? run.text.length() : wordEnd);
                i += word.length();
                while (!word.isEmpty()) {
                    int start = col + (spaceOwed ? 1 : 0);
                    if (start + word.length() > end && !line.isEmpty()) {       // the next line
                        wrapped.add(line);
                        line = new ArrayList<Piece>();
                        col = nextCol;
                        spaceOwed = false;
                        continue;
                    }
                    int fits = Math.max(1, Math.min(word.length(), end - start));   // a word wider than the line: broken
                    add(line, start, spaceOwed, word.substring(0, fits), color, run.target);
                    col = start + fits;
                    spaceOwed = false;
                    word = word.substring(fits);
                }
            }
        }
        if (!line.isEmpty()) {
            wrapped.add(line);
        }
        return wrapped;
    }

    // joined to the piece before when it is the same kind of text, so a line isn't a segment a word
    private static void add(List<Piece> line, int col, boolean spaced, String word, Color color, String target) {
        Piece last = line.isEmpty() ? null : line.get(line.size() - 1);
        boolean same = last != null && last.color == color && (target == null ? last.target == null : target.equals(last.target));
        if (same) {
            line.set(line.size() - 1, new Piece(last.col, last.text + (spaced ? " " : "") + word, color, target));
        } else {
            line.add(new Piece(col, word, color, target));
        }
    }

    private static Color colorOf(Run run, Color forced) {
        switch (run.style) {
            case BOLD:   return Colors.BWhi;
            case ITALIC: return Colors.Cya;
            case CODE:   return Colors.Yel;
            case LINK:   return Colors.BBlu;
            case IMAGE:  return Colors.BPur;
            default:     return forced != null ? forced : Colors.Whi;
        }
    }

    private static List<Piece> line(Piece piece) {
        List<Piece> line = new ArrayList<Piece>();
        line.add(piece);
        return line;
    }

    private static String repeat(char c, int times) {
        return new String(new char[Math.max(0, times)]).replace('\0', c);
    }

    // ---- drawing

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        for (int row = 0; row < lines.size(); row++) {
            for (Piece piece : lines.get(row)) {
                if (piece.col < getWidth()) {                                  // code wider than the view stops at its edge
                    segments.merge(ScreenLineSegment.UTIL.segmentsOf(piece.text, piece.color,
                            new Rectangle.Simple(row, piece.col, 1, getWidth() - piece.col)));
                }
            }
        }
    }
}
