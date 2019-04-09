package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.ScrollableComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.input.TextInput;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.FocusManager;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.OnResizeEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyInputEventsProducer;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.resize.TerminalResizeDetection;
import dtb.terminalIO.examples.BarChartTest.BarChartDemo;
import dtb.terminalIO.examples.GridBrailleTest.BrailleDemo;
import dtb.terminalIO.examples.LineChartTest.LineChartDemo;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.ExecUtil;
import dtb.terminalIO.util.StringUtils;
import dtb.terminalIO.util.Stty;

public class ScrollableComponentTest {
    
    
    static String execTop()   {
        try {
            return ExecUtil.executeWithShell("top -n 1 -b");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            return "EXCEPTION:  " + e;
        }
    }
    
    public static ScrollableComponent testWindowViewer1(Dimension size) throws IOException { 
        
        String text2a = execTop();  
        
        String text2 = text2a; // StringUtils.wrap(text2a, size.getWidth()*2);
        
        TextComponent srcOfText = new TextComponent("");
        srcOfText.setFillOnClear(false);

        Dimension sizeOfWindow = size;
        Position viewOffset = new Position.Simple(0, 0);

        ScrollableComponent viewComp1 = new ScrollableComponent(Colors.Gre, sizeOfWindow, srcOfText);
        
        
        Supplier<String> text3 = new Supplier<String>() {
            
            String bigFile = "LoremIpsumLarge.txt";
            Thread readFileThread = null;
            String largeText = null;
            
            @Override
            public String get() {
                synchronized (this) {
                    if (largeText != null) {
                        return largeText;
                    }
                    
                    if (readFileThread == null) {
                        readFileThread = new Thread(()->{
                            try {
                                largeText =  ReadSampleFiles.readSampleFile(bigFile);  
                                largeText =  StringUtils.wrap(largeText, size.getWidth()*2);
                                srcOfText.setTextAndResize(largeText);
                                viewComp1.updateCanvas();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                        readFileThread.start();
                    }
                }
                return "Reading large text file - one moment ..";
            }
        };
        
        
        boolean[] toggle = new boolean[] {false};
        
        Runnable setTextToView = new Runnable() {
            @Override
            public void run() {
                toggle[0] = !toggle[0];
                srcOfText.setTextAndResize((toggle[0])?text3.get():text2);
            }
        };
        
        setTextToView.run();
 
        viewComp1.setViewOffset(viewOffset.getRow(), viewOffset.getCol());
        
        
        viewComp1.setViewOffset(viewOffset.getRow(), viewOffset.getCol());
        viewComp1.setFooterText("Help comes in many forms!");
        
        viewComp1.setUpdateCanvas(()-> {});
         
        viewComp1.addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(Keys.CTRL_H_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                viewComp1.setShowBorder( ! viewComp1.visibleBorder() );
            }
        }));

        viewComp1.addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(Keys.CTRL_V_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                setTextToView.run();
                viewComp1.markAsDirty();
            }
        }));
        
        
        class Finder extends HorizontalPanel {
            TextComponent find;
            TextInput  findTextInput;
	    private ConditionalEventHandler findTextInputEventHandler; 
	    private String lastFindText = null;
            
            Finder(Color color) {
                super(color);
                this.find = new TextComponent(Finder.this.getColor(), " " + Finder.this.getColor().bold().asString()+"^F"+Finder.this.getColor().asString()+"ind");
                this.add(find);
            }
            
            public void hilite() {
        	super.hilite();
                if (findTextInput != null) {
                    this.findTextInput.setColor(Finder.this.getColor().invert());
                    this.findTextInput.setEmptyColor(Finder.this.getColor().invert());
                }
            }

            public void unHilite() {
        	super.unHilite();
                if (findTextInput != null) {
                    this.findTextInput.setColor(Finder.this.getColor());
                    this.findTextInput.setEmptyColor(Finder.this.getColor());
                }
            }
            
            void setInFindMode(TerminalEvent<?> te) {
                if (findTextInput == null) {
                    findTextInput = new TextInput(Finder.this.getColor().invert(), 10);
                    this.findTextInput.setEmptyColor(Finder.this.getColor().invert());
                    if (lastFindText != null) {
                       findTextInput.setText("test");
                    } else {
                	findTextInput.setText("");
                    }
                    this.add(findTextInput);
                    // findTextInput.requestFocus(te);
                    // FIXME:  1) doing a prepend causes hotkeys on viewComp1 to be blocked including CTRL-F
                    //            so cannot get out of find mode..  TextInput accepting too many keys
                    //            need to fix
                    //          2) Cursor never hides
                    //          3) Cursor mis-positioned on terminal resize
                    findTextInputEventHandler = viewComp1.prependEventHandlerWithCanvasUpdate(findTextInput);
                } else { // temp
                    lastFindText = findTextInput.getText();
                    viewComp1.removeEventHandler(findTextInputEventHandler);
                    findTextInputEventHandler = null;
                    this.remove(findTextInput);
                    this.findTextInput.setFocus(false, te.getEventBus());
                    this.findTextInput = null;
                    //findTextInput.requestReleaseFocus();
                }
            }
        };
        
        Finder finder = new Finder(viewComp1.getColor());
        
        
        viewComp1.addFooterButton(finder, Keys.CTRL_F_KEYSTROKE, (te)->{
            
            finder.setInFindMode(te);
            viewComp1.refreshFooter();
            
            });
        
        
//        viewComp1.addEventHandlerWithCanvasUpdate(new KeyInputEventHandler(Keys.CTRL_F_KEYSTROKE, new EventHandler() {
//            @Override
//            public void handle(TerminalEvent<?> te) {
//                setTextToView.run();
//                viewComp1.markAsDirty();
//            }
//        }));
        
        return viewComp1;
    }
    

    public static ScrollableComponent testScrollingGridBrailleWindowViewer(Dimension size, Dimension sizeOfGrid, EventHandlerContainer eventBus) throws IOException { 

 
        
        Composite sourceComponentContainingBrailleDemo = new Composite();
        sourceComponentContainingBrailleDemo.setBounds(sizeOfGrid);
        
        ScrollableComponent viewComp1 = new ScrollableComponent(Colors.Gre, size, sourceComponentContainingBrailleDemo);
        
        

        Position viewOffset = new Position.Simple(0, 0);

        viewComp1.setViewOffset(viewOffset.getRow(), viewOffset.getCol());
        
        
        viewComp1.setViewOffset(viewOffset.getRow(), viewOffset.getCol());
        viewComp1.setFooterText("Help comes in many forms!");
        
         viewComp1.setUpdateCanvas(()-> {});
         
         
         BrailleDemo brailleDemo = new BrailleDemo() {
             public void flushOutput() {
                 sourceComponentContainingBrailleDemo.markAsDirty();
                 viewComp1.markAsDirty();
                 viewComp1.updateCanvas();
             }
         };
         
         // this call must happen before brailleDemo.setUpDemo(..)
         brailleDemo.setupMpvementKeys(Keys.CTRL_T_KEYSTROKE,    // up
                                       Keys.CTRL_V_KEYSTROKE,    // down
                                       Keys.CTRL_F_KEYSTROKE,    // left
                                       Keys.CTRL_G_KEYSTROKE);   // right
         
         // sets up a braille demo using the source component both for size as well as 
         // the place where grid segments will be copied on a render iteration
         brailleDemo.setUpDemo(eventBus, sourceComponentContainingBrailleDemo, null);
         
         Component<?> brailleComponent = brailleDemo.getBrailleComponent();
         
         sourceComponentContainingBrailleDemo.add(brailleComponent);
         
        return viewComp1;
        
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

        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));
        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));
        eventBus.appendEventHandler(new ConditionalEventHandler.Impl(new IS_Event_TYPE(TYPE.EXCEPTION), terminate));
    }
    
    
    public static void testTwoScrollableWindows(String[] args) throws IOException {
        

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
        master_canvas.setEmptyChar('.');
        
        Dimension sizeOfWindow = new Dimension() {

            @Override
            public int getWidth() {
                return master_canvas.getWidth()/2-2;
            }

            @Override
            public int getHeight() {
                return master_canvas.getHeight()-17;
            }
            
        };

        ScrollableComponent viewComp1 = testWindowViewer1(sizeOfWindow);
        
        EventBus eventBus = new EventBus();
        try {
            new KeyInputEventsProducer(eventBus);
        } catch (Exception e) {
            eventBus.stopConsuming();
            return ;
        }
        
        eventBus.appendEventHandler(master_canvas.asShowCursorEventHandler());
        eventBus.appendEventHandler(master_canvas.asHideCursorEventHandler());
        
        
        setUpTerminateHandler(screen, master_canvas, eventBus);
        
        List<ConditionalEventHandler> gridEventHandlers = new ArrayList<ConditionalEventHandler>();

        EventHandlerContainer gridBrailleEventHandlers = new EventHandlerContainer() {
            
            ChainedEventHandler  idleEventHandlers = ChainedEventHandler.useHandleAllStrategy();

            @Override
            public EventHandlerContainer appendEventHandler(ConditionalEventHandler eventHandler) {
                
                if (eventHandler instanceof OnIdleEventEventHandler) {
                    idleEventHandlers.appendEventHandler(eventHandler);
                    eventBus.appendEventHandler(eventHandler);
                    return this;
                }
                gridEventHandlers.add(eventHandler);
                
                return this;
            }

            @Override
            public EventHandlerContainer removeEventHandler(ConditionalEventHandler eventHandler) {
                if (eventHandler instanceof OnIdleEventEventHandler) {
                    idleEventHandlers.removeEventHandler(eventHandler);
                    eventBus.removeEventHandler(eventHandler);
                    return this;
                }
                gridEventHandlers.remove(eventHandler);
                return this;
            }

	    @Override
	    public EventHandlerContainer prependEventHandler(ConditionalEventHandler eventHandler) {
                
                if (eventHandler instanceof OnIdleEventEventHandler) {
                    idleEventHandlers.prependEventHandler(eventHandler);
                    eventBus.prependEventHandler(eventHandler);
                    return this;
                }
                gridEventHandlers.add(0, eventHandler);
                
                return this;
	    }
        };
        
        ScrollableComponent brailleViewComp2 = testScrollingGridBrailleWindowViewer(sizeOfWindow, master_canvas, gridBrailleEventHandlers);
        for (ConditionalEventHandler eventHandler : gridEventHandlers) {
            brailleViewComp2.addEventHandlerWithCanvasUpdate(eventHandler);
        }
        
        
        Position viewComp1OffsetOnCanvas = new Position.Simple(1, 1);
        
        screen.add(ControlCode.HIDE_CURSOR);
        screen.refresh();

        Position brailleViewComp2OffsetOnCanvas = new Position() {
            @Override
            public int getCol() {
                return viewComp1.getWidth()+3;
            }
            @Override
            public int getRow() {
                return viewComp1OffsetOnCanvas.getRow();
            }
        };
        
        Position lineChartPosition = new Position() {
            @Override
            public int getCol() {
                return viewComp1OffsetOnCanvas.getCol();
            }
            @Override
            public int getRow() {
                return viewComp1OffsetOnCanvas.getRow()+viewComp1.getHeight()+1;
            }
        }; 
        
        Dimension sizeOfLineChartWindow = new Dimension() {

            @Override
            public int getWidth() {
                return viewComp1.getWidth() ;
            }

            @Override
            public int getHeight() {
                return 14 ;
            }
            
        };

        Position barChartPosition = new Position() {
            @Override
            public int getCol() {
                return brailleViewComp2OffsetOnCanvas.getCol();
            }
            @Override
            public int getRow() {
                return lineChartPosition.getRow();
            }
        }; 

        Dimension sizeOfBarChartWindow = new Dimension() {

            @Override
            public int getWidth() {
                return brailleViewComp2.getWidth();
            }

            @Override
            public int getHeight() {
                return sizeOfLineChartWindow.getHeight() ;
            }
            
        };
        
        class ContainerForDemo extends Composite {
        };
        
        
        Composite lineChartDemoContainer = new ContainerForDemo();
        lineChartDemoContainer.setBounds(sizeOfLineChartWindow);
        lineChartDemoContainer.setBorder(true);
        LineChartDemo lineChartDemo = new LineChartDemo();
        
        Composite barChartDemoContainer = new ContainerForDemo();
        barChartDemoContainer.setBounds(sizeOfBarChartWindow);
        barChartDemoContainer.setBorder(true);
        BarChartDemo barChartDemo = new BarChartDemo();

        Runnable updateCanvas = new Runnable() {
            
            long lastRun = 0;

            long updateTaskTime=0;
            long updateTaskDuration = 200;
            
            @Override
            public synchronized void run() {
                long now = System.currentTimeMillis();
                if (now - lastRun < updateTaskDuration) {
                    return;
                }
                lastRun = now;
                master_canvas.addComponentSegments(viewComp1OffsetOnCanvas, viewComp1);
                master_canvas.addComponentSegments(brailleViewComp2OffsetOnCanvas, brailleViewComp2);
                master_canvas.addComponentSegments(lineChartPosition, lineChartDemoContainer);
                master_canvas.addComponentSegments(barChartPosition, barChartDemoContainer);
                master_canvas.t(master_canvas.getHeight()-1,1,"[Rendering stats | Rendering time :" + updateTaskTime + " | Wait time between updates: " + updateTaskDuration + "]     " );
                master_canvas.refresh();
                updateTaskTime = System.currentTimeMillis() - now;
                updateTaskDuration = updateTaskTime * 3;
            }
        };

        lineChartDemo.setUpDemo(eventBus, new Rectangle.Simple(lineChartDemoContainer), lineChartDemoContainer);
        lineChartDemo.setFlushDemoOutput(updateCanvas);
        
        barChartDemo.setUpDemo(eventBus, new Rectangle.Simple(barChartDemoContainer), barChartDemoContainer);
        barChartDemo.setFlushDemoOutput(updateCanvas);

        updateCanvas.run();  //  can cause issues -- do an invokelater .. 
        
        viewComp1.setUpdateCanvas(updateCanvas);
        
        brailleViewComp2.setUpdateCanvas(updateCanvas);
        
        FocusManager focusManager = new FocusManager(Keys.TAB_KEYSTROKE);
        
        focusManager.addFocusable(viewComp1, eventBus );

        focusManager.addFocusable(brailleViewComp2, eventBus );
        
        TestsCommon.addNoGraphicsToggleEvent(eventBus, screen);

        eventBus.appendEventHandler(focusManager);

        TerminalResizeDetection.initiate(screen, eventBus);
        
        eventBus.appendEventHandler(new OnResizeEventHandler() {
            @Override
            public void handleResize(Dimension size) {
                master_canvas.setSize(size.getHeight() , size.getWidth() ); 
                master_canvas.clearCanvas();
                viewComp1.resize(sizeOfWindow);
                brailleViewComp2.resize(sizeOfWindow);

                lineChartDemoContainer.clear();
                lineChartDemoContainer.resize(sizeOfLineChartWindow);
                lineChartDemo.resize(sizeOfLineChartWindow);
                
                barChartDemoContainer.clear();
                barChartDemoContainer.resize(sizeOfBarChartWindow);
                barChartDemo.resize(sizeOfBarChartWindow);
                
                updateCanvas.run();
            }
        });

        
        return ;
    }
    

    public static void main(String[] args) throws IOException {

        // testWindowViewer(args);
        testTwoScrollableWindows(args);
        //testScrollingGridBrailleTest(args);

    }
    
    
}
