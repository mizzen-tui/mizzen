package dtb.terminalIO.components.input;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Supplier;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestHideCursorEvent;
import dtb.terminalIO.events.RequestShowCursorEvent;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.RequestReleaseFocusEvent;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class TextInput extends BoundedComponent<TextInput>
        implements HasValue<String>, ParentContainer, Focusable, EventHandlerContainer {

    private TextComponent textRenderer;
    private StringBuffer buffer;
    private Position viewOffset = Position.ZERO; // future use .. maybe ..
    private Position caret = Position.ZERO;
    private int maxTextWidth;
    private boolean multiLine = false;  // TODO: multi-line not implemented yet

    ConditionalEventHandler eventHandler;
    ChainedEventHandler chainedEventHandler;

    Focusable focusable;

    private boolean modeOverite = false; // true means insertMode

    public TextInput(Color color, int length) {
        super(color, new Dimension.Simple(1, length));
        init();
    }

    public TextInput(int length) {
        this(ControlCode.DEFAULT_COLOR, length);
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
        this.textRenderer = makeChildComponent(() -> new TextComponent(TextInput.this.getColor(),
                TextInput.this.getRectangle(), TextInput.this.buffer.toString()));
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

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
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

    public void resize(Dimension d) {
        setRectangle(new Rectangle.Simple(this.getPostion_TopLeft(), d));
        markAsDirty();
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

    boolean caretHome() {
        this.caret = Position.ZERO;
        markAsDirty();
        return true;
    }

    boolean caretEnd() {
        int col = Math.min(this.getText().length() + 1, this.getWidth()) - 1;
        this.caret = new Position.Simple(0, col);
        markAsDirty();
        return true;
    }

    void caretOffsetAdd(Position offsetToAdd) {
        // todo: should check valid?
        this.caret = this.caret.offsetAdd(offsetToAdd);
        markAsDirty();
    }

    void caretOffsetAdd(int row, int col) {
        caretOffsetAdd(new Position.Simple(row, col));
    }

    // TODO: needs lots 'o work! Get on it!!
    // returns true if text modified by ch
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

    private void updateCursorPositionAndRefreshCanvas(EventBus eventBus) {
        requestShowCursor(eventBus);
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
    }
    
    void raiseEventOnChanged(EventBus eventBus, Supplier<Boolean> changeTextAction) {
        if (changeTextAction.get()) {
            eventBus.raiseEvent(new HasValue.ValueChangedEvent<>(this));
            updateCursorPositionAndRefreshCanvas(eventBus);
        }
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        getEventHandler().handle(te);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (getEventHandler().canHandle(te) == null) {
            return null;
        }
        return this;
    }

    public TextInput prependEventHandler(ConditionalEventHandler eventHandler) {
        this.getChainedEventHandler().prependEventHandler(eventHandler);
        return this;
    }

    public TextInput appendEventHandler(ConditionalEventHandler eventHandler) {
        this.getChainedEventHandler().appendEventHandler(eventHandler);
        return this;
    }

    public TextInput removeEventHandler(ConditionalEventHandler eventHandler) {
        this.getChainedEventHandler().removeEventHandler(eventHandler);
        return this;
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        asFocusable().setFocus(focus, eventBus);
    }

    @Override
    public boolean hasFocus() {
        return asFocusable().hasFocus();
    }

    private ConditionalEventHandler getEventHandler() {
        if (this.eventHandler == null) {
            this.eventHandler = buildEventHandler();
        }
        return this.eventHandler;
    }

    private ChainedEventHandler getChainedEventHandler() {
        if (this.chainedEventHandler == null) {
            getEventHandler();
        }
        return this.chainedEventHandler;
    }

    private ConditionalEventHandler buildEventHandler() {

        this.chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();

        appendEventHandler(new KeyInputEventHandler(Keys.BACKSPACE_KEYSTROKE,
                (te) -> raiseEventOnChanged(te.getEventBus(), () -> deleteCharBeforeCaret())));

        appendEventHandler(new KeyInputEventHandler(Keys.DELETE_KEYSTROKE,
                (te) -> raiseEventOnChanged(te.getEventBus(), () -> deleteCharAtCaret())));

        appendEventHandler(new KeyInputEventHandler(Keys.LEFT_KEYSTROKE, (te) -> caret.getCol() > 0, (te) -> {
            if (caret.getCol() > 0) {
                caretOffsetAdd(0, -1);
                markAsDirty();
                updateCursorPositionAndRefreshCanvas(te.getEventBus());
            }
            return;
        }));

        appendEventHandler(new KeyInputEventHandler(Keys.RIGHT_KEYSTROKE,
                (te) -> caret.getCol() < getWidth() && caret.getCol() < getText().length(), (te) -> {
                    if (caret.getCol() < getWidth() && caret.getCol() < getText().length()) {
                        // TODO: This will have to be fixed when text scrolling
                        // in place
                        caretOffsetAdd(0, 1);
                        markAsDirty();
                        updateCursorPositionAndRefreshCanvas(te.getEventBus());
                    }
                    return;
                }));

        appendEventHandler(new KeyInputEventHandler(Keys.ENTER_KEYSTROKE, (te) -> {
            // for now ... do nothing - but maybe later go to next focus? (new concept)
            TextInput.this.raiseEventOnChanged(te.getEventBus(), () -> true);
            return;
        }));

        appendEventHandler(new KeyInputEventHandler(Keys.HOME_KEYSTROKE,
                (te) -> raiseEventOnChanged(te.getEventBus(), () -> caretHome())));

        appendEventHandler(new KeyInputEventHandler(Keys.END_KEYSTROKE,
                (te) -> raiseEventOnChanged(te.getEventBus(), () -> caretEnd())));

        appendEventHandler(new KeyInputEventHandler((te) -> {
            KeyEvent ke = (KeyEvent) te;
            if (ke.getKeyStroke().getKeySequenceLength() == 1) {
                if (ke.isKeyStroke(Keys.ESC_KEYSTROKE)) {
                    return false; // TODO: Is this correct? decide later (data drive this maybe?)
                }
                if (ke.getKeyStroke().isCtrlKey() || ke.getKeyStroke().isAltKey()) {
                    return false;
                }
                if (ke.isKeyStroke(Keys.TAB_KEYSTROKE) && ! multiLine) {
                    return false;
                }
                if (ke.isKeyStroke(Keys.SHIFT_TAB_KEYSTROKE) && ! multiLine) {
                    return false;
                }
                return true;
            } else {
                return false;
            }
        }, (te) -> {
            // then probably a simple char - so lets see if its ascii
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

        ConditionalEventHandler complexKeyHandler = chainedEventHandler.asConditionalEventHandler();

        return complexKeyHandler;

    }

    private void requestShowCursor(EventBus eventBus) {
        Component.UTIL.requestShowCursorAtComponent(this, caret, eventBus);
    }

    private void requestHideCursor(EventBus eventBus) {
        eventBus.raiseEvent(new RequestHideCursorEvent(TextInput.this));
    }

    private Focusable asFocusable() {
        if (focusable == null) {
            this.focusable = new DefaultFocusableBehaviour.Simple(this, true) {
                @Override
                public void setFocus(boolean focus, EventBus eventBus) {
                    super.setFocus(focus, eventBus);
                    if (focus) {
                        new ShowComponentOnFocus().onFocus(TextInput.this, eventBus);
                        requestShowCursor(eventBus);
                    } else {
                        requestHideCursor(eventBus);
                    }
                }
            };
        }
        return focusable;
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(this.textRenderer);
    }

    public void requestFocus(TerminalEvent<?> te) {
        te.getEventBus().raiseEvent(new RequestFocusEvent(this));
    }

    public void requestReleaseFocus(TerminalEvent<?> te) {
        te.getEventBus().raiseEvent(new RequestReleaseFocusEvent(this));
    }

    public void requestFocus(EventBus eventBus) {
        eventBus.raiseEvent(new RequestFocusEvent(this));
    }

    @Override
    public String toString() {
        return "TextInput [buffer=" + buffer.toString() + "]" + "[row=" + getRow() + ", lcol" + getCol() + ", rcol="
                + getRightMostCol() + " width=" + getWidth() + ", height=" + getHeight() + "]";
    }

}
