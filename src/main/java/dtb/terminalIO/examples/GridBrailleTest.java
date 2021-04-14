package dtb.terminalIO.examples;


import java.util.Comparator;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.plot.PathPlotter.CirclePathPlotter;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.TransformPointPlotter;
import dtb.terminalIO.components.plot.XYTranslate;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CircleAlgorithm.CircleCounterClockwise_SORT;
import dtb.terminalIO.components.plot.shape.CircleAlgorithm.DonutCircleAlgorithm;
import dtb.terminalIO.components.plot.shape.CompositeShape;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.components.plot.shape.ShapeWithTransform;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.examples.Demo.SimpleDemo;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionRelativeFactory;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.PositionTransform.RotateAroundFixedPoint;
import dtb.terminalIO.geom.position.PositionTransform.Translate;

public class GridBrailleTest {

    // an eventhandler that is used to move points around a circle
    // this would work well as a handler for an idle event
    static class CirclePathEventHandler implements EventHandler {
        CirclePathPlotter circlePath;
        XYTranslate pointsPlotter;

        public CirclePathEventHandler(int x, int y, int radius, Comparator<Position> pointsSorter,
                XYTranslate pointsPlotter) {
            this.pointsPlotter = pointsPlotter;
            circlePath = new CirclePathPlotter(x, y, radius, pointsSorter);
        }

        public CirclePathEventHandler(int x, int y, int radius, XYTranslate pointsPlotter) {
            this.pointsPlotter = pointsPlotter;
            circlePath = new CirclePathPlotter(x, y, radius);
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            Position curPos = circlePath.nextPosition();
            pointsPlotter.setxDelta(curPos.getCol());
            pointsPlotter.setyDelta(curPos.getRow());
        }
    }


    public static class BrailleDemo extends SimpleDemo {

        Composite brailleComponent;
        BrailleGrid grid;
        TextComponent tc;
        Runnable drawIt;

        public void resize(Dimension size) {

            brailleComponent.setBounds(size.getHeight() - 4, size.getWidth() - 4);

            Dimension textHelpDim = Dimension.UTIL.stringToDimension(tc.getText());

            tc.resize(new Dimension.Simple(Math.min(textHelpDim.getHeight(), size.getHeight() - 4),
                    Math.min(textHelpDim.getWidth(), size.getWidth() / 2)));

            grid.resize(new Dimension.Simple(brailleComponent.getHeight() - 4,
                                             brailleComponent.getWidth() - 4 - tc.getWidth()));

            brailleComponent.moveChildTo(new Position.Simple(2, tc.getWidth() + 2), grid);

        }

        public Component<?> getBrailleComponent() {
            return brailleComponent;
        }

        public void setUpDemo(EventHandlerContainer eventBus, Rectangle bounds) {

            brailleComponent = new Composite();
            brailleComponent.setBounds(bounds.getHeight() - 4, bounds.getWidth() - 4);
            brailleComponent.setBorder(true);
            brailleComponent.setName("brailleComponent");

            Color textCOLOR = Colors.DEFAULT_COLOR;
            Color textColorBOLD = textCOLOR.bold();

            TextComponent tcBrailleChars = new TextComponent(textCOLOR,
                    textColorBOLD.asString() + "Braille Character set : " + Colors.ICya.asBold().asString()
                            + "⠀⠁⠂⠃⠄⠅⠆⠇⠈⠉⠊⠋⠌⠍⠎⠏⠐⠑⠒⠓⠔⠕⠖⠗⠘⠙⠚⠛⠜⠝⠞⠟⠠⠡⠢⠣⠤⠥⠦⠧⠨⠩⠪⠫⠬⠭⠮⠯⠰⠱⠲⠳⠴⠵⠶⠷⠸⠹⠺⠻⠼⠽⠾⠿"
                            + textCOLOR.asString());

            tc = new TextComponent(textCOLOR,
                              "This is a demonstration \n" + "using the special braille\n" + "character set to plot points\n"
                            + "in a standard linux terminal.\n" + "\n" + "The idea is that we map the normal\n"
                            + "terminal row columns combined with the\n" + "2 x 8 dots of the braille character set\n"
                            + "to come up with a plottable coordinate system.\n" + "\n" + "      "
                            + Colors.IYel.asBold().asString() + "    .........." + textCOLOR.asString() + "\n" + "\n"
                            + "In this example we are\n" + "plotting boxes, circles and triangles.\n" + "\n"
                            + "We transform (move/rotate) their positions\n" + "using simple trigonometry\n" + "\n"
                            + "A more practical Acme use-case for\n" + "this plotting technique would be to\n"
                            + "to build terminal-based charts for\n" + "monitoring values stored in the OLT-MGR\n"
                            + "and rendered via CLIApp.java");

            brailleComponent.add(tcBrailleChars);
            brailleComponent.add(2, 0, tc);

            grid = new BrailleGrid(Colors.DEFAULT_COLOR, new Dimension.Simple(brailleComponent.getHeight() - 4,
                    brailleComponent.getWidth() - 4 - tc.getWidth()));
            grid.setPlottingColor(new Colors.FGBGColors(Colors.Yel));

            brailleComponent.add(new Position.Simple(2, tc.getWidth() + 2), grid);

            XYTranslate pointPlotter = new XYTranslate(grid.asPointPlotter(), 0, 0);

            CompositeShape shapes = new CompositeShape();

            XYTranslate rectangle1PointPlotter = new XYTranslate(null, 0, 0);

            shapes.addShape(new ShapeWithTransform(
                    new RectangleShape(new Rectangle.Simple(30, 30, 20, 20), Colors.IBlu), rectangle1PointPlotter));

            Position fixedPoint = new Position.Simple(30, 50);

            RotateAroundFixedPoint rectangle2Rotator = new RotateAroundFixedPoint(0, fixedPoint);

            TransformPointPlotter rectangle2PointPlotter = new TransformPointPlotter(null, rectangle2Rotator);

            shapes.addShape(new ShapeWithTransform(
                    new RectangleShape(new Rectangle.Simple(30, 30, 20, 20), Colors.IGre).setFilled(true), rectangle2PointPlotter));

            PositionTransform.FofG trianglePositionTransform = new PositionTransform.FofG(
                    new RotateAroundFixedPoint(0, fixedPoint), new PositionTransform.Translate(0, 0));
            shapes.addShape(new ShapeWithTransform(new Triangle(0, 0, 30, 100, 65, 50, Colors.ICya).setFilled(true),
                                                   new TransformPointPlotter(null, trianglePositionTransform)));

            XYTranslate circle1PointPlotter = new XYTranslate(null, 0, 0);

            DonutCircleAlgorithm donutPlotter = new DonutCircleAlgorithm(0);

            shapes.addShape(
                    new ShapeWithTransform(new Circle(0, 0, 30).setAlgorithm(donutPlotter), circle1PointPlotter));

            XYTranslate circle2PointPlotter = new XYTranslate(null, 0, 0);
            shapes.addShape(new ShapeWithTransform(new Circle(50, 50, 30, Colors.IRed), circle2PointPlotter));
            drawIt = new Runnable() {

                @Override
                public void run() {

                    renderPreUPdate();

                    brailleComponent.markAsDirty();

                    grid.clear(); // clears and marks as dirty
                    grid.clearPlots();

                    // draw the shapes into the pointPlotter ( into the grid )
                    shapes.draw(pointPlotter);
                    grid.markAsDirty();

                    flushOutput();
                }
            };

            // redraw the grid every 100 millisec's
            addAndRememberEventHandler(eventBus, new OnIdleEventEventHandler.RepeatingInterval(100, new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    drawIt.run();
                }
            }).setName("GridBrailleGrid.redraw.IdleEventHandler"));

            // move the triangle
            addAndRememberEventHandler(eventBus, new OnIdleEventEventHandler.RepeatingInterval(100, new EventHandler() {

                int x = 0;
                int xIncr = 1;

                @Override
                public void handle(TerminalEvent<?> te) {
                    if (x <= -20) {
                        xIncr = 1;
                    } else {
                        if (x >= 20) {
                            xIncr = -1;
                        }
                    }
                    x += xIncr;

                    Translate translate = (PositionTransform.Translate) trianglePositionTransform.getG_transform();
                    RotateAroundFixedPoint rotator = (RotateAroundFixedPoint) trianglePositionTransform
                            .getF_transform();

                    int newTheta = (rectangle2Rotator.getTheta() + 1) % 360;
                    rotator.setTheta(newTheta);

                    translate.setColDelta(x);
                }
            }).setName("moveTriangle.IdleEventHandler"));

            // redraw rectangle1 and move it around a circle path
            addAndRememberEventHandler(eventBus, new OnIdleEventEventHandler.RepeatingInterval(100,
                    new CirclePathEventHandler(0, 0, 20, rectangle1PointPlotter)));

            // redraw circle1 and move it around a circle path
            CirclePathEventHandler pathForCircle1 = new CirclePathEventHandler(10, 10, 35, circle1PointPlotter);
            addAndRememberEventHandler(eventBus, new OnIdleEventEventHandler.RepeatingInterval(100, (te) -> {
                int curAngle = donutPlotter.getEndAngle();
                donutPlotter.setAngle(curAngle == 360 ? 0 : curAngle + 1);
                pathForCircle1.handle(te);
            }));

            // redraw circle2 and move it around a circle path
            addAndRememberEventHandler(eventBus, new OnIdleEventEventHandler.RepeatingInterval(100,
                    new CirclePathEventHandler(0, 0, 25, new CircleCounterClockwise_SORT(0, 0), circle2PointPlotter)).setName("Circle.EventHandler"));

            addAndRememberEventHandler(eventBus, new OnIdleEventEventHandler.RepeatingInterval(100, new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    int newTheta = (rectangle2Rotator.getTheta() + 1) % 360;
                    rectangle2Rotator.setTheta(newTheta);
                }
            }).setName("rectangle2Rotator.idleHandler"));

            
            // brailleComponent
            ButtonBar movementKeys = new ButtonBar(brailleComponent.getColor()).setName("movementKeys");
            movementKeys.setBorder(true);
            movementKeys.setLeftMostBorderChar((char) 0);
            movementKeys.setRightMostBorderChar((char) 0);
            
            Button upButton = new Button("^");
            upButton.setHotKey(KEY_MOVE_UP);
            upButton.setDisplayHotKey(true);
            upButton.onChange((te)->{
                pointPlotter.setyDelta(pointPlotter.getyDelta() - 1);
                drawIt.run();
            });

            Button downButton = new Button("v");
            downButton.setHotKey(KEY_MOVE_DOWN);
            downButton.setDisplayHotKey(true);
            downButton.onChange((te)->{
                pointPlotter.setyDelta(pointPlotter.getyDelta() + 1);
                drawIt.run();
            });
            

            Button leftButton = new Button("«");
            leftButton.setHotKey(KEY_MOVE_LEFT);
            leftButton.setDisplayHotKey(true);
            leftButton.onChange((te)->{
                pointPlotter.setxDelta(pointPlotter.getxDelta() - 1);
                drawIt.run();
            });

            Button rightButton = new Button("»");
            rightButton.setHotKey(KEY_MOVE_RIGHT);
            rightButton.setDisplayHotKey(true);
            rightButton.onChange((te)->{
                pointPlotter.setxDelta(pointPlotter.getxDelta() + 1);
                drawIt.run();
            });

            movementKeys.addComponent(upButton);
            movementKeys.addComponent(downButton);
            movementKeys.addComponent(leftButton);
            movementKeys.addComponent(rightButton);

            Position movementKeysPosition = PositionRelativeFactory.fromAlign(VAlign.BOTTOM, 3, Align.MIDDLE, 0,  brailleComponent, movementKeys);
            brailleComponent.add(movementKeysPosition, movementKeys);

            addAndRememberEventHandler(eventBus, movementKeys);

        }

        private KeyStroke KEY_MOVE_UP    = Keys.UP_KEYSTROKE;
        private KeyStroke KEY_MOVE_DOWN  = Keys.DOWN_KEYSTROKE;
        private KeyStroke KEY_MOVE_LEFT  = Keys.LEFT_KEYSTROKE;
        private KeyStroke KEY_MOVE_RIGHT = Keys.RIGHT_KEYSTROKE;

        public void setupMovementKeys(KeyStroke KEY_MOVE_UP, 
                                      KeyStroke KEY_MOVE_DOWN, 
                                      KeyStroke KEY_MOVE_LEFT,
                                      KeyStroke KEY_MOVE_RIGHT) {
            this.KEY_MOVE_UP = KEY_MOVE_UP;
            this.KEY_MOVE_DOWN = KEY_MOVE_DOWN;
            this.KEY_MOVE_LEFT = KEY_MOVE_LEFT;
            this.KEY_MOVE_RIGHT = KEY_MOVE_RIGHT;
        }
    }
    
    
    public static void main(String[] args) {

        String _style = "MAX";
        if (args != null && args.length > 0 && args[0].equalsIgnoreCase("small")) {
            _style = "SMALL";
        }
        if (args != null && args.length > 0 && args[0].equalsIgnoreCase("medium")) {
            _style = "MEDIUM";
        }
        if (args != null && args.length > 0 && args[0].equalsIgnoreCase("tiny")) {
            _style = "TINY";
        }
        String style = _style;

        TUIApp app = new TUIApp(args).setEmptyChar(' ')
                                     .setMainApplicationPanel(new Composite().setEmptyChar(' '))
                                     .confirmOnExit();

        TextComponent tc_status = new TextComponent("[             ]");

        BrailleDemo brailleDemo = new BrailleDemo() {
            int counter = 0;
            public void flushOutput() {
                TestsCommon.renderScreenDEBUG(app.getCanvas(), app.getScreen(), app.getEventBus());
                counter = (counter +1) % 100 ;
                tc_status.setTextAndResize(" [ " + String.format("%4d", counter) + " ] ");
                app.doRefreshCanvasIfModified();
            }
        };

        if (style.equals("MAX")) {
            app.setOnResizeAction((tApp, size)->{
                tApp.clearCanvas();
                tApp.setCanvasSize(size);
                
                ((Composite)tApp.getMainApplicationPanel()).resize(size);
                
                brailleDemo.resize(size);
            });
        }

        app.setOnReadyStart((tApp)->{
            
            Rectangle bounds = null;
            if ("SMALL".equalsIgnoreCase(style)) {
                bounds = new Rectangle.Simple(0, 0, Math.min(20, app.getCanvas().getHeight()),
                        Math.min(100, app.getCanvas().getWidth()));
            } else if ("MEDIUM".equalsIgnoreCase(style)) {
                bounds = new Rectangle.Simple(0, 0, Math.min(25, app.getCanvas().getHeight()),
                        Math.min(100, app.getCanvas().getWidth()));
            } else if ("TINY".equalsIgnoreCase(style)) {
                bounds = new Rectangle.Simple(0, 0, Math.min(15, app.getCanvas().getHeight()),
                        Math.min(60, app.getCanvas().getWidth()));
            } else {
                bounds = new Rectangle.Simple(app.getCanvas());
            }

            app.getCanvas().setSize(bounds.getHeight(), bounds.getWidth());

            // set to size of canvas
            ((Composite)app.getMainApplicationPanel()).resize(app.getCanvas());

            brailleDemo.setUpDemo(tApp.getEventBus(), bounds);

            Component<?> brailleComponent = brailleDemo.getBrailleComponent();

            ((Composite)app.getMainApplicationPanel()).add(brailleComponent);

            ((Composite)app.getMainApplicationPanel()).add(tc_status);

        });


        app.start();

    }

}
