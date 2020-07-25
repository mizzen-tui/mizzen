package dtb.terminalIO.components.scroll;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.CanResize;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnRequestScrollToViewEventHandler;
import dtb.terminalIO.events.RequestScrollToView;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.traverse.FindPathToTarget;

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
public class ScrollableComponent extends Composite implements CanResize, FocusManager, Hiliter, EventHandlerContainer {

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
        viewer = new WindowViewComponent(color, new Dimension.Simple(d.getHeight() - 2, d.getWidth() - 2),
                this.componentBeingViewed);
        borderedComponent = new BorderedComponent(color, viewer, this);

        vScrollBar = new ScrollBarVertical(color, borderedComponent.getPostion_TopRight().offsetAdd(1, 0),
                borderedComponent.getHeight() - 2);
        vScrollBar.setOffset(Position.ZERO);
        vScrollBar.setViewedSize(new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer));

        hScrollBar = new ScrollBarHorizontal(color, borderedComponent.getPostion_BottomLeft().offsetAdd(0, 1),
                borderedComponent.getWidth() - 2);
        hScrollBar.setOffset(Position.ZERO);
        hScrollBar.setViewedSize(new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer));

        footerText = null;
        this.add(borderedComponent);

        setShowBorder(visibleBorder()); // add border and footertext if border defined
        positionFooterText();
        currentFocusable = footerControls;
        asEventHandler();
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

    public void setComponentBeingViewed(Component<?> componentBeingViewed) {
        this.componentBeingViewed = componentBeingViewed;
        viewer.setViewSource(componentBeingViewed);
    }

    public void setFillOnClear(boolean fillOnClear) {
        //super.setFillOnClear(fillOnClear);
        this.viewer.setFillOnClear(fillOnClear);
    }

    public void setShowBorder(boolean showBorder) {
        borderedComponent.setShowBorder(showBorder);
        if (showBorder) {
            if (footerControls == null) {
                footerControls = buildAndPopulateFooterPanel();
                this.add(footerControls);
            }
            attachFooterText();
            if (this.findChild(vScrollBar) == null) {
                this.add(vScrollBar);
            }
            if (this.findChild(hScrollBar) == null) {
                this.add(hScrollBar);
            }
        } else {
            if (footerControls != null) {
                this.remove(footerControls);
                this.footerControls = null;
                if (this.findChild(vScrollBar) != null) {
                    this.remove(vScrollBar);
                }
                if (this.findChild(hScrollBar) != null) {
                    this.remove(hScrollBar);
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
        footerControls.fullrecalcDim();
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
            markAsDirty();
            this.vScrollBar.setOffset(offset);
            this.hScrollBar.setOffset(offset);
        }
        return changedOffset;
    }

    public boolean scrollToView(Rectangle targetToView) {

        Position offset = viewer.calculateScrollToViewOffset(targetToView);

        if (offset != null) {
            return setViewOffset(offset.getRow(), offset.getCol());
        } else {
            return false;
        }
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        super.draw(color, segments);
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

    public ConditionalEventHandler addEventHandlerWithCanvasUpdate(ConditionalEventHandler eventHandler) {
        if (eventHandler == this) {
            throw new IllegalArgumentException("Trying to add yourself as an eventhandler to yourself!");
        }
        ConditionalEventHandler handler = wrapEventHandlerWithCanvasUpdate(eventHandler);
        appendEventHandler(handler);
        
        return handler;
    }

    public ConditionalEventHandler asEventHandler() {

        if (conditionalEventHandler != null) {
            return conditionalEventHandler;
        }

        chainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy();

        // hookup footer controls event handling
        chainedEventHandler.appendEventHandler(new ConditionalEventHandler() {
            @Override
            public EventHandler canHandle(TerminalEvent<?> te) {
                if (footerControls == null) {
                    return null;
                }
                if (footerControls.canHandle(te) == null) {
                    return null;
                }
                return this;
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
        chainedEventHandler.appendEventHandler(new OnRequestScrollToViewEventHandler((te) -> { // Predicate
            Component<?> componentToScrollTo = (Component<?>) ((RequestScrollToView)te).getScrollToComponent();
            // if a path from viewer to component exists, then return true
            return new FindPathToTarget<Component<?>>().exists(ScrollableComponent.this.viewer, componentToScrollTo);
        }, (te) -> { // Action
            Component<?> componentToScrollTo = (Component<?>) ((RequestScrollToView)te).getScrollToComponent();

            Position offset = Component.UTIL.getOffsetFromComponentContainer(componentToScrollTo,
                                                                             ScrollableComponent.this.viewer);

            Position offsetIntoSource = ScrollableComponent.this.viewer.getOffsetIntoSource();
            if ( ! offsetIntoSource.isSamePosition(Position.ZERO)) {

                offset = offset.offsetAdd( offsetIntoSource);
            }

            if (offset != null) {
                
                Rectangle offsetRectangle = ((RequestScrollToView)te).getScrollToOffsetRectangle();
                
                if (offsetRectangle != null) {
                    offsetRectangle = new Rectangle.Simple(offsetRectangle.offsetAdd(offset), (Dimension)offsetRectangle);
                    if (ScrollableComponent.this.scrollToView(offsetRectangle)) {
                        updateCanvas( te.getEventBus() );
                    }
                } else {
                    if ( ScrollableComponent.this.scrollToView(new Rectangle.Simple(offset, (Dimension) componentToScrollTo))) {
                        updateCanvas( te.getEventBus() );
                    }
                }
            }

        }));

        conditionalEventHandler = chainedEventHandler.asConditionalEventHandler();
        
        Collection<Focusable> focusables = getChildFocusables();
        for (Focusable focusable : focusables) {
            if (focusable != footerControls ) { /* footer control special case and handled above */
                prependEventHandler(focusable);
            }
         }
        
        return conditionalEventHandler;
    }

    public void addStandardNavigationKeyInputEventHandlers(EventBus eventBus) {
        eventBus.appendEventHandler(asEventHandler());
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

    private void __addFooterButton(Component<?> componentForButtonBar, KeyStroke hotKey, EventHandler handler) {

        if (footerControls == null) {
            throw new IllegalStateException("FooterControls object is null! Cannot add button control yet!");
        }

        positionFooterText();

    }

    public void addFooterButton(Component<?> componentForButtonBar, KeyStroke hotKey, EventHandler handler) {

        __addFooterButton(componentForButtonBar, hotKey, handler);
        positionFooterText();

    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusable, EventBus eventBus) {
        if (focusable == null) {
            return;
        }
        if (! getChildFocusables().contains(focusable)) {
            throw new IllegalStateException(
                    String.valueOf(focusable) + " is not the footer control or the viewer for this scrollable");
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
        
        if (componentBeingViewed instanceof FocusManager) {
            childFocusables = new ArrayList<Focusable>();
            childFocusables.add((FocusManager)componentBeingViewed);
            if (footerControls != null) {
                childFocusables.add(footerControls);
            }
            return childFocusables;
        }

        FocusManager componentBeingViewedFocusManager = buildFocusManagerFromComponent();

        if (componentBeingViewedFocusManager != null) {
            childFocusables = new ArrayList<Focusable>();
            childFocusables.add(componentBeingViewedFocusManager);
            if (footerControls != null) {
                childFocusables.add(footerControls);
            }
            return childFocusables;
        }

        childFocusables = new ArrayList<Focusable>();

        if (componentBeingViewed instanceof Focusable) {
            childFocusables.add((Focusable)componentBeingViewed);
        }
        if (footerControls != null) {
            childFocusables.add(footerControls);
        }

        return childFocusables;

    }

    private Supplier<DefaultFocusManager> defaultFocusManagerBuilder = ()-> new DefaultFocusManager();

    public ScrollableComponent setDefaultFocusManagerBuilder(Supplier<DefaultFocusManager> defaultFocusManagerBuilder) {
        this.defaultFocusManagerBuilder = defaultFocusManagerBuilder;
        return this;
    }


    private FocusManager buildFocusManagerFromComponent() {
        FocusManager componentBeingViewedFocusManager = FocusManager.UTIL.buildFocusManagerFromComponent(componentBeingViewed, 
                                                                                                         null,
                                                                                                         defaultFocusManagerBuilder);
        return componentBeingViewedFocusManager;
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        return buildChildFocusablesIfNeeded();
    }

}