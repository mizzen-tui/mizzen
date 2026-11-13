package mizzen.components;

import java.io.StringReader;

import mizzen.controlcodes.color.Colors;
import mizzen.supportfortesting.SyncEventLoop;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * A program that draws a whole screen and redraws it in place (top, watch): it clears once (ESC[2J), then starts each
 * frame at home (ESC[H), erasing what it doesn't overwrite (ESC[K, ESC[J). The viewer shows the last frame only, in
 * place, not the frames piled up under each other.
 */
public class StdoutViewerScreenTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final StdoutViewer viewer = new StdoutViewer(Colors.Gre);

    private String shownAfter(String output) throws InterruptedException {
        StdoutViewer.Feed feed = viewer.follow(new StringReader(output), bus);
        assertTrue("read to the end", feed.waitUntilDone(5000));
        viewer.flush(bus);
        StringBuilder text = new StringBuilder();
        int row = -1;
        for (ScreenLineSegment s : viewer.getSegmentsBuffer().getSegments()) {
            if (row != -1) {
                for (int r = row; r < s.getRow(); r++) {
                    text.append('\n');
                }
            }
            row = s.getRow();
            text.append(s.getText());
        }
        return text.toString().replaceAll(" +(\n|$)", "$1").replaceAll("\n+$", "");   // an erase leaves blanks: as on screen
    }

    private static final String E = "\033[";

    public void testFramesRedrawnFromHomeReplaceTheLastOne() throws Exception {
        String first = E + "H" + E + "2J" + "top - 10:00\n" + "Tasks: 120\n" + "  1 root  sshd\n";
        String second = E + "H" + "top - 10:01" + E + "K\n" + "Tasks: 98" + E + "K\n" + E + "J";
        assertEquals("top - 10:01\nTasks: 98", shownAfter(first + second));
    }

    public void testToARowAndColumn() throws Exception {
        String screen = E + "2J" + "aaaa\nbbbb\ncccc\n" + E + "2;3H" + "XY" + E + "1;1f" + "Z" + E + "3d" + "W";
        assertEquals("Zaaa\nbbXY\ncWcc", shownAfter(screen));          // ESC[3d: row 3, the column the Z left it at
    }

    public void testEraseUpClearsAboveAndTheLineToTheCursor() throws Exception {
        String screen = E + "2J" + "aaaa\nbbbb\ncccc" + E + "2;2H" + E + "1J";
        assertEquals("\n  bb\ncccc", shownAfter(screen));
    }
}
