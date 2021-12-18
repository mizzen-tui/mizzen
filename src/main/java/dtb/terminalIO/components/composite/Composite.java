package dtb.terminalIO.components.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import dtb.terminalIO.components.AbstractComponent;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.GetComponentAtOffset;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.HasTitleAndBorder;
import dtb.terminalIO.components.SimpleBorder;
import dtb.terminalIO.components.TextComponent;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.controlcodes.color.Colors;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.geom.position.PositionHorizontalRelative;
import dtb.terminalIO.geom.position.PositionVerticalRelative;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;

/**
 * A component with child components
 */
public class Composite extends AbstractComponent<Composite>
                       implements ChildComponentManager<Composite>, 
                                  HasTitleAndBorder<Composite>, 
                                  CanResize, 
                                  GetComponentAtOffset {

    private SimpleBorder border;
    protected boolean boundsFixed = false;
    private Rectangle bounds = Rectangle.ZERO;
    private List<ComponentWithOffset> children = new ArrayList<ComponentWithOffset>();
    private List<ComponentWithOffset> childrenInZOrder = new ArrayList<ComponentWithOffset>();
    private CanResize onResizeService = CanResize.NOOP;


    protected static class ComponentWithOffset implements Rectangle {
        private Position offset;
        private Component<?> component;
        private Rectangle bounds;
        private Rectangle rectangleForLayout; // used only by layout managers ( Horizontal/VerticalPanels )
        private boolean hasShadow;
        private boolean visible = true;

        public ComponentWithOffset(Position offset, Component<?> component) {
            super();
            this.offset = offset;
            this.setComponent(component);
        }

        protected void calcBounds() {
            if (this.offset == null || this.component == null) {
                return;
            }

            ComponentWithOffset.this.bounds = new Rectangle.Delegate(component) {
                @Override
                public int getCol() {
                    return super.getCol() + offset.getCol();
                }

                @Override
                public int getRow() {
                    return super.getRow() + offset.getRow();
                }
            };
            
        }

        // this function is used by Composites that want to 
        // layout components relative to other components
        // most components will return themselves as the "rectangleForLayout"
        // but others, like the DropDownListBox will return their size
        // when their dropdownlist is not visible.
        public Rectangle getRectangleForLayout() {
            if (rectangleForLayout != null) {
                return rectangleForLayout;
            }
            // normally just return yourself
            Rectangle preferredRectangle = component.getPreferredRectangleForLayout();
            if (preferredRectangle == component) {
                rectangleForLayout = this;
                return rectangleForLayout;
            }
            
            rectangleForLayout = new Rectangle.AbstractDelegate() {
                
                public Rectangle getRectangle() {
                    return component.getPreferredRectangleForLayout();
                }
                @Override
                public int getCol() {
                    return super.getCol() + (offset != null ? offset.getCol() : 0);
                }

                @Override
                public int getRow() {
                    return super.getRow() + (offset!= null ? offset.getRow() : 0);
                }
            };
            
            return this;
        }

        public void setComponent(Component<?> component) {
            this.component = component;
            calcBounds();
        }
        
        protected Component<?> getComponent() {
            return this.component;
        }

        public void setOffset(Position offset) {
            this.offset = offset;
            calcBounds();
        }

        @Override
        public int getCol() {
            return bounds.getCol();
        }

        @Override
        public int getRow() {
            return bounds.getRow();
        }

        @Override
        public int getWidth() {
            return component.getWidth();
        }

        @Override
        public int getHeight() {
            return component.getHeight();
        }

        @Override
        public void moveTo(int row, int col) {
            this.offset = new Position.Simple(row, col);
            calcBounds();
        }
    }

    public Composite(Color color) {
        super(color);
        this.setFillOnClear(false);
    }

    public Composite() {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED);
    }

    public Composite setBounds(Dimension d) {
        if (d == null) {
            this.boundsFixed = false;
            fullrecalcDim();
            return this;
        }
        Dimension previousDimension = new Rectangle.Simple(this);
        this.bounds = new Rectangle.Simple(d);
        this.boundsFixed = true;
        if (this.border != null) {
            rebuildBorder(this.bounds);
        }
        markAsDirty();
        if (this.getParentContainer() != null && !previousDimension.isSameDimension(this)) {
            this.getParentContainer().childResized(this);
        }
        return this;
    }

    public Composite setBounds(int height, int width) {
        return setBounds(new Dimension.Simple(height, width));
    }

    protected List<ComponentWithOffset> getChildrenComponentsWithOffset() {
        return children;
    }

    protected List<ComponentWithOffset> getVisibleChildrenComponentsWithOffset() {
        return children.stream().filter(c->c.visible).collect(Collectors.toList());
    }

    private boolean resizing = false;
    
    public void resize(Dimension size) {
        if (resizing) {
            return;
        }
        try {
            resizing = true;
            // yes - size = null is valid; see setBounds(..)
            if (size != null && size.isSameDimension(this)) {
                return;
            }
            boolean isFixed = this.boundsFixed;
            setBounds(size);
            handleResizeDependants(size);
            if (size != null) {
                this.boundsFixed = isFixed;
            }
            markAsDirty();
        } finally {
            resizing = false;
        }
    }
    // run resize service and tell parent that we have resized to size
    private void handleResizeDependants(Dimension size) {
        this.onResizeService.resize(size);
        if (this.getParentContainer()!=null ) {
            this.getParentContainer().childResized(this);
        }
    }
    
    public void applyResizeRules() {
        this.onResizeService.resize(this.getPreferredRectangleForLayout());
    }
    
    public Rectangle getInnerRectangle() {
        return new Rectangle() {

            @Override
            public int getRow() {
                return Composite.this.getRow() + (hasBorder()?1:0) ;
            }

            @Override
            public int getCol() {
                return Composite.this.getWidth() + (hasBorder()?1:0) ;
            }

            @Override
            public int getWidth() {
                return Composite.this.getWidth() + (hasBorder()?-2:0) ;
            }

            @Override
            public int getHeight() {
                return Composite.this.getHeight() + (hasBorder()?-2:0) ;
            }

            @Override
            public void moveTo(int row, int col) {
                // TODO Auto-generated method stub
            }
            
        };
    }
    
    public void onResizeExpandDown(Component<?> component) {
        onResizeExpandDown(component, 0); 
    }
    
    public void onResizeExpandDown(Component<?> component, int percent) {
        if (component == null) {
            throw new NullPointerException("Component onResizeExpandDown failure: Null component!");
        }
        ComponentWithOffset child = findChildOrFail(component,"onResizeExpandDown");


        if ( ! (child.component instanceof CanResize)) {
            throw new IllegalArgumentException("child component ("+HasName.UTIL.getName(child.component)+") does not implement "+CanResize.class.getSimpleName());
        }
        
        appendOnResizeRule( percent > 0 
                            ? new  ExpandDownByPercent(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component, percent)
                            : new  ExpandDown(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component));
        
    }
    
    public void onResizeExpandRight(Component<?> component) {
        onResizeExpandRight(component, 0);
    }
    
    public void onResizeExpandRight(Component<?> component, int percent) {
        if (component == null) {
            throw new NullPointerException("Component onResizeExpandRight failure: Null component!");
        }
        ComponentWithOffset child = findChildOrFail(component,"onResizeExpandRight");

        if ( ! (child.component instanceof CanResize)) {
            throw new IllegalArgumentException("child component ("+HasName.UTIL.getName(child.component)+") does not implement "+CanResize.class.getSimpleName());
        }

        appendOnResizeRule( percent>0 
                            ?  new  ExpandRightByPercent(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component, percent)
                            : new  ExpandRight(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component));

    }
    
    public void onResizeExpandDownAndRight(Component<?> component) {
        if (component == null) {
            throw new NullPointerException("Component onResizeExpandDownAndRight failure: Null component!");
        }
        ComponentWithOffset child = findChildOrFail(component,"onResizeExpandDownAndRight");

        if ( ! (child.component instanceof CanResize)) {
            throw new IllegalArgumentException("child component ("+HasName.UTIL.getName(child.component)+") does not implement "+CanResize.class.getSimpleName());
        }
        appendOnResizeRule( new  ExpandDownAndRight(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component));
        
    }
    
    
    
    public Composite appendOnResize(Consumer<Dimension> onResize) {
        return appendOnResizeRule(CanResize.UTIL.resizableFromService(onResize));
    }

    public Composite appendOnResizeRule(CanResize resizableRule) {
        if (this.onResizeService == null) {
            setOnResizeRule(resizableRule);
            return this;
        }
        this.onResizeService = new CanResize.Chain(this.onResizeService, resizableRule);
        return this;
    }

    public Composite setOnResizeRule(CanResize resizableRule) {
        this.onResizeService = resizableRule == null ? CanResize.NOOP : resizableRule;
        return this;
    }

    public Composite setOnResize(Consumer<Dimension> onResize) {
        return setOnResizeRule(CanResize.UTIL.resizableFromService(onResize));
    }

    // TODO: not sure what this method means -- what do i
    public Composite setPosition(int i, int j) {
        this.bounds.moveTo(i, j);
        fullrecalcDim();
        return this;
    }

    private Rectangle calcBorderSize(Rectangle inner) {
        Rectangle brdr = new Rectangle.Simple(inner.getCol(), inner.getRow(), inner.getHeight() + 2,
                inner.getWidth() + 2);
        return brdr;
    }

    boolean bulkAdd = false;
    
    public void setBulkAdd(boolean b) {
        if (b != bulkAdd) {
            
            if (b == true) {
                this.bulkAdd = b;
                return ;
            }

            this.bulkAdd = false;
            fullrecalcDim();
            markAsDirty();
        }
        
    }


    private boolean  fullrecalcDim_FLAG = false;
    
    public void fullrecalcDim() {
        if (this.bulkAdd) {
            return;
        }
        
        if (fullrecalcDim_FLAG) {
            return;
        }
        
        try {
            fullrecalcDim_FLAG = true;
            Dimension previousDimension = new Dimension.Simple(this);
            fullrecalcDim(hasBorder());
            // if we have a parent, then we better tell it our new size
            if (this.getParentContainer() != null && !previousDimension.isSameDimension(this)) {
                this.onResizeService.resize(this.getPreferredRectangleForLayout());
                this.getParentContainer().childResized(this);
            }
        } finally {
            fullrecalcDim_FLAG = false;
        }
        
    }

    // calculates size of this component based on size and
    // layout (positions) of all of its children
    private void fullrecalcDim(boolean includeBorder) {

        if (boundsFixed) {  // if size is fixed (based on flag)
            return;         // then don't recalculate
        }

        List<ComponentWithOffset> visibleChildren = getVisibleChildrenComponentsWithOffset();
        
        for (ComponentWithOffset child : visibleChildren) {
            if (child.component instanceof Composite) {
                ((Composite) child.component).fullrecalcDim();
            }
        }

        Rectangle calculatedBounds = Rectangle.UTIL.calcDimensionsWith(visibleChildren);
        if (includeBorder) {
            rebuildBorder(calcBorderSize(calculatedBounds));
            bounds = new Rectangle.Simple(this.border);
            return;
        }
        bounds = calculatedBounds;
    }

    @Override
    public boolean hasBorder() {
        return border != null;
    }

    protected void rebuildBorder(Rectangle borderSize) {
        Color tColor = border != null ? border.getTitleColor() : this.getColor();
        Color bColor = border != null ? border.getColor() : this.getColor();
        String title = border != null ? border.getTitle() : null;
        this.border = new SimpleBorder(bColor, borderSize);
        this.border.setParentContainer(this);
        this.border.setTitle(tColor, title);
    }

    public Composite setBorder(boolean hasBorder) {

        Rectangle prevBounds = bounds;
        if (hasBorder && boundsFixed) {
            rebuildBorder(bounds);
        } else {
            fullrecalcDim(false);
            if (hasBorder) {
                this.bounds = calcBorderSize(this.bounds);
                rebuildBorder(this.bounds);
            } else {
                this.border = null;
            }
        }
        if ( ! prevBounds.isSameDimension(bounds)) {
            handleResizeDependants(bounds);
        }
        markAsDirty();
        return this;
    }

    public Composite setBorderColor(Color borderColors) {
        if (!hasBorder()) {
            setBorder(true);
        }
        this.border.setBorderColor(borderColors);
        // bug in border component - so this until fixed
        //this.border.setColor(borderColors);
        this.border.markAsDirty();
        this.markAsDirty();
        // due a subtle bug in the order in hw
        // setBorder(true);
        return this;
    }

    public Composite setTitle(Color titleColors, String title) {
        setBorder(true);
        this.border.setTitle(titleColors, title);
        return this;
    }

    public Composite setTitle(String title) {
        setBorder(true);
        this.border.setTitle(title);
        return this;
    }
    
    public String getTitle() {
        return this.border != null ? this.border.getTitle() : null;
    }

    private void addChild(ComponentWithOffset componentWithOffset) {
        children.add(componentWithOffset);
        childrenInZOrder.add(componentWithOffset);
    }

    private void addChild(int index, ComponentWithOffset componentWithOffset) {
        children.add(index, componentWithOffset);
        childrenInZOrder.add(componentWithOffset);
    }

    private void removeChild(ComponentWithOffset componentWithOffset) {
        children.remove(componentWithOffset);
        childrenInZOrder.remove(componentWithOffset);
    }

    protected ComponentWithOffset buildCompositeWithOffset(Position offset, Component<?> component) {
        return new ComponentWithOffset(offset, component);
    }

    protected ComponentWithOffset addComponentWithOffset(Position offset, Component<?> component) {
        ComponentWithOffset componentWithOffset = buildCompositeWithOffset(offset, component);
        addChild(componentWithOffset);
        component.setParentContainer(this);
        return componentWithOffset;
    }

    protected ComponentWithOffset addComponentWithOffset(int index, Position offset, Component<?> component) {
        ComponentWithOffset componentWithOffset = buildCompositeWithOffset(offset, component);
        addChild(index, componentWithOffset);
        component.setParentContainer(this);
        return componentWithOffset;
    }
    
    public boolean isChildVisible(Component<?> component) {
        if (component == null) {
            throw new NullPointerException("Component isChildVisible failure: Null component!");
        }
        ComponentWithOffset child = findChildOrFail(component,"isChildVisible");

        return child.visible;
    }

    public Rectangle childAsRectangleOffset(Component<?> component) {
        if (component == null) {
            throw new NullPointerException("Component isChildVisible failure: Null component!");
        }
        ComponentWithOffset child = findChildOrFail(component,"isChildVisible");
        
        return child;
    }

    public Composite hide(Component<?> component) {
        if (component == null) {
            throw new NullPointerException("Component hide failure: Cannot hide null component!");
        }
        ComponentWithOffset child = findChildOrFail(component,"hide");
        
        if ( ! child.visible) {
            return this; // no change
        }
        
        child.visible = false;
        fullrecalcDim();
        this.markAsDirty();
        return this;
    }

    public Composite show(Component<?> component) {
        if (component == null) {
            throw new NullPointerException("Component show failure: Cannot show null component!");
        }
        ComponentWithOffset child = findChildOrFail(component,"show");

        if (child.visible) {
            return this; // no change
        }

        child.visible = true;
        fullrecalcDim();
        this.markAsDirty();
        return this;
    }

    public Composite insert(int index, Position offset, Component<?> component) {
        if (component == null) {
            throw new NullPointerException("Component insert failure: Cannot add null component!");
        }
        ComponentWithOffset child = findChild(component);
        if (child != null) {
            throw new IllegalArgumentException("Component insert failure:  Duplicate (already a child component)!");
        }
        if (component == this) {
            throw new IllegalArgumentException("Component insert failure:  Cannot insert component as a child of itself!");
        }
        addComponentWithOffset(index, offset, component);
        fullrecalcDim();
        markAsDirty();
        return this;
    }

    public Composite insert(int index, Component<?> component) {
        return insert(index, Position.ZERO, component);
    }

    public Composite add(Position offset, Component<?> component) {
        if (component == null) {
            throw new NullPointerException("Component add failure: Cannot add null component!");
        }
        ComponentWithOffset child = findChild(component);
        if (child != null) {
            throw new IllegalArgumentException("Component add failure:  Duplicate (already a child component) !");
        }
        if (component == this) {
            throw new IllegalArgumentException("Component add failure:  Cannot add component as a child of itself!");
        }
        addComponentWithOffset(offset, component);
        fullrecalcDim();
        markAsDirty();
        return this;
    }

    public Composite add(Component<?> component) {
        return this.add(Position.ZERO, component);
    }

    public Composite add(int offsetRow, int offsetCol, Component<?> component) {
        return this.add(new Position.Simple(offsetRow, offsetCol), component);
    }

    public Composite addBelow(Component<?> component, Component<?> anchorComponent) {
        return addBelow(component, anchorComponent, 0);
    }

    public Composite addBelow(Component<?> component, Component<?> anchorComponent, int verticalSpacer) {
        ComponentWithOffset anchorChild = findChildOrFail(anchorComponent, "addBelow.anchorComponent["+HasName.UTIL.getName(anchorComponent)+"]");
        Position childPosition = new PositionVerticalRelative(anchorChild, verticalSpacer);
        return add(childPosition, component);
    }

    public Composite addRight(Component<?> component, Component<?> anchorComponent) {
        return addRight(component, anchorComponent, 0);
    }

    public Composite addRight(Component<?> component, Component<?> anchorComponent, int horizontalSpacer) {
        ComponentWithOffset anchorChild = findChildOrFail(anchorComponent, "addRight.anchorComponent["+HasName.UTIL.getName(anchorComponent)+"]");
        Position childPosition = new PositionHorizontalRelative(anchorChild, horizontalSpacer);
        return add(childPosition, component);
    }


    private void removeOneComponent(Component<?> component) {
        ComponentWithOffset child = findChildOrFail(component, "remove");
        removeChild(child);
        child.component.setParentContainer(null);
    }

    public Composite remove(Component<?> component) {
        removeOneComponent(component);
        fullrecalcDim();
        markAsDirty();
        return this;
    }

    // layering functions

    public Composite moveUp(Component<?> component) {
        ComponentWithOffset child = findChildOrFail(component, "moveUp");
        int index = childrenInZOrder.indexOf(child);
        if (index == childrenInZOrder.size() - 1) {
            return this;
        }
        childrenInZOrder.remove(child);
        childrenInZOrder.add(index + 1, child);
        markAsDirty();
        return this;
    }

    public Composite moveDown(Component<?> component) {
        ComponentWithOffset child = findChildOrFail(component, "moveDown");
        int index = childrenInZOrder.indexOf(child);
        if (index == 0) {
            return this;
        }
        childrenInZOrder.remove(child);
        childrenInZOrder.add(index - 1, child);
        markAsDirty();
        return this;
    }

    public Composite bringToFront(Component<?> component) {
        ComponentWithOffset child = findChildOrFail(component, "bringToFront");
        int index = childrenInZOrder.indexOf(child);
        if (index == childrenInZOrder.size() - 1) {
            return this;
        }
        if (index == childrenInZOrder.size()-1) {
            // already at the top
            return this;
        }
        childrenInZOrder.remove(child);
        childrenInZOrder.add(childrenInZOrder.size(), child);
        markAsDirty();
        return this;
    }

    public boolean isChildOnTop(Component<?> component) {
        ComponentWithOffset child = findChildOrFail(component, "isChildOnTop");
        int index = childrenInZOrder.indexOf(child);
        if (index == childrenInZOrder.size() - 1) {
            return false;
        }
        if (index == childrenInZOrder.size()-1) {
            // already at the top
            return true;
        }
        return false;
    }

    public Composite bringToBack(Component<?> component) {
        ComponentWithOffset child = findChildOrFail(component, "bringToBack");
        int index = childrenInZOrder.indexOf(child);
        if (index == 0) {
            return this;
        }
        childrenInZOrder.remove(child);
        childrenInZOrder.add(0, child);
        markAsDirty();
        return this;
    }

    protected ComponentWithOffset findChild(Component<?> component) {
        for (ComponentWithOffset child : children) {
            if (component == child.component) {
                return child;
            }
        }
        return null;
    }

    public int indexOf(Component<?> component) {
        int index = 0;
        for (ComponentWithOffset child : children) {
            if (component == child.component) {
                return index;
            }
            index++;
        }
        return -1;
    }
    
    public Component<?> getChild(int index) {
        return children.get(index).component;
    }
    
    public int size() {
        return children.size();
    }

    protected ComponentWithOffset findChildOrFail(Component<?> component) {
        return findChildOrFail(component, null);
    }

    public boolean containsChild(Component<?> component) {
        ComponentWithOffset child = findChild(component);
        return (child != null);
    }

    protected ComponentWithOffset findChildOrFail(Component<?> component, String errorPrefix) {
        ComponentWithOffset child = findChild(component);
        if (child == null) {
            throw new IllegalArgumentException(
                    (errorPrefix == null ? "" : errorPrefix + ": ") + "Could not find component!");
        }
        return child;
    }

    public Composite replace(Component<?> c1, Component<?> c2) {
        ComponentWithOffset child = findChildOrFail(c1, "replace");
        if (findChild(c2) != null) {
            throw new IllegalArgumentException(
                    "Could not replace component with a component that already exists as a child in composite!");
        }
        child.component.setParentContainer(null);
        child.setComponent(c2);
        child.component.setParentContainer(this);
        fullrecalcDim();
        markAsDirty();
        return this;
    }
    
    public Composite setDropShadow(boolean dropShadow, Component<?> component) {
        ComponentWithOffset found = findChildOrFail(component, "setDropShadow");
        found.hasShadow = dropShadow;
        return this;
    }

    public Composite moveChildTo(Position offset, Component<?> component) {
        ComponentWithOffset found = findChildOrFail(component, "moveChildTo");
        found.offset = offset;
        fullrecalcDim();
        markAsDirty();
        return this;
    }

    public Composite moveChildDelta(Position offset, Component<?> component) {
        ComponentWithOffset found = findChildOrFail(component, "moveChildDelta");
        found.offset = found.offset.offsetAdd(offset);
        fullrecalcDim();
        markAsDirty();
        return this;
    }

    @Override
    public boolean moveChildToPosition(Position position, Component<?> child) {
        moveChildTo(position, child);
        return true;
    }
    
    public boolean canMoveChild(Component<?> child) {
        if (findChild(child) != null) {
            return true;
        }
        return false;
    }

    
    public Composite append(Component<?> component) {

        if (children.size() == 0) {
            return add(component);
        }

        boolean hasBorder = hasBorder();

        ComponentWithOffset cp = this.children.stream().max(Position.UTIL.POS_COMPARE).get();
        int rightMost = cp.getRightMostCol();
        if (component.getWidth() + rightMost < this.bounds.getWidth() + (hasBorder ? -2 : 0)) {
            return this.add(cp.getRow(), rightMost + 1, component);
        }

        Rectangle calculatedBounds = Rectangle.UTIL.calcDimensionsWith(children);

        int bottomMost = calculatedBounds.getBottomMostRow();
        return this.add(bottomMost + 1, 0, component);

    }

    @Override
    public int getCol() {
        return bounds.getCol();
    }

    @Override
    public int getRow() {
        return bounds.getRow();
    }

    @Override
    public int getWidth() {
        return bounds.getWidth();
    }

    @Override
    public int getHeight() {
        return bounds.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        Position delta = this.offsetToRowCol(row, col);
        bounds.moveDelta(delta.getRow(), delta.getCol());
        fullrecalcDim();
    }

    public void clearAndRemoveAllChildren() {

        List<ComponentWithOffset> copiedChildren = new ArrayList<ComponentWithOffset>(this.children);

        for (ComponentWithOffset child : copiedChildren) {
            // TODO: remove all related child resize requests 
            removeOneComponent(child.component);
        }
        fullrecalcDim();

        this.clear();
        
        this.onResizeService = CanResize.NOOP;

    }

    protected void drawBorderIfNeeded(IScreenLineSegmentsBuffer segments) {
        if (hasBorder()) {
            IScreenLineSegmentsBuffer borderSegments = this.border.getSegmentsBuffer();
            segments.merge(borderSegments);
        }
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        draw(color, segments, boundsFixed);
    }

    protected void draw(Color color, IScreenLineSegmentsBuffer segments, boolean boundsFixed) {
        drawBorderIfNeeded(segments);
        drawChildren(color, segments, childrenInZOrder, boundsFixed);
    }

    private static Color shadowColor = new Colors.FGBGColors(Colors.IBla, Colors.On_Bla);
    private static char  shadowChar = '░';
    
    protected void drawChildren(Color color, IScreenLineSegmentsBuffer destinationSegments, List<ComponentWithOffset> children, boolean boundsFixed) {

        Rectangle clippedArea = bounds;

        // loop thru children and draw each
        Position offset = Position.ZERO;
        if (this.hasBorder()) {
            offset = offset.offsetToRowCol(1, 1);
            if (boundsFixed) {
                clippedArea = new Rectangle.Simple(bounds.getRow()+1, bounds.getCol()+1, bounds.getHeight()-2, bounds.getWidth()-2);
            }
        }

        for (ComponentWithOffset child : children) {
            
            if (child.visible == false) {
                continue;
            }

            Position childOffset = child.offset.offsetAdd(offset);

            if (boundsFixed) {
                ScreenLineSegment.UTIL.addSegmentsWithOffsetBounded(childOffset, clippedArea, destinationSegments.getSegments(),
                                                                    child.component.getSegmentsBuffer().getSegments());
                addDropShadowIfNeeded(childOffset, child, clippedArea, destinationSegments);
            } else {
                ScreenLineSegment.UTIL.addSegmentsWithOffsetUnBounded(childOffset, destinationSegments.getSegments(),
                                                                      child.component.getSegmentsBuffer().getSegments());
                addDropShadowIfNeeded(childOffset, child, clippedArea, destinationSegments);
            }
        }
    }

    private void addDropShadowIfNeeded(Position childOffset, ComponentWithOffset child, Rectangle clippedArea,
                                       IScreenLineSegmentsBuffer destinationSegments) {
        if (child.hasShadow) {
            // right shadow (clipped)
            ScreenLineSegment.UTIL.fillRectWithChar(shadowColor,
                                                    shadowChar,
                                                    new Clipped(clippedArea,new Rectangle.Simple(childOffset.getRow()+1,
                                                                                                 childOffset.getCol()+child.component.getWidth(),
                                                                                                 child.component.getHeight(), 
                                                                                                 1)),
                                                    destinationSegments.getSegments());
            // bottom shadow (clipped)
            ScreenLineSegment.UTIL.fillRectWithChar(shadowColor,
                                                    shadowChar,
                                                    new Clipped(clippedArea,new Rectangle.Simple(childOffset.getRow()+child.component.getHeight(),
                                                                                                 childOffset.getCol()+1,
                                                                                                 1, 
                                                                                                 child.component.getWidth())),
                                                    destinationSegments.getSegments());
            }
    }

    public Position getPositionRelativeToParent(Component<?> child) {
        if (this.hasBorder()) {
            if (child == this.border) {
                return Position.ZERO;
            }
            // return position of child adjusting for border offset (1,1)
            return findChildOrFail(child, "getPositionRelativeToParent").offset.offsetAdd(1, 1);
        }
        return findChildOrFail(child, "getPositionRelativeToParent").offset;
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {

        if ( ! this.containsPosition(offset)) {
            return null;
        }
        

        if (this.hasBorder()) {
            offset = offset.offsetAdd(-1, -1);
        }

        ListIterator<ComponentWithOffset> li = childrenInZOrder.listIterator(childrenInZOrder.size());
        while (li.hasPrevious()) {
            ComponentWithOffset child = li.previous();
            if (child.visible && child.containsPosition(offset)) {
                if (child.component instanceof GetComponentAtOffset) {
                    Position relativeOffset =  offset.offsetAdd(child.offset.negate());
                    return ((GetComponentAtOffset)child.component).getComponentAtOffset(relativeOffset);
                }
                return child.component;
            }
        }
        return this;
    }

    
    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
        fullrecalcDim();
        this.onResizeService.resize(this.getPreferredRectangleForLayout());
    }

    @Override
    public Rectangle getRectangleForClearAndFill() {
        return bounds;
    }

    public Composite t(int i, int j, String text) {
        this.add(new TextComponent(getColor(), i, j, text));
        return this;
    }

    public Composite t(String text) {
        this.add(new TextComponent(getColor(), text));
        return this;
    }

    public void hilite() {
        if (hasBorder()) {
            this.border.hilite();
        }
    }

    public void unHilite() {
        if (hasBorder()) {
            this.border.unHilite();
        }
    }

    public void removeChildren() {
        for (ComponentWithOffset child : new ArrayList<ComponentWithOffset>(getChildrenComponentsWithOffset())) {
            remove(child.component);
        }
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return getChildrenComponentsWithOffset().stream().map(cO -> cO.component).collect(Collectors.toList());
    }

    public List<? extends Component<?>> getChildrenList() {
        return (List<? extends Component<?>>) getChildren();
    }

}
