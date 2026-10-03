package dtb.terminalIO.components.scroll;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentWithFocusManager;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent.AsSearchableView;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent.SearchableView;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.RequestFocusEvent;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.util.Logging;

public class ScrollableComponent  extends ComponentWithFocusManager<ScrollableComponent> implements CanScrollToView, ViewPortNavigable, HasViewPortWindow, AsSearchableView {

    WindowViewComponent viewer;
    BorderedComponent borderedComponent;
    ScrollBarVertical vScrollBar;
    ScrollBarHorizontal hScrollBar;

    Component<?> componentBeingViewed;
    ButtonBar footerControls; 
    private CanResize onResizeService = CanResize.NOOP;

    private ViewPortNavigable navigator;


    public ScrollableComponent(Color color, Dimension d, Component<?> componentBeingViewed)  {
        super(color);
        
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

        this.appendEventHandler(new  WindowViewComponent.ScrollToViewHandler(()->this.viewer, this));

        this.appendEventHandler(new ConditionalEventHandler() {

            @Override
            public void handle(TerminalEvent<?> te) {
                // TODO Auto-generated method stub
                if (footerControls.hasFocus()) {
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
                if (footerControls.hasFocus() && footerControls.canHandle(te)==null ) {
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
        return this;
    }

    private void buildAndPopulateFooterPanel() {
        Color borderColor = borderedComponent.getColor();
        if (footerControls != null) {
            return ;
        }
        footerControls = new ButtonBar(borderColor, HasName.UTIL.getName(this)+".buttonBar");
        // scroll buttons are for the mouse (their keys work as hot keys) - not TAB stops
        footerControls.setTabStop(false);
        footerControls.addButton("^",    Keys.UP_KEYSTROKE,    (te) -> emitViewOffsetChangedOnOperation(te,()-> ScrollableComponent.this.up(1)));
        footerControls.addButton("v",    Keys.DOWN_KEYSTROKE,  (te) -> emitViewOffsetChangedOnOperation(te,()-> ScrollableComponent.this.down(1)));
        footerControls.addButton("PgUp", Keys.PGUP_KEYSTROKE,  (te) -> emitViewOffsetChangedOnOperation(te,()-> ScrollableComponent.this.pageup()));
        footerControls.addButton("PgDn", Keys.PGDN_KEYSTROKE,  (te) -> emitViewOffsetChangedOnOperation(te,()-> ScrollableComponent.this.pagedown()));
        footerControls.addButton("«",    Keys.LEFT_KEYSTROKE,  (te) -> emitViewOffsetChangedOnOperation(te,()-> ScrollableComponent.this.left(1)));
        footerControls.addButton("»",    Keys.RIGHT_KEYSTROKE, (te) -> emitViewOffsetChangedOnOperation(te,()-> ScrollableComponent.this.right(1)));

        this.borderedComponent.setFooter(footerControls);
    }

    private void emitViewOffsetChangedOnOperation(TerminalEvent<?> event, Runnable operation) {
        Position prevOffset = this.getOffsetIntoSource();
        operation.run();
        Position currentOffset = this.getOffsetIntoSource();
        if (currentOffset == prevOffset || currentOffset.isSameLocation(prevOffset)) {
            return;
        }
        event.getEventBus().raiseEvent( new HasViewPortWindow.ViewPortWindowChangedEvent(this) );
    }

    OnViewPortWindowChangedEventHandler onViewPortWindowChangedEventHandler;

    public void onViewPortChanged(EventHandler consumeChange) {
        onViewPortWindowChangedEventHandler = HasViewPortWindow.UTIL.bindConsumerAsOnViewPortWindowChangedEventHandler(this, onViewPortWindowChangedEventHandler, this.getEventHandlerContainer(), consumeChange);
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
