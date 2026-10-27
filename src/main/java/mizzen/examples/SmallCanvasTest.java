package mizzen.examples;

import mizzen.app.TUIApp;
import mizzen.components.ChildComponentManager;
import mizzen.components.TextComponent;
import mizzen.components.plot.blocks.BlocksGrid;
import mizzen.components.plot.braille.BrailleGrid;
import mizzen.components.plot.shape.Circle;
import mizzen.controlcodes.color.Colors;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.TerminalEvent;
import mizzen.geom.position.Position;

/**
 * The library without taking over the screen: a small canvas just below the prompt, a short
 * animation (a circle and a ball slide to the right edge), then the app ends by itself and the
 * prompt comes back under the output.
 * <p/>
 * Timing uses an idle-event countdown (no sleeps); the last tick raises TERMINATE, which restores
 * the terminal and leaves the cursor below the canvas.
 */
public class SmallCanvasTest {

    private static final int FRAMES = 60;      // 50 ms each
    private static final int PAUSE = 20;       // frames before the shapes appear
    private static final int LAST_COL = 31;    // the shapes stop at the canvas's right edge

    public static void main(String[] args) {
        TextComponent text = new TextComponent("Small canvas test ..");
        BrailleGrid circle = new BrailleGrid(Colors.BBlu, 6, 11);
        new Circle(10, 10, 10).draw(circle.asPointPlotter());
        BlocksGrid ball = new BlocksGrid(Colors.BGre, 6, 11);
        new Circle(5, 5, 5).setFilled(true).draw(ball.asPointPlotter());

        new TUIApp(args)
                .setCanvasSize(16, 42)
                .setEmptyChar(' ')
                .disableMouseHandling()
                .setDefaultMainApplicationPanel()
                .setOnReadyStart((app) -> {
                    ChildComponentManager<?> panel = app.getMainApplicationPanel();
                    panel.add(text);

                    app.getEventBus().appendIdleEventHandler(new OnIdleEventEventHandler.Countdown(50, FRAMES, (te, left) -> {
                        int frame = FRAMES - left;
                        if (frame == PAUSE) {
                            text.setTextAndResize("Small canvas test ..\n added some text\n And now a circle ..");
                            panel.add(3, 0, circle);
                            panel.add(10, 0, ball);
                        } else if (frame > PAUSE) {
                            int col = Math.min(frame - PAUSE, LAST_COL);
                            panel.moveChildToPosition(new Position.Simple(3, col), circle);
                            panel.moveChildToPosition(new Position.Simple(10, col), ball);
                        }
                        if (left == 1) {
                            te.getEventBus().raiseEvent(new TerminalEvent<Object>(app, TerminalEvent.TYPE.TERMINATE, null));
                        }
                    }));
                })
                .start();
    }
}
