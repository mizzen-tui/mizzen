package mizzen.components.input;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.input.tree.DefaultTreeNode;
import mizzen.components.input.tree.TreeView;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.InputBytesEventParser;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * setValue(null) means "nothing" for every input - empty, off, nothing selected - and never throws;
 * a value an input can't show still fails at once.
 */
public class NullValueTest extends TestCase {

    @SuppressWarnings("unchecked")
    private static final DisplayableValue<String>[] ITEMS = new DisplayableValue[] {
            new DisplayableValue.SimpleDisplayValue<String>("one", "one"), new DisplayableValue.SimpleDisplayValue<String>("two", "two") };

    public void testNullIsNothingForEveryInput() {
        TextInput text = new TextInput(Colors.IYel, 10);
        text.setValue(null);
        assertEquals("", text.getValue());

        CheckBoxInput box = new CheckBoxInput(Colors.IYel);
        box.setValue(true);
        box.setValue(null);
        assertEquals(Boolean.FALSE, box.getValue());

        Button toggle = new Button("b", true);
        toggle.setValue(null);
        assertEquals(Boolean.FALSE, toggle.getValue());

        ListBox<String> list = new ListBox<String>(ITEMS);
        list.setValue("two");
        list.setValue(null);
        assertNull(list.getValue());

        DropdownListBox<String> dropdown = new DropdownListBox<String>(ITEMS);
        dropdown.setValue("one");
        dropdown.setValue(null);
        assertNull(dropdown.getValue());

        IntegerInputWithIncrDecrButtons number = new IntegerInputWithIncrDecrButtons(Colors.IYel, "", "-", "+");
        number.setValue(5);
        number.setValue(null);
        assertNull("nothing, not a made-up 0", number.getValue());
    }

    public void testAValueAnInputCantShowStillFailsAtOnce() {
        try {
            new ListBox<String>(ITEMS).setValue("three");
            fail("a value not among the items");
        } catch (IllegalArgumentException expected) {
        }
        TreeView<DefaultTreeNode<String>> tree = new TreeView<DefaultTreeNode<String>>(DefaultTreeNode.of("root").asModel());
        try {
            tree.setValue(new DefaultTreeNode<String>("elsewhere"));
            fail("a node not in the tree");
        } catch (IllegalArgumentException expected) {
        }
    }

    @SuppressWarnings("unchecked")
    public void testATreeWithNothingSelectedSelectsTheFirstRowOnAKey() {
        DefaultTreeNode<String> root = DefaultTreeNode.of("root", new DefaultTreeNode<String>("leaf"));
        TreeView<DefaultTreeNode<String>> tree = new TreeView<DefaultTreeNode<String>>(root.asModel());
        tree.setValue(null);
        assertNull(tree.getValue());
        SyncEventLoop bus = new SyncEventLoop();
        CompositeWithFocusManager top = new CompositeWithFocusManager();
        top.add(tree);
        bus.setRoot(top);
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(tree)));
        new InputBytesEventParser().parseInput("\u001b[B".getBytes(), 3, bus);   // DOWN
        bus.drain();
        assertSame("the first row", root, tree.getValue());
    }
}
