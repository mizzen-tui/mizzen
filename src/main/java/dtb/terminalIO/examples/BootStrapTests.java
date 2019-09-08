package dtb.terminalIO.examples;

import java.util.function.BiFunction;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Box;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.RectangleComponent;
import dtb.terminalIO.components.SimpleBorder;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.LayeredCanvasBackend;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

public class BootStrapTests {

    public static void main(String[] args) {

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        FGBGColors drawingcolor = new FGBGColors(Colors.IYel, null);

        Component componentBox1 = new Box(drawingcolor, 10);

        Component componentBox2 = new RectangleComponent(new FGBGColors(Colors.IRed, null), 15, 30);

        componentBox2 = new BorderedComponent((BoundedComponent<?>) componentBox2).setTitle(new FGBGColors(null, Colors.On_IYel), "Title Test");

        screen.setControlCodesEnabled(true);

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);

        master_canvas.setEmptyAttributes(new FGBGColors(Colors.Whi, Colors.On_Cya), ' ');
        master_canvas.bgc(Colors.On_Cya);

        BoundedComponent<?> txt2 = new TextComponent(new FGBGColors(Colors.BICya, Colors.On_IYel), "The world is\nflat!");
        BorderedComponent brdr2 = new BorderedComponent(txt2).setTitle("XX");
        brdr2.setEmptyChar('^');

        master_canvas.addComponentSegments(5, 80, brdr2);

        master_canvas.addComponentSegments(5, 30, new BorderedComponent(new TextComponent(new FGBGColors(Colors.BICya, Colors.On_IYel), "Hello World!")));

        master_canvas.t(0, 50, "[0,50: Will this render?]");
        master_canvas.t(1, 3, "Here is where I must go.\n-------------------------------------");
        master_canvas.t(28, 3, "Here it is again");
        master_canvas.t(1, 50, "[1,50: Will this render?]");
        master_canvas.t(2, 50, "[2,50: Will this render?]");
        master_canvas.t(2, 90, "[2,90]");
        master_canvas.t(3, 50, "[3,50: Will this render?]");
        master_canvas.t(9, 20, "Box?");
        master_canvas.addComponentSegments(10, 20, componentBox1);
        master_canvas.addComponentSegments(11, 90, componentBox2);

        master_canvas.addComponentSegments(10, 60, new SimpleBorder(null, new Dimension.Simple(10, 10)).setTitle("Here we go!"));

        master_canvas.addComponentSegments(5, 30, new BorderedComponent(new TextComponent(new FGBGColors(Colors.BICya, Colors.On_IYel), "Hello World!")));

        master_canvas.setEmptyChar('~');

        master_canvas.refresh();

        // System.exit(0);

        LayeredCanvasBackend movableBackend = new LayeredCanvasBackend(master_canvas, 6, 15);

        Canvas canvas = new Canvas(movableBackend);
        movableBackend.setSourceCanvas(canvas);
        canvas.setSize(20, 100);
        canvas.fgc(Colors.Whi);
        canvas.bgc(Colors.On_Bla);
        // canvas.setEmptyChar('.');
        canvas.setEmptyColor(new FGBGColors(Colors.BBlu, Colors.On_Bla));

        canvas.clearCanvas();

        BorderedComponent[] lastBorder = new BorderedComponent[] { null };

        Runnable drawborder = new Runnable() {

            @Override
            public void run() {
                if (1 + 1 + 1 == 3)
                    return;
                // if (lastBorder != null) return;
                BiFunction<String, Rectangle, BorderedComponent> makeBorder = (t, r) -> {
                    String title = t + " : " + String.valueOf(new Rectangle.Simple(r));
                    BorderedComponent cx = new BorderedComponent(new FGBGColors(Colors.BIBlu, null), new BoundedComponent.DONOTHING(r))
                            .setTitle(title);
                    return cx;
                };
                Rectangle r = new Rectangle.Simple(movableBackend, canvas);
                BorderedComponent c = makeBorder.apply("moved canvas", r);
                master_canvas.addComponentSegments(Position.ZERO, c);

                BorderedComponent c2 = makeBorder.apply("canvas", canvas.asRectangle());
                /*
                 * master_canvas.addComponent(Position.ZERO, c2);
                 */
                if (lastBorder[0] != null) {
                    master_canvas.getSegmentsBuffer().removeRectangles(lastBorder[0].getSegmentsBuffer().getSegments());
                }

                master_canvas.refresh();
                lastBorder[0] = c;
                sleep(250);
            }
        };

        String name = "David";
        testCanvas(canvas, "David", 0);
        sleep(1000);

        movableBackend.removeChildCanvasAndRefresh();
        canvas.t(1, 1, "removed 1            ").refresh();
        drawborder.run();

        movableBackend.moveDelta(1, 1);
        drawborder.run();

        movableBackend.removeChildCanvasAndRefresh();
        canvas.t(1, 1, "removed 2            ").refresh();
        drawborder.run();

        // System.exit(0);

        master_canvas.t(20, 55, "HOwdy-doody time!!!!");

        canvas.refresh();

        movableBackend.removeChildCanvas();
        canvas.t(1, 1, "removed 3            ").refresh();
        drawborder.run();

        canvas.refresh();

        // master_canvas.refresh();

        for (int i = 1; i < 10; i++) {
            testCanvas(canvas, "David", i);
            movableBackend.moveDelta(1, 1);
            drawborder.run();
            testCanvas(canvas, "GeorgeOFTHEJUNGLLE and others", i);
            movableBackend.moveDelta(-1, 1);
            drawborder.run();
            testCanvas(canvas, "Jenny", i);
            movableBackend.moveDelta(-1, 1);
            drawborder.run();
            testCanvas(canvas, "Bob", i);
            movableBackend.moveDelta(1, 1);
            drawborder.run();
            testCanvas(canvas, "Alfonso", i);
            movableBackend.moveDelta(0, -4);
            drawborder.run();
        }

        drawborder.run();

        // System.out.println("getTextAt.. :" +
        // String.valueOf(master_canvas.getTextAt(28,5)));
        // System.out.println("getCharAt.. :" +
        // String.valueOf(master_canvas.getCharAt(28,5)));

    }

    static int ctr = 0;

    private static void testCanvasx(Canvas canvas, String name) {

        // canvas.clearCanvas();
        canvas.t(1, 20, name + "  " + String.valueOf(++ctr) + "  ");

        canvas.t(10, 20, "  Dimension :" + String.valueOf(canvas.getHeight()) + "  " + String.valueOf(canvas.getWidth()));

        canvas.refresh();

        sleep(500);
    }

    private static void testCanvas(Canvas canvas, String name, int idx) {

        canvas.setTextColor(new FGBGColors(Colors.Whi, null));

        canvas.t(1, 20, "Hello there!!");

        canvas.t(1, 20, "Why..\nHello\nthere").fgc(Colors.BIGre).bgc(Colors.On_IYel).t(1, 30, name);

        canvas.t(3, 40, "Test1");

        canvas.fgc(Colors.BIGre);
        canvas.bgc(Colors.On_Bla);

        canvas.t(8, 11, new java.util.Date().toString());

        canvas.t(10 + idx, 11 + idx, "10-11");
        canvas.t(11 + idx, 13 + idx, "11-13");
        canvas.t(14 + idx, 17 + idx, "14-17");
        canvas.t(15 + idx, 18 + idx, "15-18S");

        canvas.fgc(Colors.BIGre);
        canvas.bgc(Colors.On_Bla);

        for (int r = 11; r < 18; r++) {
            canvas.t(r, r + 30, ".");
        }

        canvas.fgc(Colors.BIGre);
        canvas.bgc(Colors.On_IYel);

        canvas.t(18, 60, "  " + String.valueOf(++ctr) + "  ");
        //

        canvas.refresh();

        sleep(500);

    }

    public static void testScreen(TerminalScreen screen) {

        screen.setControlCodesEnabled(true);

        screen.t("Hello world").cr().cr().t("David");

        screen.refresh();

        sleep(1000);

        screen.t("Hello there");

        screen.t("Yessir!");

        screen.refresh();

        sleep(1000);

        screen.t("And more to come").cr().cr().t("This is great!!!").cr().t("What more can I want?");

        screen.refresh();

    }

    public static void sleep(int m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block

        }
    }

}
