package dtb.terminalIO.app;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.OnRequestUpdateCanvasEventHandler;
import dtb.terminalIO.events.OnResizeEventHandler;
import dtb.terminalIO.events.OnTerminateEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyInputEventsProducer;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.resize.TerminalResizeDetection;
import dtb.terminalIO.examples.TestsCommon;
import dtb.terminalIO.geom.Dimension;

public class TUIApp {

    EventBus eventBus;
    Canvas master_canvas;
    TerminalScreen screen;
    Function<TUIApp,Boolean> updateCanvas;
    BiConsumer<TUIApp, TerminalEvent<?>> exceptionHandler = new DefaultExceptionHandler();
    Consumer<TUIApp> terminateAction = new DefaultTerminateAction();
    
    Consumer<TUIApp> onReadyStartAction = null;
    BiConsumer<TUIApp, Dimension> onResizeAction = null;
    
    KeyStroke terminateAppHotKey = Keys.CTRL_C_KEYSTROKE;

    KeyStroke noGraphicsToggleHotKey = Keys.CTRL_X_KEYSTROKE;

    public TUIApp initFromArgs(String[] args) {
        screen = TestsCommon.getTerminalScreenForTesting(args);
        master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
        master_canvas.setEmptyChar(' ');
        return this;
    }

    public Dimension getDimensions() {
        return master_canvas.asRectangle();
    }

    public Canvas getCanvas() {
        return master_canvas;
    }

    public TerminalScreen getScreen() {
        return screen;
    }

    public EventBus getEventBus() {
        if (eventBus == null) 
        {
            makeEventBus();
        }
        return eventBus;
    }
    
    

    public void setUpdateCanvas(Function<TUIApp,Boolean> updateCanvas) {
        this.updateCanvas = updateCanvas;
    }

    public void setTerminateHandler(Consumer<TUIApp> terminateAction) {
        this.terminateAction = terminateAction;
    }

    public void setOnReadyStart(Consumer<TUIApp> onReadyStartAction) {
        this.onReadyStartAction = onReadyStartAction;
    }
    
    public void setOnResizeAction(BiConsumer<TUIApp, Dimension> resizeAction) {
        this.onResizeAction = resizeAction;
    }


    private void makeEventBus() {
        eventBus = new EventBus();
        try {
            new KeyInputEventsProducer(eventBus);
        } catch (Exception e) {
            eventBus.stopConsuming();
            return ;
        }
    }
    
    public void start() {
        
        if (this.eventBus == null) {
            makeEventBus();
        }

        screen.add(ControlCode.HIDE_CURSOR);
        screen.refresh();

        eventBus.appendEventHandler(new KeyInputEventHandler(terminateAppHotKey, (te)-> eventBus.raiseEvent( new TerminalEvent<Void>(eventBus, TYPE.TERMINATE, null) ) ));

        Runnable canvasUpdateThenRefresh = ()-> {
            boolean result = (updateCanvas==null)? true : updateCanvas.apply(this);
            if (result) {
                getCanvas().refresh();
            }
        };
        
        
        if (noGraphicsToggleHotKey != null) {
            // todo -move addNoGraphicsToggleEvent(..) outside of testsCommon
            TestsCommon.addNoGraphicsToggleEvent(eventBus, screen, canvasUpdateThenRefresh );
        }
        
        eventBus.appendEventHandler(master_canvas.asShowCursorEventHandler());
        eventBus.appendEventHandler(master_canvas.asHideCursorEventHandler());
        
        TerminalResizeDetection.initiate(screen, eventBus);
        
        if (this.updateCanvas != null) {
            eventBus.appendEventHandler(new OnRequestUpdateCanvasEventHandler(canvasUpdateThenRefresh));
        }

        if (terminateAction != null) {
            eventBus.appendEventHandler(new OnTerminateEventHandler(() -> terminateAction.accept(this)));
        }

        if (this.updateCanvas != null) {
            eventBus.appendEventHandler(new OnIdleEventEventHandler.OneTime(canvasUpdateThenRefresh));
        }

        if (exceptionHandler != null) {
            eventBus.appendEventHandler(
                    new ConditionalEventHandler.Impl(new IS_Event_TYPE(TYPE.EXCEPTION), new EventHandler() {
                        @Override
                        public void handle(TerminalEvent<?> te) {
                            exceptionHandler.accept(TUIApp.this, te);
                        }
                    }));
        }
        
        eventBus.appendEventHandler(new OnResizeEventHandler() {
            @Override
            public void handleResize(Dimension size) {
                
                if (onResizeAction != null) {
                    onResizeAction.accept(TUIApp.this, size);
                }
                
                if (! size.contains(getCanvas())) {
                    // canvas too big (onResizeAction didn't adjust it properly) - let's fix
                    master_canvas.setSize(size.getHeight() , size.getWidth() );
                }
                
                eventBus.raiseEvent(new RequestUpdateCanvasEvent(TUIApp.this));
            }
        });
        

        if (this.onReadyStartAction != null) {
            eventBus.appendIdleEventHandler(new OnIdleEventEventHandler.OneTime(new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    TUIApp.this.onReadyStartAction.accept(TUIApp.this);
                    eventBus.raiseEvent(new RequestUpdateCanvasEvent(TUIApp.this));
                }
            }));
        }
        
    }

}