package dtb.terminalIO.examples.plot.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import dtb.terminalIO.components.input.DisplayableValue.SimpleDisplayValue;
import dtb.terminalIO.util.VeryBasicArgsHelper;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import dtb.terminalIO.app.TUIApp;
import dtb.terminalIO.components.WindowFrame;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.IMAGE_RENDER_TYPE;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBPlotPredicate;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBToAnsiColorControlCode;
import dtb.terminalIO.components.plot.image.MovingPicturesViewer;
import dtb.terminalIO.components.plot.image.animate.AnimatedGifImageIterator;
import dtb.terminalIO.components.plot.image.animate.ImageIterator;
import dtb.terminalIO.components.plot.image.animate.ImageSplitter;
import dtb.terminalIO.examples.sampledata.ReadSampleFiles;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.Vec2;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.TerminalDimensions;

public class ImageSplitterTest {

    public static void main(String[] args) throws Exception {

       Logging.setEnabled(true);

        try {
            parseArgs(args);
        } catch (IllegalArgumentException e) {
            System.err.println("ERROR: " + e.getMessage() + "\n");
            printHelp();
            System.exit(1);
        }

        Dimension termsize = TerminalDimensions.create();
        
        new TUIApp(args)
           .setCanvasSize(termsize)
           .setDefaultMainApplicationPanel()
           .confirmOnExit()
           .setOnReadyStart((tApp)->{

               int offset = 0;

               for (ImageSpec spec : images) {
                   String imgFilePath = asFile(spec.path);
                   ImageSettings settings = spec.settings;
                   ImageIterator imageIterator;

                   try {
                   File imgFILE = new File(imgFilePath);
                   if (! imgFILE.exists()) {
                        throw new IOException(imgFilePath + " not found");
                   }
  
                   BufferedImage image = ImageIO.read(new File(imgFilePath));

                   int imageWidth = image.getWidth();
                   int imageHeight = image.getHeight();

                   float widthToHeightRatio = ((float)imageWidth)/ ((float)imageHeight);

                   Dimension imgSize = new Dimension.Simple(termsize.getHeight()-8, Math.min((int)((termsize.getHeight()-4)*widthToHeightRatio)*2, termsize.getWidth()-40));

                   ImageRenderRequest imageRequest = new ImageRenderRequest();
                   imageRequest.setDesiredImgSizeInCharacters(imgSize);

                   imageRequest.setColorMap(settings.colorMapping);
                   imageRequest.setImageRenderType(settings.renderType);
                   imageRequest.setToPlotOrNot(settings.plotPredicate);
                   imageRequest.setImageToView(image);
                   imageRequest.setGradientStr(settings.gradientStr);

                   
                   if (imgFilePath.endsWith(".gif")) {
                        imageIterator = new AnimatedGifImageIterator(imgFilePath);
                   } else {
                       imageIterator = new ImageSplitter(NotNull(imageRequest, "Image spec's not defined").getImageToView(), 
                                                         NotNull(settings.offsets, "OFFSETS not defined for " + imgFilePath),
                                                         settings.imageHeight,
                                                         settings.imageWidth);
                   }
                   
                   MovingPicturesViewer movingPicturesViewer = new MovingPicturesViewer(Colors.DEFAULT_COLOR, imageIterator, imageRequest); 
                   
                   movingPicturesViewer.setBorder(false);
                   
                   WindowFrame wf = new WindowFrame(movingPicturesViewer.getColor(), imgFILE.getName(), movingPicturesViewer);
                   
                   
                   tApp.getMainApplicationPanel().add(offset, offset, wf);
                   movingPicturesViewer.start(tApp.getEventBus(), settings.interval);
                   
                   offset++;

                   } catch (IOException e) {
                       throw new RuntimeException(e);
                   }

               }

           }).start();

    }
    
    /** rendering settings; each IMAGE gets a copy of the settings in force where it appears */
    static class ImageSettings implements Cloneable {
        int interval = 250;
        IMAGE_RENDER_TYPE renderType = ImageViewerFactory.IMAGE_RENDER_TYPE.BLOCK;
        RGBToAnsiColorControlCode colorMapping = ImageViewerFactory.rgbColorMapDIRECT;
        RGBPlotPredicate plotPredicate = ImageViewerFactory.PLOT_ALWAYS;
        String gradientStr = "ÆÑÊŒØMÉËÈÃÂWQBÅæ#NÁþEÄÀHKRŽœXgÐêqÛŠÕÔA€ßpmãâG¶øðé8ÚÜ$ëdÙýèÓÞÖåÿÒb¥FDñáZPäšÇàhû§ÝkŸ®S9žUTe6µOyxÎ¾f4õ5ôú&aü™2ùçw©Y£0VÍL±3ÏÌóC@nöòs¢u‰½¼‡zJƒ%¤Itocîrjv1lí=ïì<>i7†[¿?×}*{+()\\/»«•¬|!¡÷¦¯—^ª„”“~³º²–°­¹‹›;:’‘‚’˜ˆ¸…·¨´`";
        List<Vec2<?>> offsets;          // sprite sheet frame positions (non-GIF images)
        int imageWidth = 47;            // sprite sheet frame size
        int imageHeight = 46;

        ImageSettings copy() {
            try {
                return (ImageSettings) clone();
            } catch (CloneNotSupportedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    static class ImageSpec {
        final String path;
        final ImageSettings settings;
        ImageSpec(String path, ImageSettings settings) {
            this.path = path;
            this.settings = settings;
        }
    }

    static final ImageSettings DEFAULTS = new ImageSettings();
    static final List<ImageSpec> images = new ArrayList<ImageSpec>();

    /**
     * Left to right: options change the current settings, each IMAGE takes a copy of them.
     * So an option applies to every following IMAGE until it is given again ("sticky").
     */
    private static void parseArgs(String[] args) {
        if (Stream.of(args).anyMatch(a -> VeryBasicArgsHelper.isArgName(a, "HELP") || "-h".equals(a) || "-?".equals(a))) {
            printHelp();
            System.exit(0);
        }

        ImageSettings current = DEFAULTS.copy();
        int lastImageIndex = -1, lastOptionIndex = -1;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            int optionIndex = i;
            if (VeryBasicArgsHelper.isArgName(arg, "IMAGE")) {
                images.add(new ImageSpec(value(args, ++i, "IMAGE"), current.copy()));
                lastImageIndex = i;
                continue;
            } else if (VeryBasicArgsHelper.isArgName(arg, "INTERVAL")) {
                current.interval = parseInt("INTERVAL", value(args, ++i, "INTERVAL"));
            } else if (VeryBasicArgsHelper.isArgName(arg, "IMAGERENDER")) {
                String v = value(args, ++i, "IMAGERENDER");
                current.renderType = NotNull(ImageViewerFactory.nameToImageRenderType(v), "IMAGERENDER " + v + " unknown");
            } else if (VeryBasicArgsHelper.isArgName(arg, "IMAGECOLOR")) {
                String v = value(args, ++i, "IMAGECOLOR");
                current.colorMapping = NotNull(ImageViewerFactory.nameToRGBToAnsiColorControlCode(v), "IMAGECOLOR " + v + " unknown");
            } else if (VeryBasicArgsHelper.isArgName(arg, "IMAGEPLOT")) {
                String v = value(args, ++i, "IMAGEPLOT");
                current.plotPredicate = NotNull(ImageViewerFactory.nameToRGBPlotPredicate(v), "IMAGEPLOT " + v + " unknown");
            } else if (VeryBasicArgsHelper.isArgName(arg, "IMAGEGRADIENT")) {
                current.gradientStr = value(args, ++i, "IMAGEGRADIENT");
            } else if (VeryBasicArgsHelper.isArgName(arg, "IMAGEWIDTH")) {
                current.imageWidth = parseInt("IMAGEWIDTH", value(args, ++i, "IMAGEWIDTH"));
            } else if (VeryBasicArgsHelper.isArgName(arg, "IMAGEHEIGHT")) {
                current.imageHeight = parseInt("IMAGEHEIGHT", value(args, ++i, "IMAGEHEIGHT"));
            } else if (VeryBasicArgsHelper.isArgName(arg, "OFFSETS")) {
                // OFFSETS x y [x y ...] : the integers that follow, taken as (x,y) pairs
                List<Vec2<?>> offsets = new ArrayList<Vec2<?>>();
                while (i + 2 < args.length && isInt(args[i + 1]) && isInt(args[i + 2])) {
                    offsets.add(new Position.Simple(Integer.parseInt(args[i + 2]), Integer.parseInt(args[i + 1])));
                    i += 2;
                }
                if (i + 1 < args.length && isInt(args[i + 1])) {
                    throw new IllegalArgumentException("OFFSETS needs x y pairs (odd number of values)");
                }
                current.offsets = offsets;
            } else if (VeryBasicArgsHelper.isArgName(arg, "NOGRAPHICS") || (i == 0 && "netcat".equalsIgnoreCase(arg))) {
                continue;   // read by TUIApp itself
            } else {
                throw new IllegalArgumentException("unknown option '" + arg + "'");
            }
            lastOptionIndex = optionIndex;
        }

        if (images.isEmpty()) {
            images.add(new ImageSpec(DEFAULT_GIF, current.copy()));
        }
        if (lastOptionIndex > lastImageIndex) {
            System.err.println("WARNING: options after the last IMAGE apply to no image (" + args[lastOptionIndex] + " ...)");
        }
    }

    /** the GIF shown when no IMAGE is given */
    static final String DEFAULT_GIF = "rocket-launch.gif";

    /**
     * IMAGE's value as a file to read: a file path as it is; otherwise a bundled image - by its
     * resource name (/dtb/.../shapes.gif) or just its name (shapes.gif) - as a temporary copy.
     * Neither: the path as given (reported as not found).
     */
    static String asFile(String image) {
        if (new File(image).exists()) {
            return image;
        }
        File bundled = ReadSampleFiles.resourceAsFile(image);
        return bundled != null ? bundled.getPath() : image;
    }

    private static String value(String[] args, int i, String argName) {
        if (i >= args.length) {
            throw new IllegalArgumentException(argName + " needs a value");
        }
        return args[i];
    }

    private static boolean isInt(String s) {
        return s.matches("-?\\d+");
    }

    private static int parseInt(String argName, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(argName + " " + value + " is not a number");
        }
    }

    private static <V> String names(SimpleDisplayValue<V>[] options, V defaultValue) {
        return Stream.of(options)
                     .map(o -> {
                         String name = o.getDisplayValue().contains(" ") ? "\"" + o.getDisplayValue() + "\"" : o.getDisplayValue();
                         return o.getValue() == defaultValue ? name + " (default)" : name;
                     })
                     .collect(Collectors.joining(", "));
    }

    static void printHelp() {
        String cls = ImageSplitterTest.class.getName();
        System.out.println(String.join("\n",
            "ImageSplitterTest - plays animated GIFs (or frames cut from a sprite sheet), each in its own window",
            "",
            "usage: ./runDBG.sh " + cls + " [options] [IMAGE <file> [[options] IMAGE <file> ...]]",
            "",
            "Options are sticky: each applies to every IMAGE that follows it, until given again.",
            "",
            "  IMAGE <file>           image to show; repeat for several windows (none: the bundled rocket-launch.gif).",
            "                         A file, or a bundled image by name: rocket-launch.gif, rocket-sprites.png, shapes.gif,",
            "                         bouncing-ball.gif, solar-system.gif, ball-sprites.png, earth-messenger.jpg.",
            "                         .gif files animate their own",
            "                         frames; other images are cut into frames using OFFSETS / IMAGEWIDTH / IMAGEHEIGHT",
            "  INTERVAL <ms>          time between frames (default " + DEFAULTS.interval + ")",
            "  IMAGERENDER <type>     " + names(ImageViewerFactory.imageRenderTypeOptions(), DEFAULTS.renderType),
            "  IMAGECOLOR <map>       " + names(ImageViewerFactory.colorMap(), DEFAULTS.colorMapping),
            "                         (shortcuts: NOCOLOR, GREYSCALE, ALL)",
            "  IMAGEPLOT <condition>  which pixels to plot: " + names(ImageViewerFactory.plotPointPredicateOptions(), DEFAULTS.plotPredicate),
            "  IMAGEGRADIENT <chars>  characters used by the gradient render types",
            "  OFFSETS x y [x y ...]  sprite sheet only: top-left (x,y) of each frame, in pixels",
            "  IMAGEWIDTH <px>        sprite sheet only: frame width  (default " + DEFAULTS.imageWidth + ")",
            "  IMAGEHEIGHT <px>       sprite sheet only: frame height (default " + DEFAULTS.imageHeight + ")",
            "  NOGRAPHICS             (TUIApp) draw without graphics characters",
            "  HELP | -h              this help",
            "",
            "example (two GIFs in ASCII at 50ms, then a sprite sheet of 4 x 2 frames of 96 x 96 in Braille):",
            "  I=src/main/resources/dtb/terminalIO/examples/sampledata/images",
            "  ./runDBG.sh " + cls + " INTERVAL 50 IMAGERENDER ASCII IMAGE $I/rocket-launch.gif IMAGE $I/shapes.gif \\",
            "      IMAGERENDER BRAILLE IMAGEWIDTH 96 IMAGEHEIGHT 96 \\",
            "      OFFSETS 0 0 96 0 192 0 288 0 0 96 96 96 192 96 288 96 IMAGE $I/rocket-sprites.png"));
    }

    static <T> T NotNull(T t, String error) {
        if (t == null) {
            throw new IllegalArgumentException(error);
        }
        return t;
    }
    
}
