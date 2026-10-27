package mizzen.components.menu;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.HasName;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.focus.ToFocusable;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * Characterisation tests for menus: a MenuBar's menus pop up as lists on the ROOT panel (so they
 * are never clipped by a container), yet belong to their menu button for focus. Records TODAY's
 * behaviour so focus redesign step 7 (popups owned by their component) cannot change it silently.
 * Layout: root[menu bar [File, Edit], field]; File = [New > [Car, Boat, More > [Plane, Train]], Open],
 * Edit = [Find].
 */
public class MenuFocusCharacterisationTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final List<String> picked = new ArrayList<>();
    private CompositeWithFocusManager root;
    private MenuBar menuBar;
    private Menu file, newMenu, more, edit;
    private TextInput field;

    @Override
    protected void setUp() throws UnsupportedEncodingException {
        root = new CompositeWithFocusManager();
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);

        more = new Menu("More").addItem("Plane", (te) -> picked.add("Plane")).addItem("Train", (te) -> picked.add("Train"));
        newMenu = new Menu("New").addItem("Car", (te) -> picked.add("Car")).addItem("Boat", (te) -> picked.add("Boat"))
                                 .addItem("More", more);
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
        return (file.isVisible() ? "File " : "") + (newMenu.isVisible() ? "New " : "") + (more.isVisible() ? "More " : "")
                + (edit.isVisible() ? "Edit " : "");
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

    /** ESC inside a submenu closes the whole menu (all levels); focus goes back to its menu button */
    public void testEscInsideSubmenuClosesTheWholeMenu() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);              // New selected, its submenu open
        press(Keys.TAB_KEYSTROKE);               // into the submenu

        press(Keys.ESC_KEYSTROKE);

        assertEquals("", open());
        assertEquals("MenuBarButton[File]", focusedName());
    }

    /** ESC inside an open menu closes it; focus goes back to its menu button. (Before: ESC did nothing.) */
    public void testEscInsideMenuClosesIt() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);

        press(Keys.ESC_KEYSTROKE);

        assertEquals("", open());
        assertEquals("MenuBarButton[File]", focusedName());
    }

    /**
     * TAB inside an open menu closes it and moves on to the next menu button: the menu is part of its
     * button for focus (step 7c). (Before: focus wrapped round to the File button, menu left open.)
     */
    public void testTabInsideMenuClosesItAndMovesToNextButton() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);

        press(Keys.TAB_KEYSTROKE);

        assertEquals("", open());
        assertEquals("MenuBarButton[Edit]", focusedName());
    }

    /** TAB inside a menu whose submenu is open goes into the submenu (the submenu is part of the menu, right after it) */
    public void testTabInsideMenuWithSubmenuOpenGoesIntoSubmenu() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);              // New selected, its submenu open

        press(Keys.TAB_KEYSTROKE);

        assertEquals("File New ", open());
        assertTrue(focusedName(), focusedName().contains("New-ListBox"));
    }

    /**
     * SHIFT-TAB inside an open menu closes it and goes back to its own button (step 7c).
     * (Before: focus went to the field before the menu bar in the root's order.)
     */
    public void testShiftTabInsideMenuClosesItAndGoesBackToItsButton() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);

        press(Keys.SHIFT_TAB_KEYSTROKE);

        assertEquals("", open());
        assertEquals("MenuBarButton[File]", focusedName());
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
        file.getMenuContainer().asFocusable().focus(null, bus);

        assertTrue(file.isVisible());
    }

    /** focus can go two submenus deep (File > New > More) and every menu on the way stays open */
    public void testFocusIntoSecondLevelSubmenuKeepsAllMenusOpen() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);              // File list: New selected, New's submenu opens
        press(Keys.TAB_KEYSTROKE);               // into New's list (Car)
        press(Keys.DOWN_KEYSTROKE);              // Boat
        press(Keys.DOWN_KEYSTROKE);              // More selected, More's submenu opens

        press(Keys.TAB_KEYSTROKE);               // into More's list

        assertEquals("File New More ", open());
        assertTrue(focusedName(), focusedName().contains("More-ListBox"));
    }

    /** choosing an item two submenus deep runs it, closes all menus, focus back on the File button */
    public void testItemInSecondLevelSubmenuClosesAllMenus() {
        openFile();
        press(Keys.DOWN_KEYSTROKE);
        press(Keys.TAB_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);
        press(Keys.DOWN_KEYSTROKE);
        press(Keys.TAB_KEYSTROKE);               // in More's list (Plane)

        press(Keys.ENTER_KEYSTROKE);

        assertEquals("[Plane]", picked.toString());
        assertEquals("", open());
        assertEquals("MenuBarButton[File]", focusedName());
    }
}
