package mizzen.app;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import mizzen.components.ChildComponentManager;
import mizzen.components.Component;
import mizzen.components.CompositeWithFocusManager;
import mizzen.components.HasName;
import mizzen.controlcodes.color.Color;
import mizzen.core.Canvas;
import mizzen.core.CanvasBackend;
import mizzen.core.TerminalScreen;
import mizzen.events.EventBus;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.focus.DefaultFocusManager;
import mizzen.events.focus.FocusManager;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyStroke;
import mizzen.geom.CanResize;
import mizzen.geom.Dimension;

/**
 * TUIApp's work when it runs inside another TUIApp, the host (see TUIApp.runEmbedded). Its main
 * panel goes into a panel of the host's, so the host draws it, and sends it keys (by focus) and
 * mouse clicks (by position) like any other component. It adds its handlers to the host's event
 * bus through a RecordingEventBus, so close() can take every one off again.
 * <p/>
 * The terminal is the host's. Calls about it are ignored: canvas size, mouse, CTRL-C and
 * confirmOnExit, terminate handler, idle starvation limit, component inspector. getScreen is refused
 * - drawing on the host's screen would wreck it. The canvas is a stand-in the size of the host's
 * panel, for code that sizes itself from it; nothing drawn on it shows.
 */
final class EmbeddedApp implements TUIAppInterface {

    private final TUIApp owner;
    private final Embedding embedding;
    private final RecordingEventBus eventBus;
    private final Canvas canvas = new Canvas(new CanvasBackend.NullCanvasBackEnd());

    private ChildComponentManager<?> mainApplicationPanel;
    private Function<TUIApp, Boolean> updateCanvas;
    private Consumer<TUIApp> onReadyStartAction;
    private BiConsumer<TUIApp, Dimension> onResizeAction;
    boolean started;

    EmbeddedApp(TUIApp owner, Embedding embedding) {
        this.owner = owner;
        this.embedding = embedding;
        this.eventBus = new RecordingEventBus(embedding.host.getEventBus());
        Dimension inside = embedding.slot.getInnerRectangle();      // inside its border, if it has one
        canvas.setSize(inside.getHeight(), inside.getWidth());
        canvas.setEmptyChar(' ');
    }

    public EmbeddedApp initFromArgs(String[] args) {
        return this;                            // args are for the terminal: the host's
    }

    public Dimension getDimensions() {
        return canvas.asRectangle();
    }

    public EmbeddedApp setInitialFocus(Component<?> component) {
        configureRootFocusManager(fm -> fm.setInitialFocus(component));
        return this;
    }

    public EmbeddedApp setAutoFocus(boolean autoFocus) {
        configureRootFocusManager(fm -> fm.setAutoFocus(autoFocus));
        return this;
    }

    // the main panel's focus manager, if it is one (otherwise the host's root manages its focus)
    private void configureRootFocusManager(Consumer<DefaultFocusManager> configure) {
        if (mainApplicationPanel instanceof FocusManager) {
            DefaultFocusManager.applyIfDefaultFocusManager(mainApplicationPanel, configure);
        }
    }

    public EmbeddedApp verifyFocusables(boolean verifyFocusables) {
        return this;
    }

    public Canvas getCanvas() {
        return canvas;
    }

    public TerminalScreen getScreen() {
        throw new UnsupportedOperationException("An embedded TUIApp has no screen: the terminal is the host's");
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public void runUIAction(Runnable task) {
        eventBus.asynch((te) -> {
            task.run();
            doRefreshCanvasIfModified();
        });
    }

    public EmbeddedApp setMainApplicationPanel(ChildComponentManager<?> mainApplicationPanel) {
        if (started) {
            throw new IllegalStateException("You must only call setMainApplicationPanel() before calling start()");
        }
        if (!(mainApplicationPanel instanceof Component)) {
            throw new IllegalArgumentException("An embedded TUIApp's main panel must be a Component - it goes into the host's panel. Class = "
                                               + mainApplicationPanel.getClass().getName());
        }
        // make sure it fits
        if (!canvas.asRectangle().contains(mainApplicationPanel)) {
            if (mainApplicationPanel instanceof CanResize) {
                ((CanResize) mainApplicationPanel).resize(new Dimension.Simple(Math.min(mainApplicationPanel.getHeight(), canvas.getHeight()),
                                                                               Math.min(mainApplicationPanel.getWidth(), canvas.getWidth())));
            } else {
                throw new IllegalStateException("The main panel is larger than the host's panel and does not implement CanResize. Class = "
                                                + mainApplicationPanel.getClass().getName());
            }
        }
        this.mainApplicationPanel = mainApplicationPanel;
        HasName.UTIL.setDefaultNameFromParentObject(mainApplicationPanel, owner, "mainPanel");
        return this;
    }

    public EmbeddedApp setDefaultMainApplicationPanel() {
        return setMainApplicationPanel(new CompositeWithFocusManager()
                                               .setBounds(canvas)
                                               .setEmptyChar(canvas.getEmptyChar()));
    }

    public ChildComponentManager<?> getMainApplicationPanel() {
        if (mainApplicationPanel == null) {
            throw new IllegalStateException("The main application panel is not defined yet: first call setMainApplicationPanel(..) or setDefaultMainApplicationPanel()");
        }
        return mainApplicationPanel;
    }

    public Component<?> findFirstComponentForCondition(Function<Component<?>, Boolean> condition) {
        return getMainApplicationPanel().findFirstComponentForCondition(condition);
    }

    public Component<?> findComponentByName(String name) {
        return getMainApplicationPanel().findComponentByName(name);
    }

    public <T> T findComponentByName(String name, Class<T> clazz) {
        return getMainApplicationPanel().findComponentByName(name, clazz);
    }

    public EmbeddedApp confirmOnExit(String confirmApplicationExitMessage) {
        return this;                            // exiting is the host's
    }

    public EmbeddedApp confirmOnExit() {
        return this;
    }

    public KeyInputEventHandler getTerminateAppHotKeyEventHandler() {
        return null;                            // CTRL-C is the host's
    }

    public Function<TUIApp, Boolean> getUpdateCanvas() {
        return updateCanvas;
    }

    public void setUpdateCanvas(Function<TUIApp, Boolean> updateCanvas) {
        this.updateCanvas = updateCanvas;       // kept, not called: the host draws its canvas
    }

    public void setTerminateHandler(Consumer<TUIApp> terminateAction) {
    }

    public EmbeddedApp setOnReadyStart(Consumer<TUIApp> onReadyStartAction) {
        this.onReadyStartAction = onReadyStartAction;
        return this;
    }

    public EmbeddedApp setOnResizeAction(BiConsumer<TUIApp, Dimension> resizeAction) {
        this.onResizeAction = resizeAction;
        return this;
    }

    public EmbeddedApp appendOnResizeAction(BiConsumer<TUIApp, Dimension> resizeAction) {
        BiConsumer<TUIApp, Dimension> previous = onResizeAction != null ? onResizeAction : (t, s) -> {};
        return setOnResizeAction((tApp, size) -> {
            previous.accept(tApp, size);
            resizeAction.accept(tApp, size);
        });
    }

    public BiConsumer<TUIApp, Dimension> getResizeAction() {
        return onResizeAction;
    }

    public void setIdleEventStarvationLimit(int interval) {
    }

    public EmbeddedApp setCanvasSize(Dimension size) {
        return this;                            // the host's panel decides the size
    }

    public EmbeddedApp setCanvasSize(int height, int width) {
        return this;
    }

    /** its canvas is its panel: drawn afresh on the next refresh */
    public EmbeddedApp clearCanvas() {
        if (mainApplicationPanel != null) {
            ((Component<?>) mainApplicationPanel).markAsDirty();
        }
        return this;
    }

    public EmbeddedApp setEmptyChar(char emptyChar) {
        canvas.setEmptyChar(emptyChar);
        return this;
    }

    public char getEmptyChar() {
        return canvas.getEmptyChar();
    }

    public EmbeddedApp setEmptyColors(Color emptyColor) {
        canvas.setEmptyColors(emptyColor);
        return this;
    }

    public Color getEmptyColor() {
        return canvas.getEmptyColor();
    }

    public void doRefreshCanvasIfModified() {
        embedding.host.doRefreshCanvasIfModified();
    }

    public EmbeddedApp enableMouseHandling() {
        return this;                            // the host's mouse handling reaches it through the component tree
    }

    public EmbeddedApp disableMouseHandling() {
        return this;
    }

    public EmbeddedApp enableMouseHandling(Component<?> rootPanelForMouseEvents) {
        return this;
    }

    public EmbeddedApp enableComponentInspector() {
        return this;
    }

    public EmbeddedApp enableComponentInspector(KeyStroke toggleKey) {
        return this;
    }

    /** its main panel if that manages focus (it is then a focus manager inside the host's), otherwise the host's */
    public FocusManager getApplicationFocusManager() {
        if (mainApplicationPanel instanceof FocusManager) {
            return (FocusManager) mainApplicationPanel;
        }
        return embedding.host.getApplicationFocusManager();
    }

    public EmbeddedApp setFullScreen(boolean fullScreen) {
        return this;                            // the terminal is the host's
    }

    public boolean isFullScreen() {
        return embedding.host.isFullScreen();
    }

    /** into the host's panel; the setup code (setOnReadyStart) runs next on the host's bus */
    public void start() {
        if (mainApplicationPanel == null) {
            throw new IllegalStateException("An embedded TUIApp needs a main panel: call setMainApplicationPanel(..) or setDefaultMainApplicationPanel() before start()");
        }
        started = true;
        if (onResizeAction == null && mainApplicationPanel instanceof CanResize) {
            CanResize panel = (CanResize) mainApplicationPanel;
            onResizeAction = (tApp, size) -> panel.resize(size);
        }
        embedding.slot.add((Component<?>) mainApplicationPanel);
        if (onReadyStartAction != null) {
            eventBus.asynch((te) -> {
                onReadyStartAction.accept(owner);
                eventBus.raiseEvent(new RequestUpdateCanvasEvent(owner));
            });
        }
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(owner));
    }

    void resize(Dimension size) {
        canvas.setSize(size.getHeight(), size.getWidth());
        if (onResizeAction != null) {
            onResizeAction.accept(owner, size);
        }
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(owner));
    }

    void close() {
        eventBus.removeAll();
        if (started) {
            embedding.slot.remove((Component<?>) mainApplicationPanel);
        }
        embedding.host.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(embedding.host));
    }

    int handlersOnHostBus() {
        return eventBus.size();
    }
}
