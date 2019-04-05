package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.CheckBoxInput;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.FocusManager;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
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
        
        master_canvas.t("Some text");
        
        master_canvas.refresh();
        
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
        
        Button b_1 = new Button("Press Me", true);
        Position p_6 = new Position.Simple(12,10);
        
        Button b_2 = new Button("Press Too");
        Position p_7 = new Position.Simple(13,10);
                
        Composite  bBar = new HorizontalPanel(Colors.BBlu);
        
        
        TextComponent eventDisplayer = new TextComponent("                                       ");
        
        Runnable renderComponents = () -> {
            
            if ( ! Component.UTIL.isDirty(ti, ti_2, cb_1, ti_3, ti_4, b_1, b_2, eventDisplayer)) {
                return ;
            }
            
            master_canvas.clearCanvas();
            
            master_canvas.t(1,1,String.valueOf(p_1));
            
            master_canvas.addComponentSegments(p_1,ti);
            
            master_canvas.t(2,1,String.valueOf(Component.UTIL.getCanvasOffsetFor(ti)) );
            
            master_canvas.addComponentSegments(p_2,ti_2);
            master_canvas.addComponentSegments(p_5,cb_1);
            master_canvas.addComponentSegments(p_6,b_1);
            master_canvas.addComponentSegments(p_7,b_2);
            
            
            master_canvas.addComponentSegments(p_3,ti_3);
            master_canvas.addComponentSegments(p_4,ti_4);
            
            master_canvas.addComponentSegments(master_canvas.getHeight()-2, 2, eventDisplayer);
            
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

        StringBuffer logger = new StringBuffer();

        Runnable stopThePresses = new Runnable() {
            
            @Override
            public void run() {
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
                
                
                System.out.println("\n\n"+logger.toString());
                
                return;
            }
        };
        
        
        EventHandler terminate = new EventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {
                stopThePresses.run();
                if (te.getType().equals(TYPE.EXCEPTION)) {
                    System.out.println("\n");
                    ((Exception) te.getEventData()).printStackTrace();
                } else {
                    // next refresh() is necessary to get cursor to be below
                    // rendered canvas
                    master_canvas.refresh();
                }
                
            }
        };
        
        
        eventBus.addEventHandler(master_canvas.asShowCursorEventHandler());
        eventBus.addEventHandler(master_canvas.asHideCursorEventHandler());

        eventBus.addEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));
        eventBus.addEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));
        eventBus.addEventHandler(new ConditionalEventHandler.Impl(new IS_Event_TYPE(TYPE.EXCEPTION), terminate));
        
        
        FocusManager focusManager = new FocusManager(Keys.TAB_KEYSTROKE);
        
        FocusManager focusManager1 = new FocusManager();
        FocusManager focusManager2 = new FocusManager();

        focusManager1.addFocusable(ti, eventBus);
        focusManager1.addFocusable(ti_2, eventBus);
        focusManager1.addFocusable(cb_1, eventBus);
        focusManager1.addFocusable(b_1, eventBus);
        focusManager1.addFocusable(b_2, eventBus);
        
        focusManager2.addFocusable(ti_3, eventBus);
        focusManager2.addFocusable(ti_4, eventBus); 
        

        focusManager.addFocusable(focusManager1, eventBus);
        focusManager.addFocusable(focusManager2, eventBus);

        
        eventBus.addEventHandler(focusManager);
        
        eventBus.addEventHandler( new OnIdleEventEventHandler.RepeatingInterval(100, (te) -> {
                                          renderComponents.run();                       
                                     }));
        
        
        eventBus.addEventHandler( new HasValue.OnValueChangedEventHandler<String>(ti) {
            @Override
            public void valueChanged(HasValue<String> source) {
                String newValue = source.getValue();
                eventDisplayer.setText(newValue);
                renderComponents.run();
            }
        });
        
        eventBus.addEventHandler( new HasValue.OnValueChangedEventHandler<Boolean>(b_1) {
            @Override
            public void valueChanged(HasValue<Boolean> source) {
                Boolean newValue = source.getValue();
                eventDisplayer.setText("(1) Pressed = "+newValue);
                renderComponents.run();
            }
        });
        
        
        eventBus.addEventHandler( new HasValue.OnValueChangedEventHandler<Boolean>(b_2) {
            public void handle(TerminalEvent<?> te) {
                super.handle(te);
                ti_4.requestFocus(te);
                renderComponents.run();
            }
            
            
            @Override
            public void valueChanged(HasValue<Boolean> source) {
                Boolean newValue = source.getValue();
                eventDisplayer.setText("(2) Pressed = "+newValue);
            }
            
        });
        

        screen.add(ControlCode.SHOW_CURSOR);
        
    }
    
    
}
