package dtb.terminalIO.events;

import java.util.Arrays;
import java.util.Comparator;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Encapsulates keyboard keys 
 */
public class Keys {

    public static char ESC = 27;

    public static class KeySequence {
	int[] keys;

	public KeySequence(int... keys) {
	    this.keys = keys;
	}

	public KeySequence(int key) {
	    this(new int[] { key });
	}

	public boolean startsWith(KeySequence prefix) {
	    int plen = prefix.getLength();
	    if (plen>getLength() || keys == null) return false;
	    for (int i=0; i < plen; i++) {
		if (this.keys[i] != prefix.keys[i]) {
		    return false;
		}
	    }
	    return true;
	}

	public String toString() {
	    StringBuffer sb = new StringBuffer();
	    StringJoiner sj = new StringJoiner(",");
	    for (int i = 0; i < keys.length; i++) {
		sj.add(((int) keys[i]) + "");
	    }
	    sb.append("keys : (" + sj.toString() + ")");
	    return sb.toString();
	}

	public int[] getKeys() {
	    return keys;
	}

	public int getLength() {
	    return this.keys == null ? 0 : this.keys.length;
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + Arrays.hashCode(keys);
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
	    KeySequence other = (KeySequence) obj;
	    if (!Arrays.equals(keys, other.keys))
		return false;
	    return true;
	}

    }

    static Comparator<KeySequence> keySequenceComparator = new Comparator<KeySequence>() {
	@Override
	public int compare(KeySequence a1, KeySequence a2) {
	    int len = Math.min(a1.getLength(), a2.getLength());
	    for (int i = 0; i < len; i++) {
		int cmp = Integer.compare(a1.keys[i], a2.keys[i]);
		if (cmp != 0)
		    return cmp;
	    }
	    // at this point all elements equal so
	    return Integer.compare(a1.getLength(), a2.getLength());
	}
    };

    public static class KeyStroke {
	boolean altPressed = false;
	boolean ctrl = false;
	KeySequence keys;
	int funcKey = 0;
	String name;

	public static KeyStroke fromFuncKey(int funcKey) {
	    KeyStroke ks = new KeyStroke();
	    ks.funcKey = funcKey;
	    ks.name = "F" + funcKey;
	    return ks;
	}

	private KeyStroke() {
	}

	public KeyStroke(int[] keys, String name) {
	    super();
	    this.keys = new KeySequence(keys);
	    this.name = name;
	}

	protected KeyStroke(int[] keys) {
	    super();
	    this.keys = new KeySequence(keys);
	    this.name = "??";
	}

	public KeyStroke(int key, String name) {
	    super();
	    this.keys = new KeySequence(key);
	    this.name = name;
	}

	public KeyStroke(int key, String name, boolean altPressed) {
	    this(key, name);
	    this.altPressed = altPressed;
	}

	public String getName() {
	    return name;
	}

	public void setName(String name) {
	    this.name = name;
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + (altPressed ? 1231 : 1237);
	    result = prime * result + (ctrl ? 1231 : 1237);
	    result = prime * result + funcKey;
	    result = prime * result + ((keys == null) ? 0 : keys.hashCode());
	    result = prime * result + ((name == null) ? 0 : name.hashCode());
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
	    KeyStroke other = (KeyStroke) obj;
	    if (altPressed != other.altPressed)
		return false;
	    if (ctrl != other.ctrl)
		return false;
	    if (funcKey != other.funcKey)
		return false;
	    if (keys == null) {
		if (other.keys != null)
		    return false;
	    } else if (!keys.equals(other.keys))
		return false;
	    if (name == null) {
		if (other.name != null)
		    return false;
	    } else if (!name.equals(other.name))
		return false;
	    return true;
	}

	@Override
	public String toString() {
	    return "KeyStroke [name=" + name + " altPressed=" + altPressed + ", key(s)=" + keys + ", funcKey="
		    + funcKey + "]";
	}

    }

    static Comparator<KeyStroke> keyStrokeComparator = new Comparator<KeyStroke>() {
	@Override
	public int compare(KeyStroke ks1, KeyStroke ks2) {
	    return keySequenceComparator.compare(ks1.keys, ks2.keys);
	}
    };

    public static TreeSet<KeyStroke> keystrokesSet = new TreeSet<KeyStroke>(keyStrokeComparator);

    // (int[] keys, String name)
    public static KeyStroke storeNewKeyStroke(KeyStroke ks) {
	keystrokesSet.add(ks);
	return ks;
    }

    public static KeyStroke createAndStoreNewKeyStroke(int[] keys, String name, boolean alt) {
	KeyStroke ks = new KeyStroke(keys, name);
	ks.altPressed = alt;
	storeNewKeyStroke(ks);
	return ks;
    }

    public static KeyStroke createAndStoreNewKeyStroke(int[] keys, String name) {
	return createAndStoreNewKeyStroke(keys, name, false);
    }

    private static KeyStroke createAndStoreNewKeyStroke(int key, String name, boolean alt) {
	return createAndStoreNewKeyStroke(new int[] { key }, name, alt);
    }

    private static KeyStroke createAndStoreNewKeyStroke(int key, String name) {
	return createAndStoreNewKeyStroke(new int[] { key }, name);
    }
    
    public static final KeyStroke F1_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27,79,80 }, "F1");
    public static final KeyStroke F2_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27,79,81 }, "F2");
    public static final KeyStroke F3_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27,79,82 }, "F3");
    public static final KeyStroke F4_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27,79,83 }, "F4");
    
    public static final KeyStroke F5_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27,91,49,53,126 }, "F5");
    public static final KeyStroke F6_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27,91,49,55,126 }, "F6");
    public static final KeyStroke F7_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27,91,49,56,126 }, "F7");
    public static final KeyStroke F8_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27,91,49,57,126 }, "F8");
    
    public static final KeyStroke F9_KEYSTROKE  = createAndStoreNewKeyStroke(new int[] { 27,91,50,48,126 },  "F9");
    public static final KeyStroke F10_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27,91,50,49,126 }, "F10");
    public static final KeyStroke F11_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27,91,50,51,126 }, "F11");
    public static final KeyStroke F12_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27,91,50,52,126 }, "F12");
    

    public static final KeyStroke UP_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27, 91, 65 }, "Up");
    public static final KeyStroke DOWN_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27, 91, 66 }, "Down");
    public static final KeyStroke LEFT_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27, 91, 68 }, "Left");
    public static final KeyStroke RIGHT_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27, 91, 67 }, "Right");

    public static final KeyStroke PGUP_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27, 91, 53, 126 }, "PgUp");
    public static final KeyStroke PGDN_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27, 91, 54, 126 }, "PgDn");

    public static final KeyStroke HOME_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27, 79, 72 }, "Home");
    public static final KeyStroke END_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27, 79, 70 }, "End");

    public static final KeyStroke INSERT_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27, 91, 50, 126 }, "Insert");
    public static final KeyStroke DELETE_KEYSTROKE = createAndStoreNewKeyStroke(new int[] { 27, 91, 51, 126 }, "Delete");

    public static final KeyStroke BACKSPACE_KEYSTROKE = createAndStoreNewKeyStroke(127, "Backspace");

    public static KeyStroke ESC_KEYSTROKE = createAndStoreNewKeyStroke(ESC, "ESC");
    public static KeyStroke ENTER_KEYSTROKE = createAndStoreNewKeyStroke(13, "ENTER");
    public static KeyStroke TAB_KEYSTROKE = createAndStoreNewKeyStroke('\t', "TAB");

    public static KeyStroke SHIFT_TAB_KEYSTROKE = createAndStoreNewKeyStroke(new int[] {27, 91, 90}, "SHIFT-TAB");
    
    public static KeyStroke ALT_ESC_KEYSTROKE = createAndStoreNewKeyStroke(ESC, "ALT-ESC", true);
    public static KeyStroke ALT_ENTER_KEYSTROKE = createAndStoreNewKeyStroke(13, "ALT-ENTER", true);
    public static KeyStroke ALT_TAB_KEYSTROKE = createAndStoreNewKeyStroke('\t', "ALT-TAB", true);

    public static KeyStroke A_KEYSTROKE = createAndStoreNewKeyStroke('A', "A");
    public static KeyStroke a_KEYSTROKE = createAndStoreNewKeyStroke('a', "a");

    public static KeyStroke B_KEYSTROKE = createAndStoreNewKeyStroke('B', "B");

    public static KeyStroke CTRL_A_KEYSTROKE = controlCharKeyStrokeAndStore(1);
    public static KeyStroke CTRL_B_KEYSTROKE = controlCharKeyStrokeAndStore(2);
    public static KeyStroke CTRL_C_KEYSTROKE = controlCharKeyStrokeAndStore(3);
    public static KeyStroke CTRL_D_KEYSTROKE = controlCharKeyStrokeAndStore(4);
    public static KeyStroke CTRL_E_KEYSTROKE = controlCharKeyStrokeAndStore(5);
    public static KeyStroke CTRL_F_KEYSTROKE = controlCharKeyStrokeAndStore(6);
    public static KeyStroke CTRL_G_KEYSTROKE = controlCharKeyStrokeAndStore(7);
    public static KeyStroke CTRL_H_KEYSTROKE = controlCharKeyStrokeAndStore(8);
    public static KeyStroke CTRL_I_KEYSTROKE = controlCharKeyStrokeAndStore(9);
    public static KeyStroke CTRL_J_KEYSTROKE = controlCharKeyStrokeAndStore(10);
    public static KeyStroke CTRL_K_KEYSTROKE = controlCharKeyStrokeAndStore(11);
    public static KeyStroke CTRL_L_KEYSTROKE = controlCharKeyStrokeAndStore(12);
    public static KeyStroke CTRL_M_KEYSTROKE = controlCharKeyStrokeAndStore(13);
    public static KeyStroke CTRL_N_KEYSTROKE = controlCharKeyStrokeAndStore(14);
    public static KeyStroke CTRL_O_KEYSTROKE = controlCharKeyStrokeAndStore(15);
    public static KeyStroke CTRL_P_KEYSTROKE = controlCharKeyStrokeAndStore(16);
    public static KeyStroke CTRL_Q_KEYSTROKE = controlCharKeyStrokeAndStore(17);
    public static KeyStroke CTRL_R_KEYSTROKE = controlCharKeyStrokeAndStore(18);
    public static KeyStroke CTRL_S_KEYSTROKE = controlCharKeyStrokeAndStore(19);
    public static KeyStroke CTRL_T_KEYSTROKE = controlCharKeyStrokeAndStore(20);
    public static KeyStroke CTRL_U_KEYSTROKE = controlCharKeyStrokeAndStore(21);
    public static KeyStroke CTRL_V_KEYSTROKE = controlCharKeyStrokeAndStore(22);
    public static KeyStroke CTRL_W_KEYSTROKE = controlCharKeyStrokeAndStore(23);
    public static KeyStroke CTRL_X_KEYSTROKE = controlCharKeyStrokeAndStore(24);
    public static KeyStroke CTRL_Y_KEYSTROKE = controlCharKeyStrokeAndStore(25);
    public static KeyStroke CTRL_Z_KEYSTROKE = controlCharKeyStrokeAndStore(26);

    public static class KeyEvent extends TerminalEvent<KeyStroke> {

	private static final long serialVersionUID = -6657532777213840024L;

	public KeyEvent(Object source, KeyStroke eventData) {
	    super(source, TYPE.KEYPRESS, eventData);
	}

	public boolean isKeyStroke(KeyStroke keyStroke) {
	    return this.getEventData().equals(keyStroke);
	}

	public String getKeyName() {
	    return getEventData().name;
	}

    }

    // IS_KeyStroke.test(evt,ESC_KEYSTROKE)
    public static BiPredicate<TerminalEvent<?>, KeyStroke> IS_KeyStroke = new BiPredicate<TerminalEvent<?>, KeyStroke>() {

	@Override
	public boolean test(TerminalEvent<?> te, KeyStroke keyStroke) {
	    if (!(te instanceof KeyEvent)) {
		return false;
	    }
	    KeyEvent ke = (KeyEvent) te;
	    boolean result = ke.isKeyStroke(keyStroke);
	    return result;
	}
    };

    public static class IsKeyStrokeEventPredicate implements Predicate<TerminalEvent<?>> {

	private KeyStroke keyStroke;

	public IsKeyStrokeEventPredicate(KeyStroke keyStroke) {
	    this.keyStroke.equals(keyStroke);
	}

	@Override
	public boolean test(TerminalEvent<?> te) {
	    return IS_KeyStroke.test(te, keyStroke);
	}
    }

    public static KeyStroke controlCharKeyStroke(final int i) {

	boolean alt = false;
	switch (i) {
	case 0x0D:
	    // Carriage return --> ENTER
	    return new KeyStroke(13, "ENTER", alt);
	case 0x0A:
	    // Linefeed --> ENTER
	    return new KeyStroke(13, "ENTER", alt);
	case 0x1B:
	    // ESC
	    return new KeyStroke(ESC, "ESC", alt);
	case '\t':
	    // TAB
	    return new KeyStroke('\t', "TAB", alt);
	default:
	    // Make all other control characters come back as the alphabetic
	    // character with the ctrl field set. So SOH would be 'A' +
	    // ctrl.
	    return new KeyStroke(i, "CTRL-" + ((char) ('A' + i - 1)), alt);
	}
    }

    public static KeyStroke controlCharKeyStrokeAndStore(int key) {
	KeyStroke ks = controlCharKeyStroke(key);
	storeNewKeyStroke(ks);
	return ks;
    }

    public static KeyEvent controlChar(final Object inputSource, final char ch, final boolean alt) {

	return new KeyEvent(inputSource, controlCharKeyStroke(ch));
    }

    public static KeyEvent normalKeyEvent(final Object inputSource, char ch) {
	return new KeyEvent(inputSource, new KeyStroke(ch, "" + ch));
    }

    public static class KeyStrokeFindResult {
	public KeyStrokeFindResult(KeyStroke foundKeyStroke, char[] remainingBuffer) {
	    super();
	    this.foundKeyStroke = foundKeyStroke;
	    this.remainingBuffer = remainingBuffer;
	}

	public KeyStroke foundKeyStroke = null;
	public char[] remainingBuffer;

    }

    public static KeyStrokeFindResult getKeyStrokeFromChars(char[] buffer, int readIn) {
	int keys[] = new int[readIn];
	for (int i = 0; i < readIn; i++) {
	    keys[i] = buffer[i];
	}
	KeyStroke ksToFind = new KeyStroke(keys);
	KeyStroke ks = keystrokesSet.floor(ksToFind);
	
	Supplier<KeyStrokeFindResult> makeNullResult = new Supplier<KeyStrokeFindResult>() {
	    @Override
	    public KeyStrokeFindResult get() {
		    char newBuf[] = new char[readIn];
		    System.arraycopy(buffer, 0, newBuf, 0, readIn);
		    return new KeyStrokeFindResult(null, newBuf);
	    }
	};
	
	if (ks == null) {
	    return makeNullResult.get();
	}
	// something found - but does it match at all?
	if ( ! ksToFind.keys.startsWith(ks.keys)) {
	    return makeNullResult.get();
	}
	
	int newLen = readIn - ks.keys.getLength();
	if (newLen < 0) {
	    return makeNullResult.get();
	}
	char newBuf[] = new char[readIn - ks.keys.getLength()];
	System.arraycopy(buffer, 0, newBuf, 0, newBuf.length);
	return new KeyStrokeFindResult(ks, newBuf);
    }

}
