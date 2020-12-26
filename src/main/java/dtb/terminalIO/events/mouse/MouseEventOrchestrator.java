package dtb.terminalIO.events.mouse;

import java.util.function.BiFunction;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.input.Clickable;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.CPREvent;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler.HandleOnceHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;

/**
 * Enables/disables tracking of raw console mouse events and then translating them
 * to ApplicationMouseEvents and then mapping these mouse clicks to "Focusable" components
 * on the canvas.
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

    public void enableMouseHandling(Component<?> rootComponent) {
        enableMouseHandling();
        appendApplicationMouseEventToClickable(rootComponent, null);
    }

    public void enableMouseHandling() {

        if (mouseTrackingEventHandler != null) {
            return;
        }

        // setup event handlers

        // when mouse handling is enabled, we need to capture raw console mouse events, which contain the
        // full console row/col coordinates of the mouse click..
        // then we need to map that row/col to the row/col within the application canvas - this is 
        // referred to as the applicationMousePosition ..  if the canvas is not full screen, then 
        // resulting applicationMousePosition will be different from console row/col position.

        IS_Event_TYPE canHandleRAWMouseEventPredicate = new IS_Event_TYPE(TYPE.RAW_MOUSE);

        // capture RAW_MOUSE console events
        mouseTrackingEventHandler = new ConditionalEventHandler.Impl( canHandleRAWMouseEventPredicate,
                  (te)->{
                      RawParsedFromConsoleMouseEvent rme = (RawParsedFromConsoleMouseEvent) te;

                      Position lastRequestedCursorPosition = canvas.getLastRequestCursorPosition();

                      // (1) now let's move the screen cursor position to its top-left ( its 0,0 )
                      screen.home();

                      // (2) now let's ask the terminal its current cursor position - we get the result
                      // in an event-handler ( HandleOnceCPREvent )
                      screen.add(ControlCode.CPR);
                      screen.refresh();
                      te.getEventBus().appendEventHandler(new HandleOnceCPREvent((te2)->{
                          CPREvent cpr = (CPREvent) te2;

                          // (3) now we have the Cursor Position Report (CPR) ( note: CPR reports 1,1 as top-left )
                          // let's map the raw mouse position to the application position 
                          Position applicationMousePosition = cpr.getPosition().offsetAdd(-1, -1).negate().offsetAdd( rme.getEventData() );

                          Logging.info("------ mouse captured: ---------------");
                          Logging.info("mouse info: " + String.valueOf(rme));
                          Logging.info("current cursor pos: " + String.valueOf(cpr));
                          Logging.info("current app mouse pos: " + String.valueOf(applicationMousePosition));

                          if (lastRequestedCursorPosition != null) {
                              screen.showCursorAt(lastRequestedCursorPosition);
                              screen.refresh();
                          }

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
        // add enable mouse control code to screen
        screen.add( ControlCode.ENABLE_MOUSE_TRACKING );
        // now flush to stdout
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
        if (appMouseToFocusableComponent != null) {

            eventBus.removeEventHandler(appMouseToFocusableComponent);
            appMouseToFocusableComponent = null;

            return;
        }

    }

    ConditionalEventHandler appMouseToFocusableComponent = null;

    /**
     * Builds an eventHandler that accepts ApplicationMouseEvent events and tries very hard to
     * map this event to an appropriate Component object somewhere on the mainApplicationPanel.
     * It determines the clicked component by mapping row/col mouse event with component row/col
     * and if the component is clickable.
     */
    public void appendApplicationMouseEventToClickable(Component<?> mainApplicationPanel, MOUSE_ACTION mouseAction) {
        // for now add to eventbus - but later - instead add to chained event handler

        if (appMouseToFocusableComponent != null) {
            return; // TODO: throw an exception?  illeglState?
        }

        if (mainApplicationPanel == null) {
            throw new IllegalArgumentException("mainApplicationPanel null - cannot append ApplicationMouseEventHandler");
        }

        if (mouseAction == null) {
            mouseAction = MOUSE_ACTION.DOWN_LEFT;
        }

        ConditionalEventHandler appMouseToFocusableComponent = new ConditionalEventHandler() {

            IS_Event_TYPE appMouseType = new IS_Event_TYPE(TYPE.APPLICATION_MOUSE);

            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                if (appMouseType.test(te)) {
                    return this;
                }
                return null;
            }

            @Override
            public void handle(TerminalEvent<?> te) {
                ApplicationMouseEvent appMouseEvent = (ApplicationMouseEvent) te;

                if (appMouseEvent.getMouseAction() != MOUSE_ACTION.DOWN_LEFT) {
                    return;
                }

                Position canvasPosition = appMouseEvent.getEventData();

                // mainApplicationPanel is not always at 0,0 ..always need to account for that
                Position panelOffsetOnCanvas = Component.UTIL.getCanvasOffsetFor(mainApplicationPanel);
                if (panelOffsetOnCanvas == null) {
                    throw new IllegalStateException("mainApplicationPanel used to map mouse events is not attached (directly or indirectly) to the canvas!");
                }
                Position mousePositionAdjustedForPanel = canvasPosition.offsetAdd( panelOffsetOnCanvas.negate());

                Component<?> componentClicked = GetComponentAtOffset.UTIL.getComponentAtOffset( mainApplicationPanel, mousePositionAdjustedForPanel );

                // componentClicked - might not be a Focusable .. so go up its ancestors
                // and find an ancestor that is focusable

                BiFunction<Component<?>,Boolean,Component<?>> findFocusableParent = (c,isClickableRequired) -> {
                    while ( c != null ) {
                        if (ToFocusable.UTIL.isFocusable(c)) {
                            if ( ! isClickableRequired ) {
                                return c;
                            }
                            if (c instanceof Clickable) {
                                return c;
                            }
                        }
                        c = (Component<?>)c.getParentContainer();
                    }
                    return c;
                };

                componentClicked = findFocusableParent.apply(componentClicked, false);

                if (componentClicked == null) {
                    return;
                }

                Focusable focusComponent = ToFocusable.UTIL.toFocusable(componentClicked);

                if (focusComponent != null) {
                    eventBus.raiseEvent( new RequestFocusEvent(focusComponent));
                    componentClicked = findFocusableParent.apply(componentClicked, true);
                    if (componentClicked instanceof Clickable) {
                        Clickable clickable = (Clickable)componentClicked;
                        Focusable focusableClickable = ToFocusable.UTIL.toFocusable(componentClicked);
                        if (focusableClickable == null) {
                            return;
                        }
                        eventBus.appendEventHandler( new OnIdleEventEventHandler.OneTime((te2)-> {
                            if (focusComponent.hasFocus()) {
                                int x = 2;
                            }
                            if (focusableClickable.hasFocus()) {
                                // map mouse position as an offset into the clicked component
                                // and pass the component a new mouse event with this offset
                                Position clickableOffsetOnCanvas = Component.UTIL.getCanvasOffsetFor((Component<?>)clickable);
                                if (clickableOffsetOnCanvas != null) {
                                    Position mouseOffsetIntoClickable = clickableOffsetOnCanvas.offsetTo(canvasPosition);
                                    ApplicationMouseEvent newMouseEvent = new ApplicationMouseEvent(appMouseEvent.getSource(), appMouseEvent.getMouseAction(), mouseOffsetIntoClickable);
                                    newMouseEvent.setEventBus(te2.getEventBus());
                                    clickable.onClick(newMouseEvent);
                                    return;
                                }
                                // can't map to an offset into component so just send the eventbus
                                clickable.onClick( appMouseEvent.getEventBus() );
                            }
                        })); 
                    }
                }
            }
        };
        eventBus.appendEventHandler(appMouseToFocusableComponent);
    }

}
