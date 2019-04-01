package dtb.terminalIO.components.input;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.FocusSetterToHiliteAction;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.Focusable;
import dtb.terminalIO.events.RequestHideCursorEvent;
import dtb.terminalIO.events.RequestShowCursorEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class TextInput extends BoundedComponent<TextInput> implements HasValue<String>, ParentContainer /*, Focusable */ {

    private TextComponent textRenderer;
    private StringBuffer  buffer;
    private Position      viewOffset  = Position.ZERO;
    private Position      caret       = Position.ZERO;
    private int           maxTextWidth;

    private boolean       modeOverite = false;        // true means insertMode

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
        if (this.textRenderer != null) {
            this.textRenderer.setColor(drawingcolor);
            this.textRenderer.markAsDirty();
        }
        this.markAsDirty();
        return this;
    }

    public TextInput setEmptyColor(Color emptyColor) {
        super.setEmptyColor(emptyColor);
        if (this.textRenderer != null) {
            this.textRenderer.setEmptyColor(emptyColor);
        }
        return this;
    }
    
    public TextInput setEmptyChar(char emptyChar) {
        super.setEmptyChar(emptyChar);
        if (this.textRenderer != null) {
            this.textRenderer.setEmptyChar(emptyChar);
        }
        return this;
    }

    protected void init() {
        this.buffer = new StringBuffer();
        this.caret = new Position.Simple(0, 0);
        this.setFillOnClear(true);
        this.textRenderer = makeChildComponent( ()-> new TextComponent(TextInput.this.getColor(), TextInput.this.getRectangle(), TextInput.this.buffer.toString()));
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.textRenderer.getSegmentsBuffer());
    }

    
    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == this.textRenderer) {
            return Position.ZERO;
        }
        return null;
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

    public String getValue() {
        return this.getText();
    }

    public void appendText(String text) {
        int maxWidth = this.getWidth();
        if (this.buffer.length() >= maxWidth) {
            return; // might change this at later date to allow text to roll
        }
        // this.buffer.insert()
        this.buffer.append(text);
        if (this.buffer.length() > maxWidth) {
            String str = this.buffer.toString();
            this.buffer = new StringBuffer().append(str.substring(0, maxWidth));
        }
        this.setText(this.buffer.toString());
    }

    void caretOffsetAdd(Position offsetToAdd) {
        // todo: should check valid?
        this.caret = this.caret.offsetAdd(offsetToAdd);
        markAsDirty();
    }

    void caretOffsetAdd(int row, int col) {
        caretOffsetAdd(new Position.Simple(row, col));
    }

    // needs lots 'o work! get on it
    private boolean addCharAtCaret(char ch) {
        String currentText = getText();
        if (modeOverite) {
            return false;
        } else { // insert mode
            if (currentText.length() >= maxTextWidth) {
                return false;
            }
            if (caret.getCol() == 0) { // []ABCDE
                currentText = ch + currentText;
            } else if (caret.getCol() < currentText.length()) { // ABC[]DE
                currentText = currentText.substring(0, caret.getCol()) + ch + currentText.substring(caret.getCol());
            } else if (caret.getCol() >= currentText.length()) { // ABCDE[]
                currentText = currentText + ch;
            }
            caretOffsetAdd(0, 1);
            this.__setText(currentText);
            return true;
        }
    }

    private boolean deleteCharBeforeCaret() {
        String currentText = getText();
        if (modeOverite) {
            return false;
        } else { // insert mode
            if (caret.getCol() == 0) { // []ABCDE
                return false;
            } else if (caret.getCol() < currentText.length()) { // ABC[]DE
                currentText = currentText.substring(0, caret.getCol() - 1) + currentText.substring(caret.getCol());
            } else if (caret.getCol() >= currentText.length()) { // ABCDE[]
                currentText = currentText.substring(0, currentText.length() - 1);
            }
            if (caret.getCol() > 0) {
                caretOffsetAdd(0, -1);
            }
            this.__setText(currentText);
            return true;
        }
    }

    private boolean deleteCharAtCaret() {
        String currentText = getText();
        if (modeOverite) {
            return false;
        } else { // insert mode
            int textlen = currentText.length();
            if (caret.getCol() < textlen) {
                if (caret.getCol() == 0) {
                    currentText = currentText.substring(caret.getCol() + 1);
                } else {
                    currentText = currentText.substring(0, caret.getCol()) + currentText.substring(caret.getCol() + 1);
                }
                this.__setText(currentText);
                return true;
            }
        }
        return false;
    }

    private void raiseEventOnChanged(EventBus eventBus, Supplier<Boolean> changeTextAction) {
        if (changeTextAction.get()) {
            eventBus.addEvent(new HasValue.ValueChangedEvent<>(this));
        }
    }

    private KeyInputEventHandler asEventHandler() {

        ChainedEventHandler chainedHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();

        chainedHandler.addEventHandler(
                new KeyInputEventHandler(Keys.BACKSPACE_KEYSTROKE, (te) -> raiseEventOnChanged(te.getEventBus(), () -> deleteCharBeforeCaret())));

        chainedHandler
                .addEventHandler(new KeyInputEventHandler(Keys.DELETE_KEYSTROKE, (te) -> raiseEventOnChanged(te.getEventBus(), () -> deleteCharAtCaret())));

        chainedHandler.addEventHandler(new KeyInputEventHandler(Keys.LEFT_KEYSTROKE, (te) -> {
            if (caret.getCol() > 0) {
                caretOffsetAdd(0, -1);
            }
            return;
        }));

        chainedHandler.addEventHandler(new KeyInputEventHandler(Keys.RIGHT_KEYSTROKE, (te) -> {
            if (caret.getCol() < getWidth() && caret.getCol() < getText().length()) {
                // TODO: This will have to be fixed when text scrolling
                // in place
                caretOffsetAdd(0, 1);
                markAsDirty();
            }
            return;
        }));

        chainedHandler.addEventHandler(new KeyInputEventHandler((te) -> {
            KeyEvent ke = (KeyEvent) te;
            if (ke.getKeyStroke().getKeySequenceLength() == 1) {
                return true;
            } else {
                return false;
            }
        }, (te) -> {
            // then probably a simple char - so lets see if its asci
            // TODO: I have discovered an problem ( try CTRL-UP ) it generates
            // multiple keystroke events - which is incorrect - issue with key input parser
            // dont try to handle this bug here - do it in the parser
            // String name = ke.getEventData().getName();
            // char ch = name.charAt(0);
            KeyEvent ke = (KeyEvent) te;
            char ch = ke.getChar();

            // TODO - I know I will need some checks on ch - but this for now
            raiseEventOnChanged(te.getEventBus(), () -> addCharAtCaret(ch));

            return;
        }));

        ConditionalEventHandler complexKeyHandler = chainedHandler.asConditionalEventHandler();
        
        return new KeyInputEventHandler(complexKeyHandler.asPredicate(), complexKeyHandler);

    }

    
    public static  Position thePOSITION;
    
    private void requestShowCursor(EventBus eventBus) {
        // todo - need manage a textinput caret (pos) -- not just append

        // calc where cursor should be relative to text component + its caret position

        Position cursorPos = new Position.Simple(thePOSITION).offsetAdd(caret);

        cursorPos = Component.UTIL.getCanvasOffsetFor(TextInput.this).offsetAdd(caret);
        
        eventBus.addEvent(new RequestShowCursorEvent(TextInput.this, cursorPos));
    }

    private void requestHideCursor(EventBus eventBus) {
        eventBus.addEvent(new RequestHideCursorEvent(TextInput.this));
    }
    
    
    public Focusable asFocusable(Position position) {

        thePOSITION = position;
        
        return new AsFocusable(this) {

            KeyInputEventHandler keyEventHandler = TextInput.this.asEventHandler();

            @Override
            public void handle(TerminalEvent<?> te) {
                keyEventHandler.handle(te);
                requestShowCursor(te.getEventBus()); // todo: This might be expensive to showcursor even if key is ignored
            }

            @Override
            public void setFocus(boolean focus, EventBus eventBus) {
                super.setFocus(focus, eventBus);
                if (focus) {
                    requestShowCursor(eventBus);
                } else {
                    requestHideCursor(eventBus);
                }
                this.hasFocus = focus;
                new FocusSetterToHiliteAction().hiliteAction(this, colorHiliter);
                this.component.setColor(colorHiliter.asColor());
            }


            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                if (keyEventHandler.canHandle(te) == null) {
                    return null;
                }
                return this;
            }
        };

    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(this.textRenderer);
    }

}
