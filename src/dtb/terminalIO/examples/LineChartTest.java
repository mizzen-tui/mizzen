package dtb.terminalIO.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode;
import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.chart.LineChart;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.KeyInputEventHandler;
import dtb.terminalIO.events.Keys.KeyStroke;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.examples.Demo.SimpleDemo;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

public class LineChartTest {

    public static class LineChartDemo extends SimpleDemo {

        @Override
        public void setUpDemo(EventBus eventBus, Rectangle bounds, IScreenLineSegmentsBuffer.Provider segmentsBufferProvider) {

            LineChart lc = new LineChart(bounds.getHeight() - 5, bounds.getWidth() - 4);
            lc.setBorder(true);
            lc.setPlotColor(new FGBGColors(Colors.IYel));
            lc.setSeriesColor("cos", Colors.Blu);
            lc.setTitle(" Line Chart Demo's ");
            
            TextComponent menuOoptions = new TextComponent(FGBGColors.NO_COLOR_SPECIFIED, new Dimension.Simple(1,bounds.getWidth() - 4), "");

            class SeriesDemo {

                String            name;
                Color             seriesColor;
                boolean           enabled;
                Consumer<Integer> dataAdder;
                KeyStroke         onKeyStroke;

                public SeriesDemo(KeyStroke onKeyStroke, String name, Color seriesColor, boolean enabled, Consumer<Integer> dataAdder) {
                    super();
                    this.name = name;
                    this.seriesColor = seriesColor;
                    this.enabled = enabled;
                    this.dataAdder = dataAdder;
                    this.onKeyStroke = onKeyStroke;

                    if (onKeyStroke != null) {
                        addAndRememberEventHandler(eventBus, new KeyInputEventHandler(onKeyStroke, new EventHandler() {

                            @Override
                            public void handle(TerminalEvent<?> te) {
                                SeriesDemo.this.enabled = !SeriesDemo.this.enabled;
                            }
                        }));

                    }
                }
            }

            List<SeriesDemo> seriesDemos = new ArrayList<SeriesDemo>();

            // Keys.A_KEYSTROKE
            
            seriesDemos.add(new SeriesDemo(new KeyStroke('1'), "sine", Colors.Yel, true, new Consumer<Integer>() {
                @Override
                public void accept(Integer j) {
                    int width = lc.getGridRectangle().getWidth();
                    j = j % width;
                    for (int i = j; i < (width + j); i++) {
                        double d = Math.sin(((float) i) / 5) * 10 + 30;
                        lc.addData("sine", d, (double) i, i + "");
                    }
                }
            }));

            seriesDemos.add(new SeriesDemo(new KeyStroke('2'), "cos", Colors.BCya, true, new Consumer<Integer>() {
                @Override
                public void accept(Integer j) {
                    int width = lc.getGridRectangle().getWidth();
                    j = j % width;
                    for (int i = j; i < (width + j); i++) {
                        double d = Math.cos(((float) i) / 5) * 30 + 60;
                        lc.addData("cos", d - 5, (double) i, i + "");
                    }
                }
            }));

            seriesDemos.add(new SeriesDemo(new KeyStroke('3'), "line1", Colors.IBlu, true, new Consumer<Integer>() {
                @Override
                public void accept(Integer j) {
                    lc.addData("line1", 10d, 15d, "15");
                    lc.addData("line1", 60d, 200d, "200");
                }
            }));

            seriesDemos.add(new SeriesDemo(new KeyStroke('4'), "poly1", Colors.Cya, true, new Consumer<Integer>() {
                @Override
                public void accept(Integer j) {
                    for (int x = 0; x < 300; x++) {
                        double y = Math.pow(x, 2) / (j % 500 + 1);
                        lc.addData("poly1", y, x + "");
                    }
                }
            }));

            seriesDemos.add(new SeriesDemo(new KeyStroke('5'), "poly2", Colors.IYel, true, new Consumer<Integer>() {
                @Override
                public void accept(Integer j) {
                    for (int x = 0; x < 300; x++) {
                        double y = x - Math.pow(x, 2) / (j % 800 + 1);
                        lc.addData("poly2", y, x + "");
                    }
                }
            }));

            OnIdleEventEventHandler idleHandler = new OnIdleEventEventHandler.RepeatingInterval(100, new EventHandler() {

                int j = 0;

                @Override
                public void handle(TerminalEvent<?> te) {

                    renderPreUPdate();

                    lc.clearData();
                    lc.clear();

                    lc.setGridBounds(0, 0, 136, 262);
                    
                    StringBuffer menuText = new StringBuffer();
                    
                    String noCOLORStr = ControlCode.RCol.asString();
                    String brCOLOR = Colors.BIBla.asString();

                    for (SeriesDemo seriesDemo : seriesDemos) {

                        if (seriesDemo.enabled) {

                            lc.setSeriesColor(seriesDemo.name, seriesDemo.seriesColor);
                            seriesDemo.dataAdder.accept(j);
                            

                        }
                        if (seriesDemo.onKeyStroke != null) {
                            
                            Color onCOLOR     = ( seriesDemo.enabled) ? Colors.BIWhi : Colors.Whi; 
                            Color offCOLOR    = ( seriesDemo.enabled) ? Colors.Whi   : Colors.BIWhi;
                            
                            String onLeftBr   = ( seriesDemo.enabled) ? brCOLOR +  "["  + noCOLORStr
                                                                      : " "; 
                            String onRightBr  = ( seriesDemo.enabled) ? brCOLOR + "]"    + noCOLORStr
                                                                      : " "; 
                            String offLeftBr  = ( seriesDemo.enabled) ? " " 
                                                                      : brCOLOR + "["  + noCOLORStr; 
                            String offRightBr = ( seriesDemo.enabled) ? " "   
                                                                      : brCOLOR + "]"  + noCOLORStr; 
                            
                            String seriesHelp = new FGBGColors(Colors.BIWhi).asString() +  
                                                seriesDemo.onKeyStroke.getName() + 
                                                ControlCode.RCol.asString() +  
                                                ": " +
                                                seriesDemo.seriesColor.asString() +
                                                seriesDemo.name +
                                                ControlCode.RCol.asString() +
                                                " " +
                                                onLeftBr + onCOLOR.asString()  + "ON" + noCOLORStr + onRightBr +
                                                " / " +
                                                offLeftBr + offCOLOR.asString() + "OFF" + noCOLORStr + offRightBr;
                            
                            menuText.append(seriesHelp);
                            menuText.append("      ");
                        }

                    }

                    menuOoptions.setText(menuText.toString());
                    
                    Position lc_Pos = bounds.centered(lc, Position.ZERO);
                    
                    segmentsBufferProvider.getSegmentsBuffer().mergeAt(lc_Pos, lc.getSegmentsBuffer());
                    
                    segmentsBufferProvider.getSegmentsBuffer().mergeAt(lc_Pos.offsetAdd( lc.getHeight() + 1 , 0 ), menuOoptions.getSegmentsBuffer());

                    // refresh/flush segments to output

                    flushOutput();

                    j++;

                }
            });

            addAndRememberEventHandler(eventBus, idleHandler);

        }

    }
}
