package dtb.terminalIO.components.scroll;

import dtb.terminalIO.components.BlockChars;
import dtb.terminalIO.components.BlockChars.BlockHiliter;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.VerticalLine;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.events.mouse.ApplicationMouseEvent;
import dtb.terminalIO.events.mouse.Clickable;
import dtb.terminalIO.events.mouse.MOUSE_ACTION;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

/**
 * A ScrollBar in the Vertical orientation.
 */
public class ScrollBarVertical extends BoundedComponent<ScrollBarVertical> implements Hiliter, Clickable, Focusable,
        HasValue<Position> {

    private boolean hilited = false;
    Position offset;
    Dimension sizeOfComponentBeingViewed;
    Dimension sizeOfViewer;
    private BlockHiliter blockHiliter = new BlockChars.BlockHiliter();
    private boolean handleKeyEvents;

    // ▒
    // [░░▒] [▒░░]
    public ScrollBarVertical(Color color, Position pos, int len) {
        super(color, pos, new Dimension.Simple(len, 1));
        handleKeyEvents = true;
        this.setFillOnClear(false);
    }

    public ScrollBarVertical(Color color, int row, int col, int len) {
        super(color, new Position.Simple(row, col), new Dimension.Simple(len, 1));
        handleKeyEvents = true;
        this.setFillOnClear(false);
    }

    public ScrollBarVertical enableKeyEvents(boolean enable) {
        handleKeyEvents = enable;
        return this;
    }

    Position calcScrollbarOffset() {
        if (sizeOfComponentBeingViewed == null || offset == null) {
            return null;
        }
        float percentage = (float) sizeOfViewer.getHeight() / (float) sizeOfComponentBeingViewed.getHeight();
        if (percentage >= 1) {
            return null;
        }

        return new Position.Simple((int) (((float) offset.getRow()) * percentage),
                (int) (((float) offset.getCol()) * percentage));
    }

    Position calcOffset(Position scrollbarOffset) {
        if (sizeOfComponentBeingViewed == null || sizeOfViewer == null || scrollbarOffset == null) {
            return Position.ZERO;
        }

        int maxOffset = sizeOfComponentBeingViewed.getHeight() - sizeOfViewer.getHeight();
        if (maxOffset <= 0) {
            return Position.ZERO;
        }

        float percentage = (float) sizeOfComponentBeingViewed.getHeight() / (float) sizeOfViewer.getHeight();

        int row = (int) (((float) scrollbarOffset.getRow()) * percentage);

        if (row < 0) {
            row = 0;
        }
        if (row > maxOffset) {
            row = maxOffset;
        }

        return new Position.Simple(row, 0);
    }

    boolean canScrollUp(TerminalEvent<?> te) {
        if (sizeOfComponentBeingViewed == null || offset == null) {
            return false;
        }

        if (offset.getRow() == 0) {
            return false;
        }

        return true;
    }

    void scrollUp(TerminalEvent<?> te) {
        if (!canScrollUp(te)) {
            return;
        }
        setOffset(offset.offsetAdd(-1, 0));
        te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<>(this));
    }

    boolean canScrollDown(TerminalEvent<?> te) {
        if (sizeOfComponentBeingViewed == null || sizeOfViewer == null || offset == null) {
            return false;
        }

        int maxOffset = sizeOfComponentBeingViewed.getHeight() - sizeOfViewer.getHeight();

        if (maxOffset <= 0) {
            return false;
        }

        return offset.getRow() < maxOffset;
    }

    void scrollDown(TerminalEvent<?> te) {
        if (!canScrollDown(te)) {
            return;
        }
        setOffset(offset.offsetAdd(1, 0));
        te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<>(this));
    }

    void pageUp(TerminalEvent<?> te) {
        if (!canScrollUp(te)) {
            return;
        }
        int pageHeight = sizeOfViewer.getHeight();
        int delta = pageHeight;
        if ((this.offset.getRow()) < delta) {
            delta = this.offset.getRow();
        }
        setOffset(offset.offsetAdd(-delta, 0));
        te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<>(this));
    }

    void pagedown(TerminalEvent<?> te) {
        if (!canScrollDown(te)) {
            return;
        }

        int maxOffset = sizeOfComponentBeingViewed.getHeight() - sizeOfViewer.getHeight();
        int pageHeight = sizeOfViewer.getHeight();

        int newRow = offset.getRow() + pageHeight;
        if (newRow > maxOffset) {
            newRow = maxOffset;
        }

        setOffset(new Position.Simple(newRow, 0));
        te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<>(this));
    }

    public void setOffset(Position offset) {
        this.offset = offset;
        this.markAsDirty();
    }

    public void setViewedSize(Dimension sizeOfComponentBeingViewed, Dimension sizeOfViewer) {
        this.sizeOfComponentBeingViewed = sizeOfComponentBeingViewed;
        this.sizeOfViewer = sizeOfViewer;
        this.markAsDirty();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {

        Position scrollBarOffset = calcScrollbarOffset();

        if (scrollBarOffset == null) {
            segments.merge(
                    makeChildComponent(() -> new VerticalLine(color, this.getRow(), this.getCol(), this.getHeight()))
                            .getSegmentsBuffer());
            return;
        }
        // |
        // | [A]
        // X
        // X [B]
        // X
        // | [C]
        // |

        float percentage = (float) sizeOfViewer.getHeight() / (float) sizeOfComponentBeingViewed.getHeight();

        int a_height = scrollBarOffset.getRow();
        int b_height = Math.max((int) (percentage * (float) this.getHeight()), 1);
        int c_height = this.getHeight() - (a_height + b_height);

        if (b_height >= this.getHeight()) {
            segments.merge(
                    makeChildComponent(() -> new VerticalLine(color, this.getRow(), this.getCol(), this.getHeight()))
                            .getSegmentsBuffer());
            return;
        }

        if (a_height > 0) {
            segments.merge(makeChildComponent(() -> new VerticalLine(color, this.getRow(), this.getCol(), a_height))
                    .getSegmentsBuffer());
        }

        if (b_height > 0) {
            segments.merge(
                    makeChildComponent(() -> new VerticalLine(color, this.getRow() + a_height, this.getCol(), b_height)
                            .setVerticalLineChar('▒')).getSegmentsBuffer());
        }

        if (c_height > 0) {
            segments.merge(makeChildComponent(
                    () -> new VerticalLine(color, this.getRow() + a_height + b_height, this.getCol(), c_height))
                    .getSegmentsBuffer());
        }
    }

    public BlockChars getBlockChars() {
        return blockHiliter.getBlockChars();
    }

    @Override
    public void hilite() {
        if (!this.hilited) {
            markAsDirty();
        }
        // colorHiliter.hilite();
        blockHiliter.hilite();
        this.hilited = true;
    }

    @Override
    public void unHilite() {
        if (this.hilited) {
            markAsDirty();
        }
        // colorHiliter.unHilite();
        blockHiliter.unHilite();
        this.hilited = false;
    }

    @Override
    public void onClick(EventBus eventBus) {
        // do nothing
    }

    @Override
    public void onClick(ApplicationMouseEvent appMouseEvent) {

        if (!MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction())
                && !MOUSE_ACTION.DOWN_LEFT_DRAG.equals(appMouseEvent.getMouseAction())) {
            return;
        }

        // Logging.setEnabled(true);

        // Logging.info("clicked on " + this.getClass().getSimpleName() );
        // Logging.info(" " + appMouseEvent.getMousePosition() );

        setOffset(calcOffset(appMouseEvent.getMousePosition()));

        // Logging.info(" offset " + this.offset );

        appMouseEvent.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<>(this));

        // Logging.setEnabled(false);

    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        return asFocusable().canHandle(te);
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        asFocusable().handle(te);
    }

    @Override
    public boolean hasFocus() {
        return asFocusable().hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        asFocusable().setFocus(focus, eventBus);
    }

    @Override
    public FocusManager getParentFocusManager() {
        return asFocusable().getParentFocusManager();
    }

    @Override
    public void setParentFocusManager(FocusManager focusManager) {
        Focusable.UTIL.setParentFocusManager(this, focusManager, (fm) -> asFocusable().setParentFocusManager(fm));
    }

    Focusable meAsFocusable;

    private boolean tabStop = true;

    /** false: TAB / SHIFT-TAB skip this scrollbar (it can still be clicked / dragged), e.g. inside a scrollable pane */
    public ScrollBarVertical setTabStop(boolean tabStop) {
        this.tabStop = tabStop;
        return this;
    }

    @Override
    public boolean isTabStop() {
        return tabStop;
    }

    ChainedEventHandler eventHandlerContainer;
    ConditionalEventHandler conditionalHandler;

    Focusable asFocusable() {
        if (meAsFocusable != null) {
            return meAsFocusable;
        }

        this.eventHandlerContainer = ChainedEventHandler
                .useChainOfResponsibilityStrategy(HasName.UTIL.getName(this) + "-chainOfResponsibility");

        eventHandlerContainer.appendEventHandler(new KeyInputEventHandler(Keys.UP_KEYSTROKE,
                (te) -> handleKeyEvents && canScrollUp(te),
                (te) -> {
                    scrollUp(te);
                }));

        eventHandlerContainer.appendEventHandler(new KeyInputEventHandler(Keys.DOWN_KEYSTROKE,
                (te) -> handleKeyEvents && canScrollDown(te),
                (te) -> {
                    scrollDown(te);
                }));

        eventHandlerContainer.appendEventHandler(new KeyInputEventHandler(Keys.PGUP_KEYSTROKE,
                (te) -> handleKeyEvents && canScrollUp(te),
                (te) -> {
                    pageUp(te);
                }));

        eventHandlerContainer.appendEventHandler(new KeyInputEventHandler(Keys.PGDN_KEYSTROKE,
                (te) -> handleKeyEvents && canScrollDown(te),
                (te) -> {
                    pagedown(te);
                }));

        this.conditionalHandler = this.eventHandlerContainer.asConditionalEventHandler();
        meAsFocusable = new FocusableFromParts(this, new DefaultFocusableBehaviour(this), this.conditionalHandler);

        return meAsFocusable;
    }

    @Override
    public Position getValue() {
        return offset;
    }

    HasValue.OnValueChangedEventHandler<Position> onChangeHandler;

    public void onChange(EventHandler consumeChange) {

        if (eventHandlerContainer == null) {
            asFocusable(); // as a side-effect, asFocusable() will build eventHandlerContainer
        }

        onChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, onChangeHandler,
                eventHandlerContainer, consumeChange);

    }

}
