package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import dtb.terminalIO.components.input.DraggableTextComponent;
import dtb.terminalIO.components.input.ResizeAnchor;
import dtb.terminalIO.components.scroll.CanScrollToView;
import dtb.terminalIO.components.scroll.ScrollBarHorizontal;
import dtb.terminalIO.components.scroll.ScrollBarVertical;
import dtb.terminalIO.components.scroll.WindowViewComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.EventHandler;
import dtb.terminalIO.events.TerminalEvent;
import dtb.terminalIO.events.focus.DefaultFocusManager;
import dtb.terminalIO.events.focus.FocusManager;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.Keys;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.util.Logging;

public class WindowFrame extends AbstractComponent<WindowFrame> implements ParentContainer, 
                                                                           CanResize,
                                                                           CanScrollToView,
                                                                           GetComponentAtOffset, 
                                                                           HasName<WindowFrame>, 
                                                                           FocusManager, 
                                                                           HasTitleAndBorder<WindowFrame> {

    private SimpleBorder border;
    private Component<?> child;
    private Position childPosition;
    private Position bannerPosition;
    private Composite banner;
    private ResizeAnchor resizeAnchor;
    private int row;
    private int col;
    private int height;
    private int width;
    private int minHeight = 4;
    private int minWidth = 4;
    private List<Component<?>> children;
    private boolean sizeBounded = false;
    private boolean autoScrollbars = true;
    private ScrollBarVertical vScrollBar;
    private ScrollBarHorizontal hScrollBar;
    private WindowViewComponent viewer;

    private FocusManager focusManager;
    private Function<EventBus, FocusManager> focusManagerBuilder;
    private ConditionalEventHandler eventHandler;
    private ChainedEventHandler     eventHandlerAsChainedEventHandler;


    public WindowFrame(Color color, String title, Component<?> child) {
        super(color);
        this.child = child;
        this.banner = new Composite(color);
        this.banner.add(new DraggableTextComponent(color, title, this));
        this.banner.setBounds(new Dimension.Simple(1, this.child.getWidth()));

        this.childPosition = new Position.Simple(1, 1);
        this.bannerPosition = new Position.Simple(0, 1);
        Position.Simple p = new Position.Simple(child);
        Rectangle bounds = new Rectangle.Simple(p,
                new Dimension.Simple(child.getHeight() + 2, Math.max(child.getWidth(), this.banner.getWidth()) + 2));
        this.border = new SimpleBorder(color, bounds);
        this.border.setParentContainer(this);

        this.resizeAnchor = new ResizeAnchor(this);

        this.row = p.getRow();
        this.col = p.getCol();
        this.height = Math.max(this.border.getHeight(), this.minHeight);
        this.width = Math.max(this.border.getWidth(), this.minWidth);
        populateChildrenList();

        Position anchorPos = calcAnchorPosition();
        this.resizeAnchor.moveTo(anchorPos.getRow(), anchorPos.getCol());

        buildFocusManager();

        this.setName(HasName.UTIL.deriveName(this) + "-" + title);
    }
    
    public void setChild(Component<?> child) {
        if (this.child != null) {
            this.child.setParentContainer(null);
        }
        this.child = child;
        populateChildrenList();
        buildFocusManager();
    }

    private void populateChildrenList() {
        // populate children list
        if (this.children == null) {
           this.children = new ArrayList<Component<?>>();
        } else {
            this.children.clear();
        }
        this.children.add(this.banner);
        if (viewer == null) {
           this.children.add(this.child); 
        } else {
            this.children.add(this.viewer);
            this.children.add(this.vScrollBar);
            this.children.add(this.hScrollBar);
        }
        this.children.add(this.border);
        this.children.add(this.resizeAnchor);

        // tell children who their parent is
        this.banner.setParentContainer(this);
        if (viewer == null) {
            this.child.setParentContainer(null); // avoids a check
            this.child.setParentContainer(this);
        } else {
            this.viewer.setParentContainer(this);
            this.vScrollBar.setParentContainer(this);
            this.hScrollBar.setParentContainer(this);
        }
        this.resizeAnchor.setParentContainer(this);
        this.border.setParentContainer(this);
    }

    private Position calcAnchorPosition() {
        Position bottomRight = this.getPostion_BottomRight();
        return new Position.Simple(bottomRight.getRow() - this.resizeAnchor.getHeight() + 1,
                bottomRight.getCol() - this.resizeAnchor.getWidth() + 1);
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return this.children;
    }

    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getCol() {
        return col;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void moveTo(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {

        if (!this.containsPosition(offset)) {
            return null;
        }

        if (this.banner.containsPosition(offset.offsetAdd(this.getPositionRelativeToParent(this.banner).negate()))) {
            if (this.banner instanceof GetComponentAtOffset) {
                return ((GetComponentAtOffset) this.banner).getComponentAtOffset(offset);
            }
            return this.banner;
        }

        if (this.resizeAnchor.containsPosition(offset)) {
            return this.resizeAnchor;
        }

        if (this.vScrollBar != null && this.vScrollBar.containsPosition(offset)) {
            return this.vScrollBar;
        }

        if (this.hScrollBar != null && this.hScrollBar.containsPosition(offset)) {
            return this.hScrollBar;
        }

        Component<?> theChild = (viewer != null) ? viewer : this.child;

        Position offsetForChild = offset.offsetAdd(this.getPositionRelativeToParent(theChild).negate());
        if (this.child.containsPosition(offsetForChild)) {
            if (theChild instanceof GetComponentAtOffset) {
                return ((GetComponentAtOffset) theChild).getComponentAtOffset(offsetForChild);
            }
            return this.child;
        }

        return this;
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == this.banner) {
            return this.bannerPosition;
        }

        Component<?> theChild = (viewer != null) ? viewer : this.child;

        if (child == theChild) {
            return this.childPosition;
        }

        if (child == this.resizeAnchor) {
            return this.resizeAnchor.getPostion_TopLeft();
        }

        if (child == this.border) {
            return this.border.getPostion_TopLeft();
        }
        
        if (this.hScrollBar != null && child == this.hScrollBar) {
            return this.hScrollBar.getPostion_TopLeft();
        }

        if (this.vScrollBar != null && child == this.vScrollBar) {
            return this.vScrollBar.getPostion_TopLeft();
        }

        throw new IllegalArgumentException("getPositionRelativeToParent error:  Unknown child["
                + HasName.UTIL.getName(child) + " passed to " + this.getName());
    }

    private Dimension sizeFromChildren() {
        return new Dimension.Simple(this.child.getHeight() + this.banner.getHeight() + 1,
                Math.max(this.banner.getWidth(), this.child.getWidth()) + 2);
    }

    @Override
    public void resize(Dimension d) {
        if (this.isSameDimension(d)) {
            this.sizeBounded = true;
            return;
        }

        //Logging.info(HasName.UTIL.getName(this) + ":resize:" + String.valueOf(d));

        this.height = Math.max(this.border.getHeight(), this.minHeight);
        this.width = Math.max(this.border.getWidth(), this.minWidth);

        if (d.getHeight() < this.minHeight) {
            return;
        }

        if (d.getWidth() < this.minWidth) {
            return;
        }

        this.sizeBounded = true;
        this.banner.resize(new Dimension.Simple(1, d.getWidth() - 2));

        this.border.resize(d);
        this.height = this.border.getHeight();
        this.width = this.border.getWidth();

        Position anchorPos = calcAnchorPosition();
        this.resizeAnchor.moveTo(anchorPos.getRow(), anchorPos.getCol());
        this.resizeAnchor.markAsDirty();

        if (autoScrollbars) {
            attachScrollbarsIfNeeded(d);
        } else {
            if (this.child instanceof CanResize) {
                ((CanResize) this.child).resize(new Dimension.Simple(d.getHeight() - 2, d.getWidth() - 2));
            }
        }
        
        
        // Logging.info(HasName.UTIL.getName(this) + ":resize :" + String.valueOf(new Dimension.Simple(this)));
    }

    @Override
    public void childResized(Component<?> child) {
        Logging.info(HasName.UTIL.getName(this) + ":childResized :" + String.valueOf(child));
        Dimension dimensionOfChildren = sizeFromChildren();
        if (dimensionOfChildren.isSameDimension(this)) {
            return;
        }

        this.markAsDirty();
        if (!this.sizeBounded) {
            Dimension sizeBeforeAdjustment = new Dimension.Simple(this);
            if (this.height < dimensionOfChildren.getHeight()) {
                this.height = dimensionOfChildren.getHeight();
            }
            if (this.width < dimensionOfChildren.getWidth()) {
                this.width = dimensionOfChildren.getWidth();
            }
            this.banner.resize(new Dimension.Simple(1, this.width - 2));
            this.border.resize(new Dimension.Simple(this.height, this.width));

            Position anchorPos = calcAnchorPosition();
            this.resizeAnchor.moveTo(anchorPos.getRow(), anchorPos.getCol());
            this.resizeAnchor.markAsDirty();
            if (!sizeBeforeAdjustment.isSameDimension(this)) {
                if (this.getParentContainer() != null) {
                    this.getParentContainer().childResized(this);
                }
            }
        } else {
            // size is bounded scenario - meaning we have done an explicit resize in the past...
            //                            now we want to adjust scrollbars display based on change of child size
            /*
            Dimension d = new Dimension.Simple(this.getHeight(), this.getWidth());
            
            if (autoScrollbars) {
                Logging.info(HasName.UTIL.getName(this) + ":attachScrollbarsIfNeeded :" + String.valueOf(d));
                attachScrollbarsIfNeeded(d);
            } else {
                if (this.child instanceof CanResize) {
                    ((CanResize) this.child).resize(new Dimension.Simple(d.getHeight() - 2, d.getWidth() - 2));
                }
            }
            */
            
        }

       // Logging.info(HasName.UTIL.getName(this) + ":childResized :" + String.valueOf(new Dimension.Simple(this)));
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {

        Component<?> theChild = (viewer != null) ? viewer : this.child;

        // draw child
        if (this.contains(theChild)) {
            segments.mergeAt(this.childPosition, theChild.getSegmentsBuffer());
        } else {
            Rectangle clippedArea = this;
            ScreenLineSegment.UTIL.addSegmentsWithOffsetBounded(theChild, clippedArea, segments.getSegments(),
                    this.child.getSegmentsBuffer().getSegments());
        }

        // draw border
        segments.getSegments().addAll(this.border.getSegmentsBuffer().getSegments());
        
        if (hScrollBar != null) {
            segments.merge(hScrollBar.getSegmentsBuffer());
        }

        if (vScrollBar != null) {
            segments.merge(vScrollBar.getSegmentsBuffer());
        }

        // draw the banner
        segments.mergeAt(this.bannerPosition, this.banner.getSegmentsBuffer());

        segments.merge(this.resizeAnchor.getSegmentsBuffer());

    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return this;
    }

    private FocusManager getFocusManager(EventBus eventBus) {
        if (this.focusManager == null) {
            if (focusManagerBuilder == null) {
                throw new IllegalStateException(
                        "focusManager and focusManagerBuilder are null - cannot get access to a focusManager for "
                                + this.getClass().getName());
            }
            this.focusManager = focusManagerBuilder.apply(eventBus);
        }
        return this.focusManager;
    }

    protected FocusManager getFocusManager() {
        if (this.focusManager == null) {
            throw new NullPointerException(
                    "getFocusManager(): focusManager is null - cannot get access to a focusManager for "
                            + this.getClass().getName());
        }
        return this.focusManager;
    }

    @Override
    public void hilite() {
        this.border.hilite();
    }

    @Override
    public void unHilite() {
        this.border.unHilite();
    }

    @Override
    public boolean hasBorder() {
        return this.border.hasBorder();
    }

    @Override
    public WindowFrame setBorder(boolean hasBorder) {
        this.border.setBorder(hasBorder);
        return this;
    }

    @Override
    public WindowFrame setBorderColor(Color borderColors) {
        this.setBorderColor(borderColors);
        return this;
    }

    @Override
    public WindowFrame setTitle(Color titleColors, String title) {
        this.setTitle(titleColors, title);
        return this;
    }

    @Override
    public WindowFrame setTitle(String title) {
        setTitle(title);
        return this;
    }

    @Override
    public String getTitle() {
        return this.border.getTitle();
    }

    private void buildFocusManager() {
        this.focusManagerBuilder = (eventBus) -> {
            FocusManager focusManager = FocusManager.UTIL.buildFocusManagerFromComponentChildren(this, 
                                                                                                 eventBus,
                                                                                                 () -> new DefaultFocusManager().setPrevKeyStroke(Keys.SHIFT_TAB_KEYSTROKE)
                                                                                                                                .setNextKeyStroke(Keys.TAB_KEYSTROKE)
                                                                                                                                .setNavKeysFirst(false)
                                                                                                                                .setCycling(false));
            if (focusManager == null) {
                focusManager = new DefaultFocusManager(); // FocusManager.UTIL.buildFocusManagerFromComponent() is weird
                                                          // - that's why this extra call
            }
            return focusManager;
        };

        this.focusManager = this.focusManagerBuilder.apply(new EventBus.STUBBED());
        this.eventHandlerAsChainedEventHandler = ChainedEventHandler.useChainOfResponsibilityStrategy(HasName.UTIL.getName(this)+"-chainOfResponsibility");
        this.eventHandler = this.eventHandlerAsChainedEventHandler.asConditionalEventHandler();

        // hookup scroll to view requests
        eventHandlerAsChainedEventHandler.appendEventHandler( new  WindowViewComponent.ScrollToViewHandler(()->this.viewer, this));

        eventHandlerAsChainedEventHandler.appendEventHandler( new KeyInputEventHandler((te)-> this.hScrollBar != null && this.hScrollBar.canHandle(te)!=null,
                                                                                       (te)-> {
                                                                                           this.hScrollBar.handle(te);
                                                                                       }));

        eventHandlerAsChainedEventHandler.appendEventHandler( new KeyInputEventHandler((te)-> this.vScrollBar != null && this.vScrollBar.canHandle(te)!=null,
                                                                                       (te)-> {
                                                                                           this.vScrollBar.handle(te);
                                                                                       }));

    }
    
    
    @Override
    public EventHandler canHandle(TerminalEvent<?> te) {
        if (getFocusManager(te.getEventBus()).canHandle(te) != null) {
            return this;
        }
        if (this.eventHandler != null && this.eventHandler.canHandle(te)!=null) {
            return this;
        }
        return null;
    }

    @Override
    public void handle(TerminalEvent<?> te) {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager(te.getEventBus()).getChildFocusables().size() < 1) {
            if (this.eventHandler != null && this.eventHandler.canHandle(te)!=null) {
                this.eventHandler.handle(te);
            }
            return;
        }
        if (getFocusManager(te.getEventBus()).canHandle(te) != null) {
            getFocusManager(te.getEventBus()).handle(te);
        } else {
            this.eventHandler.handle(te);
        }
    }

    @Override
    public boolean hasFocus() {
        return getFocusManager().hasFocus();
    }

    @Override
    public void setFocus(boolean focus, EventBus eventBus) {
        // I'm avoiding an obscure bug in FocusManager
        if (getFocusManager(eventBus).getChildFocusables().size() < 1) {
            return;
        }
        getFocusManager(eventBus).setFocus(focus, eventBus);

        if (focus) {
            this.hilite();
        } else {
            this.unHilite();
        }

    }

    @Override
    public void setChildAsCurrentFocus(Focusable focusableChild, EventBus eventBus) {
        getFocusManager(eventBus).setChildAsCurrentFocus(focusableChild, eventBus);
    }

    @Override
    public Focusable getCurrentChildFocus() {
        return getFocusManager().getCurrentChildFocus();
    }

    @Override
    public Collection<Focusable> getChildFocusables() {
        if (focusManager == null) {
            return Collections.emptyList();
        }
        return focusManager.getChildFocusables();
    }

    private Position getOffsetIntoSource() {
        if (viewer == null) {
            throw new NullPointerException(HasName.UTIL.getName(this)+" called getOffsetIntoSource when viewer is null!");
        }
        return viewer.getOffsetIntoSource();
    }
    
    public boolean scrollToView(Rectangle targetToView) {
        
        if (viewer == null) {
            // ignore request
            return false;
        }

        Position offset = viewer.calculateScrollToViewOffset(targetToView);

        if (offset != null) {
            // Logging.forceLogInfo("scrollToView " + String.valueOf(offset) );
            return setViewOffset(offset.getRow(), offset.getCol());
        } else {
            return false;
        }
    }
    
    
    private boolean setViewOffset(int r, int c) {
        if (viewer == null) {
            throw new NullPointerException(HasName.UTIL.getName(this)+" called setViewOffset when viewer is null!");
        }
        if (r < 0 || c < 0)
            return false; // too small
        if (r > (this.child.getBottomMostRow() + 1)  || 
            c > (this.child.getRightMostCol()  + 1)  )
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

    private void attachScrollbarsIfNeeded(Dimension d) {
        Dimension desiredViewPortSize = new Dimension.Simple(d.getHeight() - 2, d.getWidth() - 2);
        if (this.child instanceof CanResize) {
            ((CanResize) this.child).resize(desiredViewPortSize);
        }

        if (desiredViewPortSize.contains(this.child)) {
            // need to remove scollbars etc etc 
            if (viewer != null) {
                // remove scrollbars because child fits
                this.viewer = null;
                this.hScrollBar = null;
                this.vScrollBar = null;
                populateChildrenList();
                this.focusManager = null;
            }
            return;
        }

        if (viewer != null) {

            this.border.resize(d);

            vScrollBar.setRectangle(new Rectangle.Simple(this.border.getPostion_TopRight().offsetAdd(1, 0),
                                                         new Dimension.Simple(this.border.getHeight() - 2, 1)));
            vScrollBar.markAsDirty();

            hScrollBar.setRectangle(new Rectangle.Simple(this.border.getPostion_BottomLeft().offsetAdd(0, 1),
                                                         new Dimension.Simple(1, this.border.getWidth() - 2)));
            hScrollBar.markAsDirty();

            viewer.resize(desiredViewPortSize);

            return;
        }

        this.child.setParentContainer(null);

        viewer = new WindowViewComponent(this.child.getColor(),
                                         desiredViewPortSize,
                                         this.child);

        vScrollBar = new ScrollBarVertical(this.border.getBorderColor(),
                this.border.getPostion_TopRight().offsetAdd(1, 0), this.border.getHeight() - 2);

        HasName.UTIL.decorateWithParentsNamePrefix(vScrollBar, this);
        vScrollBar.setOffset(Position.ZERO);

        vScrollBar.setViewedSize(new Rectangle.Delegate(this.child), new Rectangle.Delegate(viewer));

        vScrollBar.onChange((te) -> {
            Position curPos = this.getOffsetIntoSource();
            Position vScrolPos = vScrollBar.getValue();
            this.setViewOffset(vScrolPos.getRow(), curPos.getCol());
        });

        hScrollBar = new ScrollBarHorizontal(this.border.getBorderColor(), this.border.getPostion_BottomLeft().offsetAdd(0, 1),
                this.border.getWidth() - 2);

        HasName.UTIL.decorateWithParentsNamePrefix(hScrollBar, this);

        hScrollBar.setOffset(Position.ZERO);

        hScrollBar.setViewedSize(new Rectangle.Delegate(this.child), new Rectangle.Delegate(viewer));

        hScrollBar.onChange((te) -> {
            Position curPos = this.getOffsetIntoSource();
            Position hScrolPos = hScrollBar.getValue();
            this.setViewOffset(curPos.getRow(), hScrolPos.getCol());
        });

        populateChildrenList();
        
        viewer.setOnViewSourcResize((dimChild)->childResizedInsideViewer());

        // force focusManager to rebuild itself on its next access
        this.focusManager = null;

    }
    
    void childResizedInsideViewer() {
        Logging.info("childResizedInsideViewer(): "+this.child.toString());
    }

}
