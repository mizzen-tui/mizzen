package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.Arrays;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.ListBox.TextComponentItemFactory;
import dtb.terminalIO.components.modal.MessageBox;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.OnRequestUpdateCanvasEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
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
                                               "You thought we were done?",
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
        
        DropdownListBox<String> lb3 = DropdownListBox.fromStrings(new String[] {"Test1", "Test2", "No tests", "No more teacher's","dirty looks"}, new TextComponentItemFactory<String>());
        DropdownListBox<Boolean> lb4 = DropdownListBox.yesNoDropDown();
        // lb3.setBorder(true);

        HorizontalPanel listBoxesPanel = new HorizontalPanel().setHorizontalSpacer(4);
         
        listBoxesPanel.add(lb1_sc);
        listBoxesPanel.add(lb2);
        
        VerticalPanel droppablesPanel = new VerticalPanel();

        droppablesPanel.add(lb3);
        droppablesPanel.add(lb4);

        listBoxesPanel.add(droppablesPanel);

        TextComponent eventDisplayer = new TextComponent("                                       ");
        
        Canvas master_canvas = new Canvas(screen);

        master_canvas.setSize(listBoxesPanel.getHeight()+anchorPoint.getRow()+2+4, listBoxesPanel.getWidth()+ anchorPoint.getCol()+ 10);
        master_canvas.setEmptyChar(' ');

        Composite mainPanel = new Composite(master_canvas.getTextColor());
        mainPanel.resize((Dimension)master_canvas);
        mainPanel.setEmptyChar(' ');
        mainPanel.setEmptyColor(master_canvas.getEmptyColor());

        mainPanel.add(anchorPoint, listBoxesPanel);
        mainPanel.add(new PositionVerticalRelative(new Rectangle.Simple(anchorPoint, lb1_sc),1), eventDisplayer);
        
        mainPanel.setBorder(true);
        

        Runnable renderComponents = ()-> {
            
            if ( ! Component.UTIL.isDirty(mainPanel, listBoxesPanel, eventDisplayer)) {
                return ;
            }
            
            master_canvas.clearCanvas();
            
            master_canvas.addComponentSegments(mainPanel);
            /*
            master_canvas.addComponentSegments(anchorPoint, listBoxes);
            
            master_canvas.addComponentSegments(new PositionVerticalRelative(new Rectangle.Simple(anchorPoint, lb1_sc),1), eventDisplayer);
            */
            
            
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
               } catch (Exception e) {
                   e.printStackTrace();
               }
               // next refresh() is necessary to get cursor to be below
               // rendered canvas
               master_canvas.refresh();
               return;
           }
       };
       
       eventBus.appendEventHandler(new OnRequestUpdateCanvasEventHandler(renderComponents));
       
       
       eventBus.appendEventHandler( new HasValue.OnValueChangedEventHandler<String>(lb1) {
           
           StringDisplayValue[]  currentList = weird;
           
           @Override
           public void valueChanged(HasValue<String> source) {
               String newValue = source.getValue();
               eventDisplayer.setTextAndResize("Pressed = "+newValue);
               
               if ("WAY!".equals(newValue)) {
                   MessageBox.showInfoDialog( eventBus , mainPanel, "Way to go Joe!");
               }
               if ("Blue Suede Shoes".equals(newValue)) {
                   MessageBox.showErrorDialog( eventBus , mainPanel, "Don't step on my Blue Suede shoes!");
               }
               
               if ("Pink Flamingos".equals(newValue)) {
                   MessageBox.showWarningDialog( eventBus , mainPanel, "Not in my front lawn!\nTOO TACKY!!");
               }
               
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
       
       eventBus.appendEventHandler(
               new ConditionalEventHandler.Impl(new IS_Event_TYPE(TYPE.EXCEPTION), new EventHandler() {
                   @Override
                   public void handle(TerminalEvent<?> te) {
                       terminate.handle(te);
                       ((Exception)te.getEventData()).printStackTrace();
                   }
               }));

       
       eventBus.appendEventHandler(master_canvas.asShowCursorEventHandler());
       eventBus.appendEventHandler(master_canvas.asHideCursorEventHandler());

       EventHandler confirmTerminate = (te)->{

               MessageBox.showYesNoDialog(te.getEventBus(), 
                                          mainPanel,
                                          "  Exit application?  ",
                                          (e)->terminate.handle(te));


       };

       eventBus.appendEventHandler(new KeyInputEventHandler(confirmTerminate, Keys.CTRL_C_KEYSTROKE, Keys.ESC_KEYSTROKE));

       TestsCommon.addNoGraphicsToggleEvent(eventBus, screen, renderComponents);
       
       DefaultFocusManager fm = new DefaultFocusManager(Keys.LEFT_KEYSTROKE, Keys.RIGHT_KEYSTROKE).setOnFocusChanged((f)->renderComponents.run());
       fm.addFocusable( lb1_sc, eventBus );
       fm.addFocusable( lb2.asFocusable(), eventBus  );
       fm.addFocusable( lb3, eventBus  );
       fm.addFocusable( lb4, eventBus  );
       
       lb1_sc.prependEventHandler( lb1.asFocusable() );
       SearchFunctionForScrollableComponent.addFinderToScrollable(lb1_sc);

       
       eventBus.appendEventHandler(fm);
       
       eventBus.appendIdleEventHandler( new OnIdleEventEventHandler.RepeatingInterval(200, (te)-> renderComponents.run() ));

    }
    
    
}
