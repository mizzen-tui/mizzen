package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.KeyInputEventsProducer;
import dtb.terminalIO.events.Keys;
import dtb.terminalIO.events.Keys.KeyStroke;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.TYPE;
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
    
    
    public static void main(String[] args) throws InterruptedException, IOException {

        EventBus eventBus = new EventBus();
        try {
            KeyInputEventsProducer keyInputEventsProducer = null;
            try {
                keyInputEventsProducer = new KeyInputEventsProducer(eventBus);
            } catch (Exception e) {
                eventBus.stopConsuming();
                return;
            }

            if (keyInputEventsProducer.isActive()) {

                CompositeTest.main(args);

                eventBus.addEventHandler(new ConditionalEventHandler() {

                    @Override
                    public void handle(TerminalEvent<?> te) {
                        CompositeTest.statusMsg2(String.valueOf(te.getEventData()));
                    }

                    @Override
                    public EventHandler canHandle(TerminalEvent<?> te) {
                        if (te.getType().equals(TYPE.DEBUG))
                            return this;
                        return null;
                    }
                    
                });
                
                
                eventBus.addEventHandler(new ConditionalEventHandler() {

                    @Override
                    public void handle(TerminalEvent<?> te) {

                        Keys.KeyEvent ke = (Keys.KeyEvent) te;

                        // eventBus.stopConsuming();

                        KeyStroke A_key = new KeyStroke('A',"A");
                        KeyStroke A2_key = new KeyStroke('A');
                        
                        if (A_key.equals(A2_key)) {
                            CompositeTest.statusMsg("A = A2");
                        }
                        
                        if (A2_key.equals(A_key)) {
                            CompositeTest.statusMsg("A2 = A");
                        }
                        
                        if (ke.isKeyStroke(A_key)) {
                            CompositeTest.statusMsg("A KEY HIT!!!");
                        }
                        
                        if (ke.isKeyStroke(Keys.ESC_KEYSTROKE)) {
                            CompositeTest.statusMsg(ke.getKeyName() + " hit. Quitting..");
                            eventBus.stopConsuming();
                            try {
                                Stty.setCooked();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                        if (1+1==3 && ke.isKeyStroke(Keys.CTRL_C_KEYSTROKE)) {
                            CompositeTest.statusMsg(ke.getKeyName() + " hit. Quitting.." + Keys.CTRL_C_KEYSTROKE.getName());
                            eventBus.stopConsuming();
                            return;
                        }
//                        if (ke.isKeyStroke(Keys.A_KEYSTROKE)) {
//                            CompositeTest.statusMsg("A hit. Quitting..");
//                            eventBus.stopConsuming();
//                            return;
//                        }

                        CompositeTest.statusMsg(String.valueOf(ke.getEventData()));
                    }

                    @Override
                    public EventHandler canHandle(TerminalEvent<?> te) {
                        if (te.getType().equals(TYPE.KEYPRESS))
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
