package mizzen.events.keyboard;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import mizzen.events.EventBus;
import mizzen.examples.sampledata.ReadSampleFiles;
import junit.framework.TestCase;

public class InputBytesEventParserTest extends TestCase {

    InputBytesEventParser inputParser;
    InputBytesHelper.CaptureKeysEventBus eventBus;

    public void setUp() {
        inputParser = new InputBytesEventParser();

        eventBus = new InputBytesHelper.CaptureKeysEventBus(new EventBus.STUBBED());

    }

    public void testABCParseInput() {

        String testInputABC = "abc";

        inputParser.parseInput(testInputABC.getBytes(), testInputABC.getBytes().length, eventBus);

        assertEquals("abc", eventBus.capturedAsString());

    }

    public void testABCWithSpacesParseInput() {

        String testInputABC = "a b c";

        inputParser.parseInput(testInputABC.getBytes(), testInputABC.getBytes().length, eventBus);

        assertEquals(testInputABC, eventBus.capturedAsString());

    }

    private void util_readInput(String testInputABC, int bufSize) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(testInputABC.getBytes());

        byte[] buffer = new byte[bufSize];
        boolean isEOF = false;

        while (eventBus.isActive() && !isEOF) {

            try {

                int available = inputStream.available();
                if (available > 0) {
                    int readIn = inputStream.read(buffer, 0, buffer.length);

                    isEOF = readIn == -1;
                    if (!isEOF) {
                        inputParser.parseInput(buffer, readIn, eventBus);
                    }
                } else {
                    isEOF = true;
                }

            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        assertEquals(testInputABC, eventBus.capturedAsString());

    }

    public void testLargeFileAsInput() throws IOException {
        String bigFile = "LoremIpsumLarge.txt";
        String largeText = ReadSampleFiles.readSampleFile(bigFile);
        util_readInput(largeText, 127);
        
    }

    public void testXWithSpacesParseInput() {

        String testInputABC = "abc!234567890abc!234567890abc!2345\n"
                + "67890abc!234567890abc!234567890abc!234567890abc!234567890abc\n"
                + "!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890";

        util_readInput(testInputABC, 127);

    }

    public void testABCWithCRsParseInput() {

        String testInputABC = "a\nb\nc";

        inputParser.parseInput(testInputABC.getBytes(), testInputABC.getBytes().length, eventBus);

        assertEquals(testInputABC, eventBus.capturedAsString());

    }

    public void testABCWithBuffer() {

        String testInputABC = "a\nb\nc";

        util_readInput(testInputABC, 3);

        assertEquals(testInputABC, eventBus.capturedAsString());

    }

//    public void testIdle() {
//        fail("Not yet implemented");
//    }

}
