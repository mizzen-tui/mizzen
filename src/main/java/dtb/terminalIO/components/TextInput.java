package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.FocusManager.Focusable;
import dtb.terminalIO.events.KeyInputEventHandler;
import dtb.terminalIO.events.Keys;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;

public class TextInput extends BoundedComponent<TextInput> {

    private TextComponent textRenderer;
    private StringBuffer buffer;
    private Position caret;

    public TextInput(Color color, int length) {
	super(FGBGColors.DEFAULT, new Dimension.Simple(1, length));
	init();
    }

    public TextInput(int length) {
	this(FGBGColors.DEFAULT, length);
    }

    public TextInput(Color color, Dimension d) {
	super(color, d);
	init();
    }

    public TextInput(Color color, Position p, Dimension d) {
	super(color, p, d);
	init();
    }

    protected void init() {
	this.buffer = new StringBuffer();
	this.caret = new Position.Simple(0, 0);
	this.textRenderer = new TextComponent(this.getColor(), this.getRectangle(), this.buffer.toString());
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
	segments.merge(this.textRenderer.getSegmentsBuffer());
    }

    public void setText(String text) {
	this.buffer = new StringBuffer(text);
	this.textRenderer.setText(this.buffer.toString());
	this.markAsDirty();
    }

    public void appendText(String text) {
	this.buffer.append(text);
	this.setText(this.buffer.toString());
    }

    public KeyInputEventHandler asEventHandler() {

	EventHandler handler = new EventHandler() {
	    @Override
	    public void handle(TerminalEvent<?> te) {
		Keys.KeyEvent ke = (Keys.KeyEvent) te;

		String name = ke.getEventData().getName();
		if (name != null && name.length() == 1 && "abcdefghijklmnopqrstuvwxyz".contains(name)) {
		    TextInput.this.appendText(name);
		}
	    }
	};

	return new KeyInputEventHandler(new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS), handler);

    }

    public Focusable asFocusable(Position position) {
	return new Focusable() {
	    
	    boolean hasFocus = false;

	    @Override
	    public void handle(TerminalEvent<?> te) {
		Keys.KeyEvent ke = (Keys.KeyEvent) te;

		String name = ke.getEventData().getName();
		if (name != null && name.length() == 1 && "abcdefghijklmnopqrstuvwxyz".contains(name)) {
		    TextInput.this.appendText(name);
		}
	    }

	    @Override
	    public boolean hasFocus() {
		return hasFocus;
	    }

	    @Override
	    public void setFocus(boolean focus) {
		this.hasFocus = focus;

	    }

	    @Override
	    public Position getPosition() {
		return position;
	    }

	};
    }

}
