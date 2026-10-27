package mizzen.examples;

import java.util.function.Consumer;

import mizzen.controlcodes.ControlCode;
import mizzen.controlcodes.ControlCode.BaseTextControlCode;
import mizzen.core.Canvas;
import mizzen.core.TerminalScreen;
import mizzen.events.CPREvent;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.DSREvent;
import mizzen.events.EventBus;
import mizzen.events.EventBusImpl;
import mizzen.events.EventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.TerminalEvent.IS_Event_TYPE;
import mizzen.events.TerminalEvent.TYPE;
import mizzen.events.keyboard.InputEventsProducer;
import mizzen.events.keyboard.KeyEvent;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.keyboard.Keys;
import mizzen.events.mouse.MouseEventOrchestrator;
import mizzen.events.mouse.RawParsedFromConsoleMouseEvent;
import mizzen.util.Logging;
import mizzen.util.Stty;

/**
 * Tests KeyInputEventsProducer.
 * 
 * <p/>
 * 
 * Its also a good utility to display key codes when user presses a key.
 * 
 * <p/>
 * 
 * I used this class to help define the keys in the class ${Keys}
 * 
 */
public class KeyInputEventsProducerTest {
    
    
    public static void main(String[] args) throws Exception {

        
        Logging.setEnabled(true);
        
        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
        master_canvas.setSize(10, master_canvas.getWidth());
        master_canvas.setEmptyChar(' ');

        Consumer<String> status1Displayer = (msg) -> {
            String msgFormatted = String.format("[  %-"+master_canvas.getWidth()+"s  ]", msg);
            // System.err.println(msgFormatted);
            master_canvas.t(master_canvas.getHeight()-5 , 3, msgFormatted);
            master_canvas.refresh();
        };
        
        Consumer<String> status1Displayer2 = (msg) -> {
            String msgFormatted = String.format("[  %-"+master_canvas.getWidth()+"s  ]", msg);
            // System.err.println(msgFormatted);
            master_canvas.t(master_canvas.getHeight()-7 , 3, msgFormatted);
            master_canvas.refresh();
        };

        Runnable textualizeMouseEnable = ()-> {
            screen.add( new BaseTextControlCode(ControlCode.escape_CHR + "[?1000h"));
            screen.add( new BaseTextControlCode(ControlCode.escape_CHR + "[?1003h"));
            screen.add( new BaseTextControlCode(ControlCode.escape_CHR + "[?1015h"));
            screen.add( new BaseTextControlCode(ControlCode.escape_CHR + "[?1006h"));
        };

        Runnable mouseEnableViaOrchestrator = ()-> {
        	MouseEventOrchestrator.disableAllMouseModes(screen);
        	MouseEventOrchestrator.enableMouseMode(screen);
        	//MouseEventOrchestrator.disableAllMouseModes(screen);
        };
        
        /*
         * write("\x1b[?1000h")  # SET_VT200_MOUSE
        write("\x1b[?1003h")  # SET_ANY_EVENT_MOUSE
        write("\x1b[?1015h")  # SET_VT200_HIGHLIGHT_MOUSE
        write("\x1b[?1006h")  # SET_SGR_EXT_MODE_MOUSE
         * 
         * */
        
        Runnable normalMouseEnable = ()-> {
            screen.add( ControlCode.SET_VT200_MOUSE );
            screen.add( ControlCode.ENABLE_MOUSE_TRACKING );
            //screen.add( ControlCode.SET_FOCUS_EVENT_MOUSE );
            //screen.add( ControlCode.SET_BTN_EVENT_MOUSE );
            screen.add( ControlCode.SET_ANY_EVENT_MOUSE );
            screen.add( ControlCode.SET_EXT_MODE_MOUSE );
            //screen.add( ControlCode.SET_EXT_MODE_MOUSE );
        };
        
        
        //textualizeMouseEnable.run();
        //normalMouseEnable.run();
        mouseEnableViaOrchestrator.run();
        
        // now flush to stdout
        screen.refresh();
        
        master_canvas.t(1,1, "Keyboard Tester ");
        
        master_canvas.refresh();
        
        EventBus eventBus = new EventBusImpl();
        try {
            InputEventsProducer keyInputEventsProducer = null;
            try {
                keyInputEventsProducer = new InputEventsProducer(eventBus);
            } catch (Exception e) {
                eventBus.stopConsuming();
                return;
            }

            if (keyInputEventsProducer.isActive()) {

                eventBus.appendEventHandler(new ConditionalEventHandler() {

                    @Override
                    public void handle(TerminalEvent<?> te) {
                        status1Displayer2.accept(String.valueOf(te.getEventData()));
                    }

                    @Override
                    public EventHandler canHandle(TerminalEvent<?> te) {
                        if (te.getType().equals(TYPE.DEBUG))
                            return this;
                        return null;
                    }
                    
                });
                
                
                EventHandler terminate = new EventHandler() {

                    @Override
                    public void handle(TerminalEvent<?> te) {
                        eventBus.stopConsuming();
                        MouseEventOrchestrator.disableAllMouseModes(screen);
                        screen.add(ControlCode.SHOW_CURSOR);
                        screen.refresh();

                        try {
                            Stty.setCooked();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (te.getType().equals(TYPE.EXCEPTION)) {
                            System.out.println("\n");
                            ((Exception) te.getEventData()).printStackTrace();
                        } else {
                            // next refresh() is necessary to get cursor to be below
                            // rendered canvas
                            master_canvas.refresh();
                        }
                        return;
                    }
                };
                
                eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));
                eventBus.appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));
                eventBus.appendEventHandler(new ConditionalEventHandler.Impl(new IS_Event_TYPE(TYPE.EXCEPTION), terminate));
                
                
                eventBus.appendEventHandler(new ConditionalEventHandler() {

                    @Override
                    public void handle(TerminalEvent<?> te) {

                        
                        if (te instanceof RawParsedFromConsoleMouseEvent) {
                            status1Displayer.accept(  String.valueOf(te) );
                            
                            return;
                        }
                        if (te instanceof CPREvent || te instanceof DSREvent) {
                            status1Displayer.accept(  String.valueOf(te) );
                            return;
                        }
                        
                        if (te instanceof DSREvent || te instanceof DSREvent) {
                            status1Displayer.accept(  String.valueOf(te) );
                            return;
                        }
                        
                        KeyEvent ke = (KeyEvent) te;

                        // eventBus.stopConsuming();

                        KeyStroke A_key = new KeyStroke('A',"A");
                        KeyStroke A2_key = new KeyStroke('A');
                        
                        if (A_key.equals(A2_key)) {
                            status1Displayer.accept("A = A2");
                        }
                        
                        if (A2_key.equals(A_key)) {
                            status1Displayer.accept("A2 = A");
                        }
                        
                        if (ke.isKeyStroke(A_key)) {
                            status1Displayer.accept("A KEY HIT!!!");
                        }
                        
                        if (ke.isKeyStroke(Keys.CTRL_K_KEYSTROKE)) {
                            //status1Displayer.accept(  "Test cursor report" );
                            screen.t("                         ");
                            screen.add(ControlCode.CPR);
                            screen.refresh();
                        }

                        if (ke.isKeyStroke(Keys.CTRL_L_KEYSTROKE)) {
                            screen.t(" ");
                            screen.add(ControlCode.DSR);
                            screen.refresh();
                        }
                        
                        
                        if (ke.isKeyStroke(Keys.ESC_KEYSTROKE)) {
                            status1Displayer.accept(ke.getKeyName() + " hit. Quitting..");
                            MouseEventOrchestrator.disableAllMouseModes(screen);
                            Logging.info("mouse disabled");
                            terminate.handle(ke);
                            System.out.println("Exited!!!");
                            return;
                        }
                        if (1+1==3 && ke.isKeyStroke(Keys.CTRL_C_KEYSTROKE)) {
                            status1Displayer.accept(ke.getKeyName() + " hit. Quitting.." + Keys.CTRL_C_KEYSTROKE.getName());
                            MouseEventOrchestrator.disableAllMouseModes(screen);
                            terminate.handle(ke);
                            return;
                        }

                        status1Displayer.accept(String.valueOf(ke.getEventData()));
                    }

                    @Override
                    public EventHandler canHandle(TerminalEvent<?> te) {
                        if (te.getType().equals(TYPE.KEYPRESS))
                            return this;
                        if (te.getType().equals(TYPE.RAW_MOUSE))
                            return this;
                        if (te.getType().equals(TYPE.DSR_REPORT))
                            return this;
                        if (te.getType().equals(TYPE.CPR_REPORT))
                            return this;
                        return null;
                    }
                });

               // keyInputEventsProducer.readThread.join();
               // eventBus.stopConsuming();
            } else {
                eventBus.stopConsuming();
            }

            

        } catch (Exception e) {
            eventBus.stopConsuming();
            Stty.setCooked();
            System.out.println("");
            e.printStackTrace();
        }
    }
    

}
