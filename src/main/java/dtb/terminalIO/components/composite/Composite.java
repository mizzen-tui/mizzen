package dtb.terminalIO.components.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dtb.terminalIO.components.AbstractComponent;
import dtb.terminalIO.components.ChildComponentManager;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.GetComponentAtOffset;
import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.HasTitleAndBorder;
import dtb.terminalIO.components.MultiCharComponent;
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
import dtb.terminalIO.textsegments.SegmentsViewUtils;
import dtb.terminalIO.util.ElapsedTime;
import dtb.terminalIO.util.Logging;

/**
 * A component with child components
 * 
 * This is easily the most complex of components.
 * 
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
    private Map<ComponentWithOffset, CanResize> childToResizeRule;

    protected static class ComponentWithOffset implements Rectangle {
        private Position offset;
        private Component<?> component;
        private Rectangle bounds;
        private Rectangle rectangleForLayout; // used only by layout managers ( Horizontal/VerticalPanels )
        private boolean hasShadow;
        private boolean visible = true;
        ComponentWithOffset simpleComponentWithOffset;

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

        void clearPrecomputedOffset() {
            if (simpleComponentWithOffset != null) {
                simpleComponentWithOffset = null;
            }
        }

        ComponentWithOffset getComponentWithPrecomputedOffset() {
            if (simpleComponentWithOffset != null) {
                return simpleComponentWithOffset;
            }
            if (this.offset == null || this.offset instanceof Position.Simple) {
                return this;
            }
            ComponentWithOffset simpleMe = new ComponentWithOffset(new Position.Simple(offset), component);
            simpleMe.hasShadow = hasShadow;
            simpleMe.visible = visible;
            simpleComponentWithOffset=  simpleMe;
            return simpleComponentWithOffset;
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

        public String toString() {
            return "ComponentWithOffset:[row=" + getRow() + ", col=" + getCol()  + ", height=" + getHeight() + ", width=" + getWidth() + "][component="+(HasName.UTIL.getName(component))+"]";
        }

    }

    public Composite(Color color) {
        super(color);
        this.setFillOnClear(false);
    }

    public Composite(Color color, Dimension bounds) {
        this(color);
        this.setBounds(bounds);
    }

    public Composite() {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED);
    }

    public Composite(Dimension bounds) {
        this(Colors.FGBGColors.NO_COLOR_SPECIFIED, bounds);
    }

    private boolean clearComputedChildPositions_REQUEST_FLAG = false;
    
    private void clearComputedChildPositions() {
        clearComputedChildPositions_REQUEST_FLAG = true;
    }

    private void clearComputedChildPositionsIfNeeded() {
        if (!clearComputedChildPositions_REQUEST_FLAG) {
            return;
        }
        clearComputedChildPositions_REQUEST_FLAG = false;
        this.children.stream().forEach(c-> c.clearPrecomputedOffset());
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
                // why am I doing this?
                 //this.boundsFixed = isFixed;
            }
            markAsDirty();
            clearComputedChildPositions();
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
    
    /**
     * Run the resize rules once, at the current sizes: this Composite's, then those of each Composite
     * inside it, all the way down - whether or not a size changed. A resize skips a panel whose size
     * did not change, rules and all, so a rule added after its panel was sized would otherwise wait
     * for the next terminal resize. For setting up; a resize runs the rules as it goes.
     */
    public void applyResizeRules() {
        this.onResizeService.resize(this.getPreferredRectangleForLayout());
        for (ComponentWithOffset child : children) {
            if (child.component instanceof Composite) {
                ((Composite) child.component).applyResizeRules();
            }
        }
    }
    
    public Rectangle getInnerRectangle() {
        return new Rectangle() {

            @Override
            public int getRow() {
                return Composite.this.getRow() + (hasBorder()?1:0) ;
            }

            @Override
            public int getCol() {
                return Composite.this.getCol() + (hasBorder()?1:0) ;
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
        CanResize childResizeRule = percent>0 
                ? new  ExpandDownByPercent(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component, percent)
                : new  ExpandDown(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component);

        addChildResizeRule(child, childResizeRule);
        
    }

    private void addChildResizeRule(ComponentWithOffset child, CanResize childResizeRule) {
        removeChildResizeRule(child); // remove existing and add new
        if (this.childToResizeRule == null) {
            this.childToResizeRule = new HashMap<ComponentWithOffset, CanResize>();
        }
        this.childToResizeRule.put(child, childResizeRule);
        appendOnResizeRule(childResizeRule);
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

        CanResize childResizeRule = percent>0 
                                            ? new  ExpandRightByPercent(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component, percent)
                                            : new  ExpandRight(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component);

        addChildResizeRule(child, childResizeRule);
    }

    public void onResizeExpandDownAndRight(Component<?> component) {
        if (component == null) {
            throw new NullPointerException("Component onResizeExpandDownAndRight failure: Null component!");
        }
        ComponentWithOffset child = findChildOrFail(component,"onResizeExpandDownAndRight");

        if ( ! (child.component instanceof CanResize)) {
            throw new IllegalArgumentException("child component ("+HasName.UTIL.getName(child.component)+") does not implement "+CanResize.class.getSimpleName());
        }

        ExpandDownAndRight childResizeRule = new ExpandDownAndRight(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component);
        addChildResizeRule(child, childResizeRule);
    }

    public void onResizeExpandDownAndRight(Component<?> component, int percentDown, int percentRight) {
        if (component == null) {
            throw new NullPointerException("Component onResizeExpandDownAndRight failure: Null component!");
        }
        ComponentWithOffset child = findChildOrFail(component,"onResizeExpandDownAndRight");

        if ( ! (child.component instanceof CanResize)) {
            throw new IllegalArgumentException("child component ("+HasName.UTIL.getName(child.component)+") does not implement "+CanResize.class.getSimpleName());
        }
        
        CanResize childResizeRightRule = percentRight>0 
                ? new  ExpandRightByPercent(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component, percentRight)
                : new  ExpandRight(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component);

        
        CanResize childResizeDownRule = percentDown>0 
                ? new  ExpandDownByPercent(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component, percentDown)
                : new  ExpandDown(getInnerRectangle(), Position.ZERO,  child, (CanResize)child.component);

                
        CanResize childResizeDownAndRightByPercentRule = new CanResize.Chain(childResizeRightRule, childResizeDownRule);
                
        addChildResizeRule(child, childResizeDownAndRightByPercentRule);

    }


    public Composite appendOnResize(Consumer<Dimension> onResize) {
        return appendOnResizeRule(CanResize.UTIL.resizableFromService(onResize));
    }

    public Composite appendOnResizeRule(CanResize resizableRule) {
        if (this.onResizeService == null) {
            setOnResizeRule(resizableRule);
            return this;
        }
        
        // THIS IS NOT GOOD..  WHEN child components removed, onResize rules point to
        //                     orphans causing memory leak and undefined behaviour
        //                     need to be able to remove rules when child removed!
        
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
    
    /** 
     * Adding components to a composite can get expensive as the number
     * of child components grows.  
     * To alleviate this, you can call
     * <pre> 
     *   setBulkAdd(true) 
     *   -- this disables fullrecalcDim() <- this can be expensive 
     *   -- then add a lot of components
     *   -- then
     *   setBulkAdd(false)
     *   -- this re-enables fullrecalcDim()
     * </pre>  
     * @param b
     */
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
            return;         // then don't recalculate - because expensive
        }

        List<ComponentWithOffset> visibleChildren = getVisibleChildrenComponentsWithOffset();

        for (ComponentWithOffset child : visibleChildren) {
            if (child.component instanceof Composite) {
                ((Composite) child.component).fullrecalcDim();
            }
        }

        Rectangle calculatedBounds = Rectangle.UTIL.calcDimensionsWith(visibleChildren).mergeAndExpandDimensionsWith( Rectangle.UTIL.calcDimensionsFromPositions(this.getPostion_TopLeft()) );
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
        Color sbColor = border != null ? border.getColor() : this.getColor();
        Color bColor = border != null ? border.getBorderColor() : this.getColor();
        String title = border != null ? border.getTitle() : null;
        this.border = new SimpleBorder(sbColor, borderSize);
        this.border.setBorderColor(bColor);
        this.border.setParentContainer(this);
        this.border.setTitle(tColor, title);
    }

    public Composite markBorderAsDirty() {
        if (this.border != null) {
            this.border.markAsDirty();
        }
        return this;
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
        clearComputedChildPositions();
        children.add(componentWithOffset);
        childrenInZOrder.add(componentWithOffset);
    }

    private void addChild(int index, ComponentWithOffset componentWithOffset) {
        clearComputedChildPositions();
        children.add(index, componentWithOffset);
        childrenInZOrder.add(componentWithOffset);
    }

    private void removeChild(ComponentWithOffset componentWithOffset) {
        clearComputedChildPositions();
        removeChildResizeRule(componentWithOffset);
        children.remove(componentWithOffset);
        childrenInZOrder.remove(componentWithOffset);
    }
    
    private void removeChildResizeRule(ComponentWithOffset componentWithOffset) {
        if (this.onResizeService == null || 
            this.onResizeService == CanResize.NOOP || 
            this.childToResizeRule == null || 
            this.childToResizeRule.get(componentWithOffset) == null) {
            return;
        }
        CanResize resizeChildRule = this.childToResizeRule.get(componentWithOffset);
        if (resizeChildRule == this.onResizeService) {
            this.onResizeService = CanResize.NOOP;
            return;
        }
        if (this.onResizeService instanceof CanResize.Chain) {
            ((CanResize.Chain)this.onResizeService).remove(resizeChildRule);
        }
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
            throw new NullPointerException("Component childAsRectangleOffset failure: Null component!");
        }
        ComponentWithOffset child = findChildOrFail(component,"childAsRectangleOffset");
        
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
        clearComputedChildPositions();
        return this;
    }

    /** is child shown (not hidden with hide())? false if it is not a child of this composite */
    public boolean isShown(Component<?> child) {
        for (ComponentWithOffset c : getChildrenComponentsWithOffset()) {
            if (c.component == child) {
                return c.visible;
            }
        }
        return false;
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
        clearComputedChildPositions();
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
        clearComputedChildPositions();
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
        clearComputedChildPositions();
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
                    (errorPrefix == null ? "" : errorPrefix + ": ") + "Could not find component! : "  + HasName.UTIL.getName(component) );
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

    public Composite moveChildTo(int row, int col, Component<?> component) {
        return moveChildTo(new Position.Simple(row, col), component);
    }

    public Composite moveChildTo(Position offset, Component<?> component) {
        ComponentWithOffset found = findChildOrFail(component, "moveChildTo");
        found.offset = offset;
        fullrecalcDim();
        markAsDirty();
        clearComputedChildPositions();
        return this;
    }

    public Composite moveChildDelta(Position offset, Component<?> component) {
        ComponentWithOffset found = findChildOrFail(component, "moveChildDelta");
        found.offset = found.offset.offsetAdd(offset);
        fullrecalcDim();
        markAsDirty();
        clearComputedChildPositions();
        return this;
    }

    @Override
    public boolean moveChildToPosition(Position position, Component<?> child) {
        if (this.hasBorder()) {
            position = position.offsetAdd(Position.DOWN_BY_1_AND_RIGHT_BY_1.negate());
        }
        moveChildTo(position, child);
        return true;
    }

    @Override
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

        Rectangle calculatedBounds = Rectangle.UTIL.calcDimensionsWith(children).mergeAndExpandDimensionsWith( Rectangle.UTIL.calcDimensionsFromPositions(this.getPostion_TopLeft()) );

        int bottomMost = calculatedBounds.getBottomMostRow();
        return this.add(bottomMost + 1, this.getPostion_TopLeft().getCol(), component);

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
        if (delta.getRow() != 0 || delta.getCol() != 0) {
            markAsDirty();                               // moved: its panels redraw, clearing where it was
        }
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

    public static boolean DRAW_VIA_PARTS = true; //false;
    //public static boolean DRAW_VIA_PARTS = false;
    
    protected void clearBeforeDraw() {
       // if (!DRAW_VIA_PARTS) {
            super.clearBeforeDraw();
//            return;
//        }
        
    }

    protected void drawBorderIfNeeded(IScreenLineSegmentsBuffer segments) {
        if (hasBorder()) {
            if (segments.isEmpty()) {
               // fast!
               segments.getSegments().addAll(this.border.getSegmentsBuffer().getSegments());
            } else {
                //if (!DRAW_VIA_PARTS || this.border.isDirty()) {
                   IScreenLineSegmentsBuffer borderSegments = this.border.getSegmentsBuffer();
                   segments.merge(borderSegments);
               // }
            }
        }
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        draw(color, segments, this.boundsFixed);
    }


    protected void draw(Color color, IScreenLineSegmentsBuffer segments, boolean boundsFixedFlag) {
        drawBorderIfNeeded(segments);
        if (DRAW_VIA_PARTS /*&& children != null && children.size()>1*/) {
            clearComputedChildPositionsIfNeeded();
           DRAW_PARTS_UTIL.drawParts(this, segments.getSegments(), boundsFixedFlag);
        } else {
           drawChildren(color, segments, childrenInZOrder, boundsFixedFlag);
        }
    }

    private static Color shadowColor = new Colors.FGBGColors(Colors.IBla, Colors.On_Bla);
    private static char  shadowChar = '░';
    //private static char  shadowChar = ' ';

    protected void drawChildren(Color color, IScreenLineSegmentsBuffer destinationSegments, List<ComponentWithOffset> children, boolean boundsFixedFlag) {

        Rectangle clippedArea = bounds;

        Position offset = Position.ZERO;
        if (this.hasBorder()) {
            offset = offset.offsetToRowCol(1, 1);
            clippedArea = new Rectangle.Simple(bounds.getRow()+1, bounds.getCol()+1, bounds.getHeight()-2, bounds.getWidth()-2);   // children stay inside the border
        }

        // loop thru children and draw each
        for (ComponentWithOffset child : children) {
            
            if (child.visible == false) {
                continue;
            }

            Position childOffset = child.offset.offsetAdd(offset);

            if (boundsFixedFlag || this.hasBorder()) {
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
        clearComputedChildPositions();
    }

    @Override
    public Rectangle getRectangleForClearAndFill() {
        return bounds;
    }

    public Composite t(int i, int j, String text) {
        this.add(new TextComponent(getColor(), i, j, text));
        return this;
    }

    /**
     * Add text to panel
     * @return
     */
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
    
    @FunctionalInterface
    static interface DrawChildTask {
        void draw(ComponentWithOffset child);
    }

    static class DRAW_PARTS_UTIL {

        static boolean suppressSTATS=true;

        /**
         * @return given a child, returns its bottom and right shadows as a List of 2 ComponentWithOffset objects
         */
        static List<ComponentWithOffset> dropShadowRectangles(Rectangle child) {
            List<ComponentWithOffset> shadows = new ArrayList<ComponentWithOffset>(2);

            Rectangle bottom = new Rectangle.Simple(child.getBottomMostRow()+1, child.getCol()+1, 1, child.getWidth());
            Rectangle right  = new Rectangle.Simple(child.getRow()+1, child.getRightMostCol()+1, child.getHeight(), 1);

            shadows.add( new ComponentWithOffset(Position.ZERO, new MultiCharComponent(shadowColor, shadowChar, bottom)));
            shadows.add( new ComponentWithOffset(Position.ZERO, new MultiCharComponent(shadowColor, shadowChar, right)));
            return shadows;
        }

        static void drawParts(Composite parent, TreeSet<ScreenLineSegment> destination, boolean boundsFixedFlag) {

            Rectangle clippedArea = parent.bounds;

            // loop thru children and draw each
            Position offsetForBorder;
            if (parent.hasBorder()) {
                offsetForBorder = Position.ZERO.offsetToRowCol(1, 1);
                // children stay inside the border - fixed size or growing: a child placed relative to
                // the composite itself (e.g. top-right) can reach the border even when it grows
                clippedArea = new Rectangle.Simple(clippedArea.getRow()+1, clippedArea.getCol()+1, clippedArea.getHeight()-2, clippedArea.getWidth()-2);
            } else {
                offsetForBorder = Position.ZERO;
            }
            Rectangle boundingRectangle = clippedArea;

            ElapsedTime el = ElapsedTime.start();
            List<ComponentWithOffset> reverseZOrderChildren = new ArrayList<ComponentWithOffset>(parent.childrenInZOrder).stream()
                                                                                                               .filter(c->c.visible && c.getWidth()>0 && c.getHeight()>0)
                                                                                                               .map(c->c.getComponentWithPrecomputedOffset())
                                                                                                               .flatMap(c-> c.hasShadow ? Stream.concat(Stream.of(c), dropShadowRectangles(c).stream()) 
                                                                                                                                        : Stream.of(c))
                                                                                                               .collect(Collectors.toList());

            if (reverseZOrderChildren.size()==0) {
                return ;
            }

            Component.UTIL.assertSegmentsIntegrity(parent.bounds, destination);

            Collections.reverse(reverseZOrderChildren);
            Collection<RectangleAndSubRectangles> recs = Rectangle.UTIL.differences(reverseZOrderChildren);

            int count = recs.stream().mapToInt(r -> 1 + (r.getSubs()==null?0 : r.getSubs().size())).sum();

      //      Logging.info(String.format("Parts %-20s : %-20s  #squares %4d  elapsed  %4d", "differences", StringUtils.truncate(HasName.UTIL.getName(parent),20), count,el.elapsed()));

            
            DrawChildTask drawFullChildComponent = (childToDraw) -> {
                               //noOverLapsCheck.assertTest("testing child before draw:"+String.valueOf(childToDraw), childToDraw.component.getSegmentsBuffer().getSegments());
                               Component.UTIL.assertComponentSegmentsIntegrity(childToDraw.component);
                               Rectangle boundsForChild =  new Rectangle.Simple(((Position)childToDraw).offsetAdd(offsetForBorder),((Dimension)childToDraw) ).intersectWith(boundingRectangle);
                               if (boundsForChild == null) { // null when intersectWith() detects height or width of boundingRectangle < 1
                                   return ;
                               }

                               ElapsedTime drawChildTime = new ElapsedTime();

                               // a draw means copy childToDraw segments to destination
                               
                               try {
                                   childToDraw.component.getSegmentsBuffer().getSegments().stream()
                                                        .map(s->s.fromOffset(childToDraw.offset.offsetAdd(offsetForBorder)))
                                                        .map(s-> ScreenLineSegment.UTIL.sliceIfNeededToFitRect(s, boundsForChild))
                                                        .filter(s-> s!=null)
                                                        .forEach(s->destination.add(s));
                               } catch (ConcurrentModificationException ce) {
                                   Logging.info("ConcurrentModificationException "+childToDraw.component.getClass().getName());
                                   throw ce;
                               }
                               

//                               Logging.info(String.format("Parts-child %-20s : child: %-20s  #segments %4d  elapsed  %4d",
//                                                          StringUtils.truncate(HasName.UTIL.getName(parent),20),
//                                                          StringUtils.truncate(HasName.UTIL.getName(childToDraw.component),20),
//                                                          childToDraw.component.getSegmentsBuffer().getSegments().size(),
//                                                          drawChildTime.elapsed()));

            };

            // do the first (on top) child
            drawFullChildComponent.draw(reverseZOrderChildren.get(0));
            Component.UTIL.assertSegmentsIntegrity(parent.bounds, destination);

            for (RectangleAndSubRectangles rec : recs) {
                if (rec.isFullyHidden()) {
                    continue;
                }
                ComponentWithOffset child = (ComponentWithOffset) rec.getRectangle();
                if (rec.isNotObscured()) {
                    drawFullChildComponent.draw(child);
                    Component.UTIL.assertSegmentsIntegrity(parent.bounds, destination);
                } else {
                    int rowDelta =  offsetForBorder.getRow();
                    int colDelta =  offsetForBorder.getCol();
                    List<Rectangle> srcPartsNormalized = rec.getSubs().stream().map(r -> new Rectangle.Simple(r.getRow()+rowDelta, r.getCol()+colDelta, r.getHeight(), r.getWidth())).collect(Collectors.toList());
                    SegmentsViewUtils.copyComponentParts(destination, boundingRectangle, child.component, srcPartsNormalized, child.offset.offsetAdd(offsetForBorder));
                    Component.UTIL.assertSegmentsIntegrity(parent.bounds, destination);
                }
            }

         //   Logging.info(String.format("%-20s : %-20s  #kids %4d  elapsed  %4d", "drawViaParts", StringUtils.truncate(HasName.UTIL.getName(parent),20), reverseZOrderChildren.size(),el.recalcElapsed()));

            Component.UTIL.assertSegmentsIntegrity(parent.bounds, destination);

        }

    }

}
