package dtb.terminalIO.app;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import dtb.terminalIO.components.CanResize;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
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
    BiConsumer<TUIApp, TerminalEvent<?>> exceptionHandler = new DefaultExceptionAction();
    Exception lastException = null;
    Consumer<TUIApp> terminateAction = new DefaultTerminateAction();

    Consumer<TUIApp> onReadyStartAction = null;
    BiConsumer<TUIApp, Dimension> onResizeAction = null;
    
    String confirmApplicationExitMessage = null;  

    Runnable refreshCanvasIfModified;

    KeyStroke terminateAppHotKey = Keys.CTRL_C_KEYSTROKE;

    KeyStroke noGraphicsToggleHotKey = Keys.CTRL_X_KEYSTROKE;
    
    ChildComponentManager<?> mainApplicationPanel;
    
    boolean started = false;

    public TUIApp() {
        super();
    }
    
    public TUIApp(String[] args) {
        this();
        initFromArgs(args);
    }
    
    public TUIApp initFromArgs(String[] args) {
        screen = TestsCommon.getTerminalScreenForTesting(args);
        master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
        master_canvas.setEmptyChar(' ');
        return this;
    }

    public Dimension getDimensions() {
        return getCanvas().asRectangle();
    }

    public Canvas getCanvas() {
        return master_canvas;
    }

    public TerminalScreen getScreen() {
        return screen;
    }

    public EventBus getEventBus() {

//        if ( ! started ) {
//            throw new IllegalStateException("Cannot access EventBus until after you call app.start()");
//        }

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

    public TUIApp setMainApplicationPanel(ChildComponentManager<?> mainApplicationPanel) {
        this.mainApplicationPanel = mainApplicationPanel;
        return this;
    }

    public TUIApp setDefaultMainApplicationPanel() {
        return setMainApplicationPanel(  new Composite().setBounds(this.getCanvas()).setEmptyChar(this.getCanvas().getEmptyChar()));
    }

    public ChildComponentManager<?> getMainApplicationPanel() {
        return this.mainApplicationPanel;
    }
    
    public TUIApp confirmOnExit(String confirmApplicationExitMessage) {
        if (this.mainApplicationPanel == null) {
            throw new IllegalStateException("Cannot call confirmOnExit() yet.  First call TUIApp.setMainApplicationPanel()");
        }
        this.confirmApplicationExitMessage = confirmApplicationExitMessage;
        return this;
    }
    
    public TUIApp confirmOnExit() {
        return this.confirmOnExit("Exit application?");
    }
    
    public void setUpdateCanvas(Function<TUIApp, Boolean> updateCanvas) {
        this.updateCanvas = updateCanvas;
    }

    public void setTerminateHandler(Consumer<TUIApp> terminateAction) {
        this.terminateAction = terminateAction;
    }

    public TUIApp setOnReadyStart(Consumer<TUIApp> onReadyStartAction) {
        this.onReadyStartAction = onReadyStartAction;
        return this;
    }

    public TUIApp setOnResizeAction(BiConsumer<TUIApp, Dimension> resizeAction) {
        this.onResizeAction = resizeAction;
        return this;
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

    public TUIApp setCanvasSize(Dimension size) {
        Dimension curSize = getCanvas().asRectangle();
        if (this.started && !size.contains(curSize)) { // new size is smaller - so clear
            if (getCanvas().segmentsSize() > 0) {
                getCanvas().clearCanvas();
                getCanvas().refresh();
            }
        }
        getCanvas().setSize(size.getHeight(), size.getWidth());

        if (this.started) {
            getCanvas().refresh();
        }
        
        return this;
    }

    public TUIApp setCanvasSize(int height, int width) {
        return setCanvasSize(new Dimension.Simple(height, width));
    }
    
    public TUIApp clearCanvas() {
        getCanvas().clearCanvas();
        return this;
    }
    
    public TUIApp setEmptyChar(char emptyChar) {
        this.getCanvas().setEmptyChar(emptyChar);
        return this;
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
            // confirm application exit
            MessageBox.showYesNoDialog(te.getEventBus(), 
                                       this.mainApplicationPanel, 
                                       this.confirmApplicationExitMessage, 
                                       raiseTerminateEvent);
        } else {
            // kill the app by signaling to interested handlers that we are going down
            raiseTerminateEvent.handle(te);
        }
    }

    public void start() {

        this.started = true;
        
        // We do an initial refresh before going into RAW mode (caused by
        // makeEventBus())
        // in order that canvas is correctly refreshed after current screen cursor
        // position.  This is especially useful when the canvas is not full screen and
        // you want the canvas position just below last prompt on the terminal
        getCanvas().refresh();

        if (this.eventBus == null) {
            makeEventBus();
        }

        screen.add(ControlCode.HIDE_CURSOR);
        screen.refresh();

        eventBus.appendEventHandler(new KeyInputEventHandler(terminateAppHotKey,
                                                             (te) -> onClick_terminateAppHotKey(te) ));

        makeRefreshCanvasIfModified();

        // if updateCanvas not specified by client app, then let's try to make our own
        if (updateCanvas == null && mainApplicationPanel != null && mainApplicationPanel instanceof Component) {
            Component<?> __mainPanel = (Component<?>) mainApplicationPanel;

            this.setUpdateCanvas((tApp)->{
                if (!__mainPanel.isDirty()) {
                    return false; // false means no gui update
                }

                tApp.getCanvas().addComponentSegments( __mainPanel );
 
                return true; // true means updated
            });
        }

        Runnable updateThenForceRefresh = () -> {
            updateCanvas.apply(this);
            getCanvas().refresh();
        };

        if (noGraphicsToggleHotKey != null) {
            // TODO: -move addNoGraphicsToggleEvent(..) outside of testsCommon
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

        // Do we have an onResizeAction defined? If no - let's make one
        // TODO:  this needs a LOT of work - try out ButtonBar test to see what I mean..
        if (this.onResizeAction == null  && mainApplicationPanel != null && mainApplicationPanel instanceof CanResize) {
            
            CanResize __mainPanel = (CanResize) mainApplicationPanel;
            
            this.setOnResizeAction( (tApp, size)->{

                tApp.clearCanvas();
                tApp.setCanvasSize(size);

                if (__mainPanel instanceof Dimension ) {
                    if ( ! tApp.getCanvas().contains((Dimension)__mainPanel)) {
                        __mainPanel.resize(tApp.getCanvas());
                    } else {
                        __mainPanel.resize(tApp.getCanvas());
                    }
                } else {
                    __mainPanel.resize(tApp.getCanvas());
                    if (__mainPanel instanceof Composite) {  // this is a hack - fix composite resize!
                        ((Composite)__mainPanel).fullrecalcDim();
                    }
                }
            } );
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