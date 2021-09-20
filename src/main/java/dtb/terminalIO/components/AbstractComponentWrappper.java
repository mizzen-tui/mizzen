package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.geom.position.Position;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public class AbstractComponentWrappper<T extends Component<?>>  extends  AbstractComponent<AbstractComponentWrappper<T>> 
                      implements ParentContainer, 
                                 CanResize, 
                                 GetComponentAtOffset,
                                 HasName<AbstractComponentWrappper<T>> {

    private T wrappedComponent;
    private String name;

    protected AbstractComponentWrappper(Color color, T wrappedComponent) {
        super(color);
        setWrappedComponent(wrappedComponent);
    }

    public T getWrappedComponent() {
        return wrappedComponent;
    }

    protected void setWrappedComponent(T wrappedComponent) {
        if (this.wrappedComponent != null) {
            this.wrappedComponent.setParentContainer(null);
        }
        this.wrappedComponent = wrappedComponent;
        if (wrappedComponent != null) {
            this.wrappedComponent.setParentContainer(this);
            HasName.UTIL.decorateWithParentsNamePrefix(this.wrappedComponent, this);
        }
    }

    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singletonList(getWrappedComponent());
    }

    @Override
    public int getRow() {
        return getWrappedComponent().getRow();
    }

    @Override
    public int getCol() {
        return getWrappedComponent().getCol();
    }

    @Override
    public int getWidth() {
        return getWrappedComponent().getWidth();
    }

    @Override
    public int getHeight() {
        return getWrappedComponent().getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        getWrappedComponent().moveTo(row, col);
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        return Position.ZERO;
    }

    public void markAsDirty() {
        if (!isDirty()) {
            if (!this.getWrappedComponent().isDirty()) {
                this.getWrappedComponent().markAsDirty();
            }
            if (!isDirty()) { // child might already have marked this as dirty
                super.markAsDirty();
            }
        }
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        // this next call is basically just a fast ptr assignment - so very quick and no added memory requirements
        segments.clearAndMerge(getWrappedComponent().getSegmentsBuffer().getSegments());
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return getWrappedComponent();
    }

    @Override
    public void resize(Dimension d) {
        
        if (this.isSameDimension(d)) {
            return ;
        }
        
        T child = getWrappedComponent();
        if (child instanceof CanResize) {
            ((CanResize) child).resize(d);
        }
        if (this.getParentContainer() != null) {
            this.getParentContainer().childResized(this);
        }
        
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        if ( ! this.containsPosition(offset)) {
            return null;
        }

        if (this.getWrappedComponent() instanceof GetComponentAtOffset) {
            return ((GetComponentAtOffset)this.getWrappedComponent() ).getComponentAtOffset(offset);
        }
        return this;
    }

    @Override
    public String getName() {
        if (this.name == null) {
            String name = this.getClass().getSimpleName()+"[wrapped:"+HasName.UTIL.getName(this.getWrappedComponent())+"]";
            this.setName(name);
        }
        return name;
    }

    @Override
    public AbstractComponentWrappper<T> setName(String name) {
        this.name = name;
        return this;
    }
    
    
}
