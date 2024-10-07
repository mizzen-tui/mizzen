package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.frameset.HorizontalSplitter;
import dtb.terminalIO.components.frameset.VerticalSplitter;
import dtb.terminalIO.components.scroll.ScrollableComponent2;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TimedRequestedController;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.examples.BarChartTest.BarChartRandomDataDemo;
import dtb.terminalIO.examples.GridBrailleTest.BrailleDemo;
import dtb.terminalIO.examples.LineChartTest.LineChartDemo;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.StringUtils;
import dtb.terminalIO.util.linuxproc.ExecUtil;

// THIS DEMO NO LONGER WORKS .. I need to diagnose and fix


public class ScrollableComponentTest {

    public static void main(String[] args) {

        //Logging.setEnabled(true);

        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)->{ 

            Supplier<Dimension> calcScrollableDimension = ()-> {
                return new Dimension.Simple( ((int)(tApp.getCanvas().getHeight()*.7)), tApp.getCanvas().getWidth()/2 -3);
            };
            
            TextComponent largeTextComponent = new TextViewer().getComponent(Colors.Gre, tApp.getCanvas().getWidth()-2);
            String originalLargeText = largeTextComponent.getText();
   
            ScrollableComponent2 sc1 = new ScrollableComponent2(Colors.Gre, 
                                                                calcScrollableDimension.get(), 
                                                                largeTextComponent);

            TimedRequestedController resizeWordWrapThrottler = new TimedRequestedController((te)->{
                String largeText = originalLargeText;
                largeText = StringUtils.wrap(largeText, sc1.getWidth()-7);
                largeText = StringUtils.addLineNumbers(largeText);
                largeTextComponent.setTextAndResize(largeText);
                te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(largeTextComponent) );
            });
            resizeWordWrapThrottler.setTimerDelayMillis(500); // 1/2 sec
            
            sc1.appendOnResizeRule((newSize)->{
                resizeWordWrapThrottler.onRequest(tApp.getEventBus());
            });

            sc1.setName("ScrollableComponent-sc1");
            SearchFunctionForScrollableComponent.addFinderToScrollable(sc1.asSearchableView());  

            Composite brailleContainer = new Composite(Colors.Blu).setBounds(tApp.getCanvas().getHeight()-7, tApp.getCanvas().getWidth()/2 -2);
            brailleDemoSetup(brailleContainer, tApp.getEventBus());
            ScrollableComponent2 sc2 = new ScrollableComponent2(Colors.Blu, calcScrollableDimension.get(), brailleContainer);
            sc2.setName("ScrollableComponent-sc2");
            sc2.appendOnResizeRule((newSize)->{
                brailleContainer.resize(newSize);
            });

            Composite  lineChart = new Composite(Colors.Cya);
            lineChart.setBorder(false);
            lineChartDemoSetup(lineChart, tApp.getEventBus());

            Supplier<Dimension> lineChartDim = ()-> {
                return new Dimension.Simple( ((int)(tApp.getCanvas().getHeight() - calcScrollableDimension.get().getHeight())), sc1.getWidth());
            };

            Supplier<Dimension> barChartDim = ()-> {
                return new Dimension.Simple( ((int)(tApp.getCanvas().getHeight() - calcScrollableDimension.get().getHeight()  )), sc2.getWidth());
            };

            BarChartRandomDataDemo  barChart = new BarChartRandomDataDemo(Colors.Cya, barChartDim.get()).setupTimer(tApp.getEventBus());

            lineChart.resize(lineChartDim.get());
            barChart.resize(barChartDim.get());

            Dimension frameSetSizeCalculator = new Dimension.SupplierAsDimension(()-> new Dimension.Simple( tApp.getDimensions()));
            HorizontalSplitter horizontalSplitterTop = new HorizontalSplitter(Colors.Blu, new Dimension.Simple(frameSetSizeCalculator.getHeight()/2, frameSetSizeCalculator.getWidth() )).setBorder(false);

            horizontalSplitterTop.addComponent(sc1);
            horizontalSplitterTop.addComponent(sc2);

            HorizontalSplitter horizontalSplitterBottom = new HorizontalSplitter(Colors.Blu, new Dimension.Simple(frameSetSizeCalculator.getHeight()/2, frameSetSizeCalculator.getWidth() )).setBorder(false);

            horizontalSplitterBottom.addComponent(lineChart);
            horizontalSplitterBottom.addComponent(barChart);

            VerticalSplitter vSplitter = new VerticalSplitter(Colors.Blu, frameSetSizeCalculator);
            vSplitter.addComponent(horizontalSplitterTop);
            vSplitter.addComponent(horizontalSplitterBottom);

            tApp.getMainApplicationPanel().add(vSplitter);

            BiConsumer<TUIApp, Dimension> baseResizeAction = tApp.getResizeAction();

            tApp.setOnResizeAction( (ptApp, newSize) ->{
                baseResizeAction.accept(ptApp, newSize);

                sc1.resize(calcScrollableDimension.get());
                sc2.resize(calcScrollableDimension.get());
                lineChart.resize(lineChartDim.get());
                barChart.resize(barChartDim.get());
            } );

            //FocusManager.UTIL.verifyPathsToFocusables(tApp.getApplicationFocusManager(), vp);

        }).start();

    }

    private static void brailleDemoSetup(Composite container, EventBus eventBus) {
        BrailleDemo brailleDemo = new BrailleDemo() {
            
        };

       // this call must happen before brailleDemo.setUpDemo(..)
       brailleDemo.setupMovementKeys(Keys.CTRL_T_KEYSTROKE,    // up
                                     Keys.CTRL_V_KEYSTROKE,    // down
                                     Keys.CTRL_F_KEYSTROKE,    // left
                                     Keys.CTRL_G_KEYSTROKE);   // right

       // sets up a braille demo using the source component both for size as well as 
       // the place where grid segments will be copied on a render iteration
       brailleDemo.setUpDemo(eventBus, container);
       brailleDemo.setFlushDemoOutput(()->{
           eventBus.raiseEvent(new RequestUpdateCanvasEvent(brailleDemo));
       });


       Component<?> brailleComponent = brailleDemo.getBrailleComponent();

       container.add(brailleComponent);

       container.setOnResize((newSize)-> brailleDemo.resize(newSize) );
    }

    private static void lineChartDemoSetup(Composite container, EventBus eventBus) {

        LineChartDemo lineChartDemo = new LineChartDemo();
        Supplier<Rectangle> clippedAreaForLinechart = ()-> new Rectangle.Simple(Position.ZERO, new Dimension.Simple(container.getHeight()-2, container.getWidth()-2));
        lineChartDemo.setUpDemo(eventBus, clippedAreaForLinechart.get());
        container.add(lineChartDemo.getLineChartPanelComponent());

        lineChartDemo.setFlushDemoOutput(()->{
            eventBus.raiseEvent(new RequestUpdateCanvasEvent(lineChartDemo));
        });

        container.setOnResize((newSize)-> lineChartDemo.resize(clippedAreaForLinechart.get()) );

    }
    
    
    static class TextViewer {

        public TextComponent getComponent(Color color, int width) {
            TextComponent viewable = new TextComponent(color, readLargeSampleFile(width));
            return viewable;
        }
//
//        public EventHandler activity() {
//            return null;
//        }

    }

    static String execTop()   {
        try {
            return ExecUtil.executeWithShell("top -n 1 -b");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return "EXCEPTION:  " + e;
        }
    }

    static String readLargeSampleFile(int wrapLength) {
        String bigFile = "LoremIpsumLarge.txt";
        String largeText = null;
        try {
            largeText = ReadSampleFiles.readSampleFile(bigFile);  
           // largeText = StringUtils.wrap(largeText, wrapLength);

            //largeText = StringUtils.addLineNumbers(largeText);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return largeText;
    }

}
