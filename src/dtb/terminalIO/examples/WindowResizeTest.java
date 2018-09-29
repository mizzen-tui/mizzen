package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.function.Consumer;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.TerminalScreen;
import dtb.terminalIO.components.Box;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.RectangleComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandlerImpl;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.KeyInputEventHandler;
import dtb.terminalIO.events.KeyInputEventsProducer;
import dtb.terminalIO.events.Keys;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Stty;
import dtb.terminalIO.util.TerminalDimensions;

public class WindowResizeTest {
    
    public static void main(String[] args) throws IOException {
        // TODO Auto-generated method stub

        //Stty.setRaw();
        
        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
        
        master_canvas.setEmptyChar('.');

        FGBGColors drawingcolor = new FGBGColors(Colors.IYel, null);

        
        Component componentBox1 = new Box(drawingcolor, 10);

        Component componentBox2 = new RectangleComponent(new FGBGColors(Colors.IRed, null), 10, 20);
        
        TextComponent stats = new TextComponent("Size = : " + new Dimension.Simple(master_canvas).toString()+
                                                "\n                              "+
                                                "\n                              ");
        
        int[]  counter = new int[] {0};
        
        Consumer<Dimension> resizer = (size) -> {
            master_canvas.clearCanvas();
          //  master_canvas.refresh();
            
            master_canvas.setSize(Math.min(size.getHeight(),30) , size.getWidth() ); // todo: detect term size
            master_canvas.addComponentSegments(master_canvas.getHeight() / 3, master_canvas.getWidth() / 3,  componentBox1);
            master_canvas.addComponentSegments(master_canvas.getHeight() / 2, master_canvas.getWidth() / 2, componentBox2);
            
            master_canvas.refresh();

            stats.setText("Size = : " + new Dimension.Simple(master_canvas).toString()+ "\ncount:" + counter[0] + 
                           "\nlinecounter =    " + screen.getEmitter().DEBUGCTR());
            master_canvas.addComponentSegments(0,0,stats);
            counter[0]++;

           master_canvas.refresh();
        };
        
        TerminalDimensions size = TerminalDimensions.create();
        
        resizer.accept(size);
        
//        master_canvas.addComponentSegments(componentBox1);
//        master_canvas.addComponentSegments(5,20,componentBox2);
//        master_canvas.addComponentSegments(0,0,stats);
//        
//        master_canvas.refresh();
        
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
        
        TextComponent idleCounter = new TextComponent("IDLE COUNT : XXXX    ");
        
        
        eventBus.addEventHandler( new OnIdleEventEventHandler.RepeatingInterval(100, new EventHandler() {
            
            int idleCount = 0;
            
            @Override
            public void handle(TerminalEvent<?> te) {
                
                idleCounter.setText("IDLE COUNT : "+idleCount+"    ");
                
                master_canvas.addComponentSegments(5, 0, idleCounter);
                
                master_canvas.refresh();
                
                idleCount++;
                
            }}) );
        

        eventBus.addEventHandler( new OnIdleEventEventHandler.RepeatingInterval(1000, new EventHandler() {

            Dimension lastSize = Dimension.ZERO;

            long lastChange = 0;
            
            @Override
            public void handle(TerminalEvent<?> te) {
                TerminalDimensions size = TerminalDimensions.create();
                
                if (lastSize.isSameDimension(size)) {
                    return ;
                }
                long now = System.currentTimeMillis();
                long elapsed = now - lastChange;

//                if (elapsed < 2000) {
//                    return;
//                }
                
                // screen.clear();

                eventBus.addEvent( new TerminalEvent<Dimension>(this, TerminalEvent.TYPE.RESIZE, size) );
                
                lastChange = now;
                
                lastSize = size;

            }}) );
        
        
        class ResizeEventEventHandler extends ConditionalEventHandlerImpl {

            public ResizeEventEventHandler(EventHandler handler) {
                super(new IS_Event_TYPE(TerminalEvent.TYPE.RESIZE), handler);
            }
            
        }
        
        
        eventBus.addEventHandler( new ResizeEventEventHandler(new EventHandler() {

            int counter = 0;

            
            @Override
            public void handle(TerminalEvent<?> te) {
                
                Dimension size = (Dimension) te.getEventData();

                resizer.accept(size);
                
            }
            
        }));
        
        
        
        
        
    }
        
    

}
