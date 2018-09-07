package dtb.terminalIO.events;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandler;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.examples.CompositeTest;
import dtb.terminalIO.util.Stty;

public class KeyInputEventsProducer implements Runnable {

    private InputStream   inputStream;
    private EventBus      eventBus;
    private boolean       rawMode      = false;
    private Reader        input;
    Thread                readThread;
    InputCharsEventParser readerParser = new InputCharsEventParser();

    public KeyInputEventsProducer(InputStream input, EventBus eventBus) throws UnsupportedEncodingException {
        this.eventBus = eventBus;
        if (input == null) {
            inputStream = new FileInputStream(FileDescriptor.in);
            try {
                Stty.setRaw();
                rawMode = true;
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

    public static void mainE(String[] args) throws InterruptedException, IOException {

        try {
            
            Stty.setRaw();

            CompositeTest.main(args);
            for (int i = 0; i < 100; i++) {

                CompositeTest.statusMsg("i = " + i);

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // Ignoring intentionally
                }

            }
        } finally {
            
            Stty.setCooked();

        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {

        EventBus eventBus = new EventBus();
        try {
            KeyInputEventsProducer keyInputEventsProducer = null;
            try {
                keyInputEventsProducer = new KeyInputEventsProducer(eventBus);
            } catch (Exception e) {
                eventBus.stopConsuming();
                return;
            }

            if (keyInputEventsProducer.readThread.isAlive()) {

                CompositeTest.main(args);

                eventBus.addEventHandler(new ConditionalEventHandler() {

		    @Override
		    public void handle(TerminalEvent<?> te) {
                        CompositeTest.statusMsg2(String.valueOf(te.getEventData()));
		    }

		    @Override
		    public EventHandler canHandle(TerminalEvent<?> te) {
                        if (te.type.equals(TYPE.DEBUG))
                            return this;
                        return null;
		    }
                    
                });
                
                
                eventBus.addEventHandler(new ConditionalEventHandler() {

                    @Override
                    public void handle(TerminalEvent<?> te) {

                        Keys.KeyEvent ke = (Keys.KeyEvent) te;

                        // eventBus.stopConsuming();

                        if (ke.isKeyStroke(Keys.ESC_KEYSTROKE)) {
                            CompositeTest.statusMsg(ke.getKeyName() + " hit. Quitting..");
                            eventBus.stopConsuming();
                            return;
                        }
                        if (1+1==3 && ke.isKeyStroke(Keys.CTRL_C_KEYSTROKE)) {
                            CompositeTest.statusMsg(ke.getKeyName() + " hit. Quitting.." + Keys.CTRL_C_KEYSTROKE.name);
                            eventBus.stopConsuming();
                            return;
                        }
//                        if (ke.isKeyStroke(Keys.A_KEYSTROKE)) {
//                            CompositeTest.statusMsg("A hit. Quitting..");
//                            eventBus.stopConsuming();
//                            return;
//                        }

                        CompositeTest.statusMsg(String.valueOf(ke.getEventData()));
                    }

                    @Override
                    public EventHandler canHandle(TerminalEvent<?> te) {
                        if (te.type.equals(TYPE.KEYPRESS))
                            return this;
                        return null;
                    }
                });

                keyInputEventsProducer.readThread.join();
                eventBus.stopConsuming();
            } else {
                eventBus.stopConsuming();
            }

            Stty.setCooked();

        } catch (Exception e) {
            eventBus.stopConsuming();
            Stty.setCooked();
            System.out.println("");
            e.printStackTrace();
        }
    }

}
