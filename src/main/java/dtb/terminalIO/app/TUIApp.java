package dtb.terminalIO.app;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.keyboard.InputEventsProducer;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.geom.Dimension;

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
 * <li>  OnOffTogglesTest
 * <li>  TabPanelTest
 * 
 * etc
 * 
 * @author dbalme
 *
 */
public class TUIApp implements TUIAppInterface {

    // does the work: TerminalApp (owns the terminal), or EmbeddedApp (inside another TUIApp, see runEmbedded)
    private final TUIAppInterface app;

    // set only while runEmbedded runs its launch code, on that thread: a TUIApp made then is embedded
    private static final ThreadLocal<Embedding> EMBEDDING = new ThreadLocal<>();

    /** a TUIApp forwarding to the given implementation (tests) */
    TUIApp(TUIAppInterface app) {
        this.app = app;
    }

    public TUIApp(Function<EventBus, InputEventsProducer> inputEventsProducerBuilder) {
        Embedding embedding = EMBEDDING.get();
        this.app = embedding != null ? embedding.embed(this) : new TerminalApp(this, inputEventsProducerBuilder);
    }

    public TUIApp() {
        Embedding embedding = EMBEDDING.get();
        this.app = embedding != null ? embedding.embed(this) : new TerminalApp(this);
    }

    public TUIApp(String[] args) {
        this();
        initFromArgs(args);
    }

    public TUIApp(Function<EventBus, InputEventsProducer> inputEventsProducerBuilder, String[] args) {
        this(inputEventsProducerBuilder);
        initFromArgs(args);
    }

    /**
     * Run a TUIApp inside this one (the host): the launch code - usually an example's main - makes
     * and starts a TUIApp as it always does, unaware; that TUIApp's main panel goes into slot, sized
     * to it, and it shares the host's event bus. Call from the host's event thread (a key or click
     * handler, or the host's setup code).
     * <pre>
     *     Embedding demo = TUIApp.runEmbedded(host, rightPane, () -> ButtonBarTest.main(new String[0]));
     *     ...
     *     demo.close();
     * </pre>
     * It fails, leaving nothing behind, if the launch code throws, makes no TUIApp, makes two, or
     * does not start the one it made.
     */
    public static Embedding runEmbedded(TUIApp host, Composite slot, Embedding.Launch launch) {
        if (EMBEDDING.get() != null) {
            throw new IllegalStateException("runEmbedded called from the launch code of another runEmbedded");
        }
        Embedding embedding = new Embedding(host, slot);
        EMBEDDING.set(embedding);
        try {
            launch.run();
        } catch (RuntimeException e) {
            embedding.close();
            throw e;
        } catch (Exception e) {
            embedding.close();
            throw new IllegalStateException("The embedded app failed to start", e);
        } finally {
            EMBEDDING.remove();
        }
        embedding.checkStarted();
        return embedding;
    }

    /** the terminal side, for DefaultTerminateAction / DefaultExceptionAction */
    TerminalApp terminal() {
        return (TerminalApp) app;
    }

    @Override
    public TUIApp initFromArgs(String[] args) {
        app.initFromArgs(args);
        return this;
    }

    @Override
    public Dimension getDimensions() {
        return app.getDimensions();
    }

    @Override
    public TUIApp setInitialFocus(Component<?> component) {
        app.setInitialFocus(component);
        return this;
    }

    @Override
    public TUIApp setAutoFocus(boolean autoFocus) {
        app.setAutoFocus(autoFocus);
        return this;
    }

    @Override
    public TUIApp verifyFocusables(boolean verifyFocusables) {
        app.verifyFocusables(verifyFocusables);
        return this;
    }

    @Override
    public Canvas getCanvas() {
        return app.getCanvas();
    }

    @Override
    public TerminalScreen getScreen() {
        return app.getScreen();
    }

    @Override
    public EventBus getEventBus() {
        return app.getEventBus();
    }

    @Override
    public void runUIAction(Runnable task) {
        app.runUIAction(task);
    }

    @Override
    public TUIApp setMainApplicationPanel(ChildComponentManager<?> mainApplicationPanel) {
        app.setMainApplicationPanel(mainApplicationPanel);
        return this;
    }

    @Override
    public TUIApp setDefaultMainApplicationPanel() {
        app.setDefaultMainApplicationPanel();
        return this;
    }

    @Override
    public ChildComponentManager<?> getMainApplicationPanel() {
        return app.getMainApplicationPanel();
    }

    @Override
    public Component<?> findFirstComponentForCondition(Function<Component<?>, Boolean> condition) {
        return app.findFirstComponentForCondition(condition);
    }

    @Override
    public Component<?> findComponentByName(String name) {
        return app.findComponentByName(name);
    }

    @Override
    public <T> T findComponentByName(String name, Class<T> clazz) {
        return app.findComponentByName(name, clazz);
    }

    @Override
    public TUIApp confirmOnExit(String confirmApplicationExitMessage) {
        app.confirmOnExit(confirmApplicationExitMessage);
        return this;
    }

    @Override
    public TUIApp confirmOnExit() {
        app.confirmOnExit();
        return this;
    }

    @Override
    public KeyInputEventHandler getTerminateAppHotKeyEventHandler() {
        return app.getTerminateAppHotKeyEventHandler();
    }

    @Override
    public Function<TUIApp, Boolean> getUpdateCanvas() {
        return app.getUpdateCanvas();
    }

    @Override
    public void setUpdateCanvas(Function<TUIApp, Boolean> updateCanvas) {
        app.setUpdateCanvas(updateCanvas);
    }

    @Override
    public void setTerminateHandler(Consumer<TUIApp> terminateAction) {
        app.setTerminateHandler(terminateAction);
    }

    @Override
    public TUIApp setOnReadyStart(Consumer<TUIApp> onReadyStartAction) {
        app.setOnReadyStart(onReadyStartAction);
        return this;
    }

    @Override
    public TUIApp setOnResizeAction(BiConsumer<TUIApp, Dimension> resizeAction) {
        app.setOnResizeAction(resizeAction);
        return this;
    }

    @Override
    public TUIApp appendOnResizeAction(BiConsumer<TUIApp, Dimension> resizeAction) {
        app.appendOnResizeAction(resizeAction);
        return this;
    }

    @Override
    public BiConsumer<TUIApp, Dimension> getResizeAction() {
        return app.getResizeAction();
    }

    @Override
    public void setIdleEventStarvationLimit(int interval) {
        app.setIdleEventStarvationLimit(interval);
    }

    @Override
    public TUIApp setCanvasSize(Dimension size) {
        app.setCanvasSize(size);
        return this;
    }

    @Override
    public TUIApp setCanvasSize(int height, int width) {
        app.setCanvasSize(height, width);
        return this;
    }

    @Override
    public TUIApp clearCanvas() {
        app.clearCanvas();
        return this;
    }

    @Override
    public TUIApp setEmptyChar(char emptyChar) {
        app.setEmptyChar(emptyChar);
        return this;
    }

    @Override
    public char getEmptyChar() {
        return app.getEmptyChar();
    }

    @Override
    public TUIApp setEmptyColors(Color emptyColor) {
        app.setEmptyColors(emptyColor);
        return this;
    }

    @Override
    public Color getEmptyColor() {
        return app.getEmptyColor();
    }

    @Override
    public void doRefreshCanvasIfModified() {
        app.doRefreshCanvasIfModified();
    }

    @Override
    public TUIApp enableMouseHandling() {
        app.enableMouseHandling();
        return this;
    }

    @Override
    public TUIApp disableMouseHandling() {
        app.disableMouseHandling();
        return this;
    }

    @Override
    public TUIApp enableMouseHandling(Component<?> rootPanelForMouseEvents) {
        app.enableMouseHandling(rootPanelForMouseEvents);
        return this;
    }

    @Override
    public TUIApp enableComponentInspector() {
        app.enableComponentInspector();
        return this;
    }

    @Override
    public TUIApp enableComponentInspector(KeyStroke toggleKey) {
        app.enableComponentInspector(toggleKey);
        return this;
    }

    @Override
    public FocusManager getApplicationFocusManager() {
        return app.getApplicationFocusManager();
    }

    /**
     * true: the app takes the whole terminal - its alternate screen, as less, nano or vim do - and on
     * exit the shell's screen comes back exactly as it was. false: the app draws below the shell
     * prompt and stays in the scrollback (it may be smaller than the terminal: a pop-up in a command
     * line session).
     * <p/>
     * Not set: start() decides by size - full screen when the canvas is the terminal's size. Set
     * before start(); it cannot change after.
     */
    @Override
    public TUIApp setFullScreen(boolean fullScreen) {
        app.setFullScreen(fullScreen);
        return this;
    }

    @Override
    public boolean isFullScreen() {
        return app.isFullScreen();
    }

    @Override
    public void start() {
        app.start();
    }
}
