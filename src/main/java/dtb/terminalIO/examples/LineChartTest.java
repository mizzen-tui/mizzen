package dtb.terminalIO.examples;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.chart.LineChart;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
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
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.resize.TerminalResizeDetection;
import dtb.terminalIO.events.resize.TerminalResizeEventGenerator;
import dtb.terminalIO.examples.Demo.SimpleDemo;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.Stty;
import dtb.terminalIO.util.TerminalDimensions;

public class LineChartTest {

    public static class LineChartDemo extends SimpleDemo {

        LineChart lc;
        TextComponent menuOoptions;

        Runnable doRenderLineChart = null;

        public boolean pauseOnIdle = false;

        @Override
        public void resize(Dimension size) {

//            Position lc_Pos = bounds.centered(lineChartDemo.lc, Position.ZERO);
//            Rectangle rectToRemove = new Rectangle.Simple(lineChartDemo.lc);
//            rectToRemove.moveDelta(lc_Pos.getRow(), lc_Pos.getCol());
//            master_canvas.getSegmentsBuffer().removeRect(rectToRemove);

            Dimension lc_bounds = calc_lc_bounds(size);
            lc.resize(lc_bounds);
            menuOoptions.resize(new Dimension.Simple(1, lc_bounds.getWidth()));

            if (doRenderLineChart != null) {
                doRenderLineChart.run();
            }
        }

        private Dimension calc_lc_bounds(Dimension containingDimension) {
            return new Dimension.Simple(containingDimension.getHeight() - 5, containingDimension.getWidth() - 4);
        }

        public LineChart getLineChart() {
            return lc;
        }
        
        @Override
        public void setUpDemo(EventHandlerContainer eventBus, Rectangle bounds, IScreenLineSegmentsBuffer.Provider segmentsBufferProvider) {

            Dimension lc_bounds = calc_lc_bounds(bounds);

            lc = new LineChart(lc_bounds);
            lc.setBorder(true);
            lc.setPlotColor(new FGBGColors(Colors.IYel));
            lc.setSeriesColor("cos", Colors.Blu);
            lc.setTitle(" Line Chart Demo's ");

            menuOoptions = new TextComponent(FGBGColors.NO_COLOR_SPECIFIED,
                    new Dimension.Simple(1, lc_bounds.getWidth()), " ");
            menuOoptions.setEmptyChar('*');

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

                            StringBuffer menuText = new StringBuffer();

                            String noCOLORStr = ControlCode.RCol.asString();
                            String brCOLOR = Colors.BIBla.asString();

                            for (SeriesDemo seriesDemo : seriesDemos) {

                                if (seriesDemo.enabled) {

                                    lc.setSeriesColor(seriesDemo.name, seriesDemo.seriesColor);
                                    seriesDemo.dataAdder.accept(j);

                                }
                                if (seriesDemo.onKeyStroke != null) {

                                    Color onCOLOR = (seriesDemo.enabled) ? Colors.BIWhi : Colors.Whi;
                                    Color offCOLOR = (seriesDemo.enabled) ? Colors.Whi : Colors.BIWhi;

                                    String onLeftBr = (seriesDemo.enabled) ? brCOLOR + "[" + noCOLORStr : " ";
                                    String onRightBr = (seriesDemo.enabled) ? brCOLOR + "]" + noCOLORStr : " ";
                                    String offLeftBr = (seriesDemo.enabled) ? " " : brCOLOR + "[" + noCOLORStr;
                                    String offRightBr = (seriesDemo.enabled) ? " " : brCOLOR + "]" + noCOLORStr;

                                    String seriesHelp = new FGBGColors(Colors.BIWhi).asString()
                                            + seriesDemo.onKeyStroke.getName() + ControlCode.RCol.asString() + ": "
                                            + seriesDemo.seriesColor.asString() + seriesDemo.name
                                            + ControlCode.RCol.asString() + " " + onLeftBr + onCOLOR.asString() + "ON"
                                            + noCOLORStr + onRightBr + " / " + offLeftBr + offCOLOR.asString() + "OFF"
                                            + noCOLORStr + offRightBr;

                                    menuText.append(seriesHelp);
                                    menuText.append("      ");
                                }

                            }

                            menuOoptions.setText(menuText.toString());

                            if (segmentsBufferProvider != null) {

                                // Position lc_Pos = bounds.centered(lc, Position.ZERO);
                                Position lc_Pos = new Position.Simple(1, 1);

                                segmentsBufferProvider.getSegmentsBuffer().mergeAt(lc_Pos, lc.getSegmentsBuffer());

                                if (segmentsBufferProvider instanceof Rectangle) {
                                    segmentsBufferProvider.getSegmentsBuffer().mergeAtBounded(
                                            lc_Pos.offsetAdd(lc.getHeight() + 1, 0), (Rectangle) segmentsBufferProvider,
                                            menuOoptions.getSegmentsBuffer());
                                } else {
                                    segmentsBufferProvider.getSegmentsBuffer().mergeAt(
                                            lc_Pos.offsetAdd(lc.getHeight() + 1, 0), menuOoptions.getSegmentsBuffer());
                                }

//	                            if (segmentsBufferProvider instanceof Component) {
//	                                ((Component)segmentsBufferProvider).markAsDirty();
//	                            }

                            }
                            // refresh/flush segments to output

                            flushOutput();
                            j++;
                        }
                    });

            // idleHandler.handle(null);
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

    private static void bootDemoManualResize(String[] args) throws Exception {
        // System.out.println("Hello world");

        TerminalScreen screen = TestsCommon.getTerminalScreenForTesting(args);

        Canvas master_canvas = TestsCommon.getMasterCanvasForTesting(screen);
        // master_canvas.setEmptyChar(' ');

        LineChartDemo lineChartDemo = new LineChartDemo() {
            public void flushOutput() {
                // master_canvas.t(0,0,"Braille Characters used for plotting. MODE :"
                // + style + " ");
                master_canvas.refresh();
            }
        };

        screen.add(ControlCode.HIDE_CURSOR);
        // screen.refresh();
        // master_canvas.t(" Braille Characters used for plotting");
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

        lineChartDemo.setUpDemo(eventBus, new Rectangle.Simple(master_canvas), master_canvas);

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

        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_A_KEYSTROKE, (e) -> {
            lineChartDemo.pauseOnIdle = !lineChartDemo.pauseOnIdle;
            // System.out.println("idle = " + lineChartDemo.pauseOnIdle);
            // terminate.handle(e);
        }));

        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_R_KEYSTROKE, (e) -> {
            TerminalDimensions size = TerminalDimensions.create();
            master_canvas.clearCanvas();
            master_canvas.refresh();
            master_canvas.setSize(size.getHeight() - 1, size.getWidth() - 1);
            master_canvas.clearCanvas();
            master_canvas.refresh();
            lineChartDemo.resize(master_canvas);

            // master_canvas.t(10,10,"TEST 123");

//	    master_canvas.refresh();
//
//	    master_canvas.t(10,10,"TEST ABC");

//	    master_canvas.refresh();
            lineChartDemo.flushOutput();

            // System.out.println("idle = " + lineChartDemo.pauseOnIdle);
            // terminate.handle(e);
        }));

        // eventBus.addEventHandler(new KeyInputEventHandler(Keys.CTRL_A_KEYSTROKE,
        // (e)-> )) ;

        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.CTRL_C_KEYSTROKE, terminate));
        eventBus.appendEventHandler(new KeyInputEventHandler(Keys.ESC_KEYSTROKE, terminate));
        eventBus.appendEventHandler(new ConditionalEventHandler.Impl(new IS_Event_TYPE(TYPE.EXCEPTION), terminate));

        TestsCommon.addNoGraphicsToggleEvent(eventBus, screen);

    }

    private static void bootDemo(String[] args) throws Exception {

        String _style = "MAX";

        if (args != null && args.length > 0 && args[0].equalsIgnoreCase("manual")) {
            bootDemoManualResize(args);
            return;
        }

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

        LineChartDemo lineChartDemo = new LineChartDemo() {
            public void flushOutput() {
                // master_canvas.t(0,0,"Braille Characters used for plotting. MODE :"
                // + style + " ");
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

        lineChartDemo.setUpDemo(eventBus, bounds, master_canvas);

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
                master_canvas.refresh();
                new TerminalResizeEventGenerator(screen, eventBus).run();
            }
        }));

        if (style.equals("MAX")) {
            TerminalResizeDetection.initiate(screen, eventBus);

            eventBus.appendEventHandler(new OnResizeEventHandler() {
                @Override
                public void handleResize(Dimension size) {
                    Logging.info("DEMO: resize " + size);
                    master_canvas.clearCanvas();
                    master_canvas.refresh();

                    master_canvas.setSize(size.getHeight(), size.getWidth());
                    master_canvas.clearCanvas();
                    master_canvas.refresh();
                    lineChartDemo.resize(master_canvas);

                }
            });

            // Set styles
            Consumer<String> resizeChartOnStyle = (styleOnHotKey) -> {
                Rectangle newBounds = makeBoundsFromStyle(styleOnHotKey, master_canvas);

                Position lc_Pos = bounds.centered(lineChartDemo.lc, Position.ZERO);
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
