package mizzen.components.table;

import static mizzen.events.mouse.MouseEventsForTesting.click;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mizzen.components.CompositeWithFocusManager;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.supportfortesting.SyncEventLoop;
import junit.framework.TestCase;

/** What is at a spot on a table: the model's cell, where in it, the size it was drawn - asked, or told by a click. Also the INNER style. */
public class TableCellAtTest extends TestCase {

    private TableComponent<String> table(TableComponent.TableLinesStyle style) {
        TableComponent<String> table = new TableComponent<String>(Colors.Yel, new Dimension.Simple(10, 30),
                new ObjectTableModel<String>().column("Name", (s) -> s).column("Size", (s) -> s.length()).setRows(Arrays.asList("gear", "nut")));
        table.setLinesStyle(style);
        table.setSpacedRows(false);
        table.getSegmentsBuffer();
        return table;
    }

    public void testAskedWithABorder() {
        TableComponent.CellSelected cell = table(TableComponent.TableLinesStyle.HEADER_ALL_AND_BORDER).cellAt(new Position.Simple(4, 3));
        assertEquals("the second row", 1, cell.getTableRow());
        assertEquals(0, cell.getTableCol());
        assertEquals(0, cell.getOffsetInCell().getRow());
        assertEquals(2, cell.getOffsetInCell().getCol());
        assertEquals(1, cell.getCellSize().getHeight());
    }

    public void testNothingThere() {
        assertNull("the line under the heading", table(TableComponent.TableLinesStyle.INNER).cellAt(new Position.Simple(1, 3)));
    }

    public void testInnerHasNoBorderAroundIt() {
        TableComponent<String> table = table(TableComponent.TableLinesStyle.INNER);
        assertTrue("the heading at the top", table.cellAt(new Position.Simple(0, 0)).isHeader());
        TableComponent.CellSelected first = table.cellAt(new Position.Simple(2, 0));
        assertEquals(0, first.getTableRow());
        assertEquals(0, first.getOffsetInCell().getCol());
    }

    public void testAClickSaysTheSame() {
        SyncEventLoop bus = new SyncEventLoop();
        TableComponent<String> table = table(TableComponent.TableLinesStyle.HEADER_ALL_AND_BORDER);
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(table);
        bus.setRoot(root);
        List<String> heard = new ArrayList<String>();
        table.onCellSelected((te) -> heard.add(te.getCellSelected().getOffsetInCell().getCol() + " of " + te.getCellSelected().getCellSize().getWidth()));
        table.getSegmentsBuffer();
        table.onClick(click(3, 3, bus));
        bus.drain();
        assertEquals(1, heard.size());
        assertTrue(heard.toString(), heard.get(0).startsWith("2 of "));
    }

    public void testSizedByItsContent() {
        assertEquals("heading, its line, two rows", 4, table(TableComponent.TableLinesStyle.INNER).heightAt(30));
    }
}
