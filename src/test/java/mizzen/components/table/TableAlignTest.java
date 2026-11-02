package mizzen.components.table;

import java.util.Arrays;

import mizzen.controlcodes.color.Colors;
import mizzen.core.TextSnapshot;
import mizzen.geom.Align;
import junit.framework.TestCase;

/** Cells and headings aligned from the model; by default numbers right, text left, headings centred. */
public class TableAlignTest extends TestCase {

    private static String[] lines(ObjectTableModel<String> model) {
        TableComponent<String> table = new TableComponent<String>(Colors.Yel, model.setRows(Arrays.asList("ab")));
        table.setLinesStyle(TableComponent.TableLinesStyle.HEADER_ALL_AND_BORDER);
        return TextSnapshot.of(table, 6, 16).split("\n");
    }

    public void testByDefaultHeadingsCentredTextLeft() {
        String[] page = lines(new ObjectTableModel<String>().column("Name", (s) -> s));
        assertEquals("│     Name     │", page[1]);
        assertEquals("│ab            │", page[3]);
    }

    public void testHeaderAlign() {
        assertEquals("│Name          │", lines(new ObjectTableModel<String>().column("Name", (s) -> s).headerAlign(Align.LEFT))[1]);
        assertEquals("│          Name│", lines(new ObjectTableModel<String>().column("Name", (s) -> s).headerAlign(Align.RIGHT))[1]);
    }

    public void testAlign() {
        assertEquals("│            ab│", lines(new ObjectTableModel<String>().column("Name", (s) -> s).align(Align.RIGHT))[3]);
    }

    public void testGoesAfterAColumn() {
        try {
            new ObjectTableModel<String>().align(Align.LEFT);
            fail("no column yet");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("align"));
        }
    }
}
