package dtb.terminalIO.components.plot.shape.catalog;

import dtb.terminalIO.components.plot.PointPlotter;
import dtb.terminalIO.components.plot.shape.Shape;
import dtb.terminalIO.components.plot.shape.Shape.AbstractShape;
import dtb.terminalIO.geom.Rectangle2D;
import dtb.terminalIO.geom.position.PositionTransform;
import dtb.terminalIO.geom.position.Vec2;

/**
 * A shape made from a few settings - a gear's teeth, a star's points - placed by its centre. It
 * builds the plain shapes that draw it (a Polygon, a CompositeShape) from its settings, and builds
 * them again when a setting changes:
 * <pre>
 *     Gear gear = new Gear(40, 40, 12, 36);      // centre 40, 40; 12 teeth; 36 across the teeth
 *     gear.setTeeth(8).setFilled(true);          // drawn with 8 teeth from now on
 * </pre>
 * Like other shapes, it is moved, turned and scaled by its transform, and its settings are in
 * whatever units it is drawn in (grid dots, a plot's own numbers).
 * <p/>
 * y grows downwards, as on the screen: "up" (a house's roof, a heart's dip) is towards smaller y.
 * In a PlotArea, whose y goes up, setYUp(true) so they come out the right way up.
 */
public abstract class BuiltShape<T extends BuiltShape<?>> extends AbstractShape<T> {

    protected float cx;
    protected float cy;
    protected boolean yUp;
    protected boolean filled;
    private Shape<?> built;

    protected BuiltShape(float cx, float cy) {
        this.cx = cx;
        this.cy = cy;
    }

    /** the plain shape(s) that draw this one, from its settings now */
    protected abstract Shape<?> build();

    /** a setting changed: build again before the next draw */
    @SuppressWarnings("unchecked")
    protected T rebuild() {
        built = null;
        return (T) this;
    }

    private Shape<?> built() {
        if (built == null) {
            built = build();
        }
        return built;
    }

    public float getCentreX() {
        return cx;
    }

    public float getCentreY() {
        return cy;
    }

    public T setCentre(float x, float y) {
        this.cx = x;
        this.cy = y;
        return rebuild();
    }

    public boolean isYUp() {
        return yUp;
    }

    /** true: y grows upwards (a PlotArea), so up is towards bigger y */
    public T setYUp(boolean yUp) {
        this.yUp = yUp;
        return rebuild();
    }

    /** a point dx across and dy down from the centre (up, when y grows upwards) */
    protected Vec2<?> at(double dx, double dy) {
        return Vec2.xy((float) (cx + dx), (float) (yUp ? cy - dy : cy + dy));
    }

    /** a point on a circle round the centre: 0 degrees is to the right, 90 straight down (on screen) */
    protected Vec2<?> onCircle(double radius, double degrees) {
        double a = Math.toRadians(degrees);
        return at(radius * Math.cos(a), radius * Math.sin(a));
    }

    @Override
    protected void drawWithTransform(PointPlotter pointPlotter, PositionTransform positionTransform) {
        Shape<?> shape = built();
        shape.setPositionTransform(positionTransform);
        shape.draw(pointPlotter);
    }

    @Override
    public Rectangle2D.Float2D bounds() {
        Shape<?> shape = built();
        shape.setPositionTransform(getPositionTransform());
        return shape.bounds();
    }
}
