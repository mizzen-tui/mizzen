package dtb.terminalIO.examples.plot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.NavButtonsPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.RockerButton;
import dtb.terminalIO.components.input.TextInputInteger;
import dtb.terminalIO.components.modal.MessageBox;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.RotationButtonBar;
import dtb.terminalIO.components.plot.ScaleButtonBar;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.shape.AbstractCompositeShape;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CompositeShape;
import dtb.terminalIO.components.plot.shape.Fillable;
import dtb.terminalIO.components.plot.shape.Line;
import dtb.terminalIO.components.plot.shape.Polygon;
import dtb.terminalIO.components.plot.shape.RectangleShape;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.AbstractTimedRequestedController;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.InputEventsProducer;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
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
                   RockerButton fillOption = new RockerButton(grid.getColor(), "Fill", false);
                   controlsInGrid.add(fillOption);

                   controlsInGrid.add(new TextComponent("|  "));
                   RotationButtonBar rotationControls = new RotationButtonBar(grid.getColor());
                   controlsInGrid.add(rotationControls);

                   controlsInGrid.add(new TextComponent("|  "));
                   ScaleButtonBar scaleInput = new ScaleButtonBar(grid.getColor());
                   controlsInGrid.add(scaleInput);

                   Position controlsInGridPosition = PositionRelativeFactory.fromAlign(VAlign.BOTTOM, 3, Align.LEFT, 0,  grid, controlsInGrid);

                   grid.add(controlsInGridPosition, controlsInGrid);
                   grid.onClickHandler = (te) -> {
                       ApplicationMouseEvent appMouseEvent = (ApplicationMouseEvent) te;
                       MessageBox.showInfoDialog(te.getEventBus(), tApp.getMainApplicationPanel()  , "click at");
                   };

                   grid.setTitle(Colors.BPur, " Shape : ");

                   PointPlotter pp = grid; //.asPointPlotter();

                   Consumer<Shape<?>> drawShape = (shape)-> {
                       if (shape == null) {
                           msg.accept("shape = null");
                           return;
                       }

                       Vec2<?>  midPoint = grid.getGridRectangle().centerPosition();

                       PositionTransform.FofG transForm = new PositionTransform.FofG(new RotateAroundFixedPoint(rotationControls.getValue(),midPoint),translateTranFormer);

                       Integer scaleValuePercent = scaleInput.getValue();
                       if (scaleValuePercent != null && scaleValuePercent != 100) {
                           double scaleValue = ((double)scaleValuePercent) / 100d;
                           Scale scaleTransform = new Scale(scaleValue);
                           transForm = new PositionTransform.FofG(transForm, scaleTransform);
                       }

                       shape.setPositionTransform(transForm);

                       grid.clearPlots();

                       if (shape instanceof Fillable<?>) {
                           fillOption.show();
                           ((Fillable<?>)shape).setFilled(fillOption.getValue());
                       } else {
                           fillOption.hide();
                       }

                       shape.draw(pp);
                       grid.markAsDirty();
                       tApp.doRefreshCanvasIfModified();
                   };

                   shapesChoiceComponent.onChange((te)->{
                       Shape<?> shape = shapesChoiceComponent.getValue();
                       msg.accept("Shape selected: " + shape.getClass().getSimpleName());
                       grid.setTitle(" Shape : "+shape.getClass().getSimpleName()+" ");

                       drawShape.accept(shape);

                   });

                   fillOption.onChange((te)->{
                       drawShape.accept(shapesChoiceComponent.getValue());
                   });

                   AbstractTimedRequestedController delayedDraw = new AbstractTimedRequestedController(()->{
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

                   hp.add(grid);

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

                   DefaultFocusManager mainFocusManager = (DefaultFocusManager)FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),tApp.getEventBus());

                   mainFocusManager.setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE);
                   mainFocusManager.setNextKeyStroke(Keys.TAB_KEYSTROKE);

                   tApp.getEventBus().appendEventHandler(  mainFocusManager );

                   FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

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
                                                                            .addShape(new  RectangleShape( new Rectangle.Simple(0,0,42,42)  ))));

        

        ListBox<Shape<?>> shapes = new ListBox<Shape<?>>();
        shapes.setBorder(true);
        shapes.setItems(choices);

        return shapes;
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
