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
 * Rows as tall as their wrapped text - each row's height from its data, through the column's renderer
 * - scrolled and resized (a resize re-wraps, so heights change): between two lines there is always
 * exactly one row - its ID on its first line only.
 */
public class TableWrapResizeScrollTest extends TestCase {

    private final TableComponent<People.Person> table = new TableComponent<People.Person>(Colors.Yel,
            new Dimension.Simple(24, 90), People.allColumns(People.many(100)));

    @Override
    protected void setUp() throws Exception {
        table.setLinesStyle(TableComponent.TableLinesStyle.ALL);
        table.getSegmentsBuffer().getSegments();
    }

    private List<String> screen() {
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

    /** the data blocks between lines: each has one ID - on its first line - and is not empty */
    private void assertOneRowBetweenLines(String when) {
        List<String> lines = screen();
        List<List<String>> blocks = new ArrayList<List<String>>();
        List<String> block = null;
        for (int i = 3; i < lines.size(); i++) {                       // past the top, the heading and its line
            String line = lines.get(i);
            char first = line.isEmpty() ? ' ' : line.charAt(0);
            if (first == '├' || first == '└') {
                if (block != null) {
                    blocks.add(block);
                }
                block = new ArrayList<String>();
            } else {
                if (block == null) {
                    block = new ArrayList<String>();
                }
                block.add(line);
            }
        }
        if (block != null && !block.isEmpty()) {
            blocks.add(block);
        }
        for (List<String> rowLines : blocks) {
            if (rowLines.isEmpty() || rowLines.get(0).trim().length() <= 2) {
                continue;                                                  // the space under the last row
            }
            int ids = 0;
            for (int k = 0; k < rowLines.size(); k++) {
                String line = rowLines.get(k);
                int idEnd = line.indexOf('│', 1);                         // the ID cell: up to the line after it
                String idCell = idEnd > 1 ? line.substring(1, idEnd).trim() : "";
                if (idCell.matches("\\d+")) {
                    ids++;
                    assertEquals(when + ": an ID below a row's first line - two rows run together, or a line cut a row\n" + String.join("\n", lines), 0, k);
                }
            }
            assertEquals(when + ": one row between two lines\n" + String.join("\n", lines), 1, ids);
        }
    }

    public void testScrolledAndResized() {
        assertOneRowBetweenLines("at the top");
        table.setSelectedRow(15);
        assertOneRowBetweenLines("scrolled to 15");
        table.resize(new Dimension.Simple(24, 60));                       // narrower: more wrapping, taller rows
        assertOneRowBetweenLines("narrower");
        table.setSelectedRow(40);
        assertOneRowBetweenLines("narrower, scrolled to 40");
        table.resize(new Dimension.Simple(24, 120));                      // wider: less wrapping, shorter rows
        assertOneRowBetweenLines("wider");
        table.setSelectedRow(70);
        assertOneRowBetweenLines("wider, scrolled to 70");
        table.setSelectedRow(5);
        assertOneRowBetweenLines("back up to 5");
    }
}
