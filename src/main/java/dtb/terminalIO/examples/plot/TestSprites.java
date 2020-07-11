package dtb.terminalIO.examples.plot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.braille.BrailleGrid;
import dtb.terminalIO.components.plot.PathPlotter;
import dtb.terminalIO.components.plot.PathPlotter.CirclePathPlotter;
import dtb.terminalIO.components.plot.PathPlotter.CompositePathPlotter;
import dtb.terminalIO.components.plot.PathPlotter.LinePathPlotter;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.TransformPointPlotter;
import dtb.terminalIO.components.plot.XYTranslate;
import dtb.terminalIO.components.plot.PointPlotter.PointPlot;
import dtb.terminalIO.components.plot.PointPlotter.PointPlotterList;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CircleAlgorithm.CircleCounterClockwise_SORT;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.events.OnIdleEventEventHandler.OneTimeAfterInterval;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.keyboard.InputEventsProducer;
import dtb.terminalIO.events.OnTerminateEventHandler;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.Rectangle.Simple;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionTransform.RotateAroundFixedPoint;

public class TestSprites {
    
    
    public static void main(String args[]) {
        
        TUIApp app = new TUIApp(args).setEmptyChar(' ')
                .setDefaultMainApplicationPanel()
                .confirmOnExit();
        app.setOnReadyStart((tApp)-> {

            Supplier<Dimension> computeGridSize = ()-> {
                return new Dimension.Simple(tApp.getMainApplicationPanel().getHeight()-3,
                                            tApp.getMainApplicationPanel().getWidth()-3);
            };
            
            Dimension gridSize = computeGridSize.get();

            BrailleGrid grid = new BrailleGrid(Colors.BGre, gridSize);

            grid.setTitle(Colors.BPur, "Space Invaders");

            tApp.getEventBus().appendEventHandler( new OnTerminateEventHandler(()-> {
                
            }));

            tApp.getMainApplicationPanel().add(grid);

            PointPlotterList gamePlayPointsPlotter = new PointPlotterList();

            tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(150, (te)-> {
                grid.clearPlots();
                List<PointPlot> l = new ArrayList<PointPlot>(gamePlayPointsPlotter.getPlots());

                PointPlotter ppGrid = grid.asPointPlotter();

                for (PointPlot pp  : l) {
                    ppGrid.setColor(pp.color);
                    ppGrid.plot(pp.x, pp.y);
                }

                grid.markAsDirty();
                tApp.doRefreshCanvasIfModified();
                

            }));

            tApp.getEventBus().appendIdleEventHandler(new OneTimeAfterInterval((te)-> {
                spawnGameLoop(tApp, ()->tApp.getEventBus().isActive(), ()->grid.getGridRectangle(), gamePlayPointsPlotter);
            },200));
            
            BiConsumer<TUIApp, Dimension> defaultOnResizeAction = 
                    tApp.getResizeAction()!=null ? tApp.getResizeAction() : (t,s)->{};

            tApp.setOnResizeAction((tApp2, newSize)->{
                // do default behaviour
                defaultOnResizeAction.accept(tApp2, newSize);
                // now resize the grid
                Dimension newGridSize = computeGridSize.get();
                grid.resize( newGridSize );
            });

        }).start();
    }
    

    static void spawnGameLoop(TUIApp tApp, Supplier<Boolean> isActive, Supplier<Rectangle> size, PointPlotter pointPlotter) {


        Supplier<PathPlotter> makePathPlotter = ()-> {
            CompositePathPlotter pathShape = new CompositePathPlotter();
            
            pathShape.addPathPlotter( new CirclePathPlotter(75,75,60) );
            pathShape.addPathPlotter( new LinePathPlotter(40,70, 200,80) );
            pathShape.addPathPlotter( new LinePathPlotter(200,80, 40,70) );
            pathShape.addPathPlotter( new CirclePathPlotter(75,75,60, new CircleCounterClockwise_SORT(75,75)) );
            return pathShape;
        };
        
        
        Circle testShape = new Circle(50, 50, 15);
        testShape.setColor(Colors.BCya);
        
        Sprite wheel = new Wheel();
        
        wheel.setPath(makePathPlotter.get());
        wheel.setRotateRate(0);
        
        Stick stick = new Stick();
        stick.setPath(makePathPlotter.get());
        stick.setRotateRate(3);
        
        new Thread(()->{

            Consumer<Integer> renderAction = (i)->{
                pointPlotter.clearPlots();

                wheel.drawNextPosition(pointPlotter);
                
                stick.drawNextPosition(pointPlotter);

            };

            while (isActive.get()) {
                for (int i = 0; i < 1000; i++) {
                    if (!isActive.get()) {
                        return ;
                    }
                    try {

                        if (i % 5 == 0) {
                            renderAction.accept(i);
                        }
                        Thread.sleep(15);
                    } catch (Exception e) {
                        tApp.getEventBus().raiseEvent(Thread.currentThread(), TYPE.EXCEPTION, e);
                    }
                }
            }
        }).start();
    }

    static interface Sprite {

        public void drawNextPosition(PointPlotter pointPlotter);

        public void setRotateRate(int i);

        public void setPath(PathPlotter pathShape);
        
    }
    
    static abstract class AbstractShapeSprite implements Sprite {
        
        Shape shape;
        PathPlotter pathPlotter;
        XYTranslate delta = new XYTranslate(null, 0, 0);
        
        RotateAroundFixedPoint gridPointRotator;
        TransformPointPlotter gridRotatingPlotter;

        int drawCounter = 0;
        int nextPathPosIncr = 1;
        int rotateRate = 0;
        
        protected void setShape(Shape shape) {
            this.shape = shape;
        }
        
        protected Shape getShape() {
            return shape;
        }

        protected RotateAroundFixedPoint getPointRotator() {
            if (gridPointRotator == null) {
               gridPointRotator = new RotateAroundFixedPoint(0, spriteMidPoint());
               gridRotatingPlotter = new TransformPointPlotter(null, gridPointRotator);
            }
            return gridPointRotator;
        }

        protected TransformPointPlotter getRotatingPlotter() {
            getPointRotator();
            return gridRotatingPlotter;
        }

        public int getTheta() {
            return getPointRotator().getTheta();
        }

        public void setTheta(int theta) {
            this.getPointRotator().setTheta(theta);
        }
        
        public void draw(PointPlotter pointPlotter) {
            if (rotateRate > 0) {
                getRotatingPlotter().setPointPlotter(pointPlotter);
                getPointRotator().setTheta(drawCounter * rotateRate);
                this.shape.draw(getRotatingPlotter());
            } else {
                this.shape.draw(pointPlotter);
            }
            drawCounter++;
        }

        abstract protected Rectangle calcBoundingRect();
        
        Position spriteMidPoint() {

            Rectangle rect = calcBoundingRect();
            if (rect == null) {
                throw new NullPointerException("BoundingRect not defined.  Implement calcBoundingRect()");
            }

            return new Position() {
                    @Override
                    public int getCol() {
                        return calcBoundingRect().centerPosition().getCol();
                    }
                    @Override
                    public int getRow() {
                        return calcBoundingRect().centerPosition().getRow();
                    }
                };
        }
        
        @Override
        public void drawNextPosition(PointPlotter pointPlotter) {
            Position p = pathPlotter.nextPosition(nextPathPosIncr);
            delta.setxDelta(p.getCol());
            delta.setyDelta(p.getRow());
            delta.setPointPlotter(pointPlotter);
            draw(delta);
        }
        
        @Override
        public void setRotateRate(int i) {
            this.rotateRate = i;
            
        }
        
        @Override
        public void setPath(PathPlotter pathPlotter) {
            this.pathPlotter = pathPlotter;
        }
    }
    
    
    static class Stick extends  AbstractShapeSprite {
        public Stick() {
            RectangleShape shape = new RectangleShape( new Rectangle.Simple(0,0, 3, 50) );
            shape.setColor(Colors.BIRed);
            this.setShape(shape);
            nextPathPosIncr = 4;
        }

        @Override
        protected Rectangle calcBoundingRect() {
            return ((RectangleShape)getShape()).getRectangle();
        }
    }
    static class Wheel extends AbstractShapeSprite {

        Rectangle rect;
        
        public Wheel() {
            Circle testShape = new Circle(0, 0, 15);
            testShape.setColor(Colors.BCya);
            setShape(testShape);
        }
        
        @Override
        protected Rectangle calcBoundingRect() {
            if (rect == null) {
                rect = new Rectangle.Simple(-15,15, 30, 30);
            }
            return rect;
        }
        
    }
    
}
