package dtb.terminalIO.components.scroll;

import dtb.terminalIO.components.BorderedComponent;
import dtb.terminalIO.components.ButtonBar;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.ComponentWithFocusManager;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.scroll.SearchFunctionForScrollableComponent.SearchableView;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;

public class ScrollableComponent2  extends ComponentWithFocusManager<ScrollableComponent2> implements CanScrollToView{

    WindowViewComponent viewer;
    BorderedComponent borderedComponent;
    ScrollBarVertical vScrollBar;
    ScrollBarHorizontal hScrollBar;

    Component<?> componentBeingViewed;
    ButtonBar footerControls; 
    private CanResize onResizeService = CanResize.NOOP;


    public ScrollableComponent2(Color color, Dimension d, Component<?> componentBeingViewed)  {
        super(color);
        
    //    this.setBorder(true);
        
        this.componentBeingViewed = componentBeingViewed;

        
        viewer = new WindowViewComponent(color,
                                         new Dimension.Simple(d.getHeight() - 2, d.getWidth() - 2),
                                         this.componentBeingViewed);

        borderedComponent = new BorderedComponent(color, viewer);
        this.addComponentAndItsFocusables(borderedComponent);
        this.getContainer().onResizeExpandDownAndRight(borderedComponent);

        buildAndPopulateFooterPanel();
        
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
                                             borderedComponent.getWidth() - 3 - footerControls.getWidth()).enableKeyEvents(false);

        HasName.UTIL.decorateWithParentsNamePrefix(hScrollBar, this);

        hScrollBar.setOffset(Position.ZERO);

        hScrollBar.setViewedSize(new Rectangle.Delegate(componentBeingViewed), new Rectangle.Delegate(viewer));

        hScrollBar.onChange(  (te) ->  {
            Position curPos = this.getOffsetIntoSource();
            Position hScrolPos = hScrollBar.getValue();
            this.setViewOffset(curPos.getRow(), hScrolPos.getCol());
        } );

        this.addComponentAndItsFocusables(hScrollBar);
        this.addComponentAndItsFocusables(vScrollBar);
        
        this.appendEventHandler(new  WindowViewComponent.ScrollToViewHandler(()->this.viewer, this));

    }

    public ScrollableComponent2 setTitle(String title) {
        this.borderedComponent.setTitle(title);
        return this;
    }

    public ScrollableComponent2 setTitle(Color titleColors, String title) {
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
    
    public ScrollableComponent2 appendOnResizeRule(CanResize resizableRule) {
        if (this.onResizeService == null) {
            setOnResizeRule(resizableRule);
            return this;
        }
        this.onResizeService = new CanResize.Chain(this.onResizeService, resizableRule);
        return this;
    }
    
    public ScrollableComponent2 setOnResizeRule(CanResize resizableRule) {
        this.onResizeService = resizableRule == null ? CanResize.NOOP : resizableRule;
        return this;
    }
    
    
    private void refreshFooter() {
        footerControls.markAsDirty();
        int newHScrollBarWidth = (this.borderedComponent.getWidth() - 2) - footerControls.getWidth();
        this.hScrollBar.resize(new Dimension.Simple(1, newHScrollBarWidth > 0 ? newHScrollBarWidth : 0));
    }
    
    
    private void buildAndPopulateFooterPanel() {
        Color borderColor = borderedComponent.getColor();
        if (footerControls != null) {
            return ;
        }
        footerControls = new ButtonBar(borderColor, HasName.UTIL.getName(this)+".buttonBar");
        
        footerControls.addButton("^",    Keys.UP_KEYSTROKE,    (te) -> ScrollableComponent2.this.up(1));
        footerControls.addButton("v",    Keys.DOWN_KEYSTROKE,  (te) -> ScrollableComponent2.this.down(1));
        footerControls.addButton("PgUp", Keys.PGUP_KEYSTROKE,  (te) -> ScrollableComponent2.this.pageup());
        footerControls.addButton("PgDn", Keys.PGDN_KEYSTROKE,  (te) -> ScrollableComponent2.this.pagedown());
        footerControls.addButton("«",    Keys.LEFT_KEYSTROKE,  (te) -> ScrollableComponent2.this.left(1));
        footerControls.addButton("»",    Keys.RIGHT_KEYSTROKE, (te) -> ScrollableComponent2.this.right(1));

        this.borderedComponent.setFooter(footerControls);
        this.addComponentFocusablesToFocusManager(borderedComponent);
        
    }
    
    public void addFooterComponent(Component<?> componentForButtonBar) {

        if (footerControls == null) {
            throw new IllegalStateException("FooterControls object is null! Cannot add component control yet!");
        }

        footerControls.addComponent(componentForButtonBar);

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
    
    public SearchableView asSearchableView() {
        return new SearchableView() {

            @Override
            public int getHeight() {
                return ScrollableComponent2.this.getHeight();
            }

            @Override
            public void refreshFooter() {
                ScrollableComponent2.this.refreshFooter();
            }

            @Override
            public int getWidth() {
                return ScrollableComponent2.this.getWidth();
            }

            @Override
            public boolean scrollToView(Rectangle targetToView) {
                return ScrollableComponent2.this.scrollToView(targetToView);
            }

            @Override
            public void setTitle(String message) {
                ScrollableComponent2.this.setTitle(message);
            }

            @Override
            public Component<?> getComponentBeingViewed() {
                return ScrollableComponent2.this.getComponentBeingViewed();
            }

            @Override
            public void setComponentBeingViewed(Component<?> component) {
                ScrollableComponent2.this.setComponentBeingViewed(component);
            }

            @Override
            public Color getColor() {
                return ScrollableComponent2.this.getColor();
            }

            @Override
            public void appendOnResizeRule(CanResize resizeRule) {
                ScrollableComponent2.this.appendOnResizeRule(resizeRule);
            }

            @Override
            public void addFooterComponent(Component<?> component) {
                ScrollableComponent2.this.addFooterComponent(component);
                // call resize to adjust scrollbars
                ScrollableComponent2.this.resize((Dimension)ScrollableComponent2.this);
                
            }

            @Override
            public WindowViewComponent getViewPort() {
                return ScrollableComponent2.this.viewer;
            }
        };
    }

}
