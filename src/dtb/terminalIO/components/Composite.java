package dtb.terminalIO.components;

import java.util.ArrayList;
import java.util.List;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.ScreenLineSegment;
import dtb.terminalIO.components.Component.AbstractComponent;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

public class Composite extends AbstractComponent<Composite> implements Rectangle {

    private SimpleBorder border;
    private boolean boundsFixed = false;
    private Rectangle bounds = Rectangle.ZERO;

    private static class ComponentWithOffset implements Rectangle {
	Position offset;
	Component<?> component;
	Rectangle bounds;

	public ComponentWithOffset(Position offset, Component<?> component) {
	    super();
	    this.offset = offset;
	    this.component = component;
	    this.bounds = new Rectangle.Simple(offset.getRow() + component.getRow(), offset.getCol()
		    + component.getCol(), component.getHeight(), component.getWidth());
	}

	public ComponentWithOffset(Component<?> component) {
	    this(Position.ZERO, component);
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
	    this.bounds = new Rectangle.Simple(offset.getRow() + component.getRow(), offset.getCol()
		    + component.getCol(), component.getHeight(), component.getWidth());
	}

    }

    private List<ComponentWithOffset> children = new ArrayList<ComponentWithOffset>();

    public Composite(Color color) {
	super(color);
    }

    public Composite() {
	this(FGBGColors.NO_COLOR_SPECIFIED);
    }
    
    public Composite setBounds(Rectangle bounds) {
	this.bounds = bounds;
	this.boundsFixed = true;
	rebuildBorder(this.bounds);
	return this;
    }

    public Composite setBounds(int row, int col, int height, int width) {
	this.bounds = new Rectangle.Simple(row, col, height, width);
	this.boundsFixed = true;
	rebuildBorder(this.bounds);
	return this;
    }

    public Composite setBounds(int height, int width) {
	this.bounds = new Rectangle.Simple(new Dimension.Simple(height, width));
	this.boundsFixed = true;
	rebuildBorder(this.bounds);
	return this;
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

    private void fullrecalcDim() {
	fullrecalcDim(hasBorder());
    }

    private void fullrecalcDim(boolean includeBorder) {

	if (boundsFixed) {
	    return;
	}

	Rectangle calculatedBounds = Rectangle.UTIL.calcDimensionsWith(children);
	if (includeBorder) {
	    rebuildBorder(calcBorderSize(calculatedBounds));
	    bounds = new Rectangle.Simple(this.border);
	    return;
	}
	bounds = calculatedBounds;
    }

    // protected void moveChildrenDelta(Position delta) {
    // moveChildrenDelta(delta.getRow(), delta.getCol());
    // }
    // protected void moveChildrenDelta(int rowOffset, int colOffset) {
    // for (BoundedComponent<?> child : children) {
    // child.moveDelta(rowOffset, colOffset);
    // }
    // }

    protected boolean hasBorder() {
	return border != null;
    }

    protected void rebuildBorder(Rectangle borderSize) {
	Color tColor = border != null ? border.getTitleColor() : this.getColor();
	Color bColor = border != null ? border.getColor() : this.getColor();
	String title = border != null ? border.getTitle() : null;
	this.border = new SimpleBorder(bColor, borderSize);
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
	this.border.setColor(borderColors);
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

    public Composite add(Component<?> component) {
	children.add(new ComponentWithOffset(component));
	fullrecalcDim();
	return this;
    }

    private ComponentWithOffset findChild(Component<?> component) {
	for (ComponentWithOffset child : children) {
	    if (component == child.component) {
		return child;
	    }
	}
	return null;
    }

    public Composite moveChildTo(Position offset, Component<?> component) {
	ComponentWithOffset found = findChild(component);
	if (found == null)
	    throw new IllegalArgumentException("component is not a child of composite");
	found.offset = offset;
	return this;
    }

    public Composite add(Position offset, Component<?> component) {
	children.add(new ComponentWithOffset(offset, component));
	fullrecalcDim();
	return this;
    }

    public Composite add(int offsetRow, int offsetCol, Component<?> component) {
	return add(new Position.Simple(offsetRow, offsetCol), component);
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
	for (ComponentWithOffset child : children) {
	    child.component.clear();
	}
    }
    
    public void markAsDirty() {
	super.markAsDirty();
	for (ComponentWithOffset child : children) {
	    child.component.markAsDirty();
	}
    }
    

    protected void drawBorderIfNeeded(IScreenLineSegmentsBuffer segments) {
	if (hasBorder()) {
	    segments.merge(this.border.getSegmentsBuffer());
	}
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
	// loop thru children and draw

	Position offset = Position.ZERO;
	if (this.hasBorder()) {
	    offset = offset.offsetTo(1, 1);
	}

	for (ComponentWithOffset child : children) {

	    Position childOffset = child.offset.offsetAdd(offset);

	    if (boundsFixed) {
		ScreenLineSegment.UTIL.addSegmentsWithOffsetBounded(childOffset, bounds, segments.getSegments(),
			child.component.getSegmentsBuffer().getSegments());
	    } else {
		ScreenLineSegment.UTIL.addSegmentsWithOffsetUnBounded(childOffset, segments.getSegments(),
			child.component.getSegmentsBuffer().getSegments());
	    }

	    // segments.merge(child.getSegmentsBuffer());
	}

	drawBorderIfNeeded(segments);

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

}
