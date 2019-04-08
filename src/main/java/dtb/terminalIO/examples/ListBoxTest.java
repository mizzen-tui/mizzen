package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.Arrays;

import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.FocusManager;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyInputEventsProducer;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.PositionVerticalRelative;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.Stty;

public class ListBoxTest {

    public static void main(String[] args) throws IOException {

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        
        Position anchorPoint = new Position.Simple(1,10);
        
        String[] iFaces = new String[] 
                {"lo", 
                "eth0",
                "virbr0", 
                "virbr0-nic", 
                "docker0"};
        
        ListBox<String> lb1 = ListBox.fromStrings(Colors.IGre,
                                                new String[] {
                                               "Blue Suede Shoes", 
                                               "Red Shoes", 
                                               "Pink Shoes",
                                               "Pink Flamingos",
                                               "T'was brilling and the slithy toth\ndid gere and gimble in the wabe.\nAll mimsey were the borogroves\nAnd the momewraths outgrab",
                                               "Moss filled turtles",
                                               "Santa Claus\nIs quite fat!",
                                               "Mrs Claus\nis OK with that!",
                                               });
        
        ListBox<String> lb1x = ListBox.fromStrings(Colors.IGre, iFaces);

        
        lb1.setBorder(true);
        lb1.setTitle("Ifaces");
        
        StringDisplayValue[] weird = StringDisplayValue.fromStringValues(new String[] {
                "I am", 
                "Forever known", 
                "As the man",
                "with one red shoe"
                });
        
        StringDisplayValue[] raindeer = StringDisplayValue.fromStringValues(new String[] {
                "Comet", 
                "Blitzen", 
                "Donner",
                "Rudolf"
                });

        ListBox<String> lb2 = new ListBox<String>(Colors.IGre, weird);



         lb2.setBorder(true);
         
         HorizontalPanel listBoxes = new HorizontalPanel().setHorizontalSpacer(5);
         
         listBoxes.add(lb1);
         listBoxes.add(lb2);
         
        
        TextComponent eventDisplayer = new TextComponent("                                       ");
        
        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(listBoxes.getHeight()+anchorPoint.getRow()+2+4, listBoxes.getWidth()+ anchorPoint.getCol()+ 5);
        master_canvas.setEmptyChar(' ');
        
        Runnable renderComponents = ()-> {
            master_canvas.clearCanvas();
            master_canvas.addComponentSegments(anchorPoint, listBoxes);
            master_canvas.addComponentSegments(new PositionVerticalRelative(new Rectangle.Simple(anchorPoint, lb1),1), eventDisplayer);
            master_canvas.refresh();
        };
        
        renderComponents.run();

        screen.add(ControlCode.HIDE_CURSOR);
        
       EventBus eventBus = new EventBus();
       KeyInputEventsProducer keyInputEventsProducer = null;
       try {
           keyInputEventsProducer = new KeyInputEventsProducer(eventBus);
       } catch (Exception e) {
           eventBus.stopConsuming();
           return;
       }

       EventHandler terminate = new EventHandler() {

           @Override
           public void handle(TerminalEvent<?> te) {
               eventBus.stopConsuming();
               screen.add(ControlCode.SHOW_CURSOR);
               screen.refresh();

               try {
                   Stty.setCooked();
               } catch (IOException e) {
                   e.printStackTrace();
               }
               // next refresh() is necessary to get cursor to be below
               // rendered canvas
               master_canvas.refresh();
               return;
           }
       };
       
       
       eventBus.appendEventHandler( new HasValue.OnValueChangedEventHandler<String>(lb1) {
           
           StringDisplayValue[]  currentList = weird;
           
           @Override
           public void valueChanged(HasValue<String> source) {
               String newValue = source.getValue();
               eventDisplayer.setTextAndResize("Pressed = "+newValue);
               
               StringDisplayValue[]  newList = currentList;

               if (newValue.contains("Claus")) {
                   newList = raindeer;
               } else {
                   newList = weird;
               }
               if (newList != currentList) {
                  lb2.setItems(Arrays.asList(newList));
                  currentList = newList;
               }
               renderComponents.run();
           }
       });
       
       eventBus.appendEventHandler( new HasValue.OnValueChangedEventHandler<String>(lb2) {

           @Override
           public void valueChanged(HasValue<String> source) {
               String newValue = source.getValue();
               eventDisplayer.setTextAndResize("Pressed = "+newValue);
               renderComponents.run();
           }
       });
       
       eventBus.disableIdleEvents();
       
       eventBus.appendEventHandler(master_canvas.asShowCursorEventHandler());
       eventBus.appendEventHandler(master_canvas.asHideCursorEventHandler());

       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));

       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));

       TestsCommon.addNoGraphicsToggleEvent(eventBus, screen, renderComponents);
       
       FocusManager fm = new FocusManager(Keys.LEFT_KEYSTROKE, Keys.RIGHT_KEYSTROKE).setOnFocusChanged((f)->renderComponents.run());
       fm.addFocusable( lb1.asFocusable() , eventBus );
       fm.addFocusable( lb2.asFocusable(), eventBus  );
       
       
       eventBus.appendEventHandler(fm);
       
    }
    
    
}
