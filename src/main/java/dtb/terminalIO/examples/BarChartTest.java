package dtb.terminalIO.examples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.chart.ChartItem;
import dtb.terminalIO.components.chart.BarChart;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.RadioGroup;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;

/**
 * Demos a barchart randomly displaying data.
 *
 */
public class BarChartTest {

    public static double[] bc_DATA       = new double[] { 10.0, 5.0, 13.0, 4.0, 6.0, 5.0, 6.0, 7.0, 8.0 };
    public static String[] bc_DATALabels = new String[] { "Lumber", "Metal", "Fish", "Beef", "Lamb", "Liquor", "Milk", "Butter", "Cherries" };

    public static class BarChartRandomDataDemo extends AbstractComponentWrappper<dtb.terminalIO.components.chart.legacy.BarChart> implements ToFocusable {

        Focusable focusable = null;
        OnIdleEventEventHandler.RepeatingInterval  drawChartTimedEventHandler;

        public BarChartRandomDataDemo(Color color, Dimension drawingArea, double[] bc_DATA, String[] bc_DATALabels) {
            super(color, new dtb.terminalIO.components.chart.legacy.BarChart(drawingArea.getHeight(), drawingArea.getWidth()));
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
                dtb.terminalIO.components.chart.legacy.BarChart barchart = this.getWrappedComponent();
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
        public Focusable asFocusable() {
            return focusable;
        }
    } 

    /** a region's sales: what the new chart shows */
    static class Sale {
        final String region;
        double amount;

        Sale(String region, double amount) {
            this.region = region;
            this.amount = amount;
        }
    }

    /** what is on show: its components, and what to undo when it goes */
    static class Shown {
        final List<Component<?>> components;
        final Runnable stop;

        Shown(List<Component<?>> components, Runnable stop) {
            this.components = components;
            this.stop = stop;
        }
    }

    private static Shown shown;

    /**
     * Bar charts: the new one (your objects as bars, values changing every second - click a bar, or
     * use the arrows) and, to compare, the legacy one. The switch at the top picks which is shown.
     * When the legacy charts are dropped, so is the switch and renderLegacy.
     */
    public static void main(String[] args) {
        new TUIApp(args).setEmptyChar(' ')
                        .setDefaultMainApplicationPanel()
                        .confirmOnExit()
                        .setOnReadyStart((tApp) -> {
                            Button quit = new Button(Colors.BIGre, "Quit");
                            quit.setHotKey(Keys.CTRL_C_KEYSTROKE);
                            quit.onChange((te) -> tApp.getTerminateAppHotKeyEventHandler().handle(te));

                            RadioGroup<String> version = RadioGroup.horizontal("new", "legacy");
                            HorizontalPanel top = new HorizontalPanel().setHorizontalSpacer(2);
                            top.add(new TextComponent(" Bar chart: "));
                            top.add(version);
                            top.add(quit);
                            tApp.getMainApplicationPanel().add(0, 0, top);

                            show(tApp, renderNew(tApp));
                            version.onChange((te) -> {
                                show(tApp, "legacy".equals(version.getValue()) ? renderLegacy(tApp) : renderNew(tApp));
                                te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent((Component<?>) tApp.getMainApplicationPanel()));
                            });
                        })
                        .start();
    }

    /** takes off what was shown, puts on the new */
    private static void show(TUIApp tApp, Shown next) {
        ChildComponentManager<?> main = tApp.getMainApplicationPanel();
        if (shown != null) {
            shown.stop.run();
            for (Component<?> c : shown.components) {
                main.remove(c);
            }
        }
        shown = next;
    }

    /** the new chart: sales by region, changing every second; click a bar (or arrows) and the status line says which */
    static Shown renderNew(TUIApp tApp) {
        List<Sale> sales = new ArrayList<Sale>();
        for (int i = 0; i < bc_DATA.length; i++) {
            sales.add(new Sale(bc_DATALabels[i], bc_DATA[i]));
        }

        BarChart<Sale> bars = new BarChart<Sale>(Colors.Cya, new Dimension.Simple(20, 90))
                .barSize((sale) -> sale.amount)
                .barLabel((sale) -> sale.region)
                .setItems(sales);
        bars.setTitle(" Sales by region - click a bar, or TAB here and use the arrows ");

        TextComponent status = new TextComponent(" Nothing selected yet");
        bars.onChange((te) -> {
            ChartItem<Sale> bar = bars.getValue();
            status.setTextAndResize(String.format(" %s: %.1f (%.1f%% of all)", bar.label(), bar.value(), bar.percent()));
            te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(status));
        });

        OnIdleEventEventHandler.RepeatingInterval changing = new OnIdleEventEventHandler.RepeatingInterval(1000, (te) -> {
            for (Sale sale : sales) {
                sale.amount = Math.max(1, sale.amount + (Math.random() - 0.5) * 4);
            }
            bars.redraw();
            te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(bars));
        });
        tApp.getEventBus().appendEventHandler(changing);

        tApp.getMainApplicationPanel().add(2, 0, bars);
        tApp.getMainApplicationPanel().add(23, 0, status);
        return new Shown(Arrays.<Component<?>>asList(bars, status), () -> tApp.getEventBus().removeEventHandler(changing));
    }

    /** the legacy chart: random values every half second */
    static Shown renderLegacy(TUIApp tApp) {
        Composite main = (Composite) tApp.getMainApplicationPanel();
        BarChartRandomDataDemo barChartDemo = new BarChartRandomDataDemo(new Dimension.Simple(main.getHeight() - 3, main.getWidth()));
        barChartDemo.setEmptyColor(Colors.On_Gre);
        main.add(2, 0, barChartDemo);
        barChartDemo.setupTimer(tApp.getEventBus());
        return new Shown(Arrays.<Component<?>>asList(barChartDemo), () -> barChartDemo.tearDownTimer(tApp.getEventBus()));
    }
}
