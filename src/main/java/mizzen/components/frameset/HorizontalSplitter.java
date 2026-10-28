package mizzen.components.frameset;

import mizzen.components.AbstractComponentWrappper;
import mizzen.components.Component;
import mizzen.components.HasTitleAndBorder;
import mizzen.controlcodes.color.Color;
import mizzen.geom.Dimension;

public class HorizontalSplitter extends AbstractComponentWrappper<FrameSet> implements HasTitleAndBorder<HorizontalSplitter>  {

    Component<?> left; 
    Component<?> right;

    /** sized by what holds it (a splitter's rules, a resize): it starts at no size */
    public HorizontalSplitter(Color color) {
        this(color, Dimension.ZERO);
    }

    public HorizontalSplitter(Color color, Dimension dimension) {
        super(color);
        FrameSet frameSet = new FrameSet(color, dimension);
        frameSet.setBorder(true);
        this.setWrappedComponent(frameSet);
    }

    /**
     * Left pane first, then the right one. The left keeps its own width and the right takes the rest;
     * a left pane with no width (made without a size) gets half - onResizeExpandRight(left, percent)
     * sets another share.
     */
    public HorizontalSplitter addComponent(Component<?> component) {
        if (this.right != null) {
            throw new IllegalStateException("Already added left and right components!");
        }
        if (this.left == null) {
            getWrappedComponent().add(component);
            this.left = component;
            getWrappedComponent().onResizeExpandDown(this.left);
            if (component.getWidth() == 0) {
                getWrappedComponent().onResizeExpandRight(this.left, 50);
            }
            return this;
        }
        getWrappedComponent().addRight(component, this.left, true);  // true means draggable
        this.right = component;
        getWrappedComponent().onResizeExpandDownAndRight(this.right);
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
