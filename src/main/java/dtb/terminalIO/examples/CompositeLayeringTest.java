package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.EventHandlerWithCanvasUpdate;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;

public class CompositeLayeringTest {

    public static void main(String[] args) throws IOException { 
        
        new TUIApp(args).setCanvasSize(new Dimension.Simple(20,50))
                    .setDefaultMainApplicationPanel()
                    .confirmOnExit()
                    .setOnReadyStart((tApp)->{
                        
             
                        
            Composite container = (Composite)tApp.getMainApplicationPanel();
            
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
            
            ChainedEventHandler keyboardEvents = ChainedEventHandler.useChainOfResponsibilityStrategy();
            
            keyboardEvents.appendEventHandler(new KeyInputEventHandler(Keys.DOWN_KEYSTROKE, (te)->  container.moveDown(the_c) ));
            
            keyboardEvents.appendEventHandler(new KeyInputEventHandler(Keys.UP_KEYSTROKE, (te)->  container.moveUp(the_c) ));
             
            keyboardEvents.appendEventHandler(new KeyInputEventHandler(Keys.PGDN_KEYSTROKE, (te)->  container.bringToBack(the_c) ));
            
            keyboardEvents.appendEventHandler(new KeyInputEventHandler(Keys.PGUP_KEYSTROKE, (te)->  container.bringToFront(the_c) ));
             
             
            keyboardEvents.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_T_KEYSTROKE, (te)->  container.moveChildDelta(new Position.Simple(-1,0),the_c) ));
            
            keyboardEvents.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_F_KEYSTROKE, (te)->  container.moveChildDelta(new Position.Simple(0,-1),the_c) ));
             
            keyboardEvents.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_G_KEYSTROKE, (te)->  container.moveChildDelta(new Position.Simple(0,1),the_c) ));

            keyboardEvents.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_V_KEYSTROKE, (te)->  container.moveChildDelta(new Position.Simple(1,0),the_c)));

            tApp.getEventBus().appendEventHandler( new EventHandlerWithCanvasUpdate(keyboardEvents.asConditionalEventHandler()));

        }).start();

    }

}
