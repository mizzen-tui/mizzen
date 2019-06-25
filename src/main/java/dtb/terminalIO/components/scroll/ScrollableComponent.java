package dtb.terminalIO.components.scroll;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.BoundedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.CanResize;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.Composite;
import dtb.terminalIO.components.Hiliter;
import dtb.terminalIO.components.ParentContainer;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.EventHandlerContainer;
import dtb.terminalIO.events.OnRequestScrollToViewEventHandler;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.KeyStroke;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegmentsView;
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

    public ScrollableComponent(Color color, Dimension d, Component<?> componentBeingViewed) {

        super(color);
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
    }
    
    public ScrollableComponent setColor(Color drawingcolor) {
        super.setColor(drawingcolor);
        this.borderedComponent.setBorderColor(drawingcolor);
        this.vScrollBar.setColor(drawingcolor);
        this.hScrollBar.setColor(drawingcolor);
        setShowBorder(visibleBorder());
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
        super.setFillOnClear(fillOnClear);
        this.viewer.setFillOnClear(fillOnClear);
    }

    public void setShowBorder(boolean showBorder) {
        borderedComponent.setShowBorder(showBorder);
        if (showBorder) {
            if (footerControls == null) {
                footerControls = buildAndPopulateFooterPanel();
                this.add(footerControls);
            }
            setFooterText();
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
        Color borderColor = borderedComponent.getColor();
        if (footerControls == null) {
            footerControls = new ButtonBar(borderColor);
        }
        footerControls.removeChildren();

        addFooterButton("^", Keys.UP_KEYSTROKE, (te) -> ScrollableComponent.this.up(1));
        addFooterButton("v", Keys.DOWN_KEYSTROKE, (te) -> ScrollableComponent.this.down(1));
        addFooterButton("PgUp", Keys.PGUP_KEYSTROKE, (te) -> ScrollableComponent.this.pageup());
        addFooterButton("PgDn", Keys.PGDN_KEYSTROKE, (te) -> ScrollableComponent.this.pagedown());
        addFooterButton("«", Keys.LEFT_KEYSTROKE, (te) -> ScrollableComponent.this.left(1));
        addFooterButton("»", Keys.RIGHT_KEYSTROKE, (te) -> ScrollableComponent.this.right(1));

        return footerControls;
    }

    public void setFooterText(String s) {
        this.footerText = s;
        setFooterText();
    }

    public void setFooterText() {
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
        this.moveChildTo(new Position.Simple(desiredRow < 0 ? 0 : desiredRow, desiredCol < 0 ? 0 : desiredCol),
                footerControls);
        int newHScrollBarWidth = (this.borderedComponent.getWidth() - 2) - footerControls.getWidth();

        this.hScrollBar.resize(new Dimension.Simple(1, newHScrollBarWidth > 0 ? newHScrollBarWidth : 0));
        lastFooterWidth = footerControls.getWidth();
    }

    public void setViewOffset(int r, int c) {
        if (r < 0 || c < 0)
            return;
        if (r > componentBeingViewed.getBottomMostRow() + 1 || c > componentBeingViewed.getRightMostCol() + 1)
            return;
        markAsDirty();
        Position offset = new Position.Simple(r, c);
        viewer.setViewOffset(offset);
        this.vScrollBar.setOffset(offset);
        this.hScrollBar.setOffset(offset);
    }

    public void scrollToView(Rectangle targetToView) {

        Position offset = viewer.calculateScrollToViewOffset(targetToView);

        if (offset != null) {
            setViewOffset(offset.getRow(), offset.getCol());
        }
    }

    public static class WindowViewComponent extends BoundedComponent<WindowViewComponent>
            implements Hiliter, Focusable, ParentContainer {

        private IScreenLineSegmentsBuffer.Provider viewSource;
        private ScreenLineSegmentsView segmentsViewBuffer;

        public WindowViewComponent(Color color, Dimension d, IScreenLineSegmentsBuffer.Provider viewSource) {
            super(color, d);
            setViewSource(viewSource);
        }

        public void setViewSource(IScreenLineSegmentsBuffer.Provider viewSource) {
            this.viewSource = viewSource;
            if (viewSource instanceof Component) {
                ((Component<?>) this.viewSource).setParentContainer(this);
            }
            segmentsViewBuffer = new ScreenLineSegmentsView(this.viewSource, (Rectangle) WindowViewComponent.this);
            this.markAsDirty();
        }

        public Position calculateScrollToViewOffset(Rectangle targetToView) {
            return segmentsViewBuffer.calculateScrollToViewOffset(targetToView);
        }

        @Override
        public void resize(Dimension d) {
            super.resize(d);
            segmentsViewBuffer.resize(d);
        }

        public boolean isDirty() {
            if (super.isDirty())
                return true;
            return segmentsViewBuffer.isDirty();
        }

        public void markAsDirty() {
            super.markAsDirty();
            if (segmentsViewBuffer != null) {
                segmentsViewBuffer.markAsDirty();
            }
        }

        @Override
        protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
            segments.merge(segmentsViewBuffer);
        }

        public void setViewOffset(int r, int c) {
            setViewOffset(new Position.Simple(r, c));
        }

        public void setViewOffset(Position offset) {
            markAsDirty();
            segmentsViewBuffer.setOffsetIntoSource(offset);
        }

        Position getOffsetIntoSource() {
            return segmentsViewBuffer.getOffsetIntoSource();
        }

        @Override
        public void hilite() {
            if (viewSource instanceof Hiliter) {
                ((Hiliter) viewSource).hilite();
            }
        }

        @Override
        public void unHilite() {
            if (viewSource instanceof Hiliter) {
                ((Hiliter) viewSource).unHilite();
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Collection<? extends Component<?>> getChildren() {
            if (viewSource instanceof Component) {
                return (Collection<? extends Component<?>>) Collections.singleton((Component<?>) this.viewSource);
            }
            return Collections.emptyList();
        }

        @Override
        public Position getPositionRelativeToParent(Component<?> child) {
            // TODO Auto-generated method stub
            return Position.ZERO; // todo - need a proper implementation (using offset)
        }

        public void childResized(Component<?> child) {
            markAsDirty();
        }

        @Override
        public EventHandler canHandle(TerminalEvent<?> te) {
            if (viewSource instanceof ConditionalEventHandler) {
                return ((ConditionalEventHandler) viewSource).canHandle(te);
            }
            return null;
        }

        @Override
        public void handle(TerminalEvent<?> te) {
            if (viewSource instanceof EventHandler) {
                ((EventHandler) viewSource).handle(te);
            }
        }

        @Override
        public boolean hasFocus() {
            if (viewSource instanceof Focusable) {
                return ((Focusable) viewSource).hasFocus();
            }
            return false;
        }

        @Override
        public void setFocus(boolean focus, EventBus eventBus) {
            if (viewSource instanceof Focusable) {
                ((Focusable) viewSource).setFocus(focus, eventBus);
                return;
            }
        }
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
            public void handle(TerminalEvent<?> te) {
                if (footerControls == null) {
                    return;
                }
                footerControls.handle(te);
            }

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
        });

        // hookup scroll to view requests
        chainedEventHandler.appendEventHandler(new OnRequestScrollToViewEventHandler((te) -> { // Predicate
            Component<?> componentToScrollTo = (Component<?>) te.getEventData();
            // if a path from viewer to component exists, then return true
            return new FindPathToTarget<Component<?>>().exists(ScrollableComponent.this.viewer, componentToScrollTo);
        }, (te) -> { // Action
            Component<?> componentToScrollTo = (Component<?>) te.getEventData();

            Position offset = Component.UTIL.getOffsetFromComponentContainer(componentToScrollTo,
                    ScrollableComponent.this.viewer);

            if (offset != null) {
                ScrollableComponent.this.scrollToView(new Rectangle.Simple(offset, (Dimension) componentToScrollTo));
            }

        }));

        conditionalEventHandler = chainedEventHandler.asConditionalEventHandler();

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
        this.viewer.setFocus(focus, eventBus);
        if (footerControls != null) {
            footerControls.setFocus(focus, eventBus);
        }
        updateCanvas(eventBus);
    }

    @Override
    public void hilite() {
        this.borderedComponent.hilite();
        vScrollBar.setColor(borderedComponent.getBorderColor());
        hScrollBar.setColor(borderedComponent.getBorderColor());

        if (footerControls != null) {
            footerControls.hilite();
        }

        this.markAsDirty();
    }

    @Override
    public void unHilite() {
        this.borderedComponent.unHilite();
        vScrollBar.setColor(borderedComponent.getBorderColor());
        hScrollBar.setColor(borderedComponent.getBorderColor());
        if (footerControls != null) {
            footerControls.unHilite();
        }
        this.markAsDirty();
    }

    public void addFooterButton(String buttonText, KeyStroke hotKey, EventHandler handler) {

        if (footerControls == null) {
            throw new IllegalStateException("FooterControls object is null! Cannot add button control yet!");
        }

        footerControls.addButton(buttonText, hotKey, handler);

    }

    private void __addFooterButton(Component<?> componentForButtonBar, KeyStroke hotKey, EventHandler handler) {

        if (footerControls == null) {
            throw new IllegalStateException("FooterControls object is null! Cannot add button control yet!");
        }

        footerControls.addComponent(componentForButtonBar, hotKey, handler);

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
        if (footerControls != focusable) {
            throw new IllegalStateException(
                    String.valueOf(focusable) + " is not the footer control fro this scrollable");
        }

        FocusManager.UTIL.setNewFocusable(footerControls, null, eventBus, null);
    }

    @Override
    public Focusable getCurrentChildFocus() {
        return footerControls;
    }

}