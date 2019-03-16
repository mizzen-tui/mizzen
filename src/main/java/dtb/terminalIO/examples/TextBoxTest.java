package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.input.CheckBoxInput;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.FocusManager;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyInputEventsProducer;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.util.Stty;

public class TextBoxTest {
    
    public static void main(String[] args) throws IOException {

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
        

        TextInput ti_3 = new TextInput(15);
        ti_3.setText("Test3");
        Position p_3 = new Position.Simple(4,30);

        TextInput ti_4 = new TextInput(15);
        ti_4.setText("Test4");
        Position p_4 = new Position.Simple(10,30);
        
        CheckBoxInput cb_1 = new CheckBoxInput();
        cb_1.setValue(false);
        Position p_5 = new Position.Simple(10,10);
        
        Runnable renderComponents = () -> {
            
            if ( ! Component.UTIL.isDirty(ti, ti_2, cb_1, ti_3, ti_4)) {
                return ;
            }
            
            
            master_canvas.addComponentSegments(p_1,ti);
            master_canvas.addComponentSegments(p_2,ti_2);
            master_canvas.addComponentSegments(p_5,cb_1);
            
            
            master_canvas.addComponentSegments(p_3,ti_3);
            master_canvas.addComponentSegments(p_4,ti_4);
            
            
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

        eventBus.addEventHandler(master_canvas.asShowCursorEventHandler());
        eventBus.addEventHandler(master_canvas.asHideCursorEventHandler());

        eventBus.addEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));

        eventBus.addEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));
        
        //eventBus.addEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));
        
        
        
        FocusManager focusManager = new FocusManager(Keys.TAB_KEYSTROKE);
        
        FocusManager focusManager1 = new FocusManager();
        FocusManager focusManager2 = new FocusManager();

        focusManager1.addFocusable(ti.asFocusable(p_1, eventBus));
        focusManager1.addFocusable(ti_2.asFocusable(p_2, eventBus));
        focusManager1.addFocusable(cb_1.asFocusable(p_5));
        
        focusManager2.addFocusable(ti_3.asFocusable(p_3, eventBus));
        focusManager2.addFocusable(ti_4.asFocusable(p_4, eventBus));
        

        focusManager.addFocusable(focusManager1.asFocusable());
        focusManager.addFocusable(focusManager2.asFocusable());

        
        eventBus.addEventHandler(focusManager.asEventHandler());
        
        
        //eventBus.addEventHandler(ti.asEventHandler());
        
        //eventBus.addEventHandler(ti.asEventHandler());

        //eventBus.addEventHandler(ti_2.asEventHandler());
        
        eventBus.addEventHandler( new OnIdleEventEventHandler.RepeatingInterval(100, (te) -> {
                                          renderComponents.run();                       
                                     }));
        
        screen.add(ControlCode.SHOW_CURSOR);
        
    }
    
    
}
