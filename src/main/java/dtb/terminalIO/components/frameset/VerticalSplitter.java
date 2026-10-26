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
            getWrappedComponent().onResizeExpandRight(component);
            return this;
        }
        getWrappedComponent().addBelow(component, this.above, true); // true means draggable
        this.below = component;
        getWrappedComponent().onResizeExpandDownAndRight(this.below);
        return this;
    }

    /** a pane's height as a share of the splitter's, e.g. onResizeExpandDown(top, 40).atLeast(9) - a pane after it takes the rest */
    public ExpandOnResize onResizeExpandDown(Component<?> pane, int percent) {
        return getWrappedComponent().onResizeExpandDown(pane, percent);
    }

    /** a pane's width as a share of the splitter's, e.g. onResizeExpandRight(left, 30) - a pane after it takes the rest */
    public ExpandOnResize onResizeExpandRight(Component<?> pane, int percent) {
        return getWrappedComponent().onResizeExpandRight(pane, percent);
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
