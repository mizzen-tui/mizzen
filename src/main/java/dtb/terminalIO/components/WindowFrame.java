package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.components.input.DraggableTextComponent;
import dtb.terminalIO.components.input.ParentFocusBehaviour;
import dtb.terminalIO.components.input.ResizeAnchor;
import dtb.terminalIO.components.scroll.CanScrollToView;
import dtb.terminalIO.components.scroll.ScrollBarHorizontal;
import dtb.terminalIO.components.scroll.ScrollBarVertical;
import dtb.terminalIO.components.scroll.WindowViewComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.events.ChainedEventHandler;
import dtb.terminalIO.events.ConditionalEventHandler;
import dtb.terminalIO.events.EventBus;
import dtb.terminalIO.events.RequestUpdateCanvasEvent;
import dtb.terminalIO.events.focus.Focusable;
import dtb.terminalIO.events.focus.ToFocusable;
import dtb.terminalIO.events.keyboard.AsKeyboardHandler;
import dtb.terminalIO.events.keyboard.KeyEvent;
import dtb.terminalIO.events.keyboard.KeyInputEventHandler;
import dtb.terminalIO.events.keyboard.KeyboardHandler;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;
import dtb.terminalIO.util.Logging;

/**
 * A window: a title bar to drag it by, a corner to resize it, a border, and its content - with
 * scrollbars when the content is bigger than the window.
 * <p/>
 * Focus: when the content has focusable components, TAB moves among them; the window's border lights
 * while focus is anywhere in it, and it comes to the front. When the content can't take focus (an
 * image, text), the window itself is the TAB stop and the arrow keys scroll it. The title bar,
 * resize corner and scrollbars are for the mouse, not TAB stops.
 */
public class WindowFrame extends AbstractComponent<WindowFrame> implements ParentContainer,
                                                                           CanResize,
                                                                           CanScrollToView,
                                                                           GetComponentAtOffset,
                                                                           HasName<WindowFrame>,
                                                                           HasTitleAndBorder<WindowFrame>,
                                                                           ToFocusable,
                                                                           AsKeyboardHandler {

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
    private Component<?> footer;                               // null: none; else the last inner row(s), above the bottom border
    private Component<?> footerOnBorder;                       // null: none; else on the bottom border line itself

    // its own handlers: scrolling with the scrollbars' keys, scroll-to-view requests
    private final ChainedEventHandler ownHandlers;
    private final ParentFocusBehaviour focus;


    public WindowFrame(Color color, String title, Component<?> child) {
        super(color);
        this.child = child;
        this.banner = new Composite(color);
        // title bar and resize corner are for the mouse: TAB goes straight to the window's content
        this.banner.add(new DraggableTextComponent(color, title, this).setTabStop(false));
        this.banner.setBounds(new Dimension.Simple(1, this.child.getWidth()));

        this.childPosition = new Position.Simple(1, 1);
        this.bannerPosition = new Position.Simple(0, 1);
        Position.Simple p = new Position.Simple(child);
        Rectangle bounds = new Rectangle.Simple(p,
                new Dimension.Simple(child.getHeight() + 2, Math.max(child.getWidth(), this.banner.getWidth()) + 2));
        this.border = new SimpleBorder(color, bounds);
        this.border.setParentContainer(this);

        this.resizeAnchor = new ResizeAnchor(color, this).setTabStop(false);

        this.row = p.getRow();
        this.col = p.getCol();
        this.height = Math.max(this.border.getHeight(), this.minHeight);
        this.width = Math.max(this.border.getWidth(), this.minWidth);
        populateChildrenList();

        Position anchorPos = calcAnchorPosition();
        this.resizeAnchor.moveTo(anchorPos.getRow(), anchorPos.getCol());

        this.setName(HasName.UTIL.deriveName(this) + "-" + title);

        this.ownHandlers = ChainedEventHandler.useChainOfResponsibilityStrategy(HasName.UTIL.getName(this) + "-chainOfResponsibility");
        ownHandlers.appendEventHandler(new WindowViewComponent.ScrollToViewHandler(() -> this.viewer, this));
        ownHandlers.appendEventHandler(new KeyInputEventHandler((te) -> this.hScrollBar != null && this.hScrollBar.canHandle(te) != null,
                                                                (te) -> this.hScrollBar.handle(te)));
        ownHandlers.appendEventHandler(new KeyInputEventHandler((te) -> this.vScrollBar != null && this.vScrollBar.canHandle(te) != null,
                                                                (te) -> this.vScrollBar.handle(te)));
        ConditionalEventHandler own = ownHandlers.asConditionalEventHandler();
        this.focus = ParentFocusBehaviour.of(this)
                                         .takeFocusItselfWhenNoChildCan()
                                         .handleKeys(AsKeyboardHandler.UTIL.toKeyboardHandler(own))
                                         .handleEvents(new ConditionalEventHandler.Impl(
                                                 (te) -> !(te instanceof KeyEvent) && own.canHandle(te) != null,
                                                 (te) -> own.handle(te)))
                                         .whenFocusArrives((bus) -> showFocus(true, bus))
                                         .whenFocusLeaves((bus) -> showFocus(false, bus));
    }

    @Override
    public Focusable asFocusable() {
        return focus.asFocusable();
    }

    @Override
    public KeyboardHandler asKeyboardHandler() {
        return focus.asKeyboardHandler();
    }

    /** focus arrived in the window (or left it): its border lights, and it comes to the front */
    private void showFocus(boolean focused, EventBus eventBus) {
        if (focused) {
            hilite();
            Component.UTIL.bringToFront(this);
        } else {
            unHilite();
        }
        markAsDirty();
        eventBus.raiseEvent(new RequestUpdateCanvasEvent(this));
    }
    
    /**
     * Controls (or a status line) along the bottom of the window, inside the border: the window
     * grows by the footer's height, the footer takes the window's inner width, and the content
     * keeps the rest. E.g. a plot's zoom and pan buttons. For one row drawn on the border line
     * itself, see setFooterOnBorder.
     */
    public WindowFrame setFooterPanel(Component<?> footer) {
        int before = footerHeight();
        this.footer = footer;
        populateChildrenList();
        this.height += footerHeight() - before;
        this.border.resize(new Dimension.Simple(this.height, this.width));
        fitFooter();
        Position anchorPos = calcAnchorPosition();
        this.resizeAnchor.moveTo(anchorPos.getRow(), anchorPos.getCol());
        markAsDirty();
        return this;
    }

    public Component<?> getFooterPanel() {
        return footer;
    }

    private int footerHeight() {
        return footer == null ? 0 : footer.getHeight();
    }

    /** where the footer sits: the inner rows just above the bottom border */
    private Position footerPosition() {
        return new Position.Simple(this.height - 1 - footerHeight(), 1);
    }

    /**
     * One row on the window's bottom border line - a status line, a progress meter - from one in on
     * the left (the corner shows) to just before the resize corner. The window doesn't grow, and the
     * component (if it can resize) is kept as wide as that room as the window is resized:
     * <pre>
     *   |  ...content...                    |
     *   +Copying  ######......  42%       ::+     (the resize corner at the right)
     * </pre>
     * null takes it off. It must be one row high; a component too wide for a small window isn't drawn.
     */
    public WindowFrame setFooterOnBorder(Component<?> footerOnBorder) {
        if (footerOnBorder != null && footerOnBorder.getHeight() != 1) {
            throw new IllegalArgumentException("A footer on the border is one row high, not " + footerOnBorder.getHeight()
                    + " (" + HasName.UTIL.getName(footerOnBorder) + ")");
        }
        this.footerOnBorder = footerOnBorder;
        this.border.setFooter(footerOnBorder, Align.LEFT);
        fitFooter();
        markAsDirty();
        return this;
    }

    public Component<?> getFooterOnBorder() {
        return footerOnBorder;
    }

    /** the footer as wide as the window's inside; the one on the border as wide as its line less the corners */
    private void fitFooter() {
        if (footer instanceof CanResize && footer.getWidth() != this.width - 2) {
            ((CanResize) footer).resize(new Dimension.Simple(footerHeight(), Math.max(1, this.width - 2)));
        }
        int room = this.width - 2 - this.resizeAnchor.getWidth();
        if (footerOnBorder instanceof CanResize && footerOnBorder.getWidth() != room && room > 0) {
            ((CanResize) footerOnBorder).resize(new Dimension.Simple(1, room));
        }
    }

    public void setChild(Component<?> child) {
        if (this.child != null) {
            this.child.setParentContainer(null);
        }
        this.child = child;
        populateChildrenList();
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
        if (footer != null) {
            this.children.add(this.footer);
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
        if (footer != null) {
            this.footer.setParentContainer(this);
        }
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

        if (footerOnBorder != null) {                            // on the border line: the border knows where it is
            Component<?> onBorder = this.border.getComponentAtOffset(offset.offsetAdd(this.border.getPostion_TopLeft().negate()));
            if (onBorder != null && onBorder != this.border && onBorder != footerOnBorder.getParentContainer()) {
                return onBorder;
            }
        }

        if (this.vScrollBar != null && this.vScrollBar.containsPosition(offset)) {
            return this.vScrollBar;
        }

        if (this.hScrollBar != null && this.hScrollBar.containsPosition(offset)) {
            return this.hScrollBar;
        }

        if (footer != null) {
            Position offsetForFooter = offset.offsetAdd(footerPosition().negate());
            if (footer.containsPosition(offsetForFooter)) {
                if (footer instanceof GetComponentAtOffset) {
                    return ((GetComponentAtOffset) footer).getComponentAtOffset(offsetForFooter);
                }
                return footer;
            }
        }

        Component<?> theChild = (viewer != null) ? viewer : this.child;

        Position offsetForChild = offset.offsetAdd(this.getPositionRelativeToParent(theChild).negate());
        if (theChild.containsPosition(offsetForChild)) {
            if (theChild instanceof GetComponentAtOffset) {
                return ((GetComponentAtOffset) theChild).getComponentAtOffset(offsetForChild);
            }
            return theChild;
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
            return this.childPosition ; //.offsetAdd(new Position.Simple(child));
        }

        if (child == this.resizeAnchor) {
            return this.resizeAnchor.getPostion_TopLeft();
        }

        if (child == this.border) {
            return this.border.getPostion_TopLeft();
        }

        if (footer != null && child == this.footer) {
            return footerPosition();
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
        return new Dimension.Simple(this.child.getHeight() + this.banner.getHeight() + 1 + footerHeight(),
                Math.max(this.banner.getWidth(), this.child.getWidth()) + 2);
    }

    @Override
    public void resize(Dimension d) {
        if (this.isSameDimension(d)) {
            this.sizeBounded = true;
            return;
        }

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
        fitFooter();

        if (autoScrollbars) {
            attachScrollbarsIfNeeded(d);
        } else {
            if (this.child instanceof CanResize) {
                ((CanResize) this.child).resize(new Dimension.Simple(d.getHeight() - 2 - footerHeight(), d.getWidth() - 2));
            }
        }
        // Logging.info(HasName.UTIL.getName(this) + ":resize :" + String.valueOf(new Dimension.Simple(this)));
    }

    @Override
    public void childResized(Component<?> child) {
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
            fitFooter();

            Position anchorPos = calcAnchorPosition();
            this.resizeAnchor.moveTo(anchorPos.getRow(), anchorPos.getCol());
            this.resizeAnchor.markAsDirty();
            if (!sizeBeforeAdjustment.isSameDimension(this)) {
                if (this.getParentContainer() != null) {
                    this.getParentContainer().childResized(this);
                }
            }
        } else if (this.viewer == null) {
            // a window sized by the user: its content changed size - fit it again (scrollbars if needed)
            Dimension windowSize = new Dimension.Simple(this.getHeight(), this.getWidth());
            if (autoScrollbars) {
                attachScrollbarsIfNeeded(windowSize);
            } else if (this.child instanceof CanResize) {
                ((CanResize) this.child).resize(new Dimension.Simple(windowSize.getHeight() - 2 - footerHeight(), windowSize.getWidth() - 2));
            }
            
        }

       // Logging.info(HasName.UTIL.getName(this) + ":childResized :" + String.valueOf(new Dimension.Simple(this)));
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {

        Component<?> theChild = (viewer != null) ? viewer : this.child;
        
//        Component.UTIL.assertComponentSegmentsIntegrity(theChild);
//        Rectangle bounds = new Rectangle.Simple(this);

        TreeSet<ScreenLineSegment> thisWindowSegments = segments.getSegments();
        
//        Component.UTIL.assertSegmentsIntegrity(bounds, thisWindowSegments);

        // draw border
        this.border.markAsDirty();
        this.border.setFillOnClear(false);
        thisWindowSegments.addAll(this.border.getSegmentsBuffer().getSegments());

//        Component.UTIL.assertSegmentsIntegrity(bounds, thisWindowSegments);

        Rectangle clippedArea = new Rectangle.Simple(this.border.getRow()+1, this.border.getCol()+1, this.border.getHeight()-2-footerHeight(), this.border.getWidth()-2);
        // draw child
        Rectangle childRectangleWithOffset = new Rectangle.Simple( new Position.Simple(theChild).offsetAdd(this.childPosition), theChild );
        if (clippedArea.contains(childRectangleWithOffset)) {
            //Logging.info("WindowFrame - draw1 "+ this.getName());
            //segments.mergeAt(this.childPosition, theChild.getSegmentsBuffer());
            //TreeSet<ScreenLineSegment> childSegments = this.child.getSegmentsBuffer().getSegments();
            Position childOffset = new Position.Simple(theChild).offsetAdd(this.childPosition);
//            TreeSet<ScreenLineSegment> childTree = theChild.getSegmentsBuffer().getSegments();
//            List<ScreenLineSegment> childSegmentsList = childTree.stream().map(s -> s.fromOffset(childOffset)).collect(Collectors.toList());
//            Component.UTIL.assertSegmentsIntegrity(childRectangleWithOffset, ScreenLineSegment.UTIL.makeCollection(childSegmentsList) );
            
            theChild.getSegmentsBuffer().getSegments().stream()
                                                      .map(s -> s.fromOffset(childOffset))
                                                      .forEach(s-> thisWindowSegments.add(s));
//            Component.UTIL.assertSegmentsIntegrity(bounds, thisWindowSegments);
//            Component.UTIL.assertSegmentsIntegrity(bounds, thisWindowSegments);
        } else {
            // child does not fit so need to do slower, but more intelligent, merge
            //Logging.info("WindowFrame - draw2 "+ this.getName());
            ScreenLineSegment.UTIL.addSegmentsWithOffsetBounded(this.childPosition, 
                                                                clippedArea, 
                                                                segments.getSegments(),
                                                                theChild.getSegmentsBuffer().getSegments());
            //Component.UTIL.assertSegmentsIntegrity(bounds, thisWindowSegments);
        }

        if (hScrollBar != null) {
            segments.merge(hScrollBar.getSegmentsBuffer());
            //Component.UTIL.assertSegmentsIntegrity(bounds, thisWindowSegments);
        }

        if (vScrollBar != null) {
            segments.merge(vScrollBar.getSegmentsBuffer());
            //Component.UTIL.assertSegmentsIntegrity(bounds, thisWindowSegments);
        }

        if (footer != null) {
            segments.mergeAt(footerPosition(), this.footer.getSegmentsBuffer());
        }

        // draw the banner
        segments.mergeAt(this.bannerPosition, this.banner.getSegmentsBuffer());
        //Component.UTIL.assertSegmentsIntegrity(bounds, thisWindowSegments);

        segments.merge(this.resizeAnchor.getSegmentsBuffer());
        //Component.UTIL.assertSegmentsIntegrity(bounds, thisWindowSegments);

    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return this;
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
        this.border.setBorderColor(borderColors);
        this.resizeAnchor.setColor(borderColors);
        return this;
    }

    @Override
    public WindowFrame setTitle(Color titleColors, String title) {
        this.border.setTitle(titleColors, title);
        return this;
    }

    @Override
    public WindowFrame setTitle(String title) {
        this.border.setTitle(title);
        return this;
    }

    @Override
    public String getTitle() {
        return this.border.getTitle();
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
        Dimension desiredViewPortSize = new Dimension.Simple(d.getHeight() - 2 - footerHeight(), d.getWidth() - 2);
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
            }
            return;
        }

        if (viewer != null) {

            this.border.resize(d);

            vScrollBar.setRectangle(new Rectangle.Simple(this.border.getPostion_TopRight().offsetAdd(1, 0),
                                                         new Dimension.Simple(this.border.getHeight() - 2 - footerHeight(), 1)));
            vScrollBar.markAsDirty();

            hScrollBar.setRectangle(new Rectangle.Simple(this.border.getPostion_BottomLeft().offsetAdd(0, 1),
                                                         new Dimension.Simple(1, this.border.getWidth() - 2)));
            hScrollBar.markAsDirty();

            viewer.resize(desiredViewPortSize);

            return;
        }

        // need to set child to null before attaching to viewer - exception thrown otherwise
        this.child.setParentContainer(null);

        viewer = new WindowViewComponent(this.child.getColor(),
                                         desiredViewPortSize,
                                         this.child);

        vScrollBar = new ScrollBarVertical(this.border.getBorderColor(),
                this.border.getPostion_TopRight().offsetAdd(1, 0), this.border.getHeight() - 2 - footerHeight()).setTabStop(false);   // for the mouse

        HasName.UTIL.decorateWithParentsNamePrefix(vScrollBar, this);
        vScrollBar.setOffset(Position.ZERO);

        vScrollBar.setViewedSize(new Rectangle.Delegate(this.child), new Rectangle.Delegate(viewer));

        vScrollBar.onChange((te) -> {
            Position curPos = this.getOffsetIntoSource();
            Position vScrolPos = vScrollBar.getValue();
            this.setViewOffset(vScrolPos.getRow(), curPos.getCol());
        });

        hScrollBar = new ScrollBarHorizontal(this.border.getBorderColor(), this.border.getPostion_BottomLeft().offsetAdd(0, 1),
                this.border.getWidth() - 2).setTabStop(false);   // for the mouse

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

    }

    void childResizedInsideViewer() {
        Logging.info("childResizedInsideViewer(): "+this.child.toString());
    }

}
