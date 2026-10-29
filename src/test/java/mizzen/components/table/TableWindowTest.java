package mizzen.components.table;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import mizzen.controlcodes.color.Colors;
import mizzen.examples.People;
import mizzen.geom.Dimension;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * Columns are as wide as the rows on screen need: the ID column of rows 1 to 20 is two wide, of rows
 * 101 to 120 three. With widths that only grow, scrolling back keeps it three.
 */
public class TableWindowTest extends TestCase {

    private static TableComponent<People.Person> table() {
        TableComponent<People.Person> table = new TableComponent<People.Person>(Colors.Yel, new Dimension.Simple(14, 60),
                People.fewColumns(People.many(150)));
        table.setLinesStyle(TableComponent.TableLinesStyle.ALL);
        return table;
    }

    // the heading line: "│ID│Name ..."
    private static String heading(TableComponent<?> table) {
        TreeMap<Integer, StringBuilder> byRow = new TreeMap<Integer, StringBuilder>();
        for (ScreenLineSegment segment : table.getSegmentsBuffer().getSegments()) {
            StringBuilder line = byRow.computeIfAbsent(segment.getRow(), (r) -> new StringBuilder());
            while (line.length() < segment.getCol() + segment.getText().length()) {
                line.append(' ');
            }
            line.replace(segment.getCol(), segment.getCol() + segment.getText().length(), segment.getText());
        }
        List<String> lines = new ArrayList<String>();
        for (StringBuilder line : byRow.values()) {
            lines.add(line.toString());
        }
        return lines.get(1);
    }

    private static int idWidth(TableComponent<?> table) {
        String heading = heading(table);
        return heading.indexOf('│', 1) - 1;
    }

    public void testColumnsAreAsWideAsTheRowsShown() {
        TableComponent<People.Person> table = table();
        assertEquals("ids 1 to 20", 2, idWidth(table));
        table.setSelectedRow(110);
        assertEquals("ids past 100", 3, idWidth(table));
        table.setSelectedRow(0);
        assertEquals("back at the top", 2, idWidth(table));
    }

    public void testWidthsOnlyGrow() {
        TableComponent<People.Person> table = table().setWidthsOnlyGrow(true);
        assertEquals(2, idWidth(table));
        table.setSelectedRow(110);
        assertEquals(3, idWidth(table));
        table.setSelectedRow(0);
        assertEquals("kept from the rows past 100", 3, idWidth(table));
        table.resize(new Dimension.Simple(14, 61));
        assertEquals("a resize starts afresh", 2, idWidth(table));
    }
}
