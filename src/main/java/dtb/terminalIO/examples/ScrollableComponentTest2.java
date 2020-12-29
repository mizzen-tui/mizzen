package dtb.terminalIO.examples;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.ScreenCanvasBackend.UpdateStrategy;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.linuxproc.ExecUtil;

public class ScrollableComponentTest2 {
    
    public static void main(String[] args) {
        
        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)->{
            
            Supplier<Dimension> calcScrollableDimension = ()-> {
                return new Dimension.Simple( ((int)(tApp.getCanvas().getHeight()*.8)), tApp.getCanvas().getWidth()/2);
            };
            
            ScrollableComponent sc1 = new ScrollableComponent(Colors.BGre, calcScrollableDimension.get(), new TextViewer().getComponent(Colors.BGre));
            
            ScrollableComponent sc2 = new ScrollableComponent(Colors.BBlu, calcScrollableDimension.get(), new TextViewer().getComponent(Colors.BGre));
            
            BoundedComponent<?>  lineChart = new TextComponent("Here is where the linechart\nwill go!");
            BoundedComponent<?>  barChart = new TextComponent("Here is where the bar chart\nwill go!");
            
            Supplier<Dimension> lineChartDim = ()-> {
                return new Dimension.Simple( ((int)(tApp.getCanvas().getHeight()*.2)), sc1.getWidth());
            };
            
            Supplier<Dimension> barChartDim = ()-> {
                return new Dimension.Simple( ((int)(tApp.getCanvas().getHeight()*.2)), sc2.getWidth());
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

            ScreenCanvasBackend canvasBackend = ((ScreenCanvasBackend)tApp.getCanvas().getCanvasBackend());
            canvasBackend.setUpdateStrategy(UpdateStrategy.UPDATE_FULL);
            
            
        }).start();
        
    }
    
    static class TextViewer {
        
        public Component<?> getComponent(Color color) {
            return new TextComponent(color, "Hello world");
        }
        
        public EventHandler activity() {
            return null;
        }
        
    }
    
    
    static String execTop()   {
        try {
            return ExecUtil.executeWithShell("top -n 1 -b");
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return "EXCEPTION:  " + e;
        }
    }
    
    

}
