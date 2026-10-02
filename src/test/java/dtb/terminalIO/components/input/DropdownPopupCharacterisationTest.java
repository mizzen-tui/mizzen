package dtb.terminalIO.components.input;

import java.io.UnsupportedEncodingException;

import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.modal.DialogComponent.ButtonSpec;
import dtb.terminalIO.components.modal.DialogComponent.IconStyle;
import dtb.terminalIO.components.modal.MessageBox;
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
 * The dropdown's open list is a popup: it is drawn on the ROOT panel, so it is never clipped by the
 * dropdown's container and never makes that container grow - yet for focus it belongs to the
 * dropdown. Records TODAY's behaviour for focus redesign step 7 (popups owned by their component).
 */
public class DropdownPopupCharacterisationTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private CompositeWithFocusManager root;
    private DropdownListBox<String> dropdown;

    private static TextInput field(String name) {
        TextInput field = new TextInput(Colors.IYel, 10);
        field.setName(name);
        return field;
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    private void click(Object component) {
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(component)));
    }

    private String focusedName() {
        Object f = FocusManager.UTIL.findCurrentFocus(root);
        String name = f == null ? "-" : String.valueOf(HasName.UTIL.getName(f));
        if (name.contains("listBox")) return "list";
        if (name.startsWith("TextInput[")) return name.substring("TextInput[".length(), name.indexOf(']'));
        return name;
    }

    /** root[before, panel[dropdown, inPanel], after] */
    private VerticalPanel dropdownInPanel() throws UnsupportedEncodingException {
        root = new CompositeWithFocusManager();
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        dropdown = DropdownListBox.fromStrings(new String[] { "A", "B", "C" });
        dropdown.setName("dropdown");
        VerticalPanel panel = new VerticalPanel();
        panel.add(dropdown);
        panel.add(field("inPanel"));
        root.add(1, 1, field("before"));
        root.add(3, 1, panel);
        root.add(12, 1, field("after"));
        bus.setRoot(root);
        return panel;
    }

    /** the open list is drawn on the root panel, and the dropdown's container does not grow */
    public void testListOpensOnRootPanelAndContainerKeepsItsSize() throws UnsupportedEncodingException {
        VerticalPanel panel = dropdownInPanel();
        int heightBefore = panel.getHeight();
        click(dropdown);

        assertTrue("list should be open", dropdown.listBoxVisible());
        assertSame("the list is on the root panel", root, dropdown.getListBoxParent());
        assertEquals("the container must not grow", heightBefore, panel.getHeight());
    }

    /** TAB in the open list carries on after the dropdown (its next field in the container), not after the list */
    public void testTabInListCarriesOnAfterTheDropdown() throws UnsupportedEncodingException {
        dropdownInPanel();
        click(dropdown);
        String opened = focusedName();

        press(Keys.TAB_KEYSTROKE);

        assertEquals("list", opened);
        assertEquals("inPanel", focusedName());
        assertFalse(dropdown.listBoxVisible());
    }

    /**
     * A dropdown inside a modal dialog: the open list should get the keys (DOWN picks the next
     * item). TODAY the list sits on the main panel, outside the dialog - so the dialog's key trap
     * swallows the key and the selection does not change.
     */
    public void testDropdownInDialog_today_listDoesNotGetKeys() throws UnsupportedEncodingException {
        root = new CompositeWithFocusManager();
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        root.add(1, 1, field("outside"));
        bus.setRoot(root);
        dropdown = DropdownListBox.fromStrings(new String[] { "A", "B", "C" });
        dropdown.setName("dropdown");
        ButtonSpec[] buttons = { new ButtonSpec("OK", Keys.F1_KEYSTROKE, (close, te) -> { }, false) };
        MessageBox.simpleDialog(IconStyle.QUESTION, "Pick", buttons, bus, root, "Pick one", dropdown);
        bus.drain();
        click(dropdown);
        boolean open = dropdown.listBoxVisible();
        String valueBefore = String.valueOf(dropdown.getValue());

        press(Keys.DOWN_KEYSTROKE);
        press(Keys.ENTER_KEYSTROKE);

        assertTrue("list should be open", open);
        assertEquals("selection unchanged today", valueBefore, String.valueOf(dropdown.getValue()));
    }
}
