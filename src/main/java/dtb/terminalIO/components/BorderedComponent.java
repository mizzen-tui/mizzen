package dtb.terminalIO.components;

import dtb.terminalIO.ControlCode.Color;
import dtb.terminalIO.ControlCode.FGBGColors;
import dtb.terminalIO.IScreenLineSegmentsBuffer;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;

// todo dropshadow component

/**
 * A Component that draws a border around itself and then has an inner BoundedComponent to
 * render its contents
 */
public class BorderedComponent extends BoundedComponent<BorderedComponent> 
        implements Rectangle, HasTitleAndBorder<BorderedComponent>, CanResize {

    private SimpleBorder        border;
    private Component<?> component;
    private Position            componentPosition;

    public BorderedComponent(Color color, Component<?> component) {
        super(color, Rectangle.ZERO);
        this.component = component;
        this.componentPosition = new Position.Simple(1,1);
        //component.moveDelta(1,1);
        Position.Simple p = new Position.Simple(component);
        //p.moveDelta(-1, -1);
        Rectangle bounds = new Rectangle.Simple(p, new Dimension.Simple(component.getHeight() + 2, component.getWidth() + 2));
        this.border = new SimpleBorder(color, bounds);
        this.setRectangle( bounds );
    }

    public BorderedComponent(Component<?> component) {
        this(component.getColor(), component);
    }
    
    public Component<?>  getComponent() {
        return this.component;
    }

    public void markAsDirty() {
        super.markAsDirty();
        this.component.markAsDirty();
        this.border.markAsDirty();
    }
    
    
    public void resize(Dimension d) {
        super.resize(d);
        this.border.resize(d);
        if ( this.component instanceof CanResize)
           ((CanResize)this.component).resize(new Dimension.Simple(d.getHeight()-2, d.getWidth()-2));
    }


    public BorderedComponent setEmptyChar(char emptyChar) {
	BorderedComponent result = super.setEmptyChar(emptyChar);
        border.setEmptyChar(emptyChar);
        return result;
    }
    
    @Override
    public int getCol() {
        return this.border.getCol();
    }

    @Override
    public int getRow() {
        return this.border.getRow();
    }

    @Override
    public int getWidth() {
        return this.border.getWidth();
    }

    @Override
    public int getHeight() {
        return this.border.getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        this.border.moveTo(row, col);
        this.component.moveTo(row + 1, col + 1);
    }

    @Override
    public void moveDelta(int rowOffset, int colOffset) {
        this.border.moveDelta(rowOffset, colOffset);
        this.component.moveDelta(rowOffset, colOffset);
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.mergeAt(componentPosition.getRow(), componentPosition.getCol(), this.component.getSegmentsBuffer());
        segments.merge(this.border.getSegmentsBuffer());
    }

    public String getTitle() {
        return border.getTitle();
    }

    public BorderedComponent setTitle(FGBGColors titleColor, String title) {
        border.setTitle(titleColor, title);
        return this;
    }

    public BorderedComponent setTitle(String title) {
        border.setTitle(title);
        return this;
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return this;
    }

    @Override
    public BorderedComponent setBorder(boolean hasBorder) {
        if (!hasBorder) 
            throw new IllegalStateException("Cannnot turn off border");
        return this;
    }

    @Override
    public BorderedComponent setBorderColor(Color borderColors) {
        this.border.setBorderColor(borderColors);
        return this;
    }

    @Override
    public BorderedComponent setTitle(Color titleColors, String title) {
        this.border.setTitle(titleColors, title);
        return this;
    }

}
