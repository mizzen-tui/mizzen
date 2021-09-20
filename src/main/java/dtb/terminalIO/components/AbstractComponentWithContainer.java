package dtb.terminalIO.components;

import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.position.Position;

public abstract class AbstractComponentWithContainer<T extends Composite>  extends 
                          AbstractComponentWrappper<T> 
                          implements CanResize, GetComponentAtOffset {

    protected AbstractComponentWithContainer(Color color, T container) {
        super(color, container);
    }


    public void add(Component<?> child) {
        getWrappedComponent().add(child);
    }
    
    public AbstractComponentWithContainer<T> add(int offsetRow, int offsetCol, Component<?> component) {
        getWrappedComponent().add(offsetRow, offsetCol, component);
        return this;
    }
    
    public AbstractComponentWithContainer<T> add(Position offset, Component<?> component) {
        getWrappedComponent().add(offset, component);
        return this;
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

    public AbstractComponentWithContainer<T> setEmptyChar(char emptyChar) {
        super.setEmptyChar(emptyChar);
        if (getWrappedComponent() != null) {
            getWrappedComponent().setEmptyChar(emptyChar);
        }
        return this;
    }

    public AbstractComponentWithContainer<T> setEmptyColor(Color emptyColor) {
        super.setEmptyColor(emptyColor);
        if (getWrappedComponent() != null) {
           getWrappedComponent().setEmptyColor(emptyColor);
        }
        return this;
    }

    public void setFillOnClear(boolean fillOnClear) {
        super.setFillOnClear(fillOnClear);
        getWrappedComponent().setFillOnClear(fillOnClear);
    }
    
    public void onResizeExpandRight(Component<?> component, int percent) {
        getWrappedComponent().onResizeExpandRight(component, percent);
    }
    
    public void onResizeExpandDown(Component<?> component, int percent) {
        getWrappedComponent().onResizeExpandDown(component, percent);
    }

    public void onResizeExpandDownAndRight(Component<?> component) {
        getWrappedComponent().onResizeExpandDownAndRight(component);
    }


}
