package dtb.terminalIO.examples;

import java.util.function.Function;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.chart.BarChart;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.modal.MessageBox;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.examples.Demo.SimpleDemo;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;

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
        public void setUpDemo(EventHandlerContainer eventBus, Rectangle drawingArea) {

            this.bc = buildBarChart(drawingArea);
            
            OnIdleEventEventHandler updaterTask = buildBarChartUpdaterTask(bc);
            

            addAndRememberEventHandler(eventBus, updaterTask);

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

        OnIdleEventEventHandler buildBarChartUpdaterTask(BarChart bc) {

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
        
        Button info = new Button(Colors.BIWhi,"Barchart DEMO");
        info.onChange((te)-> MessageBox.showInfoDialog( te.getEventBus() , app.getMainApplicationPanel(), "Powered by dtbTerminalIO ") );

        Button quit = new Button(Colors.BIGre,"Quit");
        quit.setHotKey(Keys.CTRL_C_KEYSTROKE);
        quit.onChange((te)->app.getTerminateAppHotKeyEventHandler().handle(te));
        
        HorizontalPanel hPanel = new HorizontalPanel().setHorizontalSpacer(5);
        hPanel.add(info);
        hPanel.add(quit);

        app.getMainApplicationPanel().add( 0, 1, hPanel);
        
        Function<Dimension, Dimension> barchartSize = new Function<Dimension, Dimension>() {

            @Override
            public Dimension apply(Dimension size) {
                return new Dimension() {
                    @Override
                    public int getWidth() {
                        return size.getWidth();
                    }

                    @Override
                    public int getHeight() {
                        return size.getHeight()-2;
                    }
                };
            }
            
        };
                
        
        app.setOnResizeAction( (tApp, size) -> {

            tApp.clearCanvas();
            tApp.setCanvasSize(size);
            
            ((Composite)tApp.getMainApplicationPanel()).resize(size);
            
            if (barChartDemo.bc != null) {
                barChartDemo.resize(barchartSize.apply(tApp.getMainApplicationPanel()));
            }

        });
        
                
        app.setOnReadyStart((tApp) -> {

                    barChartDemo.setUpDemo(tApp.getEventBus(), new Rectangle.Simple(barchartSize.apply(tApp.getMainApplicationPanel())));

                    tApp.getMainApplicationPanel().add(2,0,barChartDemo.getBarChart());
                    
                    FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),tApp.getEventBus());
                    tApp.getEventBus().appendEventHandler(  mainFocusManager );
                    
                });

        app.start();


    }

}
