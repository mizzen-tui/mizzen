package dtb.terminalIO.examples;

import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.app.TUIApp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.chart.LineChart;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.RadioGroup;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.examples.Demo.SimpleDemo;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;

public class LineChartTest {

    public static class LineChartDemo extends SimpleDemo {

        LineChart lc;
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

            lc = new LineChart(lc_bounds);
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


    /** chart sizes: the size argument at start, and the keys t / s / m / l while running */
    private static final String[][] SIZE_KEYS = { {"t", "TINY"}, {"s", "SMALL"}, {"m", "MEDIUM"}, {"l", "LARGE"} };

    /**
     * Line chart demo: five series plotted with Braille characters, redrawn 10 times a second.
     * Keys 1..5 (or clicking the buttons below the chart) switch a series on / off;
     * t / s / m / l make the chart tiny / small / medium / large (full window). Ctrl-C quits.
     *
     * usage: LineChartTest [tiny | small | medium]   (default: large)
     *
     * (Rewritten Oct 2026 to use TUIApp - it used to build the screen, event bus, focus manager,
     * mouse handling and resize handling by hand.)
     */
    public static void main(String[] args) {
        String startSize = args.length > 0 ? args[0].toUpperCase() : "LARGE";

        new TUIApp(args)
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp) -> {

            Composite appPanel = (Composite) tApp.getMainApplicationPanel();
            String[] size = { startSize };

            LineChartDemo lineChartDemo = new LineChartDemo();
            lineChartDemo.setUpDemo(tApp.getEventBus(), chartBounds(size[0], appPanel));
            lineChartDemo.setFlushDemoOutput(() -> tApp.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(lineChartDemo)));
            appPanel.add(lineChartDemo.getLineChartPanelComponent());

            appPanel.setOnResize((newSize) -> lineChartDemo.resize(chartBounds(size[0], appPanel)));

            for (String[] sizeKey : SIZE_KEYS) {
                tApp.getEventBus().appendEventHandler(new KeyInputEventHandler(
                        (te) -> {
                            size[0] = sizeKey[1];
                            tApp.clearCanvas();
                            lineChartDemo.resize(chartBounds(size[0], appPanel));
                            appPanel.markAsDirty();
                        },
                        new KeyStroke(sizeKey[0].charAt(0)), new KeyStroke(sizeKey[0].toUpperCase().charAt(0))));
            }
        }).start();
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
        return new Rectangle.Simple(0, 0, panel.getHeight(), panel.getWidth());
    }

}
