package dtb.terminalIO.components.plot.shape;

import java.util.ArrayList;
import java.util.Collection;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.geom.position.PositionTransform;

/**
 * A {@link Shape} that contains multiple shapes
 * 
 * <p/>
 * 
 * The draw method will draw all
 * 
 */
public class CompositeShape implements Shape<CompositeShape> {

    Collection<Shape<?>> shapes = new ArrayList<Shape<?>>();
    PositionTransform positionTransform = PositionTransform.NOOP;
    String name;

    public CompositeShape addShape(Shape<?> shape) {
        shapes.add(shape);
        return this;
    }

    public CompositeShape removeShape(Shape<?> shape) {
        shapes.remove(shape);
        return this;
    }

    protected void drawChild(Shape<?> childShape, PointPlotter pointPlotter) {
        childShape.draw(pointPlotter);
    }

    @Override
    public void draw(PointPlotter pointPlotter) {
        for (Shape<?> shape : shapes) {
            drawChild(shape, pointPlotter);
        }
    }

    @Override
    public PositionTransform getPositionTransform() {
        return this.positionTransform;
    }

    @Override
    public void setPositionTransform(PositionTransform positionTransform) {
        this.positionTransform = positionTransform;
        for (Shape<?> shape : shapes) {
            shape.setPositionTransform(positionTransform);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public CompositeShape setName(String name) {
        this.name = name;
        return this;
    }

}
