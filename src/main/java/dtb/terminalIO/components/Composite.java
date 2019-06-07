package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import dtb.terminalIO.components.Component.AbstractComponent;
import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.controlcodes.ControlCode.FGBGColors;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;
import dtb.terminalIO.textsegments.ScreenLineSegment;

/**
 * A component with child components
 */
public class Composite extends AbstractComponent<Composite> 
      implements Rectangle, HasTitleAndBorder<Composite>, CanResize, ParentContainer  {

    private SimpleBorder border;
    protected boolean boundsFixed = false;
    private Rectangle bounds = Rectangle.ZERO;
    private List<ComponentWithOffset> children = new ArrayList<ComponentWithOffset>();


    protected static class ComponentWithOffset implements Rectangle {
	Position offset;
	Component<?> component;
	Rectangle bounds;

	public ComponentWithOffset(Position offset, Component<?> component) {
	    super();
	    this.offset = offset;
	    this.setComponent(component);
	}

	public ComponentWithOffset(Component<?> component) {
	    this(Position.ZERO, component);
	}
	
	protected void calcBounds() {
	    if (this.offset == null || this.component == null) {
	        return;
	    }
	    
	    ComponentWithOffset.this.bounds = new Rectangle.Delegate(component) {
	        @Override
	        public int getCol() {
	            return super.getCol()+offset.getCol();
	        }

	        @Override
	        public int getRow() {
	            return super.getRow()+offset.getRow();
	        }
	    };
	    
	}
	
	public void setComponent(Component<?> component) {
            this.component = component;
            calcBounds();
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
//	    this.bounds = new Rectangle.Simple(offset.getRow() + component.getRow(), offset.getCol()
//		    + component.getCol(), component.getHeight(), component.getWidth());
	}
    }

    public Composite(Color color) {
	super(color);
    }

    public Composite() {
	this(FGBGColors.NO_COLOR_SPECIFIED);
    }
    
    public Composite setBounds(Rectangle bounds) {
        if (bounds == null) {
            this.boundsFixed = false;
            fullrecalcDim();
            return this;
        }
	this.bounds = bounds;
	this.boundsFixed = true;
	if (this.border != null) rebuildBorder(this.bounds);
	return this;
    }

    public Composite setBounds(int row, int col, int height, int width) {
	this.bounds = new Rectangle.Simple(row, col, height, width);
	this.boundsFixed = true;
	if (this.border != null) rebuildBorder(this.bounds);
	return this;
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
	if (this.border != null) rebuildBorder(this.bounds);
	markAsDirty();
        if (this.getParentContainer()!=null && ! previousDimension.isSameDimension(this)) {
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

    public void resize(Dimension size) {
	setBounds(size);
	markAsDirty();
    }
    
    
    // todo: not sure what this method means -- what do i
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

    public void fullrecalcDim() {
        Dimension previousDimension = new Dimension.Simple(this);
	fullrecalcDim(hasBorder());
        if (this.getParentContainer()!=null && ! previousDimension.isSameDimension(this)) {
            this.getParentContainer().childResized(this);
        }
    }

    private void fullrecalcDim(boolean includeBorder) {

	if (boundsFixed) {
	    return;
	}

	for ( ComponentWithOffset child : children) {
	    if (child.component instanceof Composite) {
	        ((Composite)child.component).fullrecalcDim();
	    }
	}
	
	
	Rectangle calculatedBounds = Rectangle.UTIL.calcDimensionsWith(children);
	if (includeBorder) {
	    rebuildBorder(calcBorderSize(calculatedBounds));
	    bounds = new Rectangle.Simple(this.border);
	    return;
	}
	bounds = calculatedBounds;
    }

    protected boolean hasBorder() {
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
	return this;
    }

    public Composite setBorderColor(Color borderColors) {
	setBorder(true);
	this.border.setBorderColor(borderColors);
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

    protected ComponentWithOffset addComponentWithOffset(Position offset, Component<?> component) {
        ComponentWithOffset componentWithOffset = new ComponentWithOffset(offset, component);
        children.add(componentWithOffset);
        component.setParentContainer(this);
        return componentWithOffset;
    }
    
    
    public Composite add(Position offset, Component<?> component) {
        ComponentWithOffset child = findChild(component);
        if (child != null) {
            throw new IllegalArgumentException("Component add failure:  Duplicate !");
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

    
    public Composite remove(Component<?> component) {
        ComponentWithOffset child = findChildOrFail(component,"remove");
        children.remove(child);
        child.component.setParentContainer(null);
        fullrecalcDim();
        markAsDirty();
        return this;
    }

    // layering functions
    
    public Composite moveUp(Component<?> component) {
        ComponentWithOffset child = findChildOrFail(component,"moveUp");
        int index = children.indexOf(child);
        if (index == children.size()-1) {
            return this;
        }
        children.remove(child);
        children.add(index+1, child);
        markAsDirty();
        return this;
    }
    
    public Composite moveDown(Component<?> component) {
        ComponentWithOffset child = findChildOrFail(component,"moveDown");
        int index = children.indexOf(child);
        if (index == 0) {
            return this;
        }
        children.remove(child);
        children.add(index-1, child);
        markAsDirty();
        return this;
    }
    
    public Composite bringToFront(Component<?> component) {
        ComponentWithOffset child = findChildOrFail(component,"bringToFront");
        int index = children.indexOf(child);
        if (index == children.size()-1) {
            return this;
        }
        children.remove(child);
        children.add(children.size(), child);
        markAsDirty();
        return this;
    }

    public Composite bringToBack(Component<?> component) {
        ComponentWithOffset child = findChildOrFail(component,"bringToBack");
        int index = children.indexOf(child);
        if (index == 0) {
            return this;
        }
        children.remove(child);
        children.add(0, child);
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

    protected ComponentWithOffset findChildOrFail(Component<?> component) {
        return findChildOrFail(component,null);
    }
    
    protected ComponentWithOffset findChildOrFail(Component<?> component, String errorPrefix) {
        ComponentWithOffset child = findChild(component);
        if (child == null) {
            throw new IllegalArgumentException((errorPrefix==null?"": errorPrefix+": ")+ "Could not find component!");
        }
        return child;
    }
    
    
    public Composite replace(Component<?> c1, Component<?> c2) {
        ComponentWithOffset child = findChildOrFail(c1,"replace");
        if (findChild(c2) != null) {
            throw new IllegalArgumentException("Could not replace component with a component that already exists as a child in composite!");
        }
        child.component.setParentContainer(null);
        child.setComponent(c2);
        child.component.setParentContainer(this);
        fullrecalcDim();
        markAsDirty();
        return this;
    }
    
    public Composite moveChildTo(Position offset, Component<?> component) {
        ComponentWithOffset found = findChildOrFail(component,"moveChildTo");
        found.offset = offset;
        fullrecalcDim();
        markAsDirty();
        return this;
    }

    public Composite moveChildDelta(Position offset, Component<?> component) {
	ComponentWithOffset found = findChildOrFail(component,"moveChildDelta");
	found.offset = found.offset.offsetAdd(offset);
        fullrecalcDim();
        markAsDirty();
	return this;
    }

    public Composite append( Component<?> component) {
        
        if (children.size() ==0) {
            return add(component);
        }
        
        boolean hasBorder = hasBorder();
        
        ComponentWithOffset cp = this.children.stream().max(Position.UTIL.POS_COMPARE).get();
        int rightMost = cp.getRightMostCol();
        if (component.getWidth()+rightMost < this.bounds.getWidth()+(hasBorder?-2:0)) {
            return this.add(cp.getRow(), rightMost+1, component);
        }

        Rectangle calculatedBounds = Rectangle.UTIL.calcDimensionsWith(children);
        
        int bottomMost = calculatedBounds.getBottomMostRow();
        return this.add(bottomMost+1, 0, component);
        
//        
//        int offsetRow = 0;
//        int offsetCol = calculatedBounds.getRightMostCol();
//        if (component.getWidth() + calculatedBounds.getWidth() > this.bounds.getWidth()+(hasBorder?-2:0)) {
//            offsetRow++;
//        } else {
//            offsetCol = calculatedBounds.getWidth();
//        }
//        return add(offsetRow, offsetCol, component); 
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
	Position delta = this.offsetTo(row, col);
	bounds.moveDelta(delta.getRow(), delta.getCol());
	fullrecalcDim();

    }

    public void clear() {
	super.clear();
//	for (ComponentWithOffset child : children) {
//	    child.component.clear();
//	}
    }
    
    public void clearAndRemoveAllChildren() {
        
        List<ComponentWithOffset> copiedChildren = new ArrayList<ComponentWithOffset>(this.children);
        
        for (ComponentWithOffset child : copiedChildren) {
            this.remove( child.component);
        }
        
        this.clear();
    }


    protected void drawBorderIfNeeded(IScreenLineSegmentsBuffer segments) {
	if (hasBorder()) {
	    segments.merge(this.border.getSegmentsBuffer());
	}
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        draw(color, segments, boundsFixed);
    }
    
    protected void draw(Color color, IScreenLineSegmentsBuffer segments, boolean boundsFixed) {
        drawChildren(color, segments, children, boundsFixed);
	drawBorderIfNeeded(segments);
    }

    protected void drawChildren(Color color, IScreenLineSegmentsBuffer segments, List<ComponentWithOffset> children, boolean boundsFixed) {
        // loop thru children and draw
        Position offset = Position.ZERO;
        if (this.hasBorder()) {
            offset = offset.offsetTo(1, 1);
        }

        for (ComponentWithOffset child : children) {

            Position childOffset = child.offset.offsetAdd(offset);

            // Simple [row=20, lcol37, rcol=39 width=3, height=1]
            
            int test=0;
            if (childOffset.getRow() == 20 && childOffset.getCol() == 37) {

                test=1;
            }
            
            
            if (boundsFixed) {
                ScreenLineSegment.UTIL.addSegmentsWithOffsetBounded(childOffset, bounds, segments.getSegments(),
                        child.component.getSegmentsBuffer().getSegments());
            } else {
                ScreenLineSegment.UTIL.addSegmentsWithOffsetUnBounded(childOffset, segments.getSegments(),
                        child.component.getSegmentsBuffer().getSegments());
            }
        }
    }

    public Position getPositionRelativeToParent(Component<?> child) {
        if (this.hasBorder()) {
            if (child == this.border) {
                return Position.ZERO;
            }
            return findChildOrFail(child,"getPositionRelativeToParent").offset.offsetAdd(1,1);
        }
        return findChildOrFail(child,"getPositionRelativeToParent").offset;
    }
    
    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
        fullrecalcDim();
    }
    
    
    @Override
    protected Rectangle getRectangleForClearAndFill() {
	return bounds;
    }

    protected Rectangle getInnerRectangle() {
	if (!hasBorder())
	    return bounds;
	return new Rectangle.Simple(bounds.getRow() + 1, bounds.getCol() + 1, bounds.getHeight() - 2,
		bounds.getWidth() - 2);
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
        return getChildrenComponentsWithOffset().stream().map(cO -> cO.component ).collect( Collectors.toList()  );
    }

    public List<? extends Component<?>> getChildrenList() {
        return (List<? extends Component<?>>) getChildren();
    }


}
