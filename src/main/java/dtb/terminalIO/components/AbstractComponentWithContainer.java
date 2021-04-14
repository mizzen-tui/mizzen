package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.position.Position;

public abstract class AbstractComponentWithContainer<T extends Composite>  extends 
                          AbstractComponentWrappper<T> 
                          implements CanResize, GetComponentAtOffset {

    protected AbstractComponentWithContainer(Color color, T container) {
        super(color, container);
    }


    @Override
    public Component<?> getComponentAtOffset(Position offset) {
        Component<?> component = getWrappedComponent().getComponentAtOffset(offset);
        if (component == getWrappedComponent()) {
            return this;
        }
        return component;
    }

    @Override
    public Position getPositionRelativeToParent(Component<?> child) {
        if (child == getWrappedComponent()) {
            return Position.ZERO;
        }
        return null;
    }



}
