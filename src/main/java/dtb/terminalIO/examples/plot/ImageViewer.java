package dtb.terminalIO.examples.plot;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.FillOnClearPolicy;
import dtb.terminalIO.components.HorizontalPanel;
import dtb.terminalIO.components.VerticalPanel;
import dtb.terminalIO.components.braille.BrailleGrid;
import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.components.input.DropdownListBox;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors.BgColor;
import dtb.terminalIO.controlcodes.color.Colors.ColorFromCodes;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
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
        // gridTest(args);
    }
    public static void gridTest(String[] args) {
        new TUIApp(args)
                    .setCanvasSize(new Dimension.Simple(50,120))
                    .setDefaultMainApplicationPanel()
                    .confirmOnExit()
                    .enableMouseHandling()
                    .setOnReadyStart((tApp)->{

                        BrailleGrid grid = new BrailleGrid(Colors.BBlu, new Dimension.Simple(48,118));

                        int col[] = new int[] {10};
                        
                        long[][] fractal = makeFractal();
                        
                        tApp.getMainApplicationPanel().add(1, 1, grid);
                    })
                    .start();
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
        
        HorizontalPanel mainPanel = new HorizontalPanel();
        
        new TUIApp(args)
                    .setCanvasSize(new Dimension.Simple(50,170))
                    .setMainApplicationPanel(mainPanel)
                    .confirmOnExit()
                    .enableMouseHandling()
                    .setOnReadyStart((tApp)->{

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
//                        images.put("cheoyLee.png", "/home/dbalme/workspace/dtbTerminalIO/cheoyLee.png");
//                        images.put("Acme.jpg", "/home/dbalme/workspace/dtbTerminalIO/Acme.jpg");
//                        images.put("wb48.gif", "/home/dbalme/eclipse/configuration/org.eclipse.osgi/388/0/.cp/themes/purpleMesh/graphics/icons/ctool/wb48.gif");
//                        images.put("roltSmall.jpg", "/home/dbalme/workspace/dtbTerminalIO/roltSmall.jpg");
//                        images.put("r-olt-dots1.png","/home/dbalme/workspace/dtbTerminalIO/r-olt-dots1.png");
//                        images.put("smallrolt2.png", "/home/dbalme/workspace/dtbTerminalIO/smallrolt2.png");
//                        images.put("DavidHeadShot.jpg", "/home/dbalme/workspace/dtbTerminalIO/DavidHeadShot.jpg");
//                        images.put("roltLarge.jpg", "/home/dbalme/workspace/dtbTerminalIO/roltLarge.jpg");
//                        images.put("roltLargeEdgeDetection.png", "/home/dbalme/workspace/dtbTerminalIO/roltLargeEdgeDetection.png");
//                        images.put("smiley1.jpg", "/home/dbalme/workspace/dtbTerminalIO/smiley1.jpg");

                        ListBox<String> imagesListbox = new ListBox<String>(StringDisplayValue.fromStringValues(images));
                        imagesListbox.setTitle("Images");
                        imagesListbox.setSelected(0);
                        formPanel.add(imagesListbox);
                        
                        List<DisplayableValue<RGBToAnsiColorControlCode>> rgbMappingList = new ArrayList<DisplayableValue<RGBToAnsiColorControlCode>>();
                        
                        rgbMappingList.add(new DisplayableValue<RGBToAnsiColorControlCode>() {
                            @Override
                            public RGBToAnsiColorControlCode getValue() {
                                return rgbColorMapDIRECT;
                            }
                            @Override
                            public String getDisplayValue() {
                                return "rgb-direct";
                            }
                        });
                        
                        
                        rgbMappingList.add(new DisplayableValue<RGBToAnsiColorControlCode>() {
                            @Override
                            public RGBToAnsiColorControlCode getValue() {
                                return rgbColorMapSimple1;
                            }
                            @Override
                            public String getDisplayValue() {
                                return "rgb-simple1";
                            }
                        });

                        rgbMappingList.add(new DisplayableValue<RGBToAnsiColorControlCode>() {
                            @Override
                            public RGBToAnsiColorControlCode getValue() {
                                return rgbColorPlotBlackAndGreyAsWhite;
                            }
                            @Override
                            public String getDisplayValue() {
                                return "rgb-blackAndGreyAsWhite";
                            }
                        });
                        
                        rgbMappingList.add(new DisplayableValue<RGBToAnsiColorControlCode>() {
                            @Override
                            public RGBToAnsiColorControlCode getValue() {
                                return rgbGreyScale;
                            }
                            @Override
                            public String getDisplayValue() {
                                return "rgb-greyScale";
                            }
                        });
                        
                        
                        
                        ListBox<RGBToAnsiColorControlCode> rgbMapperListBox = new ListBox<RGBToAnsiColorControlCode>();
                        rgbMapperListBox.setItems(rgbMappingList);
                        rgbMapperListBox.setSelected(0);
                        rgbMapperListBox.setTitle("Color mapping algorithm");
                        
                        formPanel.add(rgbMapperListBox);
                        
                        List<DisplayableValue<RGBPlotPredicate>> plotConditionals = new ArrayList<DisplayableValue<RGBPlotPredicate>>();
                        
                        plotConditionals.add(new DisplayableValue<RGBPlotPredicate>() {
                            @Override
                            public RGBPlotPredicate getValue() {
                                return PLOT_ALWAYS;
                            }
                            @Override
                            public String getDisplayValue() {
                                return "plot-all";
                            }
                        });
                        
                        plotConditionals.add(new DisplayableValue<RGBPlotPredicate>() {
                            @Override
                            public RGBPlotPredicate getValue() {
                                return PLOT_WHITE_ONLY;
                            }
                            @Override
                            public String getDisplayValue() {
                                return "plot-white";
                            }
                        });
                        
                        plotConditionals.add(new DisplayableValue<RGBPlotPredicate>() {
                            @Override
                            public RGBPlotPredicate getValue() {
                                return PLOT_BLACK_ONLY;
                            }
                            @Override
                            public String getDisplayValue() {
                                return "plot-black";
                            }
                        });

                        DropdownListBox<RGBPlotPredicate> rgbPlotConditionalListBox = new DropdownListBox<RGBPlotPredicate>();
                        rgbPlotConditionalListBox.setItems(plotConditionals);
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
                        
                        PointPlotter pointPlotter = plotter.asPointPlotter();
                        
                        Consumer<Integer> renderLine = (col)-> {
                            //plotter.clear();
                            plotter.markAsDirty();
                            pointPlotter.clearPlots();
                            pointPlotter.plot(col+0, 10);
                            pointPlotter.plot(col+1, 10);
                            pointPlotter.plot(col+2, 10);
                            pointPlotter.plot(col+3, 10);
                            //plotter.markAsDirty();
                            tApp.getCanvas().clearCanvas();
                            tApp.getCanvas().t(5,5,
                                               " col: " + col + "   "+
                                               " col: " + col + "   "+
                                               " plotter: " + plotter.getSegmentsBuffer().getSegments().size()+"   " +
                                               " canvas: "  + tApp.getCanvas().getSegmentsBuffer().getSegments().size()+"   ");
                            tApp.doRefreshCanvasIfModified();
                        };

                        renderLine.accept(column[0]);
                        
                        long[][] fractal = makeFractal();
                        
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
                            pointPlotter.clearPlots();
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
                                plotImage(pointPlotter, 
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
                            tApp.getEventBus().appendEventHandler(
                                        new OnIdleEventEventHandler.OneTime((te)->  {
                                            task_DisplayImage.accept(te);
                                        }));
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

                            plotter.clear();
                            pointPlotter.clearPlots();

                            tApp2.getEventBus().appendEventHandler( new OnIdleEventEventHandler.OneTime((te)->  {
                                throttledDisplayImageRequestWrapper.accept(te);
                            }));
                            
                            
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

    @FunctionalInterface
    static interface RGBPlotPredicate {
        
        boolean test(int r,int g, int b);
        
    }
    
    static RGBPlotPredicate PLOT_ALWAYS = (r,g,b) -> true;

    static RGBPlotPredicate PLOT_WHITE_ONLY = (r,g,b) -> r > 200 && g > 200 && b > 200;

    static RGBPlotPredicate PLOT_BLACK_ONLY = (r,g,b) -> r < 100 && g < 100 && b < 100;

    @FunctionalInterface
    static interface RGBToAnsiColorControlCode {
        
        Color toControlCode(int r,int g, int b);
        
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
    
    static RGBToAnsiColorControlCode rgbColorMapDIRECT = (r,g,b) ->Colors.fromRGB(r,g,b);
    
    static RGBToAnsiColorControlCode rgbColorPlotBlackAndGreyAsWhite = (r,g,b) ->  {
        
        if (r < 50 && g < 50 && b < 50) {
            return Colors.BWhi;
        }
        
        
        return Colors.BBla;
    };

    static RGBToAnsiColorControlCode rgbColorPlotBlack = (r,g,b) ->  {
        
        if (r < 50 && g < 50 && b < 50) {
            return Colors.BWhi;
        }
        
        
        return Colors.Whi;
    };

    static RGBToAnsiColorControlCode rgbGreyScale = (r,g,b) ->  {
        return Colors.fromRGBToGreyScale(r, g, b);
    };

    
    static RGBToAnsiColorControlCode rgbColorMapSimple1 = (r,g,b) ->  {
        
        if (r + g + b == 0) {
            return Colors.Bla;
        }
        
        if (r + g + b > 3 * 200) {
            return Colors.BWhi;
        }
        
        // red
        if (r > 100 && g < 100 && b < 100) {
            return r > 200 ? Colors.BRed : Colors.Red;
        }
        // green
        if (g > 100 && r < 100 && b < 100) {
            return g > 200 ? Colors.BGre : Colors.Gre;
        }
        // blue
        if (b > 100 && r < 100 && g < 100) {
            return b > 200 ? Colors.BBlu : Colors.Blu;
        }
        
        // yellow
        if (r > 100 && g > 100 && b < 100) {
            return r > 200 && g > 200 ? Colors.BYel : Colors.Yel;
        }
        // cyan
        if (r < 100 && g > 150 && b > 150) {
            return g > 200 && b > 200 ? Colors.BICya : Colors.Cya;
        }
        // orange
        if (r < 240 && g >= 170 && b < 100) {
            return new Colors.FGBGColors(Colors.Bla, Colors.On_Yel);
        }
        
        return Colors.BBla;
    };
    
    static void plotImage(PointPlotter plotter, 
                          Image image, 
                          int width, 
                          int height, 
                          RGBToAnsiColorControlCode colorMap,
                          RGBPlotPredicate toPlotOrNot, 
                          Color plotBGColor ) throws InterruptedException {

        int[] pixels = new int[width * height];
        PixelGrabber pxGrabber = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);

        pxGrabber.grabPixels();
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
               int pixel = pixels[row * width + col];
               
               int blue  = (pixel & 0xff);
               int green = ((pixel & 0xff00) >> 8);
               int red   = ((pixel & 0xff0000) >> 16);
               
               //if (pixel != -1 && pixel != 0) {
               if (toPlotOrNot.test(red, green, blue)) {
                   Color mappedColor = colorMap.toControlCode(red, blue, green);
                   if (plotBGColor != null && plotBGColor instanceof Colors.BgColor) {
                       mappedColor = Colors.ColorFromCodes.instance(mappedColor).appendCodes(plotBGColor);
                   }
                   plotter.setColor(mappedColor);
                   plotter.plot(col, row);
               }
               //}
            }
        }

    }
    
    static long[][] makeFractal() {
        Random random = new Random();
        long[][] background = new long[240][320];
        double Cr = (random.nextDouble() - 0.5) * 2;
        double Ci = random.nextDouble() - 0.5;
        int angleOffset = random.nextInt(256);
        int A = 2 + random.nextInt(2);
        int B = 1 + random.nextInt(2);
        boolean C = random.nextBoolean();
        boolean D = random.nextBoolean();
        boolean E = random.nextBoolean();
        for (int i = 0; i < 240; i++) {
            for (int j = 0; j < 320; j++) {
                double Zi = (i - 120) / 120.0, Zr = (j - 160) / 120.0;
                int angle = random.nextInt(16);
                while (angle < 4096 && Zi * Zi + Zr * Zr < 4.0) {
                    double nextZr = Zr * Zr - Zi * Zi + Cr;
                    Zi = A * Zr * Zi * (C ? Zr : 1) * (D ? Zi : 1) + Ci;
                    if (E) {
                        Zi = Math.sqrt(Zi);
                    }
                    Zr = B * nextZr;
                    angle += 24;
                }
                background[i][j] = C(96, angleOffset + angle, 255);
            }
        }
        
        return background;
    }


    public static long C(int magnitude, int angle, int light) {
      return (D(magnitude, angle, light) << 16)
           | (D(magnitude, angle + 85, light) << 8)
           | (D(magnitude, angle - 85, light))
           | 0xff000000;
    }

    public static long D(int magnitude, int angle, int light) {
      return (int)(magnitude
                   * Math.pow((Math.cos(Math.PI * angle / 127.5) + 1) / 2,
                   light / 255.0));
    }

//    static class FastRGB
//    {
//
//        public int width;
//        public int height;
//        private boolean hasAlphaChannel;
//        private int pixelLength;
//        private int[] pixels;
//        private BufferedImage image;
//
//        FastRGB(BufferedImage image)
//        {
//
//            //pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
//            width = image.getWidth();
//            height = image.getHeight();
//            hasAlphaChannel = image.getAlphaRaster() != null;
//            pixelLength = 3;
//            if (hasAlphaChannel)
//            {
//                pixelLength = 4;
//            }
//            
//            this.image = image;
//
//        }
//
//        int getRGB(int x, int y) {
//            return this.image.getRGB(x, y);
//        }
//    
//        int getRGBFastButBroken(int x, int y)
//        {
//            
//            
//            int pos = (y * pixelLength * width) + (x * pixelLength);
//
//            int argb = -16777216; // 255 alpha
//            if (hasAlphaChannel)
//            {
//                argb = (((int) pixels[pos++] & 0xff) << 24); // alpha
//            }
//
//            argb += ((int) pixels[pos++] & 0xff); // blue
//            argb += (((int) pixels[pos++] & 0xff) << 8); // green
//            argb += (((int) pixels[pos++] & 0xff) << 16); // red
//            return argb;
//        }
//    }    
}
