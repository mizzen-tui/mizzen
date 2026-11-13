package mizzen.components;

import mizzen.controlcodes.color.Color;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.MapChars;

/**
 * Encapsulates block drawing characters
 */
public interface BlockChars {

    public static final int MIN             = 0;

    public static final int TOP_RIGHT       = 0;

    public static final int VERTICAL_LINE   = 1;

    public static final int HORIZONTAL_LINE = 2;

    public static final int TOP_LEFT        = 3;

    public static final int BOTTOM_RIGHT    = 4;

    public static final int BOTTOM_LEFT     = 5;

    public static final int VERTICAL_LEFT   = 6;

    public static final int VERTICAL_RIGHT  = 7;

    public static final int HORIZONTAL_DOWN = 8;

    public static final int HORIZONTAL_UP   = 9;
    
    public static final int INTERSECT = 10;
/*
    public static final int QUOTA_LEFT      = 10;

    public static final int QUOTA_RIGHT     = 11;
*/
    public static final int MAX             = 10;

    public char getTOP_RIGHT();

    public char getVERTICAL_LINE();

    public char getHORIZONTAL_LINE();

    public char getTOP_LEFT();

    public char getBOTTOM_RIGHT();

    public char getBOTTOM_LEFT();

    public char getVERTICAL_LEFT();

    public char getVERTICAL_RIGHT();

    public char getHORIZONTAL_DOWN();

    public char getHORIZONTAL_UP();

    public char getINTERSECT();

    public char[] blockChars();

    public boolean isBlockChar(char ch);

    public int toBlockCharIDX(char ch);

    public default char toBlockChar(int blockCharIDX) {
        if (blockCharIDX < MIN || blockCharIDX > MAX)
            throw new IllegalArgumentException(String.format("bad blockCharIDX = %d", blockCharIDX));
        return blockChars()[blockCharIDX];
    }
    


    static abstract class AbstractBaseBlockChars implements BlockChars {

        char[]        _blockCHARS;
        protected int minBlockCharValue;
        protected int maxBlockCharValue;

        protected AbstractBaseBlockChars() {
            _blockCHARS = makeBlockCharsARRAY();
            minBlockCharValue = Short.MAX_VALUE;
            maxBlockCharValue = Short.MIN_VALUE;
            for (int i = 0; i < _blockCHARS.length; i++) {

                if (_blockCHARS[i] < minBlockCharValue)
                    minBlockCharValue = _blockCHARS[i];

                if (_blockCHARS[i] > maxBlockCharValue)
                    maxBlockCharValue = _blockCHARS[i];
            }
        }

        public boolean isBlockChar(char ch) {

            if (ch < minBlockCharValue || ch > maxBlockCharValue) {
                return false;
            }

            char[] blockChars = blockChars();

            for (int i = MIN; i <= MAX; i++) {
                if (ch == blockChars[i]) {
                    return true;
                }
            }
            return false;
        }

        public int toBlockCharIDX(char ch) {

            if (ch < minBlockCharValue || ch > maxBlockCharValue) {
                return -1;
            }

            char[] blockChars = blockChars();

            for (int i = MIN; i <= MAX; i++) {
                if (ch == blockChars[i]) {
                    return i;
                }
            }
            return -1;
        }

        private char[] makeBlockCharsARRAY() {
            char[] chars = new char[MAX+1];

            chars[TOP_RIGHT] = getTOP_RIGHT();

            chars[VERTICAL_LINE] = getVERTICAL_LINE();

            chars[HORIZONTAL_LINE] = getHORIZONTAL_LINE();

            chars[TOP_LEFT] = getTOP_LEFT();

            chars[BOTTOM_RIGHT] = getBOTTOM_RIGHT();

            chars[BOTTOM_LEFT] = getBOTTOM_LEFT();

            chars[VERTICAL_LEFT] = getVERTICAL_LEFT();

            chars[VERTICAL_RIGHT] = getVERTICAL_RIGHT();

            chars[HORIZONTAL_DOWN] = getHORIZONTAL_DOWN();

            chars[HORIZONTAL_UP] = getHORIZONTAL_UP();

            chars[INTERSECT] = getINTERSECT();
            
            return chars;
        }

        public char[] blockChars() {
            return _blockCHARS;
        }
    }

    public static class Nice extends AbstractBaseBlockChars {

        @Override
        public char getTOP_RIGHT() {
            return '┐';
        }

        @Override
        public char getVERTICAL_LINE() {
            return '│';
        }

        @Override
        public char getHORIZONTAL_LINE() {
            return '─';
        }

        @Override
        public char getTOP_LEFT() {
            return '┌';
        }

        @Override
        public char getBOTTOM_RIGHT() {
            return '┘';
        }

        @Override
        public char getBOTTOM_LEFT() {
            return '└';
        }

        @Override
        public char getVERTICAL_LEFT() {
            return '┤';
        }

        @Override
        public char getVERTICAL_RIGHT() {
            return '├';
        }

        @Override
        public char getHORIZONTAL_DOWN() {
            return '┬';
        }

        @Override
        public char getHORIZONTAL_UP() {
            return '┴';
        }

        @Override
        public char getINTERSECT() {
            return '┼';
        }

//        @Override
//        public char getQUOTA_LEFT() {
//            return '«';
//        }
//
//        @Override
//        public char getQUOTA_RIGHT() {
//            return '»';
//        }

    }
    public static class NiceDouble extends Nice {

        @Override
        public char getTOP_RIGHT() {
            return '╗';
        }

        @Override
        public char getVERTICAL_LINE() {
            return '║';
        }

        @Override
        public char getHORIZONTAL_LINE() {
            return '═';
        }

        @Override
        public char getTOP_LEFT() {
            return '╔';
        }

        @Override
        public char getBOTTOM_RIGHT() {
            return '╝';
        }

        @Override
        public char getBOTTOM_LEFT() {
            return '╚';
        }

        @Override
        public char getVERTICAL_LEFT() {
            return '╣';
        }

        @Override
        public char getVERTICAL_RIGHT() {
            return '╠';
        }

        @Override
        public char getHORIZONTAL_DOWN() {
            return '╦';
        }

        @Override
        public char getHORIZONTAL_UP() {
            return '╩';
        }

        @Override
        public char getINTERSECT() {
            return '╬';
        }

    }

    public static class Basic extends AbstractBaseBlockChars {

        @Override
        public char getTOP_RIGHT() {
            return '+';
        }

        @Override
        public char getVERTICAL_LINE() {
            return '|';
        }

        @Override
        public char getHORIZONTAL_LINE() {
            return '-';
        }

        @Override
        public char getTOP_LEFT() {
            return '+';
        }

        @Override
        public char getBOTTOM_RIGHT() {
            return '+';
        }

        @Override
        public char getBOTTOM_LEFT() {
            return '+';
        }

        @Override
        public char getVERTICAL_LEFT() {
            return '+';
        }

        @Override
        public char getVERTICAL_RIGHT() {
            return '+';
        }

        @Override
        public char getHORIZONTAL_DOWN() {
            return '+';
        }

        @Override
        public char getHORIZONTAL_UP() {
            return '+';
        }

        @Override
        public char getINTERSECT() {
            return '+';
        }
        
//        @Override
//        public char getQUOTA_LEFT() {
//            return '<';
//        }
//
//        @Override
//        public char getQUOTA_RIGHT() {
//            return '>';
//        }

    }
    public static class BasicDouble extends Basic {


        @Override
        public char getVERTICAL_LINE() {
            return '|';
        }

        @Override
        public char getHORIZONTAL_LINE() {
            return '-';
        }

        @Override
        public char getTOP_LEFT() {
            return '+';
        }

        @Override
        public char getBOTTOM_RIGHT() {
            return '+';
        }

        @Override
        public char getBOTTOM_LEFT() {
            return '+';
        }

        @Override
        public char getVERTICAL_LEFT() {
            return '+';
        }

        @Override
        public char getVERTICAL_RIGHT() {
            return '+';
        }

        @Override
        public char getHORIZONTAL_DOWN() {
            return '+';
        }

        @Override
        public char getHORIZONTAL_UP() {
            return '+';
        }

//        @Override
//        public char getQUOTA_LEFT() {
//            return '<';
//        }
//
//        @Override
//        public char getQUOTA_RIGHT() {
//            return '>';
//        }

    }

    public static final BlockChars BASIC   = new Basic();

    public static final BlockChars BASIC_DOUBLE   = new BasicDouble();

    public static final BlockChars NICE    = new Nice();

    public static final BlockChars NICE_DOUBLE    = new NiceDouble();

    public static final BlockChars DEFAULT = NICE;

    /**
     * NICE with rounded corners ╭╮╰╯ - the same width class as the lines, so they join them in any font with box
     * drawing. No double rounded set exists: a focused rounded box shows its focus by colour (see Theme.focusedBlockChars).
     */
    public static final BlockChars ROUNDED = new Rounded();

    public static class Rounded extends Nice {
        @Override
        public char getTOP_LEFT() {
            return '╭';
        }

        @Override
        public char getTOP_RIGHT() {
            return '╮';
        }

        @Override
        public char getBOTTOM_LEFT() {
            return '╰';
        }

        @Override
        public char getBOTTOM_RIGHT() {
            return '╯';
        }
    }

    
    @FunctionalInterface
    static interface GetCharAt {
        char getCharAt(int row, int col);
    }

    public static class SimpleGetCharAt implements GetCharAt {

        char ch;

        public SimpleGetCharAt(char ch) {
            super();
            this.ch = ch;
        }

        @Override
        public char getCharAt(int row, int col) {
            if (row == 0 && col == 0) {
                return ch;
            }
            return 0;
        }
    }

    @FunctionalInterface
    static interface PlotCharAt {
        void setCharAt(int row, int col, char ch);
    }

    public static class UTIL {

        private static final BlockChars[] ALL_BLOCK_CHARS = new BlockChars[] {
                NICE,
                ROUNDED,                                     // after NICE: only its corners ╭╮╰╯ are its own
                NICE_DOUBLE,
                BASIC,
                BASIC_DOUBLE
        };

        private static MapChars NICE_TO_BASIC = null;

        public static boolean isBlockChar(char ch) {
            for (int i = 0; i < ALL_BLOCK_CHARS.length; i++) {
                if (ALL_BLOCK_CHARS[i].isBlockChar(ch)) {
                    return true;
                }
            }
            return false;
        }

        static int toBlockCharIDX(char ch) {
            for (int i = 0; i < ALL_BLOCK_CHARS.length; i++) {
                int idx = ALL_BLOCK_CHARS[i].toBlockCharIDX(ch);
                if (idx >= 0) {                                  // 0 is TOP_RIGHT (┐): "> 0" missed it
                    return idx;
                }
            }
            return -1;
        }

        static public boolean hasLeftComponent(int charIDX) {
            return charIDX == TOP_LEFT ||
                   charIDX == HORIZONTAL_LINE ||
                   charIDX == INTERSECT ||
                   charIDX == BOTTOM_LEFT ||
                   charIDX == VERTICAL_RIGHT ||
                   charIDX == HORIZONTAL_DOWN ||
                   charIDX == HORIZONTAL_UP;
        }

        static public boolean hasRightComponent(int charIDX) {
            return charIDX == TOP_RIGHT ||
                   charIDX == HORIZONTAL_LINE ||
                   charIDX == INTERSECT ||
                   charIDX == BOTTOM_RIGHT ||
                   charIDX == VERTICAL_LEFT ||
                   charIDX == HORIZONTAL_DOWN ||
                   charIDX == HORIZONTAL_UP;
        }

        static public boolean hasAboveComponent(int charIDX) {
            return charIDX == TOP_LEFT       ||
                   charIDX == TOP_RIGHT      ||
                   charIDX == INTERSECT      ||
                   charIDX == VERTICAL_LINE  ||
                   charIDX == VERTICAL_LEFT  ||
                   charIDX == VERTICAL_RIGHT ||
                   charIDX == HORIZONTAL_DOWN;
        }

        static public boolean hasBelowComponent(int charIDX) {
            return charIDX == BOTTOM_LEFT    ||
                   charIDX == BOTTOM_RIGHT   ||
                   charIDX == INTERSECT      ||
                   charIDX == VERTICAL_LINE  ||
                   charIDX == VERTICAL_LEFT  ||
                   charIDX == VERTICAL_RIGHT ||
                   charIDX == HORIZONTAL_DOWN;
        }
        
        // ---- joining lines: each line character as the directions it reaches (its arms) -------------------------

        /** the directions a line character reaches - one cell's arms, combined by adding (or-ing) them */
        public static final int UP = 1, DOWN = 2, LEFT = 4, RIGHT = 8;

        // the arms of each character index (TOP_RIGHT is ┐: it reaches left and down)
        private static final int[] ARMS = new int[MAX + 1];
        static {
            ARMS[TOP_RIGHT]       = LEFT | DOWN;
            ARMS[VERTICAL_LINE]   = UP | DOWN;
            ARMS[HORIZONTAL_LINE] = LEFT | RIGHT;
            ARMS[TOP_LEFT]        = RIGHT | DOWN;
            ARMS[BOTTOM_RIGHT]    = LEFT | UP;
            ARMS[BOTTOM_LEFT]     = RIGHT | UP;
            ARMS[VERTICAL_LEFT]   = UP | DOWN | LEFT;
            ARMS[VERTICAL_RIGHT]  = UP | DOWN | RIGHT;
            ARMS[HORIZONTAL_DOWN] = LEFT | RIGHT | DOWN;
            ARMS[HORIZONTAL_UP]   = LEFT | RIGHT | UP;
            ARMS[INTERSECT]       = UP | DOWN | LEFT | RIGHT;
        }

        /** a character's arms (any set's: light, double, rounded, basic); 0: not a line character */
        public static int armsOf(char ch) {
            int idx = toBlockCharIDX(ch);
            return idx < 0 ? 0 : ARMS[idx];
        }

        /**
         * The character in style for a set of arms. One arm alone (a line's end, with nothing to join) is the straight
         * line of its direction.
         * <pre>
         *     lineFor(NICE, LEFT | RIGHT | DOWN)   ┬
         *     lineFor(NICE, RIGHT)                 ─
         * </pre>
         */
        public static char lineFor(BlockChars style, int arms) {
            if ((arms & (LEFT | RIGHT)) == 0) {
                return style.getVERTICAL_LINE();                 // up, down, or both
            }
            if ((arms & (UP | DOWN)) == 0) {
                return style.getHORIZONTAL_LINE();               // left, right, or both
            }
            for (int idx = MIN; idx <= MAX; idx++) {
                if (ARMS[idx] == arms) {
                    return style.toBlockChar(idx);
                }
            }
            // three arms with one of a pair missing on each axis can't happen here: every set of two or more arms that
            // reaches both axes is one of the characters above
            throw new IllegalStateException("no line character for arms " + arms);
        }

        /**
         * Two line characters on one cell become one: the arms of both, in style - in any order, the same character.
         * Either not a line character: over, as drawing over it always does.
         * <pre>
         *     join('│', '─', NICE)   ┼        the divider over a border
         *     join('┐', '┌', NICE)   ┬        two boxes' corners on one cell
         *     join('│', 'x', NICE)   x
         * </pre>
         * The order-free core of line drawing, and of borders shared between neighbouring components (see
         * docs/ARCHITECTURE.md, "Shared borders").
         */
        public static char join(char under, char over, BlockChars style) {
            int underArms = armsOf(under);
            int overArms = armsOf(over);
            if (underArms == 0 || overArms == 0) {
                return over;
            }
            return lineFor(style, underArms | overArms);
        }

        // a line's piece with these arms, joined with whatever line is in the cell already
        private static char joinArms(BlockChars style, char current, int arms) {
            return lineFor(style, armsOf(current) | arms);
        }

        /** a horizontal line's first cell over current: it reaches right (├ on a vertical line) */
        static public char drawHorizontalStart(BlockChars blockChars, char current) {
            return joinArms(blockChars, current, RIGHT);
        }

        /** a horizontal line's cell over current: it reaches both ways (┼ across a vertical line) */
        static public char drawHorizontal(BlockChars blockChars, char current) {
            return joinArms(blockChars, current, LEFT | RIGHT);
        }

        /** a horizontal line's last cell over current: it reaches left (┤ on a vertical line) */
        static public char drawHorizontalEnd(BlockChars blockChars, char current) {
            return joinArms(blockChars, current, LEFT);
        }

        /**
         * A horizontal line len long, each cell joined with the line already there (join): its first cell reaches right,
         * its last left, the ones between both - so where it starts on a vertical line it makes ├, crossing one ┼. Text
         * already on a cell (a pane's title, moved onto a divider by shared borders) stays: a line goes under it, as a
         * border's goes under its title.
         */
        static public void drawHorizontalLine(BlockChars blockChars, int len, GetCharAt getCharAt, PlotCharAt plotCharAt ) {
            for (int i = 0; i < len; i++) {
                char current = getCharAt.getCharAt(0, i);
                boolean padding = current == ' ' && (isText(getCharAt.getCharAt(0, i - 1)) || isText(getCharAt.getCharAt(0, i + 1)));
                if (isText(current) || padding) {
                    continue;                                    // a title on it (a pane's, shared with the line), with its spaces: it stays
                }
                int arms = (i > 0 ? LEFT : 0) | (i < len - 1 ? RIGHT : 0);
                plotCharAt.setCharAt(0, i, joinArms(blockChars, current, arms == 0 ? LEFT | RIGHT : arms));
            }
        }

        // a character that is neither empty, a space nor a line: text - a line drawn across leaves it
        private static boolean isText(char ch) {
            return ch != 0 && ch != ' ' && armsOf(ch) == 0;
        }
        
        static public void drawHorizontalLine(IScreenLineSegmentsBuffer segments, Color drawColor, BlockChars blockChars, Position pos, int len ) {
            GetCharAt  charAT    = (row, col)     -> { 
                                                      String charStr = segments.getCharAt(pos.getRow()+row, pos.getCol()+col);
                                                      return charStr == null ? 0 : charStr.charAt(0);
                                                   };
            PlotCharAt setCharAT = (row, col, ch) -> segments.setCharAt(pos.getRow()+row, pos.getCol()+col, ch, drawColor );
            drawHorizontalLine(blockChars, len, charAT, setCharAT);
        }

        /** A vertical line len long, each cell joined with the line already there - as drawHorizontalLine, downwards. */
        static public void drawVerticalLine(BlockChars blockChars, int len, GetCharAt getCharAt, PlotCharAt plotCharAt ) {
            for (int i = 0; i < len; i++) {
                char current = getCharAt.getCharAt(i, 0);
                if (isText(current)) {
                    continue;                                    // a scrollbar's thumb on it: it stays
                }
                int arms = (i > 0 ? UP : 0) | (i < len - 1 ? DOWN : 0);
                plotCharAt.setCharAt(i, 0, joinArms(blockChars, current, arms == 0 ? UP | DOWN : arms));
            }
        }

        static public void drawVerticalLine(IScreenLineSegmentsBuffer segments, Color drawColor, BlockChars blockChars, Position pos, int height ) {
            GetCharAt  charAT    = (row, col)     -> {
                                                      String charStr = segments.getCharAt(pos.getRow()+row, pos.getCol()+col);
                                                      return charStr == null ? 0 : charStr.charAt(0);
                                                   };
            PlotCharAt setCharAT = (row, col, ch) -> segments.setCharAt(pos.getRow()+row, pos.getCol()+col, ch, drawColor );
            drawVerticalLine(blockChars, height, charAT, setCharAT);
        }

        /**
         * A box, joined with the lines already there. Its corners are put first, as corners, then the lines between them:
         * drawn as four lines, a line's end on an empty cell is a plain ─ and reads back as reaching both ways, so two
         * ends meeting at a corner would make ┬, not ┌.
         */
        static public void drawBox(IScreenLineSegmentsBuffer segments, Color drawColor, BlockChars blockChars, Rectangle rect ) {
            int top = rect.getRow(), left = rect.getCol();
            int bottom = rect.getBottomMostRow(), right = rect.getRightMostCol();
            putJoined(segments, drawColor, blockChars, top, left, RIGHT | DOWN);
            putJoined(segments, drawColor, blockChars, top, right, LEFT | DOWN);
            putJoined(segments, drawColor, blockChars, bottom, left, RIGHT | UP);
            putJoined(segments, drawColor, blockChars, bottom, right, LEFT | UP);
            for (int col = left + 1; col < right; col++) {
                putJoined(segments, drawColor, blockChars, top, col, LEFT | RIGHT);
                putJoined(segments, drawColor, blockChars, bottom, col, LEFT | RIGHT);
            }
            for (int row = top + 1; row < bottom; row++) {
                putJoined(segments, drawColor, blockChars, row, left, UP | DOWN);
                putJoined(segments, drawColor, blockChars, row, right, UP | DOWN);
            }
        }

        // a piece with these arms at row, col, joined with the line there already
        private static void putJoined(IScreenLineSegmentsBuffer segments, Color drawColor, BlockChars blockChars, int row, int col, int arms) {
            String current = segments.getCharAt(row, col);
            segments.setCharAt(row, col, joinArms(blockChars, current == null ? 0 : current.charAt(0), arms), drawColor);
        }

        public synchronized static MapChars getNICE_TO_BASIC() {

            if (NICE_TO_BASIC != null) {
                return NICE_TO_BASIC;
            }

            NICE_TO_BASIC = new MapChars();

            char[] nice_blockChars = NICE.blockChars();
            char[] basic_blockChars = BASIC.blockChars();
            char[] niceDouble_blockChars = NICE_DOUBLE.blockChars();
            char[] basicDouble_blockChars = BASIC_DOUBLE.blockChars();

            for (int i = 0; i < nice_blockChars.length ; i++) {
                NICE_TO_BASIC.map(nice_blockChars[i],basic_blockChars[i]);
                NICE_TO_BASIC.map(niceDouble_blockChars[i],basicDouble_blockChars[i]);
            }

            return NICE_TO_BASIC;

        }

    }

    /** focused or not, for a box: its characters are its theme's - focusedBlockChars while hilited, else blockChars */
    public static class BlockHiliter implements Hiliter {

        private boolean hilited;

        @Override
        public void hilite() {
            hilited = true;
        }

        @Override
        public void unHilite() {
            hilited = false;
        }

        /** the characters for now, from theme (worked out as the box is drawn, so a theme switch reaches it) */
        public BlockChars getBlockChars(mizzen.components.theme.Theme theme) {
            return hilited ? theme.focusedBlockChars() : theme.blockChars();
        }

    };

    

}
