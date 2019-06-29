package dtb.terminalIO.examples;

import java.io.IOException;

import dtb.terminalIO.components.chart.BarChart;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.OnResizeEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyInputEventsProducer;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.resize.TerminalResizeDetection;
import dtb.terminalIO.examples.Demo.SimpleDemo;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer.Provider;
import dtb.terminalIO.util.Stty;

public class BarChartTest {
    
    
    public static class BarChartDemo extends SimpleDemo {
        
        BarChart bc;

        @Override
        public void resize(Dimension size) {
             bc.resize(size);
        }

        @Override
        public void setUpDemo(EventHandlerContainer eventBus, Rectangle bounds, Provider segmentsBufferProvider) {
            
            this.bc = buildBarChart(bounds);
            
            OnIdleEventEventHandler updaterTask = buildBarChartUpdaterTask(bc, segmentsBufferProvider);
            
            addAndRememberEventHandler(eventBus, updaterTask);
            
        }
        
        BarChart buildBarChart(Rectangle drawingArea) {

            BarChart bc = new BarChart(drawingArea.getHeight() , drawingArea.getWidth() );
            bc.setBorder(true);
            bc.setPlotColor(new FGBGColors(Colors.BIYel));

            bc.setTitle("| Import Percentages |");

            return bc;

        }
        
        double[] bc_DATA       = new double[] { 10.0, 5.0, 13.0, 4.0, 6.0, 5.0, 6.0, 7.0, 8.0 };
        String[] bc_DATALabels = new String[] { "Lumber", "Metal", "Fish", "Beef", "Lamb", "Liquor", "Milk", "Butter", "Cherries" };

        OnIdleEventEventHandler buildBarChartUpdaterTask(BarChart bc, Provider segmentsBufferProvider) {

            return new OnIdleEventEventHandler.RepeatingInterval(500, new EventHandler() {

                @Override
                public void handle(TerminalEvent<?> te) {
                    bc.clearData();
                    bc.clear();
                    
                    bc.markAsDirty();  // necessary?
                    
                    for (int i = 0; i < bc_DATA.length; i++) {
                        double d = Math.random() * 20;
                        bc.addData(d, bc_DATALabels[i]);
                    }
                    
                    if (segmentsBufferProvider != null) {
                        // remove / erase rectangle where grid will be copied into
                        segmentsBufferProvider.getSegmentsBuffer().removeRect(bc);
    
                        // copy grid contents into segmentsBufferProvider ( the parent component)
                        segmentsBufferProvider.getSegmentsBuffer().mergeAt(0, 0, bc.getSegmentsBuffer());
                        
//                        if (segmentsBufferProvider instanceof Component) {
//                            ((Component)segmentsBufferProvider).markAsDirty();
//                        }
                        
                }
                // 
                flushOutput();
                    
                }
            });
        }
        
        
        
    }
    
    
    public static void main(String[] args) {
        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
        master_canvas.setEmptyChar(' ');

        BarChartDemo barChartDemo = new BarChartDemo() {
            public void flushOutput() {
                master_canvas.refresh();
            }
        };
        
        
        screen.add(ControlCode.HIDE_CURSOR);
        // screen.refresh();
        // master_canvas.t("  Braille Characters used for plotting");
        master_canvas.t(" ");

        master_canvas.refresh();

        EventBus eventBus = new EventBus();
        KeyInputEventsProducer keyInputEventsProducer = null;
        try {
            keyInputEventsProducer = new KeyInputEventsProducer(eventBus);
        } catch (Exception e) {
            eventBus.stopConsuming();
            return;
        }

        master_canvas.t("Braille Characters used for plotting");
        master_canvas.refresh();

        barChartDemo.setUpDemo(eventBus, master_canvas.asRectangle(), master_canvas);

        EventHandler terminate = new EventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {
                eventBus.stopConsuming();
                screen.add(ControlCode.SHOW_CURSOR);
                screen.refresh();

                try {
                    Stty.setCooked();
                } catch (Exception e) {
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

        TerminalResizeDetection.initiate(screen, eventBus);

        eventBus.appendEventHandler(new OnResizeEventHandler() {
            @Override
            public void handleResize(Dimension size) {
                master_canvas.setSize(size.getHeight() , size.getWidth() ); 
                master_canvas.clearCanvas();
                //master_canvas.refresh();
                barChartDemo.resize(master_canvas);
                eventBus.enableIdleEvents();
            }
        });
        
        
        
        
    }

}
