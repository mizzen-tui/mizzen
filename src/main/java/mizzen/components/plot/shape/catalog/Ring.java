package mizzen.components.plot.shape.catalog;

import java.util.ArrayList;
import java.util.List;

import mizzen.components.plot.shape.Fillable;
import mizzen.components.plot.shape.Polygon;
import mizzen.components.plot.shape.Shape;
import mizzen.geom.position.Vec2;

/**
 * A ring - a washer, a doughnut: a circle with a round hole. Filled, only the band between them is
 * filled. new Ring(40, 40, 30, 12).setFilled(true)
 */
public class Ring extends BuiltShape<Ring> implements Fillable<Ring> {

    private float outerRadius;
    private float innerRadius;

    public Ring(float cx, float cy, float outerRadius, float innerRadius) {
        super(cx, cy);
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        setName("Ring");
    }

    public float getOuterRadius() {
        return outerRadius;
    }

    public float getInnerRadius() {
        return innerRadius;
    }

    public Ring setRadii(float outerRadius, float innerRadius) {
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        return rebuild();
    }

    @Override
    public Ring setFilled(boolean filled) {
        this.filled = filled;
        return rebuild();
    }

    @Override
    protected Shape<?> build() {
        Polygon ring = new Polygon(circle(this, outerRadius));
        if (innerRadius > 0) {
            ring.addHole(circle(this, innerRadius));
        }
        return ring.setFilled(filled);
    }

    /** a circle round the shape's centre as points - enough of them that it looks round */
    static List<Vec2<?>> circle(BuiltShape<?> shape, float radius) {
        int steps = 48;
        List<Vec2<?>> points = new ArrayList<Vec2<?>>();
        for (int i = 0; i < steps; i++) {
            points.add(shape.onCircle(radius, i * 360d / steps));
        }
        return points;
    }
}
