package dtb.terminalIO.components.plot.shape.catalog;

import dtb.terminalIO.components.plot.shape.ConnectedPoints;
import dtb.terminalIO.components.plot.shape.Shape;

/**
 * The curve a spirograph draws: a point going round a circle of radius2 while that circle's centre
 * goes round one of radius1, ratio times as fast. new Spirograph(40, 40, 20, 30, 10)
 */
public class Spirograph extends BuiltShape<Spirograph> {

    private float radius1;
    private float radius2;
    private float ratio;

    public Spirograph(float cx, float cy, float radius1, float radius2, float ratio) {
        super(cx, cy);
        this.radius1 = radius1;
        this.radius2 = radius2;
        this.ratio = ratio;
        setName("Spirograph");
    }

    public float getRadius1() {
        return radius1;
    }

    public float getRadius2() {
        return radius2;
    }

    public float getRatio() {
        return ratio;
    }

    public Spirograph setRadii(float radius1, float radius2) {
        this.radius1 = radius1;
        this.radius2 = radius2;
        return rebuild();
    }

    /** how many times faster the point turns than the circle it is on */
    public Spirograph setRatio(float ratio) {
        this.ratio = ratio;
        return rebuild();
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected Shape<?> build() {
        ConnectedPoints curve = new ConnectedPoints(false);
        for (double t = 0; t <= Math.PI * 2; t += 0.01) {
            curve.addPoint(at(radius1 * Math.cos(t) + radius2 * Math.cos(t * ratio),
                              radius1 * Math.sin(t) + radius2 * Math.sin(t * ratio)));
        }
        return curve;
    }
}
