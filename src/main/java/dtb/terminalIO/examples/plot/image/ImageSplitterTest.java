package dtb.terminalIO.examples.plot.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.Vec2;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.TerminalDimensions;

public class ImageSplitterTest {

    public static void main(String[] args) throws Exception {

       Logging.setEnabled(true);

        Dimension termsize = TerminalDimensions.create();

        ImageRenderRequest rqst = dumbArgsParse(args);

        if (imgFilePathes.size()<1) {
            System.err.println("No image files specified");
            System.exit(1);
        }
        
        new TUIApp(args)
           .setCanvasSize(termsize)
           .setDefaultMainApplicationPanel()
           .confirmOnExit()
           .setOnReadyStart((tApp)->{

               
               
               int offset = 0;
               
               for (String imgFilePath : imgFilePathes) {
                   ImageIterator imageIterator;
                   
                   try {
                   BufferedImage image = ImageIO.read(new File(imgFilePath));

                   int imageWidth = image.getWidth();
                   int imageHeight = image.getHeight();
                   
                   float widthToHeightRatio = ((float)imageWidth)/ ((float)imageHeight);

                   Dimension imgSize = new Dimension.Simple(termsize.getHeight()-8, Math.min((int)((termsize.getHeight()-4)*widthToHeightRatio)*2, termsize.getWidth()-40));

                   ImageRenderRequest imageRequest = new ImageRenderRequest();
                   imageRequest.setDesiredImgSizeInCharacters(imgSize);

                   imageRequest.setColorMap(colorMapping);
                   imageRequest.setImageRenderType(renderType);
                   imageRequest.setToPlotOrNot(plotPredicate);
                   imageRequest.setImageToView(image);
                   imageRequest.setGradientStr(gradientStr);

                   
                   if (imgFilePath.endsWith(".gif")) {
                        imageIterator = new AnimatedGifImageIterator(imgFilePath);
                   } else {
                       imageIterator = new ImageSplitter(NotNull(imageRequest, "Image spec's not defined").getImageToView(), 
                                                         NotNull(offsets, "Image offset's not defined"), 
                                                         image_height, 
                                                         image_width);
                   }
                   
                   MovingPicturesViewer movingPicturesViewer = new MovingPicturesViewer(Colors.DEFAULT_COLOR, imageIterator, imageRequest); 
                   
                   movingPicturesViewer.setBorder(false);
                   
                   WindowFrame wf = new WindowFrame(movingPicturesViewer.getColor(), imgFilePath, movingPicturesViewer);
                   
                   
                   tApp.getMainApplicationPanel().add(offset, offset, wf);
                   movingPicturesViewer.start(tApp.getEventBus(), interval);
                   
                   offset++;

                   } catch (IOException e) {
                       throw new RuntimeException(e);
                   }

               }

           }).start();

    }
    
    public static class ImageSplitterViewer {
        
    }


    static String title = "Rotating earth";
    static int interval = 250;
    //static String DEFAULT_IMG_PATH="/home/dbalme/workspace/tmpImages/car.jpeg";
    static List<String> imgFilePathes =  new ArrayList<String>(); // DEFAULT_IMG_PATH;

    static List<Vec2<?>> offsets;

    static int image_width = 47;
    static int image_height = 46;

    static RGBPlotPredicate plotPredicate = ImageViewerFactory.PLOT_ALWAYS;
    static RGBToAnsiColorControlCode colorMapping = ImageViewerFactory.rgbColorMapDIRECT;
    static IMAGE_RENDER_TYPE  renderType = ImageViewerFactory.IMAGE_RENDER_TYPE.BLOCK;
    static String gradientStr = "ÆÑÊŒØMÉËÈÃÂWQBÅæ#NÁþEÄÀHKRŽœXgÐêqÛŠÕÔA€ßpmãâG¶øðé8ÚÜ$ëdÙýèÓÞÖåÿÒb¥FDñáZPäšÇàhû§ÝkŸ®S9žUTe6µOyxÎ¾f4õ5ôú&aü™2ùçw©Y£0VÍL±3ÏÌóC@nöòs¢u‰½¼‡zJƒ%¤Itocîrjv1lí=ïì<>i7†[¿?×}*{+()\\/»«•¬|!¡÷¦¯—^ª„”“~³º²–°­¹‹›;:’‘‚’˜ˆ¸…·¨´`";

    private static ImageRenderRequest dumbArgsParse(String[] args) throws IOException {
        if (args == null || args.length == 0) {
            return null;
        }

        boolean readingOffsets = false;


        int x = -1;
        //int y = -1;

        for (int i=0; i<args.length; i++) {
            if ("IMAGE".equalsIgnoreCase(args[i]) && i+1<args.length) {
                readingOffsets = false;
                i++;
                imgFilePathes.add(args[i]);
            } else if ("TITLE".equalsIgnoreCase(args[i]) && i+1<args.length) {
                readingOffsets = false;
                i++;
                title = args[i];
            } else if ("IMAGEPLOT".equalsIgnoreCase(args[i]) && i+1<args.length) {
                readingOffsets = false;
                i++;
                plotPredicate = NotNull(ImageViewerFactory.nameToRGBPlotPredicate(args[i]),"IMAGEPLOT "+args[i]+" unknown");
            } else if ("IMAGEGRADIENT".equalsIgnoreCase(args[i]) && i+1<args.length) {
                readingOffsets = false;
                i++;
                gradientStr = args[i];
            } else if ("IMAGECOLOR".equalsIgnoreCase(args[i]) && i+1<args.length) {
                readingOffsets = false;
                i++;
                colorMapping = NotNull(ImageViewerFactory.nameToRGBToAnsiColorControlCode(args[i]),"IMAGECOLOR "+args[i]+" unknown");
            } else if ("IMAGERENDER".equalsIgnoreCase(args[i]) && i+1<args.length) {
                readingOffsets = false;
                i++;
                renderType = NotNull(ImageViewerFactory.nameToImageRenderType(args[i]),"IMAGERENDER "+args[i]+" unknown");
            } else if ("INTERVAL".equalsIgnoreCase(args[i]) && i+1<args.length) {
                readingOffsets = false;
                i++;
                interval = Integer.parseInt(args[i]);
            } else if ("IMAGEWIDTH".equalsIgnoreCase(args[i]) && i+1<args.length) {
                readingOffsets = false;
                i++;
                image_width = Integer.parseInt(args[i]);
            } else if ("IMAGEHEIGHT".equalsIgnoreCase(args[i]) && i+1<args.length) {
                readingOffsets = false;
                i++;
                image_height = Integer.parseInt(args[i]);
            } else if ("OFFSETS".equalsIgnoreCase(args[i]) && i+1<args.length) {
                readingOffsets = true;
            } else {
                if (!readingOffsets) {
                    continue;
                }
                int value = Integer.parseInt(args[i]);
                if (x == -1) {
                    x = value;
                } else {
                    int y = value;
                    if (offsets == null) {
                        offsets = new ArrayList<Vec2<?>>();
                    }
                    offsets.add(new Position.Simple(y,x));
                    x = -1;
                }
            }
        }

        //BufferedImage image = ImageIO.read(new File(imgFilePath));

//        int imageWidth = image.getWidth();
//        int imageHeight = image.getHeight();

        //float heightToWidthRatio = ((float)imageHeight) / ((float)imageWidth);
//        float widthToHeightRatio = ((float)imageWidth)/ ((float)imageHeight);

//        Dimension termsize = TerminalDimensions.create();
//        Dimension imgSize = new Dimension.Simple(termsize.getHeight()-8, Math.min((int)((termsize.getHeight()-4)*widthToHeightRatio)*2, termsize.getWidth()-40));
//
//        ImageRenderRequest imageRequest = new ImageRenderRequest();
//        imageRequest.setDesiredImgSizeInCharacters(imgSize);
//
//        imageRequest.setColorMap(colorMapping);
//        imageRequest.setImageRenderType(renderType);
//        imageRequest.setToPlotOrNot(plotPredicate);
//        //imageRequest.setImageToView(image);
//        imageRequest.setGradientStr(gradientStr);

        return null;

    }

    static <T> T NotNull(T t, String error) {
        if (t == null) {
            throw new IllegalArgumentException(error);
        }
        return t;
    }
    
}
