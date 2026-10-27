package mizzen.components.tab;

import mizzen.components.BoundedComponent;
import mizzen.components.CharComponent;
import mizzen.components.Hiliter;
import mizzen.components.HorizontalLine;
import mizzen.components.TextComponent;
import mizzen.components.VerticalLine;
import mizzen.components.input.OnChangeListeners;
import mizzen.components.input.DefaultFocusableBehaviour;
import mizzen.components.input.HasValue;
import mizzen.components.input.SettableHasValue;
import mizzen.controlcodes.color.Color;
import mizzen.events.ChainedEventHandler;
import mizzen.events.EventBus;
import mizzen.events.EventHandler;
import mizzen.events.Registration;
import mizzen.events.RequestUpdateCanvasEvent;
import mizzen.events.TerminalEvent;
import mizzen.events.focus.FocusManager;
import mizzen.events.focus.Focusable;
import mizzen.events.focus.RequestFocusEvent;
import mizzen.events.keyboard.HasHotKey;
import mizzen.events.keyboard.KeyInputEventHandler;
import mizzen.events.keyboard.KeyStroke;
import mizzen.events.mouse.Clickable;
import mizzen.geom.Dimension;
import mizzen.geom.Rectangle;
import mizzen.geom.position.Position;
import mizzen.textsegments.CleanString;
import mizzen.textsegments.IScreenLineSegmentsBuffer;

/**
 * A TabButtonComponent is part of the TabPanel clickable components used to
 * change tab content.
 * 
 * <p/>
 * 
 * It is one of the Tab 'button's rendered at the top of the TabPanel
 * 
 * <p/>
 * 
 * <pre>
 * 
 *     +-----------------+
 *     | Sample Tab Text |
 *     +---------------- +
 * 
 * </pre>
 * 
 * @author dbalme
 *
 */
public class TabButtonComponent extends BoundedComponent<TabButtonComponent> implements Focusable, Hiliter , HasHotKey, Clickable, SettableHasValue<Boolean> {

    // implementation note:  Its important that TabComponenet implement ToFocusable and that
    // ToFocusable points to its TabAction - which is a focusable managed by the TabPanel FocusManager
    //  in this way when focus is requested of a TabComponent - it will properly travel up
    //  the FocusManager tree because it will first switch to the TabAction ( which is in the tree)
    
    
    private boolean hilited = false;
    boolean active = false;
    CleanString tabText;
    Color textActiveTabColor;
    TabAction<?> tabAction;
    Focusable meAsFocusable;
    KeyStroke hotKey;


    TabButtonComponent(Color color, String tabText, boolean active, TabAction<?> tabAction, KeyStroke hotKey) {
        super(color, new Dimension.Simple(3, tabText.length() + 4));
        this.active = active;
        this.tabText = new CleanString.Simple(tabText);
        this.textActiveTabColor = color.bold();
        this.tabAction = tabAction;
        this.hotKey = hotKey;
    }

    public KeyStroke getHotKey() {
        return this.hotKey;
    }

    public String getTabText() {
        return this.tabText.toString();
    }

    @Override
    protected void draw(Color colorcolor, IScreenLineSegmentsBuffer segments) {
        Color tabColor = active ? this.getColor().bold() : this.getColor();

        HorizontalLine topLine = makeChildComponent(
                () -> new HorizontalLine(tabColor, getRow(), getCol() + 1, getWidth() - 2));

        VerticalLine vLeft = makeChildComponent(
                () -> new VerticalLine(tabColor, this.getRow() + 1, this.getCol(), getHeight() - 2));
        VerticalLine vRight = makeChildComponent(
                () -> new VerticalLine(tabColor, this.getRow() + 1, this.getRightMostCol(), getHeight() - 2));

        // vertical!
        segments.merge(topLine.getSegmentsBuffer());
        segments.merge(vLeft.getSegmentsBuffer());
        segments.merge(vRight.getSegmentsBuffer());

        if (!active) {
            HorizontalLine bottomLine = makeChildComponent(
                    () -> new HorizontalLine(tabColor, getBottomMostRow(), getCol() + 1, getWidth() - 2));
            segments.merge(bottomLine.getSegmentsBuffer());
        }

        TextComponent tc = new TextComponent(active ? textActiveTabColor : tabColor, 1, 2, this.tabText);
        segments.merge(tc.getSegmentsBuffer());

        segments.merge(makeChildComponent(
                                            () -> new CharComponent(tabColor, this.getRow(), this.getCol(), this.getBlockChars().getTOP_LEFT()))
                                                    .getSegmentsBuffer());
        segments.merge(makeChildComponent(() -> new CharComponent(tabColor, this.getRow(), this.getRightMostCol(),
                                                    this.getBlockChars().getTOP_RIGHT())).getSegmentsBuffer());

        segments.merge(makeChildComponent(() -> new CharComponent(tabColor, this.getBottomMostRow(), this.getCol(),
                                                                    active ? this.getBlockChars().getBOTTOM_RIGHT() : this.getBlockChars().getHORIZONTAL_UP()))
                                                                            .getSegmentsBuffer());
        segments.merge(
                makeChildComponent(() -> new CharComponent(tabColor, this.getBottomMostRow(), this.getRightMostCol(),
                        active ? this.getBlockChars().getBOTTOM_LEFT() : this.getBlockChars().getHORIZONTAL_UP()))
                                .getSegmentsBuffer());
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        if (active) {
            hilite();
        } else {
            unHilite();
        }
        this.markAsDirty();
    }

    public void setTabText(String tabText) {
        setTabText(new CleanString.Simple(tabText) );
    }

    public void setTabText(CleanString tabText) {
        Rectangle new_rect = new Rectangle.Simple(new Position.Simple(this.getRow(), this.getCol()),
                                                  new Dimension.Simple(3, tabText.length() + 4));
        this.setRectangle(new_rect);
        this.tabText = tabText;
        this.markAsDirty();
    }

    public Color getTextActiveTabColor() {
        return textActiveTabColor;
    }

    public void setTextActiveTabColor(Color colortextActiveTabColor) {
        this.textActiveTabColor = colortextActiveTabColor;
    }

    @Override
    public void hilite() {
        if ( ! this.hilited) {
            markAsDirty();
        }
        //colorHiliter.hilite();
        this.hilited = true;
    }

    @Override
    public void unHilite() {
        if (this.hilited) {
            markAsDirty();
        }
        //colorHiliter.unHilite();
        this.hilited = false;
    }

    ChainedEventHandler chainedEventHandler;

    private Focusable asFocusable() {
        if (meAsFocusable != null) {
            return meAsFocusable;
        }
        
        chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);

        if (this.getHotKey() != null) {
            KeyInputEventHandler keyInputEventHandler = new KeyInputEventHandler((te)->onClick(te.getEventBus()), this.hotKey);
            chainedEventHandler.appendEventHandler(keyInputEventHandler); 
        }

        meAsFocusable = new DefaultFocusableBehaviour.Simple(this, chainedEventHandler.asConditionalEventHandler()) {
            
            @Override
            public void setFocus(boolean focus, EventBus eventBus) {
                super.setFocus(focus, eventBus);
                if (focus) {
                    // if we get focus, we can activate - but this does not mean
                    // on lose focus we de-activate
                    //setActive(true);
                    if (!isActive()) {
                        setActive(true);
                        hilite();
                        // focus arrived here (tab strip navigation): show this tab,
                        // but leave focus on the strip - see onClick()
                        activate(eventBus);
                    }
                    
                }
                //setActive(focus);
            }

        };
        return meAsFocusable;
    }

    private ChainedEventHandler getchainedHandler() {
        asFocusable(); // ensure built
        return this.chainedEventHandler;
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (asFocusable().canHandle(te) != null) {
            return this;
        }
        return null;
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
        Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> asFocusable().setParentFocusManager(fm));
    }

    @Override
    public Boolean getValue() {
        return isActive();
    }

    @Override
    public void setValue(Boolean value) {
        setActive(value);
    }

    
    // true when the tab was explicitly chosen (mouse click, hot key, prev/next shortcut):
    // the TabPanel then moves focus into the tab's content. Not set when focus merely
    // arrives on the tab button while navigating the tab strip.
    private boolean focusContentRequested = false;

    boolean consumeFocusContentRequest() {
        boolean requested = focusContentRequested;
        focusContentRequested = false;
        return requested;
    }

    /**
     * Explicitly choose this tab (mouse click, hot key): shows the tab and moves focus into its content.
     */
    public void onClick(EventBus eventBus) {
        focusContentRequested = true;
        if (!hasFocus()) {
            // focus manager should have already put focus here - but just in case ..
            eventBus.raiseEvent(new RequestFocusEvent(this));
        }
        activate(eventBus);
      //  Logging.info( HasName.UTIL.getName(this)+ " clicked!!!!!!!!  "  + Component.UTIL.pathToCanvasAsString(this) );
    }

    // shows this tab (the TabPanel reacts to the value changed event)
    private void activate(EventBus eventBus) {
        TabButtonComponent.this.setValue( (true) ); 
        eventBus.raiseEvent( new HasValue.ValueChangedEvent<>(TabButtonComponent.this) );
        eventBus.raiseEvent( new RequestUpdateCanvasEvent(this));
    }


    private final OnChangeListeners<Boolean> changeListeners = new OnChangeListeners<Boolean>(this);   // onChange: any number of listeners
    public Registration onChange(EventHandler handler) {

        return changeListeners.add(getchainedHandler(), handler);

    }

}
