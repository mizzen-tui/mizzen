package dtb.terminalIO.controlcodes.color;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.util.IntArrayComparator;

/**
 * Encapsulates some common ANSI Color ControlCodes
 */
public class Colors {


    private static final Map<String, Colors.FgColor> namedForgroundColors  = new HashMap<String, Colors.FgColor>();
    private static final Map<String, Colors.BgColor> namedBackgroundColors = new HashMap<String, Colors.BgColor>();

    private static final Map<Colors.FgColor, String> foregroundColorToName = new HashMap<Colors.FgColor, String>();
    private static final Map<Colors.BgColor, String> backgroundColorToName = new HashMap<Colors.BgColor, String>();

    private static final TreeMap<int[],Color> colorsCache = new TreeMap<int[],Color>(IntArrayComparator.INTARRAY_COMPARE);

    private static final Map<Integer, Color> rgbMap = new HashMap<Integer, Color>();


    static Colors.FgColor addNamedFGColor(String name, Colors.FgColor color) {
        namedForgroundColors.put(name,color);
        foregroundColorToName.put(color, name);
        return color;
    }
    
    static Colors.BgColor addNamedBGColor(String name, Colors.BgColor color) {
        namedBackgroundColors.put(name,color);
        backgroundColorToName.put(color,name);
        return color;
    }
    
    public static Colors.FgColor getNamedFGColor(String name) {
        return namedForgroundColors.get(name);
    }

    public static Colors.BgColor getNamedBGColor(String name) {
        return namedBackgroundColors.get(name);
    }
    
    public static String getNameOfFGColor(Colors.FgColor color) {
        return foregroundColorToName.get(color);
    }
 
    public static String getNameOfBGColor(Colors.BgColor color) {
        return backgroundColorToName.get(color);
    }
    
    public static final Color RCol                = new Color() {
        @Override
        protected int[] buildControlCodes() {
            boldTransform = (c)-> new Color() {
                protected int[] buildControlCodes() {
                    return new int[] {0,1};
                }
            };
            invertTransform = ColorTransform.NULL_colorTransform;
            return new int[] { 0 };
        }
        public Color setInvertTransform(ColorTransform invertTransform) {
            throw new IllegalStateException("Cannot change DEFAULT color!");
        }
        public Color setBoldTransform(ColorTransform boldTransform ) {
            throw new IllegalStateException("Cannot change DEFAULT color!");
        }
        
        public String toString() {
            return "RCol";
        }
        
    };
    
    public static final Color DEFAULT_COLOR            = RCol ; // new FGBGColors(Colors.Whi, null);
    
    
    // Regular                                                        
    public static final Colors.FgColor Bla= addNamedFGColor( "Bla",Colors.FgColor.instance(30).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Bla") )); 
    public static final Colors.FgColor Red= addNamedFGColor( "Red",Colors.FgColor.instance(31).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Red") )); 
    public static final Colors.FgColor Gre= addNamedFGColor( "Gre",Colors.FgColor.instance(32).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Gre") )); 
    public static final Colors.FgColor Yel= addNamedFGColor( "Yel",Colors.FgColor.instance(33).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Yel") )); 
    public static final Colors.FgColor Blu= addNamedFGColor( "Blu",Colors.FgColor.instance(34).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Blu") )); 
    public static final Colors.FgColor Pur= addNamedFGColor( "Pur",Colors.FgColor.instance(35).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Pur") )); 
    public static final Colors.FgColor Cya= addNamedFGColor( "Cya",Colors.FgColor.instance(36).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Cya") )); 
    public static final Colors.FgColor Whi= addNamedFGColor( "Whi",Colors.FgColor.instance(37).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Whi") ));     
    
    // Bold                                                               
    public static final Colors.FgColor BBla= addNamedFGColor( "BBla",Bla.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBla") )); 
    public static final Colors.FgColor BRed= addNamedFGColor( "BRed",Red.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IRed") )); 
    public static final Colors.FgColor BGre= addNamedFGColor( "BGre",Gre.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IGre") )); 
    public static final Colors.FgColor BYel= addNamedFGColor( "BYel",Yel.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IYel") )); 
    public static final Colors.FgColor BBlu= addNamedFGColor( "BBlu",Blu.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBlu") )); 
    public static final Colors.FgColor BPur= addNamedFGColor( "BPur",Pur.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Ipur") )); 
    public static final Colors.FgColor BCya= addNamedFGColor( "BCya",Cya.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_ICya") )); 
    public static final Colors.FgColor BWhi= addNamedFGColor( "BWhi",Whi.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IWhi") ));     
    
    // Underline                                                         
    public static final Colors.FgColor UBla= addNamedFGColor( "UBla",Bla.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Bla") ));
    public static final Colors.FgColor URed= addNamedFGColor( "URed",Red.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Red") ));
    public static final Colors.FgColor UGre= addNamedFGColor( "UGre",Gre.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Gre") ));
    public static final Colors.FgColor UYel= addNamedFGColor( "UYel",Yel.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Yel") ));
    public static final Colors.FgColor UBlu= addNamedFGColor( "UBlu",Blu.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Blu") ));
    public static final Colors.FgColor UPur= addNamedFGColor( "UPur",Pur.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Pur") ));
    public static final Colors.FgColor UCya= addNamedFGColor( "UCya",Cya.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Cya") ));
    public static final Colors.FgColor UWhi= addNamedFGColor( "UWhi",Whi.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Whi") ));    
    
    // High Intensity                                                    
    public static final Colors.FgColor IBla= addNamedFGColor( "IBla",Colors.FgColor.instance(90).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBla") ));
    public static final Colors.FgColor IRed= addNamedFGColor( "IRed",Colors.FgColor.instance(91).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IRed") ));
    public static final Colors.FgColor IGre= addNamedFGColor( "IGre",Colors.FgColor.instance(92).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IGre") ));
    public static final Colors.FgColor IYel= addNamedFGColor( "IYel",Colors.FgColor.instance(93).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IYel") ));
    public static final Colors.FgColor IBlu= addNamedFGColor( "IBlu",Colors.FgColor.instance(94).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBlu") ));
    public static final Colors.FgColor IPur= addNamedFGColor( "IPur",Colors.FgColor.instance(95).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IPur") ));
    public static final Colors.FgColor ICya= addNamedFGColor( "ICya",Colors.FgColor.instance(96).setInvertTransform( (c) -> Colors.getNamedBGColor("On_ICya") ));
    public static final Colors.FgColor IWhi= addNamedFGColor( "IWhi",Colors.FgColor.instance(97).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IWhi") ));    
    
    // BoldHigh Intens                                                    
    public static final Colors.FgColor BIBla= addNamedFGColor( "BIBla",IBla.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBla") ));
    public static final Colors.FgColor BIRed= addNamedFGColor( "BIRed",IRed.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IRed") ));
    public static final Colors.FgColor BIGre= addNamedFGColor( "BIGre",IGre.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IGre") ));
    public static final Colors.FgColor BIYel= addNamedFGColor( "BIYel",IYel.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IYel") ));
    public static final Colors.FgColor BIBlu= addNamedFGColor( "BIBlu",IBlu.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBlu") ));
    public static final Colors.FgColor BIPur= addNamedFGColor( "BIPur",IPur.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IPur") ));
    public static final Colors.FgColor BICya= addNamedFGColor( "BICya",ICya.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_ICya") ));
    public static final Colors.FgColor BIWhi= addNamedFGColor( "BIWhi",IWhi.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IWhi") ));    
    
    // Background                                                        
    public static final Colors.BgColor On_Bla= addNamedBGColor(  "On_Bla",Colors.BgColor.instance(40).setInvertTransform( (c) -> Colors.getNamedFGColor("Bla") ));
    public static final Colors.BgColor On_Red= addNamedBGColor(  "On_Red",Colors.BgColor.instance(41).setInvertTransform( (c) -> Colors.getNamedFGColor("Red") ));
    public static final Colors.BgColor On_Gre= addNamedBGColor(  "On_Gre",Colors.BgColor.instance(42).setInvertTransform( (c) -> Colors.getNamedFGColor("Gre") ));
    public static final Colors.BgColor On_Yel= addNamedBGColor(  "On_Yel",Colors.BgColor.instance(43).setInvertTransform( (c) -> Colors.getNamedFGColor("Yel") ));
    public static final Colors.BgColor On_Blu= addNamedBGColor(  "On_Blu",Colors.BgColor.instance(44).setInvertTransform( (c) -> Colors.getNamedFGColor("Blu") ));
    public static final Colors.BgColor On_Pur= addNamedBGColor(  "On_Pur",Colors.BgColor.instance(45).setInvertTransform( (c) -> Colors.getNamedFGColor("Pur") ));
    public static final Colors.BgColor On_Cya= addNamedBGColor(  "On_Cya",Colors.BgColor.instance(46).setInvertTransform( (c) -> Colors.getNamedFGColor("Cya") ));
    public static final Colors.BgColor On_Whi= addNamedBGColor(  "On_Whi",Colors.BgColor.instance(47).setInvertTransform( (c) -> Colors.getNamedFGColor("Whi") ));    
    
    // High Intensity Backgrounds                                          
    public static final Colors.BgColor On_IBla= addNamedBGColor(  "On_IBla",Colors.BgColor.instance(100).setInvertTransform( (c) -> Colors.getNamedFGColor("IBla") ));
    public static final Colors.BgColor On_IRed= addNamedBGColor(  "On_IRed",Colors.BgColor.instance(101).setInvertTransform( (c) -> Colors.getNamedFGColor("IRed") ));
    public static final Colors.BgColor On_IGre= addNamedBGColor(  "On_IGre",Colors.BgColor.instance(102).setInvertTransform( (c) -> Colors.getNamedFGColor("IGre") ));
    public static final Colors.BgColor On_IYel= addNamedBGColor(  "On_IYel",Colors.BgColor.instance(103).setInvertTransform( (c) -> Colors.getNamedFGColor("IYel") ));
    public static final Colors.BgColor On_IBlu= addNamedBGColor(  "On_IBlu",Colors.BgColor.instance(104).setInvertTransform( (c) -> Colors.getNamedFGColor("IBlu") ));
    public static final Colors.BgColor On_IPur= addNamedBGColor(  "On_IPur",Colors.BgColor.instance(105).setInvertTransform( (c) -> Colors.getNamedFGColor("IPur") ));
    public static final Colors.BgColor On_ICya= addNamedBGColor(  "On_ICya",Colors.BgColor.instance(106).setInvertTransform( (c) -> Colors.getNamedFGColor("ICya") ));
    public static final Colors.BgColor On_IWhi= addNamedBGColor(  "On_IWhi",Colors.BgColor.instance(107).setInvertTransform( (c) -> Colors.getNamedFGColor("IWhi") ));
    
    
    public static class RGBColor extends Color {

        int[] codes;

        public static Color instance(int r, int g, int b) {
            return new RGBColor(r, g, b);
        }
        
        private RGBColor(int r, int g, int b) {
            this.codes = new int[] {38, 2, r, g, b};
        }

        @Override
        protected int[] buildControlCodes() {
            return codes;
        }

    }
    

    public static class ColorFromCodes extends Color {
    
        public static Color instance(int[] codes) {
            return getColorFromCacheOrBuild(codes, ()->new ColorFromCodes(codes));
        }
        
        public static Color instance(Color color) {
            return getColorFromCacheOrBuild(color.getControlCodes(), ()->new ColorFromCodes(color));
        }
        
        public ColorFromCodes(int[] codes) {
            this.codes = codes;
        }
        
        private ColorFromCodes(Color color) {
            this(color.getControlCodes());
        }
        
    
        
        @Override
        public int[] buildControlCodes() {
            return codes;
        }
    }


    public static class ColorFromString extends Color {
    
        int[] codes;
        
        public ColorFromString(String colorStr) {
            colorStr = colorStr.replace(ControlCode.escape_PREFIX,"");
            colorStr = colorStr.replace("m","");
            String[] codesStr = colorStr.split(";");
            codes = new int[codesStr.length];
            for (int i = 0; i< codes.length; i++) {
                codes[i] = Integer.parseInt(codesStr[i]);
            }
        }
        
        @Override
        public int[] buildControlCodes() {
            return codes;
        }
        
    }


    public static abstract class SimpleColor extends Color {
        public int colorCode = 0;
    
        public SimpleColor(int colorCode) {
            this.colorCode = colorCode;
        }
    
        public int getColorCode() {
            return colorCode;
        }
    
        public void setColorCode(int colorCode) {
            this.colorCode = colorCode;
        }
    
    }


    /**
     * Foreground Color
     */
    public static class FgColor extends SimpleColor {
    
        boolean bold      = false;
        boolean italic    = false;
        boolean underline = false;
    
        public static FgColor instance(int colorCode) {
            int[] codes =  __buildControlCodes(false, false, false, colorCode);
            return getColorFromCacheOrBuild(codes, ()->new FgColor(colorCode));
        }
    
        private static FgColor instance(int colorCode, boolean bold, boolean italic, boolean underline) {
            int[] codes =  __buildControlCodes(bold, italic, underline, colorCode);
            return getColorFromCacheOrBuild(codes, ()->new FgColor(colorCode,bold, italic, underline));
        }
        
        private FgColor(int colorCode) {
            super(colorCode);
            this.setBoldTransform((color)->asBold());
        }
    
        private FgColor(int colorCode, boolean bold, boolean italic, boolean underline) {
            this(colorCode);
            this.bold = bold;
            this.italic = italic;
            this.underline = underline;
            this.setBoldTransform((color)->asBold());
        }
        
        public FgColor setInvertTransform(ColorTransform invertTransform) {
            super.setInvertTransform(invertTransform);
            return this;
        }
    
    
        public FgColor asBold() {
            return FgColor.instance(colorCode, true, italic, underline);
        }
        
        public FgColor removeBold() {
            return FgColor.instance(colorCode, false, italic, underline);
        }
    
        public FgColor asItalic() {
            return FgColor.instance(colorCode, bold, true, underline);
        }
    
        public FgColor asUnderline() {
            return FgColor.instance(colorCode, bold, italic, true);
        }
    
        @Override
        public int[] buildControlCodes() {
            return __buildControlCodes(bold, italic, underline, this.getColorCode());
        }
        
        static int[] __buildControlCodes(boolean bold,
                                         boolean italic,
                                         boolean underline,
                                         int colorCode) {
            int codes[] = new int[(bold ? 1 : 0) + (italic ? 1 : 0) + (underline ? 1 : 0) + 2];
            int idx = 0;
            codes[idx++] = 0; // reset previous attributes
            if (bold)
                codes[idx++] = 1; // conditionally set bold
            if (italic)
                codes[idx++] = 3; // conditionally set italic
            if (underline)
                codes[idx++] = 4; // conditionally set underline
            codes[idx] = colorCode; // now finally set the foreground
                                              // color
            return codes;
        }
    
        public String toString() {
            String name = getNameOfFGColor(this);
            if (name == null) {
                name = this.buildColorControlCode();
            }
            return "[FG=" + name + "]";
        }
    
    }


    /*
     * Background color
     */
    public static class BgColor extends SimpleColor {
    
        public static BgColor instance(int colorCode) {
            return getColorFromCacheOrBuild(new int[] { colorCode }, ()->new BgColor(colorCode));
        }
        
        private BgColor(int colorCode) {
            super(colorCode);
        }
    
        public BgColor setInvertTransform(ColorTransform invertTransform) {
            super.setInvertTransform(invertTransform);
            return this;
        }
        
        @Override
        public int[] buildControlCodes() {
            return new int[] { this.getColorCode() };
        }
    
        public String toString() {
            String name = getNameOfBGColor(this);
            if (name == null) {
                name = this.buildColorControlCode();
            }
            return "[BG=" + name + "]";
        }
        
        
    }


    /**
         * encapsulate foreground and background colors together
         */
        public static class FGBGColors extends Color {
    
            public static final Color NO_COLOR_SPECIFIED = Colors.DEFAULT_COLOR ; //new FGBGColors(null, null);
    
            BgColor                        bgc;
            FgColor                        fgc;
    
            public FGBGColors(FgColor fgc, BgColor bgc) {
                super();
                this.bgc = bgc;
                this.fgc = fgc;
                this.setBoldTransform((color)->asBold());
            }
    
            public FGBGColors(FgColor fgc) {
                super();
                this.bgc = null;
                this.fgc = fgc;
                this.setBoldTransform((color)->asBold());
            }
    
            public FGBGColors(BgColor bgc) {
                super();
                this.bgc = bgc;
                this.fgc = null;
                this.setBoldTransform((color)->asBold());
    //            this.setInvertTransform( (color) -> new  FGBGColors((FgColor)bgc==null?null: bgc.invert()) , ((BgColor)fgc==null?null:fgc.invert())) );
            }
    
            
            public FGBGColors asBold() {
                return new FGBGColors((fgc==null?(new FgColor(0)):fgc).asBold(), bgc);
            }
            
            
            protected ColorTransform buildDefaultColorTransform() {
                if (this.fgc != null && this.fgc.getInvertTransform() != null) {
                    
                    if (this.bgc == null || this.bgc.getInvertTransform() == null) {
                        return this.fgc.getInvertTransform(); 
                    }
                    
                    int[] fgcInvertedColorCodes = this.fgc.getInvertTransform().transform(this.fgc).getControlCodes();
                    int[] bgcInvertedColorCodes = this.bgc.getInvertTransform().transform(this.bgc).getControlCodes();
                    
                    int[] combinedCodes = new int[fgcInvertedColorCodes.length + bgcInvertedColorCodes.length];
                    System.arraycopy(fgcInvertedColorCodes, 0, combinedCodes, 0, fgcInvertedColorCodes.length);
                    System.arraycopy(bgcInvertedColorCodes, 0, combinedCodes, fgcInvertedColorCodes.length, bgcInvertedColorCodes.length);
                    
                    return (color) -> new ColorFromCodes(combinedCodes).setInvertTransform((c)->FGBGColors.this);
                    
                    
                } else if ((this.bgc != null && this.bgc.getInvertTransform()!=null)) {
                    return this.bgc.getInvertTransform();
                } else {
                    return ColorTransform.NULL_colorTransform;
                }
            }
            
            public ColorTransform getInvertTransform() {
                return buildDefaultColorTransform();
            }
    
    
            public FGBGColors removebold() {
                return new FGBGColors(fgc.removeBold(), bgc);
            }
    
            public BgColor getBgc() {
                return bgc;
            }
    
            public void setBgc(BgColor bgc) {
                this.bgc = bgc;
            }
    
            public FgColor getFgc() {
                return fgc;
            }
    
            public void setFgc(FgColor fgc) {
                this.fgc = fgc;
            }
    
            @Override
            public boolean isMergeable() {
                return this.getBgc() == null || this.getFgc() == null;
            }
    
            @Override
            public Color mergeWith(Color otherColor) {
                if (! (otherColor instanceof FGBGColors)) {
                    return super.mergeWith(otherColor);
                }
                FGBGColors other = (FGBGColors) otherColor;
                FGBGColors newColor = new FGBGColors(this.getFgc() != null ? this.getFgc() : other.getFgc(),
                                                     this.getBgc() != null ? this.getBgc() : other.getBgc());
                return newColor;
            }
    
            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((bgc == null) ? 0 : bgc.hashCode());
                result = prime * result + ((fgc == null) ? 0 : fgc.hashCode());
                return result;
            }
    
            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                FGBGColors other = (FGBGColors) obj;
                if (bgc == null) {
                    if (other.bgc != null)
                        return false;
                } else if (!bgc.equals(other.bgc))
                    return false;
                if (fgc == null) {
                    if (other.fgc != null)
                        return false;
                } else if (!fgc.equals(other.fgc))
                    return false;
                return true;
            }
    
            @Override
            public int[] buildControlCodes() {
                int [] fgcControlCodes = fgc != null? fgc.getControlCodes() : new int[] {};
                int [] bgcControlCodes = bgc != null? bgc.getControlCodes() : new int[] {};
                
                int[] controlCodes = new int[fgcControlCodes.length + bgcControlCodes.length];
                int i = 0;
                for (int j=0; j<fgcControlCodes.length; j++) {
                    controlCodes[i] = fgcControlCodes[j];
                    i++;
                }
                for (int j=0; j<bgcControlCodes.length; j++) {
                    controlCodes[i] = bgcControlCodes[j];
                    i++;
                }
                return controlCodes;
            }
    
            public FGBGColors newFrom(FgColor fgcNew) {
                return new FGBGColors(fgcNew, this.getBgc());
            }
    
            public FGBGColors newFrom(BgColor bgcNew) {
                return new FGBGColors(this.getFgc(), bgcNew);
            }
    
            public String toString() {
                return "(" + String.valueOf(fgc) + "," + String.valueOf(bgc) + ")";
            }
    
            public interface HasRowChange {
                int deltaRow();
            }
    
        }


    static int toPixelColor(int r, int g, int b) {
        return /* red    */      r << 16  |
               /* green  */      g <<  8  |
               /* blue   */      b        ;
    }

    static public Color fromRGBPixelColor(int pixelColor) {
        Color rgbColor = rgbMap.get(pixelColor);
        if (rgbColor != null) {
            return rgbColor;
        }
        
        int r = (pixelColor & 0xff0000) >> 16;
        int g = (pixelColor & 0xff00)   >> 8;
        int b =  pixelColor & 0xff;
        
        rgbColor = new RGBColor(r,g,b);
        rgbMap.put(pixelColor, rgbColor);
        return rgbColor;
        
    }

    static public Color fromRGB(int r, int g, int b) {
        int pixelColor = toPixelColor(r,g,b);
        Color rgbColor = rgbMap.get(pixelColor);
        if (rgbColor != null) {
            return rgbColor;
        }
        rgbColor = RGBColor.instance(r,g,b);
        rgbMap.put(pixelColor, rgbColor);
        return rgbColor;
    }
    
    static public Color fromRGBToGreyScale(int r, int g, int b) {
        // take the average of all 3 colors -> translates to grey scale.. who knew?
        int h = (r / 3 + g / 3 + b / 3);
        return fromRGB(h, h, h);
    }
    
    static public String coloredText(Color color, String text) {
        if (text == null) {
            return null;
        }
        if (color == null) {
            return text;
        }
        return color.asString()+ text +Colors.RCol.asString();
    }


    public static Color getColorFromCache(int[] codes) {
        return colorsCache.get(codes);
    }

    public static <T extends Color> T getColorFromCacheOrBuild(int[] codes, 
                                                               Supplier<T> makeColor) {
        T color = (T) colorsCache.get(codes);
        if (color == null) {
            color = makeColor.get();
            colorsCache.put(color.getControlCodes(), color);
        } 
        return color;
    }

    public static Set<String> getBackgroundColorNames() {
        return namedBackgroundColors.keySet();
    }

    // namedForgroundColors
    public static Set<String> getForegroundColorNames() {
        return namedForgroundColors.keySet();
    }

    
    
}
