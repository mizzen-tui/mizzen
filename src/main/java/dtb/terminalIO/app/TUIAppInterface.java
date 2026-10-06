package dtb.terminalIO.app;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentFinder;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.geom.Dimension;

/**
 * What a TUIApp does, whichever way it runs: TerminalApp owns the terminal (the usual case). TUIApp
 * implements this by forwarding every call to the one it holds - implementing it means the compiler
 * catches a method TUIApp forgot to forward. Chaining methods return the implementation; TUIApp
 * returns itself.
 */
interface TUIAppInterface extends ComponentFinder {
    TUIAppInterface initFromArgs(String[] args);
    Dimension getDimensions();
    TUIAppInterface setInitialFocus(Component<?> component);
    TUIAppInterface setAutoFocus(boolean autoFocus);
    TUIAppInterface verifyFocusables(boolean verifyFocusables);
    Canvas getCanvas();
    TerminalScreen getScreen();
    EventBus getEventBus();
    void runUIAction(Runnable task);
    TUIAppInterface setMainApplicationPanel(ChildComponentManager<?> mainApplicationPanel);
    TUIAppInterface setDefaultMainApplicationPanel();
    ChildComponentManager<?> getMainApplicationPanel();
    Component<?> findFirstComponentForCondition(Function<Component<?>, Boolean> condition);
    Component<?> findComponentByName(String name);
    <T> T findComponentByName(String name, Class<T> clazz);
    TUIAppInterface confirmOnExit(String confirmApplicationExitMessage);
    TUIAppInterface confirmOnExit();
    KeyInputEventHandler getTerminateAppHotKeyEventHandler();
    Function<TUIApp, Boolean> getUpdateCanvas();
    void setUpdateCanvas(Function<TUIApp, Boolean> updateCanvas);
    void setTerminateHandler(Consumer<TUIApp> terminateAction);
    TUIAppInterface setOnReadyStart(Consumer<TUIApp> onReadyStartAction);
    TUIAppInterface setOnResizeAction(BiConsumer<TUIApp, Dimension> resizeAction);
    TUIAppInterface appendOnResizeAction(BiConsumer<TUIApp, Dimension> resizeAction);
    BiConsumer<TUIApp, Dimension> getResizeAction();
    void setIdleEventStarvationLimit(int interval);
    TUIAppInterface setCanvasSize(Dimension size);
    TUIAppInterface setCanvasSize(int height, int width);
    TUIAppInterface clearCanvas();
    TUIAppInterface setEmptyChar(char emptyChar);
    char getEmptyChar();
    TUIAppInterface setEmptyColors(Color emptyColor);
    Color getEmptyColor();
    void doRefreshCanvasIfModified();
    TUIAppInterface enableMouseHandling();
    TUIAppInterface disableMouseHandling();
    TUIAppInterface enableMouseHandling(Component<?> rootPanelForMouseEvents);
    TUIAppInterface enableComponentInspector();
    TUIAppInterface enableComponentInspector(KeyStroke toggleKey);
    FocusManager getApplicationFocusManager();
    TUIAppInterface setFullScreen(boolean fullScreen);
    boolean isFullScreen();
    void start();
}
