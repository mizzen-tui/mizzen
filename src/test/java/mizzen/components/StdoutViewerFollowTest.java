package mizzen.components;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import mizzen.controlcodes.color.Colors;
import mizzen.supportfortesting.SyncEventLoop;
import mizzen.textsegments.ScreenLineSegment;
import junit.framework.TestCase;

/**
 * StdoutViewer.follow: a source read on its own thread into the viewer - all of it, colour codes
 * applied (not shown as text), and stop() ends a source that never ends.
 */
public class StdoutViewerFollowTest extends TestCase {

    private final SyncEventLoop bus = new SyncEventLoop();
    private final StdoutViewer viewer = new StdoutViewer(Colors.Gre);

    /**
     * Waits for a feed to end, applying what it queued meanwhile - as the app's timed flush does (the
     * queue is bounded: a reader waits for room, so a big source needs it emptied as it goes).
     */
    private boolean readToTheEnd(StdoutViewer.Feed feed, long millis) throws InterruptedException {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            if (feed.waitUntilDone(5)) {
                return true;
            }
            viewer.flush(bus);
        }
        return false;
    }

    /** what the viewer shows, row by row */
    private String shown() {
        viewer.flush(bus);                                    // apply what was queued (the app does it on a timer)
        StringBuilder text = new StringBuilder();
        int row = -1;
        for (ScreenLineSegment s : viewer.getSegmentsBuffer().getSegments()) {
            if (s.getRow() != row && row != -1) {
                text.append('\n');
            }
            row = s.getRow();
            text.append(s.getText());
        }
        return text.toString();
    }

    public void testFollowsLines() throws Exception {
        StdoutViewer.Feed feed = viewer.follow(Arrays.asList("first line", "second line", "third"), bus);
        assertTrue("read to the end", feed.waitUntilDone(5000));
        String shown = shown();
        assertTrue(shown, shown.contains("first line"));
        assertTrue(shown, shown.contains("second line"));
        assertTrue(shown, shown.contains("third"));
    }

    public void testFollowsAReaderWithColourCodes() throws Exception {
        String red = "\u001b[31m", reset = "\u001b[0m";
        StdoutViewer.Feed feed = viewer.follow(new StringReader("plain " + red + "red" + reset + " again\nnext\n"), bus);
        assertTrue(feed.waitUntilDone(5000));
        String shown = shown();
        assertTrue(shown, shown.contains("plain red again"));    // the codes became colours, not text
        assertFalse(shown, shown.contains("[31m"));
        assertTrue(shown, shown.contains("next"));
    }

    public void testKeepsAtMostMaxLinesTheNewest() throws Exception {
        viewer.setMaxLines(1000);
        List<String> lines = new ArrayList<String>();
        for (int i = 0; i < 25000; i++) {
            lines.add("line " + i);
        }
        StdoutViewer.Feed feed = viewer.follow(lines, bus);
        assertTrue(readToTheEnd(feed, 20000));
        String shown = shown();
        assertTrue("height " + viewer.getHeight(), viewer.getHeight() <= 1100);
        assertTrue("the newest kept", shown.contains("line 24999"));
        assertFalse("the oldest dropped", shown.contains("line 0\n"));
        String[] rows = shown.split("\n");
        int last = Integer.parseInt(rows[rows.length - 1].trim().substring(5));
        int before = Integer.parseInt(rows[rows.length - 2].trim().substring(5));
        assertEquals("in order to the end", 24999, last);
        assertEquals(24998, before);
    }

    public void testShowFileReadsTheFileAsItWasWhenOpened() throws Exception {
        File file = File.createTempFile("stdoutviewer", ".txt");
        file.deleteOnExit();
        try (FileWriter w = new FileWriter(file)) {
            w.write("one\ntwo\n");
        }
        StdoutViewer.Feed feed = viewer.showFile(file.getPath(), bus);
        try (FileWriter w = new FileWriter(file, true)) {
            w.write("written after\n");                          // a log still being written
        }
        assertTrue(feed.waitUntilDone(5000));
        String shown = shown();
        assertTrue(shown, shown.contains("two"));
        assertFalse(shown, shown.contains("written after"));
    }

    /** the demo's "show another file": a feed filling the queue is stopped, the viewer cleared, a new source shown */
    public void testSwitchingSourcesLeavesOnlyTheNewOne() throws Exception {
        Iterable<String> flood = () -> new Iterator<String>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public String next() {
                return "old old old";
            }
        };
        StdoutViewer.Feed old = viewer.follow(flood, bus);
        Thread.sleep(200);                                       // the queue fills; the reader waits for room
        old.stop();                                              // (froze for ever before: nothing emptied the queue)
        assertFalse(old.isRunning());
        viewer.clearContent();
        assertTrue(readToTheEnd(viewer.follow(Arrays.asList("fresh"), bus), 5000));
        String shown = shown();
        assertTrue(shown, shown.contains("fresh"));
        assertFalse(shown, shown.contains("old"));
    }

    /** a source that never ends (like a pipe nobody closes): read() waits until it is closed */
    static class Endless extends Reader {
        private volatile boolean closed;

        @Override
        public int read(char[] buffer, int off, int len) throws IOException {
            while (!closed) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return -1;
                }
            }
            return -1;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    public void testStopEndsASourceThatNeverEnds() throws Exception {
        StdoutViewer.Feed feed = viewer.follow(new Endless(), bus);
        assertFalse("still reading", feed.waitUntilDone(200));
        feed.stop();
        assertTrue("stopped", feed.waitUntilDone(5000));
        assertTrue(feed.isStopped());
    }

    public void testStopEndsEndlessLines() throws Exception {
        Iterable<String> forever = () -> new Iterator<String>() {
            int n = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public String next() {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "line " + n++;
            }
        };
        StdoutViewer.Feed feed = viewer.follow(forever, bus);
        assertFalse(feed.waitUntilDone(100));
        feed.stop();
        assertTrue(feed.waitUntilDone(5000));
    }
}
