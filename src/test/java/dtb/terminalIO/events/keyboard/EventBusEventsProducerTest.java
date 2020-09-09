package dtb.terminalIO.events.keyboard;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventBusImpl;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.InputBytesHelper.BytesContainer;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import junit.framework.TestCase;

public class EventBusEventsProducerTest extends TestCase {

    
    private EventBus makeEventProducer(EventBus eventBus, InputStream inputStream) {
        try {
            new InputEventsProducer(inputStream, eventBus);
            return eventBus;
        } catch (Exception e) {
            eventBus.stopConsuming();
            return null;
        }
    }
    
    
    private String runTest(String inputSource) throws Exception {
 
        boolean[] stopFlag = new boolean[] {false};

        ByteArrayInputStream inputStream = new ByteArrayInputStream(inputSource.getBytes()) {
            
            public int available()  {
                int result = super.available();
                if (result <1) {
                    stopFlag[0] = true;
                }
                return result;
            }
        };

        EventBus eventBus = new EventBusImpl() {
            protected void handleEvent(TerminalEvent<?> te) {
                super.handleEvent(te);
                // System.out.println("Got here: " + te);
            }
        };
        
        BytesContainer keyboardInputBytesContainer = new BytesContainer();
        
        eventBus.appendEventHandler( new KeyInputEventHandler((te)->{
            return true;
            },
           (te)->{
               InputBytesHelper.captureAndStoreKeyEvent(keyboardInputBytesContainer,
                                                        (KeyEvent)te);
           })); 
        
        eventBus.appendIdleEventHandler(new OnIdleEventEventHandler.RepeatingInterval(100, (te)-> {
            if (stopFlag[0] && eventBus.isEmptyQueue()) {
                eventBus.appendEventHandler( new OnIdleEventEventHandler.OneTimeAfterInterval((te2)-> {
                    eventBus.stopConsuming();
                }, 100));
            }
        }));

        makeEventProducer(eventBus, inputStream);
        
        while(eventBus.isActive()) {
            Thread.sleep(50);
        }
        String result = new String(keyboardInputBytesContainer.capturedAsBytes());
        
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
        
        String testStrx = "1111111111111111111111111111111111111111111111111111111111111111111111111111111111111";
        String testStr = testStrx;
        for (int i=2; i<9 ; i++) {
            testStr = testStr + "\n" + testStrx.replace('1', ((i+"").charAt(0)));
        }
        
        util_readInput(testStr);
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
