package dtb.terminalIO.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.chart.LineChart;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.RockerButton;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventBusImpl;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.OnResizeEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.TerminalEvent.TYPE;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.InputEventsProducer;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.MouseEventOrchestrator;
import dtb.terminalIO.events.resize.TerminalResizeDetection;
import dtb.terminalIO.events.resize.TerminalResizeEventGenerator;
import dtb.terminalIO.examples.Demo.SimpleDemo;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Stty;

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
            return panel;
        }

        @Override
        public void setUpDemo(EventHandlerContainer eventBus, Rectangle drawingArea) {

            if (drawingArea == null) {
                throw new NullPointerException("drawingArea parameter null!");
            }

            this.panel = new VerticalPanel().setVerticalSpacer(0);

            this.lineChartSize = (size) -> new Dimension.Simple(size.getHeight(), size.getWidth());
            
            Dimension lc_bounds = calc_lc_bounds(this.lineChartSize.apply(drawingArea));

            lc = new LineChart(lc_bounds);
            lc.setBorder(true);
            lc.setPlotColor(new Colors.FGBGColors(Colors.IYel));
            lc.setSeriesColor("cos", Colors.Blu);
            lc.setTitle(" Line Chart Demo's ");

            menuButtonBar = new ButtonBar(Colors.BIBla);
            menuButtonBar.setNoBorderChars();

            panel.add(lc);
            panel.add(menuButtonBar);

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
                RockerButton seriesRockerButton = new RockerButton(seriesDemo.seriesColor, seriesDemo.name, seriesDemo.enabled);
                if (seriesDemo.onKeyStroke != null) {
                    seriesRockerButton.setHotKey(seriesDemo.onKeyStroke);
                }
                seriesRockerButton.onChange((te) -> {
                    RockerButton currentRockerButton = (RockerButton) te.getSource();
                    seriesDemo.enabled = currentRockerButton.getValue();
                });
                menuButtonBar.addComponent(seriesRockerButton);
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
            addAndRememberEventHandler(eventBus, menuButtonBar);
            addAndRememberEventHandler(eventBus, idleHandler);

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


    private static void bootDemo(String[] args) throws Exception {

        String _style = "MAX";

        if (args != null && args.length > 0 && args[0].equalsIgnoreCase("small")) {
            _style = "SMALL";
        }
        if (args != null && args.length > 0 && args[0].equalsIgnoreCase("medium")) {
            _style = "MEDIUM";
        }
        if (args != null && args.length > 0 && args[0].equalsIgnoreCase("tiny")) {
            _style = "TINY";
        }
        String style = _style;

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
        master_canvas.setEmptyChar(' ');

        Composite mainPanel = new Composite().setBounds(master_canvas);

        LineChartDemo lineChartDemo = new LineChartDemo() {
            public void flushOutput() {
                master_canvas.addComponentSegments(mainPanel);
                master_canvas.refresh();
            }
        };

        Rectangle bounds = makeBoundsFromStyle(style, master_canvas);

        master_canvas.setSize(bounds.getHeight(), bounds.getWidth());

        screen.add(ControlCode.HIDE_CURSOR);
        // screen.refresh();
        // master_canvas.t(" Braille Characters used for plotting");
        master_canvas.t(" ");

        // by performing a refresh now ( before screen is set to RAW) we get the screen
        // to
        // scroll up
        master_canvas.refresh();

        EventBus eventBus = new EventBusImpl();
        InputEventsProducer keyInputEventsProducer = null;
        try {
            keyInputEventsProducer = new InputEventsProducer(eventBus);
        } catch (Exception e) {
            eventBus.stopConsuming();
            return;
        }

        mainPanel.add(new TextComponent("Braille Characters used for plotting"));

        lineChartDemo.setUpDemo(eventBus, mainPanel);
        mainPanel.add(0,1, lineChartDemo.getLineChartPanelComponent());


        // note:  need to build focusmanager after linechart demo added to panel - otherwise null focusmanager
        FocusManager mainFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(mainPanel, eventBus);
        eventBus.appendEventHandler(  mainFocusManager );
        
        MouseEventOrchestrator mOrch = new MouseEventOrchestrator(screen, master_canvas, eventBus);
        
        mOrch.enableMouseHandling(mainPanel);
        
        master_canvas.refresh();

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

        TestsCommon.addNoGraphicsToggleEvent(eventBus, screen);

        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));
        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));
        eventBus.appendEventHandler(new ConditionalEventHandler.Impl(new IS_Event_TYPE(TYPE.EXCEPTION), terminate));

        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_R_KEYSTROKE, new EventHandler() {
            @Override
            public void handle(TerminalEvent<?> te) {
                master_canvas.clearCanvas();
                master_canvas.addComponentSegments(mainPanel);
                master_canvas.refresh();
                new TerminalResizeEventGenerator(screen, eventBus).run();
            }
        }));

        if (style.equals("MAX")) {
            TerminalResizeDetection.initiate(screen, eventBus);

            eventBus.appendEventHandler(new OnResizeEventHandler() {
                @Override
                public void handleResize(Dimension size) {
                    master_canvas.clearCanvas();
                    master_canvas.refresh();

                    master_canvas.setSize(size.getHeight(), size.getWidth());
                    master_canvas.clearCanvas();

                    lineChartDemo.resize(size);
                    mainPanel.resize(size);
                    master_canvas.addComponentSegments(mainPanel);
                    master_canvas.refresh();

                }
            });

            // Set styles
            Consumer<String> resizeChartOnStyle = (styleOnHotKey) -> {
                Rectangle newBounds = makeBoundsFromStyle(styleOnHotKey, master_canvas);

                //lineChartDemo.resize(newBounds);
                //panel.resize(size);

                Position lc_Pos = bounds.centered(lineChartDemo.panel, Position.ZERO);
                Rectangle rectToRemove = new Rectangle.Simple(lineChartDemo.lc);
                rectToRemove.moveDelta(lc_Pos.getRow(), lc_Pos.getCol());

                master_canvas.getSegmentsBuffer().removeRect(rectToRemove);

                master_canvas.clearCanvas();
                lineChartDemo.resize(newBounds);
            };

            eventBus.appendEventHandler(new KeyInputEventHandler(new KeyStroke('s'), new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    resizeChartOnStyle.accept("SMALL");
                }
            }));

            eventBus.appendEventHandler(new KeyInputEventHandler(new KeyStroke('S'), new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    resizeChartOnStyle.accept("SMALL");
                }
            }));

            eventBus.appendEventHandler(new KeyInputEventHandler(new KeyStroke('m'), new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    resizeChartOnStyle.accept("MEDIUM");
                }
            }));

            eventBus.appendEventHandler(new KeyInputEventHandler(new KeyStroke('m'), new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    resizeChartOnStyle.accept("MEDIUM");
                }
            }));

            eventBus.appendEventHandler(new KeyInputEventHandler(new KeyStroke('t'), new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    resizeChartOnStyle.accept("TINY");
                }
            }));

            eventBus.appendEventHandler(new KeyInputEventHandler(new KeyStroke('T'), new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    resizeChartOnStyle.accept("TINY");
                }
            }));

            eventBus.appendEventHandler(new KeyInputEventHandler(new KeyStroke('l'), new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    resizeChartOnStyle.accept("LARGE");
                }
            }));

            eventBus.appendEventHandler(new KeyInputEventHandler(new KeyStroke('L'), new EventHandler() {
                @Override
                public void handle(TerminalEvent<?> te) {
                    resizeChartOnStyle.accept("LARGE");
                }
            }));

        }

    }

    private static Rectangle makeBoundsFromStyle(String style, Canvas master_canvas) {
        Rectangle bounds = null;
        if ("SMALL".equalsIgnoreCase(style)) {
            bounds = new Rectangle.Simple(0, 0, Math.min(20, master_canvas.getHeight()),
                    Math.min(100, master_canvas.getWidth()));
        } else if ("MEDIUM".equalsIgnoreCase(style)) {
            bounds = new Rectangle.Simple(0, 0, Math.min(25, master_canvas.getHeight()),
                    Math.min(100, master_canvas.getWidth()));
        } else if ("TINY".equalsIgnoreCase(style)) {
            bounds = new Rectangle.Simple(0, 0, Math.min(15, master_canvas.getHeight()),
                    Math.min(60, master_canvas.getWidth()));
        } else {
            bounds = new Rectangle.Simple(master_canvas);
        }
        return bounds;
    }

    public static void main(String[] args) throws Exception {
        try {
            bootDemo(args);
        } catch (Exception e) {
            Stty.setCooked();
            e.printStackTrace();
            System.exit(1);
        }
    }

}
