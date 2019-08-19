package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.chart.SparkLineComponent;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;

public class SparklineTest {
    
    public static void main(String[] args) throws IOException {

        Logging.setEnabled(true);
        
        new TUIApp(args)
                .setCanvasSize(24, 100)
                .setEmptyChar(' ')
                .setMainApplicationPanel(new Composite().setBounds(20, 100).setEmptyChar(' ').setBorder(true))
                .confirmOnExit()
                .setOnReadyStart((tApp)-> {
                    
                    Function<Integer,String> hiliteSparklineHeight = (height)-> ((height==1||height==20)? Colors.IRed.asString() : Colors.IGre.asString()) + height + ControlCode.RCol.asString();
                    
                    Function<String,String> hiliteHotKey = (s)-> Colors.IGre.asString() + s + ControlCode.RCol.asString();

                    tApp.getMainApplicationPanel().add(1, 1, new TextComponent("Demonstrate Sparkline Component:    "+ hiliteHotKey.apply("CTRL-C")+
                                                                               " to quit  "+ hiliteHotKey.apply("UP")+"/"+hiliteHotKey.apply("DOWN")+" increase/decrease sparkline height"));
                    
                    SparkLineComponent sparkLine1 = new SparkLineComponent(Colors.BCya);
                    SparkLineComponent sparkLine2 = new SparkLineComponent(Colors.BIGre);
                    
                    sparkLine1.resize(new Dimension.Simple(5, 40));
                    sparkLine2.resize(new Dimension.Simple(1, 40));
                    
                    VerticalPanel vp = new VerticalPanel();
                    TextComponent dataPoints = new TextComponent(" .. ");
                    
                    TextComponent sparkLine1Label = new TextComponent("Sparkline : \nheight="+hiliteSparklineHeight.apply(sparkLine1.getHeight()));
                    TextComponent sparkLine2Label = new TextComponent("Sparkline : \nheight="+sparkLine2.getHeight());
                    
                    vp.add(new HorizontalPanel().add(new TextComponent("Data      : ")).add(dataPoints));
                    vp.add(new HorizontalPanel().add(sparkLine1Label).add(sparkLine1));
                    vp.add(new HorizontalPanel().add(sparkLine2Label).add(sparkLine2));
                    vp.setVerticalSpacer(1);
                    
                    
                    tApp.getMainApplicationPanel().add(3, 1, vp);
                    
                    Random rint = new Random();
                    
                    tApp.getEventBus().appendEventHandler(new KeyInputEventHandler(Keys.DOWN_KEYSTROKE, (te)->{
                        if (sparkLine1.getHeight()>1) {
                            sparkLine1.resize( new Dimension.Simple(sparkLine1.getHeight()-1, sparkLine1.getWidth()) );
                            sparkLine1Label.setText("Sparkline : \nheight="+ hiliteSparklineHeight.apply(sparkLine1.getHeight()));
                        }
                    }) );
                    
                    tApp.getEventBus().appendEventHandler(new KeyInputEventHandler(Keys.UP_KEYSTROKE, (te)->{
                        if (sparkLine1.getHeight()<20) {
                            sparkLine1.resize( new Dimension.Simple(sparkLine1.getHeight()+1, sparkLine1.getWidth()) );
                            sparkLine1Label.setText("Sparkline : \nheight="+hiliteSparklineHeight.apply(sparkLine1.getHeight()));
                        }
                    }) );
                    
                    tApp.getEventBus().appendEventHandler( new OnIdleEventEventHandler.RepeatingInterval(100, (te)-> {
                        
                        Integer[] data = new Integer[30];
                        
                        for (int i = 0; i < data.length; i++) {
                            data[i] = rint.nextInt(100);
                        }
                        
                        sparkLine1.renderPoints(data);
                        sparkLine2.renderPoints(data);
                        
                        dataPoints.setTextAndResize( Arrays.deepToString(data) );
                        
                        tApp.doRefreshCanvasIfModified();
                        
                    } ));
                    

                }).start();
    }
    
    

}
