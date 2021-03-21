package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Align;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;


/**
 * A Component that draws a border around itself and then has an inner BoundedComponent to
 * render its contents
 */
public class BorderedComponent extends BoundedComponent<BorderedComponent> 
                               implements Rectangle, 
                                          HasTitleAndBorder<BorderedComponent>, 
                                          CanResize, 
                                          ParentContainer, 
                                          GetComponentAtOffset {

    private SimpleBorder      border;
    private Component<?>      component;
    private Position          componentPosition;
    private boolean           showBorder;
    
    public BorderedComponent(Color color, Component<?> component,  ParentContainer parentContainer) {
        super(color, Rectangle.ZERO, parentContainer);
        this.component = component;
        this.component.setParentContainer(this);
        this.componentPosition = new Position.Simple(1,1);
        Position.Simple p = new Position.Simple(component);
        Rectangle bounds = new Rectangle.Simple(p, new Dimension.Simple(component.getHeight() + 2, component.getWidth() + 2));
        this.border = new SimpleBorder(color, bounds);
        this.border.setParentContainer(this);
        showBorder = true;
        this.setRectangle( bounds );
        this.setFillOnClear(false);
    }
    
    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == this.border) {
                return Position.ZERO;
        }
        return componentPosition;
    }
    
    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        if ( ! this.containsPosition(offset)) {
            return null;
        }

        offset = offset.offsetAdd(componentPosition.negate());
        if (this.component.containsPosition(offset)) {
            if (this.component instanceof GetComponentAtOffset) {
                return ((GetComponentAtOffset)component).getComponentAtOffset(offset);
            }
            return this.component;
        }
        return this;
    }
    
    @Override
    public void childResized(Component<?> child) {
        if (child == this.border) {
            if (this.border.isSameDimension(this)) {
                return ;
            }
            super.resize( (Dimension)this.border );
            return;
        }
        Dimension newSize = new Dimension.Simple(child.getHeight()+2, child.getWidth()+2);
        if (newSize.isSameDimension(this)) {
            return ; //  avoid infinite recursion
        }
        // adjust this container to be big enough for its child
        super.resize(newSize);
        this.border.resize(newSize);
    }
    
    public BorderedComponent(Color color, Component<?> component) {
        this(color, component, (ParentContainer)null);
    }

    public BorderedComponent(Component<?> component, ParentContainer parentContainer) {
        this(component.getColor(), component, parentContainer);
    }
    

    public BorderedComponent(Component<?> component) {
        this(component, (ParentContainer)null);
    }
    
    public BorderedComponent(String text, ParentContainer parentContainer) {
        this(new TextComponent(text), parentContainer);
    }

    public BorderedComponent(String text) {
        this(text, null);
    }
    
    public Component<?>  getComponent() {
        return this.component;
    }

    public BorderedComponent  setComponent(Component<?> component) {
        if (this.component != null) {
            this.component.setParentContainer(null);
        }
        this.component = component;
        this.component.setParentContainer(this);
        this.componentPosition = new Position.Simple(1,1);
        Position.Simple p = new Position.Simple(component);
        Rectangle bounds = new Rectangle.Simple(p, new Dimension.Simple(component.getHeight() + 2, component.getWidth() + 2));
        this.border = new SimpleBorder(getColor(), bounds);
        this.border.setParentContainer(this);
        showBorder = true;
        this.setRectangle( bounds );
        this.setFillOnClear(false);
        return this;
    }

    public void setShowBorder(boolean showBorder) {
        this.showBorder = showBorder;
        this.markAsDirty();
    }
    

    
    public boolean visibleBorder() {
        return this.showBorder;
    }
    
    public void markAsDirty() {
        super.markAsDirty();
        
        if (this.component != null) this.component.markAsDirty();
        
        if (this.border != null) this.border.markAsDirty();
    }
    
    
    public void resize(Dimension d) {
        if (d.isSameDimension(this)) {
            return;
        }
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
        if (componentPosition.isSamePosition(Position.ZERO)) {
            // very fast merge (not this case probably does not come up)
            segments.getSegments().addAll(this.component.getSegmentsBuffer().getSegments());
        } else {
            // (SLOW)segments.mergeAt(componentPosition.getRow(), componentPosition.getCol(), this.component.getSegmentsBuffer());
            segments.getSegments().addAll( this.component.getSegmentsBuffer().getSegments().stream().map((s)->s.fromOffset(componentPosition)).collect(Collectors.toList())  );
        }
        if (showBorder) {
            // because we know the border does not overlap we can leverage the fast add from treeSet rather than the slow merge
            // from segments.merge() 
            // (SLOW) segments.merge(this.border.getSegmentsBuffer());
           segments.getSegments().addAll(this.border.getSegmentsBuffer().getSegments());
        }
    }

    public String getTitle() {
        return border.getTitle();
    }

    public BorderedComponent setTitle(Color titleColor, String title, Align titleAlign) {
        border.setTitle(titleColor, title, titleAlign);
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

    public boolean hasBorder() {
        // see setBorder() below for reason for hard-coded true value
        return true;
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

    public Color getBorderColor() {
        return border.getBorderColor();
    }

    @Override
    public BorderedComponent setTitle(Color titleColors, String title) {
        this.border.setTitle(titleColors, title);
        return this;
    }

    @Override
    public void hilite() {
        border.hilite();
        if ( this.component instanceof Hiliter)
            ((Hiliter)this.component).hilite();
    }

    @Override
    public void unHilite() {
        border.unHilite();
        if ( this.component instanceof Hiliter)
            ((Hiliter)this.component).unHilite();
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        // TODO:  do I need to include the border as a component - maybe yes ( see how it is used in the dirty method)
        return Collections.singleton( this.component );
    }

    
}
