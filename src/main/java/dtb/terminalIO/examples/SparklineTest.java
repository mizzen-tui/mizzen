package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.chart.SparkLineComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.geom.Dimension;

public class SparklineTest {
    
    public static void main(String[] args) throws IOException {

        new TUIApp(args)
                .setCanvasSize(24, 80)
                .setEmptyChar(' ')
                .setMainApplicationPanel(new Composite().setBounds(20, 80).setEmptyChar(' ').setBorder(true))
                .confirmOnExit()
                .setOnReadyStart((tApp)-> {
                    
                    tApp.getMainApplicationPanel().add(1, 1, new TextComponent("Demonstrate Sparkline Component:          CTRL-C to quit"));
                    
                    SparkLineComponent sparkLine = new SparkLineComponent(Colors.BCya);
                    
                    sparkLine.resize(new Dimension.Simple(1, 30));
                    
                    VerticalPanel vp = new VerticalPanel();
                    TextComponent dataPoints = new TextComponent(" .. ");
                    
                    vp.add(new HorizontalPanel().add(new TextComponent("Data      : ")).add(dataPoints));
                    vp.add(new HorizontalPanel().add(new TextComponent("Sparkline : ")).add(sparkLine));
                    vp.setVerticalSpacer(1);
                    
                    
                    tApp.getMainApplicationPanel().add(3, 1, vp);
                    
                    Random rint = new Random();
                    
                    tApp.getEventBus().appendEventHandler( new OnIdleEventEventHandler.RepeatingInterval(100, (te)-> {
                        
                        Integer[] data = new Integer[30];
                        
                        for (int i = 0; i < data.length; i++) {
                            data[i] = rint.nextInt(100);
                        }
                        
                        sparkLine.renderPoints(data);
                        
                        dataPoints.setTextAndResize( Arrays.deepToString(data) );
                        
                        tApp.doRefreshCanvasIfModified();
                        
                    } ));
                    

                }).start();
    }
    
    

}
