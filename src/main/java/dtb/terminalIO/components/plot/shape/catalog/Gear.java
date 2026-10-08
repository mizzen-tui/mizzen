package dtb.terminalIO.components.plot.shape.catalog;

import dtb.terminalIO.components.plot.shape.Fillable;
import dtb.terminalIO.components.plot.shape.Polygon;
import dtb.terminalIO.components.plot.shape.Shape;

/**
 * A gear wheel: teeth round a root circle, and a round axle hole. Each tooth rises from the root
 * circle to the outer one, runs flat, and drops back. new Gear(40, 40, 12, 36) has 12 teeth and is
 * 72 across; the root circle and the hole follow from that (setRootRadius, setHoleRadius to change
 * them).
 */
public class Gear extends BuiltShape<Gear> implements Fillable<Gear> {

    private int teeth;
    private float outerRadius;
    private float rootRadius;
    private float holeRadius;

    public Gear(float cx, float cy, int teeth, float outerRadius) {
        super(cx, cy);
        this.outerRadius = outerRadius;
        this.rootRadius = outerRadius * 7 / 9;
        this.holeRadius = outerRadius * 5 / 18;
        setTeeth(teeth);
        setName("Gear");
    }

    public int getTeeth() {
        return teeth;
    }

    /** how many teeth (3 or more) */
    public Gear setTeeth(int teeth) {
        if (teeth < 3) {
            throw new IllegalArgumentException("a gear needs 3 teeth or more, not " + teeth);
        }
        this.teeth = teeth;
        return rebuild();
    }

    public float getOuterRadius() {
        return outerRadius;
    }

    /** to the tips of the teeth */
    public Gear setOuterRadius(float outerRadius) {
        this.outerRadius = outerRadius;
        return rebuild();
    }

    public float getRootRadius() {
        return rootRadius;
    }

    /** to the bottom of the gaps between teeth */
    public Gear setRootRadius(float rootRadius) {
        this.rootRadius = rootRadius;
        return rebuild();
    }

    public float getHoleRadius() {
        return holeRadius;
    }

    /** the axle hole (0: none), left empty when filled */
    public Gear setHoleRadius(float holeRadius) {
        this.holeRadius = holeRadius;
        return rebuild();
    }

    @Override
    public Gear setFilled(boolean filled) {
        this.filled = filled;
        return rebuild();
    }

    @Override
    protected Shape<?> build() {
        Polygon gear = new Polygon();
        double step = 360d / teeth;
        for (int i = 0; i < teeth; i++) {
            double a = i * step;
            gear.addPoint(onCircle(rootRadius, a));
            gear.addPoint(onCircle(outerRadius, a + step * 0.15));
            gear.addPoint(onCircle(outerRadius, a + step * 0.45));
            gear.addPoint(onCircle(rootRadius, a + step * 0.6));
        }
        if (holeRadius > 0) {
            gear.addHole(Ring.circle(this, holeRadius));
        }
        return gear.setFilled(filled);
    }
}
