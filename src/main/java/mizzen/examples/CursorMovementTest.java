package mizzen.examples;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;

import mizzen.app.TUIApp;
import mizzen.components.ButtonBar;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.input.ShowComponentOnFocus;
import mizzen.components.scroll.ScrollableComponent;
import mizzen.controlcodes.color.Colors;
import mizzen.core.ScreenCanvasBackend;
import mizzen.core.ScreenCanvasBackend.UpdateStrategy;
import mizzen.events.EventBus;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.keyboard.Keys;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.util.Logging;

public class CursorMovementTest {

    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);

        new TUIApp(args).setCanvasSize(24, 100).setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .confirmOnExit().setOnReadyStart((tApp) -> {

                    Position[] cursorPositionContainer = new Position[] { new Position.Simple(0, 0) };

                    TextComponent mTextBox = new TextComponent(
                            "Silly Sally sold\nSeaShells at the seashore\nWhat a silly thing to do!");

                    mTextBox.setColor(new Colors.FGBGColors(Colors.BBlu, Colors.On_Bla));

                    BiConsumer<EventBus, Position> moveCursorAction = (eventBus, offset) -> {

                        Position requestedPosition = cursorPositionContainer[0].offsetAdd(offset);
                        if (mTextBox.containsPosition(requestedPosition)) {
                            // ensure desired cursor location will be in view
                            new ShowComponentOnFocus().onFocus(mTextBox,
                                    new Rectangle.Simple(requestedPosition, new Dimension.Simple(1, 1)), eventBus);
                            // now move cursor to caret and then request a canvas update
                            eventBus.appendEventHandler(new OnIdleEventEventHandler.OneTime(() -> {
                                Component.UTIL.requestShowCursorAtComponent(mTextBox, requestedPosition, eventBus);
                                cursorPositionContainer[0] = requestedPosition;
                            }));
                        }
                    };

                    // the cursor's moves: a button bar - each button's key (shown on it) presses it, nearest to the focus first
                    ButtonBar moves = new ButtonBar(Colors.BBlu, "cursorMoves");
                    moves.setBorder(false);
                    moves.setNoBorderChars();
                    moves.addButton("up",    Keys.CTRL_T_KEYSTROKE, (te) -> moveCursorAction.accept(te.getEventBus(), new Position.Simple(-1, 0)))
                         .setDisplayHotKey(true);
                    moves.addButton("down",  Keys.CTRL_V_KEYSTROKE, (te) -> moveCursorAction.accept(te.getEventBus(), new Position.Simple(1, 0)))
                         .setDisplayHotKey(true);
                    moves.addButton("left",  Keys.CTRL_F_KEYSTROKE, (te) -> moveCursorAction.accept(te.getEventBus(), new Position.Simple(0, -1)))
                         .setDisplayHotKey(true);
                    moves.addButton("right", Keys.CTRL_G_KEYSTROKE, (te) -> moveCursorAction.accept(te.getEventBus(), new Position.Simple(0, 1)))
                         .setDisplayHotKey(true);

                    Dimension scDimension = new Dimension.SupplierAsDimension(() -> {
                        return new Dimension.Simple(tApp.getMainApplicationPanel().getHeight() -10,
                                                    tApp.getMainApplicationPanel().getWidth() - 10);
                    });

                    ScrollableComponent sc = new ScrollableComponent(Colors.Whi,
                                                                     scDimension, 
                                                                     mTextBox);
                    sc.setFillOnClear(false);
                    
                    TextComponent debugText = new TextComponent("");

                    tApp.getMainApplicationPanel().add(0, 4, moves);
                    tApp.getMainApplicationPanel().add(4, 4, debugText);
                    tApp.getMainApplicationPanel().add(6, 4, sc);

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

                    moves.addButton("Update strategy", Keys.CTRL_U_KEYSTROKE, (te) -> {
                        toggleScreenUpdateStrategy.run();
                    }).setDisplayHotKey(true);
                    moves.addComponent(new TextComponent(" " + Keys.CTRL_C_KEYSTROKE.getName() + " Quit"));

                    canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_CHANGED);
                    sc.setTitle("UPDATE STRATEGY: "+canvasBackend.getScreenUpdateStrategy());

                    moveCursorAction.accept(tApp.getEventBus(), new Position.Simple(0,0));

                }).start();

    }

}
