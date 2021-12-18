package dtb.terminalIO.examples.plot.image;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.FillOnClearPolicy;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.VerticalPanel;
import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBPlotPredicate;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBToAnsiColorControlCode;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.ScreenCanvasBackend.UpdateStrategy;
import dtb.terminalIO.events.AbstractTimedRequestedController;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.OnIdleEventEventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;

public class ImageViewer {

    public static void main(String[] args) {
        plotterTest(args);
    }
    
    public static Map<String,String> imageFiles(String dir) {
        
        if (dir == null) {
            dir = System.getProperty("user.dir");
        }
        
        File imgDIR = new File(dir);
        if (!imgDIR.exists() || !imgDIR.isDirectory()) {
            throw new IllegalArgumentException(dir+" not a valid dir");
        }
        
        File[] files = imgDIR.listFiles((d,f)->  f.endsWith(".jpg") || f.endsWith(".png"));
        
        if (files == null || files.length == 0) {
            throw new IllegalArgumentException(dir+" does not have any jpegs or png files");
        }
        
        Map<String,String> images = new HashMap<String, String>();
        
        for (File f : files) {
            try {
                images.put(f.getName(), f.getCanonicalPath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        return images;
        
    }
    
    public static void plotterTest(String[] args) {
        
        new TUIApp(args)
                    .setDefaultMainApplicationPanel()
                    .confirmOnExit()
                    .enableMouseHandling()
                    .setOnReadyStart((tApp)->{

                        HorizontalPanel mainPanel = new HorizontalPanel();
                        tApp.getMainApplicationPanel().add(mainPanel);
                        
                        int column[] = new int[] {5};

                        BrailleGrid plotter = new BrailleGrid(Colors.BBlu, 
                                                              tApp.getDimensions().getHeight()-5, 
                                                              tApp.getDimensions().getWidth()-40);

                        plotter.setTitle(Colors.BCya, "Image Viewer");

                        VerticalPanel formPanel = new VerticalPanel();

                        formPanel.setFillOnClear(true);
                        formPanel.setEmptyColor(Colors.Blu);
                        formPanel.setEmptyChar('◍');

                        mainPanel.add(plotter);
                        mainPanel.add(formPanel);

                        Map<String,String> images = imageFiles(null);

                        ListBox<String> imagesListbox = new ListBox<String>(StringDisplayValue.fromStringValues(images));
                        imagesListbox.setTitle("Images");
                        imagesListbox.setSelected(0);
                        Dimension maxImagesSize = new Dimension.Simple(20,35);
                        if ( ! maxImagesSize.contains(imagesListbox)) {
                            ScrollableComponent imagesSC = new ScrollableComponent(imagesListbox.getColor(), maxImagesSize, imagesListbox);
                            formPanel.add(imagesSC);
                        } else {
                           formPanel.add(imagesListbox);
                        }

                        DropdownListBox<RGBToAnsiColorControlCode> rgbMapperListBox = new DropdownListBox<ImageViewerFactory.RGBToAnsiColorControlCode>(ImageViewerFactory.colorMap());
                        rgbMapperListBox.setSelected(0);
                        rgbMapperListBox.setTitle("Color mapping algorithm");

                        formPanel.add(rgbMapperListBox);

                        DropdownListBox<ImageViewerFactory.RGBPlotPredicate> rgbPlotConditionalListBox = new DropdownListBox<ImageViewerFactory.RGBPlotPredicate>(ImageViewerFactory.plotPointPredicateOptions());
                        rgbPlotConditionalListBox.setSelected(0);
                        rgbPlotConditionalListBox.setTitle("PlotPredicate");

                        formPanel.add(rgbPlotConditionalListBox);

                        List<DisplayableValue<PictureBackground>> plotBackgroundsOnEmptyFill = new ArrayList<DisplayableValue<PictureBackground>>();

                        plotBackgroundsOnEmptyFill.add(background_NOFILL.asDisplayableValue());
                        plotBackgroundsOnEmptyFill.add(background_FILL_SOLID_WHITE.asDisplayableValue());
                        plotBackgroundsOnEmptyFill.add(background_FILL_SOLID_BLACK.asDisplayableValue());
                        plotBackgroundsOnEmptyFill.add(background_FILL_SOLID_BLUE.asDisplayableValue());
                        plotBackgroundsOnEmptyFill.add(background_FILL_DOTS_WHITE.asDisplayableValue());
                        plotBackgroundsOnEmptyFill.add(background_FILL_DOTS_BLACK.asDisplayableValue());
                        plotBackgroundsOnEmptyFill.add(background_FILL_DOTS_BLUE.asDisplayableValue());
                        plotBackgroundsOnEmptyFill.add(background_FILL_DOTS_IBLUE.asDisplayableValue());

                        ListBox<PictureBackground> plotBackgroundsListBox = new ListBox<PictureBackground>();
                        plotBackgroundsListBox.setItems(plotBackgroundsOnEmptyFill);
                        plotBackgroundsListBox.setSelected(0);
                        plotBackgroundsListBox.setTitle("Empty-Fill");

                        formPanel.add(plotBackgroundsListBox);

                        List<DisplayableValue<Color>> backgroundColorForPlots = new ArrayList<DisplayableValue<Color>>();

                        backgroundColorForPlots.add(Colors.RCol.asDisplayableValue());
                        backgroundColorForPlots.add(Colors.On_IWhi.asDisplayableValue());
                        backgroundColorForPlots.add(Colors.On_Whi.asDisplayableValue());
                        backgroundColorForPlots.add(Colors.On_Bla.asDisplayableValue());
                        backgroundColorForPlots.add(Colors.On_IBla.asDisplayableValue());
                        backgroundColorForPlots.add(Colors.On_Blu.asDisplayableValue());
                        backgroundColorForPlots.add(Colors.On_IBlu.asDisplayableValue());

                        ListBox<Color> plotFillBackgroundsListBox = new ListBox<Color>();
                        plotFillBackgroundsListBox.setItems(backgroundColorForPlots);
                        plotFillBackgroundsListBox.setSelected( plotFillBackgroundsListBox.indexOf(Colors.RCol) );
                        plotFillBackgroundsListBox.setTitle("Plot-BgColor");

                        formPanel.add(plotFillBackgroundsListBox);

                        //PointPlotter pointPlotter = plotter.asPointPlotter();

                        Consumer<Integer> renderLine = (col)-> {
                            //plotter.clear();
                            plotter.markAsDirty();
                            PointPlotter pointPlotter = plotter.asPointPlotter();
                            pointPlotter.clearPlots();
                            pointPlotter.plot(col+0, 10);
                            pointPlotter.plot(col+1, 10);
                            pointPlotter.plot(col+2, 10);
                            pointPlotter.plot(col+3, 10);
                            tApp.getCanvas().clearCanvas();
                            /*
                            tApp.getCanvas().t(5,5,
                                               " col: " + col + "   "+
                                               " col: " + col + "   "+
                                               " plotter: " + plotter.getSegmentsBuffer().getSegments().size()+"   " +
                                               " canvas: "  + tApp.getCanvas().getSegmentsBuffer().getSegments().size()+"   ");
                             */
                            tApp.doRefreshCanvasIfModified();
                        };

                        renderLine.accept(column[0]);

                        tApp.getEventBus().appendEventHandler( new KeyInputEventHandler(Keys.LEFT_KEYSTROKE, (te) -> {
                            column[0]--;
                            renderLine.accept(column[0]);
                        }));

                        tApp.getEventBus().appendEventHandler( new KeyInputEventHandler(Keys.RIGHT_KEYSTROKE, (te) -> {
                            column[0]++;
                            renderLine.accept(column[0]);
                        }));

                        Map<String, Image> imageCache = new HashMap<String, Image>();

                        Consumer<TerminalEvent<?>> task_DisplayImage = (te)-> {
                            plotBackgroundsListBox.getValue().setBackGround(plotter);
                            plotter.clear();
                            //pointPlotter.clearPlots();
                            BufferedImage img;
                            Image img2;
                            int drawingWidth = plotter.getGridRectangle().getWidth();
                            int drawingHeight = plotter.getGridRectangle().getHeight();
                            try {
                                img2 = imageCache.get(imagesListbox.getValue());
                                if (img2 == null) {
                                    img = readImage(imagesListbox.getValue());
                                    img2 = img.getScaledInstance(drawingWidth, drawingHeight, 0);
                                    // TODO: need to clear the cache when window resized
                                    imageCache.put(imagesListbox.getValue(), img2);
                                }


                                plotImage(plotter, 
                                          img2, 
                                          drawingWidth, 
                                          drawingHeight, 
                                          rgbMapperListBox.getValue(),
                                          rgbPlotConditionalListBox.getValue(),
                                          plotFillBackgroundsListBox.getValue());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            tApp.doRefreshCanvasIfModified();
                        };

                        AbstractTimedRequestedController displayImgCTRL = new AbstractTimedRequestedController(()->{
                            tApp.getEventBus().asynch((te)-> task_DisplayImage.accept(te) );
                        }) {
                            @Override
                            protected boolean isRequestDifferentFromPrevious() {
                                return true;
                            }
                        };

                        displayImgCTRL.setTimerDelayMillis(2000);

                        Consumer<TerminalEvent<?>> throttledDisplayImageRequestWrapper = (te)-> {
                            displayImgCTRL.onRequest(tApp.getEventBus());
                            //task_DisplayImage.accept(te);
                        };

                        tApp.getEventBus().appendEventHandler( new KeyInputEventHandler(Keys.CTRL_I_KEYSTROKE, (te) -> {
                            throttledDisplayImageRequestWrapper.accept(te);
                        }));
                        

                        EventHandler onListBoxValueChangeHandler = (te) -> {
                            throttledDisplayImageRequestWrapper.accept(te);
                        };

                        imagesListbox.onChange(onListBoxValueChangeHandler);

                        rgbMapperListBox.onChange(onListBoxValueChangeHandler);

                        rgbPlotConditionalListBox.onChange(onListBoxValueChangeHandler);

                        plotBackgroundsListBox.onChange(onListBoxValueChangeHandler);

                        plotFillBackgroundsListBox.onChange(onListBoxValueChangeHandler);

                        ScreenCanvasBackend backEnd = (ScreenCanvasBackend) tApp.getCanvas().getCanvasBackend();
                        backEnd.setUpdateStrategy(UpdateStrategy.UPDATE_CHANGED  );

                        // tApp.getMainApplicationPanel().add(1, 1, mainPanel);

                        BiConsumer<TUIApp, Dimension> defaultOnResizeAction =   tApp.getResizeAction()!=null ? tApp.getResizeAction() : (t,s)->{};

                        tApp.setOnResizeAction((tApp2, newSize)->{
                            // do default behaviour
                            defaultOnResizeAction.accept(tApp2, newSize);
                            // now resize the grid
                            
                            imageCache.clear();
                            
                            plotter.resize( new Dimension.Simple(newSize.getHeight()-5, newSize.getWidth() - 40 )  );

                            tApp2.getEventBus().asynch((te)-> throttledDisplayImageRequestWrapper.accept(te) );
                        });

                        DefaultFocusManager mainFocusManager = (DefaultFocusManager)FocusManager.UTIL.buildFocusManagerFromComponent(tApp.getMainApplicationPanel(),tApp.getEventBus());

                        //mainFocusManager.setNavKeysFirst(false);
                        mainFocusManager.setNextKeyStroke(Keys.TAB_KEYSTROKE);
                        mainFocusManager.setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE);

                        tApp.getEventBus().appendEventHandler(  mainFocusManager );

                        // sanity check
                        FocusManager.UTIL.verifyPathsToFocusables(mainFocusManager, (Component<?>)tApp.getMainApplicationPanel());

                    })
                    .start();
    }
    
    static BufferedImage readImage(String fileName) throws IOException {
        BufferedImage image = ImageIO.read(new File(fileName));
        return image;
    }
    
    static BufferedImage getCircle(int width, int height) {

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Color transparent = new java.awt.Color(0x00FFFFFF, true);

        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        //trying to make the bufferedImage transparent 
        g.setComposite(AlphaComposite.Src);
        g.setColor(transparent);
        g.setBackground(transparent);
        g.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
        //drawing the circle 
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(java.awt.Color.black);

        g.setStroke(new BasicStroke(1));
        g.drawOval(0, 0, 100, 100);
        return bufferedImage;
    }

    
    static interface PictureBackground {
        void setBackGround(Component<?> c);
    }
    
    static class PictureBackgroundImpl implements PictureBackground {

        char  emptyChar = ' ';
        Color emptyColor = Colors.DEFAULT_COLOR;
        boolean fillOnEmpty = false;
        String name;
        
        public PictureBackgroundImpl(char emptyChar, dtb.terminalIO.controlcodes.color.Color emptyColor,
                boolean fillOnEmpty, String name) {
            super();
            this.emptyChar = emptyChar;
            this.emptyColor = emptyColor;
            this.fillOnEmpty = fillOnEmpty;
            this.name = name;
        }



        @Override
        public void setBackGround(Component<?> c) {
            if (fillOnEmpty) {
                ((FillOnClearPolicy.Provider)c).getFillOnClearPolicy().setFillOnClear(fillOnEmpty);
                ((FillOnClearPolicy.Provider)c).getFillOnClearPolicy().setEmptyColor(emptyColor);
                ((FillOnClearPolicy.Provider)c).getFillOnClearPolicy().setEmptyChar(emptyChar);
                c.markAsDirty();
            } else {
                ((FillOnClearPolicy.Provider)c).getFillOnClearPolicy().setFillOnClear(fillOnEmpty);
                c.markAsDirty();
            }
        }
        
        public DisplayableValue<PictureBackground> asDisplayableValue() {
            return new DisplayableValue<PictureBackground>() {
                @Override
                public PictureBackground getValue() {
                    return PictureBackgroundImpl.this;
                }
                @Override
                public String getDisplayValue() {
                    return name;
                }
            };
        }
        
        
    }

    static PictureBackgroundImpl background_NOFILL = new PictureBackgroundImpl(' ', null, false,"None");
    static PictureBackgroundImpl background_FILL_SOLID_WHITE = new PictureBackgroundImpl(' ', new Colors.FGBGColors(Colors.BIWhi, Colors.On_IWhi), true,"Solid white");
    static PictureBackgroundImpl background_FILL_SOLID_BLACK = new PictureBackgroundImpl(' ', new Colors.FGBGColors(Colors.Bla, Colors.On_Bla), true, "Solid black");
    static PictureBackgroundImpl background_FILL_SOLID_BLUE = new PictureBackgroundImpl(' ', new Colors.FGBGColors(Colors.Blu, Colors.On_Blu), true, "Solid blue");

    static PictureBackgroundImpl background_FILL_DOTS_WHITE = new PictureBackgroundImpl('░', Colors.BIWhi, true,"Dippled white");
    static PictureBackgroundImpl background_FILL_DOTS_BLACK = new PictureBackgroundImpl('░', Colors.Bla, true, "Dippled black");
    static PictureBackgroundImpl background_FILL_DOTS_BLUE = new PictureBackgroundImpl('░', Colors.Blu, true, "Dippled blue");
    static PictureBackgroundImpl background_FILL_DOTS_IBLUE = new PictureBackgroundImpl('░', Colors.IBlu, true, "Dippled IBlue");

    private static RGBToAnsiColorControlCode rgbColorMapDIRECT = (r,g,b) ->Colors.fromRGBFG(r,g,b);

    private static RGBToAnsiColorControlCode rgbColorPlotBlackAndGreyAsWhite = (r,g,b) ->  {

        if (r < 50 && g < 50 && b < 50) {
            return Colors.BWhi;
        }

        return Colors.BBla;
    };

    private static RGBToAnsiColorControlCode rgbColorPlotBlack = (r,g,b) ->  {
        
        if (r < 50 && g < 50 && b < 50) {
            return Colors.BWhi;
        }
        
        
        return Colors.Whi;
    };

    private static RGBToAnsiColorControlCode rgbGreyScale = (r,g,b) ->  {
        return Colors.fromRGBToGreyScaleFG(r, g, b);
    };

    

    private static void plotImage(BrailleGrid plotter, 
                                  Image image, 
                                  int width, 
                                  int height, 
                                  RGBToAnsiColorControlCode colorMap,
                                  RGBPlotPredicate toPlotOrNot, 
                                  Color plotBGColor ) throws Exception {

        
        ImageViewerFactory.simpleImageToBrailleView(plotter, 
                                                    image, 
                                                    new Dimension.Simple(height, width),toPlotOrNot, colorMap);

    }

}
