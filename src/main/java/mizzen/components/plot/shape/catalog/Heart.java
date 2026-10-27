package mizzen.components.plot.shape.catalog;

import mizzen.components.plot.shape.Fillable;
import mizzen.components.plot.shape.Polygon;
import mizzen.components.plot.shape.Shape;

/**
 * A heart, from the classic curve x = 16 sin^3 t, y = 13 cos t - 5 cos 2t - 2 cos 3t - cos 4t,
 * sized to width. new Heart(40, 40, 70)
 */
public class Heart extends BuiltShape<Heart> implements Fillable<Heart> {

    private float width;

    public Heart(float cx, float cy, float width) {
        super(cx, cy);
        this.width = width;
        setName("Heart");
    }

    public float getWidth() {
        return width;
    }

    public Heart setWidth(float width) {
        this.width = width;
        return rebuild();
    }

    @Override
    public Heart setFilled(boolean filled) {
        this.filled = filled;
        return rebuild();
    }

    @Override
    protected Shape<?> build() {
        Polygon heart = new Polygon();
        double k = width / 32d;                               // the curve is 32 across
        for (int i = 0; i < 72; i++) {
            double t = Math.toRadians(i * 5);
            double x = 16 * Math.pow(Math.sin(t), 3);
            double y = 13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t);
            heart.addPoint(at(k * x, -k * y));                // the curve's y grows upwards
        }
        return heart.setFilled(filled);
    }
}
