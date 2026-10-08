package dtb.terminalIO.components.plot.shape.catalog;

import dtb.terminalIO.components.plot.shape.Fillable;
import dtb.terminalIO.components.plot.shape.Polygon;
import dtb.terminalIO.components.plot.shape.Shape;

/**
 * A star: its points on an outer circle, the corners between them on an inner one, the first point
 * straight up. new Star(40, 40, 36, 15) is the usual five-pointed star, 72 across.
 */
public class Star extends BuiltShape<Star> implements Fillable<Star> {

    private int points = 5;
    private float outerRadius;
    private float innerRadius;

    public Star(float cx, float cy, float outerRadius, float innerRadius) {
        super(cx, cy);
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        setName("Star");
    }

    public int getPoints() {
        return points;
    }

    /** how many points (3 or more) */
    public Star setPoints(int points) {
        if (points < 3) {
            throw new IllegalArgumentException("a star needs 3 points or more, not " + points);
        }
        this.points = points;
        return rebuild();
    }

    public float getOuterRadius() {
        return outerRadius;
    }

    public float getInnerRadius() {
        return innerRadius;
    }

    /** outer: the tips' circle; inner: the corners between them - the smaller, the spikier */
    public Star setRadii(float outerRadius, float innerRadius) {
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        return rebuild();
    }

    @Override
    public Star setFilled(boolean filled) {
        this.filled = filled;
        return rebuild();
    }

    @Override
    protected Shape<?> build() {
        Polygon star = new Polygon();
        double step = 180d / points;
        for (int i = 0; i < points * 2; i++) {
            star.addPoint(onCircle(i % 2 == 0 ? outerRadius : innerRadius, -90 + i * step));
        }
        return star.setFilled(filled);
    }
}
