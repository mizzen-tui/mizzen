package dtb.terminalIO.components.scroll;

import java.util.function.Consumer;

import dtb.terminalIO.components.ComponentWithFocus;
import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent.AsSearchableView;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent.SearchableView;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.AsKeyboardHandler;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;

/**
 * A view onto a component larger than itself, with scrollbars, a footer of scroll buttons (for the
 * mouse) and scroll keys (UP, DOWN, PgUp, PgDn, LEFT, RIGHT). When the viewed component can take
 * focus, focus goes inside it and the scroll keys work for whatever it doesn't use; when it can't (a
 * TextComponent), the pane itself is the TAB stop - its border hilites and its keys scroll it.
 */
public class ScrollableComponent  extends ComponentWithFocus<Composite, ScrollableComponent>
                                  implements CanScrollToView, ViewPortNavigable, HasViewPortWindow, AsSearchableView {

    // its own handlers: the scroll keys, footer keys for the content, scroll-to-view and view-port-changed
    private final ChainedEventHandler ownHandlers = ChainedEventHandler.useChainOfResponsibilityStrategy(this);

    WindowViewComponent viewer;
    BorderedComponent borderedComponent;
    ScrollBarVertical vScrollBar;
    ScrollBarHorizontal hScrollBar;

    Component<?> componentBeingViewed;
    ButtonBar footerControls; 
    private CanResize onResizeService = CanResize.NOOP;

    private ViewPortNavigable navigator;


    public ScrollableComponent(Color color, Dimension d, Component<?> componentBeingViewed)  {
        super(color, new Composite());
        
    //    this.setBorder(true);

        navigator = new DefaultViewPortNavigableImpl();

        this.componentBeingViewed = componentBeingViewed;

        viewer = new WindowViewComponent(color,
                                         new Dimension.Simple(d.getHeight() - 2, d.getWidth() - 2),
                                         this.componentBeingViewed);

        borderedComponent = new BorderedComponent(color, viewer);
        this.getContainer().add(borderedComponent);
        this.getContainer().onResizeExpandDownAndRight(borderedComponent);

        buildAndPopulateFooterPanel();

        vScrollBar = new ScrollBarVertical(color,
                                           borderedComponent.getPostion_TopRight().offsetAdd(1, 0),
                                           borderedComponent.getHeight() - 2).enableKeyEvents(false)
                                           .setTabStop(false);   // mouse controls, like the footer: not a TAB stop

        HasName.UTIL.decorateWithParentsNamePrefix(vScrollBar, this);

        vScrollBar.setOffset(Position.ZERO);

        vScrollBar.setViewedSize(new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer));

        vScrollBar.onChange(  (te) ->  {
            Position curPos = this.getOffsetIntoSource();
            Position vScrolPos = vScrollBar.getValue();
            this.setViewOffset(vScrolPos.getRow(), curPos.getCol());
        } );

        hScrollBar = new ScrollBarHorizontal(color,
                                             borderedComponent.getPostion_BottomLeft().offsetAdd(0, 1),
                                             borderedComponent.getWidth() - 3 - footerControls.getWidth()).enableKeyEvents(false)
                                           .setTabStop(false);   // mouse controls, like the footer: not a TAB stop

        HasName.UTIL.decorateWithParentsNamePrefix(hScrollBar, this);

        hScrollBar.setOffset(Position.ZERO);

        hScrollBar.setViewedSize(new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer));

        hScrollBar.onChange(  (te) ->  {
            Position curPos = this.getOffsetIntoSource();
            Position hScrolPos = hScrollBar.getValue();
            this.setViewOffset(curPos.getRow(), hScrolPos.getCol());
        } );

        this.getContainer().add(hScrollBar);
        this.getContainer().add(vScrollBar);

        ownHandlers.appendEventHandler(new  WindowViewComponent.ScrollToViewHandler(()->this.viewer, this));

        ownHandlers.appendEventHandler(new ConditionalEventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {
                // TODO Auto-generated method stub
                if (footerControls.asFocusable().hasFocus()) {
                    Logging.info("FOOTER focus evt = " + te);
                    Focusable focusableChild = ToFocusable.UTIL.toFocusable(ScrollableComponent.this.componentBeingViewed);
                    if (focusableChild == null) {
                        return;
                    }
                    if (focusableChild.canHandle(te)!=null) {
                        EventBus evtBus = te.getEventBus();
                        focusableChild.handle(te.then(()->{
                            evtBus.raiseEvent( new RequestFocusEvent(focusableChild) );
                        }));
                    } 
                }
            }

            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                if (footerControls.asFocusable().hasFocus() && footerControls.asFocusable().canHandle(te)==null ) {
                    // footer cannot handle it
                    if ( ToFocusable.UTIL.isFocusable(ScrollableComponent.this.componentBeingViewed) ) {
                        Focusable focusableChild = ToFocusable.UTIL.toFocusable(ScrollableComponent.this.componentBeingViewed);
                        if (focusableChild != null) {
                            if (focusableChild.canHandle(te)!=null) {
                                return this;
                            }
                        }
                    }
                }
                return null;
            }});

        if (this.componentBeingViewed instanceof HasViewPortWindow.OnChange) {
            this.onViewPortChanged((te)-> {
                ((HasViewPortWindow.OnChange)this.componentBeingViewed).onViewPortWindowChange(this, te);
            });
        }

        // focus: inside the viewed component if it can take it, else on the pane itself; keys and other
        // events go to its own handlers
        ConditionalEventHandler own = ownHandlers.asConditionalEventHandler();
        focus().takeFocusItselfWhenNoChildCan()
               .handleKeys(AsKeyboardHandler.UTIL.toKeyboardHandler(own))
               .handleEvents(new ConditionalEventHandler.Impl(
                       (te) -> !(te instanceof KeyEvent) && own.canHandle(te) != null,
                       (te) -> own.handle(te)));
    }


    private Composite getContainer() {
        return getPanel();
    }

    public ScrollableComponent setTitle(String title) {
        this.borderedComponent.setTitle(title);
        return this;
    }

    public ScrollableComponent setTitle(Color titleColors, String title) {
        this.borderedComponent.setTitle(titleColors, title);
        return this;
    }
    

    public Component<?> getComponentBeingViewed() {
        return this.componentBeingViewed;
    }

    public synchronized void setComponentBeingViewed(Component<?> componentBeingViewed) {
        this.componentBeingViewed = componentBeingViewed;
        viewer.setViewSource(componentBeingViewed);
        vScrollBar.setViewedSize(new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer));
        hScrollBar.setViewedSize(new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer));
        if (componentBeingViewed instanceof HasViewPortWindow.OnChange) {
            this.onViewPortChanged((te)->{
                ((HasViewPortWindow.OnChange)this.componentBeingViewed).onViewPortWindowChange(this, te);
            });
        } else {
            this.onViewPortChanged(null);
        }
    }

    public boolean setViewOffset(int r, int c) {
        if (r < 0 || c < 0)
            return false; // too small
        if (r > (componentBeingViewed.getBottomMostRow() + 1)  || 
            c > (componentBeingViewed.getRightMostCol()  + 1)  )
            return false; // too large

        Position offset = new Position.Simple(r, c);
        boolean changedOffset = viewer.setViewOffset(offset);
        if (changedOffset) {
            //viewer.markAsDirty();
            markAsDirty();
            this.vScrollBar.setOffset(offset);
            this.hScrollBar.setOffset(offset);
        }
        return changedOffset;
    }

    public Position getOffsetIntoSource() {
        return viewer.getOffsetIntoSource();
    }

    public Rectangle getViewPortAsRectangle() {
        Position offset = this.getOffsetIntoSource();
        return new Rectangle.Simple(offset.getRow(), offset.getCol(), viewer.getHeight(), viewer.getWidth());
    }


    public boolean scrollToView(Rectangle targetToView) {

        Position offset = viewer.calculateScrollToViewOffset(targetToView);

        if (offset != null) {
            // Logging.forceLogInfo("scrollToView " + String.valueOf(offset) );
            return setViewOffset(offset.getRow(), offset.getCol());
        } else {
            return false;
        }
    }


    public void resize(Dimension d) {
        super.resize(d);
        borderedComponent.resize(d);

        vScrollBar.setRectangle(new Rectangle.Simple(borderedComponent.getPostion_TopRight().offsetAdd(1, 0),
                                                     new Dimension.Simple(borderedComponent.getHeight() - 2, 1)));
        vScrollBar.markAsDirty();

        // borderedComponent.getWidth() - 2 - footerControls.getWidth()
        hScrollBar.setRectangle(new Rectangle.Simple(borderedComponent.getPostion_BottomLeft().offsetAdd(0, 1),
                                                      new Dimension.Simple(1,borderedComponent.getWidth() - 2 - footerControls.getWidth()) ));
        hScrollBar.markAsDirty();

//        positionFooterText();
        handleResizeDependants(d);

        markAsDirty();
        this.componentBeingViewed.markAsDirty();
    }
    
    public void markAsDirty() {
        super.markAsDirty();
        if (vScrollBar != null) {
            vScrollBar.markAsDirty();
            hScrollBar.markAsDirty();
            // following is a hack because need to always ensure on top - but it is expensive
            // need to find a better (less called) place to do this
            getContainer().bringToFront(vScrollBar);
            getContainer().bringToFront(hScrollBar);
        }
    }
    
    
    private void handleResizeDependants(Dimension size) {
        this.onResizeService.resize(size);
        if (this.getParentContainer()!=null ) {
            this.getParentContainer().childResized(this);
        }
    }
    
    public ScrollableComponent appendOnResizeRule(CanResize resizableRule) {
        if (this.onResizeService == null) {
            setOnResizeRule(resizableRule);
            return this;
        }
        this.onResizeService = new CanResize.Chain(this.onResizeService, resizableRule);
        return this;
    }
    
    public ScrollableComponent setOnResizeRule(CanResize resizableRule) {
        this.onResizeService = resizableRule == null ? CanResize.NOOP : resizableRule;
        return this;
    }
    
    
    boolean hasFooterControls = true;
    
    private void refreshFooter() {
        if (!hasFooterControls) {
            return;
        }
        footerControls.markAsDirty();
        int newHScrollBarWidth = (this.borderedComponent.getWidth() - 2) - footerControls.getWidth();
        this.hScrollBar.resize(new Dimension.Simple(1, newHScrollBarWidth > 0 ? newHScrollBarWidth : 0));
    }
    
    public ScrollableComponent removeFooterComponent() {
        borderedComponent.removeFooterComponent();
        footerShown = false;
        return this;
    }

    private boolean footerShown = true;

    private void buildAndPopulateFooterPanel() {
        Color borderColor = borderedComponent.getColor();
        if (footerControls != null) {
            return ;
        }
        footerControls = new ButtonBar(borderColor, HasName.UTIL.getName(this)+".buttonBar");
        // the footer buttons are for the mouse - not TAB stops, no hot keys (scroll keys are the pane's
        // own keys, as for any scrollable area: they reach the pane from the focused component inside it,
        // or with focus on the pane itself, and don't move focus)
        footerControls.focus().skippedByTab();
        for (ScrollAction action : SCROLL_ACTIONS) {
            footerControls.addButton(action.label, null, (te) -> emitViewOffsetChangedOnOperation(te, () -> action.scroll.accept(this)));
        }

        this.borderedComponent.setFooter(footerControls);

        // the same actions' keys, while focus is in (or on) the pane and nothing inside used them
        KeyStroke[] keys = new KeyStroke[SCROLL_ACTIONS.length];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = SCROLL_ACTIONS[i].key;
        }
        ownHandlers.appendEventHandler(new KeyInputEventHandler(keys, (te) -> footerShown, (te) -> {
            for (ScrollAction action : SCROLL_ACTIONS) {
                if (((KeyEvent) te).isKeyStroke(action.key)) {
                    emitViewOffsetChangedOnOperation(te, () -> action.scroll.accept(this));
                    return;
                }
            }
        }));
    }

    /**
     * The scroll actions: each is a footer button (for the mouse) and a key (for the keyboard) -
     * add one here and both appear.
     */
    private static final ScrollAction[] SCROLL_ACTIONS = {
        new ScrollAction("^",    Keys.UP_KEYSTROKE,    (pane) -> pane.up(1)),
        new ScrollAction("v",    Keys.DOWN_KEYSTROKE,  (pane) -> pane.down(1)),
        new ScrollAction("PgUp", Keys.PGUP_KEYSTROKE,  ScrollableComponent::pageup),
        new ScrollAction("PgDn", Keys.PGDN_KEYSTROKE,  ScrollableComponent::pagedown),
        new ScrollAction("«",    Keys.LEFT_KEYSTROKE,  (pane) -> pane.left(1)),
        new ScrollAction("»",    Keys.RIGHT_KEYSTROKE, (pane) -> pane.right(1)),
    };

    private static final class ScrollAction {
        final String label;
        final KeyStroke key;
        final Consumer<ScrollableComponent> scroll;

        ScrollAction(String label, KeyStroke key, Consumer<ScrollableComponent> scroll) {
            this.label = label;
            this.key = key;
            this.scroll = scroll;
        }
    }

    private void emitViewOffsetChangedOnOperation(TerminalEvent<?> event, Runnable operation) {
        Position prevOffset = this.getOffsetIntoSource();
        operation.run();
        Position currentOffset = this.getOffsetIntoSource();
        if (currentOffset == prevOffset || currentOffset.isSameLocation(prevOffset)) {
            return;
        }
        event.getEventBus().raiseEvent( new HasViewPortWindow.ViewPortWindowChangedEvent(this) );
        event.getEventBus().raiseEvent( new RequestUpdateCanvasEvent(this) );     // show it (a key press repaints nothing else)
    }

    OnViewPortWindowChangedEventHandler onViewPortWindowChangedEventHandler;

    public void onViewPortChanged(EventHandler consumeChange) {
        onViewPortWindowChangedEventHandler = HasViewPortWindow.UTIL.bindConsumerAsOnViewPortWindowChangedEventHandler(this, onViewPortWindowChangedEventHandler, ownHandlers, consumeChange);
    }

    /** the pane's border is its BorderedComponent's (the container around it has none) - hilite that one */
    @Override
    public void hilite() {
        borderedComponent.hilite();
    }

    @Override
    public void unHilite() {
        borderedComponent.unHilite();
    }

    @Override
    public String getTitle() {
        return borderedComponent.getTitle();
    }

    /**
     * A scroll pane always shows its border (its scrollbars and footer sit on it): true is fine, false
     * is refused - before, it silently switched off an invisible box around the pane instead.
     */
    @Override
    public ScrollableComponent setBorder(boolean hasBorder) {
        if (!hasBorder) {
            throw new IllegalStateException(HasName.UTIL.getName(this) + ": a ScrollableComponent always shows its border");
        }
        return this;
    }

    @Override
    public boolean hasBorder() {
        return this.borderedComponent.hasBorder();
    }

    /** the visible border's colour (the bordered view's, also used for the scrollbars and footer) */
    @Override
    public ScrollableComponent setBorderColor(Color borderColor) {
        this.borderedComponent.setBorderColor(borderColor);
        if (footerControls != null) {
            footerControls.setColor(borderColor);
        }
        vScrollBar.setColor(borderColor);
        hScrollBar.setColor(borderColor);
        return this;
    }

    public Color getBorderColor() {
        return this.borderedComponent.getBorderColor();
    }

    /** colour of the content: the viewed component and the view around it */
    public void setContentColor(Color contentColor) {
        super.setColor(contentColor);
        this.borderedComponent.setColor(contentColor);
        this.componentBeingViewed.setColor(contentColor);
        this.viewer.setColor(contentColor);
    }

    private TextComponent footerText;

    /**
     * A short text in the footer, before the scroll buttons, e.g. "count: 42". Calling it again
     * replaces the text. Does nothing if the footer was removed (removeFooterComponent).
     */
    public ScrollableComponent setFooterText(String text) {
        if (footerControls == null) {
            return this;
        }
        if (footerText == null) {
            footerText = new TextComponent(getBorderColor(), text);
            footerControls.insertComponent(0, footerText);
        } else {
            footerText.setTextAndResize(text);
        }
        resize((Dimension) this);     // footer width changed: re-fit the horizontal scrollbar
        return this;
    }

    public void addFooterComponent(Component<?> componentForButtonBar) {

        if (footerControls == null) {
            throw new IllegalStateException("FooterControls object is null! Cannot add component control yet!");
        }
        footerControls.addComponent(componentForButtonBar);
    }

    public void up(int iLines) {
        this.navigator.up(iLines);
    }

    public void down(int iLines) {
        this.navigator.down(iLines);
    }

    public void left(int iCols) {
        this.navigator.left(iCols);
    }

    public void right(int iCols) {
        this.navigator.right(iCols);
    }

    public void pageup() {
        int pageHeight = viewer.getHeight();
        int delta = pageHeight;
        if (ScrollableComponent.this.getOffsetIntoSource().getRow() < delta) {
            delta = ScrollableComponent.this.getOffsetIntoSource().getRow();
        }
        up(delta);
    }

    public void pagedown() {
        int pageHeight = viewer.getHeight();
        int delta = pageHeight;
        if (ScrollableComponent.this.getOffsetIntoSource().getRow() + delta > componentBeingViewed.getBottomMostRow() + 1) {
            delta = (componentBeingViewed.getBottomMostRow() + 1) - ScrollableComponent.this.getOffsetIntoSource().getRow();
        }
        down(delta);
    }

    class DefaultViewPortNavigableImpl implements ViewPortNavigable {

        public void up(int iLines) {
            ScrollableComponent.this.setViewOffset(ScrollableComponent.this.getOffsetIntoSource().getRow() - iLines, ScrollableComponent.this.getOffsetIntoSource().getCol());
        }

        public void down(int iLines) {
            ScrollableComponent.this.setViewOffset(ScrollableComponent.this.getOffsetIntoSource().getRow() + iLines, ScrollableComponent.this.getOffsetIntoSource().getCol());
        }

        public void left(int iCols) {
            ScrollableComponent.this.setViewOffset(ScrollableComponent.this.getOffsetIntoSource().getRow(), ScrollableComponent.this.getOffsetIntoSource().getCol() - iCols);
        }

        public void right(int iCols) {
            ScrollableComponent.this.setViewOffset(ScrollableComponent.this.getOffsetIntoSource().getRow(), ScrollableComponent.this.getOffsetIntoSource().getCol() + iCols);
        }

    }
    
    @Override
    public SearchableView asSearchableView() {
        return new SearchableView() {

            @Override
            public int getHeight() {
                return ScrollableComponent.this.getHeight();
            }

            @Override
            public void refreshFooter() {
                ScrollableComponent.this.refreshFooter();
            }

            @Override
            public int getWidth() {
                return ScrollableComponent.this.getWidth();
            }

            @Override
            public boolean scrollToView(Rectangle targetToView) {
                return ScrollableComponent.this.scrollToView(targetToView);
            }

            @Override
            public void setTitle(String message) {
                ScrollableComponent.this.setTitle(message);
            }

            @Override
            public Component<?> getComponentBeingViewed() {
                return ScrollableComponent.this.getComponentBeingViewed();
            }

            @Override
            public void setComponentBeingViewed(Component<?> component) {
                ScrollableComponent.this.setComponentBeingViewed(component);
            }

            @Override
            public Color getColor() {
                return ScrollableComponent.this.getColor();
            }

            @Override
            public void appendOnResizeRule(CanResize resizeRule) {
                ScrollableComponent.this.appendOnResizeRule(resizeRule);
            }

            @Override
            public void addFooterComponent(Component<?> component) {
                ScrollableComponent.this.addFooterComponent(component);
                // call resize to adjust scrollbars
                ScrollableComponent.this.resize((Dimension)ScrollableComponent.this);
            }

            @Override
            public WindowViewComponent getViewPort() {
                return ScrollableComponent.this.viewer;
            }
        };
    }

}
