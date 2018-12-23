package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode;
import dtb.terminalIO.ScrollableComponent;
import dtb.terminalIO.ScrollableComponent.WindowViewComponent;
import dtb.terminalIO.TerminalScreen;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandlerImpl;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.KeyInputEventHandler;
import dtb.terminalIO.events.KeyInputEventsProducer;
import dtb.terminalIO.events.Keys;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.examples.GridBrailleTest.BrailleDemo;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.StringUtils;
import dtb.terminalIO.util.Stty;

public class ScrollableComponentTest {

    
    public static String DEBUG = "TEST";
    
    public static boolean testWindowViewer(String[] args) throws IOException { 

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));
        master_canvas.setEmptyChar(' ');

        TextComponent srcOfText = new TextComponent(
                "T'was Brillig and the slithy toth\n" + 
                "Dide gere and gimble in the wabe\n" + 
                "And hast thou slain the JaberWock\n" + 
                "Oh Frabjious day \n" + 
                "Oh Frabjious joy!\n" + 
                "Calloo callay\n" + 
                "Basterdized Lewis Carroll");
        
        
        String text = ReadSampleFiles.readSampleFile("LoremIpsumLarge.txt");
        
        text = StringUtils.wrap(text, master_canvas.getWidth()*2);
        
        srcOfText = new TextComponent(text);

        Dimension sizeOfWindow = new Dimension.Simple(master_canvas.getHeight()-10, master_canvas.getWidth()-10);
        Position offsetOnCanvas = new Position.Simple(1, 1);
        Position viewOffset = new Position.Simple(0, 0);

        ScrollableComponent viewComp1 = new ScrollableComponent(Colors.Gre, sizeOfWindow, srcOfText);

        WindowViewComponent viewComp2 = new WindowViewComponent(Colors.Gre, sizeOfWindow, srcOfText);

        viewComp1.setViewOffset(viewOffset.getRow(), viewOffset.getCol());

        screen.add(ControlCode.HIDE_CURSOR);
        screen.refresh();


        
        //viewComp2.setViewOffset(viewOffset.getRow(), viewOffset.getCol());

        Runnable updateCanvas = new Runnable() {

            @Override
            public void run() {
                master_canvas.clearCanvas();
                master_canvas.addComponentSegments(offsetOnCanvas, viewComp1);
                // master_canvas.addComponentSegments(offsetOnCanvas.offsetAdd(0, sizeOfWindow.getWidth() + 5), viewComp2);
                
                
                master_canvas.t(master_canvas.getHeight()-7,1, String.valueOf(viewComp1.getOffsetIntoSource()) + "                    ");
                master_canvas.t(master_canvas.getHeight()-6,1, String.valueOf(DEBUG) + "                         ");
                
                master_canvas.refresh();
            }
            
        };

        updateCanvas.run();
        
        
        EventBus eventBus = new EventBus();
        try {
            new KeyInputEventsProducer(eventBus);
        } catch (Exception e) {
            eventBus.stopConsuming();
            return false;
        }
        
        setUpTerminateHandler(screen, master_canvas, eventBus);
        
        viewComp1.addStandardNavigationKeyInputEventHandlers(eventBus, updateCanvas);
        
        return true;

    }

    public static boolean testScrollingGridBrailleTest(String[] args) throws IOException { 

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));
        master_canvas.setEmptyChar(' ');

        

        Dimension sizeOfWindow = new Dimension.Simple(master_canvas.getHeight()-20, master_canvas.getWidth()-15);
        Position offsetOnCanvas = new Position.Simple(1, 1);
        Position viewOffset = new Position.Simple(0, 0);

        Composite sourceComponent = new Composite();
        sourceComponent.setBounds(new Dimension.Simple(master_canvas));
        
        ScrollableComponent viewComp1 = new ScrollableComponent(Colors.Gre, sizeOfWindow, sourceComponent);

        viewComp1.setViewOffset(viewOffset.getRow(), viewOffset.getCol());

        screen.add(ControlCode.HIDE_CURSOR);
        screen.refresh();


        
        //viewComp2.setViewOffset(viewOffset.getRow(), viewOffset.getCol());

        int counter[] = new int[] {0};
        
        Runnable updateCanvas = new Runnable() {

            @Override
            public void run() {
                master_canvas.clearCanvas();
                master_canvas.addComponentSegments(offsetOnCanvas, viewComp1);
                // master_canvas.addComponentSegments(offsetOnCanvas.offsetAdd(0, sizeOfWindow.getWidth() + 5), viewComp2);
                
                
                counter[0]++;
                
                master_canvas.t(master_canvas.getHeight()-7,1, String.valueOf(viewComp1.getOffsetIntoSource()) + "                    ");
                master_canvas.t(master_canvas.getHeight()-6,1, "Counter: "+counter[0]+"   ");
                
                master_canvas.refresh();
            }
            
        };

        updateCanvas.run();
        
        
        EventBus eventBus = new EventBus();
        try {
            new KeyInputEventsProducer(eventBus);
        } catch (Exception e) {
            eventBus.stopConsuming();
            return false;
        }
        
        setUpTerminateHandler(screen, master_canvas, eventBus);
        
        viewComp1.addStandardNavigationKeyInputEventHandlers(eventBus, updateCanvas);
        
        BrailleDemo brailleDemo = new BrailleDemo() {
            public void flushOutput() {
                viewComp1.markAsDirty();
                updateCanvas.run();
                //master_canvas.refresh();
            }
        };
        
        brailleDemo.setupMpvementKeys(Keys.CTRL_T_KEYSTROKE, 
                                      Keys.CTRL_V_KEYSTROKE, 
                                      Keys.CTRL_F_KEYSTROKE, 
                                      Keys.CTRL_G_KEYSTROKE);
        
        brailleDemo.setUpDemo(eventBus, sourceComponent, sourceComponent);
        
        return true;

    }

    
    private static void setUpTerminateHandler(TerminalScreen screen, Canvas master_canvas, EventBus eventBus) {
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

        eventBus.addEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));
        eventBus.addEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));
        eventBus.addEventHandler(new ConditionalEventHandlerImpl(new IS_Event_TYPE(TYPE.EXCEPTION), terminate));
    }
    
    

    public static void main(String[] args) throws IOException {

        //testWindowViewer(args);
        testScrollingGridBrailleTest(args);
//            return;
//
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        TerminalScreen screen = new TerminalScreen();
//        screen.setControlCodesEnabled(false);
//
//        Canvas canvas = new Canvas(screen);
//        canvas.setSize(12, 30);
//
//        // canvas.t("TEST");
//
//        ScreenLineSegmentsBuffer srcSegments = new ScreenLineSegmentsBuffer();
//
//        int i = 0;
//
//        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 0, -1, "How now brown cow"));
//        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 1, -1, "Hello there"));
//        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 0, -1, "To error is devine providence"));
//        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 4, -1, "Waiter!"));
//        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 3, -1, "There is a fly in my soup"));
//        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 6, -1, "So? .. what is it doing?"));
//        srcSegments.add(new ScreenLineSegment.SegmentImpl(Colors.BBla, i++, 3, -1, "... The back stroke?"));
//
//        Rectangle viewBounds = new Rectangle.Simple(1, 2, 5, 10);
//
//        IScreenLineSegmentsBuffer theSegments = new ScreenLineSegmentsView(srcSegments, viewBounds);
//
//        // IScreenLineSegmentsBuffer theSegments = new
//        // IScreenLineSegmentsBuffer.Delegate(srcSegments);
//
//        Dimension compDIM = new Dimension.Simple(10, 25);
//        // Dimension compDIM = new Dimension.Simple(viewBounds);
//        // Dimension compDIM = new Dimension.Simple(srcSegments);
//
//        BoundedComponent comp = new BoundedComponent(null, compDIM) {
//
//            @Override
//            protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
//                segments.merge(theSegments);
//
//            }
//
//        };
//
//        canvas.addComponentSegments(1, 2, comp);
//
//        canvas.refresh();

    }
    
    
}
