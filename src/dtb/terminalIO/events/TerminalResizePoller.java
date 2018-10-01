package dtb.terminalIO.events;

import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.TerminalDimensions;

/**
 * Fires Terminal resize events when it has detected that the terminal has resized.
 * 
 * <p/>
 * 
 * It detects that the terminal has resized via polling.  Intermittently calls are made 
 * to the method TerminalDimensions.create() to get current terminal dimensions.  If the dimensions
 * have changed then it fires off a TerminalEvent.TYPE.RESIZE event.
 *  
 */
public class TerminalResizePoller {

    private static int POLLING_CHECK_RESIZED_INTERVAL = 1000;  // 1 second
    
    private static boolean setup = false;
    
    public static void startPollingForTerminalResize(EventBus eventBus) {
	startPollingForTerminalResize(eventBus, POLLING_CHECK_RESIZED_INTERVAL);
    }
    
    public static void startPollingForTerminalResize(EventBus eventBus, int pollingInterval) {
	
	if (setup) return;  
	setup = true;
	
	// uses an OnIdleEventEventHandler to watch for Terminal resizes .. if a resize is detected,
	// it fires off TerminalEvent.TYPE.RESIZE events that other handlers can then deal with.
        eventBus.addEventHandler( new OnIdleEventEventHandler.RepeatingInterval(pollingInterval, new EventHandler() {

            Dimension lastSize = Dimension.ZERO;

            long lastChange = 0;
            
            @Override
            public void handle(TerminalEvent<?> te) {
                TerminalDimensions size = TerminalDimensions.create();
                
                if (lastSize.isSameDimension(size)) {
                    return ;
                }
                
                // todo ..  possibly need to come up with a way to slow down many call resize event firings
                //          for now managing this by throttling the polling interval

                long now = System.currentTimeMillis();

                eventBus.addEvent( new TerminalEvent<Dimension>(this, TerminalEvent.TYPE.RESIZE, size) );
                
                lastChange = now;
                
                lastSize = size;

            }}) );
    }

}
