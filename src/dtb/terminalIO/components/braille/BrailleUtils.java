package dtb.terminalIO.components.braille;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.components.plot.Plotter;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

public class BrailleUtils {

    static char[][] braillePixelMap = new char[][] { { 0x01, 0x08 }, 
	                                             { 0x02, 0x10 }, 
	                                             { 0x04, 0x20 }, 
	                                             { 0x40, 0x80 } };
    static char brailleCharOffset = 0x2800;
    static char brailleCharLast = (char) (brailleCharOffset + 255);
    
    public static char setBraillePixel(char curBrailleChar, Position pos) {
	return setBraillePixel(curBrailleChar, pos.getRow(), pos.getCol());
    }
    
    public static boolean isBrailleChar(char brailleChar) {
	return brailleChar >= brailleCharOffset && brailleChar <= brailleCharLast;
    }

    public static char setBraillePixel(char curBrailleChar, int rowOffset, int colOffset) {
	if (!isBrailleChar(curBrailleChar)) {
	    curBrailleChar = brailleCharOffset;
	}
	if (rowOffset <0) throw new IllegalArgumentException("rowOffset<0 ["+ rowOffset+","+colOffset+"]");
	if (colOffset <0) throw new IllegalArgumentException("colOffset<0 ["+ rowOffset+","+colOffset+"]");
	return curBrailleChar |= braillePixelMap[rowOffset % 4][colOffset % 2];
    }
    
    public static Position gridPosToComponentPosition(Position gridPosition) {
	return new Position.Simple(gridPosition.getRow()/4, gridPosition.getCol() / 2);
    }
    
    public static Dimension componentDimensionToGridDimension(Dimension componentDimension) {
	return new Dimension.Simple(componentDimension.getHeight()*4, componentDimension.getWidth()*2 );
    }

    public static Rectangle componentRectangleToGridRectangle(final Rectangle componentRectangle) {
	return new Rectangle.Delegate(componentRectangle){
	        @Override
	        public int getCol() {
	            return super.getCol()*2;
	        }

	        @Override
	        public int getRow() {
	            return super.getRow()*4;
	        }

	        @Override
	        public int getWidth() {
	            return super.getWidth()*2;
	        }

	        @Override
	        public int getHeight() {
	            return super.getHeight()*4;
	        }

	};
    }
    
    public static void setBraillePixel(FGBGColors color, IScreenLineSegmentsBuffer segments, Position gridPosition) {
	Position pos = gridPosToComponentPosition(gridPosition);
	String charAtPos = segments.getCharAt(pos.getRow(), pos.getCol());
	char theCHAR = charAtPos == null ? 0 : charAtPos.charAt(0);
	theCHAR = setBraillePixel(theCHAR, gridPosition);
	segments.setCharAt(pos.getRow(), pos.getCol(), theCHAR, color);
    }

    public static void unsetBraillePixel(FGBGColors color, IScreenLineSegmentsBuffer segments, Position gridPosition) {
	Position pos = gridPosToComponentPosition(gridPosition);
	String charAtPos = segments.getCharAt(pos.getRow(), pos.getCol());
	char theCHAR = charAtPos == null ? 0 : charAtPos.charAt(0);
	theCHAR = unsetBraillePixel(theCHAR, gridPosition);
	segments.setCharAt(pos.getRow(), pos.getCol(), theCHAR, color);
    }
    

    public static char unsetBraillePixel(char curBrailleChar, Position pos) {
	return unsetBraillePixel(curBrailleChar, pos.getRow(), pos.getCol());
    }
    
    public static char unsetBraillePixel(char curBrailleChar, int rowOffset, int colOffset) {
	if (curBrailleChar == 0) {
	    curBrailleChar = brailleCharOffset;
	    return curBrailleChar;
	}
	return curBrailleChar &= ~braillePixelMap[rowOffset % 4][colOffset % 2];
    }

    public static Comparator<int[]> brailleKeyComparator = new Comparator<int[]>() {
	@Override
	public int compare(int[] o1, int[] o2) {
	    int cmp = Integer.compare(o1[0], o2[0]);
	    if (cmp != 0)
		return cmp;
	    return Integer.compare(o1[1], o2[1]);
	}
    };

    static Map<int[], String> braillePatterns = new TreeMap<int[], String>(brailleKeyComparator);;
    static {
	braillePatterns.put(new int[] { 0, 0 }, "⣀");
	braillePatterns.put(new int[] { 0, 1 }, "⡠");
	braillePatterns.put(new int[] { 0, 2 }, "⡐");
	braillePatterns.put(new int[] { 0, 3 }, "⡈");

	braillePatterns.put(new int[] { 1, 0 }, "⢄");
	braillePatterns.put(new int[] { 1, 1 }, "⠤");
	braillePatterns.put(new int[] { 1, 2 }, "⠔");
	braillePatterns.put(new int[] { 1, 3 }, "⠌");

	braillePatterns.put(new int[] { 2, 0 }, "⢂");
	braillePatterns.put(new int[] { 2, 1 }, "⠢");
	braillePatterns.put(new int[] { 2, 2 }, "⠒");
	braillePatterns.put(new int[] { 2, 3 }, "⠊");

	braillePatterns.put(new int[] { 3, 0 }, "⢁");
	braillePatterns.put(new int[] { 3, 1 }, "⠡");
	braillePatterns.put(new int[] { 3, 2 }, "⠑");
	braillePatterns.put(new int[] { 3, 3 }, "⠉");
    }

    static char[] lSingleBraille = new char[] { '\u2840', '⠄', '⠂', '⠁' };
    static char[] rSingleBraille = new char[] { '\u2880', '⠠', '⠐', '⠈' };

    public static void plot(Plotter plot, int row, int col) {

    }

}
