package dtb.terminalIO.examples;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentInspector;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.tab.TabAction;
import dtb.terminalIO.components.tab.TabPanel;
import dtb.terminalIO.components.tab.TabPanel.TabContext;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventBusImpl;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.examples.BarChartTest.BarChartRandomDataDemo;
import dtb.terminalIO.examples.GridBrailleTest.BrailleDemo;
import dtb.terminalIO.examples.LineChartTest.LineChartDemo;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;

public class TabPanelTest {

    static class DemoInTab {
        Demo demo;
        boolean firstSetup = false;
        Function<TabContext, Component<?>> retrieveComponentForTabFunction;

        public DemoInTab(Demo demo, Function<TabContext, Component<?>> retrieveComponentFunction) {
            this.demo = demo;
            this.retrieveComponentForTabFunction = retrieveComponentFunction;
        }

        TabAction<Component<?>> getTabAction() {
            return new TabAction<Component<?>>() {

                EventHandlerContainer.Simple demoInTabEventHandlers = new EventHandlerContainer.Simple();

                public void resize(TabContext tabContext) {
                    demo.resize(tabContext.getBounds());
                }

                @Override
                public void activateComponent(EventBus eventBus, TabContext tabContext, Component<?> component) {
                    if (!firstSetup) {
                        firstSetup = true;
                        demo.setUpDemo(demoInTabEventHandlers, tabContext.getBounds() );
                    }
                    // setupDemo populated demoInTabEventHandlers with eventHandlers specific to the
                    // demo in the tab
                    // now let's copy those to the actual eventBus
                    demoInTabEventHandlers.prependTo(eventBus);
                }

                @Override
                public void deactivateComponent(EventBus eventBus, TabContext tabContext, Component<?> component) {
                    // demo.tearDownDemo(te.getEventBus());
                    // on Tab deactivate, we will disable (remove) all eventHandlers related to the
                    // demo
                    demoInTabEventHandlers.removeFrom(eventBus);
                }

                @Override
                public Component<?> getComponent(TabContext tabContext) {
                    return retrieveComponentForTabFunction.apply(tabContext);
                }
            };
        }

    }

    public static void main(String args[]) throws Exception {

        TUIApp tabPanelDemoApp = new TUIApp();

        tabPanelDemoApp.initFromArgs(args);

        char emptyChar = '░';
        Color emptyColor = new Colors.FGBGColors(Colors.IYel, Colors.On_IBlu);
       // tabPanelDemoApp.getCanvas().setEmptyAttributes(emptyColor, emptyChar);

        Composite topFrame = new Composite();
        topFrame.setEmptyChar(emptyChar);
        topFrame.setEmptyColor(emptyColor);
        topFrame.setFillOnClear(false);

        // Dimension d_80 = acmeLOGOfileDimension(80);
        Dimension d_100 = acmeLOGOfileDimension(100);
        Dimension d_150 = acmeLOGOfileDimension(150);
        Dimension d_200 = acmeLOGOfileDimension(200);

        Dimension sizeOfTabPanelWindow = new Dimension() {
            @Override
            public int getWidth() {
                int onethird = tabPanelDemoApp.getCanvas().getWidth() / 3;
                return tabPanelDemoApp.getCanvas().getWidth() - onethird;
            }

            @Override
            public int getHeight() {
                return tabPanelDemoApp.getCanvas().getHeight() - 8;
            }
        };

        Dimension sizeOfInspectorWindow = new Dimension() {
            @Override
            public int getWidth() {
                return tabPanelDemoApp.getCanvas().getWidth() - (sizeOfTabPanelWindow.getWidth() + 6);
            }

            @Override
            public int getHeight() {
                return sizeOfTabPanelWindow.getHeight();
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
        vPanel.setFillOnClear(false);
        vPanel.setVerticalSpacer(1);
        topFrame.add(vPanel);

        TabPanel tb = new TabPanel(Colors.DEFAULT_COLOR, new Colors.FGBGColors(Colors.Blu).bold(), sizeOfTabPanelWindow);
        tb.setEmptyChar(emptyChar);
        tb.setEmptyColor(emptyColor);
        tb.setFillOnClear(false);
        tb.setPrevKeyStroke(Keys.CTRL_LEFT_KEYSTROKE);
        tb.setNextKeyStroke(Keys.CTRL_RIGHT_KEYSTROKE);

        HorizontalPanel hp = new HorizontalPanel();
        hp.add(tb);

        vPanel.add(hp);

        DefaultFocusManager focusManager = new DefaultFocusManager(Keys.LEFT_KEYSTROKE, Keys.RIGHT_KEYSTROKE);

        ComponentInspector inspector = new ComponentInspector(Colors.BIGre, sizeOfInspectorWindow, vPanel, focusManager);

        hp.add(inspector);

        focusManager.addFocusable(tb);
        focusManager.addComponentAsFocusables(inspector);

        TextComponent debugSTATs = new TextComponent(Colors.IYel, "");
        Composite debugWindow = new Composite(Colors.IGre).setBounds(sizeOfDebugWindow);
        debugWindow.setTitle(new Colors.FGBGColors(Colors.BYel, Colors.On_Gre), "DEBUG");
        debugWindow.add(debugSTATs);
        vPanel.add(debugWindow);

        AtomicInteger counter = new AtomicInteger(0);

        tabPanelDemoApp.enableMouseHandling();

        tabPanelDemoApp.setUpdateCanvas((tApp) -> {
            if (!topFrame.isDirty()) {
                return false; // false means no GUI change to update
            }

            tApp.clearCanvas();
            tApp.getCanvas().addComponentSegments(1,2,topFrame);

            return true;
        });

        tb.addTab("Acme Demo", Keys.F5_KEYSTROKE, new TabAction<Composite>() {

            TextComponent tc;
            Composite panel;

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

                // hack to ensure component is built
                getComponentForTab(tabCtx);
                tc.setTextAndResize(imgText);
                panel.resize(tabCtx.getBounds());
                panel.moveChildTo(panel.centered(tc, Position.ZERO), tc);
            }

            @Override
            public Composite getComponent(TabContext tabCtx) {
                if (this.tc == null) {
                    panel = new Composite(tabCtx.getColor()).setBounds( tabCtx.getBounds());
                    tc = new TextComponent(tabCtx.getColor(), "ACME LOGO");
                    panel.add(tc);
                }
                return panel;
            }

        });

        tb.addTab("BarChart Example11", Keys.F5_KEYSTROKE,  new TabAction<BarChartRandomDataDemo>() {
            @Override
            public BarChartRandomDataDemo getComponent(TabContext tabContext) {
                return new BarChartRandomDataDemo(tabContext.getBounds());
            }
            public void activateComponent(EventBus eventBus, TabContext tabContext, BarChartRandomDataDemo barChartDemo) {
                eventBus.appendIdleEventHandler( barChartDemo.getDrawChartTimedEventHandler()  );
            }

            public void deactivateComponent(EventBus eventBus, TabContext tabContext, BarChartRandomDataDemo barChartDemo) {
                eventBus.removeEventHandler( barChartDemo.getDrawChartTimedEventHandler()  );
            }
        });


        LineChartDemo lineChartDemo = new LineChartDemo();
        tb.addTab("LineChart Example", 
                  Keys.F7_KEYSTROKE,
                  new DemoInTab(lineChartDemo, 
                                (tc) -> lineChartDemo.getLineChartPanelComponent()).getTabAction());

        BrailleDemo brailleDemo = new BrailleDemo();
        tb.addTab("Braille Character Graphics Demo", 
                  Keys.F8_KEYSTROKE,
                  new DemoInTab(brailleDemo, 
                                (tc) -> brailleDemo.getBrailleComponent()).getTabAction());

        tabPanelDemoApp.setOnResizeAction((tApp, size) -> {
            tApp.getCanvas().setSize(size.getHeight(), size.getWidth());
            tApp.getCanvas().clearCanvas();
            tb.resize(sizeOfTabPanelWindow);
            inspector.resize(sizeOfInspectorWindow);
            hp.fullrecalcDim();
            vPanel.fullrecalcDim();
            debugWindow.resize(sizeOfDebugWindow);
        });

        tabPanelDemoApp.setMainApplicationPanel(topFrame);
        tabPanelDemoApp.confirmOnExit();

        tabPanelDemoApp.setOnReadyStart((tApp) -> {

            tApp.getEventBus().appendEventHandler(inspector.refreshEventHandler());

            tApp.getEventBus().appendEventHandler(inspector.toggleViewEventHandler());

            tApp.getEventBus().appendEventHandler(focusManager);

            // select first tab once eventBus is rdy to go
            tApp.getEventBus().appendEventHandler(
                    new OnIdleEventEventHandler.OneTime((te) ->  te.getEventBus().raiseEvent(new KeyEvent(tApp, Keys.F5_KEYSTROKE)) ));

            tApp.getEventBus().appendEventHandler(new OnIdleEventEventHandler.RepeatingInterval(100, (te) -> {
                        int count = counter.incrementAndGet();
                        debugSTATs.setTextAndResize(
                                "IDLE task invoke counter = " + count + 
                                "   \n" + ((EventBusImpl)tApp.getEventBus()).statsAsStr() + 
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
