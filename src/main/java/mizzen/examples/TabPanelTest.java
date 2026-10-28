package mizzen.examples;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.imageio.ImageIO;

import mizzen.app.TUIApp;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.composite.Composite;
import mizzen.components.composite.HorizontalPanel;
import mizzen.components.composite.VerticalPanel;
import mizzen.components.input.DropdownListBox;
import mizzen.components.plot.image.BufferedImageProvider;
import mizzen.components.plot.image.ImageViewerFactory;
import mizzen.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import mizzen.components.plot.image.ResizableImageViewer;
import mizzen.components.plot.image.animate.ImageAnimatorComponent;
import mizzen.components.plot.image.animate.ImageSplitter;
import mizzen.components.tab.TabAction;
import mizzen.components.tab.TabPanel;
import mizzen.components.tab.TabPanel.TabContext;
import mizzen.controlcodes.color.Colors;
import mizzen.events.EventBus;
import mizzen.events.EventBusImpl;
import mizzen.events.EventHandlerContainer;
import mizzen.events.OnIdleEventEventHandler;
import mizzen.events.keyboard.Keys;
import mizzen.examples.BarChartTest.BarChartRandomDataDemo;
import mizzen.examples.GridBrailleTest.BrailleDemo;
import mizzen.examples.LineChartTest.LineChartDemo;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.util.Logging;

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

        Logging.setEnabled(true);

        TUIApp tabPanelDemoApp = new TUIApp(args).setDefaultMainApplicationPanel();

        Dimension sizeOfTabPanelWindow = new Dimension() {
            @Override
            public int getWidth() {
                int onethird = tabPanelDemoApp.getCanvas().getWidth() / 3;
                return tabPanelDemoApp.getCanvas().getWidth() - 4 ; //- onethird;
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
                // the rocket sprite sheet: 4 x 2 frames of 96 x 96 (x y of each frame's top-left corner)
                int IMAGEWIDTH=96;
                int IMAGEHEIGHT=96;
                int[] offsetNbrs = new int[] {0, 0,  96, 0,  192, 0,  288, 0,  0, 96,  96, 96,  192, 96,  288, 96};
                List<Position> offsets = new ArrayList<Position>();
                for (int i=0; i< offsetNbrs.length; i+=2) {
                    offsets.add(new Position.Simple(offsetNbrs[i+1],offsetNbrs[i]));
                }
                ImageRenderRequest imageRequest = new ImageRenderRequest();
                imageRequest.setDesiredImgSizeInCharacters(tabContext.getBounds());
                imageRequest.setColorMap(ImageViewerFactory.rgbColorMapDIRECT);     // full colour blocks (as the dropdowns show)
                imageRequest.setToPlotOrNot(ImageViewerFactory.PLOT_ALWAYS);
                imageRequest.setImageRenderType(ImageViewerFactory.IMAGE_RENDER_TYPE.BLOCK);
                Function<Dimension, Dimension> imgSizeCalculator = (size)-> new Dimension.Simple(Math.max(0, tabContext.getBounds().getHeight()-2), tabContext.getBounds().getWidth());   // none in a short tab
                imageRequest.setDesiredImgSizeInCharacters(imgSizeCalculator.apply(tabContext.getBounds()));
                try {
                    BufferedImage img = ImageIO.read(this.getClass().getResource("/mizzen/examples/sampledata/images/rocket-sprites.png"));
                    imageRequest.setImageToView(img);
                    ImageSplitter splitter = new ImageSplitter(imageRequest.getImageToView(), offsets, IMAGEHEIGHT, IMAGEWIDTH);
                    animatorComponent = new ImageAnimatorComponent(Colors.DEFAULT_COLOR, imageRequest, splitter, imageRequest.getDesiredImgSizeInCharacters());
                    VerticalPanel vp = new VerticalPanel();
                    vp.add(animatorComponent);
                    HorizontalPanel drawingControlSettings = new HorizontalPanel(Colors.BBlu);
                    vp.add(drawingControlSettings);
                    
                    DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE> imageRendererTypeDDLB = new DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE>(ImageViewerFactory.imageRenderTypeOptions());
                    imageRendererTypeDDLB.setSelectedValue(ImageViewerFactory.IMAGE_RENDER_TYPE.BLOCK);
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
                animatorComponent.buildTimer(120);
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
                imageViewer = new ResizableImageViewer(Colors.DEFAULT_COLOR, imgSizeCalculator.apply(tabContext.getBounds()),
                                                       new BufferedImageProvider.FromResource("/mizzen/examples/sampledata/images/earth-messenger.jpg"));
                // a photo: full colour blocks (the dropdowns below show these settings)
                imageViewer.setImageRenderType(ImageViewerFactory.IMAGE_RENDER_TYPE.BLOCK);
                imageViewer.setColorMapping(ImageViewerFactory.rgbColorMapDIRECT);
                imageViewer.setPlotPredicate(ImageViewerFactory.PLOT_ALWAYS);
                vp.add(imageViewer);

                HorizontalPanel drawingControlSettings = new HorizontalPanel(Colors.BBlu);
                vp.add(drawingControlSettings);

                DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE> imageRendererTypeDDLB = new DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE>(ImageViewerFactory.imageRenderTypeOptions());
                imageRendererTypeDDLB.setSelectedValue(ImageViewerFactory.IMAGE_RENDER_TYPE.BLOCK);
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

            BiConsumer<TUIApp, Dimension> defaultResizeAction = tabPanelDemoApp.getResizeAction();


            tabPanelDemoApp.setOnResizeAction((tApp2, size) -> {

                defaultResizeAction.accept(tApp2, size);
                tb.resize(sizeOfTabPanelWindow);
                hp.fullrecalcDim();
                vPanel.fullrecalcDim();
                debugWindow.resize(sizeOfDebugWindow);
            });

            tApp.getEventBus().appendEventHandler(new OnIdleEventEventHandler.RepeatingInterval(100, (te) -> {
                        int count = counter.incrementAndGet();
                        debugSTATs.setTextAndResize(
                                "IDLE task invoke counter = " + count + 
                                "   \n" + ((EventBusImpl)tApp.getEventBus()).statsAsStr() + 
                                "\n" + 
                                "Text segments on screen: " + tApp.getCanvas().segmentsSize() + "  ");
        
            }));

            tb.displayCurrentTab(tApp.getEventBus());

        });

        tabPanelDemoApp.start();

    }


}
