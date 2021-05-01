package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.geom.position.PositionTransform;

/**
 * An object (shape: ie circle, square, line, polygon etc) that can draw itself into a
 * {@link PointPlotter}
 */
public interface Shape<T extends Shape<?>> extends HasName<T> {

    /**
     * Draw the shape given a pointPlotter
     * 
     * @param pointPlotter
     */
    public void draw(PointPlotter pointPlotter);
    
    PositionTransform getPositionTransform();
    
    void setPositionTransform(PositionTransform positionTransform);

    
    public static abstract class AbstractShape<T extends AbstractShape<?>> implements Shape<T> {

        PositionTransform positionTransform = PositionTransform.NOOP; 
        String name;

        public AbstractShape() {
            this.setName(HasName.UTIL.deriveName(this));
        }

        @Override
        final public void draw(PointPlotter pointPlotter) {
            drawWithTransform(pointPlotter, getPositionTransform());
        }


        abstract protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform);

        @Override
        public PositionTransform getPositionTransform() {
            return positionTransform;
        }

        @Override
        public void setPositionTransform(PositionTransform positionTransform) {
            this.positionTransform = positionTransform;
        }


        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public T setName(String name) {
            this.name = name;
            return (T) this;
        }

        
    }
    
    /** 
     * A useful base class decorator for shapes
     */
    public static class Delegate implements Shape<Delegate> {

        Shape<?> shape;

        public Delegate(Shape<?> shape) {
            super();
            this.shape = shape;
        }

        public Shape<?> getShape() {
            return shape;
        }

        public void setShape(Shape<?> shape) {
            this.shape = shape;
        }

        @Override
        public PositionTransform getPositionTransform() {
            return this.getShape().getPositionTransform();
        }

        @Override
        public void setPositionTransform(PositionTransform positionTransform) {
            this.getShape().setPositionTransform(positionTransform);
        }

        @Override
        public void draw(PointPlotter pointPlotter) {
            this.getShape().draw(pointPlotter);
        }

        @Override
        public String getName() {
            return getShape().getName();
        }

        @Override
        public Delegate setName(String name) {
            getShape().setName(name);
            return this;
        }

    }

}
