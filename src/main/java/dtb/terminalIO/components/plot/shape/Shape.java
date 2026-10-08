package dtb.terminalIO.components.plot.shape;

import java.util.Collection;

import dtb.terminalIO.components.HasName;
import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.PointPlotterAsRectangle;
import dtb.terminalIO.geom.AsRectangle2D;
import dtb.terminalIO.geom.Rectangle2D;
import dtb.terminalIO.geom.Rectangle2D.Float2D;
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

    /**
     * The box around the shape as it draws now - its transform applied (a rotated shape's box is
     * generally bigger than its unrotated one). Shapes that know their corners work it out from
     * them; any other is drawn into a box-measuring plotter.
     */
    default Rectangle2D.Float2D bounds() {
        return UTIL.getAsRectangle(this);
    }

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

    public static class UTIL {

        static public Rectangle2D.Float2D getAsRectangle(Shape<?> shape) {

            PointPlotterAsRectangle calculatedRectanglePlotter = new PointPlotterAsRectangle();
            calculateAndGrowRectangleFromShape(calculatedRectanglePlotter, shape);
            return calculatedRectanglePlotter.getRectangle2D();

        }

        static public Rectangle2D.Float2D getAsRectangle(Collection<Shape<?>> shapes) {

            PointPlotterAsRectangle calculatedRectanglePlotter = new PointPlotterAsRectangle();

            for (Shape<?> shape : shapes) {
                calculateAndGrowRectangleFromShape(calculatedRectanglePlotter, shape);
            }

            return calculatedRectanglePlotter.getRectangle2D();

        }

        static private void calculateAndGrowRectangleFromShape(PointPlotterAsRectangle calculatedRectanglePlotter, Shape<?> shape) {

            if (shape instanceof AsRectangle2D.AsFloat2D) {
                Float2D sizeOfShape = (Rectangle2D.Float2D) ((AsRectangle2D.AsFloat2D)shape).getRectangle2D(); 
                calculatedRectanglePlotter.plot(sizeOfShape.getX1(), sizeOfShape.getY1());
                calculatedRectanglePlotter.plot(sizeOfShape.getX2(), sizeOfShape.getY2());
                return;
            }

            shape.draw(calculatedRectanglePlotter);

        }

    }

}
