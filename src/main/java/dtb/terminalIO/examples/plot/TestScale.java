package dtb.terminalIO.examples.plot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.braille.BrailleGrid;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.plot.TransformPointPlotter;
import dtb.terminalIO.components.plot.XYTranslate;
import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.Line;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.components.plot.shape.Triangle;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionDelegate;
import dtb.terminalIO.geom.position.PositionHorizontalRelative;
import dtb.terminalIO.geom.position.PositionVerticalRelative;

public class TestScale {

    
    
    
    
    static Component buttons(EventHandler upAction,
                             EventHandler downAction,
                             EventHandler leftAction,
                             EventHandler rightAction) {
        Composite buttonPanel = new Composite();
        
        Button up = new Button("^");
        up.setBorder(true);
        up.setActionHandler(upAction);
        
        Button down = new Button("v");
        down.setBorder(true);
        down.setActionHandler(downAction);
        
        Button left = new Button("«");
        left.setBorder(true);
        left.setActionHandler(leftAction);

        Button right = new Button("»");
        right.setBorder(true);
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

        choices.add(new ShapeDisplayValue("Circle", new Circle(10, 10, 5)));
        choices.add(new ShapeDisplayValue("Line", new Line(2, 2, 15, 15)));
        choices.add(new ShapeDisplayValue("Triange", new Triangle(10, 2, 2, 12, 20, 15)  ));
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
                       return new Dimension.Simple(tApp.getMainApplicationPanel().getHeight()-10,
                                                   tApp.getMainApplicationPanel().getWidth()-shapesChoiceComponent.getWidth()-20);
                   };
                   
                   Dimension gridSize = computeGridSize.get();

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
                   };
                   
                   tApp.getEventBus().appendEventHandler( new HasValue.OnValueChangedEventHandler<Shape>(shapesChoiceComponent) {
                       @Override
                       public void valueChanged(TerminalEvent<?> te, HasValue<Shape> source) {
                           msg.accept("Shape selected: " + source.getValue().getClass().getSimpleName());
                           grid.setTitle(" Shape : "+source.getValue().getClass().getSimpleName()+" ");
                           drawShape.accept(source.getValue());
                       }
                   });
                   
                   
                   HorizontalPanel hp = new HorizontalPanel();
                   hp.add(buttons(
                                      (e)->{    
                                              xyTranslatePlotter.setyDelta(xyTranslatePlotter.getyDelta()-1) ;
                                              drawShape.accept(shapesChoiceComponent.getSelectedValue());
                                              msg.accept("Up  "+xyTranslatePlotter.getyDelta());
                                            },
                                      (e)->{    
                                              xyTranslatePlotter.setyDelta(xyTranslatePlotter.getyDelta()+1) ;
                                              drawShape.accept(shapesChoiceComponent.getSelectedValue());
                                              msg.accept("Down  "+xyTranslatePlotter.getyDelta());
                                            },
                                      (e)->{    
                                              xyTranslatePlotter.setxDelta(xyTranslatePlotter.getxDelta()-1) ;
                                              drawShape.accept(shapesChoiceComponent.getSelectedValue());
                                              msg.accept("Left  "+xyTranslatePlotter.getxDelta());
                                           },
                                      (e)->{    
                                              xyTranslatePlotter.setxDelta(xyTranslatePlotter.getxDelta()+1) ;
                                              drawShape.accept(shapesChoiceComponent.getSelectedValue());
                                              msg.accept("Right  "+xyTranslatePlotter.getxDelta());
                                           }
                                  ));
                   
                   hp.add(shapesChoiceComponent);
                   
                   
                   
                   hp.add(grid);
                   
                   VerticalPanel vp = new VerticalPanel().setVerticalSpacer(5);
                   
                   vp.add(hp);
                   vp.add(msgArea);
                   
                   tApp.getMainApplicationPanel().add(vp);
                   
                   ((Composite)tApp.getMainApplicationPanel()).setBorder(true);
                   
                   DefaultFocusManager mainFocusManager = (DefaultFocusManager)FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),tApp.getEventBus());
                   
                   mainFocusManager.setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE);
                   mainFocusManager.setNextKeyStroke(Keys.TAB_KEYSTROKE);
                   
                   
                   
                   tApp.getEventBus().appendEventHandler(  mainFocusManager );
                   
                   
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
