package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;

public class ShapeWithTransform extends Shape.Delegate {

    // TODO:  I need to document why this needs to be a delegate
    //        why I did this escapes me ( for the moment )
    private PointPlotter.Delegate pointPlotter;

    public ShapeWithTransform(Shape shape, PointPlotter.Delegate pointPlotter) {
        super(shape);
        this.pointPlotter = pointPlotter;
    }

    public ShapeWithTransform(Shape shape) {
        this(shape, new PointPlotter.Delegate(null));
    }

    public PointPlotter.Delegate getPointPlotter() {
        return pointPlotter;
    }

    public void setPointPlotter(PointPlotter.Delegate pointPlotter) {
        this.pointPlotter = pointPlotter;
    }

    @Override
    public void draw(PointPlotter pointPlotter) {
        this.pointPlotter.setPointPlotter(pointPlotter);
        super.draw(this.pointPlotter);
    }

}
