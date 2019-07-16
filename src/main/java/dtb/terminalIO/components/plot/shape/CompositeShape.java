package dtb.terminalIO.components.plot.shape;

import java.util.ArrayList;
import java.util.Collection;

import dtb.terminalIO.components.plot.PointPlotter;

/**
 * A shape that contains multiple shapes
 * 
 * The draw method will draw all
 * 
 */
public class CompositeShape implements Shape {

    Collection<Shape> shapes = new ArrayList<Shape>();

    public void addShape(Shape shape) {
        shapes.add(shape);
    }

    public void removeShape(Shape shape) {
        shapes.remove(shape);
    }

    @Override
    public void draw(PointPlotter pointPlotter) {
        for (Shape shape : shapes) {
            shape.draw(pointPlotter);
        }
    }

}
