package dtb.terminalIO.components.plot.shape;

import java.util.ArrayList;
import java.util.Collection;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.geom.AsRectangle2D;
import dtb.terminalIO.geom.Rectangle2D;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.traverse.HasChildren;

/**
 * A {@link Shape} that contains multiple shapes
 * 
 * <p/>
 * 
 * The draw method will draw all
 * 
 */
public class CompositeShape extends AbstractCompositeShape<CompositeShape> {

    @Override
    protected Collection<Shape<?>> buildShapes() {
        return new ArrayList<Shape<?>>();
    }

    public CompositeShape addShape(Shape<?> shape) {
        getShapes().add(shape);
        return this;
    }

    public CompositeShape removeShape(Shape<?> shape) {
        getShapes().remove(shape);
        return this;
    }

}
