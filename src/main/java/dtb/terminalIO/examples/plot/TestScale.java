package dtb.terminalIO.examples.plot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ClickableDraggableComponentWrapper;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.NavButtonsPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.RadioGroup;
import dtb.terminalIO.components.input.TextInputInteger;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.PointPlotter.PointPlot;
import dtb.terminalIO.components.plot.PointPlotter.PointPlotterList;
import dtb.terminalIO.components.plot.RotationButtonBar;
import dtb.terminalIO.components.plot.ScaleButtonBar;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.braille.BrailleUtils;
import dtb.terminalIO.components.plot.shape.AbstractCompositeShape;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CompositeShape;
import dtb.terminalIO.components.plot.shape.ConnectedPoints;
import dtb.terminalIO.components.plot.shape.DotMatrixText;
import dtb.terminalIO.components.plot.shape.Fillable;
import dtb.terminalIO.components.plot.shape.Line;
import dtb.terminalIO.components.plot.shape.Polygon;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.TimedRequestedController;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.Draggable;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionRelativeFactory;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.PositionTransform.RotateAroundFixedPoint;
import dtb.terminalIO.geom.position.PositionTransform.Scale;
import dtb.terminalIO.geom.position.Vec2;

public class TestScale {


    public static void main(String[] args) throws IOException {
        
        new TUIApp(args)
               .setEmptyChar(' ')
               .setDefaultMainApplicationPanel()
               .confirmOnExit()
               .setOnReadyStart((tApp)->{

                   PositionTransform.Translate translateTranFormer = new PositionTransform.Translate(0, 0);

                   TextComponent msgArea = new TextComponent(" ");

                   Consumer<String> msg = (s) -> msgArea.setTextAndResize("MSG: " + s);

                   ListBox<Shape<?>> shapesChoiceComponent = shapeChooser();

                   Supplier<Dimension> computeGridSize = ()-> {
                       return new Dimension.Simple(tApp.getMainApplicationPanel().getHeight()-15,
                                                   tApp.getMainApplicationPanel().getWidth()-shapesChoiceComponent.getWidth()-48);
                   };

                   Dimension gridSize = new Dimension.SupplierAsDimension(computeGridSize);

                   BrailleGrid grid = new BrailleGrid(Colors.BGre, gridSize);

                   HorizontalPanel controlsInGrid = new HorizontalPanel();
                   controlsInGrid.setBorder(true);
                   RadioGroup<Boolean> fillOption = RadioGroup.onOff("Fill", false).setColor(grid.getColor());
                   controlsInGrid.add(fillOption);

                   controlsInGrid.add(new TextComponent("|  "));
                   RotationButtonBar rotationControls = new RotationButtonBar(grid.getColor());
                   controlsInGrid.add(rotationControls);

                   controlsInGrid.add(new TextComponent("|  "));
                   ScaleButtonBar scaleInput = new ScaleButtonBar(grid.getColor());
                   controlsInGrid.add(scaleInput);

                   Position controlsInGridPosition = PositionRelativeFactory.fromAlign(VAlign.BOTTOM, 3, Align.LEFT, 0,  grid, controlsInGrid);

                   grid.add(controlsInGridPosition, controlsInGrid);
//                   grid.onClickHandler = (te) -> {
//                       ApplicationMouseEvent appMouseEvent = (ApplicationMouseEvent) te;
//                       MessageBox.showInfoDialog(te.getEventBus(), tApp.getMainApplicationPanel()  , "click at");
//                   };

                   grid.setTitle(Colors.BPur, " Shape : pick one - then drag it with the mouse ");

                   PointPlotter pp = grid; //.asPointPlotter();

                   AtomicBoolean beingDragged = new AtomicBoolean(false);     // hilite: a box around the shape

                   Consumer<Shape<?>> drawShape = (shape)-> {
                       if (shape == null) {
                           msg.accept("shape = null");
                           return;
                       }

                       // scale and turn the shape around its own middle, then move it (the arrows, a drag):
                       // a move is then always straight, whatever the rotation or scale
                       Vec2<?> centre = shapeCentre(shape);
                       PositionTransform turnAndSize = new RotateAroundFixedPoint(rotationControls.getValue(), centre);

                       Integer scaleValuePercent = scaleInput.getValue();
                       if (scaleValuePercent != null && scaleValuePercent != 100) {
                           double scaleValue = ((double)scaleValuePercent) / 100d;
                           turnAndSize = new PositionTransform.FofG(turnAndSize, scaleAround(new Scale(scaleValue), centre));
                       }

                       shape.setPositionTransform(new PositionTransform.FofG(translateTranFormer, turnAndSize));

                       grid.clearPlots();

                       if (shape instanceof Fillable<?>) {
                           fillOption.show();
                           ((Fillable<?>)shape).setFilled(fillOption.getValue());
                       } else {
                           fillOption.hide();
                       }

                       if (beingDragged.get()) {
                           drawHilite(shape, pp);
                       }
                       shape.draw(pp);
                       grid.markAsDirty();
                       tApp.doRefreshCanvasIfModified();
                   };

                   shapesChoiceComponent.onChange((te)->{
                       Shape<?> shape = shapesChoiceComponent.getValue();
                       msg.accept("Shape selected: " + shape.getClass().getSimpleName());
                       grid.setTitle(" Shape : "+shape.getName()+" (drag it with the mouse) ");

                       drawShape.accept(shape);

                   });

                   fillOption.onChange((te)->{
                       drawShape.accept(shapesChoiceComponent.getValue());
                   });

                   TimedRequestedController delayedDraw = new TimedRequestedController(()->{
                       drawShape.accept(shapesChoiceComponent.getSelectedValue());
                   }) {
                       @Override
                       protected boolean isRequestDifferentFromPrevious() {
                           return true;
                       }

                   };
                   delayedDraw.setTimerDelayMillis(15);

                   HorizontalPanel hp = new HorizontalPanel();
                   hp.add(navButtonsPanel(
                                      (e)->{
                                          translateTranFormer.setRowDelta(translateTranFormer.getRowDelta() -1) ;
                                              delayedDraw.onRequest(tApp.getEventBus());
                                              msg.accept("Up  "+translateTranFormer.getRowDelta());
                                            },
                                      (e)->{
                                          translateTranFormer.setRowDelta(translateTranFormer.getRowDelta()+1) ;
                                              delayedDraw.onRequest(tApp.getEventBus());
                                              msg.accept("Down  "+translateTranFormer.getRowDelta());
                                            },
                                      (e)->{
                                          translateTranFormer.setColDelta(translateTranFormer.getColDelta()-1) ;
                                              delayedDraw.onRequest(tApp.getEventBus());
                                              msg.accept("Left  "+translateTranFormer.getColDelta());
                                           },
                                      (e)->{
                                          translateTranFormer.setColDelta(translateTranFormer.getColDelta()+1) ;
                                              delayedDraw.onRequest(tApp.getEventBus());
                                              msg.accept("Right  "+translateTranFormer.getColDelta());
                                           }
                                  ));

                   // handle rotate requests
                   rotationControls.onChange((te)->{ 
                                                       delayedDraw.onRequest(te.getEventBus());
                                                   });

                   scaleInput.onChange((te)->{
                       delayedDraw.onRequest(te.getEventBus());
                   });

                   hp.add(shapesChoiceComponent);

                   // a drag that starts on the shape moves it
                   ClickableDraggableComponentWrapper draggableGrid = new ClickableDraggableComponentWrapper(grid.getColor(), grid);
                   draggableGrid.setDraggable(new MoveShapeOnDrag(() -> Component.UTIL.getCanvasOffsetFor(grid.getPlotter()),
                                                                  shapesChoiceComponent::getSelectedValue,
                                                                  translateTranFormer,
                                                                  beingDragged,
                                                                  (bus) -> {
                                                                      delayedDraw.onRequest(bus);
                                                                      msg.accept("Dragged to  " + translateTranFormer.getRowDelta() + ", " + translateTranFormer.getColDelta());
                                                                  }));
                   hp.add(draggableGrid);

                   VerticalPanel vp = new VerticalPanel().setVerticalSpacer(2);

                   TextInputInteger intervalControl = new TextInputInteger(3);
                   HorizontalPanel intervalFormContainer = new HorizontalPanel();
                   intervalFormContainer.add(new TextComponent("Idle-key wait time: "));
                   intervalFormContainer.add(intervalControl);

                   intervalControl.onChange((te)-> {
                       Integer newValue = intervalControl.getValue();
                       msg.accept("NBR  "+ newValue);
                       if (newValue != null && newValue > 2) {
                           tApp.setIdleEventStarvationLimit(newValue);
                           //InputEventsProducer.IDLE_STARVATION_INTERVAL_LIMIT = newValue;
                       }
                   });

                   vp.add(hp);
                   vp.add(intervalFormContainer);
                   vp.add(msgArea);

                   tApp.getMainApplicationPanel().add(vp);

                   tApp.appendOnResizeAction((tApp2, size)->{
                       grid.resize(new Dimension.SupplierAsDimension(computeGridSize));
                   });

                   ((Composite)tApp.getMainApplicationPanel()).setBorder(true);


               }).start();

    }
    
    
    static Component<?> navButtonsPanel(EventHandler upAction,
            EventHandler downAction,
            EventHandler leftAction,
            EventHandler rightAction) {
        return new NavButtonsPanel(Colors.DEFAULT_COLOR, upAction, downAction, leftAction, rightAction);
    }
    

    /**
     * Build list box of shapes to draw 
     * 
     */
    static ListBox<Shape<?>> shapeChooser() {
        
        List<DisplayableValue<Shape<?>>> choices = new ArrayList<DisplayableValue<Shape<?>>>();

        choices.add(new ShapeDisplayValue("Circle", new Circle(20, 20, 20)));
        choices.add(new ShapeDisplayValue("Line", new Line(2, 2, 35, 35)));
        choices.add(new ShapeDisplayValue("Rectangle", new  RectangleShape( new Rectangle.Simple(0,0,30,40)  ) ));

        Triangle triangle = new Triangle(0, 0, 30, 100, 65, 50);
        choices.add(new ShapeDisplayValue("Triangle", triangle ));

        int[][] dinoDOTs =  new int[][]{ { 6, 4 }, { 3, 1 }, { 1, 2 }, { -1, 5 }, { -2, 5 }, { -3, 4 }, { -4, 4 }, { -5, 3 },
                                         { -5, 2 }, { -2, 2 }, { -5, 1 }, { -4, 0 }, { -2, 1 }, { -1, 0 }, { 0, -3 }, { -1, -4 }, { 1, -4 },
                                         { 2, -3 }, { 1, -2 }, { 3, -1 }, { 5, 1 } };

         for (int[] p : dinoDOTs) {
             p[0] = (p[0] + 5 )* 10;
             p[1] = (p[1] + 6 )* 10;
         }

        Polygon dino = new Polygon(dinoDOTs);
        dino.setName("Dino");

        choices.add(new ShapeDisplayValue(dino.getName(), dino ));
        
        
        int[][] hexaDOTs =  new int[][]{ {10,10},
                                         {25,10},
                                         {35,20}, 
                                         {35,30},
                                         {25,40},
                                         {10,40},
                                         {0,30},
                                         {0,20},
                                         };
        Polygon hexagon = new Polygon(hexaDOTs);
        hexagon.setName("Hexagon");

        choices.add(new ShapeDisplayValue(hexagon.getName(), hexagon ));


        choices.add(new ShapeDisplayValue("SpiroGraph", new SpiroGraph(10.0f, 10.0f, 20, 30, 10f) ));

        choices.add(new ShapeDisplayValue("Circle+sqr", new CompositeShape().addShape(new Circle(20, 20, 20).setFilled(true))
                                                                            .addShape(new  RectangleShape( new Rectangle.Simple(0,0,42,42)  ))
                                                                            .setName("Circle+sqr")));

        choices.add(new ShapeDisplayValue("Star", star()));
        choices.add(new ShapeDisplayValue("Heart", heart()));
        choices.add(new ShapeDisplayValue("Gear", gear()));
        choices.add(new ShapeDisplayValue("House", house()));
        choices.add(new ShapeDisplayValue("Smiley", smiley()));
        choices.add(new ShapeDisplayValue("Text", new DotMatrixText("dtbTerminalIO", Colors.BYel)));

        

        ListBox<Shape<?>> shapes = new ListBox<Shape<?>>();
        shapes.setBorder(true);
        shapes.setItems(choices);

        return shapes;
    }
    
    
    

    /** the box around the points the shape plots now (with its transform); null if it plots none */
    static Rectangle plottedBox(Shape<?> shape) {
        PointPlotterList plots = new PointPlotterList();
        shape.draw(plots);
        if (plots.size() == 0) {
            return null;
        }
        int minRow = Integer.MAX_VALUE, minCol = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE, maxCol = Integer.MIN_VALUE;
        for (PointPlot p : plots.getPlots()) {
            minRow = Math.min(minRow, p.getRow());
            maxRow = Math.max(maxRow, p.getRow());
            minCol = Math.min(minCol, p.getCol());
            maxCol = Math.max(maxCol, p.getCol());
        }
        return new Rectangle.Simple(minRow, minCol, maxRow - minRow + 1, maxCol - minCol + 1);
    }

    /** the hilite while dragging: a dim box around the shape, one dot clear of it, drawn behind it */
    static void drawHilite(Shape<?> shape, PointPlotter plotter) {
        Rectangle box = plottedBox(shape);
        if (box == null) {
            return;
        }
        new RectangleShape(new Rectangle.Simple(box.getRow() - 1, box.getCol() - 1, box.getHeight() + 2, box.getWidth() + 2), Colors.BBla).draw(plotter);
    }

    /** the middle of the shape as defined - before any transform */
    static Vec2<?> shapeCentre(Shape<?> shape) {
        PositionTransform current = shape.getPositionTransform();
        shape.setPositionTransform(PositionTransform.NOOP);
        Rectangle box = plottedBox(shape);
        shape.setPositionTransform(current);
        return box == null ? Position.ZERO : box.centerPosition();
    }

    /** scale around a point (it stays put) instead of around 0,0 */
    static PositionTransform scaleAround(Scale scale, Vec2<?> point) {
        return new PositionTransform.FofG(new PositionTransform.Translate(point),
                                          new PositionTransform.FofG(scale, new PositionTransform.Translate(point.negate())));
    }

    /**
     * Dragging moves the shape: a drag that starts on it (within its box) hilites it (a box around it)
     * and moves it by as much as the mouse moved - by changing the move (translate) part of its
     * transform, so rotation and scale don't change where it goes. The hilite goes when the drag ends.
     */
    static class MoveShapeOnDrag implements Draggable {

        private static final int MARGIN = 2;        // grid dots around the shape that still grab it

        private final Supplier<Position> gridOnCanvas;
        private final Supplier<Shape<?>> shape;
        private final PositionTransform.Translate move;
        private final AtomicBoolean hilited;
        private final Consumer<EventBus> redraw;
        private Position dragFrom;                   // null: not dragging
        private float rowAtStart;
        private float colAtStart;

        /** gridOnCanvas: where the grid's dots start on the screen (mouse positions come relative to the screen) */
        MoveShapeOnDrag(Supplier<Position> gridOnCanvas, Supplier<Shape<?>> shape, PositionTransform.Translate move, AtomicBoolean hilited, Consumer<EventBus> redraw) {
            this.gridOnCanvas = gridOnCanvas;
            this.shape = shape;
            this.move = move;
            this.hilited = hilited;
            this.redraw = redraw;
        }

        /** the mouse in grid dots (the top-left dot of the character under it); null if the grid isn't on screen */
        private Position gridPosition(ApplicationMouseEvent e) {
            Position origin = gridOnCanvas.get();
            if (origin == null || e.getMousePosition() == null) {
                return null;
            }
            return BrailleUtils.componentPosToGridPos(e.getMousePosition().offsetAdd(origin.negate()));
        }

        @Override
        public boolean onDragStart(ApplicationMouseEvent e) {
            dragFrom = null;
            Shape<?> current = shape.get();
            Rectangle box = current == null ? null : plottedBox(current);
            Position at = gridPosition(e);
            if (box == null || at == null) {
                return false;
            }
            Rectangle grab = new Rectangle.Simple(box.getRow() - MARGIN, box.getCol() - MARGIN,
                                                  box.getHeight() + 2 * MARGIN, box.getWidth() + 2 * MARGIN);
            if (!grab.contains(new Rectangle.Simple(at.getRow(), at.getCol(), 1, 1))) {
                return false;                         // not on the shape
            }
            dragFrom = at;
            rowAtStart = move.getRowDelta();
            colAtStart = move.getColDelta();
            hilited.set(true);
            redraw.accept(e.getEventBus());
            return true;
        }

        @Override
        public boolean onDrag(ApplicationMouseEvent e) {
            Position at = gridPosition(e);
            if (dragFrom == null || at == null) {
                return false;
            }
            move.setRowDelta(rowAtStart + at.getRow() - dragFrom.getRow());
            move.setColDelta(colAtStart + at.getCol() - dragFrom.getCol());
            redraw.accept(e.getEventBus());
            return true;
        }

        @Override
        public boolean onDragStop(ApplicationMouseEvent e) {
            boolean wasDragging = dragFrom != null;
            dragFrom = null;
            if (wasDragging) {
                hilited.set(false);
                redraw.accept(e.getEventBus());
            }
            return wasDragging;
        }
    }

    /** a point at angle degrees on a circle (y grows downwards, as on the grid) */
    static Vec2<?> onCircle(float cx, float cy, float radius, double degrees) {
        double a = Math.toRadians(degrees);
        return new Vec2.BaseVec2((float) (cx + radius * Math.cos(a)), (float) (cy + radius * Math.sin(a)));
    }

    /** five points: corners alternating between an outer and an inner circle - concave, a good fill test */
    static Polygon star() {
        Polygon star = new Polygon();
        for (int i = 0; i < 10; i++) {
            star.addPoint(onCircle(40, 40, i % 2 == 0 ? 36 : 15, -90 + i * 36));
        }
        star.setName("Star");
        return star;
    }

    /** the classic heart curve, x = 16 sin^3 t, y = 13 cos t - 5 cos 2t - 2 cos 3t - cos 4t, sampled */
    static Polygon heart() {
        Polygon heart = new Polygon();
        for (int i = 0; i < 72; i++) {
            double t = Math.toRadians(i * 5);
            double x = 16 * Math.pow(Math.sin(t), 3);
            double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
            heart.addPoint((float) (40 + 2.2 * x), (float) (40 - 2.2 * y));     // y flipped: the curve's y grows upwards
        }
        heart.setName("Heart");
        return heart;
    }

    /** twelve teeth: each a rise to the outer circle, a flat top, a drop back to the root circle; a round axle hole */
    static Polygon gear() {
        Polygon gear = new Polygon();
        int teeth = 12;
        double step = 360d / teeth;
        for (int i = 0; i < teeth; i++) {
            double a = i * step;
            gear.addPoint(onCircle(40, 40, 28, a));
            gear.addPoint(onCircle(40, 40, 36, a + step * 0.15));
            gear.addPoint(onCircle(40, 40, 36, a + step * 0.45));
            gear.addPoint(onCircle(40, 40, 28, a + step * 0.6));
        }
        List<Vec2<?>> axle = new ArrayList<Vec2<?>>();      // a hole in the middle: stays empty when filled
        for (int degrees = 0; degrees < 360; degrees += 15) {
            axle.add(onCircle(40, 40, 10, degrees));
        }
        gear.addHole(axle);
        gear.setName("Gear");
        return gear;
    }

    /** a composite: walls, a filled roof, a filled door, a window and a chimney - one shape, it all moves together */
    static CompositeShape house() {
        CompositeShape house = new CompositeShape()
                .addShape(new RectangleShape(new Rectangle.Simple(30, 5, 30, 50)))                    // walls
                .addShape(new Triangle(0, 30, 30, 4, 60, 30).setFilled(true))                         // roof
                .addShape(new RectangleShape(new Rectangle.Simple(8, 42, 12, 6)))                     // chimney
                .addShape(new RectangleShape(new Rectangle.Simple(42, 22, 18, 11)).setFilled(true))   // door
                .addShape(new RectangleShape(new Rectangle.Simple(36, 9, 9, 10)))                     // window
                .addShape(new Line(14, 36, 14, 44))                                                   // window bars
                .addShape(new Line(9, 40, 18, 40));
        house.setName("House");
        return house;
    }

    /** a composite: the face, two filled eyes, and the mouth - an open curve (not closed back to its start) */
    @SuppressWarnings("rawtypes")
    static CompositeShape smiley() {
        ConnectedPoints mouth = new ConnectedPoints(false);
        for (int degrees = 25; degrees <= 155; degrees += 5) {
            mouth.addPoint(onCircle(30, 30, 18, degrees));
        }
        CompositeShape smiley = new CompositeShape()
                .addShape(new Circle(30, 30, 28))                     // face
                .addShape(new Circle(20, 20, 4).setFilled(true))      // eyes
                .addShape(new Circle(40, 20, 4).setFilled(true))
                .addShape(mouth);
        smiley.setName("Smiley");
        return smiley;
    }

    static class ShapeDisplayValue implements DisplayableValue<Shape<?>> {

        String name;
        Shape<?> shape;
        
        public ShapeDisplayValue(String name, Shape<?> shape) {
            this.name = name;
            this.shape = shape;
        }

        @Override
        public Shape<?> getValue() {
            return shape;
        }

        @Override
        public String getDisplayValue() {
            return name;
        }
    }
    
    
    static class SpiroGraph extends AbstractCompositeShape<SpiroGraph> {

        
        float cx;
        float cy;
        int radius1;
        int radius2;
        float ratio;
        
        
        public SpiroGraph(float cx, float cy, int radius1, int radius2, float ratio) {
            super();
            this.cx = cx;
            this.cy = cy;
            this.radius1 = radius1;
            this.radius2 = radius2;
            this.ratio = ratio;
            this.setName("SpiroGraph");
        }

        Collection<Shape<?>> drawSpirographToLines(float cx, float cy, int radius1, int radius2, float ratio) {
            double x,y, theta;
            
            List<Shape<?>> lines = new ArrayList<Shape<?>>();
            
            Vec2<?> currentPosition = new Vec2.BaseVec2(cx + radius1 + radius2,  cy);
            
            for (theta = 0; theta <= Math.PI * 2; theta += 0.01) {
                x = cx + radius1 * Math.cos(theta) + radius2 * Math.cos(theta * ratio);
                y = cy + radius1 * Math.sin(theta) + radius2 * Math.sin(theta * ratio);
                Vec2<?> nextPosition = new Vec2.BaseVec2((float)x,  (float)y);
                lines.add(new Line(currentPosition, nextPosition));
                currentPosition = nextPosition;
            }
            return lines;
        }

        @Override
        protected Collection<Shape<?>> buildShapes() {
            return drawSpirographToLines(cx, cy, radius1, radius2, ratio);
        }

    }
    

}
