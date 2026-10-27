package mizzen.components.plot.braille;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

import mizzen.components.plot.Plotter;
import mizzen.controlcodes.color.Colors;
import mizzen.controlcodes.color.Colors.FGBGColors;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.geom.position.Vec2;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.MapChars;

public class BrailleUtils {

    static char[][] braillePixelMap = new char[][] { { 0x01, 0x08 }, { 0x02, 0x10 }, { 0x04, 0x20 }, { 0x40, 0x80 } };
    public static char brailleCharOffset = 0x2800;
    public static char brailleCharLast = (char) (brailleCharOffset + 255);

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
        if (rowOffset < 0)
            throw new IllegalArgumentException("rowOffset<0 [" + rowOffset + "," + colOffset + "]");
        if (colOffset < 0)
            throw new IllegalArgumentException("colOffset<0 [" + rowOffset + "," + colOffset + "]");
        return curBrailleChar |= braillePixelMap[rowOffset % 4][colOffset % 2];
    }

    public static Position gridPosToComponentPosition(Vec2<?> gridPosition) {
        return new Position.Simple(gridPosition.getY() / 4, gridPosition.getX() / 2);
    }

    public static Dimension componentDimensionToGridDimension(Dimension componentDimension) {
        return new Dimension.Simple(componentDimension.getHeight() * 4, componentDimension.getWidth() * 2);
    }

    /**
     * A grid rectangle in relation to a component rectangle is 2 * (times) wider
     * and 4 * taller.. This is because a grid char is 2x4 . ie In a braille character you can
     * plot 2 dots wide (X) by 4 dots high (Y)
     */
    public static Rectangle componentRectangleToGridRectangle(final Rectangle componentRectangle) {
        return new Rectangle.Delegate(componentRectangle) {
            @Override
            public int getCol() {
                return super.getCol() * 2;
            }

            @Override
            public int getRow() {
                return super.getRow() * 4;
            }

            @Override
            public int getWidth() {
                return super.getWidth() * 2;
            }

            @Override
            public int getHeight() {
                return super.getHeight() * 4;
            }
        };
    }

    public static void setBraillePixel(Colors.FGBGColors color, IScreenLineSegmentsBuffer segments, Position gridPosition) {
        Position pos = gridPosToComponentPosition(gridPosition);
        String charAtPos = segments.getCharAt(pos.getRow(), pos.getCol());
        char theCHAR = charAtPos == null ? 0 : charAtPos.charAt(0);
        theCHAR = setBraillePixel(theCHAR, gridPosition);
        segments.setCharAt(pos.getRow(), pos.getCol(), theCHAR, color);
    }

    public static void unsetBraillePixel(Colors.FGBGColors color, IScreenLineSegmentsBuffer segments, Position gridPosition) {
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
    
    public static Position componentPosToGridPos(Position componentPos) {
        if (componentPos == null) {
            return null;
        }
        // 1 terminal character = 2×4 braille dots
        int gridRow = componentPos.getRow() * 4;
        int gridCol = componentPos.getCol() * 2;

        return new Position.Simple(gridRow, gridCol);
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

    private static MapChars BRAILLE_TO_ASCII = null;

    public synchronized static MapChars getBrailleToAsciiMapChars() {
        if (BRAILLE_TO_ASCII != null) {
            return BRAILLE_TO_ASCII;
        }

        BRAILLE_TO_ASCII = new MapChars() {
            public void map(char key, char value) {
                // if (value == '*') {
                // value = '+';
                // }
                // value = 'x';
                super.map(key, value);
            }

        };

        BRAILLE_TO_ASCII.map(new char[] { '⠀', ' ',

                '⠁', '`',

                '⠂', '`',

                '⠃', ':',

                '⠄', '.',

                '⠅', ':',

                '⠆', ';',

                '⠇', '|',

                '⠈', '\'',

                '⠉', '-',

                '⠊', '/',

                '⠋', '+',

                '⠌', '/',

                '⠍', '+',

                '⠎', '+',

                '⠏', '[',

                '⠐', '.',

                '⠑', '\\',

                '⠒', '-',

                '⠓', '*',

                '⠔', '/',

                '⠕', '>',

                '⠖', 'r',

                '⠗', '+',

                '⠘', ':',

                '⠙', '+',

                '⠚', '+',

                '⠛', '"',

                '⠜', ')',

                '⠝', '?',

                '⠞', '/',

                '⠟', 'P',

                '⠠', '.',

                '⠡', '\\',

                '⠢', '\\',

                '⠣', '\\',

                '⠤', '-',

                '⠥', 'l',

                '⠦', 'c',

                '⠧', 'L',

                '⠨', ':',

                '⠩', 't',

                '⠪', '<',

                '⠫', '*',

                '⠬', '*',

                '⠭', ':',

                '⠮', 'd',

                '⠯', 'E',

                '⠰', ':',

                '⠱', ':',

                '⠲', '+',

                '⠳', '\\',

                '⠴', ';',

                '⠵', '>',

                '⠶', 'o',

                '⠷', '6',

                '⠸', ':',

                '⠹', '7',

                '⠺', '+',

                '⠻', '9',

                '⠼', '+',

                '⠽', ']',

                '⠾', '#',

                '⠿', '#',

                '⡀', '.',

                '⡁', ':',

                '⡂', ':',

                '⡃', ';',

                '⡄', ':',

                '⡅', ':',

                '⡆', '|',

                '⡇', '|',

                '⡈', '/',

                '⡉', '7',

                '⡊', '{',

                '⡋', '[',

                '⡌', '(',

                '⡍', '[',

                '⡎', ':',

                '⡏', '|',

                '⡐', '/',

                '⡑', '>',

                '⡒', '+',

                '⡓', '+',

                '⡔', 'c',

                '⡕', 'P',

                '⡖', '+',

                '⡗', '+',

                '⡘', '?',

                '⡙', '?',

                '⡚', '/',

                '⡛', '?',

                '⡜', '/',

                '⡝', 'P',

                '⡞', 'Y',

                '⡟', 'P',

                '⡠', '/',

                '⡡', '/',

                '⡢', '>',

                '⡣', ';',

                '⡤', '+',

                '⡥', '+',

                '⡦', '+',

                '⡧', '+',

                '⡨', '/',

                '⡩', '7',

                '⡪', 'S',

                '⡫', '5',

                '⡬', '+',

                '⡭', 'P',

                '⡮', 'P',

                '⡯', 'P',

                '⡰', '/',

                '⡱', ')',

                '⡲', '>',

                '⡳', '5',

                '⡴', '/',

                '⡵', '*',

                '⡶', 'p',

                '⡷', 'b',

                '⡸', '*',

                '⡹', '*',

                '⡺', 'y',

                '⡻', 'g',

                '⡼', 'j',

                '⡽', '?',

                '⡾', '/',

                '⡿', '#',

                '⢀', '.',

                '⢁', ':',

                '⢂', ':',

                '⢃', ';',

                '⢄', '\\',

                '⢅', 'e',

                '⢆', 'L',

                '⢇', '|',

                '⢈', ':',

                '⢉', '7',

                '⢊', '(',

                '⢋', '0',

                '⢌', '<',

                '⢍', '3',

                '⢎', 'C',

                '⢏', 'Q',

                '⢐', ':',

                '⢑', '}',

                '⢒', '-',

                '⢓', '\\',

                '⢔', '<',

                '⢕', 'd',

                '⢖', 'c',

                '⢗', 'b',

                '⢘', '|',

                '⢙', '7',

                '⢚', '*',

                '⢛', '9',

                '⢜', 'q',

                '⢝', '2',

                '⢞', '(',

                '⢟', 'P',

                '⢠', ':',

                '⢡', ';',

                '⢢', ';',

                '⢣', '\\',

                '⢤', '+',

                '⢥', '\\',

                '⢦', '\\',

                '⢧', '\\',

                '⢨', '|',

                '⢩', '7',

                '⢪', '9',

                '⢫', '9',

                '⢬', '4',

                '⢭', '9',

                '⢮', '4',

                '⢯', '{',

                '⢰', '|',

                '⢱', '|',

                '⢲', '*',

                '⢳', '*',

                '⢴', '*',

                '⢵', '*',

                '⢶', '*',

                '⢷', '*',

                '⢸', '*',

                '⢹', 'T',

                '⢺', 't',

                '⢻', '?',

                '⢼', '4',

                '⢽', 't',

                '⢾', '*',

                '⢿', '@',

                '⣀', '_',

                '⣁', '_',

                '⣂', 'c',

                '⣃', 'l',

                '⣄', 'c',

                '⣅', 'L',

                '⣆', 'l',

                '⣇', 'L',

                '⣈', '_',

                '⣉', ']',

                '⣊', '<',

                '⣋', '*',

                '⣌', '*',

                '⣍', '*',

                '⣎', '[',

                '⣏', '[',

                '⣐', '>',

                '⣑', '*',

                '⣒', '*',

                '⣓', 'b',

                '⣔', 'c',

                '⣕', 'c',

                '⣖', 'c',

                '⣗', 't',

                '⣘', '*',

                '⣙', '*',

                '⣚', '*',

                '⣛', '*',

                '⣜', '*',

                '⣝', '*',

                '⣞', '*',

                '⣟', '*',

                '⣠', '*',

                '⣡', '*',

                '⣢', '*',

                '⣣', '*',

                '⣤', '*',

                '⣥', '*',

                '⣦', '*',

                '⣧', '*',

                '⣨', '*',

                '⣩', '5',

                '⣪', '5',

                '⣫', '5',

                '⣬', 'd',

                '⣭', 'A',

                '⣮', '6',

                '⣯', '5',

                '⣰', '>',

                '⣱', ')',

                '⣲', ';',

                '⣳', 'S',

                '⣴', 'd',

                '⣵', '*',

                '⣶', '*',

                '⣷', '*',

                '⣸', '*',

                '⣹', ']',

                '⣺', '*',

                '⣻', '9',

                '⣼', 'd',

                '⣽', '#',

                '⣾', '#',

                '⣿', '#' });

        return BRAILLE_TO_ASCII;
    }

    public static void main(String[] args) {
        System.out.println("Hello there");

        for (char ch = brailleCharOffset; ch <= brailleCharLast; ch++) {

            System.out.println("                                          '" + ch + "' , '*' ,\n");
        }

    }

}
