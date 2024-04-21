package dtb.terminalIO.components.frameset;

import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasTitleAndBorder;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;

public class HorizontalSplitter extends AbstractComponentWrappper<FrameSet> implements HasTitleAndBorder<HorizontalSplitter>  {

    Component<?> left; 
    Component<?> right;

    public HorizontalSplitter(Color color, Dimension dimension) {
        super(color);
        FrameSet frameSet = new FrameSet(color, dimension);
        frameSet.setBorder(true);
        this.setWrappedComponent(frameSet);
    }

    public HorizontalSplitter addComponent(Component<?> component) {
        if (this.right != null) {
            throw new IllegalStateException("Already added left and right components!");
        }
        if (this.left == null) {
            getWrappedComponent().add(component);
            this.left = component;
            return this;
        }
        getWrappedComponent().addRight(component, this.left, true); 
        this.right = component;
        return this;
    }

    @Override
    public void hilite() {
        getWrappedComponent().hilite();
    }

    @Override
    public void unHilite() {
        getWrappedComponent().hilite();
    }

    @Override
    public boolean hasBorder() {
        return getWrappedComponent().hasBorder();
    }

    @Override
    public HorizontalSplitter setBorder(boolean hasBorder) {
        getWrappedComponent().setBorder(hasBorder);
        return this;
    }

    @Override
    public HorizontalSplitter setBorderColor(Color borderColors) {
        getWrappedComponent().setBorderColor(borderColors);
        return this;
    }

    @Override
    public HorizontalSplitter setTitle(Color titleColors, String title) {
        getWrappedComponent().setTitle(titleColors, title);
        return this;
    }

    @Override
    public HorizontalSplitter setTitle(String title) {
        getWrappedComponent().setTitle(title);
        return this;
    }

    @Override
    public String getTitle() {
        return getWrappedComponent().getTitle();
    }

}
