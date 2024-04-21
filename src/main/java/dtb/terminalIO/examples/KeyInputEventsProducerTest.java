package dtb.terminalIO.examples;

import java.util.function.Consumer;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.BaseTextControlCode;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.CPREvent;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.DSREvent;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventBusImpl;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.keyboard.InputEventsProducer;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.RawParsedFromConsoleMouseEvent;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.Stty;

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
        
        
        textualizeMouseEnable.run();
        //normalMouseEnable.run();
        
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
                            terminate.handle(ke);
                            return;
                        }
                        if (1+1==3 && ke.isKeyStroke(Keys.CTRL_C_KEYSTROKE)) {
                            status1Displayer.accept(ke.getKeyName() + " hit. Quitting.." + Keys.CTRL_C_KEYSTROKE.getName());
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
