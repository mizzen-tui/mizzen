package dtb.terminalIO.examples;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.imageio.ImageIO;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentInspector;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.plot.image.BufferedImageProvider;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import dtb.terminalIO.components.plot.image.ResizableImageViewer;
import dtb.terminalIO.components.plot.image.animate.ImageAnimatorComponent;
import dtb.terminalIO.components.plot.image.animate.ImageSplitter;
import dtb.terminalIO.components.tab.TabAction;
import dtb.terminalIO.components.tab.TabPanel;
import dtb.terminalIO.components.tab.TabPanel.TabContext;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventBusImpl;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.examples.BarChartTest.BarChartRandomDataDemo;
import dtb.terminalIO.examples.GridBrailleTest.BrailleDemo;
import dtb.terminalIO.examples.LineChartTest.LineChartDemo;
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

        TUIApp tabPanelDemoApp = new TUIApp(args).setDefaultMainApplicationPanel();

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
        vPanel.setVerticalSpacer(1);
        tabPanelDemoApp.getMainApplicationPanel().add(vPanel);

        TabPanel tb = new TabPanel(Colors.BIYel, new Colors.FGBGColors(Colors.Blu).bold(), sizeOfTabPanelWindow);
        tb.setPrevKeyStroke(Keys.CTRL_LEFT_KEYSTROKE);
        tb.setNextKeyStroke(Keys.CTRL_RIGHT_KEYSTROKE);

        HorizontalPanel hp = new HorizontalPanel();
        hp.add(tb);

        vPanel.add(hp);

        TextComponent debugSTATs = new TextComponent(Colors.IYel, "");
        Composite debugWindow = new Composite(Colors.IGre).setBounds(sizeOfDebugWindow);
        debugWindow.setTitle(new Colors.FGBGColors(Colors.BYel, Colors.On_Gre), "DEBUG");
        debugWindow.add(debugSTATs);
        vPanel.add(debugWindow);

        AtomicInteger counter = new AtomicInteger(0);

        tb.addTab("Animation", Keys.F4_KEYSTROKE, new TabAction<VerticalPanel>() {
            ImageAnimatorComponent animatorComponent;
            @Override
            protected VerticalPanel getComponent(TabContext tabContext) {
                int IMAGEWIDTH=150;
                int IMAGEHEIGHT=70;

                                          //  0  10   150  10    300 10    450 10    0 10   150  10   300  10   450  10   0  105   150  105   300  105   450  105   0  200   150  200   300  200   450  200
                int[] offsetNbrs = new int[] {0, 10,  150, 10,  300, 10,  450, 10,  0, 10,  150, 10,  300, 10,  450, 10,  0, 105,  150, 105,  300, 105,  450, 105,  0, 200,  150, 200,  300, 200,  450, 200};
                List<Position> offsets = new ArrayList<Position>();
                for (int i=0; i< offsetNbrs.length; i+=2) {
                    offsets.add(new Position.Simple(offsetNbrs[i+1],offsetNbrs[i]));
                }
                ImageRenderRequest imageRequest = new ImageRenderRequest();
                imageRequest.setDesiredImgSizeInCharacters(tabContext.getBounds());
                imageRequest.setColorMap(ImageViewerFactory.rgbColorMapRCol);
                imageRequest.setToPlotOrNot(ImageViewerFactory.PLOT_ALWAYS);
                imageRequest.setImageRenderType(ImageViewerFactory.IMAGE_RENDER_TYPE.ASCII);
                Function<Dimension, Dimension> imgSizeCalculator = (size)-> new Dimension.Simple(tabContext.getBounds().getHeight()-2, tabContext.getBounds().getWidth());
                imageRequest.setDesiredImgSizeInCharacters(imgSizeCalculator.apply(tabContext.getBounds()));
                try {
                    BufferedImage img = ImageIO.read(this.getClass().getResource("/lionRunning.jpg"));
                    imageRequest.setImageToView(img);
                    ImageSplitter splitter = new ImageSplitter(imageRequest.getImageToView(), offsets, IMAGEHEIGHT, IMAGEWIDTH);
                    animatorComponent = new ImageAnimatorComponent(Colors.DEFAULT_COLOR, imageRequest, splitter, imageRequest.getDesiredImgSizeInCharacters());
                    VerticalPanel vp = new VerticalPanel();
                    vp.add(animatorComponent);
                    HorizontalPanel drawingControlSettings = new HorizontalPanel(Colors.BBlu);
                    vp.add(drawingControlSettings);
                    
                    DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE> imageRendererTypeDDLB = new DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE>(ImageViewerFactory.imageRenderTypeOptions());
                    imageRendererTypeDDLB.setSelected(0);
                    imageRendererTypeDDLB.onChange((te) -> {
                        imageRequest.setImageRenderType(imageRendererTypeDDLB.getValue());
                    });
                    drawingControlSettings.addText("Render Type:");
                    drawingControlSettings.add(imageRendererTypeDDLB);

                    DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode> colorMapDDLB = new DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode>(ImageViewerFactory.colorMap());
                    colorMapDDLB.setSelected(0);
                    colorMapDDLB.onChange((te)->{
                        imageRequest.setColorMap( colorMapDDLB.getValue()  );
                    });
                    drawingControlSettings.addText("Color Map:");
                    drawingControlSettings.add(colorMapDDLB);


                    DropdownListBox<ImageViewerFactory.RGBPlotPredicate> plotPredicatesDDLB = new DropdownListBox<ImageViewerFactory.RGBPlotPredicate>(ImageViewerFactory.plotPointPredicateOptions());
                    plotPredicatesDDLB.setSelected(0);
                    plotPredicatesDDLB.onChange((te)->{
                        imageRequest.setToPlotOrNot( plotPredicatesDDLB.getValue() );
                    });

                    drawingControlSettings.addText("Plot Condition:");
                    drawingControlSettings.add(plotPredicatesDDLB);

                    vp.appendOnResize((sizeOfPanel)->  animatorComponent.resize(imgSizeCalculator.apply(sizeOfPanel)) );
                    return vp;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            public void activateComponent(EventBus eventBus, TabContext tabContext, VerticalPanel vp) {
                animatorComponent.buildTimer(250);
                eventBus.appendIdleEventHandler( animatorComponent.getIdleEventEventHandler()  );
            }

            public void deactivateComponent(EventBus eventBus, TabContext tabContext, VerticalPanel vp) {
                eventBus.removeEventHandler( animatorComponent.getIdleEventEventHandler()  );
            }
        });

        tb.addTab("Image", Keys.F5_KEYSTROKE, new TabAction<Composite>() {

            ResizableImageViewer imageViewer;
            Function<Dimension, Dimension> imgSizeCalculator = (size)-> new Dimension.Simple(size.getHeight()-2, size.getWidth());
            boolean resizeRuleAdded = false;
            
            @Override
            protected Composite getComponent(TabContext tabContext) {
                VerticalPanel vp = new VerticalPanel();
                // "/lionRunning.jpg"
                // "/grannySmithAppleSingle.jpg"
                imageViewer = new ResizableImageViewer(Colors.DEFAULT_COLOR, imgSizeCalculator.apply(tabContext.getBounds()), new BufferedImageProvider.FromResource("/grannySmithAppleSingle.jpg") );
                vp.add(imageViewer);

                HorizontalPanel drawingControlSettings = new HorizontalPanel(Colors.BBlu);
                vp.add(drawingControlSettings);

                DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE> imageRendererTypeDDLB = new DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE>(ImageViewerFactory.imageRenderTypeOptions());
                imageRendererTypeDDLB.setSelected(0);
                imageRendererTypeDDLB.onChange((te) -> {
                    imageViewer.setImageRenderType(imageRendererTypeDDLB.getValue());
                    imageViewer.resizeImageAsynch(te.getEventBus(), new Dimension.Simple(imageViewer)); 
                });
                drawingControlSettings.addText("Render Type:");
                drawingControlSettings.add(imageRendererTypeDDLB);

                DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode> colorMapDDLB = new DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode>(ImageViewerFactory.colorMap());
                colorMapDDLB.setSelected(0);
                colorMapDDLB.onChange((te)->{
                    imageViewer.setColorMapping(colorMapDDLB.getValue()  );
                    imageViewer.resizeImageAsynch(te.getEventBus(), new Dimension.Simple(imageViewer)); 
                });
                drawingControlSettings.addText("Color Map:");
                drawingControlSettings.add(colorMapDDLB);

                DropdownListBox<ImageViewerFactory.RGBPlotPredicate> plotPredicatesDDLB = new DropdownListBox<ImageViewerFactory.RGBPlotPredicate>(ImageViewerFactory.plotPointPredicateOptions());
                plotPredicatesDDLB.setSelected(0);
                plotPredicatesDDLB.onChange((te)->{
                    imageViewer.setPlotPredicate( plotPredicatesDDLB.getValue() );
                    imageViewer.resizeImageAsynch(te.getEventBus(), new Dimension.Simple(imageViewer)); 
                });
                drawingControlSettings.addText("Plot Condition:");
                drawingControlSettings.add(plotPredicatesDDLB);

                return vp;
            }
            public void activateComponent(EventBus eventBus, TabContext tabContext, Composite vp) {
                imageViewer.resizeImageAsynch(eventBus, imgSizeCalculator.apply(tabContext.getBounds()));
                if (resizeRuleAdded) {
                    resizeRuleAdded = true;
                    vp.appendOnResize((sizeOfPanel)-> imageViewer.resizeImageAsynch(eventBus, imgSizeCalculator.apply(sizeOfPanel)));
                }
            }
        });

        tb.addTab("BarChart Example", Keys.F6_KEYSTROKE,  new TabAction<BarChartRandomDataDemo>() {
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


        tabPanelDemoApp.confirmOnExit();

        tabPanelDemoApp.setOnReadyStart((tApp) -> {

            // ComponentInspector inspector = new ComponentInspector(Colors.BIGre, sizeOfInspectorWindow, vPanel, focusManager);
            
//            ComponentInspector inspector = new ComponentInspector(Colors.IGre, sizeOfInspectorWindow, (Component<?>)tApp.getMainApplicationPanel(), tApp.getApplicationFocusManager());
//            
//            tApp.getEventBus().appendEventHandler(inspector.refreshEventHandler());
//
//            tApp.getEventBus().appendEventHandler(inspector.toggleViewEventHandler());

            tabPanelDemoApp.setOnResizeAction((tApp2, size) -> {
                tApp2.getCanvas().setSize(size.getHeight(), size.getWidth());
                tApp2.getCanvas().clearCanvas();
                tb.resize(sizeOfTabPanelWindow);
//                inspector.resize(sizeOfInspectorWindow);
                hp.fullrecalcDim();
                vPanel.fullrecalcDim();
                debugWindow.resize(sizeOfDebugWindow);
            });
            
            
            // select first tab once eventBus is rdy to go
//            tApp.getEventBus().appendEventHandler(
//                    new OnIdleEventEventHandler.OneTime((te) ->  te.getEventBus().raiseEvent(new KeyEvent(tApp, Keys.F5_KEYSTROKE)) ));

            tApp.getEventBus().appendEventHandler(new OnIdleEventEventHandler.RepeatingInterval(100, (te) -> {
                        int count = counter.incrementAndGet();
                        debugSTATs.setTextAndResize(
                                "IDLE task invoke counter = " + count + 
                                "   \n" + ((EventBusImpl)tApp.getEventBus()).statsAsStr() + 
                                "\n" + 
                                "Text segments on screen: " + tApp.getCanvas().segmentsSize() + "  ");
        
                        te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(tApp));
            }));
            
            tb.displayCurrentTab(tApp.getEventBus());


        });

        tabPanelDemoApp.start();

    }


}
