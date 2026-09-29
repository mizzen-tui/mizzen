package dtb.terminalIO.components.plot.image.animate;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import javax.imageio.ImageIO;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.plot.image.ImageViewerFactory;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.IMAGE_RENDER_TYPE;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderRequest;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.ImageRenderer;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBPlotPredicate;
import dtb.terminalIO.components.plot.image.ImageViewerFactory.RGBToAnsiColorControlCode;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.core.Canvas;
import dtb.terminalIO.core.ScreenCanvasBackend;
import dtb.terminalIO.core.TerminalScreen;
import dtb.terminalIO.examples.TestsCommon;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.Vec2;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.TerminalDimensions;

public class ImageSplitter implements ImageIterator {

    Image fullImage;
    List<Vec2<?>> offsets;

    // please note: This is the height/width of image segment in pixels 
    //              It is not meant to represent size in chars
    int height_subImage;
    int width_subImage;

    int currentImageIDX = 0;
    boolean looping = true;

    public ImageSplitter(Image fullImage, Collection<? extends Vec2<?>> offsets, int height_subImage, int width_subImage) {
        super();
        this.fullImage = fullImage;
        this.offsets = new ArrayList<Vec2<?>>(offsets);
        this.height_subImage = height_subImage;
        this.width_subImage = width_subImage;
    }

    public ImageSplitter(Image fullImage, int[] xpos, int[] ypos, int height_subImage, int width_subImage) {
        super();
        this.fullImage = fullImage;
        this.height_subImage = height_subImage;
        this.width_subImage = width_subImage;
        offsets = new ArrayList<Vec2<?>>(xpos.length * ypos.length);
        for (int j=0; j<ypos.length; j++) {
            for (int i=0; i<xpos.length; i++ ) {
                offsets.add(new Position.Simple(ypos[j], xpos[i]));
            }
        }
    }

    public int currentIDX() {
        return currentImageIDX;
    }

    public Vec2<?> getcurrentOffset() {
        int idx = 0; 
        if (currentImageIDX == 0) {
            idx = offsets.size()-1;
        } else {
            idx = currentImageIDX - 1;
        }
        return offsets.get(idx);
    }

    @Override
    public boolean hasNext() {
        return (looping || currentImageIDX < offsets.size()-1);
    }

    @Override
    public BufferedImage next() {
        if (currentImageIDX >= offsets.size()) {
            currentImageIDX = 0;
        }

        Vec2<?> offset = offsets.get(currentImageIDX);
        currentImageIDX++;
        BufferedImage subImage = ((BufferedImage) fullImage).getSubimage((int)offset.getX(), (int)offset.getY(), width_subImage, height_subImage);
        return subImage;
    }

    @Override
    public void close() {
       // NOOP
    }


    public static void main(String[] args) throws Exception {
        
//        if (args.length == 0) {
//            System.err.println("Filename not specified");
//            System.exit(1);
//        }
//        
//        String fileName = args[0]; 
//        BufferedImage image = ImageIO.read(new File(fileName));

        ElapsedTime timer = new ElapsedTime();

        dumbArgsParse(args);

        long parseT = timer.elapsed();

        makeSequence(dumbArgsParse(args));

      //  printImage(dumbArgsParse(args)) ;

      //  System.out.println(parseT + "  " + timer.recalcElapsed());

    }
    
    

    static void printImage(ImageRenderRequest imageRequest) throws Exception {
        ElapsedTime timer = new ElapsedTime();

        TerminalScreen terminal = TestsCommon.getTerminalScreenForTesting(null);

        Canvas canvas = new Canvas(new ScreenCanvasBackend(terminal));

        Dimension termsize = TerminalDimensions.create();
        
        canvas.setSize(termsize.getHeight()-2, termsize.getWidth());
        canvas.setEmptyChar(' ');

        canvas.t(0,2,"file: "+imgFilePath);
        
        printImage(imageRequest, canvas, null);

    }

    static void printImage(ImageRenderRequest imageRequest, Canvas canvas, String msg) throws Exception {
        ElapsedTime timer = new ElapsedTime();

        long t1 = timer.elapsed();

        ImageRenderer viewer = ImageViewerFactory.fromImage(imageRequest);
        long t2 = timer.recalcElapsed() - t1;

        if (msg != null) {
            canvas.t(0,0, msg+"   ");
        }
        canvas.addComponentSegments(2,0,viewer);
        long t3 = timer.recalcElapsed() - (t1 + t2);

        canvas.addComponentSegments(15,20,new BorderedComponent(new TextComponent("As the world churns\nWe need to realize our\nmistakes of the past.")));

        canvas.refresh();

      //  System.out.println(t1 + "  " + t2 + " " + t3 + " " + timer.recalcElapsed());

    }
    
    
    static String DEFAULT_IMG_PATH="/home/dbalme/workspace/tmpImages/car.jpeg";
    static String imgFilePath = DEFAULT_IMG_PATH;
    
    static RGBPlotPredicate plotPredicate = ImageViewerFactory.PLOT_ALWAYS;
    static RGBToAnsiColorControlCode colorMapping = ImageViewerFactory.rgbColorMapDIRECT;
    static IMAGE_RENDER_TYPE  renderType = ImageViewerFactory.IMAGE_RENDER_TYPE.BLOCK;
    static String gradientStr = "ÆÑÊŒØMÉËÈÃÂWQBÅæ#NÁþEÄÀHKRŽœXgÐêqÛŠÕÔA€ßpmãâG¶øðé8ÚÜ$ëdÙýèÓÞÖåÿÒb¥FDñáZPäšÇàhû§ÝkŸ®S9žUTe6µOyxÎ¾f4õ5ôú&aü™2ùçw©Y£0VÍL±3ÏÌóC@nöòs¢u‰½¼‡zJƒ%¤Itocîrjv1lí=ïì<>i7†[¿?×}*{+()\\/»«•¬|!¡÷¦¯—^ª„”“~³º²–°­¹‹›;:’‘‚’˜ˆ¸…·¨´`";
    
    private static ImageRenderRequest dumbArgsParse(String[] args) throws IOException {
        if (args == null || args.length == 0) {
            return null;
        }
        
        
        int xOffset_subImage = -1;
        int yOffset_subImage = -1;
        int height_subImage = -1;
        int width_subImage = -1;
        
        
        for (int i=0; i<args.length; i++) {
            if ("IMAGE".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                imgFilePath=args[i];
            } else if ("IMAGEPLOT".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                plotPredicate = NotNull(ImageViewerFactory.nameToRGBPlotPredicate(args[i]),"IMAGEPLOT "+args[i]+" unknown");
            } else if ("IMAGEGRADIENT".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                gradientStr = args[i];
            } else if ("IMAGECOLOR".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                colorMapping = NotNull(ImageViewerFactory.nameToRGBToAnsiColorControlCode(args[i]),"IMAGECOLOR "+args[i]+" unknown");
            } else if ("IMAGERENDER".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                renderType = NotNull(ImageViewerFactory.nameToImageRenderType(args[i]),"IMAGERENDER "+args[i]+" unknown");
            } else if ("X".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                xOffset_subImage = Integer.parseInt(args[i]);
            } else if ("Y".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                yOffset_subImage = Integer.parseInt(args[i]);
            } else if ("HEIGHT".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                height_subImage = Integer.parseInt(args[i]);
            } else if ("WIDTH".equalsIgnoreCase(args[i]) && i+1<args.length) {
                i++;
                width_subImage = Integer.parseInt(args[i]);
            }
        }
        
        BufferedImage image = ImageIO.read(new File(imgFilePath));
        
//        if (xOffset_subImage > -1 && yOffset_subImage > -1 && height_subImage > -1 && width_subImage > -1) {
//            image = image.getSubimage(xOffset_subImage, yOffset_subImage, width_subImage, height_subImage);
//        }
        
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        
        float heightToWidthRatio = ((float)imageHeight) / ((float)imageWidth);
        float widthToHeightRatio = ((float)imageWidth)/ ((float)imageHeight);

        Dimension termsize = TerminalDimensions.create();
        Dimension imgSize = new Dimension.Simple(termsize.getHeight()-4, Math.min((int)((termsize.getHeight()-4)*widthToHeightRatio)*2, termsize.getWidth()));

        ImageRenderRequest imageRequest = new ImageRenderRequest();
        imageRequest.setDesiredImgSizeInCharacters(imgSize);

        
        
        imageRequest.setColorMap(colorMapping);
        imageRequest.setImageRenderType(renderType);
        imageRequest.setToPlotOrNot(plotPredicate);
        imageRequest.setImageToView(image);
        imageRequest.setGradientStr(gradientStr);
        
        return imageRequest;
        
    }
    
    static ImageRenderRequest makeImgRQST(int xOffset_subImage, 
                                          int yOffset_subImage,
                                          int height_subImage,
                                          int width_subImage,
                                          BufferedImage image,
                                          ImageRenderRequest imageRequest) {
        
        
        if (xOffset_subImage > -1 && yOffset_subImage > -1 && height_subImage > -1 && width_subImage > -1) {
            image = image.getSubimage(xOffset_subImage, yOffset_subImage, width_subImage, height_subImage);
        }
        
        imageRequest.setImageToView(image);
        
        return imageRequest;
    }
    
    static void makeSequence(ImageRenderRequest imageRequest) throws Exception {
        TerminalScreen terminal = TestsCommon.getTerminalScreenForTesting(null);

        Canvas canvas = new Canvas(new ScreenCanvasBackend(terminal));
        canvas.setEmptyChar(' ');
        
        BufferedImage fullImage = (BufferedImage) imageRequest.getImageToView();
        

        Dimension termsize = TerminalDimensions.create();
        
        canvas.setSize(termsize.getHeight()-2, termsize.getWidth());
        
        //int incrX = 96-36;
        
        //int startX = 36;
        
        //int currentX = startX;
        
        int interval = 200;
        
        int[] xpos = new int[] {36 , 
                                96,
                                155,
                                220,
                                };
        
        int[] ypos = new int[] {24 , 
                                77,
                                132,
                                191,
                                247,
                                303,
                                };
        

        ImageSplitter splitter = new ImageSplitter(fullImage, xpos, ypos, 47, 46);

        StringJoiner sj = new StringJoiner(" ");
        splitter.offsets.stream().forEach(o ->  sj.add( ((int)o.getX())+" " + ((int)o.getY())  ));
        System.out.println(sj);

        System.exit(0);

        ImageAnimatorComponent animatorComponent = new ImageAnimatorComponent(Colors.DEFAULT_COLOR, imageRequest, splitter, imageRequest.getDesiredImgSizeInCharacters());
        
        while (true) {
            animatorComponent.showNext();
//            imageRequest.setImageToView(splitter.next());
            canvas.addComponentSegments(2,0,animatorComponent);
            canvas.refresh();
//            printImage(imageRequest, canvas, null);
            Thread.currentThread().sleep(interval);
            
//            for (int j=0; j<ypos.length; j++) {
//                    for (int i=0; i<xpos.length; i++ ) {
//                        imageRequest = makeImgRQST(xpos[i], ypos[j], 47, 46, fullImage, imageRequest);
//                        printImage(imageRequest, canvas, String.format("(%d,%d)", j,i));
//                        Thread.currentThread().sleep(interval);
//                    }
//            }
        }
        
//        imageRequest = makeImgRQST(currentX, 24, 47, 46, fullImage, imageRequest);
//        printImage(imageRequest, canvas);
//        
//        Thread.currentThread().sleep(interval);
//
//        imageRequest = makeImgRQST(currentX, 24, 47, 46, fullImage, imageRequest);
//        printImage(imageRequest, canvas);
//
//        Thread.currentThread().sleep(interval);
//
//        imageRequest = makeImgRQST(currentX, 24, 47, 46, fullImage, imageRequest);
//        printImage(imageRequest, canvas);
        
        
    }
    
    
    static <T> T NotNull(T t, String error) {
        if (t == null) {
            throw new IllegalArgumentException(error);
        }
        return t;
    }


    
    
}
