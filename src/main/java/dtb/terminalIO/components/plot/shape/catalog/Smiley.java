package dtb.terminalIO.components.plot.shape.catalog;

import dtb.terminalIO.components.plot.shape.Circle;
import dtb.terminalIO.components.plot.shape.CompositeShape;
import dtb.terminalIO.components.plot.shape.ConnectedPoints;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.geom.position.Vec2;

/**
 * A smiley face: the face, two filled eyes, and the mouth - an open curve. new Smiley(30, 30, 28)
 */
public class Smiley extends BuiltShape<Smiley> {

    private float radius;

    public Smiley(float cx, float cy, float radius) {
        super(cx, cy);
        this.radius = radius;
        setName("Smiley");
    }

    public float getRadius() {
        return radius;
    }

    public Smiley setRadius(float radius) {
        this.radius = radius;
        return rebuild();
    }

    private Circle circle(Vec2<?> centre, float r) {
        return new Circle(centre.getX(), centre.getY(), r);
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected Shape<?> build() {
        float k = radius / 28;                                // laid out for a face 28 in radius
        ConnectedPoints mouth = new ConnectedPoints(false);
        for (int degrees = 25; degrees <= 155; degrees += 5) {
            mouth.addPoint(onCircle(18 * k, degrees));
        }
        return new CompositeShape()
                .addShape(circle(at(0, 0), radius))                         // face
                .addShape(circle(at(-10 * k, -10 * k), 4 * k).setFilled(true)) // eyes
                .addShape(circle(at(10 * k, -10 * k), 4 * k).setFilled(true))
                .addShape(mouth);
    }
}
