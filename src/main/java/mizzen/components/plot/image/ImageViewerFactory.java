package mizzen.components.plot.image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;

import mizzen.components.AbstractComponentWrappper;
import mizzen.components.Component;
import mizzen.components.TextComponent;
import mizzen.components.input.DisplayableValue;
import mizzen.components.input.DisplayableValue.SimpleDisplayValue;
import mizzen.components.input.DisplayableValue.StringDisplayValue;
import mizzen.components.plot.Plotter;
import mizzen.components.plot.blocks.BlocksGrid;
import mizzen.components.plot.braille.BrailleGrid;
import mizzen.components.plot.braille.BrailleUtils;
import mizzen.controlcodes.color.Color;
import mizzen.controlcodes.color.Colors;
import mizzen.geom.CanResize;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;

public class ImageViewerFactory {

    public static enum ASCII_IMAGE_STYLE {
        ASCII_GRADIENT1,
        ASCII_GRADIENT2,
        ASCII_GRADIENT3,
        ASCII_GRADIENT_PRECOMUTE1,
        ASCII_GRADIENT_PRECOMUTE2,
        ASCII_GRADIENT_PRECOMUTE3
    }
    
    
    public static DisplayableValue<String>[] standardAsciiGradients() {
        return StringDisplayValue.fromStringValues(new String[] {
                   SIMPLE_char_gradientStr1,
                   SIMPLE_char_gradientStr2,
                   SIMPLE_char_gradientStr3,
                   //" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"
                 //  "$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^`'. ",
                   "@%#*+=-:. ", 
                   "##@$%WRHGo\"~;:'. ",
                   "#",
                   "#$%WHOo.",
                   "/",
                   "-",
                   "====--- ",
                   "===---.. ",
                   "###||||!!!!;;;::: ",
                   "|",
                   "▌",
                   "Oo.",
                   "o",
                   "o●●" ,
                    "●●o" ,
                   ".✷",
                   "@@@@@@@OOOOoooo::::....",
                   "▓" + "▒" + "░" + "⣯" + "⠹" + "⠸" + "⠰",
                   "▉"+"▊"+"▋"+"▌"+"▍"+"▎"
                                                                }, 15);
    }
    
    public static class ColorMapOption extends SimpleDisplayValue<RGBToAnsiColorControlCode> {
        public ColorMapOption(String displayValue, RGBToAnsiColorControlCode value) {
            super(displayValue, value);
        }
    }   

    public static ColorMapOption[] colorMap() {
        ColorMapOption[] colorMaps = new ColorMapOption[] {
                 new ColorMapOption("rgbColorMapDIRECT"    , rgbColorMapDIRECT),
                 new ColorMapOption("rgbColorPlotBlack"    , rgbColorPlotBlack),
                 new ColorMapOption("rgbColorPlotBlackAndGreyAsWhite", rgbColorPlotBlackAndGreyAsWhite),
                 new ColorMapOption("rgbColorMapSimple1"   , rgbColorMapSimple1),
                 new ColorMapOption("rgbGreyScale"         , rgbGreyScale),
                 new ColorMapOption("rgbGreyScale-small"   , rgbGreyScaleSmall),
                 new ColorMapOption("rgbGreyScale-smaller" , rgbGreyScaleSmaller),
                 new ColorMapOption("rgbGreyScale-smallest", rgbGreyScaleSmallest),
                 new ColorMapOption("No Color (default)"   , rgbColorMapRCol)
        }; 
        return colorMaps;
    }

    public static RGBToAnsiColorControlCode nameToRGBToAnsiColorControlCode(String name) {
        String colorName=name;
        if (colorName.equalsIgnoreCase("NOCOLOR")) {
            colorName = "No Color (default)";
        }
        if (colorName.equalsIgnoreCase("GREYSCALE")) {
            colorName = "rgbGreyScale";
        }
        if (colorName.equalsIgnoreCase("ALL")) {
            colorName = "rgbColorMapDIRECT";
        }
        for (ColorMapOption option : colorMap()) {
            if (colorName.equalsIgnoreCase(option.getDisplayValue())) {
                return option.getValue();
            }
        }
        return null;
    }
     
    public static class PlotPredicateOption extends SimpleDisplayValue<RGBPlotPredicate> {
        public PlotPredicateOption(String displayValue, RGBPlotPredicate value) {
            super(displayValue, value);
        }
    }
    
    static private PlotPredicateOption[] predicateOptions = null; 
    static {
        
    }
    
/*
 *     public static RGBPlotPredicate PLOT_LIGHTER_THAN_25  = plotLighterThan(25);
    public static RGBPlotPredicate PLOT_LIGHTER_THAN_50  = plotLighterThan(50);
    public static RGBPlotPredicate PLOT_LIGHTER_THAN_75  = plotLighterThan(75);
    public static RGBPlotPredicate PLOT_LIGHTER_THAN_100 = plotLighterThan(100);
    public static RGBPlotPredicate PLOT_LIGHTER_THAN_125 = plotLighterThan(125);
    public static RGBPlotPredicate PLOT_LIGHTER_THAN_150 = plotLighterThan(150);
    public static RGBPlotPredicate PLOT_LIGHTER_THAN_200 = plotLighterThan(200);
    public static RGBPlotPredicate PLOT_DARKER_THAN_50   = plotDarkerThan(50);
    public static RGBPlotPredicate PLOT_DARKER_THAN_75   = plotDarkerThan(75);
    public static RGBPlotPredicate PLOT_DARKER_THAN_125  = plotDarkerThan(125);
    public static RGBPlotPredicate PLOT_DARKER_THAN_150  = plotDarkerThan(150);
    public static RGBPlotPredicate PLOT_DARKER_THAN_200  = plotDarkerThan(200);
    public static RGBPlotPredicate PLOT_DARKER_THAN_225  = plotDarkerThan(225);
    public static RGBPlotPredicate PLOT_DARKER_THAN_240  = plotDarkerThan(240);
    public static RGBPlotPredicate PLOT_DARKER_THAN_250  = plotDarkerThan(250);
    public static RGBPlotPredicate PLOT_DARKER_THAN_255  = plotDarkerThan(255);

 * 
 * */    
    
    private static PlotPredicateOption[] __PointPredicateOptions = null;
    
    public static PlotPredicateOption[] plotPointPredicateOptions() {
        if (__PointPredicateOptions != null) {
            return __PointPredicateOptions;
        }
        PlotPredicateOption[] predicateOptions = new PlotPredicateOption[] {
                 new PlotPredicateOption("PLOT_ALWAYS",PLOT_ALWAYS),
                 new PlotPredicateOption("PLOT_WHITE_ONLY",PLOT_WHITE_ONLY),
                 new PlotPredicateOption("PLOT_BLACK_ONLY",PLOT_BLACK_ONLY),
                 new PlotPredicateOption("PLOT_DARK_ONLY", PLOT_DARK_ONLY),
                 new PlotPredicateOption("PLOT_DARKISH_ONLY",PLOT_DARKISH_ONLY),
                 new PlotPredicateOption("PLOT_LIGHT_ONLY",PLOT_LIGHT_ONLY),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_25",PLOT_LIGHTER_THAN_25),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_30",plotLighterThan(30)),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_35",plotLighterThan(35)),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_40",plotLighterThan(40)),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_45",plotLighterThan(45)),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_50",PLOT_LIGHTER_THAN_50),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_55",plotLighterThan(55)),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_60",plotLighterThan(60)),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_65",plotLighterThan(65)),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_70",plotLighterThan(70)),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_75",PLOT_LIGHTER_THAN_75),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_80",plotLighterThan(80)),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_85",plotLighterThan(85)),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_90",plotLighterThan(90)),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_95",plotLighterThan(95)),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_100",PLOT_LIGHTER_THAN_100),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_125",PLOT_LIGHTER_THAN_125),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_150",PLOT_LIGHTER_THAN_150),
                 new PlotPredicateOption("PLOT_LIGHTER_THAN_200",PLOT_LIGHTER_THAN_200),
                 new PlotPredicateOption("PLOT_DARKER_THAN_50",PLOT_DARKER_THAN_50),
                 new PlotPredicateOption("PLOT_DARKER_THAN_75",PLOT_DARKER_THAN_75),
                 new PlotPredicateOption("PLOT_DARKER_THAN_125",PLOT_DARKER_THAN_125),
                 new PlotPredicateOption("PLOT_DARKER_THAN_150",PLOT_DARKER_THAN_150),
                 new PlotPredicateOption("PLOT_DARKER_THAN_200",PLOT_DARKER_THAN_200),
                 new PlotPredicateOption("PLOT_DARKER_THAN_225",PLOT_DARKER_THAN_225),
                 new PlotPredicateOption("PLOT_DARKER_THAN_240",PLOT_DARKER_THAN_240),
                 new PlotPredicateOption("PLOT_DARKER_THAN_250",PLOT_DARKER_THAN_250),
                 new PlotPredicateOption("PLOT_DARKER_THAN_255",PLOT_DARKER_THAN_255),
                 
        }; 
        __PointPredicateOptions = predicateOptions;
        return predicateOptions;
    }

    public static RGBPlotPredicate nameToRGBPlotPredicate(String name) {
        for (PlotPredicateOption option : plotPointPredicateOptions()) {
            if (name.equalsIgnoreCase(option.getDisplayValue())) {
                return option.getValue();
            }
        }
        return null;
    }
    
    
    public static class ImageRenderTypeOption extends SimpleDisplayValue<IMAGE_RENDER_TYPE> {
        public ImageRenderTypeOption(String displayValue, IMAGE_RENDER_TYPE value) {
            super(displayValue, value);
        }
    }   
 
    public static ImageRenderTypeOption[] imageRenderTypeOptions() {
        ImageRenderTypeOption[] imageRenderTypes = new ImageRenderTypeOption[] {
                 new ImageRenderTypeOption("Ascii",IMAGE_RENDER_TYPE.ASCII),
                 new ImageRenderTypeOption("Braille",IMAGE_RENDER_TYPE.BRAILLE),
                 new ImageRenderTypeOption("Block",IMAGE_RENDER_TYPE.BLOCK)
        }; 
        return imageRenderTypes;
    }

    public static IMAGE_RENDER_TYPE nameToImageRenderType(String name) {
        for (ImageRenderTypeOption option : imageRenderTypeOptions()) {
            if (name.equalsIgnoreCase(option.getDisplayValue())) {
                return option.getValue();
            }
        }
        return null;
    }

    public static class ImageRenderer extends AbstractComponentWrappper<Component<?>> {

        public ImageRenderer(Component<?> wrappedComponent) {
            super(wrappedComponent.getColor(), wrappedComponent);
            this.setFillOnClear(false);
        }

        void rebuildWrappedComponentIfNeeded(ImageRenderRequest rqst) {
            if (!(this.getWrappedComponent() instanceof BlocksGrid) && rqst.isBlocksType()) {
                BlocksGrid grid = new BlocksGrid(this.getWrappedComponent().getColor(), (Dimension)this.getWrappedComponent());
                this.setWrappedComponent(grid);
            } else if (!(this.getWrappedComponent() instanceof BrailleGrid) && rqst.isBrailleType()) {
                BrailleGrid grid = new BrailleGrid(this.getWrappedComponent().getColor(), (Dimension)this.getWrappedComponent());
                this.setWrappedComponent(grid);
            } else if (!(this.getWrappedComponent() instanceof Plotter) && (!rqst.isBrailleType()) && (!rqst.isBlocksType())) {
                Plotter plotter = new Plotter(this.getWrappedComponent().getColor(), (Dimension)this.getWrappedComponent());
                this.setWrappedComponent(plotter);
            }
        }

        @Override
        public void resize(Dimension d) {
            if (this.isSameDimension(d)) {
                return ;
            }
            if (getWrappedComponent() instanceof CanResize) {
                ((CanResize) getWrappedComponent()).resize(d);
            }
            if (this.getParentContainer() != null) {
                this.getParentContainer().childResized(this);
            }
        }
    }

    public static enum IMAGE_RENDER_TYPE {
        ASCII,
        BRAILLE,
        BLOCK
    }

    public static class ImageRenderRequest {

        IMAGE_RENDER_TYPE imageRenderType = IMAGE_RENDER_TYPE.ASCII;
        Image imageToView;
        Dimension desiredImgSizeInCharacters;
        RGBPlotPredicate toPlotOrNot;
        RGBToAnsiColorControlCode colorMap;
        String gradientStr;

        public Image getImageToView() {
            return imageToView;
        }

        public ImageRenderRequest setImageToView(Image imageToView) {
            this.imageToView = imageToView;
            return this;
        }

        public Dimension getDesiredImgSizeInCharacters() {
            return desiredImgSizeInCharacters;
        }

        public ImageRenderRequest setDesiredImgSizeInCharacters(Dimension desiredImgSizeInCharacters) {
            this.desiredImgSizeInCharacters = desiredImgSizeInCharacters;
            return this;
        }

        public RGBPlotPredicate getToPlotOrNot() {
            return toPlotOrNot != null ? toPlotOrNot : plotPointPredicateOptions()[0].getValue();
        }

        public ImageRenderRequest setToPlotOrNot(RGBPlotPredicate toPlotOrNot) {
            this.toPlotOrNot = toPlotOrNot;
            return this;
        }

        public RGBToAnsiColorControlCode getColorMap() {
            return colorMap != null ? colorMap : colorMap()[0].getValue();
        }

        public ImageRenderRequest setColorMap(RGBToAnsiColorControlCode colorMap) {
            this.colorMap = colorMap;
            return this;
        }

        public IMAGE_RENDER_TYPE getImageRenderType() {
            return imageRenderType;
        }

        public void setImageRenderType(IMAGE_RENDER_TYPE imageRenderType) {
            this.imageRenderType = imageRenderType;
        }

        public boolean isAsciiType() {
            return this.imageRenderType.equals(IMAGE_RENDER_TYPE.ASCII);
        }

        public boolean isBrailleType() {
            return this.imageRenderType.equals(IMAGE_RENDER_TYPE.BRAILLE);
        }

        public boolean isBlocksType() {
            return this.imageRenderType.equals(IMAGE_RENDER_TYPE.BLOCK);
        }

        public String getGradientStr() {
            return gradientStr != null ? gradientStr : standardAsciiGradients()[0].getValue();
        }

        public ImageRenderRequest setGradientStr(String gradientStr) {
            this.gradientStr = gradientStr;
            return this;
        }

        public ImageRenderRequest copyFrom(ImageRenderRequest srcRqst) {
            this.imageRenderType = srcRqst.imageRenderType;
            this.desiredImgSizeInCharacters = srcRqst.desiredImgSizeInCharacters;
            this.toPlotOrNot = srcRqst.toPlotOrNot;
            this.colorMap = srcRqst.getColorMap();
            this.gradientStr = srcRqst.gradientStr;
            return this;
        }

        public ImageRenderRequest copyAllFrom(ImageRenderRequest srcRqst) {
            this.copyFrom(srcRqst);
            this.imageToView = srcRqst.imageToView;
            return this;
        }

        public boolean isSameRequest(ImageRenderRequest other) {
            if (this == other)
                return true;
            if (other == null)
                return false;
            if (colorMap == null) {
                if (other.colorMap != null)
                    return false;
            } else if (!colorMap.equals(other.colorMap))
                return false;
            if (desiredImgSizeInCharacters == null) {
                if (other.desiredImgSizeInCharacters != null)
                    return false;
            } else if (!desiredImgSizeInCharacters.isSameDimension(other.desiredImgSizeInCharacters))
                return false;
            if (gradientStr == null) {
                if (other.gradientStr != null)
                    return false;
            } else if (!gradientStr.equals(other.gradientStr))
                return false;
            if (imageRenderType != other.imageRenderType)
                return false;
            if (imageToView == null) {
                if (other.imageToView != null)
                    return false;
            } else if (!imageToView.equals(other.imageToView))
                return false;
            if (toPlotOrNot == null) {
                if (other.toPlotOrNot != null)
                    return false;
            } else if (!toPlotOrNot.equals(other.toPlotOrNot))
                return false;
            return true;
        }

    }

    public static ImageRenderer emptyImage(Color color) {
        return new ImageRenderer(new TextComponent(color, ""));
    }

    public static ImageRenderer fromImage(ImageRenderRequest rqst) throws Exception {

        if (rqst.isBlocksType()) {
            return new ImageRenderer(simpleImageToBlocksView(rqst.getImageToView(), 
                                                             rqst.getDesiredImgSizeInCharacters(), 
                                                             rqst.getToPlotOrNot(), 
                                                             rqst.getColorMap()));
       }

        if (rqst.isBrailleType()) {
            return new ImageRenderer(simpleImageToBrailleView(rqst.getImageToView(), 
                                                              rqst.getDesiredImgSizeInCharacters(), 
                                                              rqst.getToPlotOrNot(), 
                                                              rqst.getColorMap()));
       }

        return new ImageRenderer(simpleImageToStringView(rqst.getImageToView(), 
                                                         rqst.getDesiredImgSizeInCharacters(), 
                                                         rqst.getToPlotOrNot(), 
                                                         rqst.getColorMap(),
                                                         rqst.getGradientStr()));

    }

    public static Component<?> fromImage(Component<?> imageViewer, ImageRenderRequest rqst) throws Exception {

        ImageRenderer viewer = (ImageRenderer) imageViewer;
        viewer.rebuildWrappedComponentIfNeeded(rqst);

        if (rqst.isBlocksType()) {
            return simpleImageToBlocksView(viewer.getWrappedComponent(),
                                           rqst.getImageToView(), 
                                           rqst.getDesiredImgSizeInCharacters(), 
                                           rqst.getToPlotOrNot(), 
                                           rqst.getColorMap());
        }

        if (rqst.isBrailleType()) {
            return simpleImageToBrailleView(viewer.getWrappedComponent(),
                                            rqst.getImageToView(), 
                                            rqst.getDesiredImgSizeInCharacters(), 
                                            rqst.getToPlotOrNot(), 
                                            rqst.getColorMap());
        }

        return simpleImageToStringView(viewer.getWrappedComponent(),
                                       Position.ZERO,
                                       rqst.getImageToView(), 
                                       rqst.getDesiredImgSizeInCharacters(), 
                                       rqst.getToPlotOrNot(), 
                                       rqst.getColorMap(),
                                       rqst.getGradientStr());
    }

    public static Component<?> fromImage(Image imageToView, Dimension desiredImgSizeInCharacters) throws Exception {
        return simpleImageToStringView(imageToView, desiredImgSizeInCharacters);
    }

    public static Component<?> simpleImageToStringView(Image inputImage, Dimension desiredImgSizeInCharacters) throws Exception {
        return simpleImageToStringView(inputImage, desiredImgSizeInCharacters, plotPointPredicateOptions()[0].getValue(),  colorMap()[0].getValue(), standardAsciiGradients()[0].getValue());
    }

    public static Component<?> simpleImageToStringView(Image inputImage, Dimension desiredImgSizeInCharacters, String gradientStr) throws Exception {
        return simpleImageToStringView(inputImage, desiredImgSizeInCharacters, plotPointPredicateOptions()[0].getValue(),  colorMap()[0].getValue(), gradientStr);
    }

    public static Component<?> simpleImageToStringView(Image inputImage, 
                                                       Dimension desiredImgSizeInCharacters, 
                                                       RGBPlotPredicate toPlotOrNot, 
                                                       RGBToAnsiColorControlCode colorMap, 
                                                       String gradientStr) throws Exception {

        Plotter plotterComponent = new Plotter(Colors.On_Whi, desiredImgSizeInCharacters);
        plotterComponent.setEmptyColor(Colors.Whi);
        plotterComponent.setEmptyChar(' ');
        plotterComponent.setFillOnClear(false);
        plotterComponent.setName("imageViewer-plotter");

        return simpleImageToStringView(plotterComponent,
                                       Position.ZERO,
                                       inputImage, 
                                       desiredImgSizeInCharacters, 
                                       toPlotOrNot, 
                                       colorMap, 
                                       gradientStr);
    }

    public static Component<?> simpleImageToStringView(Component<?> plotterDestination,
                                                       Position offset,
                                                       Image inputImage, 
                                                       Dimension desiredImgSizeInCharacters, 
                                                       RGBPlotPredicate toPlotOrNot, 
                                                       RGBToAnsiColorControlCode colorMap, 
                                                       String gradientStr) throws Exception {

        Plotter plotterComponent = (Plotter) plotterDestination;
        plotterComponent.clearPlots();
        plotterComponent.clear();

        Image image = resize(inputImage, desiredImgSizeInCharacters, ASCII_ADJUST_TO_CONTAINER_SIZE_PRESERVE_ASPECT_RATIO);
        //Image image = resize(inputImage, desiredImgSizeInCharacters); //, ADJUST_TO_CONTAINER_SIZE);

        Rectangle imgSize = new ImageRectangleAdapter(image);

        int width = imgSize.getWidth();
        int height = imgSize.getHeight();

        int[] pixels = new int[width * height];

        PixelGrabber pxGrabber = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);

        pxGrabber.grabPixels();

        int row_offset = offset.getRow();
        int col_offset = offset.getCol();

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
               int pixel = pixels[row * width + col];

               int blue  = (pixel & 0xff);
               int green = ((pixel & 0xff00) >> 8);
               int red   = ((pixel & 0xff0000) >> 16);

               //if (pixel != -1 && pixel != 0) {
               if (toPlotOrNot.test(red, green, blue)) {

                   Color mappedColor = colorMap.toControlCode(red, green, blue);

                   char plotChar = rgbToChar(red, green, blue, gradientStr);
                   plotterComponent.addPlot(row + row_offset, col + col_offset, plotChar, mappedColor);
                   //plotter.setColor(mappedColor);
                   //plotter.plot(col, row);
               }
            }
        }
        return plotterComponent;
    }

    public static Component<?> simpleImageToBrailleView(Image imageToView, Dimension desiredImgSizeInCharacters) throws Exception {
        return simpleImageToBrailleView(imageToView, desiredImgSizeInCharacters, plotPointPredicateOptions()[0].getValue(),  colorMap()[0].getValue());
    }

    public static Component<?> simpleImageToBrailleView(Component<?> plotterDestination, Image imageToView, Dimension desiredImgSizeInCharacters) throws Exception {
        return simpleImageToBrailleView(plotterDestination, imageToView, desiredImgSizeInCharacters, plotPointPredicateOptions()[0].getValue(),  colorMap()[0].getValue());
    }

    public static Component<?> simpleImageToBrailleView(Component<?> plotterDestination, Image imageToView) throws Exception {
        return simpleImageToBrailleView(plotterDestination, imageToView, plotterDestination.getPreferredRectangleForLayout(), plotPointPredicateOptions()[0].getValue(),  colorMap()[0].getValue());
    }

    //TODO:  Copy the braille stuff - but make specific for blocksgrid 
    //       optimize it!!

    public static Component<?> simpleImageToBrailleView(Image inputImage, Dimension desiredImgSizeInCharacters, RGBPlotPredicate toPlotOrNot, RGBToAnsiColorControlCode colorMap) throws Exception {

        BrailleGrid brailleGrid = new BrailleGrid(Colors.On_Bla, desiredImgSizeInCharacters);
        brailleGrid.setEmptyColor(Colors.Whi);
        brailleGrid.setEmptyChar(' ');
        brailleGrid.setFillOnClear(false);
        brailleGrid.setName("imageViewer-brailleGrid");

        return simpleImageToBrailleView(brailleGrid, 
                                        inputImage, 
                                        desiredImgSizeInCharacters,
                                        toPlotOrNot,
                                        colorMap);
    }

    public static Component<?> simpleImageToBrailleView(Component<?> plotterDestination,
                                                        Image inputImage, 
                                                        Dimension desiredImgSizeInCharacters, 
                                                        RGBPlotPredicate toPlotOrNot, 
                                                        RGBToAnsiColorControlCode colorMap) throws Exception {

        BrailleGrid brailleGrid = (BrailleGrid) plotterDestination;

        brailleGrid.getPlotter().clearPlots();
        brailleGrid.clear();

        Image image = resize(inputImage, brailleGrid.getGridRectangle());

        Rectangle imgSize = new ImageRectangleAdapter(image);

        int width = imgSize.getWidth();
        int height = imgSize.getHeight();

        int[] pixels = new int[width * height];

        PixelGrabber pxGrabber = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);

        pxGrabber.grabPixels();

        int charsWidth  = width / 2 ;  // desiredImgSizeInCharacters.getWidth();
        int charsHeight = height / 4 ; // desiredImgSizeInCharacters.getHeight();
        int pixelLen = pixels.length;

        for (int charsRow = 0; charsRow < charsHeight; charsRow++) {
            for (int charsCol = 0; charsCol < charsWidth; charsCol++) {
               int gridRow = charsRow * 4;
               int gridCol = charsCol * 2;
               char curBrailleChar = 0;
               int blueAVG  = 0;
               int greenAVG = 0;
               int redAVG   = 0;
               int charPlotCount = 0;
               // iterate thru a 4x2 braille char generation
               for (int gridRowOffset = 0; gridRowOffset<4; gridRowOffset++) {
                   for (int gridColOffset = 0; gridColOffset<2; gridColOffset++) {
                       int curPixelRow = gridRow+gridRowOffset;
                       int curPixelCol = gridCol+gridColOffset;
                       int curPixelDIX = curPixelRow * width + curPixelCol;
                       if (curPixelDIX >= pixelLen) {
                           break;
                       }
                       int pixel = pixels[curPixelDIX];

                       int blue  = (pixel & 0xff);
                       int green = ((pixel & 0xff00) >> 8);
                       int red   = ((pixel & 0xff0000) >> 16);
                       if (/*!isTransparentPixel(pixel) &&*/ toPlotOrNot.test(red, green, blue)) {
                           blueAVG  += blue;
                           greenAVG += green;
                           redAVG   += red;
                           charPlotCount++;
                           curBrailleChar = BrailleUtils.setBraillePixel(curBrailleChar, curPixelRow, curPixelCol);
                       }
                   }
               }
               if (charPlotCount>0) {
                   blueAVG  = blueAVG  / charPlotCount;
                   greenAVG = greenAVG / charPlotCount;
                   redAVG   = redAVG   / charPlotCount;

                   Color mappedColor = colorMap.toControlCode(redAVG, greenAVG, blueAVG);

                   brailleGrid.getPlotter().addPlot(charsRow, charsCol, curBrailleChar, mappedColor);
               }
            }
        }
        return brailleGrid;
    }

    public static boolean isTransparentPixel(int pixel) {
        if (pixel == -16777216 ) {
        //if ( pixel < 2 ) {
            return true;
        }
        return false;
    }
    
    public static Component<?> simpleImageToBlocksView(Image inputImage, Dimension desiredImgSizeInCharacters, RGBPlotPredicate toPlotOrNot, RGBToAnsiColorControlCode colorMap) throws Exception {
        
        BlocksGrid blocksGrid = new BlocksGrid(Colors.On_Whi, desiredImgSizeInCharacters);
        blocksGrid.setEmptyColor(Colors.Whi);
        blocksGrid.setEmptyChar(' ');
        blocksGrid.setFillOnClear(false);
        blocksGrid.setName("imageViewer-BlocksGrid");

        return simpleImageToBlocksView(blocksGrid, 
                                       inputImage, 
                                       desiredImgSizeInCharacters,
                                       toPlotOrNot,
                                       colorMap);
    }

    public static Component<?> simpleImageToBlocksView(Component<?> plotterDestination,
                                                       Image inputImage, 
                                                       Dimension desiredImgSizeInCharacters, 
                                                       RGBPlotPredicate toPlotOrNot, 
                                                       RGBToAnsiColorControlCode colorMap) throws Exception {

        BlocksGrid blocksGrid = (BlocksGrid) plotterDestination;

        blocksGrid.getPlotter().clearPlots();
        blocksGrid.clear();

        Image image = resize(inputImage, blocksGrid.getGridRectangle());

        Rectangle imgSize = new ImageRectangleAdapter(image);

        int width = imgSize.getWidth();
        int height = imgSize.getHeight();

        int[] pixels = new int[width * height];

        PixelGrabber pxGrabber = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);

        pxGrabber.grabPixels();

        int charsWidth  = width ; // desiredImgSizeInCharacters.getWidth();
        int charsHeight = height / 2; desiredImgSizeInCharacters.getHeight();
        int pixelLen = pixels.length;

        for (int charsRow = 0; charsRow < charsHeight; charsRow++) {
            for (int charsCol = 0; charsCol < charsWidth; charsCol++) {
               int gridRow = charsRow * 2;
               int gridCol = charsCol * 1;
               // iterate thru a 2x1 braille char generation
               for (int gridColOffset = 0; gridColOffset<1; gridColOffset++) {
                   for (int gridRowOffset = 0; gridRowOffset<2; gridRowOffset++) {
                       int curPixelRow = gridRow+gridRowOffset;
                       int curPixelCol = gridCol+gridColOffset;
                       int curPixelDIX = curPixelRow * width + curPixelCol;
                       if (curPixelDIX >= pixelLen) {
                           break;
                       }
                       int pixel = pixels[curPixelDIX];
                       int blue  = (pixel & 0xff);
                       int green = ((pixel & 0xff00) >> 8);
                       int red   = ((pixel & 0xff0000) >> 16);
                       if (toPlotOrNot.test(red, green, blue)) {
                           Color mappedColor = colorMap.toControlCode(red, green, blue);
                           blocksGrid.plotPosition(curPixelRow, curPixelCol, mappedColor);
                       }
                   }
               }
            }
        }
        return blocksGrid;
    }

    private static String SIMPLE_char_gradientStr1 = "ÆÑÊŒØMÉËÈÃÂWQBÅæ#NÁþEÄÀHKRŽœXgÐêqÛŠÕÔA€ßpmãâG¶øðé8ÚÜ$ëdÙýèÓÞÖåÿÒb¥FDñáZPäšÇàhû§ÝkŸ®S9žUTe6µOyxÎ¾f4õ5ôú&aü™2ùçw©Y£0VÍL±3ÏÌóC@nöòs¢u‰½¼‡zJƒ%¤Itocîrjv1lí=ïì<>i7†[¿?×}*{+()\\/»«•¬|!¡÷¦¯—^ª„”“~³º²–°­¹‹›;:’‘‚’˜ˆ¸…·¨´`";
    private static String SIMPLE_char_gradientStr2 = new StringBuilder(" .:-=+*#%@").reverse().toString();
    private static String SIMPLE_char_gradientStr3 = "$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^`'. ";

    static char rgbToChar(int r, int g, int b, char[] greyScale) {
        int h = (int) Math.floor((r  + g  + b) / 3.0);
        return greyScale[h];
    }
    
    static char rgbToChar(int r, int g, int b) {
        return rgbToChar(r,g,b, SIMPLE_char_gradientStr1);
    }

    static char rgbToChar(int r, int g, int b, String greyScale) {
        int h = (r / 3 + g / 3 + b / 3);
        float strLen = (float) greyScale.length();
        
        int chrIndex =  (int) Math.floor(  (((r / 3.0 + g / 3.0 + b / 3.0) / 255.0f) * strLen)   );
        
        chrIndex = ((int)strLen) - chrIndex;
        if (chrIndex>=strLen) {
            chrIndex = (int) strLen - 1;
        } else {
            if (chrIndex<0) {
                chrIndex = 0;
            }
        }
        
        return greyScale.charAt(chrIndex);

    }
    
    private static Dimension preserveAspectRatio(Dimension inputImageSize, Dimension desiredDimension) {
//        Rectangle imgRect = new ImageRectangleAdapter(inputImageSize);
        
        float curImgHeight = inputImageSize.getHeight();
        float curImgWidth = inputImageSize.getWidth();
        
        float heightRatio = ((float)desiredDimension.getHeight()) / curImgHeight;
        float newWidth = curImgWidth * heightRatio;
        if (newWidth <= desiredDimension.getWidth()) {
            desiredDimension = new Dimension.Simple( desiredDimension.getHeight(),  (int) newWidth); 
        } else {
            float widthRatio = ((float)desiredDimension.getWidth()) / curImgWidth;
            float newHeight = curImgHeight * widthRatio;
            desiredDimension = new Dimension.Simple( (int) newHeight,  desiredDimension.getWidth()); 
        }
        return desiredDimension;
    }

    @FunctionalInterface
    interface ContainerToImageSize {
        Dimension calcDimension(Dimension inputImageSize, Dimension containerDimension);
    }

    public static ContainerToImageSize ADJUST_NONE = (inputImageSize, containerDimension) -> inputImageSize;
    public static ContainerToImageSize ADJUST_TO_CONTAINER_SIZE_PRESERVE_ASPECT_RATIO = (inputImageSize, containerDimension) -> preserveAspectRatio(inputImageSize, containerDimension);
    public static ContainerToImageSize ADJUST_TO_CONTAINER_SIZE = (inputImageSize, containerDimension) -> containerDimension;
    public static ContainerToImageSize ASCII_ADJUST_TO_CONTAINER_SIZE_PRESERVE_ASPECT_RATIO = (inputImageSize, containerDimension) -> preserveAspectRatio(new Dimension.Simple(inputImageSize.getHeight(), inputImageSize.getWidth()*2), containerDimension);

    public static Image resize(Image inputImage, Dimension d, int imageTYPE, ContainerToImageSize containerToImageSize) {

        // creates output image

        Dimension desiredDimension = d;

        Rectangle imgRect = new ImageRectangleAdapter(inputImage);
        
        desiredDimension =  containerToImageSize.calcDimension(imgRect, desiredDimension);

        BufferedImage outputImage = new BufferedImage(desiredDimension.getWidth(), desiredDimension.getHeight(), imageTYPE);

        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2d.drawImage(inputImage, 0, 0, desiredDimension.getWidth(), desiredDimension.getHeight(), null);

        g2d.dispose();

        return outputImage;
    }

    public static Image resize(Image inputImage, Dimension d, ContainerToImageSize containerToImageSize) {
        return resize(inputImage, d, BufferedImage.TYPE_INT_ARGB, containerToImageSize);
    }

    public static Image resize(Image inputImage, Dimension d) {
        return resize(inputImage, d, BufferedImage.TYPE_INT_ARGB, ADJUST_TO_CONTAINER_SIZE_PRESERVE_ASPECT_RATIO);
    }


    @FunctionalInterface
    public static interface RGBPlotPredicate {
        boolean test(int r,int g, int b);
    }
    
    public static RGBPlotPredicate PLOT_ALWAYS = (r,g,b) -> true;

    public static RGBPlotPredicate PLOT_WHITE_ONLY = (r,g,b) -> r > 200 && g > 200 && b > 200;

//    public static RGBPlotPredicate PLOT_BLACK_ONLY = (r,g,b) -> r < 100 && g < 100 && b < 100;
    
    public static RGBPlotPredicate PLOT_BLACK_ONLY = (r,g,b) -> ! PLOT_WHITE_ONLY.test(r, g, b);

    public static RGBPlotPredicate PLOT_DARK_ONLY = plotDarkerThan(100);

    public static RGBPlotPredicate PLOT_DARKISH_ONLY = plotDarkerThan(150);
// 
    public static RGBPlotPredicate PLOT_LIGHT_ONLY = plotLighterThan(9);

    public static RGBPlotPredicate PLOT_LIGHTER_THAN_25  = plotLighterThan(25);
    public static RGBPlotPredicate PLOT_LIGHTER_THAN_50  = plotLighterThan(50);
    public static RGBPlotPredicate PLOT_LIGHTER_THAN_75  = plotLighterThan(75);
    public static RGBPlotPredicate PLOT_LIGHTER_THAN_100 = plotLighterThan(100);
    public static RGBPlotPredicate PLOT_LIGHTER_THAN_125 = plotLighterThan(125);
    public static RGBPlotPredicate PLOT_LIGHTER_THAN_150 = plotLighterThan(150);
    public static RGBPlotPredicate PLOT_LIGHTER_THAN_200 = plotLighterThan(200);
    public static RGBPlotPredicate PLOT_DARKER_THAN_50   = plotDarkerThan(50);
    public static RGBPlotPredicate PLOT_DARKER_THAN_75   = plotDarkerThan(75);
    public static RGBPlotPredicate PLOT_DARKER_THAN_125  = plotDarkerThan(125);
    public static RGBPlotPredicate PLOT_DARKER_THAN_150  = plotDarkerThan(150);
    public static RGBPlotPredicate PLOT_DARKER_THAN_200  = plotDarkerThan(200);
    public static RGBPlotPredicate PLOT_DARKER_THAN_225  = plotDarkerThan(225);
    public static RGBPlotPredicate PLOT_DARKER_THAN_240  = plotDarkerThan(240);
    public static RGBPlotPredicate PLOT_DARKER_THAN_250  = plotDarkerThan(250);
    public static RGBPlotPredicate PLOT_DARKER_THAN_255  = plotDarkerThan(255);
    
    static RGBPlotPredicate plotLighterThan(int grey) {
        return new RGBPlotPredicate() {

            @Override
            public boolean test(int r, int g, int b) {
                return (r + g + b)/3 >= grey;
            }
            
            public String toString() {
                return "RGBPlotPredicate.plotLighterThan("+grey+")";
            }
            
            
            
        };
    }

    static RGBPlotPredicate plotDarkerThan(int grey) {
        return new RGBPlotPredicate() {

            @Override
            public boolean test(int r, int g, int b) {
                return(r + g + b)/3 < grey;
            }
            public String toString() {
                return "RGBPlotPredicate.plotDarkerThan("+grey+")";
            }
        };
//        return (r,g,b) -> {
//            //  Logging.info(String.format("r %d g %d b %d = %d",r,g,b,(r + g + b)/3 ));
//              return(r + g + b)/3 < grey;
//        };
    }
    
//    public static RGBPlotPredicate PLOT_LIGHT_ONLY = (r,g,b) -> (r + g + b)/3 >= 9;

    @FunctionalInterface
    public static interface RGBToAnsiColorControlCode {
        Color toControlCode(int r,int g, int b);
    }

    public static RGBToAnsiColorControlCode rgbColorMapRCol = (r,g,b) ->Colors.RCol;
    
    public static RGBToAnsiColorControlCode rgbColorMapDIRECT = (r,g,b) ->Colors.fromRGBFG(r,g,b);

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
        return Colors.fromRGBToGreyScaleFG(r, g, b);
    };

    static RGBToAnsiColorControlCode rgbGreyScaleSmall = (r,g,b) ->  {
        return Colors.fromRGBToGreyScaleFGReducer(r, g, b, 5);
    };

    static RGBToAnsiColorControlCode rgbGreyScaleSmaller = (r,g,b) ->  {
        return Colors.fromRGBToGreyScaleFGReducer(r, g, b, 15);
    };

    static RGBToAnsiColorControlCode rgbGreyScaleSmallest = (r,g,b) ->  {
        return Colors.fromRGBToGreyScaleFGReducer(r, g, b, 20);
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


}
