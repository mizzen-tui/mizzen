package dtb.terminalIO.events;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandler;
import dtb.terminalIO.events.Keys.KeyStroke;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.examples.CompositeTest;
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
 * in the EventBus can subsequently process the timerevent. 
 * 
 */
public class KeyInputEventsProducer implements Runnable {

    private InputStream   inputStream;
    private EventBus      eventBus;
    private Reader        input;
    Thread                readThread;
    InputCharsEventParser readerParser = new InputCharsEventParser();

    public KeyInputEventsProducer(InputStream input, EventBus eventBus) throws UnsupportedEncodingException {
        this.eventBus = eventBus;
        if (input == null) {
            inputStream = new FileInputStream(FileDescriptor.in);
            try {
                Stty.setRaw();
            } catch (IOException e) {
                System.err.println("Failed to go into raw mode!");
                inputStream = System.in;
            }
        } else {
            inputStream = input;
        }
        this.input = new InputStreamReader(inputStream, "UTF-8");

        readThread = new Thread(this);
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
                	eventBus.addEvent(new OnIdleEvent(this));
                    }
                }

            } catch (Exception e) {
                eventBus.addEvent(KeyInputEventsProducer.this, TYPE.EXCEPTION, e);
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
