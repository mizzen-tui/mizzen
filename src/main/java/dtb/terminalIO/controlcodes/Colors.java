package dtb.terminalIO.controlcodes;

import java.util.HashMap;
import java.util.Map;

import dtb.terminalIO.controlcodes.ControlCode.BgColor;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FgColor;

/**
 * Encapsulates some common ANSI Color ControlCodes
 */
public class Colors {

	 
	public static Map<String, FgColor> namedForgroundColors  = new HashMap<String, FgColor>();
	public static Map<String, BgColor> namedBackgroundColors = new HashMap<String, BgColor>();

	static Map<FgColor, String> foregroundColorToName = new HashMap<FgColor, String>();
	static Map<BgColor, String> backgroundColorToName = new HashMap<BgColor, String>();
	
	
	static FgColor addNamedFGColor(String name, FgColor color) {
		namedForgroundColors.put(name,color);
		foregroundColorToName.put(color, name);
		return color;
	}
	
	static BgColor addNamedBGColor(String name, BgColor color) {
		namedBackgroundColors.put(name,color);
		backgroundColorToName.put(color,name);
		return color;
	}
	
	public static FgColor getNamedFGColor(String name) {
		return namedForgroundColors.get(name);
	}

	public static BgColor getNamedBGColor(String name) {
		return namedBackgroundColors.get(name);
	}
	
	public static String getNameOfFGColor(FgColor color) {
		return foregroundColorToName.get(color);
	}
 
	public static String getNameOfBGColor(BgColor color) {
		return backgroundColorToName.get(color);
	}
	
	// Regular                                                        
	public static final FgColor Bla= addNamedFGColor( "Bla",FgColor.instance(30).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Bla") )); 
	public static final FgColor Red= addNamedFGColor( "Red",FgColor.instance(31).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Red") )); 
	public static final FgColor Gre= addNamedFGColor( "Gre",FgColor.instance(32).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Gre") )); 
	public static final FgColor Yel= addNamedFGColor( "Yel",FgColor.instance(33).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Yel") )); 
	public static final FgColor Blu= addNamedFGColor( "Blu",FgColor.instance(34).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Blu") )); 
	public static final FgColor Pur= addNamedFGColor( "Pur",FgColor.instance(35).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Pur") )); 
	public static final FgColor Cya= addNamedFGColor( "Cya",FgColor.instance(36).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Cya") )); 
	public static final FgColor Whi= addNamedFGColor( "Whi",FgColor.instance(37).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Whi") )); 	
	
	// Bold                                                               
	public static final FgColor BBla= addNamedFGColor( "BBla",Bla.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBla") )); 
	public static final FgColor BRed= addNamedFGColor( "BRed",Red.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IRed") )); 
	public static final FgColor BGre= addNamedFGColor( "BGre",Gre.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IGre") )); 
	public static final FgColor BYel= addNamedFGColor( "BYel",Yel.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IYel") )); 
	public static final FgColor BBlu= addNamedFGColor( "BBlu",Blu.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBlu") )); 
	public static final FgColor BPur= addNamedFGColor( "BPur",Pur.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Ipur") )); 
	public static final FgColor BCya= addNamedFGColor( "BCya",Cya.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_ICya") )); 
	public static final FgColor BWhi= addNamedFGColor( "BWhi",Whi.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IWhi") )); 	
	
	// Underline                                                         
	public static final FgColor UBla= addNamedFGColor( "UBla",Bla.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Bla") ));
	public static final FgColor URed= addNamedFGColor( "URed",Red.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Red") ));
	public static final FgColor UGre= addNamedFGColor( "UGre",Gre.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Gre") ));
	public static final FgColor UYel= addNamedFGColor( "UYel",Yel.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Yel") ));
	public static final FgColor UBlu= addNamedFGColor( "UBlu",Blu.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Blu") ));
	public static final FgColor UPur= addNamedFGColor( "UPur",Pur.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Pur") ));
	public static final FgColor UCya= addNamedFGColor( "UCya",Cya.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Cya") ));
	public static final FgColor UWhi= addNamedFGColor( "UWhi",Whi.asUnderline().setInvertTransform( (c) -> Colors.getNamedBGColor("On_Whi") ));	
	
	// High Intensity                                                    
	public static final FgColor IBla= addNamedFGColor( "IBla",FgColor.instance(90).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBla") ));
	public static final FgColor IRed= addNamedFGColor( "IRed",FgColor.instance(91).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IRed") ));
	public static final FgColor IGre= addNamedFGColor( "IGre",FgColor.instance(92).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IGre") ));
	public static final FgColor IYel= addNamedFGColor( "IYel",FgColor.instance(93).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IYel") ));
	public static final FgColor IBlu= addNamedFGColor( "IBlu",FgColor.instance(94).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBlu") ));
	public static final FgColor IPur= addNamedFGColor( "IPur",FgColor.instance(95).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IPur") ));
	public static final FgColor ICya= addNamedFGColor( "ICya",FgColor.instance(96).setInvertTransform( (c) -> Colors.getNamedBGColor("On_ICya") ));
	public static final FgColor IWhi= addNamedFGColor( "IWhi",FgColor.instance(97).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IWhi") ));	
	
	// BoldHigh Intens                                                    
	public static final FgColor BIBla= addNamedFGColor( "BIBla",IBla.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBla") ));
	public static final FgColor BIRed= addNamedFGColor( "BIRed",IRed.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IRed") ));
	public static final FgColor BIGre= addNamedFGColor( "BIGre",IGre.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IGre") ));
	public static final FgColor BIYel= addNamedFGColor( "BIYel",IYel.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IYel") ));
	public static final FgColor BIBlu= addNamedFGColor( "BIBlu",IBlu.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBlu") ));
	public static final FgColor BIPur= addNamedFGColor( "BIPur",IPur.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IPur") ));
	public static final FgColor BICya= addNamedFGColor( "BICya",ICya.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_ICya") ));
	public static final FgColor BIWhi= addNamedFGColor( "BIWhi",IWhi.asBold().setInvertTransform( (c) -> Colors.getNamedBGColor("On_IWhi") ));	
	
	// Background                                                        
	public static final BgColor On_Bla= addNamedBGColor(  "On_Bla",BgColor.instance(40).setInvertTransform( (c) -> Colors.getNamedFGColor("Bla") ));
	public static final BgColor On_Red= addNamedBGColor(  "On_Red",BgColor.instance(41).setInvertTransform( (c) -> Colors.getNamedFGColor("Red") ));
	public static final BgColor On_Gre= addNamedBGColor(  "On_Gre",BgColor.instance(42).setInvertTransform( (c) -> Colors.getNamedFGColor("Gre") ));
	public static final BgColor On_Yel= addNamedBGColor(  "On_Yel",BgColor.instance(43).setInvertTransform( (c) -> Colors.getNamedFGColor("Yel") ));
	public static final BgColor On_Blu= addNamedBGColor(  "On_Blu",BgColor.instance(44).setInvertTransform( (c) -> Colors.getNamedFGColor("Blu") ));
	public static final BgColor On_Pur= addNamedBGColor(  "On_Pur",BgColor.instance(45).setInvertTransform( (c) -> Colors.getNamedFGColor("Pur") ));
	public static final BgColor On_Cya= addNamedBGColor(  "On_Cya",BgColor.instance(46).setInvertTransform( (c) -> Colors.getNamedFGColor("Cya") ));
	public static final BgColor On_Whi= addNamedBGColor(  "On_Whi",BgColor.instance(47).setInvertTransform( (c) -> Colors.getNamedFGColor("Whi") ));	
	
	// High Intensity Backgrounds                                          
	public static final BgColor On_IBla= addNamedBGColor(  "On_IBla",BgColor.instance(100).setInvertTransform( (c) -> Colors.getNamedFGColor("IBla") ));
	public static final BgColor On_IRed= addNamedBGColor(  "On_IRed",BgColor.instance(101).setInvertTransform( (c) -> Colors.getNamedFGColor("IRed") ));
	public static final BgColor On_IGre= addNamedBGColor(  "On_IGre",BgColor.instance(102).setInvertTransform( (c) -> Colors.getNamedFGColor("IGre") ));
	public static final BgColor On_IYel= addNamedBGColor(  "On_IYel",BgColor.instance(103).setInvertTransform( (c) -> Colors.getNamedFGColor("IYel") ));
	public static final BgColor On_IBlu= addNamedBGColor(  "On_IBlu",BgColor.instance(104).setInvertTransform( (c) -> Colors.getNamedFGColor("IBlu") ));
	public static final BgColor On_IPur= addNamedBGColor(  "On_IPur",BgColor.instance(105).setInvertTransform( (c) -> Colors.getNamedFGColor("IPur") ));
	public static final BgColor On_ICya= addNamedBGColor(  "On_ICya",BgColor.instance(106).setInvertTransform( (c) -> Colors.getNamedFGColor("ICya") ));
	public static final BgColor On_IWhi= addNamedBGColor(  "On_IWhi",BgColor.instance(107).setInvertTransform( (c) -> Colors.getNamedFGColor("IWhi") ));
	
	
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
    

	private static Map<Integer, Color> rgbMap = new HashMap<Integer, Color>();
	
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
        return color.asString()+ text +ControlCode.RCol.asString();
    }
	
}
