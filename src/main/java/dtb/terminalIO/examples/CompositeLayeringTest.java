package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventBusImpl;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.InputEventsProducer;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Stty;

public class CompositeLayeringTest {

    public static void main(String[] args) throws IOException { 

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);
        
        Composite container = new Composite();
        
        container.setBounds(new Dimension.Simple(20,50));
        
        container.t(1,1, "Demo container layering and child moving");
        
        int j=0 ;
        for (int i = 0; i < 3; i++) {
            
            j+=2;
            
            Component<?> c = new BorderedComponent(new TextComponent(j+" : Box\n"+j+" XXX"));
            
            container.add(j,j, c);
            
        }

        j+=2;
        
        Component<?> the_c = new BorderedComponent(Colors.IYel,new TextComponent(Colors.IYel,new Dimension.Simple(10,20),
                " UP     : Layer up\n"+
                " DOWN   : Layer down\n"+
                " PgUp   : Layer Top\n"+
                " PgDn   : Layer Bottom \n"+
                "  ------------------\n"+
                " CTRL-T : up\n"+
                " CTRL-V : down\n"+
                " CTRL-F : left\n"+
                " CTRL-G : right\n"
             ));
        
        
        container.add(3,7, the_c);
        
        
        for (int i = 0; i < 3; i++) {
            
            j+=2;
            
            Component<?> c = new BorderedComponent(new TextComponent(j+" : Box\n"+j+" XXX"));
            
            container.add(j,j, c);
            
        }

        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(container.getHeight()+4, container.getWidth()+ 5);
        master_canvas.setEmptyChar(' ');
        
        
        
        Runnable renderComponents = ()-> {
            master_canvas.clearCanvas();
            master_canvas.addComponentSegments(1,1, container);
            master_canvas.refresh();
        };
        
        
        renderComponents.run();
        
        screen.add(ControlCode.HIDE_CURSOR);
        renderComponents.run();

        
       EventBus eventBus = new EventBusImpl();
       InputEventsProducer keyInputEventsProducer = null;
       try {
           keyInputEventsProducer = new InputEventsProducer(eventBus);
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
        
       eventBus.disableIdleEvents();
       
       eventBus.appendEventHandler(master_canvas.asShowCursorEventHandler());
       eventBus.appendEventHandler(master_canvas.asHideCursorEventHandler());

       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));

       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));

       TestsCommon.addNoGraphicsToggleEvent(eventBus, screen, renderComponents);
       
       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.DOWN_KEYSTROKE, (te)-> {container.moveDown(the_c); renderComponents.run();} ));
        
       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.UP_KEYSTROKE, (te)-> {container.moveUp(the_c); renderComponents.run();} ));
        
       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.PGDN_KEYSTROKE, (te)-> {container.bringToBack(the_c); renderComponents.run();} ));
       
       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.PGUP_KEYSTROKE, (te)-> {container.bringToFront(the_c); renderComponents.run();} ));
        
        
       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_T_KEYSTROKE, (te)-> {container.moveChildDelta(new Position.Simple(-1,0),the_c); renderComponents.run();} ));
       
       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_F_KEYSTROKE, (te)-> {container.moveChildDelta(new Position.Simple(0,-1),the_c); renderComponents.run();} ));
        
       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_G_KEYSTROKE, (te)-> {container.moveChildDelta(new Position.Simple(0,1),the_c); renderComponents.run();} ));
       
       eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_V_KEYSTROKE, (te)-> {container.moveChildDelta(new Position.Simple(1,0),the_c); renderComponents.run();} ));
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
        
    }

}
