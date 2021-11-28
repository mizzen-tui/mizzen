package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.examples.BarChartTest.BarChartDemo;
import dtb.terminalIO.examples.GridBrailleTest.BrailleDemo;
import dtb.terminalIO.examples.LineChartTest.LineChartDemo;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.StringUtils;
import dtb.terminalIO.util.linuxproc.ExecUtil;

public class ScrollableComponentTest {

    public static void main(String[] args) {

        //Logging.setEnabled(true);
        
        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)->{ 
            
            Supplier<Dimension> calcScrollableDimension = ()-> {
                return new Dimension.Simple( ((int)(tApp.getCanvas().getHeight()*.7)), tApp.getCanvas().getWidth()/2 -2);
            };
   
            ScrollableComponent sc1 = new ScrollableComponent(Colors.Gre, calcScrollableDimension.get(), new TextViewer().getComponent(Colors.Gre));
            
            sc1.setName("ScrollableComponent-sc1");            
            SearchFunctionForScrollableComponent.addFinderToScrollable(sc1);  

            Composite brailleContainer = new Composite(Colors.Blu).setBounds(tApp.getCanvas().getHeight()-7, tApp.getCanvas().getWidth()/2 -2);
            brailleDemoSetup(brailleContainer, tApp.getEventBus());
            ScrollableComponent sc2 = new ScrollableComponent(Colors.Blu, calcScrollableDimension.get(), brailleContainer);
            sc2.setName("ScrollableComponent-sc2");
 //           SearchFunctionForScrollableComponent.addFinderToScrollable(sc2);

            Composite  lineChart = new Composite(Colors.Cya);
            lineChart.setBorder(true);
            lineChartDemoSetup(lineChart, tApp.getEventBus());

            Composite  barChart = new Composite(Colors.Cya);
            barChart.setBorder(true);
            barChartDemoSetup(barChart, tApp.getEventBus());

            Supplier<Dimension> lineChartDim = ()-> {
                return new Dimension.Simple( ((int)(tApp.getCanvas().getHeight() - calcScrollableDimension.get().getHeight())), sc1.getWidth());
            };

            Supplier<Dimension> barChartDim = ()-> {
                return new Dimension.Simple( ((int)(tApp.getCanvas().getHeight() - calcScrollableDimension.get().getHeight()  )), sc2.getWidth());
            };

            lineChart.resize(lineChartDim.get());
            barChart.resize(barChartDim.get());

            VerticalPanel vp = new VerticalPanel();
            HorizontalPanel hpSCs = new HorizontalPanel();
            vp.add(hpSCs);

            hpSCs.add(sc1);
            hpSCs.add(sc2);

            HorizontalPanel hpCharts = new HorizontalPanel();

            hpCharts.add(lineChart);
            hpCharts.add(barChart);

            vp.add(hpCharts);

            tApp.getMainApplicationPanel().add(vp);

            FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),
                                                                                             tApp.getEventBus(),
                                                                                             ()-> new DefaultFocusManager(Keys.SHIFT_TAB_KEYSTROKE, Keys.TAB_KEYSTROKE)
                                                                                                       .setNavKeysFirst(false)
                                                                                                       .setCycling(true));

            tApp.getEventBus().appendEventHandler( mainFocusManager );

            BiConsumer<TUIApp, Dimension> baseResizeAction = tApp.getResizeAction();

            tApp.setOnResizeAction( (ptApp, newSize) ->{
                baseResizeAction.accept(ptApp, newSize);

                sc1.resize(calcScrollableDimension.get());
                sc2.resize(calcScrollableDimension.get());
                lineChart.resize(lineChartDim.get());
                barChart.resize(barChartDim.get());
                
            } );

            //ScreenCanvasBackend canvasBackend = ((ScreenCanvasBackend)tApp.getCanvas().getCanvasBackend());
            //canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_FULL);
            
            FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>) tApp.getMainApplicationPanel());
            
            
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

    private static void barChartDemoSetup(Composite container, EventBus eventBus) {
        BarChartDemo barChartDemo = new BarChartDemo();
        Supplier<Rectangle> clippedAreaForBarchart = ()-> new Rectangle.Simple(Position.ZERO, new Dimension.Simple(container.getHeight()-2, container.getWidth()-2));

        barChartDemo.setUpDemo(eventBus, clippedAreaForBarchart.get());
        container.add(barChartDemo.getBarChart(clippedAreaForBarchart.get()));
        
        barChartDemo.setFlushDemoOutput(()->{
            eventBus.raiseEvent(new RequestUpdateCanvasEvent(barChartDemo));
        });
        
        container.setOnResize((newSize)-> barChartDemo.resize(clippedAreaForBarchart.get()) );

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

        public Component<?> getComponent(Color color) {
            Component<?> viewable = new TextComponent(color, readLargeSampleFile(100));
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
            largeText = StringUtils.wrap(largeText, wrapLength);

            largeText = StringUtils.addLineNumbers(largeText);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return largeText;
    }

}
