package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.Arrays;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.ListBox.TextComponentItemFactory;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyInputEventsProducer;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
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
                                               "Let's agree on one thing",
                                               "We don't talk about Rudolf",
                                               "I  mean come on..\nHe's got a red nose",
                                               "He's a red nosed reindeer after all",
                                               "1\n2\n3\n4\n5\6\n6\n7\n8\n9\n10\n11\n12\n13\n14\n15\n16",
                                               "What?",
                                               "You though we were done?",
                                               "NO!",
                                               "WAY!"
                                               });
        
        
        lb1.setBorder(false);
        //lb1.setTitle("Ifaces");
        
        ScrollableComponent lb1_sc = new ScrollableComponent(Colors.Blu, new Dimension.Simple(Math.min(15,lb1.getHeight()-5), lb1.getWidth()+20), lb1);
        
        
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

        ListBox<String> lb2 = new ListBox<String>(Colors.IGre, weird, new TextComponentItemFactory<String>());
        // TODO: make it so we can specify border color/ title color
        lb2.setTitle("Child list");
        
        ListBox<String> lb3 = ListBox.fromStrings(new String[] {"Test1", "Test2"}, new TextComponentItemFactory<String>());
       // lb3.setBorder(true);
        
         
        HorizontalPanel listBoxes = new HorizontalPanel().setHorizontalSpacer(4);
         
        listBoxes.add(lb1_sc);
        listBoxes.add(lb2);
        listBoxes.add(lb3);
         
        
        TextComponent eventDisplayer = new TextComponent("                                       ");
        
        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(listBoxes.getHeight()+anchorPoint.getRow()+2+4, listBoxes.getWidth()+ anchorPoint.getCol()+ 10);
        master_canvas.setEmptyChar(' ');
        
        Runnable renderComponents = ()-> {
            
            if ( ! Component.UTIL.isDirty(listBoxes, eventDisplayer)) {
                return ;
            }
            
            master_canvas.clearCanvas();
            master_canvas.addComponentSegments(anchorPoint, listBoxes);
            master_canvas.addComponentSegments(new PositionVerticalRelative(new Rectangle.Simple(anchorPoint, lb1_sc),1), eventDisplayer);
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
       
       eventBus.appendEventHandler(master_canvas.asShowCursorEventHandler());
       eventBus.appendEventHandler(master_canvas.asHideCursorEventHandler());

       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));

       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));

       TestsCommon.addNoGraphicsToggleEvent(eventBus, screen, renderComponents);
       
       DefaultFocusManager fm = new DefaultFocusManager(Keys.LEFT_KEYSTROKE, Keys.RIGHT_KEYSTROKE).setOnFocusChanged((f)->renderComponents.run());
       fm.addFocusable( lb1_sc, eventBus );
       fm.addFocusable( lb2.asFocusable(), eventBus  );
       fm.addFocusable( lb3.asFocusable(), eventBus  );
       
       
       lb1_sc.setUpdateCanvas(renderComponents);
       lb1_sc.prependEventHandler( lb1.asFocusable() );
       SearchFunctionForScrollableComponent.addFinderToScrollable(lb1_sc);

       
       eventBus.appendEventHandler(fm);
       
       eventBus.appendIdleEventHandler( new OnIdleEventEventHandler.RepeatingInterval(200, (te)-> renderComponents.run() ));

    }
    
    
}
