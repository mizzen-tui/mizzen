package mizzen.components.plot.shape;

import java.util.Collection;

import mizzen.components.plot.PointPlotter;
import mizzen.geom.AsRectangle2D;
import mizzen.geom.Rectangle2D;
import mizzen.geom.position.PositionTransform;
import mizzen.traverse.HasChildren;

public abstract class AbstractCompositeShape<T extends AbstractCompositeShape<?>> implements Shape<T> , HasChildren<Shape<?>>, AsRectangle2D.AsFloat2D {
    
    private Collection<Shape<?>> shapes = null;
    private PositionTransform positionTransform = PositionTransform.NOOP;
    private String name;

    protected void drawChild(Shape<?> childShape, PointPlotter pointPlotter) {
        childShape.draw(pointPlotter);
    }

    protected Collection<Shape<?>> getShapes() {
        if (shapes == null) {
            shapes = buildShapes();
        }
        return shapes;
    }

    abstract protected Collection<Shape<?>> buildShapes();

    @Override
    public void draw(PointPlotter pointPlotter) {
        for (Shape<?> shape : getShapes()) {
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
        for (Shape<?> shape : getShapes()) {
            shape.setPositionTransform(positionTransform);
        }
    }

    @Override
    public String getName() {
        return name;
    }


    @Override
    public Rectangle2D.Float2D getRectangle2D() {
        return Shape.UTIL.getAsRectangle( getShapes() );
    }

    @SuppressWarnings("unchecked")
    @Override
    public T setName(String name) {
        this.name = name;
        return (T)this;
    }

    @Override
    public Collection<? extends Shape<?>> getChildren() {
        return getShapes();
    }
}