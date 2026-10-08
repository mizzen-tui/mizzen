package dtb.terminalIO.components.plot.shape.catalog;

import dtb.terminalIO.components.plot.shape.ConnectedPoints;
import dtb.terminalIO.components.plot.shape.Shape;

/**
 * A coil spring seen from the side, lying across: a straight end, a zig-zag of coils, the other
 * end. new Spring(40, 20, 70, 16, 6) is 70 long, 16 high, with 6 coils.
 */
public class Spring extends BuiltShape<Spring> {

    private float length;
    private float height;
    private int coils;

    public Spring(float cx, float cy, float length, float height, int coils) {
        super(cx, cy);
        this.length = length;
        this.height = height;
        setCoils(coils);
        setName("Spring");
    }

    public float getLength() {
        return length;
    }

    public float getHeight() {
        return height;
    }

    public Spring setSize(float length, float height) {
        this.length = length;
        this.height = height;
        return rebuild();
    }

    public int getCoils() {
        return coils;
    }

    /** how many coils (1 or more) */
    public Spring setCoils(int coils) {
        if (coils < 1) {
            throw new IllegalArgumentException("a spring needs a coil or more, not " + coils);
        }
        this.coils = coils;
        return rebuild();
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected Shape<?> build() {
        ConnectedPoints spring = new ConnectedPoints(false);
        double half = length / 2d, end = length * 0.12, coilsFrom = -half + end;
        double each = (length - 2 * end) / (coils * 2);       // a coil: up, then down
        spring.addPoint(at(-half, 0));
        spring.addPoint(at(coilsFrom, 0));
        for (int i = 0; i < coils * 2; i++) {
            spring.addPoint(at(coilsFrom + each * (i + 0.5), i % 2 == 0 ? -height / 2 : height / 2));
        }
        spring.addPoint(at(half - end, 0));
        spring.addPoint(at(half, 0));
        return spring;
    }
}
