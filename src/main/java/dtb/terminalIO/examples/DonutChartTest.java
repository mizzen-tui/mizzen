package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.chart.DonutChart;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.OnIdleEventEventHandler;

/**
 * Demo's a donut chart
 * 
 * @author dbalme
 *
 */
public class DonutChartTest {
    
    public static void main(String args[]) {
        
        new TUIApp(args)
        .setEmptyChar(' ')
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)->{
            
            int[] dataPoint = new int[] {0};
            DonutChart donutChart = new DonutChart(Colors.BIGre, tApp.getCanvas() );
            donutChart.setData(dataPoint[0],100,String.format("%3d",dataPoint[0])+"%");
            
            tApp.getMainApplicationPanel().add(donutChart);
            
            tApp.setOnResizeAction((tSameApp, size)->{
                tApp.clearCanvas();
                tApp.setCanvasSize(size);
                
                ((Composite)tApp.getMainApplicationPanel()).resize(size);
                
                donutChart.resize(size);
            });
            
            
            tApp.getEventBus().appendEventHandler( new OnIdleEventEventHandler.RepeatingInterval(100, (te)-> {
                
                int maxData = 360;
                
                dataPoint[0] = ( dataPoint[0] + 1 ) % maxData;
                
                donutChart.setData(dataPoint[0],maxData,String.format("%3d",dataPoint[0]));
                
                tApp.getCanvas().clearCanvas();
                
                tApp.doRefreshCanvasIfModified();
                
            }));

            
        })
        .start();
        
    }

}
