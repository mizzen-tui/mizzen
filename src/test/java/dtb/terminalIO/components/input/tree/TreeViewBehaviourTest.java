package dtb.terminalIO.components.input.tree;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.CheckBoxInput;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.supportfortesting.CanvasForTesting;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * TreeView: keys select, expand, collapse and activate; onChange / onExpansion / onActivate hear
 * it; one TAB stop; the selected row shows differently with and without focus.
 * Layout: root[agree (checkbox), tree, after (text field)]; the tree:
 * Shapes{Round{Circle, Ellipse}, Polygons{Square, Triangle}, Text{Hello}}.
 */
public class TreeViewBehaviourTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();   // as TUIApp builds it
    private final CheckBoxInput agree = new CheckBoxInput(Colors.IYel);
    private final TextInput after = new TextInput(Colors.IYel, 10);
    private final DefaultTreeNode<String> circle = DefaultTreeNode.of("Circle");
    private final DefaultTreeNode<String> ellipse = DefaultTreeNode.of("Ellipse");
    private final DefaultTreeNode<String> round = DefaultTreeNode.of("Round", circle, ellipse);
    private final DefaultTreeNode<String> shapes = DefaultTreeNode.of("Shapes", round,
                                                       DefaultTreeNode.of("Polygons", DefaultTreeNode.of("Square"), DefaultTreeNode.of("Triangle")),
                                                       DefaultTreeNode.of("Text", DefaultTreeNode.of("Hello")));
    private final TreeView<DefaultTreeNode<String>> tree = new TreeView<DefaultTreeNode<String>>(shapes.asModel());
    private final List<String> heard = new ArrayList<String>();

    @Override
    protected void setUp() throws Exception {
        agree.setName("agree");
        after.setName("after");
        tree.setName("tree");
        root.add(agree);
        root.add(tree);
        root.add(after);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        bus.setRoot(root);
        tree.onChange((te) -> heard.add("change:" + tree.getValue()));
        tree.onExpansion((node, expanded, eventBus) -> heard.add((expanded ? "expand:" : "collapse:") + node));
        tree.onActivate((node, eventBus) -> heard.add("activate:" + node));
        bus.deliver(new RequestFocusEvent(tree.asFocusable()));
        heard.clear();    // getting focus fires onChange (VALUE_CHANGED_ON_FOCUS, like ListBox)
    }

    private void press(KeyStroke... keys) {
        for (KeyStroke key : keys) {
            bus.deliver(new KeyEvent("test", key));
        }
    }

    private List<String> rows() {
        List<String> rows = new ArrayList<String>();
        for (Component<?> row : tree.getChildren()) {
            rows.add(((TextComponent) row).getText());
        }
        return rows;
    }

    private String focusedName() {
        Focusable focused = FocusManager.UTIL.findCurrentFocus(root);
        return focused == null ? "-" : dtb.terminalIO.components.HasName.UTIL.getName(focused.getFocusComponent());
    }

    public void testStartsWithTheRootExpandedAndSelected() {
        assertEquals("[▾ Shapes,   ├─▸ Round,   ├─▸ Polygons,   └─▸ Text]", rows().toString());
        assertSame(shapes, tree.getValue());
    }

    /** DOWN / UP select; every move fires onChange (the default, like ListBox) */
    public void testArrowsSelect() {
        press(Keys.DOWN_KEYSTROKE, Keys.DOWN_KEYSTROKE);
        assertEquals("Polygons", tree.getValue().getValue());

        press(Keys.UP_KEYSTROKE);
        assertSame(round, tree.getValue());
        assertEquals("[change:Round, change:Polygons, change:Round]", heard.toString());
    }

    /** RIGHT expands, RIGHT again goes to the first child; LEFT goes to the parent, LEFT again collapses */
    public void testRightAndLeft() {
        press(Keys.DOWN_KEYSTROKE, Keys.RIGHT_KEYSTROKE);
        assertEquals("[▾ Shapes,   ├─▾ Round,   │   ├── Circle,   │   └── Ellipse,   ├─▸ Polygons,   └─▸ Text]", rows().toString());

        press(Keys.RIGHT_KEYSTROKE);
        assertSame(circle, tree.getValue());

        press(Keys.LEFT_KEYSTROKE);
        assertSame(round, tree.getValue());

        press(Keys.LEFT_KEYSTROKE);
        assertEquals(4, rows().size());
        assertEquals("[change:Round, expand:Round, change:Circle, change:Round, collapse:Round]", heard.toString());
    }

    /** ENTER on a parent expands / collapses it; on a leaf activates it */
    public void testEnter() {
        press(Keys.DOWN_KEYSTROKE, Keys.ENTER_KEYSTROKE, Keys.DOWN_KEYSTROKE, Keys.ENTER_KEYSTROKE);

        assertSame(circle, tree.getValue());
        assertEquals("[change:Round, expand:Round, change:Round, change:Circle, activate:Circle, change:Circle]", heard.toString());
    }

    /** onChange only on ENTER and clicks: moving with the arrows is quiet */
    public void testTriggerOnlyOnEnter() {
        tree.setEventTriggerForValueChanged(ListBox.VALUE_CHANGED_ON_ENTER_KEY | ListBox.VALUE_CHANGED_ON_MOUSE_CLICK);

        press(Keys.DOWN_KEYSTROKE, Keys.DOWN_KEYSTROKE, Keys.ENTER_KEYSTROKE);

        assertEquals("[expand:Polygons, change:Polygons]", heard.toString());
    }

    /** HOME / END: the first / last row; UP at the top is used up (focus stays) */
    public void testHomeEndAndTheEdges() {
        press(Keys.END_KEYSTROKE);
        assertEquals("Text", tree.getValue().getValue());

        press(Keys.HOME_KEYSTROKE, Keys.UP_KEYSTROKE);
        assertSame(shapes, tree.getValue());
        assertEquals("tree", focusedName());
    }

    /** collapsing a node with the selection inside moves the selection to that node */
    public void testCollapseMovesTheSelectionUp() {
        tree.setValue(ellipse);
        assertTrue(tree.isExpanded(round));

        tree.collapse(round);

        assertSame(round, tree.getValue());
    }

    /** one TAB stop: TAB leaves it, SHIFT-TAB comes back with the same selection */
    public void testOneTabStop() {
        press(Keys.DOWN_KEYSTROKE);

        press(Keys.TAB_KEYSTROKE);
        assertEquals("after", focusedName());

        press(Keys.SHIFT_TAB_KEYSTROKE);
        assertEquals("tree", focusedName());
        assertSame(round, tree.getValue());
    }

    /** keys the tree doesn't use go on (the root doesn't take them for the tree) */
    public void testOtherKeysAreNotTaken() {
        assertNull(root.canHandle(new KeyEvent("test", new KeyStroke('x'))));
        assertNotNull(root.canHandle(new KeyEvent("test", Keys.DOWN_KEYSTROKE)));
    }

    /** without focus the selected row shows in the unfocused style */
    public void testUnfocusedSelectionStyles() {
        press(Keys.DOWN_KEYSTROKE);
        tree.setUnfocusedSelection(TreeView.UnfocusedSelection.BRACKETS);
        assertEquals("  ├─▸ Round", rows().get(1));          // focused: no brackets

        press(Keys.TAB_KEYSTROKE);
        assertEquals("  ├─▸ [Round]", rows().get(1));

        tree.setUnfocusedSelection(TreeView.UnfocusedSelection.ARROW);
        assertEquals(">   ├─▸ Round", rows().get(1));
    }

    public void testAsciiStyle() {
        tree.asciiStyle().expandAll();

        assertEquals("[v Shapes,   |-v Round,   |   |-- Circle,   |   `-- Ellipse,   |-v Polygons,   |   |-- Square,"
                   + "   |   `-- Triangle,   `-v Text,       `-- Hello]", rows().toString());
    }
}
