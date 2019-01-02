package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.ControlCode;
import dtb.terminalIO.TerminalScreen;
import dtb.terminalIO.components.TextInput;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.FocusManager;
import dtb.terminalIO.events.KeyInputEventHandler;
import dtb.terminalIO.events.KeyInputEventsProducer;
import dtb.terminalIO.events.Keys;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.util.Stty;

public class TextBoxTest {

    
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub

        //Stty.setRaw();
        
        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = new Canvas(screen);
        master_canvas.setSize(20, 60);
        
        screen.setRaw();
        
        master_canvas.setEmptyChar('.');
        
        master_canvas.t("Some text");
        
        TextInput ti = new TextInput(10);
        
        Position p_1 = new Position.Simple(5,10);
        
        ti.setText("Test");
        
        TextInput ti_2 = new TextInput(15);
        
        ti_2.setText("Test 2");
        Position p_2 = new Position.Simple(7,10);
        
        
        Runnable renderComponents = () -> {
            master_canvas.addComponentSegments(p_1,ti);
            master_canvas.addComponentSegments(p_2,ti_2);
            master_canvas.refresh();
            
        };
        
        renderComponents.run();
        
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

        eventBus.addEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));

        eventBus.addEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));
        
        FocusManager focusManager = new FocusManager();
        
        eventBus.addEventHandler(focusManager.asEventHandler());

        focusManager.addFocusable(ti.asFocusable(p_1));
        focusManager.addFocusable(ti_2.asFocusable(p_2));
        
        //eventBus.addEventHandler(ti.asEventHandler());
        
        //eventBus.addEventHandler(ti.asEventHandler());

        //eventBus.addEventHandler(ti_2.asEventHandler());
        
        eventBus.addEventHandler( new OnIdleEventEventHandler.RepeatingInterval(100, (te) -> {
                                          renderComponents.run();                       
                                     }));
        
        screen.add(ControlCode.SHOW_CURSOR);
        
    }
    
    
}
