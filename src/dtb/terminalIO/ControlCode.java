package dtb.terminalIO;

import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dtb.terminalIO.ControlCode.FGBGColors.HasRowChange;
import dtb.terminalIO.TerminalContext.StringContext;

public abstract class ControlCode implements Renderer {

    public ControlCode() {
    }

    @Override
    public void render(TerminalContext ctx) {
	if (ctx.isControlCodesEnabled()) {
	    doRender(ctx);
	}
    }

    abstract protected void doRender(TerminalContext ctx);

    public static class BaseTextControlCode extends ControlCode {

	private String controlCode;

	public BaseTextControlCode(String controlCode) {
	    this.controlCode = controlCode;
	}

	public String getControlCode() {
	    return controlCode;
	}

	public void setControlCode(String controlCode) {
	    this.controlCode = controlCode;
	}

	protected void doRender(TerminalContext ctx) {
	    // ctx.write(this.getControlCode());
	    ctx.write(this);
	}
    }

    public static abstract class Color extends ControlCode {

	int colorCode = 0;

	public Color(int colorCode) {
	    this.colorCode = colorCode;
	}

	public int getColorCode() {
	    return colorCode;
	}

	public void setColorCode(int colorCode) {
	    this.colorCode = colorCode;
	}

	abstract protected int[] buildControlCodes();

	protected String buildColorControlCode() {
	    int codes[] = buildControlCodes();
	    StringJoiner sj = new StringJoiner(";");
	    for (int code : codes) {
		sj.add(code + "");
	    }
	    return escape_PREFIX + sj.toString() + "m";
	}

	protected void doRender(TerminalContext ctx) {
	    if (ctx.isColorEnabled()) {
		ctx.write(this.buildColorControlCode());
	    }
	}
    }

    /**
     * Foreground Color
     */
    public static class FgColor extends Color {

	boolean bold = false;
	boolean italic = false;
	boolean underline = false;

	public FgColor(int colorCode) {
	    super(colorCode);
	}

	public FgColor(int colorCode, boolean bold, boolean italic, boolean underline) {
	    this(colorCode);
	    this.bold = bold;
	    this.italic = italic;
	    this.underline = underline;
	}

	public FgColor asBold() {
	    return new FgColor(colorCode, true, italic, underline);
	}

	public FgColor removeBold() {
	    return new FgColor(colorCode, false, italic, underline);
	}

	public FgColor asItalic() {
	    return new FgColor(colorCode, bold, true, underline);
	}

	public FgColor asUnderline() {
	    return new FgColor(colorCode, bold, italic, true);
	}

	@Override
	protected int[] buildControlCodes() {
	    int codes[] = new int[(bold ? 1 : 0) + (italic ? 1 : 0) + (underline ? 1 : 0) + 2];
	    int idx = 0;
	    codes[idx++] = 0; // reset previous attributes
	    if (bold)
		codes[idx++] = 1; // conditionally set bold
	    if (italic)
		codes[idx++] = 3; // conditionally set italic
	    if (underline)
		codes[idx++] = 4; // conditionally set underline
	    codes[idx] = this.getColorCode(); // now finally set the foreground
					      // color
	    return codes;
	}

	public String toString() {
	    String name = Colors.getNameOfFGColor(this);
	    if (name == null) {
		name = this.buildColorControlCode();
	    }
	    return "[FG=" + name + "]";
	}

    }

    /*
     * Background color
     */
    public static class BgColor extends Color {

	public BgColor(int colorCode) {
	    super(colorCode);
	}

	@Override
	protected int[] buildControlCodes() {
	    return new int[] { this.getColorCode() };
	}

	public String toString() {
	    String name = Colors.getNameOfBGColor(this);
	    if (name == null) {
		name = this.buildColorControlCode();
	    }
	    return "[BG=" + name + "]";
	}
    }

    /**
     * encapsulate foreground and background colors together
     */
    public static class FGBGColors implements Renderer {

	// public static final FGBGColors DEFAULT = new
	// FGBGColors(Colors.On_Bla,Colors.Whi);
	public static final FGBGColors DEFAULT = new FGBGColors(Colors.Whi, null);

	public static final FGBGColors NO_COLOR_SPECIFIED = new FGBGColors(null, null);

	BgColor bgc;
	FgColor fgc;

	public FGBGColors(FgColor fgc, BgColor bgc) {
	    super();
	    this.bgc = bgc;
	    this.fgc = fgc;
	}

	public FGBGColors(FgColor fgc) {
	    super();
	    this.bgc = null;
	    this.fgc = fgc;
	}

	public FGBGColors(BgColor bgc) {
	    super();
	    this.bgc = bgc;
	    this.fgc = null;
	}

	public FGBGColors bold() {
	    return new FGBGColors(fgc.asBold(), bgc);
	}

	public FGBGColors removebold() {
	    return new FGBGColors(fgc.removeBold(), bgc);
	}

	public BgColor getBgc() {
	    return bgc;
	}

	public void setBgc(BgColor bgc) {
	    this.bgc = bgc;
	}

	public FgColor getFgc() {
	    return fgc;
	}

	public void setFgc(FgColor fgc) {
	    this.fgc = fgc;
	}

	public boolean isMergeable() {
	    return this.getBgc() == null || this.getFgc() == null;
	}

	public FGBGColors mergeWith(FGBGColors other) {
	    FGBGColors newColor = new FGBGColors(this.getFgc() != null ? this.getFgc() : other.getFgc(),
		    this.getBgc() != null ? this.getBgc() : other.getBgc());
	    return newColor;
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((bgc == null) ? 0 : bgc.hashCode());
	    result = prime * result + ((fgc == null) ? 0 : fgc.hashCode());
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
	    FGBGColors other = (FGBGColors) obj;
	    if (bgc == null) {
		if (other.bgc != null)
		    return false;
	    } else if (!bgc.equals(other.bgc))
		return false;
	    if (fgc == null) {
		if (other.fgc != null)
		    return false;
	    } else if (!fgc.equals(other.fgc))
		return false;
	    return true;
	}

	@Override
	public void render(TerminalContext ctx) {
	    if (ctx.isColorEnabled()) {
		if (fgc != null)
		    fgc.render(ctx);
		if (bgc != null)
		    bgc.render(ctx);
	    }
	}

	public FGBGColors newFrom(FgColor fgcNew) {
	    return new FGBGColors(fgcNew, this.getBgc());
	}

	public FGBGColors newFrom(BgColor bgcNew) {
	    return new FGBGColors(this.getFgc(), bgcNew);
	}

	public String toString() {
	    return "(" + String.valueOf(fgc) + "," + String.valueOf(bgc) + ")";
	}

	public static interface Provider {
	    FGBGColors getColor();
	}

	public interface HasRowChange {
	    int deltaRow();
	}

    }

    public static final String escape_CHR = "\u001B";
    public static final String escape_PREFIX = escape_CHR + "[";

    public static final ControlCode RCol = new Color(0) {
	@Override
	protected int[] buildControlCodes() {
	    return new int[] { 0 };
	}
    };

    // erasing ..

    public static final ControlCode ERASE_END_OF_LINE = new BaseTextControlCode(escape_CHR + "[K");
    public static final ControlCode ERASE_START_OF_LINE = new BaseTextControlCode(escape_CHR + "[1K");
    public static final ControlCode ERASE_LINE = new BaseTextControlCode(escape_CHR + "[2K");
    public static final ControlCode ERASE_DOWN = new BaseTextControlCode(escape_CHR + "[J");
    public static final ControlCode ERASE_UP = new BaseTextControlCode(escape_CHR + "[1J");
    public static final ControlCode ERASE_SCREEN = new BaseTextControlCode(escape_CHR + "[2J");

    // \033[?25l")
    public static final ControlCode HIDE_CURSOR = new BaseTextControlCode(escape_CHR + "[?25l");
    public static final ControlCode SHOW_CURSOR = new BaseTextControlCode(escape_CHR + "[?25h");

    public static final String CURSOR_FAR_LEFT_STR = escape_CHR + "[G";
    public static final ControlCode CURSOR_LEFT = new BaseTextControlCode(escape_CHR + "[G");

    protected static class CursorMovementWithCount extends BaseTextControlCode {

	protected int count;

	public CursorMovementWithCount(int count, String suffixChar) {
	    super(escape_CHR + "[" + count + suffixChar);
	    this.count = count;
	}

	protected int getCount() {
	    return count;
	}

    }

    public static class CursorUp extends CursorMovementWithCount implements HasRowChange {
	public CursorUp(int nLines) {
	    super(nLines, "A");
	}

	@Override
	public int deltaRow() {
	    return -getCount();
	}
    }

    public static class CursorDown extends CursorMovementWithCount implements HasRowChange {
	public CursorDown(int nLines) {
	    super(nLines, "B");
	}

	@Override
	public int deltaRow() {
	    return getCount();
	}
    }

    public static class CursorForward extends CursorMovementWithCount {
	public CursorForward(int nLines) {
	    super(nLines, "C");
	}
    }

    public static class CursorBackward extends CursorMovementWithCount {
	public CursorBackward(int nLines) {
	    super(nLines, "D");
	}
    }

    public static class CursorUpAndClearLines implements Renderer, HasRowChange {
	int nLines;

	public CursorUpAndClearLines(int nLines) {
	    this.nLines = nLines;
	}

	@Override
	public void render(TerminalContext ctx) {
	    if (!ctx.isControlCodesEnabled()) {
		return;
	    }
	    RendererBuffer rb = new RendererBuffer();
	    rb.append(new CursorUp(nLines));
	    for (int i = 0; i < nLines; i++) {
		// rb.append("|");
		rb.append(ERASE_END_OF_LINE);
		rb.append(Renderer.CR); // todo - should I create and use a
					// movedown renderer? .. might be
					// smarter
	    }
	    rb.append(new CursorUp(nLines));
	    rb.render(ctx);
	}

	@Override
	public int deltaRow() {
	    return -nLines;
	}

    }

    static String colorCODE_patternStr = "\\e\\[[0-9,;]*m";
    static Pattern colorCODE_pattern = Pattern.compile(colorCODE_patternStr);

    public static boolean hasColorCodes(String source) {
	Matcher matcher = colorCODE_pattern.matcher(source);
	boolean matchFound = matcher.find();
	return matchFound;
    }

    public static String removeColorCodes(String source) {

	Matcher matcher = colorCODE_pattern.matcher(source);
	boolean matchFound = matcher.find();
	if (!matchFound) {
	    return source;
	}
	StringBuffer sb = new StringBuffer();
	while (matchFound) {
	    matcher.appendReplacement(sb, "");
	    matchFound = matcher.find();
	}
	matcher.appendTail(sb);

	return sb.toString();
    }

}
