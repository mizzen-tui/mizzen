package mizzen.events.keyboard;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import mizzen.events.EventBus;
import mizzen.events.EventBusImpl;
import mizzen.events.OnIdleEvent;
import mizzen.events.TerminalEvent.TYPE;
import mizzen.util.Stty;

/**
 * Warning - once you instantiate this object, you are creating and 
 * running a new Thread.  So use this sparingly. 
 * 
 * <p/>
 * 
 * Usually only one of these objects per application.
 * 
 * <p/>
 * 
 * This object reads input (usually from stdin) and then converts
 * the input (a sequence of bytes) to the appropriate event (usually a keystroke or mouse event).
 * This event is enqueued onto an eventBus for later handling.  Any handlers
 * attached to the eventBus can then process the enqueued event.
 * 
 * <p/>
 * 
 * If there is no input generated within a time-interval (usually 50 ms) then
 * a timerEvent is generated and enqueued onto the eventBus.  Any Idle event handlers
 * in the {@link EventBusImpl} can subsequently process the timer event. 
 * 
 */
public class InputEventsProducer implements Runnable { 

    public static int IDLE_STARVATION_INTERVAL_LIMIT = 60;
    
    private InputStream   inputStream;
    private EventBus      eventBus;
//    private Reader        input;
    Thread                readThread;
    InputBytesEventParserInterface readerParser = InputParserManager.getParser();
    
    int idleEventStarvationLimit = IDLE_STARVATION_INTERVAL_LIMIT;
    int sleepInterval = IDLE_STARVATION_INTERVAL_LIMIT - 10;

    // reads failing in a row: reported on the first, then every this many (a second apart by then: about every 10 s)
    static final int REPORT_EVERY_FAILURES = 10;
    
    /* For auto-numbering KeyInputEventsProducer threads. */
    private static int threadInitNumber;
    private static synchronized int nextThreadNum() {
        return threadInitNumber++;
    }

    // TrackingInputStream
    public InputEventsProducer(InputStream input, EventBus eventBus) throws UnsupportedEncodingException {
        this.eventBus = eventBus;
        if (input == null) {
             inputStream = new FileInputStream(FileDescriptor.in);
            //inputStream = new TrackingInputStream(FileDescriptor.in);
            try {
                Stty.setRaw();
            } catch (Exception e) {
                System.err.println("Failed to go into raw mode! " + e.getMessage());
                inputStream = System.in;
            }
        } else {
            inputStream = input;
        }
  //      this.input = new InputStreamReader(inputStream , Charset.defaultCharset());

        readThread = new Thread(this, "KeyInputEvents-Thread"+nextThreadNum());
        readThread.start();

    }
    
    public Thread getReaderThread() {
        return readThread;
    }

    public InputEventsProducer(EventBus eventBus) throws UnsupportedEncodingException {
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
        byte[] buffer = new byte[127];
        boolean isEOF = false;

        boolean inputStreamHasMoreData = false;
        int failuresInARow = 0;     // reads failing one after another: the terminal is gone, or this app is in the background
        
        while (eventBus.isActive() && !isEOF) {
            generateStarvedIdleEvent();
            inputStreamHasMoreData = false;
            boolean failed = false;
            try {
                int available = this.inputStream.available();
                if (available > 0) {
                    int readIn = inputStream.read(buffer, 0, buffer.length);

                    inputStreamHasMoreData = readIn < available;

                    isEOF = readIn == -1;
                    if (!isEOF) {
                        handleReaderInput(buffer, readIn);
                    }

                } else {
                    generateIdleEventIfNeeded();
                }

            } catch (Throwable e) {  // Warning .. dangerous to catch Throwable (I know) - but I have 
                                     //            to signal other threads (in EventBus) of the situation
                Exception e1 = null;
                if (!(e instanceof Exception)) {
                    e1 = new Exception(e);
                } else {
                    e1 = (Exception) e;
                }
                failed = true;
                failuresInARow++;
                if (failuresInARow % REPORT_EVERY_FAILURES == 1) {
                    // the first of a run of failures, and again every so often while it lasts (in case a report
                    // was lost): when reads keep failing (the terminal went away, or the app was put in the
                    // background), every retry would raise one more event and flood the bus and the log
                    eventBus.raiseEvent(InputEventsProducer.this, TYPE.EXCEPTION, e1);
                }
                if (!(e instanceof Exception)) { // if not an Exception - then probably an Error - so better to get the hell out
                    throw new RuntimeException(e);
                }
            }
            if (!failed) {
                failuresInARow = 0;
            }

            try {
                // failing: wait longer each time (twice, up to a second) instead of retrying at full speed
                long wait = failuresInARow > 0 ? Math.min(1000, (long) sleepInterval << Math.min(failuresInARow, 10))
                                               : (inputStreamHasMoreData ? 1 : sleepInterval);
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                // Ignoring intentionally
            }

        }

        if (isEOF && eventBus.isActive()) {
            terminateReads();
        }

    }

    long lastIdleEventTime = 0;
    
    private void generateIdleEventIfNeeded() {
        boolean idleHandled = idle();
        if (!idleHandled) {
            // no pending keyboard keys handled - lets generate a timerEvent
            if (eventBus.isEmptyQueue()) {
              eventBus.raiseEvent(new OnIdleEvent(this));
              lastIdleEventTime = System.currentTimeMillis();
            }
        } else {
            generateStarvedIdleEvent();
        }
    }

    private void generateStarvedIdleEvent() {
        // we will do an idle event if too much time has elapsed
        long elapsed = System.currentTimeMillis() - lastIdleEventTime;
        if (elapsed > sleepInterval) {
                eventBus.raiseEvent(new OnIdleEvent(this));
                lastIdleEventTime = System.currentTimeMillis();
        }
    }

    private boolean idle() {
        return readerParser.idle(eventBus);
    }

    private void handleReaderInput(byte[] buffer, int readIn) {
        readerParser.parseInput(buffer, readIn, eventBus);
    }

    public void setIdleEventStarvationLimit(int interval) {
        if (interval < 2) {
            throw new IllegalArgumentException("idle interval of "+ interval + " is too low!");
        }
        if (interval < 60) {
            if ( (interval - 10) > 1 ) {
                this.sleepInterval = interval - 10;
            } else {
                this.sleepInterval = 1;
            }
        }
        this.idleEventStarvationLimit = interval;
    }

}
