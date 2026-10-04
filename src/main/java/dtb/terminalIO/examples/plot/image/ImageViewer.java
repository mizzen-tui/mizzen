package dtb.terminalIO.examples.plot.image;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.stream.Stream;
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
import dtb.terminalIO.components.input.IntegerInputWithIncrDecrButtons;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.plot.braille.BrailleGrid;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderer;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBPlotPredicate;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBToAnsiColorControlCode;
import dtb.terminalIO.components.scroll.ScrollableComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.ScreenCanvasBackend.UpdateStrategy;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TimedRequestedController;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.util.VeryBasicArgsHelper;

public class ImageViewer {

    public static void main(String[] args) {
        if (Stream.of(args).anyMatch(a -> VeryBasicArgsHelper.isArgName(a, "HELP") || "-h".equals(a) || "-?".equals(a))) {
            printHelp();
            System.exit(0);
        }
        String[] imageDir = { null };
        VeryBasicArgsHelper.onNamedArg(args, "imagedir", (dir) -> imageDir[0] = dir);
        if (imageDir[0] == null || !new File(imageDir[0]).isDirectory()) {
            System.out.println(imageDir[0] == null ? "no image directory given\n" : "not a directory: " + imageDir[0] + "\n");
            printHelp();
            System.exit(1);
        }
        plotterTest(args);
    }

    static void printHelp() {
        String cls = ImageViewer.class.getName();
        System.out.println(String.join("\n",
            "ImageViewer - pick an image (.jpg / .png) from a directory and view it with different render settings",
            "",
            "usage: ./runDBG.sh " + cls + " imagedir <directory>",
            "",
            "  imagedir <directory>   directory holding the .jpg / .png images to list",
            "  HELP | -h | -?         this help",
            "",
            "example: ./runDBG.sh " + cls + " imagedir ~/eclipse-workspace/tmpImages"));
    }

    public static void plotterTest(String[] args) {

        new TUIApp(args)
                    .setDefaultMainApplicationPanel()
                    .confirmOnExit()
                    .setOnReadyStart((tApp)->{

                        HorizontalPanel mainPanel = new HorizontalPanel();
                        mainPanel.setBorder(true);
                        tApp.getMainApplicationPanel().add(mainPanel);

                        ImageRenderRequest imageRequest = new ImageRenderRequest();

                        Dimension basePlotterSize = new Dimension.SupplierAsDimension(()-> new Dimension.Simple(tApp.getDimensions().getHeight()-7, tApp.getDimensions().getWidth() - 42 ));
                        Dimension scrollableWithPlotterSize = new Dimension.SupplierAsDimension(()-> new Dimension.Simple(basePlotterSize.getHeight()+2,basePlotterSize.getWidth()+2));
                        Dimension.Delegate plotterDimension = new Dimension.Delegate( basePlotterSize );

                        imageRequest.setDesiredImgSizeInCharacters(plotterDimension);
                        imageRequest.setColorMap(ImageViewerFactory.rgbColorMapRCol);
                        imageRequest.setToPlotOrNot(ImageViewerFactory.PLOT_ALWAYS);
                        imageRequest.setImageRenderType(ImageViewerFactory.IMAGE_RENDER_TYPE.BRAILLE);
                        imageRequest.setImageToView(getCircle(plotterDimension.getWidth()-4, plotterDimension.getHeight()-4));

                        ImageRenderer plotter;
                        try {
                            plotter = ImageViewerFactory.fromImage(imageRequest);
                        } catch (Exception e1) {
                            // TODO Auto-generated catch block
                            throw new RuntimeException(e1);
                        } // new BrailleGrid(Colors.BBlu, plotterDimension);

                        ScrollableComponent scrollableWithPlotter = new ScrollableComponent(Colors.BCya, new Dimension.Simple(scrollableWithPlotterSize),plotter);

                        scrollableWithPlotter.setTitle(Colors.BCya, "Image Viewer");

                        IntegerInputWithIncrDecrButtons imgScaleControls = new IntegerInputWithIncrDecrButtons(scrollableWithPlotter.getColor(),"Zoom","-","+");

                        scrollableWithPlotter.addFooterComponent(imgScaleControls);
                        imgScaleControls.setValue(100);

                        Dimension plotterDimensionWithScale = new Dimension.SupplierAsDimension(()->{
                            if (imgScaleControls.getValue() == 100) {
                                return basePlotterSize;
                            }
                            float ratio = ((float)imgScaleControls.getValue()) / 100.0f;
                            int height = Math.round(basePlotterSize.getHeight()*ratio);
                            int width = Math.round(basePlotterSize.getWidth()*ratio);
                            height = height > 5  ? height : 1;
                            width  = width  > 5  ? width  : 1;
                            
                            return new Dimension.Simple(height, width);
                        });

                        plotterDimension.setBaseSize(plotterDimensionWithScale);

                        VerticalPanel formPanel = new VerticalPanel();

                        formPanel.setFillOnClear(true);
                        formPanel.setEmptyColor(Colors.Blu);
                        formPanel.setEmptyChar(' ');
                        formPanel.setBorder(true);

                        mainPanel.add(scrollableWithPlotter);
                        mainPanel.add(formPanel);

                        Map<String,String> images = new HashMap<String, String>();
                        VeryBasicArgsHelper.onNamedArg(args,"imagedir",(imageDir)-> images.putAll(imageFiles(imageDir)));

                        if (images.size()<1) {
                            throw new IllegalStateException("No image directory specified");
                        }

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

                        DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE> ddlbRenderType = new DropdownListBox<ImageViewerFactory.IMAGE_RENDER_TYPE>(ImageViewerFactory.imageRenderTypeOptions());
                        ddlbRenderType.setSelected(0);
                        ddlbRenderType.setTitle("Render type");
                        formPanel.add(ddlbRenderType);

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

                        Map<String, Image> imageCache = new HashMap<String, Image>();

                        boolean forceException = true;

                        Consumer<TerminalEvent<?>> task_DisplayImage = (te)-> {
                                                        
                            plotBackgroundsListBox.getValue().setBackGround(plotter);
                            plotter.clear();
                            //pointPlotter.clearPlots();
                            BufferedImage img;
                            Image img2;
                           // int drawingWidth = plotter.getWidth() getGridRectangle().getWidth();
                           // int drawingHeight = plotter.getGridRectangle().getHeight();
                            try {
                                img2 = imageCache.get(imagesListbox.getValue());
                                if (img2 == null) {
                                    img = readImage(imagesListbox.getValue());
                                    img2 = img; // img.getScaledInstance(drawingWidth, drawingHeight, 0);
                                    // TODO: need to clear the cache when window resized
                                    imageCache.put(imagesListbox.getValue(), img2);
                                }
                                
                                if (forceException == true) {
                                //    throw new IllegalStateException("Lets see where this goes");
                                }
                                
                                // set render image and render options inside request
                                imageRequest.setColorMap(rgbMapperListBox.getValue());
                                imageRequest.setDesiredImgSizeInCharacters(plotterDimension);
                                imageRequest.setImageRenderType(ddlbRenderType.getValue());
                                imageRequest.setToPlotOrNot(rgbPlotConditionalListBox.getValue());
                                imageRequest.setImageToView(img2);
                                
                                // now do the render!
                                ImageViewerFactory.fromImage(plotter, imageRequest);

                            } catch (Exception e) {
                                System.out.println("GOT HERE!!!!!!!!!!!!!!!!!!!!!!!!1");
                                throw new RuntimeException(e);

                            }
                            te.getEventBus().raiseEvent(new RequestUpdateCanvasEvent(plotter));
                        };

                        TimedRequestedController displayImgCTRL = new TimedRequestedController(()->{
                            tApp.getEventBus().asynch((te)-> task_DisplayImage.accept(te) );
                        });

                        displayImgCTRL.setTimerDelayMillis(250);

                        Consumer<TerminalEvent<?>> throttledDisplayImageRequestWrapper = (te)-> {
                            displayImgCTRL.onRequest(tApp.getEventBus());
                            //task_DisplayImage.accept(te);
                        };

                        imgScaleControls.onChange((te)->{
                            plotter.resize( new Dimension.Simple(plotterDimension)  );
                            scrollableWithPlotter.resize(new Dimension.Simple(scrollableWithPlotterSize));
                            throttledDisplayImageRequestWrapper.accept(te);
                        });

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
                        
                        ddlbRenderType.onChange(onListBoxValueChangeHandler);

                        ScreenCanvasBackend backEnd = (ScreenCanvasBackend) tApp.getCanvas().getCanvasBackend();
                        backEnd.setUpdateStrategy(UpdateStrategy.UPDATE_CHANGED  );

                        // tApp.getMainApplicationPanel().add(1, 1, mainPanel);

                        BiConsumer<TUIApp, Dimension> defaultOnResizeAction =   tApp.getResizeAction()!=null ? tApp.getResizeAction() : (t,s)->{};

                        tApp.setOnResizeAction((tApp2, newSize)->{
                            // do default behaviour
                            defaultOnResizeAction.accept(tApp2, newSize);
                            // now resize the grid
                            imageCache.clear();

                            plotter.resize( new Dimension.Simple(plotterDimension)  );
                            scrollableWithPlotter.resize(new Dimension.Simple(scrollableWithPlotterSize));
                            tApp2.getEventBus().asynch((te)-> throttledDisplayImageRequestWrapper.accept(te) );
                        });

                    }).start();
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

    static BufferedImage readImage(String fileName) throws IOException {
        BufferedImage image = ImageIO.read(new File(fileName));
        return image;
    }

    static BufferedImage getCircle(int width, int height) {

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
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

    

    static Object plotImage_mutex = new Object();
    
    private static void plotImage111(BrailleGrid plotter, 
                                  Image image, 
                                  int width, 
                                  int height, 
                                  RGBToAnsiColorControlCode colorMap,
                                  RGBPlotPredicate toPlotOrNot, 
                                  Color plotBGColor ) throws Exception {

        
        synchronized (plotImage_mutex) {
            ImageViewerFactory.simpleImageToBrailleView(plotter, 
                    image, 
                    new Dimension.Simple(height, width),
                    toPlotOrNot, 
                    colorMap);
        }

    }

}
