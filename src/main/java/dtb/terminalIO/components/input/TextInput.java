package dtb.terminalIO.components.input;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestHideCursorEvent;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.RequestReleaseFocusEvent;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.StringUtils;

public class TextInput extends BoundedComponent<TextInput>
                       implements SettableHasValue<String>, 
                                  ParentContainer, 
                                  Focusable  {

    private TextComponent textRenderer;
    private StringBuilder buffer;
    private Position viewOffset = Position.ZERO; // future use .. maybe ..
    private Position caret = Position.ZERO;
    private int maxTextWidth;
    private boolean multiLine = false;  // TODO: multi-line not implemented yet
    private boolean visible = true;

    ConditionalEventHandler eventHandler;
    ChainedEventHandler chainedEventHandler;

    Focusable focusable;
    
    
    private static final BiFunction<TextInput, String, String> TEXT_FORMATTER_BASIC = (t,s)->s;

    BiFunction<TextInput, String, String>  textDisplayFormatter = TEXT_FORMATTER_BASIC;

    private boolean modeOverite = false; // true means insertMode   (not used?)
    private Object align;

    public TextInput(Color color, int length) {
        super(color, new Dimension.Simple(1, length));
        init();
        setLength(length); // necessary
        setText("");
    }

    public TextInput(int length) {
        this(Colors.DEFAULT_COLOR, length);
    }

    public TextInput(Color color, Dimension d) {
        super(color, d);
        init();
        setText("");
    }

    public TextInput(Color color, Position p, Dimension d) {
        super(color, p, d);
        init();
        setText("");
    }
    
    public TextInput align(Align align) {
        this.align = align;
        this.textDisplayFormatter = (t,s) -> Align.UTIL.formatToAlignedData(s, t.getWidth(), align, t.getWidth(), true);
        this.setText(getText());
        return this;
    }

    public TextInput setAsPassword() {
        this.align = Align.LEFT;
        this.textDisplayFormatter = (t,s) -> Align.UTIL.formatToAlignedData(StringUtils.repeat("*", s.length()), t.getWidth(), Align.LEFT, t.getWidth(), true);
        this.setText(getText());
        return this;
    }

    public void markAsDirty() {
        if (! isDirty()) {
            super.markAsDirty();
            this.textRenderer.markAsDirty();
        }
    }

    public boolean hide() {
        boolean isVisible = super.hide();
        this.visible = isVisible;
        return isVisible;
    }
    
    public boolean show() {
        boolean isVisible = super.show();
        this.visible = isVisible;
        return isVisible;
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
        this.buffer = new StringBuilder();
        this.caret = new Position.Simple(0, 0);
        this.setFillOnClear(true);
        this.maxTextWidth = this.getWidth();
        this.textRenderer = makeChildComponent(() -> new TextComponent(TextInput.this.getColor(),
                                                                       TextInput.this.getRectangle(), 
                                                                       TextInput.this.buffer.toString()));
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer mySegments) {
        mySegments.clearAndMerge(this.textRenderer.getSegmentsBuffer().getSegments());
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
        this.setTextOnControl(text);
        this.caret = new Position.Simple(caret.getRow(), this.getText().length());
        this.maxTextWidth = Math.max(this.getWidth(), this.getText().length());
    }

    protected boolean validateText(String newText) {
        return true;
    }

    protected void setTextOnControl(String text) {
        if (validateText(text)) {
            this.buffer = new StringBuilder(text);
            String textForDisplay = textDisplayFormatter.apply(this,this.buffer.toString());
            this.textRenderer.setText(textForDisplay);
            this.markAsDirty();
        }
    }

    public String getText() {
        return this.buffer.toString();
    }

    @Override
    public String getValue() {
        return this.getText();
    }
    
    @Override
    public void setValue(String value) {
        this.setText(value);
    }


    public void setLength(int length) {
        resize(new Dimension.Simple(getHeight(), length));
    }
    
    public void resize(Dimension d) {
        if (d != null && d.isSameDimension(this)) {
            return;
        }
        setRectangle(new Rectangle.Simple(this.getPostion_TopLeft(), d));
        this.textRenderer.resize(d);
        this.maxTextWidth = Math.max(this.getWidth(), this.getText().length());
        if (this.getParentContainer()!=null ) {
            this.getParentContainer().childResized(this);
        }
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
            this.buffer = new StringBuilder().append(str.substring(0, maxWidth));
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
            if ( ! validateText(currentText)) {
                return false;
            }
            
            caretOffsetAdd(0, 1);
            this.setTextOnControl(currentText);
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
            if ( ! validateText(currentText)) {
                return false;
            }
            if (caret.getCol() > 0) {
                caretOffsetAdd(0, -1);
            }
            this.setTextOnControl(currentText);
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
                if ( ! validateText(currentText)) {
                    return false;
                }
                this.setTextOnControl(currentText);
                return true;
            }
        }
        return false;
    }

    private void updateCursorPositionAndRefreshCanvas(EventBus eventBus) {
        requestShowCursor(eventBus);
        this.markAsDirty();
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
        if (!this.hasFocus()) {
            // Compensating for a bug in defaultFocusManager/TUIApp 
            requestFocus(te);
        }
        getEventHandler().handle(te);
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (getEventHandler().canHandle(te) == null) {
            return null;
        }
        return this;
    }

    private TextInput appendEventHandler(ConditionalEventHandler eventHandler) {
        this.getChainedEventHandler().appendEventHandler(eventHandler);
        return this;
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        asFocusable().setFocus(focus, eventBus);
    }

    @Override
    public void unFocus(Focusable newFocusable, EventBus eventBus) {
        asFocusable().unFocus(newFocusable, eventBus);
    }
    
    @Override
    public void focus(Focusable oldFocusable, EventBus eventBus) {
        asFocusable().focus(oldFocusable, eventBus);
    }


    @Override
    public FocusManager getParentFocusManager() {
        return asFocusable().getParentFocusManager();
    }

    @Override
    public void setParentFocusManager(FocusManager focusManager) {
        Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> asFocusable().setParentFocusManager(fm));
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

        this.chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);

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
            // then probably a simple char - so lets see if its ASCII
            // TODO: I have discovered a problem ( try CTRL-UP ) it generates
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

        ConditionalEventHandler handleEventsWhenVisible = new ConditionalEventHandler.AbstractDelegate() {
            
            @Override
            protected ConditionalEventHandler getDelegate() {
                return complexKeyHandler;
            }
            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                if (!visible) {
                    return null;
                }
                return super.canHandle(te);
            }   
        };
        
        return handleEventsWhenVisible;

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
                        updateCursorPositionAndRefreshCanvas(eventBus);
                        new ShowComponentOnFocus().onFocus(TextInput.this, eventBus);
                    } else {
                        requestHideCursor(eventBus);
                    }
                }
                @Override
                public void unFocus(Focusable newFocusable, EventBus eventBus) {
                    super.unFocus(newFocusable, eventBus);
                    requestHideCursor(eventBus);
                }
                @Override
                public void focus(Focusable oldFocusable, EventBus eventBus) {
                    super.focus(oldFocusable, eventBus);
                    updateCursorPositionAndRefreshCanvas(eventBus);
                    new ShowComponentOnFocus().onFocus(TextInput.this, eventBus);
                }
            };
        }
        return focusable;
    }

    HasValue.OnValueChangedEventHandler<String> onChangeHandler;

    public void onChange(EventHandler consumeChange) {

        onChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, onChangeHandler, getChainedEventHandler(), consumeChange);

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
        String identifier = buffer.toString();
        if (buffer.length()<1) {
            identifier = "name="+HasName.UTIL.getName(this);
        } else {
            identifier = "buffer="+buffer.toString();
        }
        return "TextInput [" + identifier + "]" + 
                           "[row=" + getRow() + ", lcol" + getCol() + ", rcol="
                + getRightMostCol() + " width=" + getWidth() + ", height=" + getHeight() + "]";
    }


}
