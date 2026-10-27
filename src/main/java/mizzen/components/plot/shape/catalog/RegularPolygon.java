package mizzen.components.plot.shape.catalog;

import mizzen.components.plot.shape.Fillable;
import mizzen.components.plot.shape.Polygon;
import mizzen.components.plot.shape.Shape;

/**
 * A polygon with equal sides - a triangle, a square on its corner, a hexagon - its corners on a
 * circle, the first one straight up. Optionally a round hole in the middle: a hex nut is
 * new RegularPolygon(40, 40, 30, 6).setHoleRadius(12).
 */
public class RegularPolygon extends BuiltShape<RegularPolygon> implements Fillable<RegularPolygon> {

    private float radius;
    private int sides;
    private float holeRadius;

    /** radius: from the centre to a corner */
    public RegularPolygon(float cx, float cy, float radius, int sides) {
        super(cx, cy);
        this.radius = radius;
        setSides(sides);
        setName("Polygon");
    }

    public float getRadius() {
        return radius;
    }

    public RegularPolygon setRadius(float radius) {
        this.radius = radius;
        return rebuild();
    }

    public int getSides() {
        return sides;
    }

    /** how many sides (3 or more) */
    public RegularPolygon setSides(int sides) {
        if (sides < 3) {
            throw new IllegalArgumentException("a polygon needs 3 sides or more, not " + sides);
        }
        this.sides = sides;
        return rebuild();
    }

    public float getHoleRadius() {
        return holeRadius;
    }

    /** a round hole in the middle (0: none), left empty when filled */
    public RegularPolygon setHoleRadius(float holeRadius) {
        this.holeRadius = holeRadius;
        return rebuild();
    }

    @Override
    public RegularPolygon setFilled(boolean filled) {
        this.filled = filled;
        return rebuild();
    }

    @Override
    protected Shape<?> build() {
        Polygon polygon = new Polygon();
        for (int i = 0; i < sides; i++) {
            polygon.addPoint(onCircle(radius, -90 + i * 360d / sides));
        }
        if (holeRadius > 0) {
            polygon.addHole(Ring.circle(this, holeRadius));
        }
        return polygon.setFilled(filled);
    }

}
