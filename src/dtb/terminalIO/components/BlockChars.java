package dtb.terminalIO.components;

/**
 * Encapsulates block drawing characters
 */
public interface BlockChars {

    // Why I didn't go the enum route I will never know.. sigh

    public static final int MIN = 0;

    public static final int TOP_RIGHT = 0;

    public static final int VERTICAL_LINE = 1;

    public static final int HORIZONTAL_LINE = 2;

    public static final int TOP_LEFT = 3;

    public static final int BOTTOM_RIGHT = 4;

    public static final int BOTTOM_LEFT = 5;

    public static final int VERTICAL_LEFT = 6;

    public static final int VERTICAL_RIGHT = 7;

    public static final int HORIZONTAL_DOWN = 8;

    public static final int HORIZONTAL_UP = 9;

    public static final int QUOTA_LEFT = 10;

    public static final int QUOTA_RIGHT = 11;

    public static final int MAX = 11;

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
    
    public default boolean isBlockChar(char ch) {
	char[] blockChars = blockChars();
	for (int i=MIN;i<MAX;i++) {
	    if (ch == blockChars[i]){
		return true;
	    }
	}
	return false;
    }
    
    public default char toBlockChar(int blockCharIDX) {
	if (blockCharIDX<MIN || blockCharIDX>MAX) throw new IllegalArgumentException(String.format("bad blockCharIDX = %d",blockCharIDX));
	return blockChars()[blockCharIDX];
    }

    static abstract class AbstractBaseBlockChars implements BlockChars {

	char[] _blockCHARS = makeBlockCharsARRAY();

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

    public static final BlockChars BASIC = new Basic();

    public static final BlockChars NICE = new Nice();

    public static final BlockChars DEFAULT = NICE;

}
