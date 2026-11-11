package mizzen.components.plot.image.animate;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import junit.framework.TestCase;

/**
 * A GIF given as bytes or as a stream plays as the same GIF given as a file: for a GIF this process can't open as a
 * file (one sent by another process). The stream stays the caller's.
 */
public class AnimatedGifFromBytesTest extends TestCase {

    private static final String GIF = "src/main/resources/mizzen/examples/sampledata/images/bouncing-ball.gif";

    public void testBytesAndStreamShowTheFilesFirstFrame() throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(GIF));
        AnimatedGifImageIterator fromFile = new AnimatedGifImageIterator(GIF);
        AnimatedGifImageIterator fromBytes = new AnimatedGifImageIterator(bytes);
        AnimatedGifImageIterator fromStream = new AnimatedGifImageIterator(new ByteArrayInputStream(bytes));
        try {
            BufferedImage expected = fromFile.next();
            assertSamePixels(expected, fromBytes.next());
            assertSamePixels(expected, fromStream.next());
        } finally {
            fromFile.close();
            fromBytes.close();
            fromStream.close();
        }
    }

    public void testTheStreamIsLeftToTheCaller() throws IOException {
        boolean[] closed = { false };
        ByteArrayInputStream stream = new ByteArrayInputStream(Files.readAllBytes(Paths.get(GIF))) {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        AnimatedGifImageIterator gif = new AnimatedGifImageIterator(stream);
        assertFalse("the iterator must not close the caller's stream", closed[0]);
        gif.close();
        assertFalse("nor when it is closed itself", closed[0]);
    }

    public void testClosedFromBytesItEnds() throws IOException {
        AnimatedGifImageIterator gif = new AnimatedGifImageIterator(Files.readAllBytes(Paths.get(GIF)));
        assertTrue(gif.hasNext());
        gif.close();
        gif.close();                                    // twice is harmless
        assertFalse(gif.hasNext());
    }

    public void testBytesThatArentAGifFail() {
        try {
            new AnimatedGifImageIterator(new byte[] { 1, 2, 3, 4 });
            fail("expected an IOException");
        } catch (IOException expected) {
            // what ImageIO says is its own; only that it fails, as a file that isn't a GIF does
        }
    }

    private static void assertSamePixels(BufferedImage expected, BufferedImage actual) {
        assertEquals(expected.getWidth(), actual.getWidth());
        assertEquals(expected.getHeight(), actual.getHeight());
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                assertEquals("pixel " + x + "," + y, expected.getRGB(x, y), actual.getRGB(x, y));
            }
        }
    }
}
