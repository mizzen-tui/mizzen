package dtb.terminalIO.events.keyboard;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import junit.framework.TestCase;

public class InputEventsProducerTest extends TestCase {

    protected String runTest(String inputSource) throws Exception {
        InputEventsProducer inputEventsProducer;
        final InputBytesHelper.CaptureKeysEventBus eventBus = new InputBytesHelper.CaptureKeysEventBus(new EventBus.STUBBED());
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(inputSource.getBytes()) {
            
            public int available()  {
                int result = super.available();
                if (result <1) {
                    eventBus.stopConsuming();
                }
                return result;
            }

        };
        
        
        inputEventsProducer = new InputEventsProducer(inputStream, eventBus);
        
        inputEventsProducer.getReaderThread().join();
        
        inputEventsProducer.terminateReads();
        while(inputEventsProducer.isActive()) {
            Thread.sleep(250);
        }
        inputEventsProducer = null;
        
        String result = eventBus.capturedAsString();
        
        return result;
    }
    
    private void util_readInput(String testInputABC) throws Exception {
        String result = runTest(testInputABC);
        assertEquals(testInputABC, result);
    }
    
    
    public void testABCParseInput() throws Exception {
        util_readInput("ABC");
    }

    public void testMediumSmallParseInput() throws Exception {
        util_readInput(
"890abc0abc!234567890abc!234567890abc0abc!234567890abc!234567890abc0abc!234567890abc!234567890abc!234567890abc0abc!234567890abc!234567890abc!234567890abc0abc!234567890abc!234567"
                );
    }
    
    public void testMediumSizeParseInput() throws Exception {
        util_readInput(
                     "890abc0abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890"
                    +"890abc0abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890"
                    +"890abc0abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890"
                    +"890abc0abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890"
                    +"890abc0abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890"
                    +"890abc0abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890"
                    +"890abc0abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890"
                    +"890abc0abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890abc!234567890"
                );
    }

    public void testXWithSpacesParseInput() throws Exception {
        util_readInput("abc!2345\n67 890abc!234567890abc!2345\n"
                + "67890abc!234567890abc!234567890abc!234567890abc!234567890abc\n"
               );
    }

    public void testLargeFileAsInput() throws Exception {
        String bigFile = "LoremIpsumLarge.txt";
        String largeText = ReadSampleFiles.readSampleFile(bigFile);
        util_readInput(largeText);
        
    }

}
