package mizzen.components.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

import mizzen.controlcodes.color.Colors;
import mizzen.examples.People;
import mizzen.geom.Dimension;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * Columns marked droppable go when the table is too narrow - lower order first, before any column is
 * truncated; the others are never dropped. People.allColumns marks the bio 1 and the email 2.
 */
public class TableDropColumnsTest extends TestCase {

    private static final List<String> NEVER_DROPPED = Arrays.asList("ID", "FirstName", "LastName", "Age",
            "Occupation", "City", "Country", "Phone");

    private static TableComponent<People.Person> table(int width) {
        TableComponent<People.Person> table = new TableComponent<People.Person>(Colors.Yel,
                new Dimension.Simple(12, width), People.allColumns(People.many(5)));
        table.setLinesStyle(TableComponent.TableLinesStyle.ALL);
        return table;
    }

    // the screen, line by line
    private static List<String> screen(TableComponent<?> table) {
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
        return lines;
    }

    // the column headings, left to right: "│ID│FirstName│...│" gives [ID, FirstName, ...]
    private static List<String> headings(TableComponent<?> table) {
        List<String> headings = new ArrayList<String>();
        for (String heading : screen(table).get(1).split("│")) {
            if (!heading.trim().isEmpty()) {
                headings.add(heading.trim());
            }
        }
        return headings;
    }

    public void testWideEnoughNothingIsDropped() {
        List<String> headings = headings(table(250));
        assertTrue(headings.toString(), headings.contains("Bio") && headings.contains("Email"));
    }

    /** narrowing: the bio goes, then the email; while either shows, every other column shows whole */
    public void testDroppedInOrderBeforeAnythingElse() {
        boolean bothDropped = false;
        for (int width = 250; width >= 40; width--) {
            List<String> headings = headings(table(width));
            if (headings.contains("Bio") || headings.contains("Email")) {
                assertTrue("at " + width + " a column was cut while one could be dropped: " + headings,
                        headings.containsAll(NEVER_DROPPED));
            }
            if (headings.contains("Bio")) {
                assertTrue("the email went before the bio, at " + width + ": " + headings, headings.contains("Email"));
            }
            bothDropped |= !headings.contains("Bio") && !headings.contains("Email");
        }
        assertTrue("the bio and the email were never both dropped", bothDropped);
    }

    /** with the email dropped, the phone column after it shows phone numbers - not emails */
    public void testAColumnAfterADroppedOneShowsItsOwnData() {
        TableComponent<People.Person> table = table(70);
        List<String> headings = headings(table);
        assertFalse(headings.toString(), headings.contains("Email"));
        assertEquals(headings.toString(), "Phone", headings.get(headings.size() - 1));
        String firstRow = screen(table).get(3);
        assertTrue(firstRow, firstRow.contains("555-01") && !firstRow.contains("@"));
    }

    /** the columns left are widened to fill the table */
    public void testTheColumnsLeftFillTheWidth() {
        String heading = screen(table(70)).get(1);
        assertEquals(heading, 70, heading.trim().length());
    }

    /** drawn wide and back again, the columns are as wide as before - widening doesn't stick */
    public void testWideningDoesNotStick() {
        TableComponent<People.Person> table = table(110);
        String before = screen(table).get(1);
        table.resize(new Dimension.Simple(12, 250));
        screen(table);
        table.resize(new Dimension.Simple(12, 110));
        assertEquals(before, screen(table).get(1));
    }

    /** redrawn without a resize (a new selection), the same columns show - wider, narrower, wider */
    public void testRedrawnAfterResizesTheSameColumnsShow() {
        TableComponent<People.Person> table = table(70);
        List<String> narrow = headings(table);
        table.resize(new Dimension.Simple(12, 250));
        assertEquals(10, headings(table).size());
        table.resize(new Dimension.Simple(12, 70));
        assertEquals(narrow, headings(table));
        table.setSelectedRow(1);
        assertEquals(narrow, headings(table));
    }
}
