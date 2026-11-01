package mizzen.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

/** A log file that can't be written: said on stdout by default, nothing at all when quiet; neither fails the app. */
public class LoggingFileErrorsTest extends TestCase {

    private final PrintStream realOut = System.out;
    private final boolean wasEnabled = Logging.isEnabled();

    @Override
    protected void tearDown() {
        System.setOut(realOut);
        Logging.setFileErrorsQuiet(false);
        Logging.setBackend(null);
        Logging.setEnabled(wasEnabled);
    }

    // what a line to a file that can't be opened puts on stdout
    private String stdoutOfALine() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        Logging.setEnabled(true);
        Logging.setBackend(Logging.file("no/such/folder/at/all/mizzen.log"));
        Logging.info("a line");
        System.setOut(realOut);
        return out.toString();
    }

    public void testShownByDefault() {
        assertTrue(stdoutOfALine().contains("mizzen.log"));
    }

    public void testQuietSaysNothing() {
        Logging.setFileErrorsQuiet(true);
        assertEquals("", stdoutOfALine());
    }
}
