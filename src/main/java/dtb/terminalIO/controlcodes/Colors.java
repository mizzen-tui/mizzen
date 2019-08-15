package dtb.terminalIO.controlcodes;

import java.util.HashMap;
import java.util.Map;

import dtb.terminalIO.controlcodes.ControlCode.BgColor;
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
	public static final FgColor Bla= addNamedFGColor( "Bla",new FgColor(30).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Bla") )); 
	public static final FgColor Red= addNamedFGColor( "Red",new FgColor(31).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Red") )); 
	public static final FgColor Gre= addNamedFGColor( "Gre",new FgColor(32).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Gre") )); 
	public static final FgColor Yel= addNamedFGColor( "Yel",new FgColor(33).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Yel") )); 
	public static final FgColor Blu= addNamedFGColor( "Blu",new FgColor(34).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Blu") )); 
	public static final FgColor Pur= addNamedFGColor( "Pur",new FgColor(35).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Pur") )); 
	public static final FgColor Cya= addNamedFGColor( "Cya",new FgColor(36).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Cya") )); 
	public static final FgColor Whi= addNamedFGColor( "Whi",new FgColor(37).setInvertTransform( (c) -> Colors.getNamedBGColor("On_Whi") )); 	
	
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
	public static final FgColor IBla= addNamedFGColor( "IBla",new FgColor(90).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBla") ));
	public static final FgColor IRed= addNamedFGColor( "IRed",new FgColor(91).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IRed") ));
	public static final FgColor IGre= addNamedFGColor( "IGre",new FgColor(92).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IGre") ));
	public static final FgColor IYel= addNamedFGColor( "IYel",new FgColor(93).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IYel") ));
	public static final FgColor IBlu= addNamedFGColor( "IBlu",new FgColor(94).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IBlu") ));
	public static final FgColor IPur= addNamedFGColor( "IPur",new FgColor(95).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IPur") ));
	public static final FgColor ICya= addNamedFGColor( "ICya",new FgColor(96).setInvertTransform( (c) -> Colors.getNamedBGColor("On_ICya") ));
	public static final FgColor IWhi= addNamedFGColor( "IWhi",new FgColor(97).setInvertTransform( (c) -> Colors.getNamedBGColor("On_IWhi") ));	
	
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
	public static final BgColor On_Bla= addNamedBGColor(  "On_Bla",new BgColor(40).setInvertTransform( (c) -> Colors.getNamedFGColor("Bla") ));
	public static final BgColor On_Red= addNamedBGColor(  "On_Red",new BgColor(41).setInvertTransform( (c) -> Colors.getNamedFGColor("Red") ));
	public static final BgColor On_Gre= addNamedBGColor(  "On_Gre",new BgColor(42).setInvertTransform( (c) -> Colors.getNamedFGColor("Gre") ));
	public static final BgColor On_Yel= addNamedBGColor(  "On_Yel",new BgColor(43).setInvertTransform( (c) -> Colors.getNamedFGColor("Yel") ));
	public static final BgColor On_Blu= addNamedBGColor(  "On_Blu",new BgColor(44).setInvertTransform( (c) -> Colors.getNamedFGColor("Blu") ));
	public static final BgColor On_Pur= addNamedBGColor(  "On_Pur",new BgColor(45).setInvertTransform( (c) -> Colors.getNamedFGColor("Pur") ));
	public static final BgColor On_Cya= addNamedBGColor(  "On_Cya",new BgColor(46).setInvertTransform( (c) -> Colors.getNamedFGColor("Cya") ));
	public static final BgColor On_Whi= addNamedBGColor(  "On_Whi",new BgColor(47).setInvertTransform( (c) -> Colors.getNamedFGColor("Whi") ));	
	
	// High Intensity Backgrounds                                          
	public static final BgColor On_IBla= addNamedBGColor(  "On_IBla",new BgColor(100).setInvertTransform( (c) -> Colors.getNamedFGColor("IBla") ));
	public static final BgColor On_IRed= addNamedBGColor(  "On_IRed",new BgColor(101).setInvertTransform( (c) -> Colors.getNamedFGColor("IRed") ));
	public static final BgColor On_IGre= addNamedBGColor(  "On_IGre",new BgColor(102).setInvertTransform( (c) -> Colors.getNamedFGColor("IGre") ));
	public static final BgColor On_IYel= addNamedBGColor(  "On_IYel",new BgColor(103).setInvertTransform( (c) -> Colors.getNamedFGColor("IYel") ));
	public static final BgColor On_IBlu= addNamedBGColor(  "On_IBlu",new BgColor(104).setInvertTransform( (c) -> Colors.getNamedFGColor("IBlu") ));
	public static final BgColor On_IPur= addNamedBGColor(  "On_IPur",new BgColor(105).setInvertTransform( (c) -> Colors.getNamedFGColor("IPur") ));
	public static final BgColor On_ICya= addNamedBGColor(  "On_ICya",new BgColor(106).setInvertTransform( (c) -> Colors.getNamedFGColor("ICya") ));
	public static final BgColor On_IWhi= addNamedBGColor(  "On_IWhi",new BgColor(107).setInvertTransform( (c) -> Colors.getNamedFGColor("IWhi") ));
	
}
