package mizzen.components.table;

import static mizzen.events.mouse.MouseEventsForTesting.click;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mizzen.components.CompositeWithFocusManager;
import mizzen.controlcodes.color.Colors;
import mizzen.events.Registration;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/**
 * A typed table hands over the picked row itself (onPick), alongside onRowSelected's row number; pick
 * picks by code as a click would; a click on a heading picks nothing.
 */
public class TablePickTest extends TestCase {

    public static class Part {
        public final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Part gear = new Part("gear"), nut = new Part("nut"), spring = new Part("spring");
    private final SyncEventLoop bus = new SyncEventLoop();
    private final List<String> heard = new ArrayList<String>();
    private TableComponent<Part> table;

    @Override
    protected void setUp() throws Exception {
        ObjectTableModel<Part> model = new ObjectTableModel<Part>().column("Name", (p) -> p.name).setRows(Arrays.asList(gear, nut, spring));
        table = new TableComponent<Part>(Colors.Yel, new Dimension.Simple(8, 30), model);
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(table);
        bus.setRoot(root);
        bus.deliver(new RequestFocusEvent(table.asFocusable()));
        table.getSegmentsBuffer();
    }

    private void down() {
        bus.deliver(new KeyEvent("test", Keys.DOWN_KEYSTROKE));
        table.getSegmentsBuffer();
    }

    public void testOnPickHandsOverTheRowItself() {
        table.onPick((part, te) -> heard.add(part.name));
        down();
        down();
        assertEquals("[gear, nut]", heard.toString());
        assertSame(nut, table.getPicked());
    }

    public void testOnPickAndOnRowSelectedBothHear() {
        table.onRowSelected((te) -> heard.add("row " + te.getRowSelected().getTableRow()));
        table.onPick((part, te) -> heard.add(part.name));
        down();
        assertEquals("[row 0, gear]", heard.toString());
    }

    public void testTheRegistrationTakesAListenerOut() {
        Registration listening = table.onPick((part, te) -> heard.add(part.name));
        down();
        listening.unregister();
        down();
        assertEquals("[gear]", heard.toString());
    }

    public void testPickByCodeTellsTheListeners() {
        table.onPick((part, te) -> heard.add(part.name));
        table.pick(spring, bus);
        bus.drain();
        assertEquals("[spring]", heard.toString());
        assertEquals(2, table.getSelectedRow());
    }

    public void testPickingSomethingNotInTheTableFails() {
        try {
            table.pick(new Part("elsewhere"), bus);
            fail("not one of the rows");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testNothingPickedAtFirst() {
        assertNull(table.getPicked());
    }

    /** a click on a column heading isn't a row: the selection stays, nobody told (it used to select row -1) */
    public void testAClickOnAHeadingPicksNothing() {
        down();                                           // gear selected
        table.onRowSelected((te) -> heard.add("row " + te.getRowSelected().getTableRow()));
        table.onClick(click(1, 3, bus));                  // row 0 is the border, row 1 the headings
        bus.drain();
        assertEquals(0, table.getSelectedRow());
        assertEquals("[]", heard.toString());
    }

    /** with nothing selected, a heading click isn't "a click on the selected row" (it used to activate row -1) */
    public void testAClickOnAHeadingActivatesNothing() {
        table.onRowActivated((row, eventBus) -> heard.add("activated " + row));
        table.onClick(click(1, 3, bus));
        bus.drain();
        assertEquals("[]", heard.toString());
    }

    /** a model that gives no rows still shows; asking for picks fails at once, saying why */
    public void testAModelWithoutRowsFailsAtOnPick() {
        TableModel<Part> noRows = new TableModel<Part>() {
            public int getRowCount() { return 1; }
            public int getColumnCount() { return 1; }
            public String getColumnName(int columnIndex) { return "Name"; }
            public Object getValueAt(int rowIndex, int columnIndex) { return "gear"; }
        };
        TableComponent<Part> old = new TableComponent<Part>(Colors.Yel, new Dimension.Simple(8, 30), noRows);
        old.getSegmentsBuffer();
        try {
            old.onPick((part, te) -> heard.add(part.name));
            fail("no rows to hand over");
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("implement getRow"));
        }
    }
}
