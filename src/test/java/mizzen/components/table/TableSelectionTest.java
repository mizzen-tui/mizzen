package mizzen.components.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mizzen.components.CompositeWithFocusManager;
import mizzen.components.input.TextInput;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.supportfortesting.CanvasForTesting;
import mizzen.supportfortesting.SyncEventLoop;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * TableComponent's selected row: keys move it (scrolling to keep it on screen), onRowSelected hears
 * every move, ENTER activates it; one TAB stop; the selected row is painted in the selected colour.
 * Layout: root[before (text field), table (20 rows "name1".."name20", 8 lines high), after].
 */
public class TableSelectionTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final CompositeWithFocusManager root = new CompositeWithFocusManager();   // as TUIApp builds it
    private final TextInput before = new TextInput(Colors.IYel, 10);
    private final TextInput after = new TextInput(Colors.IYel, 10);
    private TableComponent table;
    private final List<String> heard = new ArrayList<String>();

    @Override
    protected void setUp() throws Exception {
        List<Object[]> rows = new ArrayList<Object[]>();
        for (int i = 1; i <= 20; i++) {
            rows.add(new Object[] { i, "name" + i });
        }
        table = new TableComponent(Colors.Yel, new Dimension.Simple(8, 30), new DefaultTableModel(Arrays.asList("ID", "Name"), rows));
        table.setName("table");
        before.setName("before");
        after.setName("after");
        root.add(before);
        root.add(1, 0, table);
        root.add(10, 0, after);
        CanvasForTesting.buildCanvasForTestingNoControlCodesInOutput().addComponentSegments(root);
        bus.setRoot(root);
        table.onRowSelected((te) -> heard.add("selected:" + te.getRowSelected().getTableRow()));
        table.onRowActivated((row, eventBus) -> heard.add("activated:" + row));
        bus.deliver(new RequestFocusEvent(table.asFocusable()));
        table.getSegmentsBuffer();     // paint once: it learns how many rows fit
    }

    private void press(KeyStroke... keys) {
        for (KeyStroke key : keys) {
            bus.deliver(new KeyEvent("test", key));
            table.getSegmentsBuffer();
        }
    }

    private String focusedName() {
        Focusable focused = FocusManager.UTIL.findCurrentFocus(root);
        return focused == null ? "-" : mizzen.components.HasName.UTIL.getName(focused.getFocusComponent());
    }

    /** the colour a cell's text was painted in */
    private Color colorOf(String text) {
        for (ScreenLineSegment segment : table.getSegmentsBuffer().getSegments()) {
            if (segment.getText().trim().equals(text)) {
                return segment.getColors();
            }
        }
        return null;
    }

    public void testNoRowSelectedAtFirst() {
        assertEquals(-1, table.getSelectedRow());
    }

    /** DOWN / UP move the selected row; every move fires onRowSelected */
    public void testArrowsMoveTheSelection() {
        press(Keys.DOWN_KEYSTROKE, Keys.DOWN_KEYSTROKE, Keys.DOWN_KEYSTROKE, Keys.UP_KEYSTROKE);

        assertEquals(1, table.getSelectedRow());
        assertEquals("[selected:0, selected:1, selected:2, selected:1]", heard.toString());
    }

    /** HOME / END: first / last row; the arrows are used up at the ends (focus stays) */
    public void testHomeEndAndTheEdges() {
        press(Keys.END_KEYSTROKE, Keys.DOWN_KEYSTROKE);
        assertEquals(19, table.getSelectedRow());

        press(Keys.HOME_KEYSTROKE, Keys.UP_KEYSTROKE);
        assertEquals(0, table.getSelectedRow());
        assertEquals("table", focusedName());
    }

    /** moving below the screen scrolls the table; moving back above it scrolls back */
    public void testTheTableScrollsToTheSelectedRow() {
        int fit = table.getRowsDisplayed();
        assertTrue("some rows fit, not all", fit > 0 && fit < 20);

        press(Keys.END_KEYSTROKE);
        assertTrue("the last row is on screen", table.topRow <= 19 && 19 < table.topRow + table.getRowsDisplayed());

        press(Keys.HOME_KEYSTROKE);
        assertEquals(0, table.topRow);
    }

    /** PgDn / PgUp move about a screenful */
    public void testPageKeys() {
        press(Keys.DOWN_KEYSTROKE, Keys.PGDN_KEYSTROKE);
        assertTrue(table.getSelectedRow() > 1);

        press(Keys.PGUP_KEYSTROKE);
        assertEquals(0, table.getSelectedRow());
    }

    /** ENTER activates the selected row */
    public void testEnterActivates() {
        press(Keys.DOWN_KEYSTROKE, Keys.DOWN_KEYSTROKE, Keys.ENTER_KEYSTROKE);

        assertEquals("[selected:0, selected:1, activated:1]", heard.toString());
    }

    /** the selected row is painted in the selected colour, inverted while focused; other rows are not */
    public void testSelectedRowIsPainted() {
        press(Keys.DOWN_KEYSTROKE, Keys.DOWN_KEYSTROKE);

        assertEquals(new Colors.FGBGColors(Colors.IYel).invert(), colorOf("name2"));
        assertFalse(new Colors.FGBGColors(Colors.IYel).invert().equals(colorOf("name1")));
    }

    /** one TAB stop: TAB leaves the table, SHIFT-TAB comes back with the same selection */
    public void testOneTabStop() {
        press(Keys.DOWN_KEYSTROKE);

        press(Keys.TAB_KEYSTROKE);
        assertEquals("after", focusedName());

        press(Keys.SHIFT_TAB_KEYSTROKE);
        assertEquals("table", focusedName());
        assertEquals(0, table.getSelectedRow());
    }

    /** keys the table doesn't use go on (the root doesn't take them for the table) */
    public void testOtherKeysAreNotTaken() {
        assertNull(root.canHandle(new KeyEvent("test", new KeyStroke('x'))));
        assertNotNull(root.canHandle(new KeyEvent("test", Keys.DOWN_KEYSTROKE)));
    }
}
