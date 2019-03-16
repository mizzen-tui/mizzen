package dtb.terminalIO.components.input;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.FocusManager.Focusable;
import dtb.terminalIO.events.RequestHideCursorEvent;
import dtb.terminalIO.events.RequestShowCursorEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.TerminalEvent.IS_Event_TYPE;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class TextInput extends BoundedComponent<TextInput> {

    private TextComponent textRenderer;
    private StringBuffer buffer;
    private Position viewOffset = Position.ZERO;
    private Position caret = Position.ZERO;
    private int maxTextWidth;
    
    private boolean modeOverite = false;  // true means insertMode

    public TextInput(Color color, int length) {
	super(color, new Dimension.Simple(1, length));
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
    
    @Override
    public TextInput setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        if (this.textRenderer  != null) {
            this.textRenderer.setColor(drawingcolor);
            this.textRenderer.markAsDirty();
        }
        this.markAsDirty();
        return this;
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
        if (text == null) {
            text = "";
        }
        this.__setText(text);
        this.caret = new Position.Simple(caret.getRow(), this.getText().length());
        this.maxTextWidth = Math.max(this.getWidth(), this.getText().length());
    }

    private void __setText(String text) {
        this.buffer = new StringBuffer(text);
        this.textRenderer.setText(this.buffer.toString());
        this.markAsDirty();
    }

    
    public String getText() {
        return this.buffer.toString();
    }

    public void appendText(String text) {
        int maxWidth = this.getWidth();
        if (this.buffer.length()>=maxWidth) {
            return;  // might change this at later date to allow text to roll
        }
        // this.buffer.insert()
	this.buffer.append(text);
	if (this.buffer.length()>maxWidth) {
	    String str = this.buffer.toString();
	    this.buffer = new StringBuffer().append(str.substring(0, maxWidth));
	}
	this.setText(this.buffer.toString());
    }
    
    void caretOffsetAdd(Position offsetToAdd) {
        // todo:  should check valid?  
        this.caret = this.caret.offsetAdd(offsetToAdd);
        markAsDirty();
    }
    
    void caretOffsetAdd(int row, int col) {
        caretOffsetAdd( new Position.Simple(row, col));
    }
    
    // needs lots 'o work!  get on it
    private void addCharAtCaret(char ch) {
        String currentText = getText();
        if (modeOverite) {
            
        } else { // insert mode
            if (  currentText.length() >= maxTextWidth) {
                return ;
            }
            if (caret.getCol() == 0) {                            // []ABCDE
               currentText = ch + currentText;
            } else if (caret.getCol() < currentText.length())  {  // ABC[]DE
               currentText = currentText.substring(0, caret.getCol()) +
                       ch +
                       currentText.substring(caret.getCol());
            } else if (caret.getCol() >= currentText.length()) {  // ABCDE[] 
                currentText = currentText + ch;
            }
            caretOffsetAdd(0,1);
            this.__setText(currentText);
        }
    }

    private void deleteCharBeforeCaret() {
        String currentText = getText();
        if (modeOverite) {
            
        } else { // insert mode
            if (caret.getCol() == 0) {                            // []ABCDE
               return ;
            } else if (caret.getCol() < currentText.length())  {  // ABC[]DE
               currentText = currentText.substring(0, caret.getCol()-1) +
                             currentText.substring(caret.getCol());
            } else if (caret.getCol() >= currentText.length()) {  // ABCDE[] 
                currentText = currentText.substring(0, currentText.length()-1);
            }
            if (caret.getCol()>0) {
                caretOffsetAdd(0,-1);
            }
            this.__setText(currentText);
        }
    }

    private void deleteCharAtCaret() {
        String currentText = getText();
        if (modeOverite) { 
            
        } else { // insert mode
               int textlen = currentText.length();
               if (caret.getCol() < textlen ) {
                   if (caret.getCol() == 0) {
                       currentText = currentText.substring(caret.getCol()+1);
                   } else {
                      currentText = currentText.substring(0, caret.getCol())+ currentText.substring(caret.getCol()+1);
                   }
                   this.__setText(currentText);
               }
        }
    }

    public KeyInputEventHandler asEventHandler() {

	EventHandler handler = new EventHandler() {
	    @Override
	    public void handle(TerminalEvent<?> te) {
		KeyEvent ke = (KeyEvent) te;

		// check for control keys
		if (ke.isKeyStroke(Keys.BACKSPACE_KEYSTROKE)) {
		    deleteCharBeforeCaret();
		    return ;
		}

                if (ke.isKeyStroke(Keys.DELETE_KEYSTROKE)) {
                    deleteCharAtCaret();
                    return ;
                }

		if (ke.isKeyStroke(Keys.LEFT_KEYSTROKE)) {
                    if (caret.getCol()>0) {
                        caretOffsetAdd(0,-1);
                    }
                    return ;
                }

                if (ke.isKeyStroke(Keys.RIGHT_KEYSTROKE)) {
                    if (caret.getCol()< getWidth() && caret.getCol()< getText().length()) {  // TODO:  This will have to be fixed when text scrolling in place
                        caretOffsetAdd(0,1);
                        markAsDirty();
                    }
                    return ;
                }
		
                if (ke.getKeyStroke().getKeySequenceLength() == 1) {
                    // then probably a simple char - so lets see if its asci
                    // TODO:  I have discovered an problem ( try CTRL-UP ) it generates
                    //        multiple keystroke events - which is incorrect - issue with key input parser
                    //        dont try to handle this bug here - do it in the parser
//                    String name = ke.getEventData().getName();
//                    char ch = name.charAt(0);
                    char ch = ke.getChar();
                    
                    // TODO - I know I will need some checks on ch - but this for now 
                    addCharAtCaret(ch);
                    
                }
                
//		// not a control key - so check for valid text input key
//		String name = ke.getEventData().getName();
//		if (name != null && name.length() == 1 && "abcdefghijklmnopqrstuvwxyz".contains(name)) {
//		    addCharAtCaret(name.charAt(0));
//		}
	    }
	};

	return new KeyInputEventHandler(new IS_Event_TYPE(TerminalEvent.TYPE.KEYPRESS), handler);

    }

    public Focusable asFocusable(Position position, final EventBus eventbus) {
	return new Focusable() {
	    
	    boolean hasFocus = false;
	    
	    Color unhilitedColor = TextInput.this.getColor();
	    Color hilitedColor = TextInput.this.getColor().bold();
	    
	    KeyInputEventHandler keyEventHandler = TextInput.this.asEventHandler();

	    @Override
	    public void handle(TerminalEvent<?> te) {
	        keyEventHandler.handle(te);
	        requestShowCursor();  // todo:  This might be expensive to showcursor even if key is ignored
	    }
	    
	    private void requestShowCursor() {
                // todo - need manage a textinput caret (pos) -- not just append
	        
	        // calc where cursor should be relative to text component + its caret position
	        
	        Position cursorPos = new Position.Simple(getPosition()).offsetAdd(caret);

	        eventbus.addEvent(  new  RequestShowCursorEvent(TextInput.this, cursorPos ) );
	    }

            private void requestHideCursor() {
                eventbus.addEvent(  new  RequestHideCursorEvent(TextInput.this) );
            }

	    @Override
	    public boolean hasFocus() {
		return hasFocus;
	    }

	    @Override
	    public void setFocus(boolean focus) {
		this.hasFocus = focus;
		if (focus) {
		   requestShowCursor();
		   TextInput.this.setColor(  hilitedColor );
		} else {
		    requestHideCursor(); 
		    TextInput.this.setColor(  unhilitedColor );
		}
	    }

	    @Override
	    public Position getPosition() {
		return position;
	    }

	};
    }

}
