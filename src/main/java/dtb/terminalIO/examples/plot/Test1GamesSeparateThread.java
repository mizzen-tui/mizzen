package dtb.terminalIO.examples.plot;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.PointPlotter.PointPlot;
import dtb.terminalIO.components.plot.PointPlotter.PointPlotterList;
import dtb.terminalIO.components.plot.TransformPointPlotter;
import dtb.terminalIO.components.plot.XYTranslate;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CircleAlgorithm;
import dtb.terminalIO.components.plot.shape.CompositeShape;
import dtb.terminalIO.components.plot.shape.DotShape;
import dtb.terminalIO.components.plot.shape.Line;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.OnIdleEventEventHandler.OneTimeAfterInterval;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform.RotateAroundFixedPoint;

public class Test1GamesSeparateThread {

    public static void main(String args[]) throws IOException {

        // Logging.setEnabled(true);

//        BufferedImage image[] = new BufferedImage[] { null };
//
//        if (args.length != 0) {
//            String fileName = args[0];
//            image[0] = ImageIO.read(new File(fileName));
//        }

        TUIApp app = new TUIApp(args).setEmptyChar(' ').setDefaultMainApplicationPanel().confirmOnExit();

        app.setOnReadyStart((tApp) -> {
            addTestSpritesPanel(tApp, tApp.getMainApplicationPanel(), args);
        });

        app.start();

    }

    public static Component<?> addTestSpritesPanel(TUIApp tApp, ChildComponentManager<?> parentPanel, String args[]) {
        BufferedImage image[] = new BufferedImage[] { null };
        try {

            if (args.length != 0) {
                String fileName = args[0];
                image[0] = ImageIO.read(new File(fileName));
            }
            return addTestSpritesPanel(tApp, tApp.getMainApplicationPanel(), true, image[0],
                                        args.length > 1 ? args[1] : null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static Component<?> addTestSpritesPanel(TUIApp tApp, 
                                                   ChildComponentManager<?> parentPanel) {
        return addTestSpritesPanel(tApp, parentPanel, false, null, null);
    }

    public static Component<?> addTestSpritesPanel(TUIApp tApp, 
                                                   ChildComponentManager<?> parentPanel,
                                                   boolean includeTitle) {
        return addTestSpritesPanel(tApp, parentPanel, includeTitle, null, null);
    }

    public static Component<?> addTestSpritesPanel(TUIApp tApp, 
                                                   ChildComponentManager<?> parentPanel,
                                                   boolean includeTitle, 
                                                   Image backgroundImage, 
                                                   String gradientStr) {

        Supplier<Dimension> computeGridSize = () -> {
            return new Dimension.Simple(parentPanel.getHeight() - 3, parentPanel.getWidth() - 3);
        };

        Dimension gridSize = computeGridSize.get();

        //BlocksGrid grid = new BlocksGrid(Colors.BGre, gridSize);
        BrailleGrid grid = new BrailleGrid(Colors.BGre, gridSize);
        if (includeTitle) {
            grid.setTitle(Colors.BPur, "Space Invaders");
        }
        grid.setName("GameGrid");

        BiConsumer<Dimension, Boolean> buildImageIfDefined = (size, addToParent) -> {
            Component<?> imgViewer[] = new Component[] { null };
            if (backgroundImage != null) {
                try {
                    imgViewer[0] = gradientStr == null ? ImageViewerFactory.fromImage(backgroundImage, grid)
                            : ImageViewerFactory.simpleImageToStringView(backgroundImage, size, gradientStr);
                    if (addToParent) {
                        parentPanel.add(imgViewer[0]);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        buildImageIfDefined.accept(grid, true);

        parentPanel.add(grid);

        PointPlotterList gamePlayPointsPlotter = new PointPlotterList();

        tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(150, (te) -> {
            grid.clearPlots();
            List<PointPlot> l = new ArrayList<PointPlot>(gamePlayPointsPlotter.getPlots());

            PointPlotter ppGrid = grid.asPointPlotter();

            for (PointPlot pp : l) {
                ppGrid.setNextPlotColor(pp.color);
                ppGrid.plot(pp.x, pp.y);
            }

            grid.markAsDirty();
            tApp.doRefreshCanvasIfModified();
        }));

        tApp.getEventBus().appendIdleEventHandler(new OneTimeAfterInterval((te) -> {
            spawnGameLoop(tApp, () -> tApp.getEventBus().isActive(), () -> grid.getGridRectangle(),
                    gamePlayPointsPlotter);
        }, 200));

        BiConsumer<TUIApp, Dimension> defaultOnResizeAction = tApp.getResizeAction() != null ? tApp.getResizeAction()
                : (t, s) -> {
                };

        tApp.setOnResizeAction((tApp2, newSize) -> {
            // do default behaviour
            defaultOnResizeAction.accept(tApp2, newSize);
            // now resize the grid
            Dimension newGridSize = computeGridSize.get();
            grid.resize(newGridSize);

            buildImageIfDefined.accept(newGridSize, false);
//            if (imgViewer[0] != null) {
//                ((CanResize)imgViewer[0]).resize(newGridSize);
//            }
        });

        return grid;
    }

    static void spawnGameLoop(TUIApp tApp, Supplier<Boolean> isActive, Supplier<Rectangle> size,
            PointPlotter pointPlotter) {

        Position gridMidpoint = new Position() {
            @Override
            public int getCol() {
                return size.get().centerPosition().getCol();
            }

            @Override
            public int getRow() {
                return size.get().centerPosition().getRow();
            }
        };

        RotateAroundFixedPoint gridPointRotator = new RotateAroundFixedPoint(0, gridMidpoint);
        TransformPointPlotter gridRotatingPlotter = new TransformPointPlotter(null, gridPointRotator);

        XYTranslate pPrime1 = new XYTranslate(pointPlotter, 10, 10);
        XYTranslate pPrime2 = new XYTranslate(pointPlotter, size.get().centerPosition().getCol() + 20, 0);
        XYTranslate pPrime3 = new XYTranslate(pointPlotter, size.get().centerPosition().getCol() + 20,
                size.get().getHeight() - 10);

        new Thread(() -> {

            Consumer<Integer> renderAction = (i) -> {
                pointPlotter.clearPlots();

                pPrime1.setxDelta(i * 2);
                pPrime1.setyDelta(i);

                pPrime2.setxDelta(size.get().centerPosition().getCol() + 20 - i);
                pPrime2.setyDelta(pPrime1.getyDelta());

                pPrime3.setxDelta(size.get().centerPosition().getCol() + 20);
                pPrime3.setyDelta(size.get().getHeight() - 10 - i);

                gridPointRotator.setTheta(0);

                // gridRotatingPlotter.setPointPlotter(pPrime1);
                plotEnemyONE(enemies[0], pPrime1);

                gridRotatingPlotter.setPointPlotter(pPrime2);
                plotEnemyONE(enemies[1], gridRotatingPlotter);

                gridRotatingPlotter.setPointPlotter(pPrime3);
                plotEnemyONE(enemies[2], gridRotatingPlotter);

                // iLabel.setText("i = "+i);

                // pPrime2 = new
            };

            while (isActive.get()) {
                for (int i = 0; i < 220; i++) {
                    if (!isActive.get()) {
                        return;
                    }
                    try {

                        if (i % 1 == 0) {
                            renderAction.accept(i);
                        }

                        Thread.sleep(25);
                    } catch (Exception e) {
                        tApp.getEventBus().raiseEvent(Thread.currentThread(), TYPE.EXCEPTION, e);
                    }
                }
            }

        }).start();

    }

    static Shape[] enemies = new Shape[] { new EnemyTwoShape(), new EnemyOneShape(), new EnemyOneShape() };

    static void plotEnemyONE(Shape _enemyONE, PointPlotter pointPlotter) {
        _enemyONE.draw(pointPlotter);
    }

    static class EnemyOneShape extends RotatingShape {

        CompositeShape componentShapes;
        Line l1;
        Line l2;
        Line l3;
        Line l4;
        Rectangle boundingRectangle;

        public EnemyOneShape() {
            boundingRectangle = new Rectangle.Simple(0, 0, 9, 18);
            this.componentShapes = new CompositeShape();
            Position midPoint = spriteMidPoint();
            componentShapes.addShape(this.l1 = new Line(-3, -3, midPoint.getRow(), midPoint.getCol(), Colors.BBlu));
            componentShapes.addShape(this.l2 = new Line(midPoint.getRow(), midPoint.getCol(), 15, -3, Colors.BCya));
            componentShapes.addShape(this.l3 = new Line(-5, 3, midPoint.getRow(), midPoint.getCol(), Colors.BBlu));
            componentShapes.addShape(this.l4 = new Line(midPoint.getRow(), midPoint.getCol(), 15, 3, Colors.BCya));
            componentShapes.addShape(new Circle(midPoint.getRow(), midPoint.getCol(), 6, Colors.BGre));
            componentShapes.addShape(new Circle(midPoint.getRow(), midPoint.getCol(), 1, Colors.BRed));
        }

        Color getLineColor(Line l) {
            if (l.getColor().equals(Colors.BWhi)) {
                return Colors.BCya;
            }
            return Colors.BWhi;
        }

        protected Shape<?> getCurrentShape() {
            return this.componentShapes;
        }

        public void drawShape(int drawCounter, Shape currentShape, PointPlotter pointPlotter) {
            if (drawCounter % 6 == 0) {
                l1.setColor(getLineColor(l1));
                l2.setColor(getLineColor(l2));
                l3.setColor(getLineColor(l3));
                l4.setColor(getLineColor(l4));
            }
            setTheta(drawCounter * 10);
            currentShape.draw(pointPlotter);
        }

        @Override
        public Rectangle calcBoundingRect() {
            if (boundingRectangle == null) {
                boundingRectangle = new Rectangle.Simple(0, 0, 9, 18);

            }
            return boundingRectangle;
        }

        @Override
        public void drawShape(int drawCounter, PointPlotter pointPlotter) {
            // TODO Auto-generated method stub
            
        }

    }

    static class EnemyTwoShape extends RotatingShape {

        CompositeShape componentShapes;
        Triangle t1;

        RotateAroundFixedPoint pointRotator;
        TransformPointPlotter rotatingPlotter;

        int drawCounter = 0;
        Rectangle boundingRect;

        public EnemyTwoShape() {

            Position p1 = new PointPlot(0, 0);
            Position p2 = new PointPlot(30, 0);
            Position p3 = new PointPlot(15, 30);

            boundingRect = Rectangle.UTIL.calcDimensionsFromPositions(p1, p2, p3);
            Position midPoint = boundingRect.centerPosition();

            pointRotator = new RotateAroundFixedPoint(0, midPoint);
            rotatingPlotter = new TransformPointPlotter(null, pointRotator);

            this.t1 = new Triangle(p1, p2, p3, Colors.BBlu).setFilled(true);

            this.componentShapes = new CompositeShape() {
                protected void drawChild(Shape<?> childShape, PointPlotter pointPlotter) {
                    if (childShape != t1) {
                        super.drawChild(childShape, pointPlotter);
                        return;
                    }
                    rotatingPlotter.setPointPlotter(pointPlotter);
                    pointRotator.setTheta(drawCounter * 3);
                    childShape.draw(rotatingPlotter);
                }
            };

            componentShapes.addShape(t1);
            componentShapes.addShape(new Circle(midPoint.getRow(), midPoint.getCol(), 5, Colors.BGre).setAlgorithm(new CircleAlgorithm(true)));
            componentShapes.addShape(new Circle(midPoint.getRow(), midPoint.getCol(), 1, Colors.BRed));

            componentShapes.addShape(new RectangleShape(boundingRect, Colors.BIBla));
            componentShapes.addShape(new DotShape(p1, Colors.BIYel));
            componentShapes.addShape(new DotShape(p2, Colors.BIYel));
            componentShapes.addShape(new DotShape(p3, Colors.BIYel));

        }

        Color getLineColor(Triangle t) {
            if (t.getColor().equals(Colors.IYel)) {
                return Colors.BCya;
            }
            return Colors.IYel;
        }


        @Override
        public void drawShape(int drawCounter,  PointPlotter pointPlotter) {
            if (drawCounter % 6 == 0) {
                t1.setColor(getLineColor(t1));
            }
            componentShapes.draw(pointPlotter);
        }

        @Override
        public Rectangle calcBoundingRect() {
            return boundingRect;
        }

    };

}
