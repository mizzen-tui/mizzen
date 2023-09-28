package dtb.terminalIO.examples;

import java.util.function.Function;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.AbstractWrappedComponentAndFocusable;
import dtb.terminalIO.components.chart.BarChart;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.components.modal.MessageBox;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.TerminalDimensions;

/**
 * Demos a barchart randomly displaying data.
 *
 */
public class BarChartTest {

    public static double[] bc_DATA       = new double[] { 10.0, 5.0, 13.0, 4.0, 6.0, 5.0, 6.0, 7.0, 8.0 };
    public static String[] bc_DATALabels = new String[] { "Lumber", "Metal", "Fish", "Beef", "Lamb", "Liquor", "Milk", "Butter", "Cherries" };

    public static class BarChartRandomDataDemo extends AbstractWrappedComponentAndFocusable<BarChart>{

        Focusable focusable = null;
        OnIdleEventEventHandler.RepeatingInterval  drawChartTimedEventHandler;

        public BarChartRandomDataDemo(Color color, Dimension drawingArea, double[] bc_DATA, String[] bc_DATALabels) {
            super(color, new BarChart(drawingArea.getHeight(), drawingArea.getWidth()));
            focusable = new DefaultFocusableBehaviour.HiliteOnFocus(this.getWrappedComponent());
        }
        public BarChartRandomDataDemo(Dimension drawingArea, double[] bc_DATA, String[] bc_DATALabels) {
            this(Colors.DEFAULT_COLOR, drawingArea, bc_DATA, bc_DATALabels);
        }

        public BarChartRandomDataDemo(Color color, Dimension drawingArea) {
            this(color, drawingArea, bc_DATA, bc_DATALabels);
        }
        
        public BarChartRandomDataDemo(Dimension drawingArea) {
            this(drawingArea, bc_DATA, bc_DATALabels);
        }
        

        OnIdleEventEventHandler getDrawChartTimedEventHandler() {
            if (this.drawChartTimedEventHandler == null)  {
                BarChart barchart = this.getWrappedComponent();
                drawChartTimedEventHandler = new OnIdleEventEventHandler.RepeatingInterval(500, new EventHandler() {
                    @Override
                    public void handle(TerminalEvent<?> te) {
                        BarChartRandomDataDemo.this.getWrappedComponent().clearData();
                        barchart.clear();
                        for (int i = 0; i < bc_DATA.length; i++) {
                            double d = Math.random() * 20;
                            barchart.addData(d, bc_DATALabels[i]);
                        }
                        
                        te.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(BarChartRandomDataDemo.this) );
                        
                    }
                });
            }
            return drawChartTimedEventHandler;
        }
        
        public BarChartRandomDataDemo setupTimer(EventBus eventBus) {
            eventBus.appendEventHandler(getDrawChartTimedEventHandler());
            return this;
        }

        public void tearDownTimer(EventBus eventBus) {
            eventBus.removeEventHandler(getDrawChartTimedEventHandler());
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if ( eventHandler().canHandle(te) == null ) {
                return null;
            }
            return this;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            if (te.getType()  == TYPE.TIMER) {
                Logging.info("HOW?");
            }
            eventHandler().handle(te);
        }
        
        
        @Override
        protected Focusable asFocusable() {
            return focusable;
        }
    } 

    public static void main(String[] args) {

        Dimension termSize = TerminalDimensions.create();

        TUIApp app = new TUIApp(args).setEmptyChar(' ')
                                     .setDefaultMainApplicationPanel()
                                     .confirmOnExit();
        // set to size of canvas

        app.getCanvas().setSize( termSize );

        ((Composite)app.getMainApplicationPanel()).resize(app.getCanvas());

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

        BarChartRandomDataDemo  barChartDemo = new BarChartRandomDataDemo( barchartSize.apply(((Composite)app.getMainApplicationPanel()))  );
        barChartDemo.setEmptyColor(Colors.On_Gre);

        app.setOnResizeAction( (tApp, size) -> {

            tApp.clearCanvas();
            tApp.setCanvasSize(size);

            ((Composite)tApp.getMainApplicationPanel()).resize(size);
            
            barChartDemo.resize(barchartSize.apply(tApp.getMainApplicationPanel()));
        });

        app.setOnReadyStart((tApp) -> {
                    tApp.getMainApplicationPanel().add(2,0,barChartDemo);
                    barChartDemo.setupTimer(tApp.getEventBus());
                });

        app.start();


    }

}
