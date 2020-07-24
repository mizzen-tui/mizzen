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
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.braille.BrailleGrid;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.TextInputInteger;
import dtb.terminalIO.components.plot.XYTranslate;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.Line;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.events.AbstractTimedRequestedController;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.InputEventsProducer;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionDelegate;
import dtb.terminalIO.geom.position.PositionHorizontalRelative;
import dtb.terminalIO.geom.position.PositionVerticalRelative;

public class TestScale {


    static Component<?> navButtonsPanel(EventHandler upAction,
                                        EventHandler downAction,
                                        EventHandler leftAction,
                                        EventHandler rightAction) {
        CompositeWithFocusManager buttonPanel = new CompositeWithFocusManager();
        buttonPanel.getFocusManager().setNextKeyStroke(Keys.DOWN_KEYSTROKE);
        buttonPanel.getFocusManager().setPrevKeyStroke(Keys.UP_KEYSTROKE);

        Button up = new Button("^");
        up.setHotKey(Keys.CTRL_T_KEYSTROKE);
        up.setBorder(true);
        up.setDisplayHotKey(true);
        up.setActionHandler(upAction);

        Button down = new Button("v");
        down.setHotKey(Keys.CTRL_V_KEYSTROKE);
        down.setBorder(true);
        down.setDisplayHotKey(true);
        down.setActionHandler(downAction);

        Button left = new Button("«");
        left.setHotKey(Keys.CTRL_F_KEYSTROKE);
        left.setBorder(true);
        left.setDisplayHotKey(true);
        left.setActionHandler(leftAction);

        Button right = new Button("»");
        right.setHotKey(Keys.CTRL_G_KEYSTROKE);
        right.setBorder(true);
        right.setDisplayHotKey(true);
        right.setActionHandler(rightAction);

        Position upVPosition = Position.ZERO;
        Position leftHPosition =  Position.ZERO;

        Position upPosition  = new PositionDelegate(upVPosition, new PositionHorizontalRelative(left,1));
        Position leftPosition = new PositionDelegate(new PositionVerticalRelative(up, 0), leftHPosition);
        
        Position rightPosition = new PositionDelegate(leftPosition, 
                                                      ()-> up.getWidth() + left.getWidth() + leftPosition.getCol() + 2);
        
        Position downPosition =  new PositionDelegate(()->upVPosition.getRow()+up.getHeight()+ left.getHeight(),
                                                      upPosition);
        buttonPanel.add(leftPosition, left);
        buttonPanel.add(upPosition, up);
        buttonPanel.add(rightPosition, right);
        buttonPanel.add(downPosition, down);

        return buttonPanel;
    }

    static ListBox<Shape> shapeChooser() {
        
        List<DisplayableValue<Shape>> choices = new ArrayList<DisplayableValue<Shape>>();

        choices.add(new ShapeDisplayValue("Circle", new Circle(20, 20, 20)));
        choices.add(new ShapeDisplayValue("Line", new Line(2, 2, 35, 35)));
        choices.add(new ShapeDisplayValue("Triange", new Triangle(20, 2, 2, 32, 40, 45)  ));
        ListBox<Shape> shapes = new ListBox<Shape>();
        shapes.setBorder(true);
        shapes.setItems(choices);
        
        return shapes;
    }
    
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

                   BrailleGrid grid = new BrailleGrid(Colors.BGre, gridSize);

                   grid.setTitle(Colors.BPur, " Shape : ");

                   xyTranslatePlotter.setPointPlotter(grid.asPointPlotter());

                   Consumer<Shape> drawShape = (shape)-> {
                       if (shape == null) {
                           msg.accept("shape = null");
                           return;
                       }
                       grid.clearPlots();
                       shape.draw(xyTranslatePlotter);
                       grid.markAsDirty();
                       tApp.doRefreshCanvasIfModified();
                   };

                   tApp.getEventBus().appendEventHandler( new HasValue.OnValueChangedEventHandler<Shape>(shapesChoiceComponent) {
                       @Override
                       public void valueChanged(TerminalEvent<?> te, HasValue<Shape> source) {
                           msg.accept("Shape selected: " + source.getValue().getClass().getSimpleName());
                           grid.setTitle(" Shape : "+source.getValue().getClass().getSimpleName()+" ");
                           drawShape.accept(source.getValue());
                       }
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
                                              //drawShape.accept(shapesChoiceComponent.getSelectedValue());
                                              delayedDraw.onRequest(tApp.getEventBus());
                                              msg.accept("Up  "+xyTranslatePlotter.getyDelta());
                                            },
                                      (e)->{    
                                              xyTranslatePlotter.setyDelta(xyTranslatePlotter.getyDelta()+1) ;
                                              //drawShape.accept(shapesChoiceComponent.getSelectedValue());
                                              delayedDraw.onRequest(tApp.getEventBus());
                                              msg.accept("Down  "+xyTranslatePlotter.getyDelta());
                                            },
                                      (e)->{    
                                              xyTranslatePlotter.setxDelta(xyTranslatePlotter.getxDelta()-1) ;
                                              //drawShape.accept(shapesChoiceComponent.getSelectedValue());
                                              delayedDraw.onRequest(tApp.getEventBus());
                                              msg.accept("Left  "+xyTranslatePlotter.getxDelta());
                                           },
                                      (e)->{    
                                              xyTranslatePlotter.setxDelta(xyTranslatePlotter.getxDelta()+1) ;
                                              //drawShape.accept(shapesChoiceComponent.getSelectedValue());
                                              delayedDraw.onRequest(tApp.getEventBus());
                                              msg.accept("Right  "+xyTranslatePlotter.getxDelta());
                                           }
                                  ));

                   hp.add(shapesChoiceComponent);

                   hp.add(grid);

                   VerticalPanel vp = new VerticalPanel().setVerticalSpacer(5);

                   TextInputInteger intervalControl = new TextInputInteger(3);
                   HorizontalPanel intervalFormContainer = new HorizontalPanel();
                   intervalFormContainer.add(new TextComponent("Idle-key wait time: "));
                   intervalFormContainer.add(intervalControl);

                   intervalControl.onChange((te,newValue)-> {
                       msg.accept("NBR  "+ newValue);
                       if (newValue != null && newValue > 1) {
                           InputEventsProducer.IDLE_INTERVAL = newValue;
                       }
                   });
                   
                   vp.add(hp);
                   vp.add(intervalFormContainer);
                   vp.add(msgArea);

                   long counter[] = new long[] {0};
                   TextComponent counterText = new TextComponent("0");

                   vp.add(counterText);

                   tApp.getMainApplicationPanel().add(vp);
/*
                   tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(75,
                           (e)-> {
                                counter[0]++;
                                counterText.setTextAndResize(counter[0]+"");
                                tApp.doRefreshCanvasIfModified();
                           }));
*/
                   ((Composite)tApp.getMainApplicationPanel()).setBorder(true);

                   DefaultFocusManager mainFocusManager = (DefaultFocusManager)FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),tApp.getEventBus());

                   mainFocusManager.setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE);
                   mainFocusManager.setNextKeyStroke(Keys.TAB_KEYSTROKE);

                   tApp.getEventBus().appendEventHandler(  mainFocusManager );
                   
                 //  FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

               }).start();
        
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
