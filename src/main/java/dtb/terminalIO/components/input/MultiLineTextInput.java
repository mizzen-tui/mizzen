package dtb.terminalIO.components.input;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import dtb.terminalIO.components.AbstractComponent;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.scroll.HasViewPortWindow;
import dtb.terminalIO.components.scroll.ViewPortNavigable;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.events.AbstractTimedRequestedController;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnIdleEventEventHandler;
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
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.Clickable;
import dtb.terminalIO.events.mouse.MOUSE_ACTION;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.Logging;
import dtb.terminalIO.util.StringUtils;


public class MultiLineTextInput extends AbstractComponent<MultiLineTextInput>
                                implements HasValue<String>, 
                                           ParentContainer, 
                                           Focusable, 
                                           EventHandlerContainer,
                                           Clickable,
                                           HasViewPortWindow.OnChange {

    private TextComponent textRenderer;
    private boolean textRendererNeedsUpdate = false;
    private MultiLineTextBuffer buffer;
    private Position caret = Position.ZERO;
    private boolean multiLine = true;

    ConditionalEventHandler eventHandler;
    ChainedEventHandler chainedEventHandler;  // test

    Focusable focusable;
    
    // when external component (usually parent) is paging up / down etc we want to control behaviour here
    ViewPortNavigable viewPortNavigator = new DefaultMultiLineTextViewPortNavigableImpl();

    private boolean modeOverite = false; // true means insertMode

    public MultiLineTextInput(Color color) {
        super(color);
        init();
        //, new Dimension.Simple(1, length));
    }

    public MultiLineTextInput() {
        this(Colors.DEFAULT_COLOR);
    }

    @Override
    public MultiLineTextInput setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        if (this.textRenderer != null) {
            this.textRenderer.setColor(drawingcolor);
            this.textRenderer.markAsDirty();
        }
        this.markAsDirty();
        return this;
    }

    public MultiLineTextInput setEmptyColor(Color emptyColor) {
        super.setEmptyColor(emptyColor);
        if (this.textRenderer != null) {
            this.textRenderer.setEmptyColor(emptyColor);
        }
        return this;
    }

    public MultiLineTextInput setEmptyChar(char emptyChar) {
        super.setEmptyChar(emptyChar);
        if (this.textRenderer != null) {
            this.textRenderer.setEmptyChar(emptyChar);
        }
        return this;
    }

    protected void init() {
        this.buffer = new MultiLineTextBuffer();
        this.buffer.add(new StringBuilder());
        this.caret = new Position.Simple(0, 0);
        this.setFillOnClear(true);
        this.textRenderer = new TextComponent(this.getColor(),"");
        this.textRendererNeedsUpdate = false;
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        concatAndSetRendererTextIfNeeded();
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
        signalResized();
    }
    
    protected void signalResized() {
        if (this.getParentContainer()!=null) {
            this.getParentContainer().childResized(this);
        }
    }

    public void setText(String text) {
        if (text == null) {
            text = "";
        }
        this.__setText(text);
        setCaretPosition(new Position.Simple(this.buffer.size() - 1, this.buffer.get(this.buffer.size() - 1).length()));
    }
    
    public void setText(EventBus eventBus, String text) {
        setText(text);
        raiseEventOnChanged(eventBus, ()-> true);
    }

    private void __setText(String text) {
        this.buffer = new MultiLineTextBuffer(Arrays.stream(text.split("\n", -1)).map(line -> new StringBuilder(line))
                                                                                 .collect(Collectors.toList()));
        this.textRendererNeedsUpdate = true;
        signalResized();
        this.markAsDirty();
    }


    public String getValue() {
        concatAndSetRendererTextIfNeeded();
        return this.textRenderer.getText();
    }

    private void concatAndSetRendererTextIfNeeded() {
        if (textRendererNeedsUpdate) {
            this.textRenderer.setTextAndResize(this.buffer.toString());
            this.textRendererNeedsUpdate = false;
        }
    }
    

    public void setCaretPosition(Position newRequestedPosition) {

        int newRow = 0;
        int newCol = 0;

        if (newRequestedPosition.getRow()>0) {
            if (newRequestedPosition.getRow()<this.buffer.size()-1) {
                newRow = newRequestedPosition.getRow();
            } else {
                newRow = this.buffer.size()-1;
            }
        }

        if (newRequestedPosition.getCol()>0) {
            int strLineLen = this.buffer.get(newRow).length();
            if (newRequestedPosition.getCol()<=strLineLen) {
                newCol = newRequestedPosition.getCol();
            } else {
                newCol = strLineLen;
            }
        }

        this.caret = new Position.Simple(newRow, newCol);
        // markAsDirty();
    }

    boolean caretHome() {
        setCaretPosition(new Position.Simple(this.caret.getRow(), 0));
        return true;
    }

    boolean caretEnd() {
        int col = Math.min(this.buffer.get(this.caret.getRow()).length() + 1, this.getWidth()) - 1;
        setCaretPosition(new Position.Simple(this.caret.getRow(), col));
        return true;
    }

    boolean canMoveCaretLeft() {
        if (caret.getCol() > 0 || caret.getRow()>0) {
            return true;
        }
        return false;
    }
    
    boolean caretLeft() {
        if (!canMoveCaretLeft()) {
            return false;
        }
        if (caret.getCol()>0) {
            // simple case 
            caretOffsetAdd(0, -1);
            return true;
        }
        // move up one line and to  the end of line
        caretUp();
        caretEnd();
        return true;
    }

    boolean canMoveCaretRight() {
        if (caret.getCol() < lengthAtCaretLine()) {
            return true;
        }

        if (canMoveCaretDown()) {
            return true;
        }

        return false;
    }

    boolean caretRight() {
        if (! canMoveCaretRight()) {
            return false;
        }
        if (caret.getCol() < lengthAtCaretLine()) {
            caretOffsetAdd(0, 1);
            return true;
        }

        caretDown();
        caretHome();

        return true;
    }

    boolean canMoveCaretPrevWord() {
        return canMoveCaretLeft();
    }

    boolean caretPreviousWord() {
        if ( ! canMoveCaretPrevWord()) {
            return false;
        }
        int newCol = StringUtils.indexOfPrevWord(caret.getCol(), this.buffer.get(caret.getRow()).toString());

        // special case ..
        if (newCol == -1 && caret.getCol()>0) {
            newCol = 0;
        }

        if (newCol != -1) {
            setCaretPosition(new Position.Simple(this.caret.getRow(), newCol));
            return true;
        }

        return caretLeft();
    }

    boolean canMoveCaretNextWord() {
        return canMoveCaretRight();
    }

    boolean caretNextWord() {
        if ( ! canMoveCaretNextWord()) {
            return false;
        }
        int newCol = StringUtils.indexOfNextWord(caret.getCol(), this.buffer.get(caret.getRow()).toString());
        
        // special case ..
        if (newCol == -1 && caret.getCol() < lengthAtCaretLine()-1 ) {
            newCol = lengthAtCaretLine()-1;
        }
        if (newCol != -1) {
            setCaretPosition(new Position.Simple(this.caret.getRow(), newCol));
            return true;
        }

        return caretRight();
    }

    boolean canMoveCaretUp(int iLines) {
        if (this.caret.getRow()-iLines >= 0) {
            return true;
        }
        return false;
    }
    
    
    boolean canMoveCaretUp() {
        return canMoveCaretUp(1);
    }

    boolean caretUp(int iLines) {
        if (!canMoveCaretUp(iLines)) {
            return false;
        }

        int targetRow = this.caret.getRow() - iLines;
        int strLineLen = this.buffer.get(targetRow).length();

        setCaretPosition(new Position.Simple(targetRow, Math.min(this.caret.getCol(),strLineLen)));
        return true;
    }

    boolean caretUp() {
        return caretUp(1);
    }
    
    
    boolean canMoveCaretDown() {
        return canMoveCaretDown(1);
    }

    boolean canMoveCaretDown(int delta) {
        if (this.caret.getRow() + delta >= this.buffer.size()) {
            return false;
        }
        return true;
    }

    boolean caretDown() {
        return caretDown(1);
    }

    boolean caretDown(int delta) {
        if (!canMoveCaretDown(delta)) {
            return false;
        }
        int targetRow = this.caret.getRow() + 1;
        int strLineLen = this.buffer.get(targetRow).length();
        setCaretPosition(new Position.Simple(targetRow, Math.min(this.caret.getCol(),strLineLen)));
        return true;
    }

    private void caretOffsetAdd(Position offsetToAdd) {
        // TODO: should check valid?

        int rowOffset = offsetToAdd.getRow();
        int colOffset = offsetToAdd.getCol();
        
        if (this.caret.getRow()+rowOffset<0) {
            rowOffset = 0;
        } 
        
        if (this.caret.getCol()+colOffset<0) {
            colOffset = 0;
        }
        
        
        setCaretPosition(this.caret.offsetAdd(rowOffset, colOffset));
    }

    private void caretOffsetAdd(int row, int col) {
        caretOffsetAdd(new Position.Simple(row, col));
    }

    // TODO: needs lots 'o work! Get on it!!
    // returns true if text modified by ch
    private boolean addCharAtCaret(char ch) {
        StringBuilder currentTextLine = this.buffer.get(caret.getRow());
        if (modeOverite) {
            return false;
        } else { // insert mode
//            if (currentTextLine.length() >= maxTextWidth) {
//                return false;
//            }
            if (caret.getCol() < currentTextLine.length()) { // []ABCDE
                currentTextLine.insert(caret.getCol(), ch);
            } else if (caret.getCol() >= currentTextLine.length()) { // ABCDE[]
                currentTextLine.append(ch);
            }
            caretOffsetAdd(0, 1);
            this.buffer.adjustDimensionsFromBufferText(currentTextLine.toString());
            this.textRendererNeedsUpdate = true;
            return true;
        }
    }

    private boolean lineBreakAtCaret() {
        if (modeOverite) {
            return false;
        } else { // insert mode
            StringBuilder currentTextLine = this.buffer.get(caret.getRow());
            if (caret.getCol() < currentTextLine.length()) { // []ABCDE
                String left = currentTextLine.substring(0, caret.getCol());
                String nextLine = currentTextLine.substring(caret.getCol());
                this.buffer.set(caret.getRow(), new StringBuilder(left));
                this.buffer.add(caret.getRow() + 1, new StringBuilder(nextLine));
                setCaretPosition(new Position.Simple(caret.getRow() + 1, 0));
            } else if (caret.getCol() >= currentTextLine.length()) { // ABCDE[]
                this.buffer.add(caret.getRow() + 1, new StringBuilder(""));
                setCaretPosition(new Position.Simple(caret.getRow() + 1, 0));
            }
            //this.setMaxTextWidth( Math.max(this.getWidth(), currentTextLine.length()));
            this.textRendererNeedsUpdate = true;
            this.markAsDirty();
            return true;
        }
    }

    private boolean deleteCharBeforeCaret() {
        StringBuilder currentTextLine = this.buffer.get(caret.getRow());
        if (modeOverite) {
            return false;
        } else { // insert mode
            if (caret.getCol() == 0) { // []ABCDE
                if (caret.getRow()>0) {
                    int newCol = this.buffer.get(caret.getRow()-1).length();
                    this.buffer.get(caret.getRow()-1).append(this.buffer.get(caret.getRow()));
                    this.buffer.remove(caret.getRow());
                    caretOffsetAdd(-1, newCol);
                    this.textRendererNeedsUpdate = true;
                    return true;
                }
                return false;
            } else if (caret.getCol() < currentTextLine.length()) { // ABC[]DE
                currentTextLine.deleteCharAt(caret.getCol()-1);
            } else if (caret.getCol() >= currentTextLine.length()) { // ABCDE[]
                currentTextLine.deleteCharAt(currentTextLine.length() - 1);
            }
            if (caret.getCol() > 0) {
                caretOffsetAdd(0, -1);
            } else {
                caretOffsetAdd(-1, 0);
            }
            this.buffer.adjustDimensionsFromBufferText(currentTextLine.toString());
            this.textRendererNeedsUpdate = true;
            return true;
        }
    }

    private int lengthAtCaretLine() {
        return this.buffer.get(caret.getRow()).length();
    }

    private boolean deleteCharAtCaret() {
        StringBuilder currentTextLine = this.buffer.get(caret.getRow());
        if (modeOverite) {
            return false;
        } else { // insert mode
            int textlen = currentTextLine.length();
            if (caret.getCol() < textlen) {
                currentTextLine.deleteCharAt(caret.getCol());
                this.buffer.adjustDimensionsFromBufferText(currentTextLine.toString());
                this.textRendererNeedsUpdate = true;
                markAsDirty();
                return true;
            } else {
                if (caret.getRow()+1 < this.buffer.size()) {
                    currentTextLine.append(this.buffer.get(caret.getRow()+1));
                    this.buffer.remove(caret.getRow()+1);
                    this.textRendererNeedsUpdate = true;
                    markAsDirty();
                    return true;
                }
            }
        }
        return false;
    }

    private void updateCursorPositionAndRefreshCanvas(EventBus eventBus) {
        requestShowCursor(eventBus);
        //eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
    }

    AbstractTimedRequestedController timedOnChangeRequestController = null;
    
    AbstractTimedRequestedController buildTimedOnChangeController(EventBus eventBus) {
        return new AbstractTimedRequestedController(()->{
            eventBus.raiseEvent(new HasValue.ValueChangedEvent<>(MultiLineTextInput.this));
            updateCursorPositionAndRefreshCanvas(eventBus);
        }) {
            @Override
            protected boolean isRequestDifferentFromPrevious() {
                return true;
            }
            
        };
    }

    void raiseEventOnChanged(EventBus eventBus, Supplier<Boolean> changeTextAction) {
        if (timedOnChangeRequestController == null) {
            timedOnChangeRequestController = buildTimedOnChangeController(eventBus);
            timedOnChangeRequestController.setTimerDelayMillis(50);
        }
        if (changeTextAction.get()) {
            // signal that text or cursor has changed
            // and a visual update is needed
            timedOnChangeRequestController.onRequest(eventBus);
        }
    }

    public void onViewPortWindowChange(HasViewPortWindow source, TerminalEvent<?> te) {
        Rectangle viewPort = source.getViewPortAsRectangle();
        if (viewPort.containsPosition(this.caret)) {
            Logging.info(HasName.UTIL.getName(this)+"  onViewPortWindowChange -> "+viewPort);
            if (te != null) {
                raiseEventOnChanged(te.getEventBus(), ()-> true);
            }
            return;
        }
        
        Rectangle viewIntoThis = viewPort.intersectWith(this);
        
        if ( viewIntoThis == null) {
            // This component is not visible in viewport so nothing to do
            return;
        }

        //  caret needs to be moved - try to find a new sensible position based on its current
        //  location and viewport size and location

        Position newCaretPosition = new Position.Simple(this.caret);
        
        if (newCaretPosition.getRow() < viewIntoThis.getRow()) {
            newCaretPosition = newCaretPosition.offsetAdd(viewIntoThis.getRow()-newCaretPosition.getRow(), 0);
        } else if (this.caret.getRow() > viewIntoThis.getRow()) {
            newCaretPosition = newCaretPosition.offsetAdd(newCaretPosition.getRow()-viewIntoThis.getRow(), 0);
        }
        
        if (newCaretPosition.getCol() < viewIntoThis.getCol()) {
            newCaretPosition = newCaretPosition.offsetAdd(0, viewIntoThis.getCol() -  newCaretPosition.getCol());
        } else if (newCaretPosition.getCol() > viewIntoThis.getCol()) {
            newCaretPosition = newCaretPosition.offsetAdd(0, newCaretPosition.getCol() - viewIntoThis.getCol());
        }

        if (newCaretPosition.isSameLocation(this.caret)) {
            // TODO: IllegalState - should not be possible 
            Logging.info(HasName.UTIL.getName(this)+": Illegal CURSOR state: Caret was not moved on viewport change: "+this.caret+ " viewport" + viewIntoThis);
            // maybe throw exception?
            return;
        }

        setCaretPosition(newCaretPosition);

        if (te != null) {
            raiseEventOnChanged(te.getEventBus(), ()-> true);
        }
    }
    
    class DefaultMultiLineTextViewPortNavigableImpl implements ViewPortNavigable {

        public void up(int iLines) {
            caretUp(iLines);
        }

        public void down(int iLines) {
            caretDown(iLines);
        }

        public void left(int iCols) {
            // TODO: thhis is not correct 
            caretLeft();
        }

        public void right(int iCols) {
            // TODO: thhis is not correct 
            caretRight();
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

    public MultiLineTextInput prependEventHandler(ConditionalEventHandler eventHandler) {
        this.getChainedEventHandler().prependEventHandler(eventHandler);
        return this;
    }

    public MultiLineTextInput appendEventHandler(ConditionalEventHandler eventHandler) {
        this.getChainedEventHandler().appendEventHandler(eventHandler);
        return this;
    }

    public MultiLineTextInput removeEventHandler(ConditionalEventHandler eventHandler) {
        this.getChainedEventHandler().removeEventHandler(eventHandler);
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
    public boolean hasFocus() {
        return asFocusable().hasFocus();
    }
    

    @Override
    public FocusManager getParentFocusManager() {
        return asFocusable().getParentFocusManager();
    }

    @Override
    public void setParentFocusManager(FocusManager focusManager) {
        Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> asFocusable().setParentFocusManager(fm));
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

        /*
         * KeyInputEventHandler(KeyStroke onKeyStroke, Predicate<TerminalEvent<?>>
         * canHandlePredicate, EventHandler handler)
         */

        appendEventHandler(new KeyInputEventHandler(Keys.UP_KEYSTROKE, (te) -> canMoveCaretUp(),
                (te) -> raiseEventOnChanged(te.getEventBus(), () -> {
                    boolean result = caretUp();
                    return result;
                })));

        appendEventHandler(new KeyInputEventHandler(Keys.DOWN_KEYSTROKE, 
                (te) -> true,
                (te) -> raiseEventOnChanged(te.getEventBus(), () -> caretDown())));

        appendEventHandler(new KeyInputEventHandler(Keys.BACKSPACE_KEYSTROKE,
                (te) -> raiseEventOnChanged(te.getEventBus(), () -> deleteCharBeforeCaret())));

        appendEventHandler(new KeyInputEventHandler(Keys.DELETE_KEYSTROKE,
                (te) -> raiseEventOnChanged(te.getEventBus(), () -> deleteCharAtCaret())));

        appendEventHandler(new KeyInputEventHandler(Keys.LEFT_KEYSTROKE, 
            (te) -> canMoveCaretLeft(), 
            (te) -> raiseEventOnChanged(te.getEventBus(), ()-> caretLeft() )));

        appendEventHandler(new KeyInputEventHandler(Keys.RIGHT_KEYSTROKE,
                (te) -> canMoveCaretRight(), 
                (te) -> raiseEventOnChanged(te.getEventBus(), ()-> caretRight() )));

        appendEventHandler(new KeyInputEventHandler(Keys.CTRL_LEFT_KEYSTROKE,
                (te) -> canMoveCaretPrevWord(), 
                (te) -> raiseEventOnChanged(te.getEventBus(), ()-> caretPreviousWord() )));

        appendEventHandler(new KeyInputEventHandler(Keys.CTRL_RIGHT_KEYSTROKE,
                (te) -> canMoveCaretNextWord(), 
                (te) -> raiseEventOnChanged(te.getEventBus(), ()-> caretNextWord() )));

        appendEventHandler(new KeyInputEventHandler(Keys.ENTER_KEYSTROKE, (te) -> {
            MultiLineTextInput.this.raiseEventOnChanged(te.getEventBus(), () -> lineBreakAtCaret());
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
                if (ke.isKeyStroke(Keys.TAB_KEYSTROKE) && !multiLine) {
                    return false;
                }
                if (ke.isKeyStroke(Keys.SHIFT_TAB_KEYSTROKE) && !multiLine) {
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
        }));

        ConditionalEventHandler complexKeyHandler = chainedEventHandler.asConditionalEventHandler();

        return complexKeyHandler;

    }

    protected void requestShowCursor(EventBus eventBus) {

        // ensure desired cursor location will be in view
        new ShowComponentOnFocus().onFocus(MultiLineTextInput.this, 
                                           new Rectangle.Simple(caret, new Dimension.Simple(1,1)),
                                           eventBus);
        // now move cursor to caret and then request a canvas update
        eventBus.appendEventHandler( new OnIdleEventEventHandler.OneTime(()-> {
                                            Component.UTIL.requestShowCursorAtComponent(MultiLineTextInput.this, caret, eventBus);
                                            MultiLineTextInput.this.markAsDirty();
                                            eventBus.raiseEvent(new RequestUpdateCanvasEvent(MultiLineTextInput.this));
                                          }
                                   ));
    }

    private void requestHideCursor(EventBus eventBus) {
        eventBus.raiseEvent(new RequestHideCursorEvent(MultiLineTextInput.this));
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(MultiLineTextInput.this));
    }

    private Focusable asFocusable() {
        if (focusable == null) {
            this.focusable = new DefaultFocusableBehaviour.Simple(this, true) {
                @Override
                public void setFocus(boolean focus, EventBus eventBus) {
                    super.setFocus(focus, eventBus);
                    if (focus) {
                        //new ShowComponentOnFocus().onFocus(MultiLineTextInput.this, eventBus);
                        requestShowCursor(eventBus);
                    } else {
                        requestHideCursor(eventBus);
                    }
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
        return "MultiLineTextInput ]";
    }

    @Override
    public void onClick(ApplicationMouseEvent appMouseEvent) {
        if ( MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction()) || MOUSE_ACTION.DOWN_LEFT_DRAG.equals(appMouseEvent.getMouseAction()) ) {
            setCaretPosition(appMouseEvent.getMousePosition());
            raiseEventOnChanged(appMouseEvent.getEventBus(), ()-> true);
        }
    }

    @Override
    public void onClick(EventBus eventBus) {
        if (!hasFocus()) {
            // focus manager should have already put focus here - but just in case ..
            eventBus.raiseEvent(new RequestFocusEvent(this));
        }
    }

    
    @Override
    public int getCol() {
        return this.textRenderer.getCol();
    }

    @Override
    public int getRow() {
        return this.textRenderer.getRow();
    }

    @Override
    public int getWidth() {
        return this.buffer.getWidth();
    }

    @Override
    public int getHeight() {
        return this.buffer.getHeight();
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return this.textRenderer;
    }

    @Override
    public void moveTo(int row, int col) {
        this.textRenderer.moveTo(row, col);
    }
    
    
    class MultiLineTextBuffer implements Dimension {
        
        private int maxTextWidth = 0;
        private List<StringBuilder> bufferText;
        

        MultiLineTextBuffer(List<StringBuilder> bufferText) {
            this.setBufferText(bufferText);
        }
        
        MultiLineTextBuffer() {
            this(new ArrayList<StringBuilder>());
        }
        

        

        public int size() {
            return getHeight();
        }


        public void set(int row, StringBuilder stringBuffer) {
            this.bufferText.set(row, stringBuffer);
            adjustDimensionsFromBufferText(stringBuffer.toString());
        }


        public void add(int i, StringBuilder stringBuffer) {
            this.bufferText.add(i, stringBuffer);
            adjustDimensionsFromBufferText(true);
        }


        public void remove(int row) {
            this.bufferText.remove(row);
            adjustDimensionsFromBufferText(true);
        }


        public StringBuilder get(int i) {
            return this.bufferText.get(i);
        }


        void setBufferText(List<StringBuilder> bufferText) {
            this.bufferText =  bufferText;
            adjustDimensionsFromBufferText(true);
        }

        private void adjustDimensionsFromBufferText(boolean heightChanged) {
            if (this.bufferText.size()==0) {
                return;
            }
            int textMaxWidth = this.bufferText.stream().max(Comparator.comparing(StringBuilder::length)).get().length();
            boolean widthChanged = this.setMaxTextWidth( Math.max(this.getWidth(), textMaxWidth) );
            if (heightChanged || widthChanged) {
                signalResized();
            }
        }

        public void adjustDimensionsFromBufferText(String textLine) {
            boolean widthChanged = this.setMaxTextWidth( Math.max(this.getWidth(), textLine.length()) );
            if (widthChanged) {
                signalResized();
            }
        }
        
        
        public void adjustDimensions() {
            adjustDimensionsFromBufferText(false);
        }


        private boolean setMaxTextWidth(int maxTextWidth) {
            boolean changed = this.maxTextWidth != maxTextWidth;
            this.maxTextWidth = maxTextWidth;
            return changed;
        }


        public void add(StringBuilder stringBuffer) {
            this.bufferText.add(stringBuffer);
            adjustDimensionsFromBufferText(true);
        }

        @Override
        public int getWidth() {
            return this.maxTextWidth;
        }


        @Override
        public int getHeight() {
            return this.bufferText.size();
        }
        
        public String toString() {
            StringJoiner sj = new StringJoiner("\n");
            for (StringBuilder sb : this.bufferText) {
                sj.add(sb);
            }
            return sj.toString();
        }
        
    }


}
