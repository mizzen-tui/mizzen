package mizzen.components.card;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import mizzen.components.Component;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.theme.Theme;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/** a tile: title, value in its state's colour, a note, a bar; tiles of a width line up */
public class TileTest extends TestCase {

    private static List<String> lines(Component<?> component) {
        TreeMap<Integer, StringBuilder> rows = new TreeMap<Integer, StringBuilder>();
        for (ScreenLineSegment s : component.getSegmentsBuffer().getSegments()) {
            StringBuilder line = rows.computeIfAbsent(s.getRow(), (r) -> new StringBuilder());
            while (line.length() < s.getCol() + s.getText().length()) {
                line.append(' ');
            }
            line.replace(s.getCol(), s.getCol() + s.getText().length(), s.getText());
        }
        List<String> out = new ArrayList<String>();
        for (StringBuilder line : rows.values()) {
            out.add(line.toString());
        }
        return out;
    }

    private static boolean drawnIn(Component<?> component, String text, mizzen.controlcodes.color.Color color) {
        for (ScreenLineSegment s : component.getSegmentsBuffer().getSegments()) {
            if (s.getText().contains(text) && s.getColors() == color) {
                return true;
            }
        }
        return false;
    }

    public void testTitleValueNoteAtItsWidth() {
        HorizontalPanel screen = new HorizontalPanel();
        Tile site = new Tile("Site", 16).value("HTTP 200").note("store.example.com");
        screen.add(site);
        screen.setTheme(Theme.ROUNDED);
        List<String> shown = lines(site);
        assertEquals(16, site.getWidth());
        assertEquals("│ HTTP 200     │", shown.get(1));
        assertEquals("│ store.examp… │", shown.get(2));
    }

    public void testTheStateColoursTheValue() {
        HorizontalPanel screen = new HorizontalPanel();
        Tile site = new Tile("Site").value("HTTP 500");
        screen.add(site);
        screen.setTheme(Theme.DARK);
        assertTrue("normal: strong", drawnIn(site, "HTTP 500", Theme.DARK.surfaceTheme().strongColor()));
        site.state(Tile.State.ALERT);
        assertTrue("alert: the error colour", drawnIn(site, "HTTP 500", Theme.DARK.surfaceTheme().errorColor()));
    }

    public void testABar() {
        HorizontalPanel screen = new HorizontalPanel();
        Tile disk = new Tile("Disk", 20).value("52% used").bar(52, 100);
        screen.add(disk);
        assertTrue(lines(disk).get(2), lines(disk).get(2).contains("52 ⣿⣿⣿⣿⣿⣀⣀⣀⣀⣀"));
    }
}
