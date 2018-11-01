package dtb.terminalIO.events;

import dtb.terminalIO.TerminalScreen;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.TerminalDimensions;

public class TerminalResizeEventGenerator implements Runnable {

    Dimension lastSize = Dimension.ZERO;

    long lastChange = 0;
    
    TerminalScreen screen;
    EventBus eventBus;
    
    public TerminalResizeEventGenerator(TerminalScreen screen, EventBus eventBus) {
        super();
        this.screen = screen;
        this.eventBus = eventBus;
    }

    @Override
    public void run() {
        TerminalDimensions size = TerminalDimensions.create();
        
        if (lastSize.isSameDimension(size) || lastSize == Dimension.ZERO) {
            lastSize = size;
            return ;
        }
        
        screen.getEmitter().clearLastTrackedLines();
        
        // todo ..  possibly need to come up with a way to slow down many call resize event firings
        //          for now managing this by throttling the polling interval

        long now = System.currentTimeMillis();
        
        eventBus.disableIdleEvents();

        eventBus.addEvent( new TerminalEvent<Dimension>(this, TerminalEvent.TYPE.RESIZE, size) );
        
        lastChange = now;
        
        lastSize = size;
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