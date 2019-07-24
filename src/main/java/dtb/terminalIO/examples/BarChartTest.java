package dtb.terminalIO.examples;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.chart.BarChart;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.examples.Demo.SimpleDemo;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer.Provider;

/**
 * Demos a barchart
 *
 */
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

        public void setUpDemo(EventHandlerContainer eventBus, Rectangle bounds) {
            setUpDemo(eventBus, bounds, null);
        }

        BarChart buildBarChart(Rectangle drawingArea) {

            BarChart bc = new BarChart(drawingArea.getHeight(), drawingArea.getWidth());
            bc.setBorder(true);
            bc.setPlotColor(new FGBGColors(Colors.BIYel));

            bc.setTitle("| Import Percentages |");

            return bc;

        }

        public BarChart getBarChart(Rectangle drawingArea) {
            if (bc == null) {
                bc = buildBarChart(drawingArea);
            }
            return this.bc;
        }

        public BarChart getBarChart() {
            if (bc == null) {
                throw new IllegalStateException("Barchart is null.  call getBarChart(Rectangle drawingArea) first");
            }
            return this.bc;
        }

        double[] bc_DATA = new double[] { 10.0, 5.0, 13.0, 4.0, 6.0, 5.0, 6.0, 7.0, 8.0 };
        String[] bc_DATALabels = new String[] { "Lumber", "Metal", "Fish", "Beef", "Lamb", "Liquor", "Milk", "Butter",
                "Cherries" };

        OnIdleEventEventHandler buildBarChartUpdaterTask(BarChart bc, Provider segmentsBufferProvider) {

            return new OnIdleEventEventHandler.RepeatingInterval(500, new EventHandler() {

                @Override
                public void handle(TerminalEvent<?> te) {
                    bc.clearData();
                    bc.clear();

                    bc.markAsDirty(); // necessary?

                    for (int i = 0; i < bc_DATA.length; i++) {
                        double d = Math.random() * 20;
                        bc.addData(d, bc_DATALabels[i]);
                    }

                    if (segmentsBufferProvider != null) {
                        // remove / erase rectangle where grid will be copied into
                        segmentsBufferProvider.getSegmentsBuffer().removeRect(bc);

                        // copy grid contents into segmentsBufferProvider ( the parent component)
                        // this technique is utterly useless for segmentsBufferProvider's that are compoonents because
                        // components clear their buffer before doing a draw operation
                        segmentsBufferProvider.getSegmentsBuffer().mergeAt(0, 0, bc.getSegmentsBuffer());

                        if (segmentsBufferProvider instanceof Component) {
                            ((Component)segmentsBufferProvider).markAsDirty();
                        }

                    }
                    //
                    flushOutput();

                }
            });
        }

    }

    public static void main(String[] args) {

        TUIApp app = new TUIApp(args).setEmptyChar(' ')
                                     .setMainApplicationPanel(new Composite().setEmptyChar(' '))
                                     .confirmOnExit();
                
        // set to size of canvas
        ((Composite)app.getMainApplicationPanel()).resize(app.getCanvas());
        
        BarChartDemo barChartDemo = new BarChartDemo() {
            public void flushOutput() {
                app.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(this));
            }
        };

        app.setOnResizeAction( (tApp, size) -> {

            tApp.clearCanvas();
            tApp.setCanvasSize(size);
            
            ((Composite)tApp.getMainApplicationPanel()).resize(size);
            
            if (barChartDemo.bc != null) {
                barChartDemo.resize(size);
            }

        });
        
                
        app.setOnReadyStart((tApp) -> {

                    tApp.getMainApplicationPanel().add( 1, 1, new TextComponent("Braille Characters used for plotting"));
                    
                    barChartDemo.setUpDemo(tApp.getEventBus(), tApp.getMainApplicationPanel());
                    
                    ((Composite)tApp.getMainApplicationPanel()).add( barChartDemo.getBarChart()  );
                    
                    
                });

        app.start();


    }

}
