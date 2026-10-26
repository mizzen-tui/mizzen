package dtb.terminalIO.examples;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.Predicate;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.AbstractComponent;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ClickableFocusableWrapper;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.WindowFrame;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.composite.HorizontalPanel;
import dtb.terminalIO.components.composite.LayoutManagerUtils;
import dtb.terminalIO.components.input.Button;
import dtb.terminalIO.components.input.ListBox;
import dtb.terminalIO.components.input.MultiLineTextInput;
import dtb.terminalIO.components.plot.image.BufferedImageProvider;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.IMAGE_RENDER_TYPE;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBPlotPredicate;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBToAnsiColorControlCode;
import dtb.terminalIO.components.plot.image.ResizableImageViewer;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.controlcodes.color.Colors.FGBGColors;
import dtb.terminalIO.events.OnIdleEventEventHandler.RepeatingInterval;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.VAlign;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionRelativeFactory;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.components.input.DisplayableValue.SimpleDisplayValue;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.VeryBasicArgsHelper;
import dtb.terminalIO.util.TerminalDimensions;

public class WindowFrameTest {

    
    static final String DEFAULT_IMAGE = "/dtb/terminalIO/examples/sampledata/images/earth-messenger.jpg";   // bundled
    static String imgFilePath = null;     // IMAGE <file>; none: the bundled image
    
    static RGBPlotPredicate plotPredicate = ImageViewerFactory.PLOT_ALWAYS;
    static RGBToAnsiColorControlCode colorMapping = ImageViewerFactory.rgbColorMapDIRECT;
    static IMAGE_RENDER_TYPE  renderType = ImageViewerFactory.IMAGE_RENDER_TYPE.BLOCK;
    
    private static void dumbArgsParse(String[] args) {
        if (args != null && Stream.of(args).anyMatch(a -> VeryBasicArgsHelper.isArgName(a, "HELP") || "-h".equals(a) || "-?".equals(a))) {
            printHelp();
            System.exit(0);
        }
        if (args == null || args.length == 0) {
            checkImageFile();
            return;
        }
        
        for (int i=0; i<args.length; i++) {
            if ("IMAGE".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                imgFilePath=args[i];
            } else if ("IMAGEPLOT".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                plotPredicate = NotNull(ImageViewerFactory.nameToRGBPlotPredicate(args[i]),"IMAGEPLOT "+args[i]+" unknown");
            } else if ("IMAGECOLOR".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                colorMapping = NotNull(ImageViewerFactory.nameToRGBToAnsiColorControlCode(args[i]),"IMAGECOLOR "+args[i]+" unknown");
            } else if ("IMAGERENDER".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                renderType = NotNull(ImageViewerFactory.nameToImageRenderType(args[i]),"IMAGERENDER "+args[i]+" unknown");
            }
        }
        checkImageFile();
    }

    /** an image file given must exist - otherwise say so, show the help and stop */
    private static void checkImageFile() {
        if (imgFilePath != null && !new File(imgFilePath).isFile()) {
            System.out.println("image file not found: " + imgFilePath + "\n");
            printHelp();
            System.exit(1);
        }
    }

    static void printHelp() {
        String cls = WindowFrameTest.class.getName();
        System.out.println(String.join("\n",
            "WindowFrameTest - window frames (drag the title to move, the corner to resize) holding a table, text and an image",
            "",
            "usage: ./runDBG.sh " + cls + " [options] [IMAGE <file>]",
            "",
            "  IMAGE <file>           image shown in one of the windows (default: a bundled photo of the Earth)",
            "  IMAGERENDER <type>     " + names(ImageViewerFactory.imageRenderTypeOptions(), renderType),
            "  IMAGECOLOR <map>       " + names(ImageViewerFactory.colorMap(), colorMapping),
            "  IMAGEPLOT <condition>  which pixels to plot: " + names(ImageViewerFactory.plotPointPredicateOptions(), plotPredicate),
            "  HELP | -h | -?         this help",
            "",
            "example: ./runDBG.sh " + cls + " IMAGE ~/Pictures/photo.jpg"));
    }

    private static <V> String names(SimpleDisplayValue<V>[] options, V defaultValue) {
        return Stream.of(options)
                     .map(o -> {
                         String name = o.getDisplayValue().contains(" ") ? "\"" + o.getDisplayValue() + "\"" : o.getDisplayValue();
                         return o.getValue() == defaultValue ? name + " (default)" : name;
                     })
                     .collect(Collectors.joining(", "));
    }
    
    static <T> T NotNull(T t, String error) {
        if (t == null) {
            throw new IllegalArgumentException(error);
        }
        return t;
    }
    
    
    public static void main(String[] args) throws IOException {

        dumbArgsParse(args);
        
        Logging.setEnabledFromArgs(args);

        
        new TUIApp(args)
        //.setCanvasSize(24, 100)
        .setEmptyChar(' ')
        .setEmptyColors(Colors.On_Blu)
        .setCanvasSize(40, 130)
        .setDefaultMainApplicationPanel()
        .confirmOnExit()
        .setOnReadyStart((tApp)-> {

            MultiLineTextInput mTextBox = new MultiLineTextInput(); 
            mTextBox.setText("Silly Sally sold\nSeaShells at the seashore\nWhat a silly thing to do!");

            mTextBox.setColor(  new Colors.FGBGColors( Colors.BBlu, Colors.On_Bla  )  );

            // mTextBox.resize(new Dimension.Simple(20,50));

            AbstractComponent<?> wf1 = makeWF(Colors.Whi, "Test Window1", mTextBox); 
            wf1.setEmptyColor(Colors.On_IBla);

            AbstractComponent<?> wf2 = makeWF(Colors.Whi, "Test Window2", ListBox.fromStrings(new String[] {"Silly Sally",
                                                                                                            "Sold sea shells",
                                                                                                            "at the sea shore"}));
            wf2.setEmptyColor(Colors.On_Bla);

            Composite sparkLineContainer = new Composite().setBorder(false);
            sparkLineContainer.resize(new Dimension.Simple(24,100));
            SparklineTest.buildSparklineTest(tApp.getEventBus(), sparkLineContainer, null);
            WindowFrame wfSparkline = new WindowFrame(Colors.Blu, "SparkLine Demo", ClickableFocusableWrapper.wrappedIfNeeded( sparkLineContainer));

            Button toggleDrawType = new Button(Composite.DRAW_VIA_PARTS ? "Drawing via Parts" : "Drawing traditional");
            toggleDrawType.setHotKey(Keys.CTRL_V_KEYSTROKE);
            toggleDrawType.onChange((te)-> {
               Composite.DRAW_VIA_PARTS = ! Composite.DRAW_VIA_PARTS;
               toggleDrawType.setButtonText(Composite.DRAW_VIA_PARTS ? "Drawing via Parts" : "Drawing traditional");
            });

            tApp.getMainApplicationPanel().add(5, 3, new BorderedComponent( toggleDrawType ));

            Composite windowContainer = new Composite();

            Component<?> wfFruit = makeWF(Colors.Whi, "Fruit", ListBox.fromStrings(new String[] {"Apples",
                                                                                                 "Pears",
                                                                                                 "Plums"}));

            Component<?> wfVegatables = makeWF(Colors.Whi, "Fruit", ListBox.fromStrings(new String[] {"Tomotoes",
                                                                                                      "Lettuce",
                                                                                                      "Cucumber",
                                                                                                      "Kale",
                                                                                                      "Potatoes",
                                                                                                      "Carrots",
                                                                                                      "Peppers",
                                                                                                      "Cabbage",
                                                                                                      "Olives",
                                                                                                      "Celery",
                                                                                                      "Onions"}));

            windowContainer.add(4, 10, wfFruit);
            windowContainer.add(8, 14, wfVegatables);

            Component<?> nestedWindows = makeWF(Colors.Whi, "Nested Windows", ClickableFocusableWrapper.wrappedIfNeeded(windowContainer));

            AbstractComponent<?> wfSimpleText = makeWF(Colors.Whi, "Test WindowX", ClickableFocusableWrapper.wrappedIfNeeded(new TextComponent("Water is\nwet.\nExcept when its\nVery hot\nor\nvery cold!")));
            //wfSimpleText.setEmptyColor(Colors.On_Bla);
            
            
            tApp.getMainApplicationPanel().add(2, 0, wf1);
            tApp.getMainApplicationPanel().add(2, 30, wf2);
            tApp.getMainApplicationPanel().setDropShadow(true, wf2);
            tApp.getMainApplicationPanel().add(10, 30, wfSparkline);
            tApp.getMainApplicationPanel().add(20, 40, nestedWindows);
            tApp.getMainApplicationPanel().add(22, 45, wfSimpleText);

            BufferedImageProvider image = imgFilePath == null ? new BufferedImageProvider.FromResource(DEFAULT_IMAGE)
                                                              : new BufferedImageProvider.FromFilePath(imgFilePath);
            ResizableImageViewer imageViewer = new ResizableImageViewer (Colors.Whi, new Dimension.Simple(25,50), image);

            imageViewer.setPlotPredicate(plotPredicate);
            imageViewer.setColorMapping(colorMapping);
            imageViewer.setImageRenderType(renderType);
            
            WindowFrame imageContainerWF = new WindowFrame(Colors.IPur, "Image", ClickableFocusableWrapper.wrappedIfNeeded(imageViewer)) {
                @Override
                protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
                    Logging.info("WindowFrame.img draw called!" + String.valueOf(this));

                    super.draw(color, segments);
                }
            };
            tApp.getMainApplicationPanel().add(18, 42, imageContainerWF);


              tApp.getEventBus().asynchAfter(10,(te)-> imageViewer.resizeImageAsynch(te.getEventBus(), new Dimension.Simple(40, 150)));

            Composite expandDownContainer = new Composite() {
                public void fullrecalcDim() {
                    super.fullrecalcDim();
                }
            };

            expandDownContainer.setOnResize( (size) ->{}  );
            Button redButton = new Button(new FGBGColors(Colors.BWhi, Colors.On_Blu),"Top widget");
            redButton.onChange((te)->{
                if (redButton.getButtonText().equals("Top widget")) {
                    redButton.setButtonText("Top --\nWidget");
                } else {
                    redButton.setButtonText("Top widget");
                }
            });
            BorderedComponent blue = new BorderedComponent(redButton);
            expandDownContainer.add(0,0,blue);
            expandDownContainer.onResizeExpandRight(blue);

            BorderedComponent red = new BorderedComponent(new TextComponent(new FGBGColors(Colors.BWhi, Colors.On_Red),"Bottom widget"));
            //Position redPosition = new PositionVerticalRelative(blue);
            expandDownContainer.addBelow(red, blue);
            expandDownContainer.onResizeExpandDown(red);
            expandDownContainer.onResizeExpandRight(red, 30);

            BorderedComponent green = new BorderedComponent(new TextComponent(new FGBGColors(Colors.BWhi, Colors.On_Gre),"Bottom Right"));
            expandDownContainer.addRight(green, red);
            expandDownContainer.onResizeExpandDownAndRight(green);


            AbstractComponent<?> expandDownWindows = makeWF(Colors.Whi, "Expand down tests", expandDownContainer);

            tApp.getMainApplicationPanel().add(13, 65, expandDownWindows);

            HorizontalPanel statsPanel = new HorizontalPanel();

            statsPanel.add( new TextComponent("Current focus:"));
            TextComponent stats = new TextComponent("None");
            statsPanel.add(stats);
            stats.setColor(Colors.IYel);

            Position statsPosition = PositionRelativeFactory.fromAlign(VAlign.BOTTOM, Align.LEFT,  tApp.getMainApplicationPanel(), statsPanel);
            tApp.getMainApplicationPanel().add(statsPosition, statsPanel);
            
            
            Button distributeEvenlyButton = new Button("DISTIRBUTE-EVENLY");
            tApp.getMainApplicationPanel().add(0,35, distributeEvenlyButton);

            Button distributeStaggerButton = new Button("DISTIRBUTE-STAGGERED");
            tApp.getMainApplicationPanel().add(0,60, distributeStaggerButton);
            
            
            // every 2 seconds: the component with the focus
            tApp.getEventBus().appendIdleEventHandler(new RepeatingInterval(2000, (te)->{
                                                         Object currentFocus = FocusManager.UTIL.findCurrentFocus(tApp.getApplicationFocusManager());
                                                         if (currentFocus == null) {
                                                             return;
                                                         }
                                                         stats.setTextAndResize(HasName.UTIL.getName(currentFocus));
                                                      }));



            tApp.getResizeAction().accept(tApp, TerminalDimensions.create());
            
            Predicate<Component<?>> canMoveComponentsPredicate = (c) -> {
                if (c == distributeEvenlyButton || c == distributeStaggerButton) {
                    return false;
                }
                if (c instanceof TextComponent || c instanceof Button) {
                    return false;
                }
                return true;
            };
            
            //LayoutManagerUtils
            distributeEvenlyButton.onChange((te)->{
                LayoutManagerUtils.distributeComponentsEvenly((Composite)tApp.getMainApplicationPanel(), 
                                                              canMoveComponentsPredicate,
                                                              new Position.Simple(1,0),
                                                              new Dimension.Simple(tApp.getCanvas().getHeight()-1, tApp.getCanvas().getWidth()));
            });

            distributeStaggerButton.onChange((te)->{
                LayoutManagerUtils.distributeComponentsStaggered((Composite)tApp.getMainApplicationPanel(), canMoveComponentsPredicate);
            });

        }).start();

    }

    static AbstractComponent<?> makeWF(Color color, String title, Component<?> child) {
        return new WindowFrame(color, title, child);
    }

}
