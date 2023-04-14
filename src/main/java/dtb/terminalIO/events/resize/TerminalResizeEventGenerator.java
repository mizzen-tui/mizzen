package dtb.terminalIO.events.resize;

import java.util.concurrent.atomic.AtomicLong;

import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.TerminalDimensions;

/**
 * A utility class that inspects the current terminal size
 * and if it has changed since last checked it will
 * raise and enqueue a resize event onto the EventBus.
 *
 */
public class TerminalResizeEventGenerator implements Runnable {


    private static final int RESIZE_DELAY_INTERVAL_MILLISECS = 300;

    private Dimension lastSize = Dimension.ZERO;
    private AtomicLong lastChange = new AtomicLong(0);
    private TerminalScreen screen;
    private EventBus eventBus;
    private DelayedResizer delayedResizer = new DelayedResizer(); 

    /**
     * Purpose of this internal class (DelayedResizer) is to delay (or slow down) the resize events getting fired into the
     * event bus.   The purpose is to not overwhelm the application layer with
     * a flood or resize events because usually when the app needs to react to a 
     * resize, it has a lot of work to do to redraw everything.  We only send resize events (TerminalEvent.TYPE.RESIZE)
     * after a specified interval
     * We use a very short-lived thread to perform this task.
     */
    private class DelayedResizer  {

        Object myLock = new Object();
        Dimension size;
        Thread resizer = null;

        DelayedResizer() {
        }

        void addResizerThreadIfNeeded() {
            synchronized (myLock) {
                if (resizer != null) {
                    return;
                }
                
                this.resizer = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        boolean keepChecking = true;

                        while (keepChecking) {
                            
                            long now = System.currentTimeMillis();
                            if (now - lastChange.get() >= RESIZE_DELAY_INTERVAL_MILLISECS) {
                                keepChecking = false;
                            }
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                            }
                            
                        } /* while */
                        
                        synchronized (myLock) {
                            eventBus.raiseEvent( new TerminalEvent<Dimension>(this, TerminalEvent.TYPE.RESIZE, new DelayedDimensionProperties(size)) );
                            TerminalResizeEventGenerator.this.lastChange.set(System.currentTimeMillis());
                            eventBus.enableIdleEvents();
                            DelayedResizer.this.resizer = null;   // get rid of thread - its task is done..
                        }
                    }
                });

                this.resizer.start();
            }
        }


        synchronized public void addDelayResize(Dimension size) {
            synchronized (myLock) {
                this.size = size;
                TerminalResizeEventGenerator.this.lastChange.set(System.currentTimeMillis());
                addResizerThreadIfNeeded();
            }
        }

        
    }
    
    public TerminalResizeEventGenerator(TerminalScreen screen, EventBus eventBus) {
        super();
        this.screen = screen;
        this.eventBus = eventBus;
    }

    @Override
    public void run() {
        TerminalDimensions size = TerminalDimensions.create();
        
        if (lastSize.isSameDimension(size)) {
            return ;
        }
        
        this.lastSize = size;
        eventBus.disableIdleEvents();

        delayedResizer.addDelayResize(lastSize);
        
        eventBus.disableIdleEvents();
    }
    
    
    static class DelayedDimensionProperties implements Dimension {
        
        Dimension d;
        
        boolean recalced = false;
        
        DelayedDimensionProperties(Dimension size) {
            this.d = size;
        }
        
        private synchronized Dimension recalcDimension() {
            if (recalced) {
                return d;
            }
            TerminalDimensions size = TerminalDimensions.create();
            d = size;
            recalced = true;
            return d;
        }

        @Override
        public int getWidth() {
            recalcDimension();
            return d.getWidth();
        }

        @Override
        public int getHeight() {
            recalcDimension();
            return d.getHeight();
        }
        
        public String toString() {
           return  this.d.toString();
        }
        
    }
    
    public EventHandler asEventHandler() {
        return new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                TerminalResizeEventGenerator.this.run();
            }
        };
    } 
}