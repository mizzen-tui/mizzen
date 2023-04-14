package dtb.terminalIO.events.resize;

import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.OnIdleEventEventHandler;

/**
 * Fires Terminal resize events when it has detected that the terminal has resized.
 * 
 * <p/>
 * 
 * It detects that the terminal has resized via polling.  Intermittent calls are made 
 * to the method TerminalDimensions.create() to get current terminal dimensions.  If the dimensions
 * have changed then it fires off a TerminalEvent.TYPE.RESIZE event.
 * 
 * <p/>
 * 
 * This is *not* the preferred way to detect terminal size changes.
 *  
 */
public class TerminalResizePoller {

    private static int POLLING_CHECK_RESIZED_INTERVAL = 1000;  // 1 second
    
    private static boolean setup = false;
    
    public static void startPollingForTerminalResize(TerminalScreen screen, EventBus eventBus) {
    startPollingForTerminalResize(screen, eventBus, POLLING_CHECK_RESIZED_INTERVAL);
    }
    
    public static void startPollingForTerminalResize(TerminalScreen screen, EventBus eventBus, int pollingInterval) {

    if (setup) return;

    setup = true;

    // uses an OnIdleEventEventHandler to watch for Terminal resizes .. if a resize is detected,
    // it fires off TerminalEvent.TYPE.RESIZE events that other handlers can then deal with.

    eventBus.appendEventHandler( new OnIdleEventEventHandler.RepeatingInterval(pollingInterval, 
                                                                               new TerminalResizeEventGenerator(screen, eventBus).asEventHandler()));	
    }

}
