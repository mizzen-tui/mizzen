package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.MapChars;

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
                if (idx > 0) {
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
        
        static int[] horizonal_start = new int[MAX+1];
        static {
            horizonal_start[TOP_RIGHT]       = HORIZONTAL_DOWN;
            horizonal_start[VERTICAL_LINE]   = VERTICAL_RIGHT;
            horizonal_start[HORIZONTAL_LINE] = HORIZONTAL_LINE;
            horizonal_start[TOP_LEFT]        = TOP_LEFT;
            horizonal_start[BOTTOM_RIGHT]    = HORIZONTAL_UP;
            horizonal_start[BOTTOM_LEFT]     = BOTTOM_LEFT;
            horizonal_start[VERTICAL_LEFT]   = INTERSECT;
            horizonal_start[VERTICAL_RIGHT]  = VERTICAL_RIGHT;
            horizonal_start[HORIZONTAL_DOWN] = HORIZONTAL_DOWN;
            horizonal_start[HORIZONTAL_UP]   = HORIZONTAL_UP;
            horizonal_start[INTERSECT]       = INTERSECT;
        }

        static int[] horizonal_end = new int[MAX+1];
        static {
            horizonal_end[TOP_RIGHT]       = TOP_RIGHT;
            horizonal_end[VERTICAL_LINE]   = VERTICAL_LEFT;
            horizonal_end[HORIZONTAL_LINE] = HORIZONTAL_LINE;
            horizonal_end[TOP_LEFT]        = HORIZONTAL_DOWN;
            horizonal_end[BOTTOM_RIGHT]    = BOTTOM_RIGHT;
            horizonal_end[BOTTOM_LEFT]     = HORIZONTAL_UP;
            horizonal_end[VERTICAL_LEFT]   = VERTICAL_LEFT;
            horizonal_end[VERTICAL_RIGHT]  = INTERSECT;
            horizonal_end[HORIZONTAL_DOWN] = HORIZONTAL_DOWN;
            horizonal_end[HORIZONTAL_UP]   = HORIZONTAL_UP;
            horizonal_end[INTERSECT]       = INTERSECT;
        }

        static int[] horizonal = new int[MAX+1];
        static {
            horizonal[TOP_RIGHT]       = HORIZONTAL_DOWN;
            horizonal[VERTICAL_LINE]   = INTERSECT;
            horizonal[HORIZONTAL_LINE] = HORIZONTAL_LINE;
            horizonal[TOP_LEFT]        = HORIZONTAL_DOWN;
            horizonal[BOTTOM_RIGHT]    = HORIZONTAL_UP;
            horizonal[BOTTOM_LEFT]     = HORIZONTAL_UP;
            horizonal[VERTICAL_LEFT]   = INTERSECT;
            horizonal[VERTICAL_RIGHT]  = INTERSECT;
            horizonal[HORIZONTAL_DOWN] = HORIZONTAL_DOWN;
            horizonal[HORIZONTAL_UP]   = HORIZONTAL_UP;
            horizonal[INTERSECT]       = INTERSECT;
        }

/*
        static int[] vertical_start = new int[MAX+1];
        static {
            vertical_start[TOP_RIGHT]       = TOP_RIGHT;
            vertical_start[VERTICAL_LINE]   = VERTICAL_LINE;
            vertical_start[HORIZONTAL_LINE] = TOP_LEFT;
            vertical_start[TOP_LEFT]        = TOP_LEFT;
            vertical_start[BOTTOM_RIGHT]    = VERTICAL_LEFT;
            vertical_start[BOTTOM_LEFT]     = VERTICAL_RIGHT;
            vertical_start[VERTICAL_LEFT]   = VERTICAL_LEFT;
            vertical_start[VERTICAL_RIGHT]  = VERTICAL_RIGHT;
            vertical_start[HORIZONTAL_DOWN] = HORIZONTAL_DOWN;
            vertical_start[HORIZONTAL_UP]   = INTERSECT;
            vertical_start[INTERSECT]       = INTERSECT;
        }
*/        
/*
        static int[] vertical_end = new int[MAX+1];
        static {
            vertical_end[TOP_RIGHT]       = VERTICAL_LEFT;
            vertical_end[VERTICAL_LINE]   = VERTICAL_LINE;
            vertical_end[HORIZONTAL_LINE] = HORIZONTAL_UP;
            vertical_end[TOP_LEFT]        = VERTICAL_RIGHT;
            vertical_end[BOTTOM_RIGHT]    = BOTTOM_RIGHT;
            vertical_end[BOTTOM_LEFT]     = BOTTOM_LEFT;
            vertical_end[VERTICAL_LEFT]   = VERTICAL_LEFT;
            vertical_end[VERTICAL_RIGHT]  = VERTICAL_RIGHT;
            vertical_end[HORIZONTAL_DOWN] = INTERSECT;
            vertical_end[HORIZONTAL_UP]   = HORIZONTAL_UP;
            vertical_end[INTERSECT]       = INTERSECT;
        }
*/
        static int[] vertical = new int[MAX+1];
        static {
            vertical[TOP_RIGHT]       = VERTICAL_LEFT;
            vertical[VERTICAL_LINE]   = VERTICAL_LINE;
            vertical[HORIZONTAL_LINE] = INTERSECT;
            vertical[TOP_LEFT]        = VERTICAL_RIGHT;
            vertical[BOTTOM_RIGHT]    = VERTICAL_LEFT;
            vertical[BOTTOM_LEFT]     = VERTICAL_RIGHT;
            vertical[VERTICAL_LEFT]   = VERTICAL_LEFT;
            vertical[VERTICAL_RIGHT]  = VERTICAL_RIGHT;
            vertical[HORIZONTAL_DOWN] = INTERSECT;
            vertical[HORIZONTAL_UP]   = INTERSECT;
            vertical[INTERSECT]       = INTERSECT;
        }

        
        static private char drawLineCharHelper(BlockChars blockChars, int[] idxMappingArray, char current, char defaultChar) { 
            int idx = toBlockCharIDX(current);
            if (idx<0) {
                return defaultChar;
            }
            
            int mappedIDX = idxMappingArray[idx];
            return blockChars.toBlockChar(mappedIDX);
        }

        static public char drawHorizontalStart(BlockChars blockChars, char current) { 
            
            return drawLineCharHelper(blockChars, horizonal_start, current, blockChars.getHORIZONTAL_LINE());
        }

        static public char drawHorizontal(BlockChars blockChars, char current) { 
            if (current == blockChars.getINTERSECT()) {
                //return blockChars.getINTERSECT();
                return drawLineCharHelper(blockChars, horizonal, current, blockChars.getHORIZONTAL_LINE());
            }
            return drawLineCharHelper(blockChars, horizonal, current, blockChars.getHORIZONTAL_LINE());
        }

        static public char drawHorizontalEnd(BlockChars blockChars, char current) { 
            return drawLineCharHelper(blockChars, horizonal_end, current, blockChars.getHORIZONTAL_LINE());
        }
        
        /*
        static public char drawHorizontalEnd(BlockChars blockChars, GetCharAt getCharAt) { 
            boolean hasBelow = hasAboveComponent(toBlockCharIDX(getCharAt.getCharAt(1, 0)));
            boolean hasAbove =  hasLeftComponent(toBlockCharIDX(getCharAt.getCharAt(0, -1)));
            boolean hasRight = hasRightComponent(toBlockCharIDX(getCharAt.getCharAt(0, 1)));
            
            if (hasBelow && hasLeft && hasRight) {
                return blockChars.toBlockChar( INTERSECT );
            }
            
            if (hasBelow && hasLeft ) {
                return blockChars.toBlockChar( VERTICAL_LEFT );
            }

            if (hasBelow && hasRight ) {
                return blockChars.toBlockChar( VERTICAL_RIGHT );
            }
            
            if (hasBelow) {
                return blockChars.toBlockChar( VERTICAL_LINE );
            }
            
            if (hasLeft && hasRight ) {
                return blockChars.toBlockChar( HORIZONTAL_UP );
            }

            if (hasLeft ) {
                return blockChars.toBlockChar( BOTTOM_RIGHT );
            }

            if (hasRight) {
                return blockChars.toBlockChar( BOTTOM_LEFT );
            }
            
            return blockChars.getVERTICAL_LINE();
            
            //return drawLineCharHelper(blockChars, vertical_end, current, blockChars.getVERTICAL_LINE());
        }
        
        */
        
        static public void drawHorizontalLine(BlockChars blockChars, int len, GetCharAt getCharAt, PlotCharAt plotCharAt ) {
            plotCharAt.setCharAt(0,0, drawHorizontalStart(blockChars, getCharAt.getCharAt(0,0)));
            for (int i=1; i<len-1; i++) {
                plotCharAt.setCharAt(0,i, drawHorizontal(blockChars, getCharAt.getCharAt(0,i)));
            }
            plotCharAt.setCharAt(0,len-1, drawHorizontalEnd(blockChars, getCharAt.getCharAt(0,len-1)));
        }
        
        static public void drawHorizontalLine(IScreenLineSegmentsBuffer segments, Color drawColor, BlockChars blockChars, Position pos, int len ) {
            GetCharAt  charAT    = (row, col)     -> { 
                                                      String charStr = segments.getCharAt(pos.getRow()+row, pos.getCol()+col);
                                                      return charStr == null ? 0 : charStr.charAt(0);
                                                   };
            PlotCharAt setCharAT = (row, col, ch) -> segments.setCharAt(pos.getRow()+row, pos.getCol()+col, ch, drawColor );
            drawHorizontalLine(blockChars, len, charAT, setCharAT);
        }

        static private char drawVerticalStart(BlockChars blockChars, GetCharAt getCharAt) { 
            boolean hasAbove = hasAboveComponent(toBlockCharIDX(getCharAt.getCharAt(-1, 0)));
            boolean hasLeft =  hasLeftComponent(toBlockCharIDX(getCharAt.getCharAt(0, -1)));
            boolean hasRight = hasRightComponent(toBlockCharIDX(getCharAt.getCharAt(0, 1)));

            if (hasAbove && hasLeft && hasRight) {
                return blockChars.toBlockChar( INTERSECT );
            }
            
            if (hasAbove && hasLeft ) {
                return blockChars.toBlockChar( VERTICAL_LEFT );
            }

            if (hasAbove && hasRight ) {
                return blockChars.toBlockChar( VERTICAL_RIGHT );
            }
            
            if (hasAbove) {
                return blockChars.toBlockChar( VERTICAL_LINE );
            }
            
            if (hasLeft && hasRight ) {
                return blockChars.toBlockChar( HORIZONTAL_DOWN );
            }

            if (hasLeft ) {
                return blockChars.toBlockChar( TOP_RIGHT );
            }

            if (hasRight) {
                return blockChars.toBlockChar( TOP_LEFT );
            }
            
            return blockChars.getVERTICAL_LINE();
        }

        static private char drawVertical(BlockChars blockChars, char current) { 
            
            return drawLineCharHelper(blockChars, vertical, current, blockChars.getVERTICAL_LINE());
        }

        static public char drawVerticalEnd(BlockChars blockChars, GetCharAt getCharAt) { 
            boolean hasBelow = hasBelowComponent(toBlockCharIDX(getCharAt.getCharAt(1, 0)));
            boolean hasLeft =  hasLeftComponent(toBlockCharIDX(getCharAt.getCharAt(0, -1)));
            boolean hasRight = hasRightComponent(toBlockCharIDX(getCharAt.getCharAt(0, 1)));
            
            if (hasBelow && hasLeft && hasRight) {
                return blockChars.toBlockChar( INTERSECT );
            }
            
            if (hasBelow && hasLeft ) {
                return blockChars.toBlockChar( VERTICAL_LEFT );
            }

            if (hasBelow && hasRight ) {
                return blockChars.toBlockChar( VERTICAL_RIGHT );
            }
            
            if (hasBelow) {
                return blockChars.toBlockChar( VERTICAL_LINE );
            }
            
            if (hasLeft && hasRight ) {
                return blockChars.toBlockChar( HORIZONTAL_UP );
            }

            if (hasLeft ) {
                return blockChars.toBlockChar( BOTTOM_RIGHT );
            }

            if (hasRight) {
                return blockChars.toBlockChar( BOTTOM_LEFT );
            }

            return blockChars.getVERTICAL_LINE();

            //return drawLineCharHelper(blockChars, vertical_end, current, blockChars.getVERTICAL_LINE());
        }

        static public void drawVerticalLine(BlockChars blockChars, int len, GetCharAt getCharAt, PlotCharAt plotCharAt ) {
            plotCharAt.setCharAt(0,0, drawVerticalStart(blockChars, getCharAt));
            for (int i=1; i<len-1; i++) {
                plotCharAt.setCharAt(i, 0, drawVertical(blockChars, getCharAt.getCharAt(i,0)));
            }
            plotCharAt.setCharAt(len-1,0, drawVerticalEnd(blockChars, (row, col)-> getCharAt.getCharAt(row+len-1, col)  ));
        }

        static public void drawVerticalLine(IScreenLineSegmentsBuffer segments, Color drawColor, BlockChars blockChars, Position pos, int height ) {
            GetCharAt  charAT    = (row, col)     -> {
                                                      String charStr = segments.getCharAt(pos.getRow()+row, pos.getCol()+col);
                                                      return charStr == null ? 0 : charStr.charAt(0);
                                                   };
            PlotCharAt setCharAT = (row, col, ch) -> segments.setCharAt(pos.getRow()+row, pos.getCol()+col, ch, drawColor );
            drawVerticalLine(blockChars, height, charAT, setCharAT);
        }

        static public void drawBox(IScreenLineSegmentsBuffer segments, Color drawColor, BlockChars blockChars, Rectangle rect ) {
            drawHorizontalLine(segments, drawColor, blockChars, rect.getPostion_TopLeft(), rect.getWidth());
            drawHorizontalLine(segments, drawColor, blockChars, rect.getPostion_BottomLeft(), rect.getWidth());
            drawVerticalLine(segments, drawColor, blockChars, rect.getPostion_TopLeft(), rect.getHeight());
            drawVerticalLine(segments, drawColor, blockChars, rect.getPostion_TopRight(), rect.getHeight());
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

    public static class BlockHiliter implements Hiliter {

        BlockChars blockChars = NICE;

        @Override
        public void hilite() {
            blockChars = NICE_DOUBLE;
        }

        @Override
        public void unHilite() {
            blockChars = NICE;
        }

        public BlockChars getBlockChars() {
            return blockChars;
        }

    };

    

}
