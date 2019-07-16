package dtb.terminalIO.app;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.modal.MessageBox;
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
    Function<TUIApp, Boolean> updateCanvas;
    BiConsumer<TUIApp, TerminalEvent<?>> exceptionHandler = new DefaultExceptionHandler();
    Exception lastException = null;
    Consumer<TUIApp> terminateAction = new DefaultTerminateAction();

    Consumer<TUIApp> onReadyStartAction = null;
    BiConsumer<TUIApp, Dimension> onResizeAction = null;
    
    String confirmApplicationExitMessage = null;  

    Runnable refreshCanvasIfModified;

    KeyStroke terminateAppHotKey = Keys.CTRL_C_KEYSTROKE;

    KeyStroke noGraphicsToggleHotKey = Keys.CTRL_X_KEYSTROKE;
    
    ChildComponentManager<?> mainApplicationPanel;

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
        if (eventBus == null) {
            makeEventBus();
        }
        return eventBus;
    }

    public void runUIAction(Runnable task) {
        getEventBus().appendEventHandler(new OnIdleEventEventHandler.OneTime(
                ()-> {
                    task.run();
                    doRefreshCanvasIfModified();
                }
        ));
    }

    public void setMainApplicationPanel(ChildComponentManager<?> mainApplicationPanel) {
        this.mainApplicationPanel = mainApplicationPanel;
    }
    
    public void confirmOnExit(String confirmApplicationExitMessage) {
        if (this.mainApplicationPanel == null) {
            throw new IllegalStateException("Cannot call confirmOnExit() yet.  First call TUIApp.setMainApplicationPanel()");
        }
        this.confirmApplicationExitMessage = confirmApplicationExitMessage;
    }
    
    public void confirmOnExit() {
        this.confirmOnExit("Exit application?");
    }
    
    
    public void setUpdateCanvas(Function<TUIApp, Boolean> updateCanvas) {
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
            return;
        }
    }

    public void setCanvasSize(Dimension size) {
        Dimension curSize = getCanvas().asRectangle();
        if (!size.contains(curSize)) { // new size is smaller - so clear
            if (getCanvas().segmentsSize() > 0) {
                getCanvas().clearCanvas();
                getCanvas().refresh();
            }
        }
        getCanvas().setSize(size.getHeight(), size.getWidth());
        getCanvas().refresh();
    }

    private void makeRefreshCanvasIfModified() {
        refreshCanvasIfModified = () -> {
            boolean result = (updateCanvas == null) ? true : updateCanvas.apply(this);
            if (result) {
                getCanvas().refresh();
            }
        };
    }

    public void doRefreshCanvasIfModified() {
        if (this.refreshCanvasIfModified == null) {
            makeRefreshCanvasIfModified();
        }
        refreshCanvasIfModified.run();
    }
    
    private void onClick_terminateAppHotKey(TerminalEvent<?> te) {
        EventHandler raiseTerminateEvent = (e)-> e.getEventBus().raiseEvent(new TerminalEvent<Void>(e.getEventBus(), TYPE.TERMINATE, null));
        if (this.confirmApplicationExitMessage != null) {
            MessageBox.showYesNoDialog(te.getEventBus(), 
                                       this.mainApplicationPanel, 
                                       this.confirmApplicationExitMessage, 
                                       raiseTerminateEvent);
        } else {
            raiseTerminateEvent.handle(te);
        }
    }

    public void start() {

        // We do an initial refresh before going into RAW mode (caused by
        // makeEventBus())
        // in order that canvas is correctly refresh after current screen cursor
        // position
        getCanvas().refresh();

        if (this.eventBus == null) {
            makeEventBus();
        }

        screen.add(ControlCode.HIDE_CURSOR);
        screen.refresh();

        eventBus.appendEventHandler(new KeyInputEventHandler(terminateAppHotKey,
                (te) -> onClick_terminateAppHotKey(te) ));

        makeRefreshCanvasIfModified();

        Runnable updateThenForceRefresh = () -> {
            updateCanvas.apply(this);
            getCanvas().refresh();
        };

        if (noGraphicsToggleHotKey != null) {
            // todo -move addNoGraphicsToggleEvent(..) outside of testsCommon
            TestsCommon.addNoGraphicsToggleEvent(eventBus, screen, updateThenForceRefresh);
        }

        eventBus.appendEventHandler(master_canvas.asShowCursorEventHandler());
        eventBus.appendEventHandler(master_canvas.asHideCursorEventHandler());

        TerminalResizeDetection.initiate(screen, eventBus);

        if (this.updateCanvas != null) {
            eventBus.appendEventHandler(new OnRequestUpdateCanvasEventHandler(refreshCanvasIfModified));
        }

        if (terminateAction != null) {
            eventBus.appendEventHandler(new OnTerminateEventHandler(() -> terminateAction.accept(this)));
        }

        if (this.updateCanvas != null) {
            eventBus.appendEventHandler(new OnIdleEventEventHandler.OneTime(refreshCanvasIfModified));
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

                if (!size.contains(getCanvas())) {
                    // canvas too big (onResizeAction didn't adjust it properly) - let's fix
                    master_canvas.setSize(size.getHeight(), size.getWidth());
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