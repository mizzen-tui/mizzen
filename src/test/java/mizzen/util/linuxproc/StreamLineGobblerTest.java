package mizzen.util.linuxproc;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mizzen.util.linuxproc.ExecUtil.StreamLineGobbler;
import junit.framework.TestCase;

/**
 * A program's output arrives in chunks of any size; StreamLineGobbler hands it on line by line
 * (a line ends at \n or \r), must not lose a character wherever a chunk ends, and stops at the
 * end of the output. Seen in ExecTest: the ESC of control codes and letters at line starts went missing.
 */
public class StreamLineGobblerTest extends TestCase {

    private static final String ESC = "\u001b";
    private static final String OUTPUT = "line one\nline two\r\n" + ESC + "[42Dab" + ESC + "[0;1;34mcolour\n"
                                         + "a much longer line, longer than most of the chunks\nlast";

    /** reads at most chunk bytes at a time, as a pipe from a running program may */
    private static class ChunkedStream extends InputStream {
        private final byte[] bytes;
        private final int chunk;
        private int at;

        ChunkedStream(String text, int chunk) {
            this.bytes = text.getBytes(StandardCharsets.UTF_8);
            this.chunk = chunk;
        }

        private boolean drained() {
            return at >= bytes.length;
        }

        @Override
        public int read() {
            return drained() ? -1 : bytes[at++];
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (drained()) {
                return -1;
            }
            int n = Math.min(Math.min(len, chunk), bytes.length - at);
            System.arraycopy(bytes, at, b, off, n);
            at += n;
            return n;
        }
    }

    private static List<String> linesOf(String text) {
        return Arrays.asList(text.split("[\r\n]", -1));
    }

    /** run() returning at all shows it ends by itself at the end of the output */
    public void testNoCharacterIsLostWhateverTheChunkSize() {
        for (int chunk = 1; chunk <= 7; chunk++) {
            List<String> lines = new ArrayList<>();
            new StreamLineGobbler(new ChunkedStream(OUTPUT, chunk), lines::add, () -> true).run();
            assertEquals("chunks of " + chunk, linesOf(OUTPUT), lines);
        }
    }
}
