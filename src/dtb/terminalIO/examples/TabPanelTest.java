package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import dtb.terminalIO.Canvas;
import dtb.terminalIO.Colors;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.chart.BarChart;
import dtb.terminalIO.components.chart.LineChart;
import dtb.terminalIO.components.tab.TabPanel;
import dtb.terminalIO.components.tab.TabPanel.TabAction;
import dtb.terminalIO.components.tab.TabPanel.TabContext;
import dtb.terminalIO.events.ChainedEventHandler.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.KeyInputEventHandler;
import dtb.terminalIO.events.KeyInputEventsProducer;
import dtb.terminalIO.events.Keys;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.Stty;

public class TabPanelTest {
 
    
    public static void main(String args[]) throws Exception {

        EventBus eventBus = new EventBus();
        try {
            new KeyInputEventsProducer(eventBus);
        } catch (Exception e) {
            eventBus.stopConsuming();
            return;
        }

        try {
            Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(TestsCommon.getTerminalScreenForTesting(args));
            master_canvas.setEmptyChar(' ');

            TabPanel tb = new TabPanel(FGBGColors.DEFAULT, new FGBGColors(Colors.Blu).bold(), eventBus, new Dimension.Simple(master_canvas.getHeight()-6, master_canvas.getWidth()-10));
            tb.setBorder(false);

            tb.addTab(new TabAction(Keys.F5_KEYSTROKE, "Hello World") {

                @Override
                public void drawOnActive(TabContext tabCtx) {
                    TextComponent tc = new TextComponent(tabCtx.getColor(),"Hello there!");
                    tabCtx.getSegmentsBuffer().mergeAt(1,1,tc.getSegmentsBuffer());
                }

                @Override
                public void drawOnUnActive(TabContext tabCtx) {
                    // nothing to do ..
                }

            });

            tb.addTab(new TabAction(Keys.F6_KEYSTROKE, "Disney World") {

                int count = 0;

                @Override
                public void drawOnActive(TabContext tabCtx) {
                    TextComponent tc = new TextComponent(tabCtx.getColor(), "  Disney has its own world!");
                    count++;
                    tabCtx.getSegmentsBuffer().merge(tc.getSegmentsBuffer());
                   // master_canvas.t(master_canvas.getHeight() - 2, 0, "F2_KEYSTROKE = " + count + "  ");

                }

                @Override
                public void drawOnUnActive(TabContext tabCtx) {
                }
            });

            tb.addTab(new TabAction(Keys.F7_KEYSTROKE, "BarChart Example") {

                BarChart buildBarChart(Rectangle drawingArea) {

                    BarChart bc = new BarChart(drawingArea.getHeight() - 4, drawingArea.getWidth() - 4);
                    bc.setBorder(true);
                    bc.setPlotColor(new FGBGColors(Colors.BIYel));

                    bc.setTitle("| Import Percentages |");

                    return bc;

                }

                double[] bc_DATA       = new double[] { 10.0, 5.0, 13.0, 4.0, 6.0, 5.0, 6.0, 7.0, 8.0 };
                String[] bc_DATALabels = new String[] { "Lumber", "Metal", "Fish", "Beef", "Lamb", "Liquor", "Milk", "Butter", "Cherries" };

                OnIdleEventEventHandler buildBarChartUpdaterTask(BarChart bc, TabContext tabCtx) {

                    return new OnIdleEventEventHandler.RepeatingInterval(500, new EventHandler() {

                        @Override
                        public void handle(TerminalEvent<?> te) {
                            bc.clearData();
                            bc.clear();
                            tabCtx.clearForRedraw();

                            for (int i = 0; i < bc_DATA.length; i++) {
                                double d = Math.random() * 20;
                                bc.addData(d, bc_DATALabels[i]);
                            }

                            tabCtx.getSegmentsBuffer().mergeAt(2,2, bc.getSegmentsBuffer());

                        }
                    });
                }

                ConditionalEventHandler barChartUpdater;

                int                     count = 0;

                void terminate() {
                    try {
                        Stty.setCooked();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    System.exit(0);
                }

                @Override
                public void drawOnActive(TabContext tabCtx) {

                    if (barChartUpdater != null) return;
                    
                    BarChart bc = buildBarChart(tabCtx.getDrawingArea());

                    barChartUpdater = buildBarChartUpdaterTask(bc, tabCtx);

                    tabCtx.getEventBus().addEventHandler(barChartUpdater);

                }

                @Override
                public void drawOnUnActive(TabContext tabCtx) {

                    if (barChartUpdater != null) {
                        tabCtx.getEventBus().remove(barChartUpdater);
                        barChartUpdater = null;
                    }

                }
            });

            tb.addTab(new TabAction(Keys.F8_KEYSTROKE, "LineChart Example") {

                LineChart buildLineChart(Rectangle drawingArea) {

                    LineChart lc = new LineChart(drawingArea.getHeight() - 4, drawingArea.getWidth() - 4);
                    lc.setBorder(true);
                    lc.setPlotColor(new FGBGColors(Colors.BIYel));
                    // lc.setTitle("Sin/Cos");

                    return lc;

                }

                OnIdleEventEventHandler buildLineChartUpdaterTask(LineChart lc, TabContext tabCtx) {

                    return new OnIdleEventEventHandler.RepeatingInterval(250, new EventHandler() {

                        int j = 0;
                        
                        @Override
                        public void handle(TerminalEvent<?> te) {
                            lc.clearData();
                            lc.clear();
                            tabCtx.clearForRedraw();
                            
                            for (int i = j; i < (220 + j); i++) {
                                double d = Math.sin(((float) i) / 5) * 10;
                                // System.out.println(d);
                                lc.addData(d, i+"");
                                d = Math.cos(((float) i) / 5)*3;
                                lc.addData(d);
                            }
                            
                            j++;

                            tabCtx.getSegmentsBuffer().mergeAt(2,2, lc.getSegmentsBuffer());

                        }
                    });
                }

                ConditionalEventHandler lineChartUpdater;

                int                     count = 0;

                void terminate() {
                    try {
                        Stty.setCooked();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    System.exit(0);
                }

                @Override
                public void drawOnActive(TabContext tabCtx) {

                    if (lineChartUpdater != null) return;
                    
                    LineChart lc = buildLineChart(tabCtx.getDrawingArea());

                    lineChartUpdater = buildLineChartUpdaterTask(lc, tabCtx);

                    tabCtx.getEventBus().addEventHandler(lineChartUpdater);

                }

                @Override
                public void drawOnUnActive(TabContext tabCtx) {

                    if (lineChartUpdater != null) {
                        tabCtx.getEventBus().remove(lineChartUpdater);
                        lineChartUpdater = null;
                    }

                }
            });
            
            
            tb.selectAsActive(Keys.F5_KEYSTROKE);

            AtomicInteger counter = new AtomicInteger(0);

            eventBus.addEventHandler(new OnIdleEventEventHandler.RepeatingInterval(100, new EventHandler() {

                @Override
                public void handle(TerminalEvent<?> te) {
                    // master_canvas.remove_rect()
                    master_canvas.getSegmentsBuffer().removeRect(tb);
                    master_canvas.addComponent(tb);
                    int count = counter.incrementAndGet();
                    master_canvas.t(master_canvas.getHeight() - 2, 0, "IDLE task invoke counter = " + count + "   ");
                    master_canvas.refresh();
                }
            }));

            EventHandler terminate = new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    eventBus.stopConsuming();
                    try {
                        Stty.setCooked();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            eventBus.addEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));

            master_canvas.refresh();
        } finally {
            // eventBus.stopConsuming();
            // Stty.setCooked();
        }
    }
    
    
}
