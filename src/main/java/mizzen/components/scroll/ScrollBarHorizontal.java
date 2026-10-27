package mizzen.components.scroll;

import mizzen.components.BlockChars;
import mizzen.components.BlockChars.BlockHiliter;
import mizzen.components.BoundedComponent;
import mizzen.components.HasName;
import mizzen.components.Hiliter;
import mizzen.components.HorizontalLine;
import mizzen.components.input.DefaultFocusableBehaviour;
import mizzen.components.input.HasValue;
import mizzen.controlcodes.color.Color;
import mizzen.events.ChainedEventHandler;
import mizzen.events.ConditionalEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.Keys;
import mizzen.events.mouse.ApplicationMouseEvent;
import mizzen.events.mouse.Clickable;
import mizzen.events.mouse.MOUSE_ACTION;
import mizzen.geom.Dimension;
import mizzen.geom.position.Position;
import mizzen.textsegments.IScreenLineSegmentsBuffer;
import mizzen.util.Logging;

/**
 * A ScrollBar in the Horizontal orientation.
 */
public class ScrollBarHorizontal extends BoundedComponent<ScrollBarVertical>
        implements Hiliter, Clickable, Focusable, HasValue<Position> {

    private boolean hilited = false;
    Position offset;
    Dimension sizeOfComponentBeingViewed;
    Dimension sizeOfViewer;
    private BlockHiliter blockHiliter = new BlockChars.BlockHiliter();
    private boolean handleKeyEvents;

    // ▒
    // '░'

    public ScrollBarHorizontal(Color color, Position pos, int len) {
        super(color, pos, new Dimension.Simple(1, len));
        handleKeyEvents = true;
    }

    public ScrollBarHorizontal(Color color, int row, int col, int len) {
        super(color, new Position.Simple(row, col), new Dimension.Simple(1, len));
        handleKeyEvents = true;
    }

    public ScrollBarHorizontal enableKeyEvents(boolean enable) {
        handleKeyEvents = enable;
        return this;
    }

    Position calcScrollbarOffset() {
        if (sizeOfComponentBeingViewed == null || offset == null) {
            return null;
        }
        float percentage = (float) this.getWidth() / (float) sizeOfComponentBeingViewed.getWidth();
        if (percentage >= 1.0) {
            return null;
        }

        return new Position.Simple((int) (((float) offset.getRow()) * percentage),
                (int) (((float) offset.getCol()) * percentage));
    }

    Position calcOffset(Position scrollbarOffset) {
        if (sizeOfComponentBeingViewed == null || sizeOfViewer == null || scrollbarOffset == null) {
            Logging.info("nulls");
            return Position.ZERO;
        }

        int maxOffset = sizeOfComponentBeingViewed.getWidth() - sizeOfViewer.getWidth();
        if (maxOffset <= 0) {
            return Position.ZERO;
        }

        float percentage = (float) sizeOfComponentBeingViewed.getWidth() / (float) sizeOfViewer.getWidth();

        int col = (int) (((float) scrollbarOffset.getCol()) * percentage);

        if (col < 0) {
            col = 0;
        }
        if (col > maxOffset) {
            col = maxOffset;
        }

        return new Position.Simple(0, col);
    }

    boolean canScrollLeft(TerminalEvent<?> te) {
        if (sizeOfComponentBeingViewed == null || offset == null) {
            return false;
        }

        if (offset.getCol() == 0) {
            return false;
        }

        return true;
    }

    void scrollLeft(TerminalEvent<?> te) {
        if (!canScrollLeft(te)) {
            return;
        }
        setOffset(offset.offsetAdd(0, -1));
        te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<>(this));
    }

    boolean canScrollRight(TerminalEvent<?> te) {
        if (sizeOfComponentBeingViewed == null || sizeOfViewer == null || offset == null) {
            return false;
        }

        int maxOffset = sizeOfComponentBeingViewed.getWidth() - sizeOfViewer.getWidth();

        if (maxOffset <= 0) {
            return false;
        }

        return offset.getCol() < maxOffset;
    }

    void scrollRight(TerminalEvent<?> te) {
        if (!canScrollRight(te)) {
            return;
        }
        setOffset(offset.offsetAdd(0, 1));
        te.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<>(this));
    }

    public void setOffset(Position offset) {
        this.offset = offset;
        this.markAsDirty();
    }

    public void setViewedSize(Dimension viewedSize, Dimension viewerSize) {
        this.sizeOfComponentBeingViewed = viewedSize;
        this.sizeOfViewer = viewerSize;
        this.markAsDirty();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {

        Position scrollBarOffset = calcScrollbarOffset();

        if (scrollBarOffset == null) {
            segments.merge(
                    makeChildComponent(() -> new HorizontalLine(color, this.getRow(), this.getCol(), this.getWidth()))
                            .getSegmentsBuffer());
            return;
        }

        // --------XXXXXXXX--------
        // [A] [B] [C]

        float percentage = (float) sizeOfViewer.getWidth() / (float) sizeOfComponentBeingViewed.getWidth();

        int a_width = scrollBarOffset.getCol();
        int b_width = Math.max((int) (Math.round(percentage * (float) this.getWidth())), 1);
        int c_width = this.getWidth() - (a_width + b_width);

        if (b_width >= this.getWidth()) {
            segments.merge(
                    makeChildComponent(() -> new HorizontalLine(color, this.getRow(), this.getCol(), this.getWidth()))
                            .getSegmentsBuffer());
            return;
        }

        if (a_width > 0) {
            segments.merge(makeChildComponent(() -> new HorizontalLine(color, this.getRow(), this.getCol(), a_width))
                    .getSegmentsBuffer());
        }

        if (b_width > 0) {
            segments.merge(
                    makeChildComponent(() -> new HorizontalLine(color, this.getRow(), this.getCol() + a_width, b_width)
                            .setHorizontalLineChar('▒')).getSegmentsBuffer());
        }

        if (c_width > 0) {
            segments.merge(makeChildComponent(
                    () -> new HorizontalLine(color, this.getRow(), this.getCol() + a_width + b_width, c_width))
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
        // do nothing because handled by onClick(ApplicationMouseEvent appMouseEvent)
    }

    public void onClick(ApplicationMouseEvent appMouseEvent) {

        if (!MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction())
                && !MOUSE_ACTION.DOWN_LEFT_DRAG.equals(appMouseEvent.getMouseAction())) {
            return;
        }

        setOffset(calcOffset(appMouseEvent.getMousePosition()));

        appMouseEvent.getEventBus().raiseEvent(new HasValue.ValueChangedEvent<>(this));

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
    public ScrollBarHorizontal setTabStop(boolean tabStop) {
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

        eventHandlerContainer.appendEventHandler(new KeyInputEventHandler(Keys.LEFT_KEYSTROKE,
                (te) -> handleKeyEvents && canScrollLeft(te),
                (te) -> {
                    scrollLeft(te);
                }));

        eventHandlerContainer.appendEventHandler(new KeyInputEventHandler(Keys.RIGHT_KEYSTROKE,
                (te) -> handleKeyEvents && canScrollRight(te),
                (te) -> {
                    scrollRight(te);
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
