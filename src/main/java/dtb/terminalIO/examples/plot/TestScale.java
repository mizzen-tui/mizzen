package dtb.terminalIO.examples.plot;

import java.io.IOException;
import java.util.ArrayList;
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
import dtb.terminalIO.components.plot.DragMove;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.RotationButtonBar;
import dtb.terminalIO.components.plot.ScaleButtonBar;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.braille.BrailleUtils;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CompositeShape;
import dtb.terminalIO.components.plot.shape.DotMatrixText;
import dtb.terminalIO.components.plot.shape.Ellipse;
import dtb.terminalIO.components.plot.shape.Fillable;
import dtb.terminalIO.components.plot.shape.Line;
import dtb.terminalIO.components.plot.shape.Polygon;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.components.plot.shape.catalog.Gear;
import dtb.terminalIO.components.plot.shape.catalog.Heart;
import dtb.terminalIO.components.plot.shape.catalog.House;
import dtb.terminalIO.components.plot.shape.catalog.RegularPolygon;
import dtb.terminalIO.components.plot.shape.catalog.Ring;
import dtb.terminalIO.components.plot.shape.catalog.Smiley;
import dtb.terminalIO.components.plot.shape.catalog.Spirograph;
import dtb.terminalIO.components.plot.shape.catalog.Spring;
import dtb.terminalIO.components.plot.shape.catalog.Star;
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
        
        
        choices.add(new ShapeDisplayValue("Hexagon", new RegularPolygon(20, 25, 16, 6).setName("Hexagon")));
        choices.add(new ShapeDisplayValue("Spirograph", new Spirograph(45, 45, 15, 25, 10)));

        choices.add(new ShapeDisplayValue("Circle+sqr", new CompositeShape().addShape(new Circle(20, 20, 20).setFilled(true))
                                                                            .addShape(new  RectangleShape( new Rectangle.Simple(0,0,42,42)  ))
                                                                            .setName("Circle+sqr")));

        Ellipse ellipse = new Ellipse(35, 20, 70, 36);
        ellipse.setName("Ellipse");
        choices.add(new ShapeDisplayValue("Ellipse", ellipse));
        choices.add(new ShapeDisplayValue("Star", new Star(40, 40, 36, 15)));
        choices.add(new ShapeDisplayValue("Heart", new Heart(40, 40, 70)));
        choices.add(new ShapeDisplayValue("Gear", new Gear(40, 40, 12, 36)));
        choices.add(new ShapeDisplayValue("Ring", new Ring(30, 30, 28, 12)));
        choices.add(new ShapeDisplayValue("Spring", new Spring(40, 20, 70, 16, 6)));
        choices.add(new ShapeDisplayValue("House", new House(30, 32, 60, 56)));
        choices.add(new ShapeDisplayValue("Smiley", new Smiley(30, 30, 28)));
        choices.add(new ShapeDisplayValue("Text", new DotMatrixText("dtbTerminalIO", Colors.BYel)));

        

        ListBox<Shape<?>> shapes = new ListBox<Shape<?>>();
        shapes.setBorder(true);
        shapes.setItems(choices);

        return shapes;
    }
    
    
    

    /** the hilite while dragging: a dim box around the shape, one dot clear of it, drawn behind it */
    static void drawHilite(Shape<?> shape, PointPlotter plotter) {
        Rectangle box = shape.bounds().toRectangle();
        new RectangleShape(new Rectangle.Simple(box.getRow() - 1, box.getCol() - 1, box.getHeight() + 2, box.getWidth() + 2), Colors.BBla).draw(plotter);
    }

    /** the middle of the shape as defined - before any transform */
    static Vec2<?> shapeCentre(Shape<?> shape) {
        PositionTransform current = shape.getPositionTransform();
        shape.setPositionTransform(PositionTransform.NOOP);
        Vec2<?> centre = shape.bounds().centerPosition();
        shape.setPositionTransform(current);
        return centre == null ? Position.ZERO : centre;
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
        private final AtomicBoolean hilited;
        private final Consumer<EventBus> redraw;
        private final DragMove dragMove;

        /** gridOnCanvas: where the grid's dots start on the screen (mouse positions come relative to the screen) */
        MoveShapeOnDrag(Supplier<Position> gridOnCanvas, Supplier<Shape<?>> shape, PositionTransform.Translate move, AtomicBoolean hilited, Consumer<EventBus> redraw) {
            this.gridOnCanvas = gridOnCanvas;
            this.shape = shape;
            this.dragMove = DragMove.of(move);        // the shape's move (translate), in grid dots
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
            dragMove.stop();
            Shape<?> current = shape.get();
            Rectangle box = current == null ? null : current.bounds().toRectangle();
            Position at = gridPosition(e);
            if (box == null || at == null) {
                return false;
            }
            Rectangle grab = new Rectangle.Simple(box.getRow() - MARGIN, box.getCol() - MARGIN,
                                                  box.getHeight() + 2 * MARGIN, box.getWidth() + 2 * MARGIN);
            if (!grab.contains(new Rectangle.Simple(at.getRow(), at.getCol(), 1, 1))) {
                return false;                         // not on the shape
            }
            dragMove.start(at);
            hilited.set(true);
            redraw.accept(e.getEventBus());
            return true;
        }

        @Override
        public boolean onDrag(ApplicationMouseEvent e) {
            Position at = gridPosition(e);
            if (!dragMove.isMoving() || at == null) {
                return false;
            }
            dragMove.moveTo(at);
            redraw.accept(e.getEventBus());
            return true;
        }

        @Override
        public boolean onDragStop(ApplicationMouseEvent e) {
            boolean wasDragging = dragMove.isMoving();
            dragMove.stop();
            if (wasDragging) {
                hilited.set(false);
                redraw.accept(e.getEventBus());
            }
            return wasDragging;
        }
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
}
