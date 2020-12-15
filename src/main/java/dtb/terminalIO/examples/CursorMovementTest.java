package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.ShowComponentOnFocus;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.ScreenCanvasBackend.UpdateStrategy;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;

public class CursorMovementTest {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);

        new TUIApp(args).setCanvasSize(24, 100).setEmptyChar(' ')
                .setMainApplicationPanel(new Composite().setBounds(30, 120).setEmptyChar('X').setBorder(false))
                .confirmOnExit().setOnReadyStart((tApp) -> {

                    Position[] cursorPositionContainer = new Position[] { new Position.Simple(0, 0) };

                    TextComponent mTextBox = new TextComponent(
                            "Silly Sally sold\nSeaShells at the seashore\nWhat a silly thing to do!");

                    mTextBox.setColor(new FGBGColors(Colors.BBlu, Colors.On_Bla));

                    BiConsumer<EventBus, Position> moveCursorAction = (eventBus, offset) -> {

                        Position requestedPosition = cursorPositionContainer[0].offsetAdd(offset);
                        if (mTextBox.containsPosition(requestedPosition)) {
                            // ensure desired cursor location will be in view
                            new ShowComponentOnFocus().onFocus(mTextBox,
                                    new Rectangle.Simple(requestedPosition, new Dimension.Simple(1, 1)), eventBus);
                            // now move cursor to caret and then request a canvas update
                            eventBus.appendEventHandler(new OnIdleEventEventHandler.OneTime(() -> {
                                Component.UTIL.requestShowCursorAtComponent(mTextBox, requestedPosition, eventBus);
                                mTextBox.markAsDirty();
                                eventBus.raiseEvent(new RequestUpdateCanvasEvent(mTextBox));
                                cursorPositionContainer[0] = requestedPosition;
                            }));
                        }
                    };

                    // up-cursor
                    tApp.getEventBus().appendEventHandler(new KeyInputEventHandler(Keys.CTRL_T_KEYSTROKE, (te) -> {
                        moveCursorAction.accept(te.getEventBus(), new Position.Simple(-1, 0));
                    }));

                    // down-cursor
                    tApp.getEventBus().appendEventHandler(new KeyInputEventHandler(Keys.CTRL_V_KEYSTROKE, (te) -> {
                        moveCursorAction.accept(te.getEventBus(), new Position.Simple(1, 0));
                    }));

                    // left-cursor
                    tApp.getEventBus().appendEventHandler(new KeyInputEventHandler(Keys.CTRL_F_KEYSTROKE, (te) -> {
                        moveCursorAction.accept(te.getEventBus(), new Position.Simple(0, -1));
                    }));

                    // right-cursor
                    tApp.getEventBus().appendEventHandler(new KeyInputEventHandler(Keys.CTRL_G_KEYSTROKE, (te) -> {
                        moveCursorAction.accept(te.getEventBus(), new Position.Simple(0, 1));
                    }));

                    Dimension scDimension = new Dimension.SupplierAsDimension(() -> {
                        return new Dimension.Simple(tApp.getMainApplicationPanel().getHeight() -10,
                                                    tApp.getMainApplicationPanel().getWidth() - 10);
                    });

                    ScrollableComponent sc = new ScrollableComponent(Colors.Whi,
                                                                     scDimension, 
                                                                     mTextBox,
                                                                     () -> new DefaultFocusManager(Keys.SHIFT_TAB_KEYSTROKE, Keys.TAB_KEYSTROKE)
                                                                               .setNavKeysFirst(false));
                    sc.setFillOnClear(false);
                    
                    BiFunction<KeyStroke, String, String> hotKeyHelpText = (key, helpText) -> {
                         return Colors.coloredText(Colors.BBlu, key.getName())+" : "+helpText;
                    };


                    TextComponent helpText = new TextComponent(hotKeyHelpText.apply(Keys.CTRL_T_KEYSTROKE,"up")   + " | " +
                                                               hotKeyHelpText.apply(Keys.CTRL_V_KEYSTROKE,"down") + " | " +
                                                               hotKeyHelpText.apply(Keys.CTRL_F_KEYSTROKE,"left") + " | " +
                                                               hotKeyHelpText.apply(Keys.CTRL_G_KEYSTROKE,"right")+ " | " +
                                                               hotKeyHelpText.apply(Keys.CTRL_U_KEYSTROKE,"Update Strategy")+ " | " +
                                                               hotKeyHelpText.apply(Keys.CTRL_C_KEYSTROKE,"Quit")
                                                                );
                    
                    TextComponent debugText = new TextComponent("");

                    tApp.getMainApplicationPanel().add(2, 4, helpText);
                    tApp.getMainApplicationPanel().add(4, 4, debugText);
                    tApp.getMainApplicationPanel().add(6, 4, sc);

                    FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(
                            tApp.getMainApplicationPanel(), tApp.getEventBus(),
                            () -> new DefaultFocusManager().setNavKeysFirst(false).setCycling(false));

                    tApp.getEventBus().appendEventHandler(mainFocusManager);

                    FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager,
                            (Component<?>) tApp.getMainApplicationPanel());

                    BiConsumer<TUIApp, Dimension> baseResizeAction = tApp.getResizeAction();

                    tApp.setOnResizeAction( (ptApp, newSize) ->{
                        baseResizeAction.accept(ptApp, newSize);
                        sc.resize(scDimension);
                    } );

                   ScreenCanvasBackend canvasBackend = ((ScreenCanvasBackend)tApp.getCanvas().getCanvasBackend());

                   UpdateStrategy lastRememberedUpdateStrategy[] = {canvasBackend.getScreenUpdateStrategy()};

                   Function<TUIApp, Boolean> baseScreenUpdater = tApp.getUpdateCanvas();

                   tApp.setUpdateCanvas( (tApp2)-> {
                       UpdateStrategy currentUpdateStrategy = canvasBackend.getScreenUpdateStrategy();
                       boolean updateStrategyChanged = false;
                       if ( ! currentUpdateStrategy.equals(lastRememberedUpdateStrategy[0])) {
                           updateStrategyChanged = true;
                           sc.setTitle("UPDATE STRATEGY: "+currentUpdateStrategy);
                       }

                       debugText.setTextAndResize(tApp2.getScreen().getEmitter().DEBUGCTR());

                       TestsCommon.renderScreenDEBUG(tApp2.getCanvas(), tApp2.getScreen(), tApp2.getEventBus());

                       return baseScreenUpdater.apply(tApp2) || updateStrategyChanged;
                   } );

                    Runnable toggleScreenUpdateStrategy = ()-> {
                        if (canvasBackend.getScreenUpdateStrategy() == UpdateStrategy.UPDATE_FULL) {
                            canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_CHANGED);
                            sc.setTitle("UPDATE STRATEGY: "+canvasBackend.getScreenUpdateStrategy());
                        } else {
                            canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_FULL);
                            sc.setTitle("UPDATE STRATEGY: "+canvasBackend.getScreenUpdateStrategy());
                        }
                    };

                    tApp.getEventBus().appendEventHandler(new KeyInputEventHandler(Keys.CTRL_U_KEYSTROKE, (te) -> {
                        toggleScreenUpdateStrategy.run();
                        te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(tApp));
                    }));

                    canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_CHANGED);
                    sc.setTitle("UPDATE STRATEGY: "+canvasBackend.getScreenUpdateStrategy());

                    moveCursorAction.accept(tApp.getEventBus(), new Position.Simple(0,0));

                }).start();

    }

}
