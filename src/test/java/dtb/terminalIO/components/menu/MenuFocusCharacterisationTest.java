package dtb.terminalIO.components.menu;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.input.TextInput;
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
 * Characterisation tests for menus: a MenuBar's menus pop up as lists on the ROOT panel (so they
 * are never clipped by a container), yet belong to their menu button for focus. Records TODAY's
 * behaviour so focus redesign step 7 (popups owned by their component) cannot change it silently.
 * Layout: root[menu bar [File, Edit], field]; File = [New > [Car, Boat], Open], Edit = [Find].
 */
public class MenuFocusCharacterisationTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final List<String> picked = new ArrayList<>();
    private CompositeWithFocusManager root;
    private MenuBar menuBar;
    private Menu file, newMenu, edit;
    private TextInput field;

    @Override
    protected void setUp() throws UnsupportedEncodingException {
        root = new CompositeWithFocusManager();
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);

        newMenu = new Menu("New").addItem("Car", (te) -> picked.add("Car")).addItem("Boat", (te) -> picked.add("Boat"));
        file = new Menu("File").addItem("New", newMenu).addItem("Open", (te) -> picked.add("Open"));
        edit = new Menu("Edit").addItem("Find", (te) -> picked.add("Find"));
        menuBar = new MenuBar(Colors.IBlu);
        menuBar.addDropDownMenu("File", file);
        menuBar.addDropDownMenu("Edit", edit);

        field = new TextInput(Colors.IYel, 10);
        field.setName("field");
        root.add(0, 1, menuBar);
        root.add(10, 1, field);
        bus.setRoot(root);
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(field)));
    }

    private void press(KeyStroke key) {
        bus.deliver(new KeyEvent("test", key));
    }

    private void click(Object component) {
        bus.deliver(new RequestFocusEvent(ToFocusable.UTIL.toFocusable(component)));
    }

    private Object fileButton() {
        return menuBar.menuBarItems.get(0);
    }

    private String focusedName() {
        Object f = FocusManager.UTIL.findCurrentFocus(root);
        return f == null ? "-" : String.valueOf(HasName.UTIL.getName(f));
    }

    private String open() {
        return (file.isVisible() ? "File " : "") + (newMenu.isVisible() ? "New " : "") + (edit.isVisible() ? "Edit " : "");
    }

    /** open the File menu the keyboard way: focus its button, ENTER */
    private void openFile() {
        click(fileButton());
        press(Keys.ENTER_KEYSTROKE);
    }

    public void testEnterOnMenuButtonOpensMenu() {
        openFile();

        assertEquals("File ", open());
        assertEquals("MenuBarButton[File]", focusedName());
    }

    /** DOWN goes into the menu; the submenu under the selected item opens by itself */
    public void testDownGoesIntoMenuAndOpensSubmenuOfSelectedItem() {
        openFile();

        press(Keys.DOWN_KEYSTROKE);

        assertEquals("File New ", open());
        assertTrue(focusedName(), focusedName().contains("File-ListBox"));
    }

    /** ENTER on an item runs it, closes the menus, and focus goes back to the menu's button (as on master) */
    public void testEnterOnItemRunsItClosesMenusAndFocusReturnsToButton() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);              // New -> Open

        press(Keys.ENTER_KEYSTROKE);

        assertEquals("[Open]", picked.toString());
        assertEquals("", open());
        assertEquals("MenuBarButton[File]", focusedName());
    }

    public void testRightOnMenuButtonOpensNextMenu() {
        openFile();

        press(Keys.RIGHT_KEYSTROKE);

        assertEquals("Edit ", open());
        assertEquals("MenuBarButton[Edit]", focusedName());
    }

    /** RIGHT inside an open menu opens the next menu, focus on its button (as on master) */
    public void testRightInsideMenuOpensNextMenu() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);

        press(Keys.RIGHT_KEYSTROKE);

        assertEquals("Edit ", open());
        assertEquals("MenuBarButton[Edit]", focusedName());
    }

    /** TODAY: ESC inside an open menu does nothing */
    public void testEscInsideMenu_today_doesNothing() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);

        press(Keys.ESC_KEYSTROKE);

        assertEquals("File ", open());
        assertTrue(focusedName(), focusedName().contains("File-ListBox"));
    }

    /**
     * TODAY: TAB inside an open menu goes to the File button (the menu's list sits last on the root
     * panel, so TAB wraps around to the menu bar); the menu stays open.
     */
    public void testTabInsideMenu_today_wrapsToMenuButton() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);

        press(Keys.TAB_KEYSTROKE);

        assertEquals("File ", open());
        assertEquals("MenuBarButton[File]", focusedName());
    }

    /** TODAY: TAB inside a menu whose submenu is open goes into the submenu (it sits next on the root panel) */
    public void testTabInsideMenuWithSubmenuOpen_today_goesIntoSubmenu() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);              // New selected, its submenu open

        press(Keys.TAB_KEYSTROKE);

        assertEquals("File New ", open());
        assertTrue(focusedName(), focusedName().contains("New-ListBox"));
    }

    /** SHIFT-TAB inside an open menu closes it and goes back to the field before it in the root's order */
    public void testShiftTabInsideMenuClosesItAndMovesBack() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);

        press(Keys.SHIFT_TAB_KEYSTROKE);

        assertEquals("", open());
        assertEquals("field", focusedName().replaceAll("^TextInput\\[|\\]$", ""));
    }

    public void testClickElsewhereClosesMenus() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);

        click(field);

        assertEquals("", open());
        assertEquals("field", focusedName().replaceAll("^TextInput\\[|\\]$", ""));
    }

    /**
     * Focus moving INTO a menu while nothing has focus (no previous component) must not fail - it
     * did: MenuContainer.focus logged the previous component's name and threw on null.
     */
    public void testFocusIntoMenuFromNothingFocused() {
        openFile();
        file.getMenuContainer().focus(null, bus);

        assertTrue(file.isVisible());
    }
}
