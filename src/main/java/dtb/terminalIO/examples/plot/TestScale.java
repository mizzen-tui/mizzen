package dtb.terminalIO.examples.plot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.CompositeWithFocusManager;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.NavButtonsPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.RockerButton;
import dtb.terminalIO.components.input.TextInputInteger;
import dtb.terminalIO.components.plot.XYTranslate;
import dtb.terminalIO.components.plot.blocks.BlocksGrid;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.Fillable;
import dtb.terminalIO.components.plot.shape.Line;
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
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionDelegate;
import dtb.terminalIO.geom.position.PositionHorizontalRelative;
import dtb.terminalIO.geom.position.PositionRelativeFactory;
import dtb.terminalIO.geom.position.PositionVerticalRelative;

public class TestScale {


    public static void main(String[] args) throws IOException {
        
        new TUIApp(args)
               .setEmptyChar(' ')
               .setDefaultMainApplicationPanel()
               .confirmOnExit()
               .setOnReadyStart((tApp)->{

                   XYTranslate xyTranslatePlotter = new XYTranslate(null, 0, 0);

                   TextComponent msgArea = new TextComponent(" ");

                   Consumer<String> msg = (s) -> msgArea.setTextAndResize("MSG: " + s);

                   ListBox<Shape> shapesChoiceComponent = shapeChooser();

                   Supplier<Dimension> computeGridSize = ()-> {
                       return new Dimension.Simple(tApp.getMainApplicationPanel().getHeight()-15,
                                                   tApp.getMainApplicationPanel().getWidth()-shapesChoiceComponent.getWidth()-20);
                   };

                   Dimension gridSize = new Dimension.SupplierAsDimension(computeGridSize);

                   BlocksGrid grid = new BlocksGrid(Colors.BGre, gridSize);
                   //BrailleGrid grid = new BrailleGrid(Colors.BGre, gridSize);
                   
                   RockerButton fillOption = new RockerButton(grid.getColor(), "Fill", false);
                   Position fillOptionPosition = PositionRelativeFactory.fromAlign(VAlign.BOTTOM, 3, Align.LEFT, 0,  grid, fillOption);
                   grid.add(fillOptionPosition, fillOption);


                   grid.setTitle(Colors.BPur, " Shape : ");

                   xyTranslatePlotter.setPointPlotter(grid.asPointPlotter());

                   Consumer<Shape> drawShape = (shape)-> {
                       if (shape == null) {
                           msg.accept("shape = null");
                           return;
                       }

                       grid.clearPlots();

                       if (shape instanceof Fillable<?>) {
                           fillOption.show();
                           ((Fillable<?>)shape).setFilled(fillOption.getValue());
                       } else {
                           fillOption.hide();
                       }

                       shape.draw(xyTranslatePlotter);
                       grid.markAsDirty();
                       tApp.doRefreshCanvasIfModified();
                   };

                   shapesChoiceComponent.onChange((te)->{
                       Shape shape = shapesChoiceComponent.getValue();
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
                                              xyTranslatePlotter.setyDelta(xyTranslatePlotter.getyDelta()-1) ;
                                              delayedDraw.onRequest(tApp.getEventBus());
                                              msg.accept("Up  "+xyTranslatePlotter.getyDelta());
                                            },
                                      (e)->{
                                              xyTranslatePlotter.setyDelta(xyTranslatePlotter.getyDelta()+1) ;
                                              delayedDraw.onRequest(tApp.getEventBus());
                                              msg.accept("Down  "+xyTranslatePlotter.getyDelta());
                                            },
                                      (e)->{
                                              xyTranslatePlotter.setxDelta(xyTranslatePlotter.getxDelta()-1) ;
                                              delayedDraw.onRequest(tApp.getEventBus());
                                              msg.accept("Left  "+xyTranslatePlotter.getxDelta());
                                           },
                                      (e)->{
                                              xyTranslatePlotter.setxDelta(xyTranslatePlotter.getxDelta()+1) ;
                                              delayedDraw.onRequest(tApp.getEventBus());
                                              msg.accept("Right  "+xyTranslatePlotter.getxDelta());
                                           }
                                  ));

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
                       if (newValue != null && newValue > 1) {
                           InputEventsProducer.IDLE_INTERVAL = newValue;
                       }
                   });
                   
                   vp.add(hp);
                   vp.add(intervalFormContainer);
                   vp.add(msgArea);

                   tApp.getMainApplicationPanel().add(vp);

                   ((Composite)tApp.getMainApplicationPanel()).setBorder(true);

                   DefaultFocusManager mainFocusManager = (DefaultFocusManager)FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),tApp.getEventBus());

                   mainFocusManager.setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE);
                   mainFocusManager.setNextKeyStroke(Keys.TAB_KEYSTROKE);

                   tApp.getEventBus().appendEventHandler(  mainFocusManager );
                   
                 //  FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

               }).start();
        
    }
    
    
    /**
     * Builds the nav button panel and attaches the up, down, left, right action handlers to it
     *
     * 
     * <pre>
     *             ^
     *             |
     *       <--        -->
     *             |
     *             V
     *      
     * </pre> 
     *       
     * @param upAction
     * @param downAction
     * @param leftAction
     * @param rightAction
     * @return
     */
    
    
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
    static ListBox<Shape> shapeChooser() {
        
        List<DisplayableValue<Shape>> choices = new ArrayList<DisplayableValue<Shape>>();

        choices.add(new ShapeDisplayValue("Circle", new Circle(20, 20, 20)));
        choices.add(new ShapeDisplayValue("Line", new Line(2, 2, 35, 35)));
        choices.add(new ShapeDisplayValue("Rectangle", new  RectangleShape( new Rectangle.Simple(0,0,30,40)  ) ));
        
        Triangle triangle = new Triangle(0, 0, 30, 100, 65, 50);
        
        choices.add(new ShapeDisplayValue("Triangle", triangle ));
        ListBox<Shape> shapes = new ListBox<Shape>();
        shapes.setBorder(true);
        shapes.setItems(choices);
        
        return shapes;
    }
    

    static class ShapeDisplayValue implements DisplayableValue<Shape> {

        String name;
        Shape shape;
        
        public ShapeDisplayValue(String name, Shape shape) {
            this.name = name;
            this.shape = shape;
        }

        @Override
        public Shape getValue() {
            return shape;
        }

        @Override
        public String getDisplayValue() {
            return name;
        }
    }

    
}
