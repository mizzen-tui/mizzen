package dtb.terminalIO.components.plot.shape;

import java.util.ArrayList;
import java.util.Collection;

import dtb.terminalIO.components.plot.PointPlotter;

/**
 * A {@link Shape} that contains multiple shapes
 * 
 * <p/>
 * 
 * The draw method will draw all
 * 
 */
public class CompositeShape implements Shape {

    Collection<Shape> shapes = new ArrayList<Shape>();

    public CompositeShape addShape(Shape shape) {
        shapes.add(shape);
        return this;
    }

    public CompositeShape removeShape(Shape shape) {
        shapes.remove(shape);
        return this;
    }

    protected void drawChild(Shape childShape, PointPlotter pointPlotter) {
        childShape.draw(pointPlotter);
    }
    
    @Override
    public void draw(PointPlotter pointPlotter) {
        for (Shape shape : shapes) {
            drawChild(shape, pointPlotter);
        }
    }

}
