package dtb.terminalIO.components;

import dtb.terminalIO.components.composite.Composite;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.CanResize;
import dtb.terminalIO.geom.position.Position;

public abstract class AbstractComponentWithContainer<T extends Composite>  extends 
                          AbstractComponentWrappper<T> 
                          implements CanResize, 
                                     GetComponentAtOffset,
                                     HasTitleAndBorder<AbstractComponentWithContainer<T>> {

    protected AbstractComponentWithContainer(Color color) {
        super(color);
    }

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
        if (getWrappedComponent() != null) {
           getWrappedComponent().setFillOnClear(fillOnClear);
        }
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

    public AbstractComponentWithContainer<T>  setBorder(boolean hasBorder) {
        getWrappedComponent().setBorder(hasBorder);
        return this;
    }
    
    public boolean hasBorder() {
        return getWrappedComponent().hasBorder();
    }

    public AbstractComponentWithContainer<T> setBorderColor(Color borderColors) {
        getWrappedComponent().setBorderColor(borderColors);
        return this;
    }

    public AbstractComponentWithContainer<T> setTitle(Color titleColors, String title) {
        getWrappedComponent().setTitle(titleColors, title);
        return this;
    }

    public AbstractComponentWithContainer<T> setTitle(String title) {
        getWrappedComponent().setTitle(title);
        return this;
    }
    
    public String getTitle() {
        return getWrappedComponent().getTitle();
    }
    
    @Override
    public void hilite() {
        getWrappedComponent().hilite();
    }

    @Override
    public void unHilite() {
        getWrappedComponent().unHilite();
    }
    

}
