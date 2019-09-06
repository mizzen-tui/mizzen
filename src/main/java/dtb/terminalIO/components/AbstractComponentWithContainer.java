package dtb.terminalIO.components;

import java.util.Collection;
import java.util.Collections;

import dtb.terminalIO.controlcodes.ControlCode.Color;
import dtb.terminalIO.events.mouse.GetComponentAtOffset;
import dtb.terminalIO.geom.Dimension;
import dtb.terminalIO.geom.Position;
import dtb.terminalIO.geom.Rectangle;
import dtb.terminalIO.textsegments.IScreenLineSegmentsBuffer;

public abstract class AbstractComponentWithContainer<T extends Composite>  extends 
                       AbstractComponent<AbstractComponentWithContainer<T>> 
                          implements CanResize, ParentContainer,  GetComponentAtOffset {

    protected AbstractComponentWithContainer(Color color) {
        super(color);
    }

    abstract protected T getContainer();  
    
    @Override
    public Collection<? extends Component<?>> getChildren() {
        return Collections.singleton(getContainer());
    }

    @Override
    public int getCol() {
        return getContainer().getRow();
    }

    @Override
    public int getRow() {
        return getContainer().getRow();
    }

    @Override
    public int getWidth() {
        return getContainer().getWidth();
    }

    @Override
    public int getHeight() {
        return getContainer().getHeight();
    }

    @Override
    public void moveTo(int row, int col) {
        this.getContainer().moveTo(row, col);
    }

    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        Component<?> component = this.getContainer().getComponentAtOffset(offset);
        if (component == this.getContainer()) {
            return this;
        }
        return component;
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == this.getContainer()) {
            return Position.ZERO;
        }
        return null;
    }

    @Override
    public void childResized(Component<?> child) {
        this.markAsDirty();
    }

    @Override
    public void resize(Dimension d) {
        this.getContainer().resize(d);
    }

    @Override
    protected void draw(Color color, IScreenLineSegmentsBuffer segments) {
        segments.merge(this.getContainer().getSegmentsBuffer());
    }

    @Override
    protected Rectangle getRectangleForClearAndFill() {
        return this.getContainer();
    }



}
