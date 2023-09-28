package dtb.terminalIO.components.scroll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent.SearchableView;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.util.ElapsedTime;

/**
 * 
 * A ScrollableComponent is a {@link Component} that has a view into another
 * (child) component.
 * 
 * <p/>
 * 
 * If the child component is larger than this component, then the child
 * component is clipped to fit inside this component.
 * 
 * <p/>
 * 
 * You can use navigation keys (Up. Down, Left, Right, PgUp, PgDn ) to scroll
 * the child up/down left/right so that you view different portions of the
 * child.
 * 
 * <p/>
 * 
 * Scrollbars will appear as needed if/when the child is wider or taller than
 * this component. And the scrollbars will visually reflect the offset position
 * the child component is currently being viewed from.
 * 
 */
public class ScrollableComponent extends Composite implements CanResize, CanScrollToView, FocusManager, Hiliter, EventHandlerContainer {

    FocusManager      parentFocusManager;
    BorderedComponent borderedComponent;
    ScrollBarVertical vScrollBar;
    ScrollBarHorizontal hScrollBar;
    WindowViewComponent viewer;
    Component<?> componentBeingViewed;
    ButtonBar footerControls;
    TextComponent footerTextComponent;
    String footerText;
    Focusable currentFocusable;

    public ScrollableComponent(Color color, Dimension d, Component<?> componentBeingViewed) {
        this(color, d, componentBeingViewed, null);
    }
    
    public ScrollableComponent(Color color, Dimension d, Component<?> componentBeingViewed, Supplier<DefaultFocusManager> defaultFocusManagerBuilder) {

        super(color);

        if (defaultFocusManagerBuilder != null) {
            setDefaultFocusManagerBuilder(defaultFocusManagerBuilder);
        }

        this.componentBeingViewed = componentBeingViewed;

        viewer = new WindowViewComponent(color,
                                         new Dimension.Simple(d.getHeight() - 2, d.getWidth() - 2),
                                         this.componentBeingViewed);

        borderedComponent = new BorderedComponent(color, viewer, this);

        vScrollBar = new ScrollBarVertical(color,
                                           borderedComponent.getPostion_TopRight().offsetAdd(1, 0),
                                           borderedComponent.getHeight() - 2).enableKeyEvents(false);

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
                                             borderedComponent.getWidth() - 2).enableKeyEvents(false);

        HasName.UTIL.decorateWithParentsNamePrefix(hScrollBar, this);

        hScrollBar.setOffset(Position.ZERO);

        hScrollBar.setViewedSize(new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer));

        hScrollBar.onChange(  (te) ->  {
            Position curPos = this.getOffsetIntoSource();
            Position hScrolPos = hScrollBar.getValue();
            this.setViewOffset(curPos.getRow(), hScrolPos.getCol());
        } );

        footerText = null;

        this.add(borderedComponent);
        this.onResizeExpandDownAndRight(borderedComponent);

        setShowBorder(visibleBorder()); // add border and footertext if border defined

        positionFooterText();

        currentFocusable = footerControls;

        buildEventHandler();
    }
    
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
            }
            
            @Override
            public WindowViewComponent getViewPort() {
                return ScrollableComponent.this.viewer;
            }
            
        };
    }

    public ScrollableComponent setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        this.borderedComponent.setBorderColor(drawingcolor);
        this.vScrollBar.setColor(drawingcolor);
        this.hScrollBar.setColor(drawingcolor);
        setShowBorder(visibleBorder());
        return this;
    }

    public void setContentColor(Color drawingColor) {
        super.setColor(drawingColor);
        this.borderedComponent.setColor(drawingColor);
        this.componentBeingViewed.setColor(drawingColor);
        this.viewer.setColor(drawingColor);
    }

    public ScrollableComponent setEmptyColor(Color emptyColor) {
        super.setEmptyColor(emptyColor);
        if (this.viewer != null) {
            this.viewer.setEmptyColor(emptyColor);
        }
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
    }

    public void setFillOnClear(boolean fillOnClear) {
        if (this.viewer != null) {
           this.viewer.setFillOnClear(fillOnClear);
        }
        super.setFillOnClear(fillOnClear);
    }

    public ScrollableComponent setEmptyChar(char emptyChar) {
        if (this.viewer != null) {
            this.viewer.setEmptyChar(emptyChar);
         }
        super.setEmptyChar(emptyChar);
        return this;
    }

    public void setShowBorder(boolean showBorder) {
        borderedComponent.setShowBorder(showBorder);
        if (showBorder) {
            if (footerControls == null) {
                footerControls = buildAndPopulateFooterPanel();
                this.add(footerControls);
                footerControls.setParentFocusManager(this);
            }
            attachFooterText();
            if (this.findChild(vScrollBar) == null) {
                this.add(vScrollBar);
                vScrollBar.setParentFocusManager(this);
            }
            if (this.findChild(hScrollBar) == null) {
                this.add(hScrollBar);
                hScrollBar.setParentFocusManager(parentFocusManager);
            }
        } else {
            if (footerControls != null) {
                this.remove(footerControls);
                this.footerControls = null;
                if (this.findChild(vScrollBar) != null) {
                    this.remove(vScrollBar);
                    vScrollBar.setParentFocusManager(null);
                }
                if (this.findChild(hScrollBar) != null) {
                    this.remove(hScrollBar);
                    hScrollBar.setParentFocusManager(null);
                }
            }
        }
        this.markAsDirty();
    }

    public Composite setBorderColor(Color borderColors) {
        this.borderedComponent.setBorderColor(borderColors);
        this.footerControls.setColor(borderColors);
        vScrollBar.setColor(this.borderedComponent.getBorderColor());
        hScrollBar.setColor(this.borderedComponent.getBorderColor());
        return this;
    }

    public Color getBorderColor() {
        return this.borderedComponent.getBorderColor();
    }


    public Composite setTitle(String title) {
        this.borderedComponent.setTitle(title);
        return this;
    }

    public Composite setTitle(Color titleColors, String title) {
        this.borderedComponent.setTitle(titleColors, title);
        return this;
    }

    public boolean visibleBorder() {
        return this.borderedComponent.visibleBorder();
    }

    private ButtonBar buildAndPopulateFooterPanel() {
        boolean rebuild = false;
        Color borderColor = borderedComponent.getColor();
        if (footerControls == null) {
            footerControls = new ButtonBar(borderColor);
            rebuild = true;
        }

        if (footerTextComponent != null && !footerControls.containsChild(footerTextComponent)) {
            rebuild = true;
        }

        if (! rebuild) {
            return footerControls;
        }

        footerControls.removeChildren();

        if (footerTextComponent != null) {
            footerControls.addComponent(footerTextComponent);
        }

        addFooterButton("^",    Keys.UP_KEYSTROKE,    (te) -> ScrollableComponent.this.up(1));
        addFooterButton("v",    Keys.DOWN_KEYSTROKE,  (te) -> ScrollableComponent.this.down(1));
        addFooterButton("PgUp", Keys.PGUP_KEYSTROKE,  (te) -> ScrollableComponent.this.pageup());
        addFooterButton("PgDn", Keys.PGDN_KEYSTROKE,  (te) -> ScrollableComponent.this.pagedown());
        addFooterButton("«",    Keys.LEFT_KEYSTROKE,  (te) -> ScrollableComponent.this.left(1));
        addFooterButton("»",    Keys.RIGHT_KEYSTROKE, (te) -> ScrollableComponent.this.right(1));

        return footerControls;
    }

    public void setFooterText(String s) {
        this.footerText = s;
        attachFooterText();
    }

    private void attachFooterText() {
        if (!visibleBorder())
            return;

        if (footerText != null) {
            if (footerTextComponent == null) {
                footerTextComponent = new TextComponent(getColor(), footerText);
            } else {
                footerTextComponent.setTextAndResize(footerText);
            }
        } else {
            footerTextComponent = null;
        }

        buildAndPopulateFooterPanel();
        positionFooterText();
    }

    public void resize(Dimension d) {
        super.resize(d);
        borderedComponent.resize(d);

        vScrollBar.setRectangle(new Rectangle.Simple(borderedComponent.getPostion_TopRight().offsetAdd(1, 0),
                                                     new Dimension.Simple(borderedComponent.getHeight() - 2, 1)));
        vScrollBar.markAsDirty();

        hScrollBar.setRectangle(new Rectangle.Simple(borderedComponent.getPostion_BottomLeft().offsetAdd(0, 1),
                                                     (Dimension) hScrollBar));
        hScrollBar.markAsDirty();

        positionFooterText();

        markAsDirty();
        this.componentBeingViewed.markAsDirty();
    }

    public void refreshFooter() {
        footerControls.markAsDirty();
        //footerControls.fullrecalcDim();
        positionFooterText();
    }

    int lastFooterWidth = -1;

    private void positionFooterText() {
        if (!visibleBorder())
            return;
        Dimension d = this;
        // TODO: validate position - problematic when width smaller than footerText -
        // FIXME!
        int desiredRow = d.getHeight() - 1;
        int desiredCol = d.getWidth() - footerControls.getWidth() - 1;
        Position footerPosition = new Position.Simple(desiredRow < 0 ? 0 : desiredRow, desiredCol < 0 ? 0 : desiredCol);
        this.moveChildTo(footerPosition, footerControls);
        int newHScrollBarWidth = (this.borderedComponent.getWidth() - 2) - footerControls.getWidth();

        this.hScrollBar.resize(new Dimension.Simple(1, newHScrollBarWidth > 0 ? newHScrollBarWidth : 0));
        lastFooterWidth = footerControls.getWidth();
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

    public boolean scrollToView(Rectangle targetToView) {

        Position offset = viewer.calculateScrollToViewOffset(targetToView);

        if (offset != null) {
            // Logging.forceLogInfo("scrollToView " + String.valueOf(offset) );
            return setViewOffset(offset.getRow(), offset.getCol());
        } else {
            return false;
        }
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
//        super.draw(color, segments);
        
        long elapsed = ElapsedTime.onRunOf(()->{
            
            super.draw(color, segments);
            
        }).elapsed();
        
//        Logging.info("sc.draw() - elapsed: " + elapsed );
        
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }

    public Position getOffsetIntoSource() {
        return viewer.getOffsetIntoSource();
    }

    public void up(int iLines) {
        this.setViewOffset(this.getOffsetIntoSource().getRow() - iLines, this.getOffsetIntoSource().getCol());
    }

    public void down(int iLines) {
        this.setViewOffset(this.getOffsetIntoSource().getRow() + iLines, this.getOffsetIntoSource().getCol());
    }

    public void left(int iCols) {
        this.setViewOffset(this.getOffsetIntoSource().getRow(), this.getOffsetIntoSource().getCol() - iCols);
    }

    public void right(int iCols) {
        this.setViewOffset(this.getOffsetIntoSource().getRow(), this.getOffsetIntoSource().getCol() + iCols);
    }

    public void pageup() {
        int pageHeight = viewer.getHeight();
        int delta = pageHeight;
        if (this.getOffsetIntoSource().getRow() < delta) {
            delta = this.getOffsetIntoSource().getRow();
        }
        up(delta);
    }

    public void pagedown() {
        int pageHeight = viewer.getHeight();
        int delta = pageHeight;
        if (this.getOffsetIntoSource().getRow() + delta > componentBeingViewed.getBottomMostRow() + 1) {
            delta = (componentBeingViewed.getBottomMostRow() + 1) - this.getOffsetIntoSource().getRow();
        }
        down(delta);
    }

    ConditionalEventHandler conditionalEventHandler = null;
    ChainedEventHandler chainedEventHandler;

    private void updateCanvas(EventBus eventBus) {
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(this) );
    }


    public ScrollableComponent prependEventHandler(ConditionalEventHandler eventHandler) {

        asEventHandler();
        if (chainedEventHandler == null) {
            throw new IllegalStateException(
                    "Must first call asEventHandler() before calling handle(TerminalEvent<?> te)");
        }

        this.chainedEventHandler.prependEventHandler(eventHandler);
        return this;
    }

    @Override
    public ScrollableComponent appendEventHandler(ConditionalEventHandler eventHandler) {
        asEventHandler();
        if (chainedEventHandler == null) {
            throw new IllegalStateException(
                    "Must first call asEventHandler() before calling handle(TerminalEvent<?> te)");
        }
        this.chainedEventHandler.appendEventHandler(eventHandler);
        return this;
    }

    @Override
    public ScrollableComponent removeEventHandler(ConditionalEventHandler eventHandler) {
        asEventHandler();
        if (chainedEventHandler == null) {
            throw new IllegalStateException(
                    "Must first call asEventHandler() before calling handle(TerminalEvent<?> te)");
        }
        this.chainedEventHandler.removeEventHandler(eventHandler);
        return this;
    }

    private ConditionalEventHandler wrapEventHandlerWithCanvasUpdate(ConditionalEventHandler eventHandler) {
        ConditionalEventHandler handler = new ConditionalEventHandler.Delegate(eventHandler) {
            @Override
            public void handle(TerminalEvent<?> te) {
//                if (eventHandler != footerControls && !ScrollableComponent.this.viewer.hasFocus()) {
//                    ScrollableComponent.this.viewer.setFocus(true, te.getEventBus());
//                }
                super.handle(te);
                updateCanvas(te.getEventBus());
            }
        };
        return handler;
    }

    public ConditionalEventHandler prependEventHandlerWithCanvasUpdate(ConditionalEventHandler eventHandler) {
        if (eventHandler == this) {
            throw new IllegalArgumentException("Trying to add yourself as an eventhandler to yourself!");
        }
        ConditionalEventHandler handler = wrapEventHandlerWithCanvasUpdate(eventHandler);
        prependEventHandler(handler);

        return handler;
    }

//    public ConditionalEventHandler addEventHandlerWithCanvasUpdate(ConditionalEventHandler eventHandler) {
//        if (eventHandler == this) {
//            throw new IllegalArgumentException("Trying to add yourself as an eventhandler to yourself!");
//        }
//        ConditionalEventHandler handler = wrapEventHandlerWithCanvasUpdate(eventHandler);
//        appendEventHandler(handler);
//
//        return handler;
//    }

    public ConditionalEventHandler asEventHandler() {
        return buildEventHandler();
    }
    
    private ConditionalEventHandler buildEventHandler() {

        if (conditionalEventHandler != null) {
            return conditionalEventHandler;
        }

        chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(this);

        // hookup footer controls event handling
        chainedEventHandler.appendEventHandler(new ConditionalEventHandler() {
            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                if (footerControls != null  && footerControls.canHandle(te) != null) {
                    return this;
                }
                return null;
            }
            @Override
            public void handle(TerminalEvent<?> te) {
                
                if (footerControls == null) {
                    return;
                }
                footerControls.handle(te);
            }
        });

        // hookup scroll to view requests
        chainedEventHandler.appendEventHandler( new  WindowViewComponent.ScrollToViewHandler(()->this.viewer, this));

        // to avoid recursion we need to define conditionalEventHandler before calling prependEventHandler(..)
        conditionalEventHandler = chainedEventHandler.asConditionalEventHandler();

        // As this is not a standard FocusManager we have to explicitly add our own RequestFocusEvent handler
        chainedEventHandler.appendEventHandler(FocusManager.UTIL.buildRequestFocusEventHandler(this));

        // attach the child focusables - hmm.. this is non-standard beaviour...
        Collection<Focusable> focusables = getChildFocusables();
        for (Focusable focusable : focusables) {
            if (focusable != footerControls ) { /* footer control special case'd and handled above */
                prependEventHandler(focusable);
            }
        }

        return conditionalEventHandler;
    }

    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (conditionalEventHandler == null) {
            throw new IllegalStateException(
                    "Must first call asEventHandler() before calling canHandle(TerminalEvent<?> te)");
        }
        if (conditionalEventHandler.canHandle(te) != null) {
            return this;
        }
        return null;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        if (conditionalEventHandler == null) {
            throw new IllegalStateException(
                    "Must first call asEventHandler() before calling handle(TerminalEvent<?> te)");
        }
        conditionalEventHandler.handle(te);
    }

    boolean hasFocus = false;

    @Override
    public boolean hasFocus() {
        return hasFocus;
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        hasFocus = focus;
        if (hasFocus) {
            this.hilite();
        } else {
            this.unHilite();
        }
        
        Collection<Focusable> focusables = getChildFocusables();
        
        for (Focusable focusable : focusables) {
            focusable.setFocus(focus, eventBus);
        }
        updateCanvas(eventBus);
    }

    @Override
    public FocusManager getParentFocusManager() {
        return this.parentFocusManager;
    }

    @Override
    public void setParentFocusManager(FocusManager focusManager) {
        Focusable.UTIL.setParentFocusManager(this, focusManager, (fm)-> this.parentFocusManager = fm);
    }
    
    
    public Hiliter getHiliter() {
        return hiliter;
    }
    
    public void setHiliter(Hiliter hiliter) {
        this.hiliter = hiliter;
    }
    
    private Hiliter hiliter = new Hiliter() {

        @Override
        public void hilite() {
            ScrollableComponent.this.borderedComponent.hilite();
            // copy color from border
            vScrollBar.setColor(borderedComponent.getBorderColor());
            vScrollBar.hilite();
            hScrollBar.setColor(borderedComponent.getBorderColor());
            hScrollBar.hilite();

            if (footerControls != null) {
                footerControls.hilite();
            }

            ScrollableComponent.this.markAsDirty();
        }

        @Override
        public void unHilite() {
            ScrollableComponent.this.borderedComponent.unHilite();
            vScrollBar.setColor(borderedComponent.getBorderColor());
            vScrollBar.unHilite();
            hScrollBar.setColor(borderedComponent.getBorderColor());
            hScrollBar.unHilite();
            if (footerControls != null) {
                footerControls.unHilite();
            }
            ScrollableComponent.this.markAsDirty();
        }

    };

    @Override
    public void hilite() {
        hiliter.hilite();
    }

    @Override
    public void unHilite() {
        hiliter.unHilite();
    }

    private void addFooterButton(String buttonText, KeyStroke hotKey, EventHandler handler) {

        if (footerControls == null) {
            throw new IllegalStateException("FooterControls object is null! Cannot add button control yet!");
        }

        footerControls.addButton(buttonText, hotKey, handler).setUseCursor(false);

    }

    public void addFooterComponent(Component<?> componentForButtonBar) {

        if (footerControls == null) {
            throw new IllegalStateException("FooterControls object is null! Cannot add component control yet!");
        }

        footerControls.addComponent(componentForButtonBar);
        
        positionFooterText();

    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusable, EventBus eventBus) {
        if (focusable == null) {
            return;
        }
        if (! getChildFocusables().contains(focusable)) {
            return ; // ignorem
//            throw new IllegalStateException(
//                    String.valueOf(focusable) + " is not the footer control or the viewer for this scrollable");
        }

        FocusManager.UTIL.setNewFocusable(focusable, currentFocusable, eventBus, null);

        currentFocusable = focusable;
    }

    @Override
    public Focusable getCurrentChildFocus() {
        return currentFocusable;
    }

    Collection<Focusable> childFocusables = null;

    // try very hard to build a list of child focusables from componentBeingViewed and FooterControls
    private Collection<Focusable> buildChildFocusablesIfNeeded() {

        if (childFocusables != null) {
            return childFocusables;
        }

        FocusManager componentBeingViewedFocusManager = buildFocusManagerFromComponent();

        childFocusables = new ArrayList<Focusable>();

        if (componentBeingViewedFocusManager != null) {
            childFocusables.add(componentBeingViewedFocusManager);
        }

        if (footerControls != null) {
            childFocusables.add(footerControls);
        }

        if (this.vScrollBar != null) {
            childFocusables.add(this.vScrollBar);
        }
 
        if (this.hScrollBar != null) {
            childFocusables.add(this.hScrollBar);
        }

        return childFocusables;

    }

    private Supplier<DefaultFocusManager> defaultFocusManagerBuilder = ()-> new DefaultFocusManager().setNameFromOwner(this);;

    public ScrollableComponent setDefaultFocusManagerBuilder(Supplier<DefaultFocusManager> defaultFocusManagerBuilder) {
        this.defaultFocusManagerBuilder = defaultFocusManagerBuilder;
        return this;
    }


    private FocusManager buildFocusManagerFromComponent() {
        FocusManager componentBeingViewedFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(componentBeingViewed, 
                                                                                                         null,
                                                                                                         defaultFocusManagerBuilder);
        DefaultFocusManager.setDefaultNameFromParentObject(componentBeingViewedFocusManager, this);
        return componentBeingViewedFocusManager;
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        return buildChildFocusablesIfNeeded();
    }

}