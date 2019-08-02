package dtb.terminalIO.components.plot.shape;

import dtb.terminalIO.components.plot.PointPlotter;

/**
 * An object (shape: ie circle, square, line, polygon etc) that can draw itself into a
 * {@link PointPlotter}
 */
public interface Shape {

    /**
     * Draw the shape given a pointPlotter
     * 
     * @param pointPlotter
     */
    public void draw(PointPlotter pointPlotter);

    
    /** 
     * A useful base class decorator for shapes
     */
    public static class Delegate implements Shape {

        Shape shape;

        public Delegate(Shape shape) {
            super();
            this.shape = shape;
        }

        public Shape getShape() {
            return shape;
        }

        public void setShape(Shape shape) {
            this.shape = shape;
        }

        @Override
        public void draw(PointPlotter pointPlotter) {
            this.getShape().draw(pointPlotter);
        }

    }

}
