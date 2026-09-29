package dtb.terminalIO.app;

import java.io.UnsupportedEncodingException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentFinder;
import dtb.terminalIO.components.ComponentInspector;
import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.WindowFrame;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.modal.MessageBox;
import dtb.terminalIO.components.scroll.FindScrollToViewHandler;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventBusImpl;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.OnRequestUpdateCanvasEventHandler;
import dtb.terminalIO.events.OnResizeEventHandler;
import dtb.terminalIO.events.OnTerminateEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.InputEventsProducer;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.MouseEventOrchestrator;
import dtb.terminalIO.events.resize.TerminalResizeDetection;
import dtb.terminalIO.examples.TestsCommon;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.ElapsedTime;

/**
 * 
 * Simplifies using this library GREATLY if you instantiate a TUIApp object
 * and interact with the components and the eventBus via this object.
 * 
 * <p/>
 * 
 * This library is finicky and the order in which things are set up matter.
 * 
 * <p/>
 * 
 * By using this class, it tries to help coerce the developer to do UI setups in the correct order.  (still a work in progress! :) )
 * 
 * <p/>
 * 
 * For some examples of its use.. check out:
 * 
 * </p>
 * 
 * <li>  SparklineTest
 * <li>  BarChartTest
 * <li>  CharacterViewerTest
 * <li>  RockerButtonTest
 * <li>  TabPanelTest
 * 
 * etc
 * 
 * @author dbalme
 *
 */
public class TUIApp implements ComponentFinder {

    EventBus eventBus;
    Canvas master_canvas;
    TerminalScreen screen;
    Function<TUIApp, Boolean> updateCanvas;
    BiConsumer<TUIApp, TerminalEvent<?>> exceptionHandler = new DefaultExceptionAction();
    Exception lastException = null;
    Consumer<TUIApp> terminateAction = new DefaultTerminateAction();
    MouseEventOrchestrator mouseEventOchestrator;
    
    Consumer<TUIApp> onReadyStartAction = null;
    BiConsumer<TUIApp, Dimension> onResizeAction = null;
    
    String confirmApplicationExitMessage = null;  

    Runnable refreshCanvasIfModifiedAction;

    KeyStroke terminateAppHotKey = Keys.CTRL_C_KEYSTROKE;
    KeyInputEventHandler terminateAppHotKeyEventHandler;

    KeyStroke noGraphicsToggleHotKey = Keys.CTRL_X_KEYSTROKE;
    
    ChildComponentManager<?> mainApplicationPanel;
    Component<?>  rootPanelForMouseEvents;   // usually rootPanelForMouseEvents and mainApplicationPanel are the same
    
    WindowFrame inspectorWindowFrame;

    InputEventsProducer eventsProducer = null;
    
    boolean verifyFocusables = true;
    
    Function<EventBus, InputEventsProducer> inputEventsProducerBuilder = (eventBus)-> {
        try {
            return new InputEventsProducer(eventBus);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    };

    boolean useMouse = true;

    boolean started = false;

    public TUIApp(Function<EventBus, InputEventsProducer> inputEventsProducerBuilder ) {
        super();
        this.inputEventsProducerBuilder = inputEventsProducerBuilder;
    }

    public TUIApp() {
        super();
    }

    public TUIApp(String[] args) {
        this();
        initFromArgs(args);
    }

    public TUIApp(Function<EventBus, InputEventsProducer> inputEventsProducerBuilder, String[] args) {
        this(inputEventsProducerBuilder);
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
    
    public TUIApp verifyFocusables(boolean verifyFocusables) {
        this.verifyFocusables = verifyFocusables;
        return this;
    }

    public Canvas getCanvas() {
        if (master_canvas == null) {
            throw new NullPointerException("Canvas not initialized.\n  Need to first call initFromArgs(args) or new TUIApp(args)");
        }
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

    public TUIApp setMainApplicationPanel(ChildComponentManager<?> mainApplicationPanel) {

        if (started) {
            throw new IllegalStateException("You must only call setMainApplicationPanel() before calling setOnReadyStart() and before calling start()");
        }

        // make sure size is not too large
        if ( ! this.getCanvas().asRectangle().contains(mainApplicationPanel)) {
            if (mainApplicationPanel instanceof CanResize) {
                Dimension newDimension = new Dimension.Simple(Math.min(mainApplicationPanel.getHeight(), getCanvas().getHeight()),
                                                              Math.min(mainApplicationPanel.getWidth() , getCanvas().getWidth()));
                ((CanResize)mainApplicationPanel).resize(newDimension);
            } else {
                throw new IllegalStateException("You have attempted to assign a mainApplicationPanel "
                                                + "that is larger than the canvas and does not implement CanResize. "
                                                + "Class = " + mainApplicationPanel.getClass().getName());
            }
        }

        this.mainApplicationPanel = mainApplicationPanel;

        HasName.UTIL.setDefaultNameFromParentObject(this.mainApplicationPanel, this, "mainPanel");
        
        return this;
    }
    
    static  IS_Event_TYPE IS_KEYPRESS = new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS);

    
    static class TUIDefaultPanel extends CompositeWithFocusManager {
        TUIDefaultPanel(){
            super();
        }
        
        @Override
        public EventHandler canHandle(TerminalEvent<?> te)
        {
            if (IS_KEYPRESS.test(te)) {
                return super.canHandle(te);
            }
            return super.canHandle(te);
        }
        
        @Override
        public void handle(TerminalEvent<?> te)
        {
            super.handle(te);
        }
        
    }

    public TUIApp setDefaultMainApplicationPanel() {
        return setMainApplicationPanel(  new TUIDefaultPanel()
                                                 .setBounds(this.getCanvas())
                                                 .setEmptyChar(this.getCanvas().getEmptyChar()));
    }

    public ChildComponentManager<?> getMainApplicationPanel() {
        if (this.mainApplicationPanel == null) {
            throw new NullPointerException("\n\nError calling getMainApplicationPanel():\n"+
                                           "  mainApplicationPanel is not yet defined.\n"+
                                           "   You must first call either ..\n"+
                                           "      1) setMainApplicationPanel(..) ..\n"+
                                           "        or ..\n"+
                                           "      2) setDefaultMainApplicationPanel()\n"
                                           );
                                            
        }
        return this.mainApplicationPanel;
    }

    @Override
    public Component<?> findFirstComponentForCondition(Function<Component<?>, Boolean> condition) {
        return getMainApplicationPanel().findFirstComponentForCondition(condition);
    }

    public Component<?> findComponentByName(String name) {
        return getMainApplicationPanel().findComponentByName(name);
    }

    public <T> T findComponentByName(String name, Class<T> clazz) {
        return getMainApplicationPanel().findComponentByName(name, clazz);
    }

    public TUIApp confirmOnExit(String confirmApplicationExitMessage) {
        if (this.mainApplicationPanel == null) {
            throw new IllegalStateException("Cannot call confirmOnExit() yet!\n  First call TUIApp.setMainApplicationPanel(..) or .. TUIApp.setDefaultMainApplicationPanel()");
        }
        this.confirmApplicationExitMessage = confirmApplicationExitMessage;
        return this;
    }

    public TUIApp confirmOnExit() {
        return this.confirmOnExit("Exit application?");
    }

    public KeyInputEventHandler getTerminateAppHotKeyEventHandler() {
        return terminateAppHotKeyEventHandler;
    }
    
    public Function<TUIApp, Boolean> getUpdateCanvas() {
        return this.updateCanvas;
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

    public TUIApp appendOnResizeAction(BiConsumer<TUIApp, Dimension> resizeAction) {

        BiConsumer<TUIApp, Dimension> previousOnResizeAction =   this.getResizeAction()!=null ? this.getResizeAction() : (t,s)->{};

        setOnResizeAction((tApp,size)->{
            previousOnResizeAction.accept(tApp, size);
            resizeAction.accept(tApp, size);
        });

        return this;
    }

    public BiConsumer<TUIApp, Dimension> getResizeAction() {
        return this.onResizeAction;
    }

    private void makeEventBus() {
        eventBus = new EventBusImpl();
        try {
            //new InputEventsProducer(eventBus);
            if (this.eventsProducer != null) {
                // TODO:  GOTTA HAZARD A GUESS:  But I dont think this will work properly because effects eventbus - need to test!!!
                this.eventsProducer.terminateReads();
            }
            this.eventsProducer = inputEventsProducerBuilder.apply(eventBus);
        } catch (Exception e) {
            eventBus.stopConsuming();
            return;
        }
    }

    public void setIdleEventStarvationLimit(int interval) {
        if (this.eventsProducer == null) {
            throw new IllegalStateException("ERROR in setIdleEventStarvationLimit()!  You cannot call setIdleEventStarvationLimit() until InputEventsProducer has been created.  Make the call in the onReadyStartAction callback. ");
        }
        this.eventsProducer.setIdleEventStarvationLimit(interval);
    }

    public TUIApp setCanvasSize(Dimension size) {
        Canvas canvas = getCanvas();
        Dimension curSize = canvas.asRectangle();
        if (this.started && !size.contains(curSize)) { // new size is smaller - so clear
            if (canvas.segmentsSize() > 0) {
                canvas.clearCanvas();
                canvas.refresh();
            }
        }
        canvas.setSize(size.getHeight(), size.getWidth());
        if (this.started) {
            canvas.refresh();
        }
        return this;
    }

    public TUIApp setCanvasSize(int height, int width) {
        Dimension desiredDimension = new Dimension.Simple(height, width);
        Dimension terminalDimension = TestsCommon.terminalSize();
        return setCanvasSize(terminalDimension.contains(desiredDimension)?desiredDimension:terminalDimension);
    }

    boolean canvasClearedAndNowEmpty = true;
    protected boolean inOnReadyStartAction = false;

    public TUIApp clearCanvas() {
        getCanvas().clearCanvas();
        canvasClearedAndNowEmpty = true;
        return this;
    }

    public TUIApp setEmptyChar(char emptyChar) {
        this.getCanvas().setEmptyChar(emptyChar);
        return this;
    }

    public char getEmptyChar() {
        return this.getCanvas().getEmptyChar();
    }

    public TUIApp setEmptyColors(Color emptyColor) {
        this.getCanvas().setEmptyColors(emptyColor);
        return this;
    }

    public Color getEmptyColor() {
        return this.getCanvas().getEmptyColor();
    }

    private void makeRefreshCanvasIfModifiedAction() {
        refreshCanvasIfModifiedAction = () -> {
            boolean canvasRefreshRequired = (updateCanvas == null) ? true : updateCanvas.apply(this);
            if (canvasRefreshRequired) {
                canvasClearedAndNowEmpty = false;
                getCanvas().refresh();
            }
        };
    }

    public void doRefreshCanvasIfModified() {
        if (this.refreshCanvasIfModifiedAction == null) {
            makeRefreshCanvasIfModifiedAction();
        }
        refreshCanvasIfModifiedAction.run();
    }

    // this gets call when user hits terminate app hot key -- usually CTRL-C
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

    public TUIApp enableMouseHandling() {
        this.useMouse = true;
        return this;
    }

    public TUIApp enableMouseHandling(Component<?> rootPanelForMouseEvents) {
        this.useMouse = true;
        this.rootPanelForMouseEvents = rootPanelForMouseEvents;
        return this;
    }

    public TUIApp enableComponentInspector() {
        return enableComponentInspector(null);
    }
    
    public TUIApp enableComponentInspector(KeyStroke toggleKey) {
        if (toggleKey == null) {
            toggleKey = Keys.CTRL_D_KEYSTROKE;
        }

        // Add a hotkey to toggle the visibility of the inspector window
        KeyInputEventHandler inspectorToggleHandler = new KeyInputEventHandler(toggleKey, (te) -> {
            if (inspectorWindowFrame != null) {
                String inspectorName = HasName.UTIL.getName(this.inspectorWindowFrame);
                if (this.findComponentByName(inspectorName) == null) {
                    Position startPos = ((Component<?>)getMainApplicationPanel())
                                                       .getPreferredRectangleForLayout()
                                                       .centered(inspectorWindowFrame);
                    getMainApplicationPanel().add(startPos, inspectorWindowFrame);
                } else {
                    getMainApplicationPanel().remove(inspectorWindowFrame);
                }
                te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
            } else {
                // First time request, so create a WindowFrame wrapping the ComponentInspector
                if (this.inspectorWindowFrame == null) {
                    this.inspectorWindowFrame = ComponentInspector.resizableInspector(this);
                    HasName.UTIL.setDefaultNameFromParentObject(this.inspectorWindowFrame, this, "ComponentInspector");
                    te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
                }
            }
        });

        // Add hotkeys for toggling view mode and refreshing the inspector
        getEventBus().appendEventHandler(inspectorToggleHandler);

        return this;
    }
    
    
    public FocusManager getApplicationFocusManager() {
        return getApplicationFocusManager(false || this.inOnReadyStartAction);
    }

    public void rebuildFocusManager() {
        getApplicationFocusManager(true, verifyFocusables); 
    }

    protected FocusManager getApplicationFocusManager(boolean forceRebuild) {
        return getApplicationFocusManager(forceRebuild, verifyFocusables); 
    }

    protected FocusManager getApplicationFocusManager(boolean forceRebuild, boolean verifyFocusables) {

        if (TUIApp.this.eventBus == null) {
            //throw new IllegalStateException("TUIApp.getApplicationFocusManager() ERROR!.  Cannot call getApplicationFocusManager() before the eventbus has been created!");
            getEventBus();
        }
        FocusManager mainFocusManager = null;
        boolean addToEventBusRequired = false;
        
        if (this.getMainApplicationPanel() != null && this.getMainApplicationPanel() instanceof FocusManager) {
            mainFocusManager = (FocusManager) this.getMainApplicationPanel();
            final FocusManager focusManageToSearchFor = mainFocusManager;
            addToEventBusRequired = null == TUIApp.this.eventBus.findFirstEventHandlerOfType((cH) -> cH == focusManageToSearchFor );
        } else {
            // main panel is not a focus manager so let's look for the first one on the eventbus and use it
            mainFocusManager = (FocusManager) TUIApp.this.eventBus.findFirstEventHandlerOfType((cH) -> cH instanceof FocusManager );
        }

        if (mainFocusManager != null) {
            if (forceRebuild && mainFocusManager instanceof CompositeWithFocusManager) {
                ((CompositeWithFocusManager)mainFocusManager).rebuildFocusManager();
            }
            if (addToEventBusRequired) {
                return buildAndAttachFocusManager(verifyFocusables);
            }
            return mainFocusManager;
        }
        if (forceRebuild && this.getMainApplicationPanel() instanceof CompositeWithFocusManager) {
            ((CompositeWithFocusManager)this.getMainApplicationPanel()).rebuildFocusManager();
        }

        return buildAndAttachFocusManager(verifyFocusables);
    }
    
    private FocusManager buildAndAttachFocusManager(boolean verifyFocusables) {
        FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(TUIApp.this.getMainApplicationPanel(),
                                                                                         TUIApp.this.getEventBus(),
                                                                                         ()->new DefaultFocusManager()
                                                                                                    .setNameFromOwner(TUIApp.this.getMainApplicationPanel())
                                                                                                    .setNavKeysFirst(false));
        if (mainFocusManager != null) {
            HasName.UTIL.setDefaultNameFromParentObject(mainFocusManager, this, "mainFocusManager");
            TUIApp.this.getEventBus().appendEventHandler(  mainFocusManager );
        }

     // sanity test
        if (verifyFocusables) {
            FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>) TUIApp.this.getMainApplicationPanel());
        }
        return mainFocusManager;
    }

    public void start() {

        this.started = true;

        // We do an initial refresh before going into RAW mode (caused by
        // makeEventBus())
        // in order that canvas is correctly refreshed after current screen cursor
        // position.  This is especially useful when the canvas is not full screen and
        // you want the canvas position just below last prompt on the terminal
        getCanvas().refresh();

        screen.add(ControlCode.HIDE_CURSOR);
        screen.refresh();

        if (this.eventBus == null) {
            makeEventBus();
        }

        mouseEventOchestrator = new MouseEventOrchestrator(screen, getCanvas(), eventBus);

        if (useMouse) {
            mouseEventOchestrator.enableMouseHandling();
            if (rootPanelForMouseEvents == null) {
                if (mainApplicationPanel != null && mainApplicationPanel instanceof Component) {
                    rootPanelForMouseEvents = (Component<?>)mainApplicationPanel;
                }
            }

            if (rootPanelForMouseEvents != null) {
                mouseEventOchestrator.appendApplicationMouseEventToClickable(rootPanelForMouseEvents, null);
            }

        } else {
            mouseEventOchestrator.disableMouseHandling();
        }

        terminateAppHotKeyEventHandler = new KeyInputEventHandler(terminateAppHotKey, (te) -> onClick_terminateAppHotKey(te) );
        eventBus.appendEventHandler(terminateAppHotKeyEventHandler);

        makeRefreshCanvasIfModifiedAction();

        // if updateCanvas not specified by client app, then let's try to make our own
        if (updateCanvas == null && mainApplicationPanel != null && mainApplicationPanel instanceof Component) {
            Component<?> __mainPanel = (Component<?>) mainApplicationPanel;

            this.setUpdateCanvas((tApp)->{
                
                boolean dirty = __mainPanel.isDirty() ||
                                canvasClearedAndNowEmpty ||
                                (getCanvas().isEmpty() && !__mainPanel.getSegmentsBuffer().isEmpty());
                
                if ( ! dirty) {
                    return false; // false means no GUI update required
                }

                ElapsedTime canvasUpdateTime = ElapsedTime.start();

                tApp.getCanvas().clearAndAddComponentSegments( __mainPanel );

                // Logging.info("Canvas.addComponentSegments() time: " + canvasUpdateTime.toString());
 
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

        // setup event handlers to deal with terminal resizes
        TerminalResizeDetection.initiate(screen, eventBus);

        if (this.updateCanvas != null) {
            eventBus.appendEventHandler(new OnRequestUpdateCanvasEventHandler(refreshCanvasIfModifiedAction));
        }

        if (terminateAction != null) {
            eventBus.appendEventHandler(new OnTerminateEventHandler(() -> terminateAction.accept(this)));
        }

        if (this.updateCanvas != null) {
            eventBus.appendEventHandler(new OnIdleEventEventHandler.OneTime(refreshCanvasIfModifiedAction));
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

                if ( __mainPanel instanceof Dimension ) {
                    if ( ! tApp.getCanvas().contains((Dimension) __mainPanel )) {
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

        eventBus.appendEventHandler(new FindScrollToViewHandler());
        
        eventBus.appendEventHandler(new OnResizeEventHandler() {
            @Override
            public void handleResize(Dimension size) {

                if (onResizeAction != null) {
                    onResizeAction.accept(TUIApp.this, size);
                }

                if ( ! size.contains( getCanvas() )) {
                    // canvas too big (custom onResizeAction didn't adjust it properly) 
                    // - let's fix - otherwise the screen output will be messed up!
                    master_canvas.setSize( size.getHeight(), size.getWidth() );
                }

                // asynchronously tell the rest of the system, its time to do a screen refresh

                eventBus.raiseEvent(new RequestUpdateCanvasEvent(TUIApp.this));
            }
        });

        if (this.onReadyStartAction != null) {
            // asynchronously run the application custom startup code
            eventBus.asynch(new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    // run start up (custom setup) code
                    TUIApp.this.inOnReadyStartAction  = true;
                    TUIApp.this.onReadyStartAction.accept(TUIApp.this);
                    TUIApp.this.inOnReadyStartAction  = false;
                    // do the first canvas paint (asynchronously)
                    
                    if (TUIApp.this.eventBus != null) {
                        // eventbus is non-null - then ensure that we have a focusManager
                        getApplicationFocusManager(true);
                    }
                    
                    
                    eventBus.raiseEvent(new RequestUpdateCanvasEvent(TUIApp.this));
                }
            });
        }
    }



}