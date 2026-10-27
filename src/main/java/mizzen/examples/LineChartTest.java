package mizzen.examples;

import mizzen.app.TUIApp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import mizzen.components.ButtonBar;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.chart.LineChart;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.RadioGroup;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventHandler;
import mizzen.events.EventHandlerContainer;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyStroke;
import mizzen.examples.Demo.SimpleDemo;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;

public class LineChartTest {

    public static class LineChartDemo extends SimpleDemo {

        mizzen.components.chart.legacy.LineChart lc;
        Function<Dimension, Dimension> lineChartSize;

        ButtonBar menuButtonBar;
        Composite panel;

        Runnable doRenderLineChart = null;

        public boolean pauseOnIdle = false;

        @Override
        public void resize(Dimension size) {

            Dimension lc_bounds = calc_lc_bounds(lineChartSize.apply(size));
            lc.resize(lc_bounds);
            menuButtonBar.resize(new Dimension.Simple(1, lc_bounds.getWidth()));

            if (doRenderLineChart != null) {
                doRenderLineChart.run();
            }
        }

        private Dimension calc_lc_bounds(Dimension containingDimension) {
            return new Dimension.Simple(containingDimension.getHeight() - 1, containingDimension.getWidth());
        }

        public Composite getLineChartPanelComponent() {
            if (panel == null) {
                this.panel = new VerticalPanel().setVerticalSpacer(0);
            }
            return panel;
        }

        @Override
        public void setUpDemo(EventHandlerContainer eventBus, Rectangle drawingArea) {

            if (drawingArea == null) {
                throw new NullPointerException("drawingArea parameter null!");
            }

            getLineChartPanelComponent().clearAndRemoveAllChildren();

            this.lineChartSize = (size) -> new Dimension.Simple(size.getHeight(), size.getWidth());
            
            Dimension lc_bounds = calc_lc_bounds(this.lineChartSize.apply(drawingArea));

            lc = new mizzen.components.chart.legacy.LineChart(lc_bounds);
            lc.setBorder(true);
            lc.setPlotColor(new Colors.FGBGColors(Colors.IYel));
            lc.setSeriesColor("cos", Colors.Blu);
            lc.setTitle(" Line Chart Demo's ");

            menuButtonBar = new ButtonBar(Colors.BIBla);
            menuButtonBar.setNoBorderChars();

            getLineChartPanelComponent().add(lc);
            getLineChartPanelComponent().add(menuButtonBar);

            class SeriesDemo {

                String name;
                Color seriesColor;
                boolean enabled;
                Consumer<Integer> dataAdder;
                KeyStroke onKeyStroke;

                public SeriesDemo(KeyStroke onKeyStroke, String name, Color seriesColor, boolean enabled,
                        Consumer<Integer> dataAdder) {
                    super();
                    this.name = name;
                    this.seriesColor = seriesColor;
                    this.enabled = enabled;
                    this.dataAdder = dataAdder;
                    this.onKeyStroke = onKeyStroke;
                }
            }

            List<SeriesDemo> seriesDemos = new ArrayList<SeriesDemo>();

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

            seriesDemos.forEach((seriesDemo)->{
                RadioGroup<Boolean> seriesToggle = RadioGroup.onOff(seriesDemo.name, seriesDemo.enabled).setColor(seriesDemo.seriesColor);
                if (seriesDemo.onKeyStroke != null) {
                    seriesToggle.setHotKey(seriesDemo.onKeyStroke);
                }
                seriesToggle.onChange((te) -> {
                    seriesDemo.enabled = seriesToggle.getValue();
                });
                menuButtonBar.addComponent(seriesToggle);
                menuButtonBar.addComponent(new TextComponent("   "));
            });


            OnIdleEventEventHandler idleHandler = new OnIdleEventEventHandler.RepeatingInterval(100,
                    new EventHandler() {

                        int j = 0;

                        Object lock = new Object();

                        @Override
                        public void handle(TerminalEvent<?> te) {
                            synchronized (lock) {
                                if (pauseOnIdle) {
                                    return;
                                }
                                __handle(te);
                            }
                        }

                        private void __handle(TerminalEvent<?> te) {

                            renderPreUPdate();

                            lc.clearData();
                            lc.clear();

                            lc.setGridBounds(0, 0, 136, 262);

                            for (SeriesDemo seriesDemo : seriesDemos) {

                                if (seriesDemo.enabled) {

                                    lc.setSeriesColor(seriesDemo.name, seriesDemo.seriesColor);
                                    seriesDemo.dataAdder.accept(j);

                                }

                            }

                            flushOutput();
                            j++;
                        }
                    });

            // idleHandler.handle(null);
            addAndRememberEventHandler(eventBus, idleHandler);      // (the menu bar's keys reach it through the component tree)

            doRenderLineChart = new Runnable() {

                Object lock = new Object();

                @Override
                public void run() {
                    synchronized (lock) {
                        idleHandler.handle(null);
                    }

                }
            };

            doRenderLineChart.run();
        }

    }


    /** chart sizes: the size argument at start, and the keys t / s / m / l while running (legacy) */
    private static final String[][] SIZE_KEYS = { {"t", "TINY"}, {"s", "SMALL"}, {"m", "MEDIUM"}, {"l", "LARGE"} };

    /** what is on show: its components, and what to undo when it goes */
    static class Shown {
        final List<Component<?>> components;
        final Runnable stop;

        Shown(List<Component<?>> components, Runnable stop) {
            this.components = components;
            this.stop = stop;
        }
    }


    /**
     * Line charts: the new one (sine and cosine, and a series fed live - click a line or its name,
     * LEFT / RIGHT along it, UP / DOWN between series, [x] in the legend hides / shows one) and, to compare, the legacy demo (five series redrawn 10 times a second; keys
     * 1..5 switch a series, t / s / m / l size the chart). The switch at the top picks which is shown.
     * When the legacy charts are dropped, so is the switch and renderLegacy.
     *
     * usage: LineChartTest [tiny | small | medium]   (the legacy chart's starting size; default: large)
     */
    public static void main(String[] args) {
        String startSize = args.length > 0 ? args[0].toUpperCase() : "LARGE";
        new TUIApp(args)
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp) -> {
            RadioGroup<String> version = RadioGroup.horizontal("new", "legacy");
            HorizontalPanel top = new HorizontalPanel();
            top.add(new TextComponent(" Line chart: "));
            top.add(version);
            tApp.getMainApplicationPanel().add(0, 0, top);

            Shown[] shown = { null };   // what this run shows - not a static: it would outlive the run (e.g. an app embedded twice)

            show(tApp, shown, renderNew(tApp));
            version.onChange((te) -> {
                show(tApp, shown, "legacy".equals(version.getValue()) ? renderLegacy(tApp, startSize) : renderNew(tApp));
            });
        }).start();
    }

    /** takes off what was shown, puts on the new */
    private static void show(TUIApp tApp, Shown[] shown, Shown next) {
        if (shown[0] != null) {
            shown[0].stop.run();
            for (Component<?> c : shown[0].components) {
                tApp.getMainApplicationPanel().remove(c);
            }
        }
        shown[0] = next;
    }

    /** the new chart: sine (filled), cosine, and a series fed a point every 200 ms (the latest 120 kept) */
    static Shown renderNew(TUIApp tApp) {
        Composite appPanel = (Composite) tApp.getMainApplicationPanel();
        LineChart chart = new LineChart(Colors.Gre, new Dimension.Simple(Math.max(10, appPanel.getHeight() - 4), appPanel.getWidth()))
                .series("sine", Colors.Blu)
                .series("cosine", Colors.BYel)
                .series("live", Colors.BRed);
        for (int i = 0; i <= 120; i++) {
            double x = i / 10.0;
            chart.add("sine", x, Math.sin(x) * 3 + 4).add("cosine", x, Math.cos(x) * 2 + 4);
        }
        chart.fillUnder("sine", true);
        chart.setTitle(" Line chart - click a line or its name: LEFT / RIGHT along it, UP / DOWN between series; [x] hides ");

        TextComponent status = new TextComponent(" Nothing selected yet");
        chart.onChange((te) -> {
            LineChart.Point p = chart.getValue();
            status.setTextAndResize(String.format(" %s point %d: x=%.2f  y=%.2f", p.series(), p.index(), p.x(), p.y()));
        });

        List<double[]> live = new ArrayList<double[]>();
        double[] t = { 0 };
        OnIdleEventEventHandler.RepeatingInterval feed = new OnIdleEventEventHandler.RepeatingInterval(200, (te) -> {
            live.add(new double[] { t[0], 4 + Math.sin(t[0] * 3) * 1.5 + (Math.random() - 0.5) });
            if (live.size() > 120) {
                live.remove(0);
            }
            t[0] += 0.1;
            chart.clear("live");
            for (double[] p : live) {
                chart.add("live", p[0], p[1]);
            }
            chart.redraw();
        });
        tApp.getEventBus().appendEventHandler(feed);

        appPanel.add(2, 0, chart);
        appPanel.add(appPanel.getHeight() - 1, 0, status);

        // the terminal resized: the chart takes the new size (and redraws to fit), the status line goes to the new bottom
        appPanel.setOnResize((size) -> {
            chart.resize(new Dimension.Simple(Math.max(10, size.getHeight() - 4), size.getWidth()));
            appPanel.remove(status);
            appPanel.add(size.getHeight() - 1, 0, status);
        });
        return new Shown(Arrays.<Component<?>>asList(chart, status), () -> {
            tApp.getEventBus().removeEventHandler(feed);
            appPanel.setOnResize((size) -> { });
        });
    }

    /** the legacy demo, as it was */
    static Shown renderLegacy(TUIApp tApp, String startSize) {
        Composite appPanel = (Composite) tApp.getMainApplicationPanel();
        String[] size = { startSize };
        LineChartDemo lineChartDemo = new LineChartDemo();
        lineChartDemo.setUpDemo(tApp.getEventBus(), chartBounds(size[0], appPanel));
        appPanel.add(1, 0, lineChartDemo.getLineChartPanelComponent());
        appPanel.setOnResize((newSize) -> lineChartDemo.resize(chartBounds(size[0], appPanel)));
        List<KeyInputEventHandler> sizeKeys = new ArrayList<KeyInputEventHandler>();
        for (String[] sizeKey : SIZE_KEYS) {
            KeyInputEventHandler key = new KeyInputEventHandler(
                    (te) -> {
                        size[0] = sizeKey[1];
                        tApp.clearCanvas();
                        lineChartDemo.resize(chartBounds(size[0], appPanel));
                    },
                    new KeyStroke(sizeKey[0].charAt(0)), new KeyStroke(sizeKey[0].toUpperCase().charAt(0)));
            sizeKeys.add(key);
            tApp.getEventBus().appendEventHandler(key);
        }
        return new Shown(Arrays.<Component<?>>asList(lineChartDemo.getLineChartPanelComponent()), () -> {
            lineChartDemo.tearDownDemo(tApp.getEventBus());
            appPanel.setOnResize((newSize) -> { });
            for (KeyInputEventHandler key : sizeKeys) {
                tApp.getEventBus().removeEventHandler(key);
            }
        });
    }

    /** the chart's area for a size name, never bigger than the app's panel */
    private static Rectangle chartBounds(String size, Dimension panel) {
        if ("TINY".equals(size)) {
            return new Rectangle.Simple(0, 0, Math.min(15, panel.getHeight()), Math.min(60, panel.getWidth()));
        }
        if ("SMALL".equals(size)) {
            return new Rectangle.Simple(0, 0, Math.min(20, panel.getHeight()), Math.min(100, panel.getWidth()));
        }
        if ("MEDIUM".equals(size)) {
            return new Rectangle.Simple(0, 0, Math.min(25, panel.getHeight()), Math.min(100, panel.getWidth()));
        }
        return new Rectangle.Simple(0, 0, panel.getHeight() - 1, panel.getWidth());
    }

}
