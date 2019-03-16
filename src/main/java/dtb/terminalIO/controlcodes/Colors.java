package dtb.terminalIO.controlcodes;

import java.util.HashMap;
import java.util.Map;

import dtb.terminalIO.controlcodes.ControlCode.BgColor;
import dtb.terminalIO.controlcodes.ControlCode.FgColor;

/**
 * Encapsulates some common ANSI Color ControlCodes
 */
public class Colors {

	
	static Map<String, FgColor> namedForgroundColors  = new HashMap<String, FgColor>();
	static Map<String, BgColor> namedBackgroundColors = new HashMap<String, BgColor>();

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
	public static final FgColor Bla= addNamedFGColor( "Bla",new FgColor(30)); 
	public static final FgColor Red= addNamedFGColor( "Red",new FgColor(31)); 
	public static final FgColor Gre= addNamedFGColor( "Gre",new FgColor(32)); 
	public static final FgColor Yel= addNamedFGColor( "Yel",new FgColor(33)); 
	public static final FgColor Blu= addNamedFGColor( "Blu",new FgColor(34)); 
	public static final FgColor Pur= addNamedFGColor( "Pur",new FgColor(35)); 
	public static final FgColor Cya= addNamedFGColor( "Cya",new FgColor(36)); 
	public static final FgColor Whi= addNamedFGColor( "Whi",new FgColor(37)); 	
	
	// Bold                                                               
	public static final FgColor BBla= addNamedFGColor( "BBla",Bla.asBold()); 
	public static final FgColor BRed= addNamedFGColor( "BRed",Red.asBold()); 
	public static final FgColor BGre= addNamedFGColor( "BGre",Gre.asBold()); 
	public static final FgColor BYel= addNamedFGColor( "BYel",Yel.asBold()); 
	public static final FgColor BBlu= addNamedFGColor( "BBlu",Blu.asBold()); 
	public static final FgColor BPur= addNamedFGColor( "BPur",Pur.asBold()); 
	public static final FgColor BCya= addNamedFGColor( "BCya",Cya.asBold()); 
	public static final FgColor BWhi= addNamedFGColor( "BWhi",Whi.asBold()); 	
	
	// Underline                                                         
	public static final FgColor UBla= addNamedFGColor( "UBla",Bla.asUnderline());
	public static final FgColor URed= addNamedFGColor( "URed",Red.asUnderline());
	public static final FgColor UGre= addNamedFGColor( "UGre",Gre.asUnderline());
	public static final FgColor UYel= addNamedFGColor( "UYel",Yel.asUnderline());
	public static final FgColor UBlu= addNamedFGColor( "UBlu",Blu.asUnderline());
	public static final FgColor UPur= addNamedFGColor( "UPur",Pur.asUnderline());
	public static final FgColor UCya= addNamedFGColor( "UCya",Cya.asUnderline());
	public static final FgColor UWhi= addNamedFGColor( "UWhi",Whi.asUnderline());	
	
	// High Intensity                                                    
	public static final FgColor IBla= addNamedFGColor( "IBla",new FgColor(90));
	public static final FgColor IRed= addNamedFGColor( "IRed",new FgColor(91));
	public static final FgColor IGre= addNamedFGColor( "IGre",new FgColor(92));
	public static final FgColor IYel= addNamedFGColor( "IYel",new FgColor(93));
	public static final FgColor IBlu= addNamedFGColor( "IBlu",new FgColor(94));
	public static final FgColor IPur= addNamedFGColor( "IPur",new FgColor(95));
	public static final FgColor ICya= addNamedFGColor( "ICya",new FgColor(96));
	public static final FgColor IWhi= addNamedFGColor( "IWhi",new FgColor(97));	
	
	// BoldHigh Intens                                                    
	public static final FgColor BIBla= addNamedFGColor( "BIBla",IBla.asBold());
	public static final FgColor BIRed= addNamedFGColor( "BIRed",IRed.asBold());
	public static final FgColor BIGre= addNamedFGColor( "BIGre",IGre.asBold());
	public static final FgColor BIYel= addNamedFGColor( "BIYel",IYel.asBold());
	public static final FgColor BIBlu= addNamedFGColor( "BIBlu",IBlu.asBold());
	public static final FgColor BIPur= addNamedFGColor( "BIPur",IPur.asBold());
	public static final FgColor BICya= addNamedFGColor( "BICya",ICya.asBold());
	public static final FgColor BIWhi= addNamedFGColor( "BIWhi",IWhi.asBold());	
	
	// Background                                                        
	public static final BgColor On_Bla= addNamedBGColor(  "On_Bla",new BgColor(40));
	public static final BgColor On_Red= addNamedBGColor(  "On_Red",new BgColor(41));
	public static final BgColor On_Gre= addNamedBGColor(  "On_Gre",new BgColor(42));
	public static final BgColor On_Yel= addNamedBGColor(  "On_Yel",new BgColor(43));
	public static final BgColor On_Blu= addNamedBGColor(  "On_Blu",new BgColor(44));
	public static final BgColor On_Pur= addNamedBGColor(  "On_Pur",new BgColor(45));
	public static final BgColor On_Cya= addNamedBGColor(  "On_Cya",new BgColor(46));
	public static final BgColor On_Whi= addNamedBGColor(  "On_Whi",new BgColor(47));	
	
	// High Intensity Backgrounds                                          
	public static final BgColor On_IBla= addNamedBGColor(  "On_IBla",new BgColor(100));
	public static final BgColor On_IRed= addNamedBGColor(  "On_IRed",new BgColor(101));
	public static final BgColor On_IGre= addNamedBGColor(  "On_IGre",new BgColor(102));
	public static final BgColor On_IYel= addNamedBGColor(  "On_IYel",new BgColor(103));
	public static final BgColor On_IBlu= addNamedBGColor(  "On_IBlu",new BgColor(104));
	public static final BgColor On_IPur= addNamedBGColor(  "On_IPur",new BgColor(105));
	public static final BgColor On_ICya= addNamedBGColor(  "On_ICya",new BgColor(106));
	public static final BgColor On_IWhi= addNamedBGColor(  "On_IWhi",new BgColor(107));
	
	
}
