package mizzen.components.markdown;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import mizzen.components.BoundedComponent;
import mizzen.components.input.LeafFocusBehaviour;
import mizzen.components.markdown.MarkdownDocument.Block;
import mizzen.components.markdown.MarkdownDocument.BlockKind;
import mizzen.components.markdown.MarkdownDocument.Run;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.components.table.TableColumnImpl;
import mizzen.components.table.TableComponent;
import mizzen.components.table.TableComponent.CellSelected;
import mizzen.components.table.TableModel;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.ChainedEventHandler;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.Registration;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.events.mouse.Clickable;
import mizzen.events.mouse.MOUSE_ACTION;
import mizzen.geom.Align;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
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
 * Pictures show as their words, in brackets.
 * <p/>
 * Links are underlined; a click raises a LinkClickedEvent, and the newest onLink handler that wants the
 * target gets it. Failing those, with documents to read from (inScrollPane with a source), a link to a
 * .md opens that document here, relative to the one showing, with a Back line at the top - itself a link,
 * to BACK. Any other link does nothing.
 * <pre>
 *     ScrollableComponent docs = MarkdownView.inScrollPane(Colors.IGre, view);
 *     view.onLink((target) -&gt; target.endsWith(".java"), (te) -&gt; showSource(te.getTarget(), te.getEventBus()));
 * </pre>
 * Tables are real TableComponents, drawn off to one side - pictures of tables: they take no clicks of
 * their own, so the view asks one what is at a spot (TableComponent.cellAt).
 */
public class MarkdownView extends BoundedComponent<MarkdownView> implements ToFocusable, Clickable {

    /** the target of the Back line: a link like any other - a handler that wants it can take it */
    public static final String BACK = "mizzen:back";

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

    private MarkdownDocument document;
    final List<List<Piece>> lines = new ArrayList<List<Piece>>();
    private int laidOutTo = -1;
    private final ChainedEventHandler linkHandlers = ChainedEventHandler.useChainOfResponsibilityStrategy("MarkdownView-links");
    private final List<TableShown> tables = new ArrayList<TableShown>();
    private Function<String, String> source = null;                            // a document's text from its path; null: none to open
    private String path = "";                                                  // the document showing, when it came from source
    private final Deque<String> history = new ArrayDeque<String>();            // the paths Back goes to
    private Runnable onNewDocument = () -> { };

    private Supplier<Rectangle> showing = () -> this;                          // the part in view, in a scroll pane

    // it takes focus, showing nothing: only a component that does is told where a click landed. Keys go on to its
    // parents (the pane scrolls). Focus arriving shows the part already in view - the whole of it would jump to the top.
    private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this).look(LeafFocusBehaviour.Look.NONE)
            .showOnFocus(() -> showing.get())
            .handleEvents(linkHandlers.asConditionalEventHandler());

    private static final StyledTextCellRenderer CELLS = new StyledTextCellRenderer(null);
    private static final StyledTextCellRenderer HEADINGS = new StyledTextCellRenderer(Colors.BWhi);

    /** a table in the document, and the line it starts on */
    private static final class TableShown {
        final TableComponent<Object> table;
        final TableModel<Object> model;
        final int top;

        TableShown(TableComponent<Object> table, TableModel<Object> model, int top) {
            this.table = table;
            this.model = model;
            this.top = top;
        }
    }

    public MarkdownView(String markdown) {
        super(Colors.Whi, Dimension.ZERO);
        this.document = MarkdownParser.parse(markdown);
        opensDocuments();
    }

    /** the document at path, from source: its text for a path, null if there is none - links to other .md files open through it too */
    public MarkdownView(String path, Function<String, String> source) {
        super(Colors.Whi, Dimension.ZERO);
        this.source = source;
        this.path = path;
        this.document = read(path);
        opensDocuments();
    }

    /** markdown, wrapped to the width inside a scroll pane, and again each time the pane is resized */
    public static ScrollableComponent inScrollPane(Color color, String markdown) {
        MarkdownView view = new MarkdownView(markdown);
        return inScrollPane(color, view);
    }

    /** the document at path, from source, in a scroll pane titled with its path - back to the top when a link opens another */
    public static ScrollableComponent inScrollPane(Color color, String path, Function<String, String> source) {
        return inScrollPane(color, new MarkdownView(path, source));
    }

    /** view, wrapped to the width inside a scroll pane, and again each time the pane is resized */
    public static ScrollableComponent inScrollPane(Color color, MarkdownView view) {
        ScrollableComponent pane = new ScrollableComponent(color, view);
        pane.appendOnResizeRule((size) -> view.fitTo(pane.getViewPortAsRectangle().getWidth()));
        view.showing = () -> new Rectangle.Simple(pane.getOffsetIntoSource(), new Dimension.Simple(pane.getViewPortAsRectangle()));
        view.onNewDocument = () -> {
            pane.setViewOffset(0, 0);
            pane.setTitle(" " + view.path + " ");
        };
        if (view.source != null) {
            pane.setTitle(" " + view.path + " ");
        }
        return pane;
    }

    /** the document at path, from source, with Back to what showed before; already showing it: nothing moves */
    public MarkdownView open(String newPath) {
        if (!newPath.equals(path)) {
            history.push(path);
            show(newPath);
        }
        return this;
    }

    /**
     * handler hears each link clicked whose target (as the document wrote it) wanted accepts - the
     * newest wanting it, and it alone. The Registration takes it out again.
     */
    public Registration onLink(Predicate<String> wanted, Consumer<LinkClickedEvent> handler) {
        ConditionalEventHandler claiming = new ConditionalEventHandler.Impl(
                (te) -> isMyLink(te) && wanted.test(((LinkClickedEvent) te).getTarget()),
                (te) -> handler.accept((LinkClickedEvent) te));
        linkHandlers.prependEventHandler(claiming);
        return () -> linkHandlers.removeEventHandler(claiming);
    }

    private boolean isMyLink(TerminalEvent<?> te) {
        return te instanceof LinkClickedEvent && te.getSource() == this;
    }

    // last on the chain: Back, and a link no handler wanted to a document that source can read, opens here
    private void opensDocuments() {
        linkHandlers.appendEventHandler(new ConditionalEventHandler.Impl(
                (te) -> isMyLink(te) && BACK.equals(((LinkClickedEvent) te).getTarget()) && !history.isEmpty(),
                (te) -> {
                    show(history.pop());
                    RequestUpdateCanvasEvent.invoke(te.getEventBus(), this);
                }));
        linkHandlers.appendEventHandler(new ConditionalEventHandler.Impl(
                (te) -> isMyLink(te) && source != null && isDocument(fileOf(((LinkClickedEvent) te).getTarget())),
                (te) -> {
                    history.push(path);
                    show(relativeToShowing(fileOf(((LinkClickedEvent) te).getTarget())));
                    RequestUpdateCanvasEvent.invoke(te.getEventBus(), this);
                }));
    }

    private static String fileOf(String target) {
        return target.contains("#") ? target.substring(0, target.indexOf('#')) : target;
    }

    private static boolean isDocument(String file) {
        return file.toLowerCase().endsWith(".md") && !file.contains("://");
    }

    /** the link at row, col of this view: its target; null: no link there */
    public String linkAt(int row, int col) {
        if (row < 0 || row >= lines.size()) {
            return null;
        }
        for (TableShown shown : tables) {
            if (row >= shown.top && row < shown.top + shown.table.getHeight()) {
                CellSelected cell = shown.table.cellAt(new Position.Simple(row - shown.top, col));
                return cell == null || cell.isHeader() ? null : CELLS.linkAt(
                        shown.model.getValueAt(cell.getTableRow(), cell.getTableCol()), cell.getCellSize(), cell.getOffsetInCell());
            }
        }
        return targetIn(lines.get(row), col);
    }

    /** the target of the piece of line at col; null: none */
    static String targetIn(List<Piece> line, int col) {
        for (Piece piece : line) {
            if (piece.target != null && col >= piece.col && col < piece.col + piece.text.length()) {
                return piece.target;
            }
        }
        return null;
    }

    @Override
    public Focusable asFocusable() {
        return focus.asFocusable();
    }

    @Override
    public void onClick(ApplicationMouseEvent click) {
        if (!MOUSE_ACTION.DOWN_LEFT.equals(click.getMouseAction()) || click.getMousePosition() == null) {
            return;
        }
        String target = linkAt(click.getMousePosition().getRow(), click.getMousePosition().getCol());
        EventBus bus = click.getEventBus();
        if (target != null) {
            focus.requestFocus(bus);                                           // the handlers hear it through focus
            bus.raiseEvent(new LinkClickedEvent(this, target));
        }
    }

    @Override
    public void onClick(EventBus eventBus) {
        // only clicks with a position mean anything here: onClick(ApplicationMouseEvent)
    }

    private void show(String newPath) {
        path = newPath;
        document = read(newPath);
        int width = laidOutTo;
        laidOutTo = -1;
        fitTo(width);
        onNewDocument.run();
    }

    private MarkdownDocument read(String documentPath) {
        String text = source.apply(documentPath);
        return MarkdownParser.parse(text != null ? text : "# Not found\n\nThere is no " + documentPath);
    }

    // "B.md" from docs/A.md is docs/B.md, "../README.md" is README.md
    private String relativeToShowing(String file) {
        int slash = path.lastIndexOf('/');
        Deque<String> parts = new ArrayDeque<String>();
        for (String part : ((slash < 0 ? "" : path.substring(0, slash + 1)) + file).split("/")) {
            if (part.equals("..")) {
                parts.pollLast();
            } else if (!part.equals(".") && !part.isEmpty()) {
                parts.addLast(part);
            }
        }
        return String.join("/", parts);
    }

    /** lay the text out to width: this component becomes that wide, and as tall as the text then is */
    public void fitTo(int width) {
        if (width == laidOutTo || width < 1) {
            return;
        }
        laidOutTo = width;
        lines.clear();
        tables.clear();
        if (!history.isEmpty()) {
            lines.add(line(new Piece(0, "<< Back", Colors.UCya, BACK)));
            lines.add(new ArrayList<Piece>());
        }
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

    // ---- tables: a real TableComponent draws each, off to one side; its segments become this view's lines

    private void layOutTable(Block block, int width) {
        TableModel<Object> model = tableModelOf(block);
        TableComponent<Object> table = new TableComponent<Object>(Colors.IBla, model);
        table.setLinesStyle(TableComponent.TableLinesStyle.INNER);
        table.setSpacedRows(false);
        for (int c = 0; c < model.getColumnCount(); c++) {
            TableColumnImpl column = new TableColumnImpl(model.getColumnName(c), null, model.getColumnName(c), c, Align.LEFT, null, true, false);
            column.setColumnRenderer(CELLS);
            column.setHeaderRenderer(HEADINGS);
            table.addColumn(column);
        }
        int wide = Math.min(width, table.widthNeeded());
        table.resize(new Dimension.Simple(table.heightAt(wide), wide));
        int top = lines.size();
        for (int row = 0; row < table.getHeight(); row++) {
            lines.add(new ArrayList<Piece>());
        }
        for (ScreenLineSegment segment : table.getSegmentsBuffer().getSegments()) {
            lines.get(top + segment.getRow()).add(new Piece(segment.getCol(), segment.getText(), segment.getColors(), null));
        }
        tables.add(new TableShown(table, model, top));
    }

    // the block's rows as a model: the first are the headings, each cell's value its runs
    private static TableModel<Object> tableModelOf(Block table) {
        int columns = 0;
        for (List<List<Run>> row : table.tableRows) {
            columns = Math.max(columns, row.size());
        }
        int columnCount = columns;
        return new TableModel<Object>() {
            public int getRowCount() {
                return table.tableRows.size() - 1;
            }

            public int getColumnCount() {
                return columnCount;
            }

            public String getColumnName(int column) {
                List<List<Run>> headings = table.tableRows.get(0);
                StringBuilder name = new StringBuilder();
                if (column < headings.size()) {
                    for (Run run : headings.get(column)) {
                        name.append(run.text);
                    }
                }
                return name.toString();
            }

            public Object getValueAt(int row, int column) {
                List<List<Run>> cells = table.tableRows.get(row + 1);
                return column < cells.size() ? cells.get(column) : new ArrayList<Run>();
            }
        };
    }

    // ---- wrapping

    /** runs as lines ending before column end; the first starts at firstCol, the rest at nextCol. forced: every plain word in this colour */
    static List<List<Piece>> wrapped(List<Run> runs, int end, int firstCol, int nextCol, Color forced) {
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
            case LINK:   return Colors.UCya;
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
