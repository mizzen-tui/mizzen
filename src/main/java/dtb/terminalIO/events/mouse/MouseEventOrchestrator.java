package dtb.terminalIO.events.mouse;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.CPREvent;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler.HandleOnceHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.util.Logging;

/**
 * Enables/disables tracking of raw console mouse events and then translating them
 * to ApplicationMouseEvents 
 */
public class MouseEventOrchestrator {
    
    private TerminalScreen screen;
    private Canvas canvas;
    private EventBus eventBus;
    private ConditionalEventHandler mouseTrackingEventHandler;
    
    /**
     * MouseEventOrchestrator  
     * @param screen
     * @param canvas
     * @param eventBus
     */
    public MouseEventOrchestrator(TerminalScreen screen, Canvas canvas, EventBus eventBus) {
        super();
        this.screen = screen;
        this.canvas = canvas;
        this.eventBus = eventBus;
        this.mouseTrackingEventHandler = null;
    }

    private static IS_Event_TYPE canHandle_CPR_Event = new IS_Event_TYPE(TYPE.CPR_REPORT);
    
    private static class HandleOnceCPREvent extends HandleOnceHandler {
        public HandleOnceCPREvent(EventHandler handler) {
            super(new ConditionalEventHandler.Impl(canHandle_CPR_Event, handler));
        }
    }
    
    public void enableMouseHandling() {
        
        if (mouseTrackingEventHandler != null) {
            return;
        }
        
        // setup event handlers
        
        // when mouse handling is enabled, we need to capture raw console events, which contain the
        // full console row/col coordinates of the mouse click..
        // then we need to map that row/col to the row/col within the application canvas - this is 
        // referred to as the applicationMousePosition ..  if the canvas is not full screen, then 
        // applicationMousePosition will be different from console row/col position.

        IS_Event_TYPE canHandleRAWMouseEventPredicate = new IS_Event_TYPE(TYPE.RAW_MOUSE);

        // capture RAW_MOUSE console events
        mouseTrackingEventHandler = new ConditionalEventHandler.Impl( canHandleRAWMouseEventPredicate,
                  (te)->{
                      RawParsedFromConsoleMouseEvent rme = (RawParsedFromConsoleMouseEvent) te;
                      
                      // (1) now let's move the screen cursor position to its top-left ( its 0,0 )
                      boolean erasePreviousContent = false;
                      screen.getEmitter().clearLastTrackedLines(erasePreviousContent);

                      // (2) now let's ask the terminal its current cursor position - we get the result
                      // in an event-handler ( HandleOnceCPREvent )
                      screen.add(ControlCode.CPR);
                      screen.refresh();
                      te.getEventBus().appendEventHandler(new HandleOnceCPREvent((te2)->{
                          CPREvent cpr = (CPREvent) te2;
                          
                          // (3) now we have the Cursor Position Report (CPR) ( note: CPR reports 1,1 as top-left )
                          //  let's map the raw mouse position to the application position 
                          Position applicationMousePosition = cpr.getEventData().offsetAdd(-1, -1).negate().offsetAdd( rme.getEventData() );

                          Logging.info("------ mouse captured: ---------------");
                          Logging.info("mouse info: " + String.valueOf(rme));
                          Logging.info("current cursor pos: " + String.valueOf(cpr));
                          Logging.info("current app mouse pos: " + String.valueOf(applicationMousePosition));

                          if (this.canvas.asRectangle().containsPosition(applicationMousePosition)) {
                              // (4) raise an ApplicationMouseEvent event only if within canvas rectangle
                              ApplicationMouseEvent aMe = new ApplicationMouseEvent( rme.getSource() , rme.getMouseAction(), applicationMousePosition);
                              te2.getEventBus().raiseEvent( aMe );
                              Logging.info("app mouse info:: " + String.valueOf(aMe));
                          }
                          Logging.info("--------------------------------------");
                      }));
                      
                  });

        eventBus.appendEventHandler( mouseTrackingEventHandler );
        screen.add( ControlCode.ENABLE_MOUSE_TRACKING );
        screen.refresh();
    }

    public void disableMouseHandling()  {
        
        // disable event handlers
        screen.add( ControlCode.DISABLE_MOUSE_TRACKING );
        screen.refresh();
        
        if (mouseTrackingEventHandler != null) {
            
            eventBus.removeEventHandler(mouseTrackingEventHandler);
            mouseTrackingEventHandler = null;
            
            return;
        }
        
    }

}
