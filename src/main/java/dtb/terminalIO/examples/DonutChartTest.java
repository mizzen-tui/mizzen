package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.chart.DonutChart;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.TextInputInteger;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;

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
            DonutChart donutChart = new DonutChart(Colors.BRed ,  tApp.getCanvas() );
            donutChart.setData(dataPoint[0],100,String.format("%3d",dataPoint[0])+"%");
            
            tApp.getMainApplicationPanel().add(donutChart);
            
            HorizontalPanel userControls = new HorizontalPanel().setHorizontalSpacer(3);
            userControls.setEmptyChar('░');
            
            Button stopButton = new Button(Colors.BGre,"Stop");
            stopButton.setHotKey(Keys.CTRL_C_KEYSTROKE).setDisplayHotKey(true);
            stopButton.onChange( tApp.getTerminateAppHotKeyEventHandler()  );
            
            TextInputInteger dataPointTextBox = new TextInputInteger(3);
            dataPointTextBox.onChange((te)->{
                Integer newValue = dataPointTextBox.getValue();
                if (newValue >=0 && newValue <=360) {
                    dataPoint[0] = newValue;
                }
            });
            
            userControls.add(stopButton);
            userControls.add(dataPointTextBox);
            
            tApp.getMainApplicationPanel().add(0, 1,userControls);
            
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
