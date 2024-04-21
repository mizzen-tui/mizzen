package dtb.terminalIO.components.frameset;

import dtb.terminalIO.components.AbstractComponentWrappper;
import dtb.terminalIO.components.Component;
import dtb.terminalIO.components.HasTitleAndBorder;
import dtb.terminalIO.controlcodes.color.Color;
import dtb.terminalIO.geom.Dimension;

public class VerticalSplitter extends AbstractComponentWrappper<FrameSet> implements HasTitleAndBorder<VerticalSplitter> {

    Component<?> above; 
    Component<?> below;

    public VerticalSplitter(Color color, Dimension dimension) {
        super(color);
        FrameSet frameSet = new FrameSet(color, dimension);
        frameSet.setBorder(false);
        this.setWrappedComponent(frameSet);
    }

    public VerticalSplitter addComponent(Component<?> component) {
        if (this.below != null) {
            throw new IllegalStateException("Already added above and below components!");
        }
        if (this.above == null) {
            getWrappedComponent().add(component);
            this.above = component;
            return this;
        }
        getWrappedComponent().addBelow(component, this.above, true); 
        this.below = component;
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
    public VerticalSplitter setBorder(boolean hasBorder) {
        getWrappedComponent().setBorder(hasBorder);
        return this;
    }

    @Override
    public VerticalSplitter setBorderColor(Color borderColors) {
        getWrappedComponent().setBorderColor(borderColors);
        return this;
    }

    @Override
    public VerticalSplitter setTitle(Color titleColors, String title) {
        getWrappedComponent().setTitle(titleColors, title);
        return this;
    }

    @Override
    public VerticalSplitter setTitle(String title) {
        getWrappedComponent().setTitle(title);
        return this;
    }

    @Override
    public String getTitle() {
        return getWrappedComponent().getTitle();
    }


}
