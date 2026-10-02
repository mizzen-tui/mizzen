package dtb.terminalIO.components.input;

import java.io.UnsupportedEncodingException;

import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.supportfortesting.CanvasForTesting;
import dtb.terminalIO.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Characterisation tests for DropdownListBox focus behaviour: opening on focus, the focus
 * hand-off into its popup list (unFocus(newFocusable) keeps it open), and TAB / SHIFT-TAB from
 * the open list (interim fix, see DropdownListBox). Records TODAY's behaviour; a failure means
 * "behaviour changed" - decide whether that is wanted, then update the expectation.
 */
public class DropdownListBoxCharacterisationTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private CompositeWithFocusManager root;
    private TextInput before, after;
    private DropdownListBox<String> dropdown;

    @Override
    protected void setUp() throws UnsupportedEncodingException {
        // root[before, dropdown, after], attached to a canvas (the popup list is positioned on it)
        root = new CompositeWithFocusManager();
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        before = field("before");
        after = field("after");
        dropdown = DropdownListBox.fromStrings(new String[] { "A", "B", "C" });
        dropdown.setName("dropdown");
        root.add(1, 1, before);
        root.add(2, 1, dropdown);
        root.add(10, 1, after);
        bus.setRoot(root);
    }

    private static TextInput field(String name) {
        TextInput field = new TextInput(Colors.IYel, 10);
        field.setName(name);
        return field;
    }

    private void click(Object component) {
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(component)));
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    private String focusedName() {
        String name = String.valueOf(HasName.UTIL.getName(FocusManager.UTIL.findCurrentFocus(root)));
        if (name.contains("listBox")) return "list";
        if (name.startsWith("TextInput[")) return name.substring("TextInput[".length(), name.indexOf(']'));
        return name;
    }

    /** TAB onto the dropdown opens its list and moves keyboard focus into the list */
    public void testTabOntoDropdownOpensListAndFocusesIt() {
        click(before);

        press(Keys.TAB_KEYSTROKE);

        assertTrue("list should be open", dropdown.listBoxVisible());
        assertEquals("list", focusedName());
    }

    /**
     * Hand-off: when focus moves from the dropdown into its own list, the dropdown's
     * unFocus(newFocusable) sees the list and keeps it open.
     */
    public void testListStaysOpenWhenFocusMovesIntoIt() {
        click(before);

        press(Keys.TAB_KEYSTROKE);

        assertEquals("list", focusedName());
        assertTrue(dropdown.listBoxVisible());
    }

    /** clicking another component while the list is open moves focus there and closes the list */
    public void testClickElsewhereWhileListOpenClosesList() {
        click(before);
        press(Keys.TAB_KEYSTROKE);
        boolean openBefore = dropdown.listBoxVisible();

        click(after);

        assertTrue(openBefore);
        assertEquals("after", focusedName());
        assertFalse("list should close when focus goes elsewhere", dropdown.listBoxVisible());
    }

    /** TAB in the open list: keeps the selection, closes the list, focus moves on from the dropdown */
    public void testTabInOpenListMovesToNextAndKeepsSelection() {
        click(before);
        press(Keys.TAB_KEYSTROKE);              // open, focus in list
        press(Keys.DOWN_KEYSTROKE);             // highlight next item (A -> B)

        press(Keys.TAB_KEYSTROKE);

        assertEquals("after", focusedName());
        assertFalse(dropdown.listBoxVisible());
        assertEquals("B", dropdown.getValue());
    }

    /** SHIFT-TAB in the open list: closes it, focus moves back from the dropdown */
    public void testShiftTabInOpenListMovesToPrevious() {
        click(before);
        press(Keys.TAB_KEYSTROKE);

        press(Keys.SHIFT_TAB_KEYSTROKE);

        assertEquals("before", focusedName());
        assertFalse(dropdown.listBoxVisible());
    }

    /** TODAY: ENTER in the list commits the selection but leaves the list open */
    public void testEnterInList_today_commitsButLeavesListOpen() {
        click(before);
        press(Keys.TAB_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);             // highlight next item (A -> B)

        press(Keys.ENTER_KEYSTROKE);

        assertEquals("B", dropdown.getValue());
        assertTrue("today the list stays open after ENTER", dropdown.listBoxVisible());
    }


    /** focus on the dropdown's button with the list closed */
    private void focusButtonWithListClosed() {
        click(before);
        press(Keys.TAB_KEYSTROKE);              // open, focus in list
        press(Keys.ESC_KEYSTROKE);              // focus back to the button (list stays open)
        dropdown.hideListBox();
    }

    /** DOWN on the button with the list closed opens the list (the dropdown's own handler); focus stays on the button */
    public void testDownWithListClosedOpensList() {
        focusButtonWithListClosed();
        boolean openBefore = dropdown.listBoxVisible();

        press(Keys.DOWN_KEYSTROKE);

        assertFalse(openBefore);
        assertTrue("DOWN should open the list", dropdown.listBoxVisible());
        assertEquals("dropdown-button", focusedName());
    }

    /** UP / DOWN on the button with the list already open: nothing happens (the list is not inside the dropdown's focus manager) */
    public void testArrowsOnButtonWithListOpenDoNothing() {
        focusButtonWithListClosed();
        press(Keys.DOWN_KEYSTROKE);             // opens the list

        press(Keys.DOWN_KEYSTROKE);
        press(Keys.UP_KEYSTROKE);

        assertEquals("dropdown-button", focusedName());
        assertTrue(dropdown.listBoxVisible());
        assertEquals("A", dropdown.getValue());
    }

    /** UP on the first item of the open list: the list keeps it - focus and selection stay */
    public void testUpAtTopOfListStaysInList() {
        click(before);
        press(Keys.TAB_KEYSTROKE);

        press(Keys.UP_KEYSTROKE);

        assertEquals("list", focusedName());
        assertTrue(dropdown.listBoxVisible());
        assertEquals("A", dropdown.getValue());
    }
}
