package mizzen.components.table;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import mizzen.controlcodes.color.Colors;
import mizzen.geom.Dimension;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * Rows of different heights, scrolled past the first page: the line under each row is drawn where
 * that row ends - not where rows from the top of the data would have ended (which put lines inside
 * cells and left rows unparted).
 */
public class TableMultiLineScrollTest extends TestCase {

    public static class Item {
        public final String label;

        Item(String label) {
            this.label = label;
        }
    }

    private TableComponent<Item> table(int pickRow) {
        List<Item> items = new ArrayList<Item>();
        for (int i = 0; i < 30; i++) {
            items.add(new Item("r" + i + (i % 2 == 1 ? "\nx\nx" : "")));   // odd rows are three lines high
        }
        TableComponent<Item> table = new TableComponent<Item>(Colors.Yel, new Dimension.Simple(16, 20),
                new ObjectTableModel<Item>().column("Label", (item) -> item.label).setRows(items));
        table.setLinesStyle(TableComponent.TableLinesStyle.ALL);
        table.getSegmentsBuffer().getSegments();                   // drawn once: it learns how many rows fit
        table.setSelectedRow(pickRow);                              // scrolled to it
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

    /** under every row's last line comes a line between rows (or the bottom) - never inside a row */
    private static void assertRowsParted(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.matches("^.r\\d+.*")) {
                int number = Integer.parseInt(line.replaceAll("^.r(\\d+).*$", "$1"));
                int after = i + (number % 2 == 1 ? 3 : 1);             // the line under its last line
                if (after < lines.size()) {
                    for (int k = i + 1; k < after; k++) {
                        assertTrue("row r" + number + " goes on (" + k + "): " + lines, lines.get(k).contains("x"));
                    }
                    char first = lines.get(after).charAt(0);
                    assertTrue("a line under r" + number + " at " + after + ": " + lines, first == '├' || first == '└');
                }
            }
        }
    }

    public void testAtTheTop() {
        assertRowsParted(screen(table(0)));
    }

    public void testScrolledPastTheFirstPages() {
        TableComponent<Item> table = table(21);
        assertTrue("scrolled", table.topRow > 0);
        assertRowsParted(screen(table));
    }

    public void testScrolledToAnEvenRow() {
        assertRowsParted(screen(table(26)));
    }
}
