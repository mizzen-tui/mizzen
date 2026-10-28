package mizzen.components.input.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import mizzen.components.BlockChars;
import mizzen.components.BoundedComponent;
import mizzen.components.Component;
import mizzen.components.GetComponentAtOffset;
import mizzen.components.HasName;
import mizzen.components.ParentContainer;
import mizzen.components.TextComponent;
import mizzen.components.input.HasValue;
import mizzen.components.input.LeafFocusBehaviour;
import mizzen.components.input.FormInput;
import mizzen.components.input.OnChangeListeners;
import mizzen.components.input.SettableHasValueComponentMixen;
import mizzen.components.input.UnfocusedSelection;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.Registration;
import mizzen.events.RequestScrollToView;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.AsKeyboardHandler;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.KeyboardHandler;
import mizzen.events.keyboard.Keys;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.events.mouse.Clickable;
import mizzen.events.mouse.MOUSE_ACTION;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;

/**
 * A tree of nodes you can expand, collapse and select - one TAB stop that draws its own rows.
 * <pre>
 *     ▾ Shapes
 *       ├─▾ Round
 *       │   ├── Circle        the selected row is hilited while the tree has focus
 *       │   └── Ellipse
 *       └─▸ Polygons          ▸ collapsed, ▾ expanded (asciiStyle: &gt; and v)
 * </pre>
 * Keys: UP / DOWN select the row above / below; RIGHT expands (or goes to the first child); LEFT
 * collapses (or goes to the parent); HOME / END the first / last row; ENTER expands / collapses, or
 * activates a leaf. Mouse: a click selects a row; a click on ▸ / ▾ expands / collapses; a click on
 * the selected row activates it (a leaf) or expands / collapses it.
 * <p/>
 * The nodes come from a TreeViewModel (only asked for a node's children when it is expanded); for
 * a tree built by hand see DefaultTreeNode. nodeText sets the text shown for a node.
 * <p/>
 * The selected node is the value (settable, so forms can bind it). onChange hears it change - by
 * default on every move (like ListBox); setEventTriggerForValueChanged(HasValue.VALUE_CHANGED_ON_...)
 * picks which moves count, e.g. only ENTER and clicks. onExpansion hears nodes expand / collapse;
 * onActivate hears ENTER or a click on the selected leaf (e.g. "open this file").
 * <p/>
 * While the tree doesn't have focus its selected row shows in the setUnfocusedSelection style
 * (default DIM: the selected colour, not inverted).
 */
public class TreeView<N> extends BoundedComponent<TreeView<N>>
                         implements SettableHasValueComponentMixen<TreeView<N>, N>, FormInput<N>, ToFocusable, AsKeyboardHandler,
                                    Clickable, ParentContainer, GetComponentAtOffset {

    /** a node was expanded (true) or collapsed (false) by the user */
    public interface ExpansionListener<N> {
        void expansionChanged(N node, boolean expanded, EventBus eventBus);
    }

    /** one visible row: its node, where it sits, and what it shows */
    private class Row {
        final N node;
        final Row parent;
        final boolean expandable;
        final int markCol;          // the column of its ▸ / ▾ (expandable rows only)
        final String text;

        Row(N node, Row parent, boolean expandable, int markCol, String text) {
            this.node = node;
            this.parent = parent;
            this.expandable = expandable;
            this.markCol = markCol;
            this.text = text;
        }
    }

    private TreeViewModel<N> model;
    private Function<N, String> nodeText;
    private final Set<N> expanded = new HashSet<N>();
    private final List<Row> rows = new ArrayList<Row>();
    private final List<TextComponent> rowComponents = new ArrayList<TextComponent>();
    private N selected;

    private Color selectedColor = new Colors.FGBGColors(Colors.IYel);     // as ListBox
    private UnfocusedSelection unfocusedSelection = UnfocusedSelection.DIM;
    private String collapsedMark = "▸";
    private String expandedMark = "▾";
    private boolean ascii = false;
    private int eventTriggerForValueChanged = HasValue.DEFAULT_TRIGGER_FOR_ONVALUE_CHANGED;

    private final OnChangeListeners<N> changeListeners = new OnChangeListeners<N>(this);   // onChange: any number of listeners
    private ExpansionListener<N> onExpansion = (node, isExpanded, bus) -> { };
    private BiConsumer<N, EventBus> onActivate = (node, bus) -> { };

    private final LeafFocusBehaviour focus = LeafFocusBehaviour.of(this)
            .look(LeafFocusBehaviour.Look.NONE)                 // the selected row shows focus
            .showOnFocus(this::selectedRowRectangle)
            .handleKeys(new TreeKeys())
            .whenFocusArrives(this::focusGained)
            .whenFocusLeaves(this::repaint)
            .handleEvents(new ConditionalEventHandler.Impl(
                    (te) -> changeListeners.canHandle(te) != null,
                    (te) -> changeListeners.handle(te)));

    public TreeView(TreeViewModel<N> model) {
        this(Colors.DEFAULT_COLOR, new Dimension.Simple(1, 1));
        setModel(model);
    }

    public TreeView(Color color, Dimension d, Function<N, String> nodeText) {
        super(color, d);
        this.nodeText = nodeText;
    }

    public TreeView(Color color, Dimension d) {
        this(color, d, (node) -> String.valueOf(node));
    }

    /** the nodes to show; the root starts expanded and selected */
    public void setModel(TreeViewModel<N> model) {
        this.model = model;
        this.expanded.clear();
        N root = model.getRoot();
        if (root != null) {
            expanded.add(root);
        }
        this.selected = root;
        rebuild();
    }

    // ---- looks ------------------------------------------------------------------------------

    /** the text shown for a node (default: String.valueOf) */
    public TreeView<N> nodeText(Function<N, String> nodeText) {
        this.nodeText = nodeText;
        rebuild();
        return this;
    }

    /** the colour of the selected row (inverted while the tree has focus) */
    public TreeView<N> setSelectedColor(Color selectedColor) {
        this.selectedColor = selectedColor;
        rebuild();
        return this;
    }

    public TreeView<N> setUnfocusedSelection(UnfocusedSelection unfocusedSelection) {
        this.unfocusedSelection = unfocusedSelection;
        rebuild();
        return this;
    }

    /** the marks before a collapsed / expanded node, e.g. "▸" and "▾" */
    public TreeView<N> setExpanders(String collapsed, String expanded) {
        this.collapsedMark = collapsed;
        this.expandedMark = expanded;
        rebuild();
        return this;
    }

    /** plain ASCII: "&gt;" / "v" marks and |- `- branch lines, for terminals without box drawing characters */
    public TreeView<N> asciiStyle() {
        this.ascii = true;
        return setExpanders(">", "v");
    }

    @Override
    public TreeView<N> setColor(Color color) {
        super.setColor(color);
        if (rows != null) {          // null while the super constructor sets the first color
            rebuild();
        }
        return this;
    }

    // ---- expansion --------------------------------------------------------------------------

    /** expand node (by code: onExpansion is not told) */
    public TreeView<N> expand(N node) {
        if (model != null && !model.isLeaf(node) && expanded.add(node)) {
            rebuild();
        }
        return this;
    }

    /** collapse node (by code: onExpansion is not told); a selection inside it moves to node */
    public TreeView<N> collapse(N node) {
        if (expanded.remove(node)) {
            if (isInside(selected, node)) {
                selected = node;
            }
            rebuild();
        }
        return this;
    }

    /** expand every node - asks the model for the whole tree */
    public TreeView<N> expandAll() {
        if (model != null && model.getRoot() != null) {
            expandAllBelow(model.getRoot());
            rebuild();
        }
        return this;
    }

    public boolean isExpanded(N node) {
        return expanded.contains(node);
    }

    // ---- value: the selected node -----------------------------------------------------------

    @Override
    public N getValue() {
        return selected;
    }

    /** select node, expanding the nodes above it */
    @Override
    /** node selected (its ancestors expanded); null: nothing selected. A node not in the tree fails at once */
    public void setValue(N node) {
        if (node == null) {
            selected = null;
            rebuild();
            return;
        }
        List<N> path = pathTo(node);
        if (path == null) {
            throw new IllegalArgumentException("TreeView(" + HasName.UTIL.getName(this) + ").setValue(" + node + "): not in the tree");
        }
        expanded.addAll(path.subList(0, path.size() - 1));
        selected = node;
        rebuild();
    }

    public Registration onChange(EventHandler handler) {
        return changeListeners.add(handler);
    }

    /** which selection moves fire onChange - HasValue.VALUE_CHANGED_ON_... flags (default: all) */
    public TreeView<N> setEventTriggerForValueChanged(int eventTriggerForValueChanged) {
        this.eventTriggerForValueChanged = eventTriggerForValueChanged;
        return this;
    }

    public TreeView<N> onExpansion(ExpansionListener<N> onExpansion) {
        this.onExpansion = onExpansion;
        return this;
    }

    public TreeView<N> onActivate(BiConsumer<N, EventBus> onActivate) {
        this.onActivate = onActivate;
        return this;
    }

    // ---- focus, keys, mouse -----------------------------------------------------------------

    @Override
    public Focusable asFocusable() {
        return focus.asFocusable();
    }

    @Override
    public KeyboardHandler asKeyboardHandler() {
        return focus.asKeyboardHandler();
    }

    /** the tree's keys - see the class comment. It wants only these, so other keys go on to its parents. */
    private class TreeKeys implements KeyboardHandler {
        private final KeyStroke[] mine = { Keys.UP_KEYSTROKE, Keys.DOWN_KEYSTROKE, Keys.LEFT_KEYSTROKE, Keys.RIGHT_KEYSTROKE,
                                           Keys.HOME_KEYSTROKE, Keys.END_KEYSTROKE, Keys.ENTER_KEYSTROKE };

        @Override
        public boolean wantsKey(KeyEvent ke) {
            if (rows.isEmpty()) {
                return false;
            }
            for (KeyStroke key : mine) {
                if (ke.isKeyStroke(key)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean onKey(KeyEvent ke) {
            if (!wantsKey(ke)) {
                return false;
            }
            EventBus bus = ke.getEventBus();
            int index = selectedIndex();
            if (index < 0) {                             // nothing selected: any key of the tree's selects the first row
                select(0, HasValue.VALUE_CHANGED_ON_NEXT_KEYSTROKE, bus);
                return true;
            }
            Row row = rows.get(index);
            if (ke.isKeyStroke(Keys.UP_KEYSTROKE)) {
                select(index - 1, HasValue.VALUE_CHANGED_ON_PREV_KEYSTROKE, bus);
            } else if (ke.isKeyStroke(Keys.DOWN_KEYSTROKE)) {
                select(index + 1, HasValue.VALUE_CHANGED_ON_NEXT_KEYSTROKE, bus);
            } else if (ke.isKeyStroke(Keys.HOME_KEYSTROKE)) {
                select(0, HasValue.VALUE_CHANGED_ON_PREV_KEYSTROKE, bus);
            } else if (ke.isKeyStroke(Keys.END_KEYSTROKE)) {
                select(rows.size() - 1, HasValue.VALUE_CHANGED_ON_NEXT_KEYSTROKE, bus);
            } else if (ke.isKeyStroke(Keys.RIGHT_KEYSTROKE)) {
                if (row.expandable && !expanded.contains(row.node)) {
                    setExpanded(row.node, true, bus);
                } else if (row.expandable) {
                    select(index + 1, HasValue.VALUE_CHANGED_ON_NEXT_KEYSTROKE, bus);     // its first child
                }
            } else if (ke.isKeyStroke(Keys.LEFT_KEYSTROKE)) {
                if (row.expandable && expanded.contains(row.node)) {
                    setExpanded(row.node, false, bus);
                } else if (row.parent != null) {
                    select(rows.indexOf(row.parent), HasValue.VALUE_CHANGED_ON_PREV_KEYSTROKE, bus);
                }
            } else if (ke.isKeyStroke(Keys.ENTER_KEYSTROKE)) {
                enter(row, HasValue.VALUE_CHANGED_ON_ENTER_KEY, bus);
            }
            return true;     // the tree's keys are used up even at the top / bottom row
        }
    }

    /**
     * A click: on a row's ▸ / ▾ expands / collapses it; on another row selects it; on the selected
     * row activates it (a leaf) or expands / collapses it. Position: relative to the tree.
     */
    @Override
    public void onClick(ApplicationMouseEvent appMouseEvent) {
        if (!MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction())) {
            return;
        }
        EventBus bus = appMouseEvent.getEventBus();
        focus.requestFocus(bus);
        Position at = appMouseEvent.getMousePosition();
        if (at.getRow() < 0 || at.getRow() >= rows.size()) {
            return;
        }
        Row row = rows.get(at.getRow());
        if (row.expandable && at.getCol() == row.markCol) {
            setExpanded(row.node, !expanded.contains(row.node), bus);
        } else if (row.node == selected) {
            enter(row, HasValue.VALUE_CHANGED_ON_MOUSE_CLICK, bus);
        } else {
            select(at.getRow(), HasValue.VALUE_CHANGED_ON_MOUSE_CLICK, bus);
        }
    }

    @Override
    public void onClick(EventBus eventBus) {
        focus.requestFocus(eventBus);
    }

    // ---- inside: selection, expansion, events -----------------------------------------------

    /** select the row at index (if there is one); onChange is told if trigger is one of the chosen triggers */
    private void select(int index, int trigger, EventBus bus) {
        if (index < 0 || index >= rows.size() || rows.get(index).node == selected) {
            return;
        }
        selected = rows.get(index).node;
        rebuild();
        bus.raiseEvent(new RequestScrollToView(this, this, selectedRowRectangle()));
        fireValueChangedIf(trigger, bus);
        repaint(bus);
    }

    /** ENTER (or a click on the selected row): a parent expands / collapses, a leaf is activated */
    private void enter(Row row, int trigger, EventBus bus) {
        if (row.expandable) {
            setExpanded(row.node, !expanded.contains(row.node), bus);
        } else {
            onActivate.accept(row.node, bus);
        }
        fireValueChangedIf(trigger, bus);
    }

    /** expand / collapse by the user: onExpansion is told; a selection inside a collapsed node moves to it */
    private void setExpanded(N node, boolean expand, EventBus bus) {
        N selectedBefore = selected;
        if (expand) {
            expand(node);
        } else {
            collapse(node);
        }
        onExpansion.expansionChanged(node, expand, bus);
        if (selected != selectedBefore) {
            fireValueChangedIf(HasValue.VALUE_CHANGED_ON_PREV_KEYSTROKE, bus);
        }
        repaint(bus);
    }

    private void fireValueChangedIf(int trigger, EventBus bus) {
        if ((eventTriggerForValueChanged & trigger) != 0) {
            bus.raiseEvent(new HasValue.ValueChangedEvent<N>(this));
        }
    }

    private void focusGained(EventBus bus) {
        fireValueChangedIf(HasValue.VALUE_CHANGED_ON_FOCUS, bus);
        repaint(bus);
    }

    private void repaint(EventBus bus) {
        rebuild();
        RequestUpdateCanvasEvent.invoke(bus, this);
    }

    // ---- inside: rows -----------------------------------------------------------------------

    /** work out the visible rows again and redraw them (selected row in its colours) */
    private void rebuild() {
        rows.clear();
        rowComponents.clear();
        if (model != null && model.getRoot() != null) {
            addRows(model.getRoot(), null, "", true, true);
        }
        if (selected != null && selectedIndex() < 0) {
            selected = rows.isEmpty() ? null : rows.get(0).node;
        }
        int width = 1;
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            boolean isSelected = row.node == selected;
            String text = isSelected ? selectedText(row) : row.text;
            Color color = isSelected ? selectedRowColor() : getColor();
            int rowIndex = i;
            TextComponent component = makeChildComponent(() -> new TextComponent(color, rowIndex, 0, text));
            rowComponents.add(component);
            width = Math.max(width, component.getWidth());
        }
        resize(new Dimension.Simple(Math.max(1, rows.size()), width));
        markAsDirty();
    }

    /**
     * Add node's row (and, if it is expanded, its children's rows). prefix: the branch lines of the
     * levels above it; last: it is its parent's last child.
     */
    private void addRows(N node, Row parent, String prefix, boolean last, boolean isRoot) {
        boolean expandable = !model.isLeaf(node);
        String branch = isRoot ? "" : prefix + (last ? lastBranch() : teeBranch());
        String mark = expandable ? (expanded.contains(node) ? expandedMark : collapsedMark) : (isRoot ? "" : horizontal());
        String text = branch + mark + (mark.isEmpty() ? "" : " ") + nodeText.apply(node);
        Row row = new Row(node, parent, expandable, branch.length(), text);
        rows.add(row);
        if (!expandable || !expanded.contains(node)) {
            return;
        }
        String childPrefix = isRoot ? "  " : prefix + (last ? "    " : vertical() + "   ");
        int count = model.getChildCount(node);
        for (int i = 0; i < count; i++) {
            addRows(model.getChild(node, i), row, childPrefix, i == count - 1, false);
        }
    }

    private String selectedText(Row row) {
        if (focus.hasFocus()) {
            return row.text;
        }
        switch (unfocusedSelection) {
            case BRACKETS: return bracketed(row);
            case ARROW:    return "> " + row.text;
            default:       return row.text;
        }
    }

    /** "├── [Circle]": the brackets around the node's text, not its branch lines */
    private String bracketed(Row row) {
        String shown = nodeText.apply(row.node);
        int at = row.text.lastIndexOf(shown);
        return at < 0 ? "[" + row.text + "]" : row.text.substring(0, at) + "[" + shown + "]";
    }

    private Color selectedRowColor() {
        if (focus.hasFocus()) {
            return selectedColor.invert();
        }
        return unfocusedSelection == UnfocusedSelection.DIM ? selectedColor : getColor();
    }

    private int selectedIndex() {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).node == selected) {
                return i;
            }
        }
        return -1;
    }

    /** the selected row, relative to the tree - what to scroll into view */
    private Rectangle selectedRowRectangle() {
        int index = Math.max(0, selectedIndex());
        return new Rectangle.Simple(index, 0, 1, Math.max(1, getWidth()));
    }

    /** is node inside ancestor (or ancestor itself)? - by the visible rows */
    private boolean isInside(N node, N ancestor) {
        for (Row row = rowOf(node); row != null; row = row.parent) {
            if (row.node == ancestor) {
                return true;
            }
        }
        return false;
    }

    private Row rowOf(N node) {
        for (Row row : rows) {
            if (row.node == node) {
                return row;
            }
        }
        return null;
    }

    private void expandAllBelow(N node) {
        if (model.isLeaf(node)) {
            return;
        }
        expanded.add(node);
        for (int i = 0; i < model.getChildCount(node); i++) {
            expandAllBelow(model.getChild(node, i));
        }
    }

    /** the nodes from the root down to node (searching the whole model); null if node isn't in it */
    private List<N> pathTo(N node) {
        if (model == null || model.getRoot() == null) {
            return null;
        }
        List<N> path = new ArrayList<N>();
        return findPath(model.getRoot(), node, path) ? path : null;
    }

    private boolean findPath(N at, N node, List<N> path) {
        path.add(at);
        if (at == node || at.equals(node)) {
            return true;
        }
        for (int i = 0; i < model.getChildCount(at); i++) {
            if (findPath(model.getChild(at, i), node, path)) {
                return true;
            }
        }
        path.remove(path.size() - 1);
        return false;
    }

    private String teeBranch()   { return ascii ? "|-" : "" + blockChars().getVERTICAL_RIGHT() + blockChars().getHORIZONTAL_LINE(); }
    private String lastBranch()  { return ascii ? "`-" : "" + blockChars().getBOTTOM_LEFT() + blockChars().getHORIZONTAL_LINE(); }
    private String horizontal()  { return ascii ? "-"  : "" + blockChars().getHORIZONTAL_LINE(); }
    private String vertical()    { return ascii ? "|"  : "" + blockChars().getVERTICAL_LINE(); }

    private BlockChars blockChars() {
        return getBlockChars();
    }

    // ---- drawing and children (the row components) ------------------------------------------

    @Override
    public void draw(Color color, IScreenLineSegmentsBuffer segments) {
        for (Component<?> row : rowComponents) {
            segments.merge(row.getSegmentsBuffer());
        }
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return rowComponents;
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        return child instanceof BoundedComponent ? ((BoundedComponent<?>) child).getPostion_TopLeft() : null;
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        return this.containsPosition(offset) ? this : null;
    }
}
