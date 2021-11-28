package dtb.terminalIO.components.scroll;

import dtb.terminalIO.components.BlockChars;
import dtb.terminalIO.components.BlockChars.BlockHiliter;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.HorizontalLine;
import dtb.terminalIO.components.input.DefaultFocusableBehaviour;
import dtb.terminalIO.components.input.HasValue;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
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
import dtb.terminalIO.util.Logging;


/**
 * A ScrollBar in the Horizontal orientation.
 */
public class ScrollBarHorizontal  extends BoundedComponent<ScrollBarVertical>  implements Hiliter, Clickable, Focusable, HasValue<Position> {

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
        float percentage = (float)this.getWidth() / (float)sizeOfComponentBeingViewed.getWidth();
        if (percentage >= 1.0) {
            return null;
        }
        
        return new Position.Simple( (int)(  ((float)offset.getRow()) * percentage) , 
                                    (int)(  ((float)offset.getCol()) * percentage) );
    }

    Position calcOffset(Position scrollbarOffset) {
        if (sizeOfComponentBeingViewed == null || scrollbarOffset == null) {
            Logging.info("nulls");
            return Position.ZERO;
        }

        float percentage = (float)sizeOfComponentBeingViewed.getWidth() / ((float)this.getWidth());
        if (percentage < 1 ) {
            //Logging.info("percentage = " + percentage);
            return Position.ZERO;
        }

        Logging.info("percentage = " + percentage);

        return new Position.Simple(  0 , (int)(  ((float)scrollbarOffset.getCol()) * percentage) );
    }
    
    boolean canScrollLeft(TerminalEvent<?> te) {
        if (sizeOfComponentBeingViewed == null || offset==null) {
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
        te.getEventBus().raiseEvent( new HasValue.ValueChangedEvent<>(this) );
    }

    boolean canScrollRight(TerminalEvent<?> te) {
        if (sizeOfComponentBeingViewed == null || offset==null) {
            return false;
        }
        if (offset.getCol()>=sizeOfComponentBeingViewed.getWidth()) {
            return false;
        }
        return true;
    }

    void scrollRight(TerminalEvent<?> te) {
        if (!canScrollRight(te)) {
            return;
        }
        setOffset(offset.offsetAdd(0, 1));
        te.getEventBus().raiseEvent( new HasValue.ValueChangedEvent<>(this) );
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

        if (scrollBarOffset==null) {
            segments.merge(makeChildComponent(()->new HorizontalLine(color, this.getRow(), this.getCol(), this.getWidth() )).getSegmentsBuffer());
            return;
        }

        //  --------XXXXXXXX--------
        //    [A]     [B]      [C]

        float percentage = (float)sizeOfViewer.getWidth() / (float)sizeOfComponentBeingViewed.getWidth();

        int a_width = scrollBarOffset.getCol();
        int b_width = Math.max((int)(  Math.round( percentage * (float)this.getWidth() ) ),1);
        int c_width = this.getWidth() - ( a_width + b_width );

        if (b_width >= this.getWidth()) {
            segments.merge(makeChildComponent(()->new HorizontalLine(color, this.getRow(), this.getCol(), this.getWidth() )).getSegmentsBuffer());
            return;
        }

        if (a_width>0) {
            segments.merge(makeChildComponent(()->new HorizontalLine(color, this.getRow(), this.getCol(), a_width )).getSegmentsBuffer());
        }

        if (b_width>0) {
            segments.merge(makeChildComponent(()->new HorizontalLine(color, this.getRow(), this.getCol()+a_width, b_width ).setHorizontalLineChar('▒')).getSegmentsBuffer());
        }

        if (c_width>0) {
            segments.merge(makeChildComponent(()->new HorizontalLine(color, this.getRow(), this.getCol()+a_width+b_width, c_width )).getSegmentsBuffer());
        }
    }

    public BlockChars getBlockChars() {
        return blockHiliter.getBlockChars();
    }

    @Override
    public void hilite() {
        if ( ! this.hilited) {
            markAsDirty();
        }
        //colorHiliter.hilite();
        blockHiliter.hilite();
        this.hilited = true;
    }

    @Override
    public void unHilite() {
        if (this.hilited) {
            markAsDirty();
        }
        //colorHiliter.unHilite();
        blockHiliter.unHilite();
        this.hilited = false;
    }

    @Override
    public void onClick(EventBus eventBus) {
       // do nothing because handled by onClick(ApplicationMouseEvent appMouseEvent)
    }

    public void onClick(ApplicationMouseEvent appMouseEvent) {
        
        if (!MOUSE_ACTION.DOWN_LEFT.equals(appMouseEvent.getMouseAction()) && !MOUSE_ACTION.DOWN_LEFT_DRAG.equals(appMouseEvent.getMouseAction()) ) {
            return;
        }

        setOffset( calcOffset(appMouseEvent.getMousePosition()));

        appMouseEvent.getEventBus().raiseEvent( new HasValue.ValueChangedEvent<>(this) );
        
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
        asFocusable().setParentFocusManager(focusManager);
    }
    
    Focusable meAsFocusable;

    ChainedEventHandler eventHandlerContainer;
    ConditionalEventHandler conditionalHandler;

    Focusable asFocusable() {
        if (meAsFocusable != null) {
            return meAsFocusable;
        }

        this.eventHandlerContainer = ChainedEventHandler.useChainOfResponsibilityStrategy(HasName.UTIL.getName(this)+"-chainOfResponsibility");

        eventHandlerContainer.appendEventHandler(new KeyInputEventHandler(Keys.LEFT_KEYSTROKE, 
                                                                          (te)-> handleKeyEvents && canScrollLeft(te),
                                                                          (te)-> {
                                                                              scrollLeft(te);
                                                                          }) );

        eventHandlerContainer.appendEventHandler(new KeyInputEventHandler(Keys.RIGHT_KEYSTROKE, 
                                                                          (te)-> handleKeyEvents && canScrollRight(te),
                                                                          (te)-> {
                                                                              scrollRight(te);
                                                                          }) );

        this.conditionalHandler = this.eventHandlerContainer.asConditionalEventHandler();

        meAsFocusable = new FocusableFromParts(new DefaultFocusableBehaviour(),this.conditionalHandler);
            
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

        onChangeHandler = HasValue.UTIL.bindConsumerAsOnValueChangedHandler(this, onChangeHandler, eventHandlerContainer, consumeChange);

    }


}
