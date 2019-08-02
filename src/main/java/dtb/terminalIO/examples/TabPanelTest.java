package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.tab.TabPanel;
import dtb.terminalIO.components.tab.TabPanel.TabAction;
import dtb.terminalIO.components.tab.TabPanel.TabContext;
import dtb.terminalIO.controlcodes.Colors;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.examples.BarChartTest.BarChartDemo;
import dtb.terminalIO.examples.GridBrailleTest.BrailleDemo;
import dtb.terminalIO.examples.LineChartTest.LineChartDemo;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;

public class TabPanelTest {

    static class DemoInTab {
        Demo demo;
        boolean firstSetup = false;
        Function<TabContext, Component<?>> componentSupplier;

        public DemoInTab(Demo demo, Function<TabContext, Component<?>> component) {
            this.demo = demo;
            this.componentSupplier = component;
        }

        TabAction getTabAction(KeyStroke hotKey, String tabText) {
            return new TabAction(hotKey, tabText) {

                EventHandlerContainer.Simple demoInTabEventHandlers = new EventHandlerContainer.Simple();

                public void resize(TabContext tabContext) {
                    demo.resize(tabContext.getBounds());
                }

                public void activateComponent(TerminalEvent<?> te, TabContext tabContext) {
                    if (!firstSetup) {
                        firstSetup = true;
                        demo.setUpDemo(demoInTabEventHandlers, tabContext.getBounds(),
                                componentSupplier.apply(tabContext));
                    }
                    // setupDemo populated demoInTabEventHandlers with eventHandlers specific to the
                    // demo in the tab
                    // now let's copy those to the actual eventBus
                    demoInTabEventHandlers.prependTo(te.getEventBus());
                }

                @Override
                public void deactivateComponent(TerminalEvent<?> te, TabContext tabContext) {
                    // demo.tearDownDemo(te.getEventBus());
                    // on Tab deactivate, we will disable (remove) all eventHandlers related to the
                    // demo
                    demoInTabEventHandlers.removeFrom(te.getEventBus());
                }

                @Override
                public Component<?> getComponent(TabContext tabContext) {
                    return componentSupplier.apply(tabContext);
                }
            };
        }

    }

    public static void main(String args[]) throws Exception {

        TUIApp tabPanelDemoApp = new TUIApp();

        tabPanelDemoApp.initFromArgs(args);

        char emptyChar = '░';
        Color emptyColor = new FGBGColors(Colors.IYel, Colors.On_IBlu);
        tabPanelDemoApp.getCanvas().setEmptyAttributes(emptyColor, emptyChar);

        Composite topFrame = new Composite();
        topFrame.setEmptyChar(emptyChar);
        topFrame.setEmptyColor(emptyColor);

        // Dimension d_80 = acmeLOGOfileDimension(80);
        Dimension d_100 = acmeLOGOfileDimension(100);
        Dimension d_150 = acmeLOGOfileDimension(150);
        Dimension d_200 = acmeLOGOfileDimension(200);

        Dimension sizeOfTabPanelWindow = new Dimension() {
            @Override
            public int getWidth() {
                return tabPanelDemoApp.getCanvas().getWidth() - 4;
            }

            @Override
            public int getHeight() {
                return tabPanelDemoApp.getCanvas().getHeight() - 8;
            }
        };

        Dimension sizeOfDebugWindow = new Dimension() {
            @Override
            public int getWidth() {
                return tabPanelDemoApp.getCanvas().getWidth() - 4;
            }

            @Override
            public int getHeight() {
                return 5;
            }
        };

        VerticalPanel vPanel = new VerticalPanel();
        vPanel.setEmptyChar(emptyChar);
        vPanel.setEmptyColor(emptyColor);
        vPanel.setVerticalSpacer(1);
        topFrame.add(vPanel);

        TabPanel tb = new TabPanel(ControlCode.DEFAULT_COLOR, new FGBGColors(Colors.Blu).bold(), sizeOfTabPanelWindow);
        tb.setBorder(false);
        tb.setEmptyChar(emptyChar);
        tb.setEmptyColor(emptyColor);
        vPanel.add(tb);

        TextComponent debugSTATs = new TextComponent(Colors.IYel, "");
        Composite debugWindow = new Composite(Colors.IGre).setBounds(sizeOfDebugWindow);
        debugWindow.setTitle(new FGBGColors(Colors.BYel, Colors.On_Gre), "DEBUG");
        debugWindow.add(debugSTATs);
        vPanel.add(debugWindow);

        AtomicInteger counter = new AtomicInteger(0);

        tabPanelDemoApp.setUpdateCanvas((tApp) -> {
            if (!topFrame.isDirty()) {
                return false; // false means no GUI change to update
            }

            tApp.getCanvas().addComponentSegments(1,2,topFrame);

            return true;
        });

        tb.addTab(new TabAction(Keys.F5_KEYSTROKE, "Acme Demo") {

            TextComponent tc;

            public void resize(TabContext tabCtx) {
                int fileID = 80;
                if (tabCtx.getBounds().contains(d_200)) {
                    fileID = 200;
                } else if (tabCtx.getBounds().contains(d_150)) {
                    fileID = 150;
                } else if (tabCtx.getBounds().contains(d_100)) {
                    fileID = 100;
                }

                String imgText;
                try {
                    imgText = acmeLOGO(fileID);
                } catch (IOException e) {
                    imgText = "Failed to read file.  Exception " + e;
                }

                ((TextComponent) getComponent(tabCtx)).setTextAndResize(imgText);
            }

            @Override
            public void activateComponent(TerminalEvent<?> te, TabContext tabCtx) {
                resize(tabCtx);
            }

            @Override
            public void deactivateComponent(TerminalEvent<?> te, TabContext tabCtx) {
                // nothing to do ..
            }

            @Override
            public Component<?> getComponent(TabContext tabCtx) {
                if (this.tc == null) {
                    tc = new TextComponent(tabCtx.getColor(), "ACME LOGO");
                }
                return tc;
            }

        });

        BarChartDemo barChartDemo = new BarChartDemo();
        tb.addTab(new DemoInTab(barChartDemo, (tc) -> barChartDemo.getBarChart(tc.getBounds()))
                .getTabAction(Keys.F6_KEYSTROKE, "BarChart Example"));

        LineChartDemo lineChartDemo = new LineChartDemo();
        tb.addTab(new DemoInTab(lineChartDemo, (tc) -> lineChartDemo.getLineChart()).getTabAction(Keys.F7_KEYSTROKE,
                "LineChart Example"));

        BrailleDemo brailleDemo = new BrailleDemo();
        tb.addTab(new DemoInTab(brailleDemo, (tc) -> brailleDemo.getBrailleComponent()).getTabAction(Keys.F8_KEYSTROKE,
                "Braille Character Graphics Demo"));

        tabPanelDemoApp.setOnResizeAction((tApp, size) -> {
            tApp.getCanvas().setSize(size.getHeight(), size.getWidth());
            tApp.getCanvas().clearCanvas();
            tb.resize(sizeOfTabPanelWindow);
            debugWindow.resize(sizeOfDebugWindow);
        });

        tabPanelDemoApp.setOnReadyStart((tApp) -> {
            tApp.setMainApplicationPanel(topFrame);
            tApp.confirmOnExit();

            tApp.getEventBus().appendEventHandler(tb);

            // select first tab once eventBus is rdy to go
            tApp.getEventBus().appendEventHandler(
                    new OnIdleEventEventHandler.OneTime((te) -> tb.selectAsActive(te, Keys.F5_KEYSTROKE)));

            tApp.getEventBus().appendEventHandler(new OnIdleEventEventHandler.RepeatingInterval(100, (te) -> {
                        int count = counter.incrementAndGet();
                        debugSTATs.setTextAndResize(
                                "IDLE task invoke counter = " + count + 
                                "   \n" + tApp.getEventBus().statsAsStr() + 
                                "\n" + 
                                "Text segments on screen: " + tApp.getCanvas().segmentsSize() + "  ");
        
                        te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(tApp));
            }));

        });

        tabPanelDemoApp.start();

    }

    static private String acmeLOGOFileName(int sizeInName) {
        String fileName = "acme-white-" + sizeInName + ".txt";
        return fileName;
    }

    static private Dimension acmeLOGOfileDimension(int sizeInName) throws IOException {
        return Dimension.UTIL.stringToDimension(ReadSampleFiles.readSampleFile(acmeLOGOFileName(sizeInName), true));
    }

    static private String acmeLOGO(int sizeInName) throws IOException {
        return ReadSampleFiles.readSampleFile(acmeLOGOFileName(sizeInName), false);
    }

}
