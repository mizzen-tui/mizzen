package mizzen.components.table;

import static mizzen.events.mouse.MouseEventsForTesting.click;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import mizzen.components.CompositeWithFocusManager;
import mizzen.controlcodes.color.Colors;
import mizzen.examples.People;
import mizzen.geom.Dimension;
import mizzen.supportfortesting.SyncEventLoop;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * A click on a cell says which cell of the model it is - its row and column are what getValueAt
 * takes, whatever the screen did: rows scrolled, columns dropped.
 */
public class TableCellClickTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final ObjectTableModel<People.Person> model = People.allColumns(People.many(100));
    private final List<Object> clicked = new ArrayList<Object>();
    private TableComponent<People.Person> table;

    private void showAt(int width) {
        table = new TableComponent<People.Person>(Colors.Yel, new Dimension.Simple(20, width), model);
        table.setLinesStyle(TableComponent.TableLinesStyle.ALL);
        table.onCellSelected((te) -> clicked.add(model.getValueAt(te.getCellSelected().getTableRow(), te.getCellSelected().getTableCol())));
        CompositeWithFocusManager root = new CompositeWithFocusManager();
        root.add(table);
        bus.setRoot(root);
        table.getSegmentsBuffer();
    }

    // the screen, line by line
    private List<String> screen() {
        TreeMap<Integer, StringBuilder> byRow = new TreeMap<Integer, StringBuilder>();
        for (ScreenLineSegment segment : table.getSegmentsBuffer().getSegments()) {
            StringBuilder line = byRow.computeIfAbsent(segment.getRow(), (r) -> new StringBuilder());
            while (line.length() < segment.getCol() + segment.getText().length()) {
                line.append(' ');
            }
            line.replace(segment.getCol(), segment.getCol() + segment.getText().length(), segment.getText());
        }
        return new ArrayList<String>(new ArrayList<CharSequence>(byRow.values()).stream().map(CharSequence::toString)
                .collect(java.util.stream.Collectors.toList()));
    }

    // a click on the first line of the row showing text, in the column headed heading
    private Object clickOn(String text, String heading) {
        List<String> lines = screen();
        int line = 0;
        while (line < lines.size() && !lines.get(line).contains(text)) {
            line++;
        }
        assertTrue("\"" + text + "\" is on screen:\n" + String.join("\n", lines), line < lines.size());
        int col = lines.get(1).indexOf(heading);
        clicked.clear();
        table.onClick(click(line, col, bus));
        bus.drain();
        assertEquals("one cell clicked", 1, clicked.size());
        return clicked.get(0);
    }

    public void testAColumnAfterADroppedOne() {
        showAt(80);
        String heading = screen().get(1);
        assertFalse("email dropped: " + heading, heading.contains("Email"));
        assertEquals("the phone number - not the email the model has in that place on screen", "555-0102",
                clickOn("Chloe", "Phone"));
    }

    public void testScrolledRowsAndDroppedColumns() {
        showAt(80);
        table.setSelectedRow(60);
        assertEquals("row 61's (Ben Horvat's) phone", "555-0161", clickOn("Horvat", "Phone"));
    }

    public void testNothingDropped() {
        showAt(250);
        assertEquals("chloe.castillo2@example.com", clickOn("Chloe", "Email"));
    }
}
