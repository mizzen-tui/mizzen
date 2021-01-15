package dtb.terminalIO.components.plot.image;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;

import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.braille.BrailleGrid;
import dtb.terminalIO.components.braille.BrailleUtils;
import dtb.terminalIO.components.input.DisplayableValue;
import dtb.terminalIO.components.input.DisplayableValue.SimpleDisplayValue;
import dtb.terminalIO.components.input.DisplayableValue.StringDisplayValue;
import dtb.terminalIO.components.plot.Plotter;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.util.Logging;

public class ImageViewerFactory {

    public static enum IMAGE_STYLE {
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
                 new ColorMapOption("rgbColorMapDIRECT",rgbColorMapDIRECT),
                 new ColorMapOption("rgbColorPlotBlack",rgbColorPlotBlack),
                 new ColorMapOption("rgbColorPlotBlackAndGreyAsWhite",rgbColorPlotBlackAndGreyAsWhite),
                 new ColorMapOption("rgbColorMapSimple1",rgbColorMapSimple1),
                 new ColorMapOption("rgbGreyScale",rgbGreyScale),
                 new ColorMapOption("No Color (default)", rgbColorMapRCol)
        }; 
        return colorMaps;
    }
    
    public static class PlotPredicateOption extends SimpleDisplayValue<RGBPlotPredicate> {
        public PlotPredicateOption(String displayValue, RGBPlotPredicate value) {
            super(displayValue, value);
        }

    }

    public static PlotPredicateOption[] plotPointPredicateOptions() {
        PlotPredicateOption[] colorMaps = new PlotPredicateOption[] {
                 new PlotPredicateOption("PLOT_ALWAYS",PLOT_ALWAYS),
                 new PlotPredicateOption("PLOT_WHITE_ONLY",PLOT_WHITE_ONLY),
                 new PlotPredicateOption("PLOT_BLACK_ONLY",PLOT_BLACK_ONLY),
                 new PlotPredicateOption("PLOT_DARK_ONLY", PLOT_DARK_ONLY),
                 new PlotPredicateOption("PLOT_LIGHT_ONLY",PLOT_LIGHT_ONLY)
        }; 
        return colorMaps;
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
    
    
    public static Component<?> simpleImageToStringView(Image inputImage, Dimension desiredImgSizeInCharacters, RGBPlotPredicate toPlotOrNot, RGBToAnsiColorControlCode colorMap, String gradientStr) throws Exception {

        Plotter plotterComponent = new Plotter(Colors.On_Whi, desiredImgSizeInCharacters);
        plotterComponent.setEmptyColor(Colors.Whi);
        plotterComponent.setEmptyChar(' ');
        plotterComponent.setFillOnClear(true);
        plotterComponent.setName("imageViewer-plotter");
        
        Image image = resize(inputImage, desiredImgSizeInCharacters);
        
        //ImageIO.write((BufferedImage)image, "png", new File("/home/dbalme/workspace/dtbTerminalIO/testScale.png"));
        
        Rectangle imgSize = new ImageRectangleAdapter(image);
        
        int width = imgSize.getWidth();
        int height = imgSize.getHeight();
        
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

                   Color mappedColor = colorMap.toControlCode(red, green, blue);
                   
                   char plotChar = rgbToChar(red, green, blue, gradientStr);
                   plotterComponent.addPlot(row, col, plotChar, mappedColor);
                   //plotter.setColor(mappedColor);
                   //plotter.plot(col, row);
               }
            }
        }
        return plotterComponent;
    
    }
    
    public static Component<?> simpleImageToBrailleView(Image inputImage, Dimension desiredImgSizeInCharacters, RGBPlotPredicate toPlotOrNot, RGBToAnsiColorControlCode colorMap) throws Exception {
        
        BrailleGrid brailleGrid = new BrailleGrid(Colors.On_Whi, desiredImgSizeInCharacters);
        brailleGrid.setEmptyColor(Colors.Whi);
        brailleGrid.setEmptyChar(' ');
        brailleGrid.setFillOnClear(true);
        brailleGrid.setName("imageViewer-brailleGrid");
        
        Image image = resize(inputImage, brailleGrid.getGridRectangle());
        
        Rectangle imgSize = new ImageRectangleAdapter(image);
        
        int width = imgSize.getWidth();
        int height = imgSize.getHeight();
        
        int[] pixels = new int[width * height];

        PixelGrabber pxGrabber = new PixelGrabber(image, 0, 0, width, height, pixels, 0, width);

        pxGrabber.grabPixels();

        int charsWidth  = desiredImgSizeInCharacters.getWidth();
        int charsHeight = desiredImgSizeInCharacters.getHeight();

        for (int charsRow = 0; charsRow < charsHeight; charsRow++) {
            for (int charsCol = 0; charsCol < charsWidth; charsCol++) {
               int gridRow = charsRow * 4;
               int gridCol = charsCol * 2;
               char curBrailleChar = 0;
               int blueAVG  = 0;
               int greenAVG = 0;
               int redAVG   = 0;
               int charPlotCount = 0;
               for (int gridRowOffset = 0; gridRowOffset<4; gridRowOffset++) {
                   for (int gridColOffset = 0; gridColOffset<2; gridColOffset++) {
                       int curPixelRow = gridRow+gridRowOffset;
                       int curPixelCol = gridCol+gridColOffset;
                       int pixel = pixels[curPixelRow * width + curPixelCol];
                       
                       int blue  = (pixel & 0xff);
                       int green = ((pixel & 0xff00) >> 8);
                       int red   = ((pixel & 0xff0000) >> 16);
                       if (toPlotOrNot.test(red, green, blue)) {
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

    

    static String SIMPLE_char_gradientStr3 = "$@B%8&WM#*oahkbdpqwmZO0QLCJUYXzcvunxrjft/\\|()1{}[]?-_+~<>i!lI;:,\"^`'. ";

    static String SIMPLE_char_gradientStr1 = "ÆÑÊŒØMÉËÈÃÂWQBÅæ#NÁþEÄÀHKRŽœXgÐêqÛŠÕÔA€ßpmãâG¶øðé8ÚÜ$ëdÙýèÓÞÖåÿÒb¥FDñáZPäšÇàhû§ÝkŸ®S9žUTe6µOyxÎ¾f4õ5ôú&aü™2ùçw©Y£0VÍL±3ÏÌóC@nöòs¢u‰½¼‡zJƒ%¤Itocîrjv1lí=ïì<>i7†[¿?×}*{+()\\/»«•¬|!¡÷¦¯—^ª„”“~³º²–°­¹‹›;:’‘‚’˜ˆ¸…·¨´`";

    static String SIMPLE_char_gradientStr2 = new StringBuilder(" .:-=+*#%@").reverse().toString();

    static char[] SIMPLE_char_gradient3 = buildCharacterGradient(SIMPLE_char_gradientStr3);

    static char[] SIMPLE_char_gradient1 = buildCharacterGradient(SIMPLE_char_gradientStr1);

    static char[] SIMPLE_char_gradient2 = buildCharacterGradient(SIMPLE_char_gradientStr2);

    static char[] buildCharacterGradient(String greyScale) {
        float strLen = (float) greyScale.length();
        char[]  gradient = new char[256];
        for (int i = 0; i<256; i++) {
            int chrIndex = (int) Math.floor(  ((((float)i) / 255.0f) * strLen)   );
            if (chrIndex>=strLen) {
                chrIndex = (int) strLen - 1;
            } else {
                if (chrIndex<0) {
                    chrIndex = 0;
                }
            }
            gradient[i] = greyScale.charAt(chrIndex);
        }
        return gradient;
    }
    
    
    
    static char rgbToChar(int r, int g, int b, char[] greyScale) {
        int h = (int) Math.floor((r  + g  + b) / 3.0);
        return greyScale[h];
    }
    
    static char rgbToChar(int r, int g, int b) {
        return rgbToChar(r,g,b, SIMPLE_char_gradientStr1);
        //return rgbToChar(r,g,b, SIMPLE_char_gradient1);
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
    
    public static Image resize(Image inputImage, Dimension d) {

        // creates output image
        BufferedImage outputImage = new BufferedImage(d.getWidth(), d.getHeight(), BufferedImage.TYPE_INT_RGB);
        
        ImageRectangleAdapter original = new ImageRectangleAdapter(inputImage);
        
        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                             RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2d.drawImage(inputImage, 0, 0, d.getWidth(), d.getHeight(), null);

        g2d.dispose();

        return outputImage;
    }
    
    
    
    @FunctionalInterface
    public static interface RGBPlotPredicate {
        boolean test(int r,int g, int b);
    }
    
    static RGBPlotPredicate PLOT_ALWAYS = (r,g,b) -> true;

    static RGBPlotPredicate PLOT_WHITE_ONLY = (r,g,b) -> r > 200 && g > 200 && b > 200;

    static RGBPlotPredicate PLOT_BLACK_ONLY = (r,g,b) -> r < 100 && g < 100 && b < 100;

    static RGBPlotPredicate PLOT_DARK_ONLY = (r,g,b) -> (r + g + b)/3 < 100;
// 
    static RGBPlotPredicate PLOT_LIGHT_ONLY = (r,g,b) -> {
        //Logging.info(String.format("r %d g %d b %d",r,g,b));
        return (r + g + b)/3 >= 9;
    };

    @FunctionalInterface
    public static interface RGBToAnsiColorControlCode {
        
        Color toControlCode(int r,int g, int b);
        
    }

    static RGBToAnsiColorControlCode rgbColorMapRCol = (r,g,b) ->Colors.RCol;
    
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

    
    
}
