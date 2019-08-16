package dtb.terminalIO.events.keyboard;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.OnIdleEvent;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.util.Stty;

/**
 * Warning - once you instantiate this object, you are creating and 
 * running a new Thread.  So use this sparingly. 
 * 
 * Usually only one of these objects per application.
 * 
 * <p/>
 * 
 * This object reads input (usually from stdin) and then converts
 * the input to the appropriate event (usually a keystroke event).
 * This event is enqueued onto an eventBus for later handling.  Any keyStroke handlers
 * can then process the keystroke.
 * 
 * <p/>
 * 
 * If there is no input generated within a time-interval (usually 50 ms) then
 * a timerEvent is generated and enqueued onto the eventBus.  Any Idle event handlers
 * in the {@link EventBus} can subsequently process the timer event. 
 * 
 */
public class KeyInputEventsProducer implements Runnable {

    private InputStream   inputStream;
    private EventBus      eventBus;
    private Reader        input;
    Thread                readThread;
    InputCharsEventParser readerParser = new InputCharsEventParser();

    
    /* For auto-numbering KeyInputEventsProducer threads. */
    private static int threadInitNumber;
    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }
    
    
    public KeyInputEventsProducer(InputStream input, EventBus eventBus) throws UnsupportedEncodingException {
        this.eventBus = eventBus;
        if (input == null) {
            inputStream = new FileInputStream(FileDescriptor.in);
            try {
                Stty.setRaw();
            } catch (Exception e) {
                System.err.println("Failed to go into raw mode!");
                inputStream = System.in;
            }
        } else {
            inputStream = input;
        }
        this.input = new InputStreamReader(inputStream , Charset.defaultCharset());

        readThread = new Thread(this, "KeyInputEvents-Thread"+nextThreadNum());
        readThread.start();

    }

    public KeyInputEventsProducer(EventBus eventBus) throws UnsupportedEncodingException {
        this(null, eventBus);
    }

    public void terminateReads() {
        eventBus.stopConsuming();
    }

    public boolean isActive() {
        return eventBus.isActive();
    }

    @Override
    public void run() {
        char[] buffer = new char[255];
        boolean isEOF = false;

        while (eventBus.isActive() && !isEOF) {

            try {

                int available = this.inputStream.available();
                if (available > 0) {
                    int readIn = input.read(buffer, 0, buffer.length);

                    isEOF = readIn == -1;
                    if (!isEOF) {
                        handleReaderInput(buffer, readIn);
                    }

                } else {
                    boolean idleHandled = idle();
                    if (!idleHandled) {
                	// no pending keyboard keys handled - lets generate a timerEvent
                        if (eventBus.isEmptyQueue()) {
                          eventBus.raiseEvent(new OnIdleEvent(this));
                        }
                    }
                }

            } catch (Throwable e) {  // Warning .. dangerous to catch Throwable (I know) - but I have 
                                     //            to signal other threads (in EventBus) of the situation
                Exception e1 = null;
                if (!(e instanceof Exception)) {
                    e1 = new Exception(e);
                } else {
                    e1 = (Exception) e;
                }
                eventBus.raiseEvent(KeyInputEventsProducer.this, TYPE.EXCEPTION, e1);
                if (!(e instanceof Exception)) { // if not an Exception - then probably an Error - so better to get the hell out
                    throw new RuntimeException(e);
                }
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Ignoring intentionally
            }

        }

        if (isEOF && eventBus.isActive()) {
            terminateReads();
        }

    }

    private boolean idle() {
	return readerParser.idle(eventBus);
    }

    private void handleReaderInput(char[] buffer, int readIn) {
        readerParser.parseInput(buffer, readIn, eventBus);
    }

}
