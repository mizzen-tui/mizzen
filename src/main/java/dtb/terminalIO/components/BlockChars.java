package dtb.terminalIO.components;

import dtb.terminalIO.util.MapChars;

/**
 * Encapsulates block drawing characters
 */
public interface BlockChars {

    // Why I didn't go the enum route I will never know.. sigh

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

    public static final int QUOTA_LEFT      = 10;

    public static final int QUOTA_RIGHT     = 11;

    public static final int MAX             = 11;

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

    public char getQUOTA_LEFT();

    public char getQUOTA_RIGHT();

    public char[] blockChars();

    public boolean isBlockChar(char ch);

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

            for (int i = MIN; i < MAX; i++) {
                if (ch == blockChars[i]) {
                    return true;
                }
            }
            return false;
        }

        private char[] makeBlockCharsARRAY() {
            char[] chars = new char[MAX];

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

            chars[QUOTA_LEFT] = getQUOTA_LEFT();

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
        public char getQUOTA_LEFT() {
            return '«';
        }

        @Override
        public char getQUOTA_RIGHT() {
            return '»';
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
        public char getQUOTA_LEFT() {
            return '<';
        }

        @Override
        public char getQUOTA_RIGHT() {
            return '>';
        }

    }

    public static final BlockChars BASIC   = new Basic();

    public static final BlockChars NICE    = new Nice();

    public static final BlockChars DEFAULT = NICE;

    public static class UTIL {

        private static MapChars NICE_TO_BASIC = null;

        public synchronized static MapChars getNICE_TO_BASIC() {

            if (NICE_TO_BASIC != null) {
                return NICE_TO_BASIC;
            }

            NICE_TO_BASIC = new MapChars();

            char[] nice_blockChars = NICE.blockChars();
            char[] basic_blockChars = BASIC.blockChars();
            
            for (int i = 0; i < nice_blockChars.length ; i++) {
                NICE_TO_BASIC.map(nice_blockChars[i],basic_blockChars[i]);
            }

            return NICE_TO_BASIC;

        }

    }

}
